
package sc.fiji.labkit.cli;

import net.imagej.ImgPlus;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.parallel.TaskExecutors;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.Context;
import picocli.CommandLine;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuPool;
import sc.fiji.labkit.ui.inputimage.SpimDataInputImage;
import sc.fiji.labkit.ui.segmentation.Segmenter;
import sc.fiji.labkit.ui.segmentation.weka.TrainableSegmentationSegmenter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * This class defines the "segment-chunk" sub command.
 * <p>
 * Given an image and a classifier, this command will calculate the segmentation
 * of a part of the image. Which part, is defined by the given number of chunks
 * and the chunk index.
 */
@CommandLine.Command(name = "segment-chunk",
	description = "Segment a part (chunk) of the image using a given classifier." +
		"Stores the results in the N5 folder.")
public class SegmentChunkCommand implements Callable<Optional<Integer>> {

	@CommandLine.Option(names = { "--image" }, required = true,
		description = "Image to be segmented.")
	private File imageXml;

	@CommandLine.Option(names = { "--classifier" }, required = true,
		description = "Classifier that was trained using the FIJI Labkit plugin.")
	private File classifier;

	@CommandLine.Option(names = { "--n5" }, required = true,
		description = "N5 folder that was created using the \"prepare\" sub command.")
	private File n5;

	@CommandLine.Option(names = { "--chunks" }, required = true,
		paramLabel = "NUMBER_OF_CHUNKS",
		description = "The segmentation task will be divided into the given number of chunks.")
	private int number_of_chunks;

	@CommandLine.Option(names = { "--index" }, required = true,
		paramLabel = "CHUNK_INDEX",
		description = "Index, of the chunk to be processed. Integer value greater or equal to zero, but smaller than the number of chunks.")
	private int index;

	@CommandLine.Option(names = { "--use-gpu" })
	private boolean use_gpu = false;

	@Override
	public Optional<Integer> call() throws Exception {
		SpimDataInputImage image = new SpimDataInputImage(imageXml
			.getAbsolutePath(), 0);
		Segmenter segmenter = openSegmenter();
		final ImgPlus<?> imgPlus = image.imageForSegmentation();
		Consumer<RandomAccessibleInterval<UnsignedByteType>> loader =
				block -> segmenter.segment(imgPlus, block);
		if(segmenter.requiresFixedCellSize())
			loader = fixBlockSize(loader);
		try(TaskExecutor taskExecutor = getTaskExecutor()) {
			writeN5Range(n5.getAbsolutePath(), index % number_of_chunks,
					number_of_chunks, loader, taskExecutor);
		}
		return Optional.of(0); // exit code 0
	}

	private TaskExecutor getTaskExecutor() {
		if (use_gpu) {
			// Use as many threads as there are parallel gpu accesses allowed with the GpuPool.
			// Each of those treads uses standard multithreading, for fast memory copying to the GPU.
			if(!GpuPool.isGpuAvailable()) {
				System.err.println("No OpenCL device found. Make sure you properly install your OpenCL drivers.");
				System.exit(1);
			}
			TaskExecutor taskExecutor = TaskExecutors.multiThreaded();
			ThreadFactory threadFactory = TaskExecutors.threadFactory(() -> taskExecutor);
			return TaskExecutors.forExecutorService(Executors.newFixedThreadPool(GpuPool.size(), threadFactory));
		}
		else {
			// Use as many threads as there are processors.
			// Single threading inside each of those threads.
			return TaskExecutors.fixedThreadPool(Runtime.getRuntime().availableProcessors());
		}
	}

	private Consumer<RandomAccessibleInterval<UnsignedByteType>> fixBlockSize(Consumer<RandomAccessibleInterval<UnsignedByteType>> loader) throws IOException {
		int[] blockSize = new N5FSWriter(n5.getAbsolutePath())
				.getDatasetAttributes(PrepareCommand.N5_DATASET_NAME)
				.getBlockSize();
		long[] size = IntStream.of(blockSize).mapToLong(x -> x).toArray();
		return block -> {
			if(Arrays.equals(Intervals.dimensionsAsLongArray(block), size))
				loader.accept(block);
			else {
				long[] min = Intervals.minAsLongArray(block);
				FinalInterval interval = FinalInterval.createMinSize(min, size);
				loader.accept(Views.interval(Views.extendZero(block), interval));
			}
		};
	}

	private TrainableSegmentationSegmenter openSegmenter()
	{
		TrainableSegmentationSegmenter segmenter =
			new TrainableSegmentationSegmenter(new Context());
		segmenter.openModel(classifier.getAbsolutePath());
		segmenter.setUseGpu(use_gpu);
		return segmenter;
	}

	private static void writeN5Range(String output, int index, int numberOfChunks,
			Consumer<RandomAccessibleInterval<UnsignedByteType>> loader, TaskExecutor taskExecutor)
		throws IOException
	{
		N5Writer writer = new N5FSWriter(output);
		long[] gridDimensions = getCellGrid(writer).getGridDimensions();
		int size = (int) Intervals.numElements(gridDimensions);
		int chunkSize = (size + numberOfChunks - 1) / numberOfChunks;
		int start = index * chunkSize;
		int end = Math.min(size, start + chunkSize);
		StopWatch watch = StopWatch.createAndStart();
		AtomicInteger counter = new AtomicInteger(start);
		taskExecutor.forEach(new IntRange(start, end), ignore -> {
			int i = counter.getAndIncrement();
			long[] blockOffset = new long[gridDimensions.length];
			IntervalIndexer.indexToPosition(i, gridDimensions, blockOffset);
			try {
				saveBlock(writer, blockOffset, loader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			System.out.println("Block " + (i - start) + " of " + (end - start) +
					" has been segmented. Block coordinates: " + Arrays.toString(
					blockOffset));
		});
		System.out.println("Time elapsed: " + watch);
	}

	private static void saveBlock(N5Writer writer, long[] blockOffset,
		Consumer<RandomAccessibleInterval<UnsignedByteType>> loader)
		throws IOException
	{
		CellGrid grid = getCellGrid(writer);
		long[] cellMin = new long[grid.numDimensions()];
		int[] cellDims = new int[grid.numDimensions()];
		grid.getCellDimensions(blockOffset, cellMin, cellDims);
		Img<UnsignedByteType> block = ArrayImgs.unsignedBytes(toLongs(cellDims));
		loader.accept(Views.translate(block, cellMin));
		N5Utils.saveBlock(block, writer, PrepareCommand.N5_DATASET_NAME,
			blockOffset);
	}

	private static CellGrid getCellGrid(N5Writer writer) throws IOException {
		DatasetAttributes attributes = writer.getDatasetAttributes(
			PrepareCommand.N5_DATASET_NAME);
		return new CellGrid(attributes.getDimensions(), attributes.getBlockSize());
	}

	private static long[] toLongs(int[] values) {
		return IntStream.of(values).mapToLong(x -> x).toArray();
	}
}
