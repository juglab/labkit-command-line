
package labkit_cluster.command;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.segmentation.Segmenter;
import net.imglib2.labkit.segmentation.weka.TimeSeriesSegmenter;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.Context;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
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
public class SegmentCommand implements Callable<Optional<Integer>> {

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

	@Override
	public Optional<Integer> call() throws Exception {
		SpimDataInputImage image = new SpimDataInputImage(imageXml
			.getAbsolutePath(), 0);
		Segmenter segmenter = initSegmenter(image);
		writeN5Range(n5.getAbsolutePath(), index % number_of_chunks,
			number_of_chunks, block -> segmenter.segment(image.imageForSegmentation(),
				block));
		return Optional.of(0); // exit code 0
	}

	private Segmenter initSegmenter(SpimDataInputImage image) {
		Segmenter segmenter = openSegmenter(classifier.getAbsolutePath(), image);
		if (image.isTimeSeries()) segmenter = new TimeSeriesSegmenter(segmenter);
		return segmenter;
	}

	private static TrainableSegmentationSegmenter openSegmenter(String classifier,
		SpimDataInputImage image)
	{
		TrainableSegmentationSegmenter segmenter =
			new TrainableSegmentationSegmenter(new Context(), image);
		segmenter.openModel(classifier);
		return segmenter;
	}

	private static void writeN5Range(String output, int index, int numberOfChunks,
		Consumer<RandomAccessibleInterval<UnsignedByteType>> loader)
		throws IOException
	{
		N5Writer writer = new N5FSWriter(output);
		long[] gridDimensions = getCellGrid(writer).getGridDimensions();
		long size = Intervals.numElements(gridDimensions);
		long chunkSize = (size + numberOfChunks - 1) / numberOfChunks;
		long start = index * chunkSize;
		long end = Math.min(size, start + chunkSize);
		for (long i = start; i < end; i++) {
			long[] blockOffset = new long[gridDimensions.length];
			IntervalIndexer.indexToPosition(i, gridDimensions, blockOffset);
			saveBlock(writer, blockOffset, loader);
			System.out.println("Block " + (i - start) + " of " + (end - start) +
				" has been segmented. Block coordinates: " + Arrays.toString(
					blockOffset));
		}
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
