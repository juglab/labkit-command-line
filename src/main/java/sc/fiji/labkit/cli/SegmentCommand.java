
package sc.fiji.labkit.cli;

import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import io.scif.services.DatasetIOService;
import sc.fiji.labkit.cli.dilation.FastDilation;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.io.location.BytesLocation;
import org.scijava.util.ByteArray;
import picocli.CommandLine;
import sc.fiji.labkit.ui.plugin.SegmentImageWithLabkitPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "segment",
		description = "Segment an image using a given classifier.")
public class SegmentCommand implements Callable<Optional<Integer>> {

	@CommandLine.Option(names = { "--image" }, required = true,
			description = "Image to be segmented.")
	private File imageFile;

	@CommandLine.Option(names = { "--classifier" }, required = true,
			description = "Classifier that was trained using the FIJI Labkit plugin.")
	private File classifier;

	@CommandLine.Option(names = { "--output" }, required = true,
			description = "File to store the output image.")
	private File outputFile;

	@CommandLine.Option(names = { "--use-gpu" })
	private boolean useGpu = false;

	@CommandLine.Option(names = { "--connected-components"},
			description = "(experimental) Will perform a connected component analysis on the segmentation. " +
				"Each connected component will be written with an individual id as pixel value.")
	private boolean connectedComponents = false;

	@CommandLine.Option(names = { "--dilation" },
			description = "(experimental) Performs a dilation with the given radius on the output image.")
	private Integer dilationRadius = null;

	@Override
	public Optional<Integer> call() throws Exception {
		Context context = new Context();
		// services
		DatasetIOService datasetIoService = context.service(DatasetIOService.class);
		CommandService commandService = context.service(CommandService.class);
		System.out.println("open image");
		Dataset input = datasetIoService.open(imageFile.getAbsolutePath());
		System.out.println("segment");
		RandomAccessibleInterval<? extends IntegerType<?>> segmentation = Cast.unchecked(commandService.run(
				SegmentImageWithLabkitPlugin.class, true, "input", input,
				"segmenter_file", classifier.getAbsolutePath(), "use_gpu", useGpu)
				.get().getOutput("output"));
		input = null;
		if(connectedComponents) {
			System.out.println("connected component analysis");
			segmentation = runConnectedComponents(Cast.unchecked(segmentation));
		}
		if(dilationRadius != null) {
			System.out.println("dilation");
			segmentation = dilation(dilationRadius, Cast.unchecked(segmentation));
		}
		System.out.println("write output");
		writeImage(context, Cast.unchecked(segmentation));
		System.out.println("done");
		return Optional.of(0);
	}

	private RandomAccessibleInterval<UnsignedShortType> runConnectedComponents(RandomAccessibleInterval<? extends IntegerType<?>> segmentation) {
		RandomAccessibleInterval<UnsignedShortType> result = ArrayImgs.unsignedShorts(Intervals.dimensionsAsLongArray(segmentation));
		int nThreads = Runtime.getRuntime().availableProcessors();
		ConnectedComponents.labelAllConnectedComponents(Cast.unchecked(segmentation), result,
				ConnectedComponents.StructuringElement.FOUR_CONNECTED, Executors.newFixedThreadPool(nThreads));
		return result;
	}

	private <T extends IntegerType<T>> RandomAccessibleInterval<T> dilation(int radius, RandomAccessibleInterval<T> segmentation) {
		if(radius <= 0)
			return segmentation;
		RandomAccessibleInterval<T> tmp = null;
		for(int i = 0; i < radius / 2; i++) {
			tmp = FastDilation.dilate(new DiamondShape(1), segmentation, tmp);
			segmentation = FastDilation.dilate(new RectangleShape(1, true), tmp, segmentation);
		}
		if(radius % 2 == 0)
			return segmentation;
		else {
			FastDilation.dilate(new DiamondShape(1), segmentation, tmp);
			return tmp;
		}
	}

	private void writeImage(Context context, RandomAccessibleInterval<UnsignedShortType> segmentation) throws IOException {
		try(FileOutputStream os = new FileOutputStream(outputFile)) {
			ByteArray bytes = new ByteArray();
			SCIFIOConfig config = new SCIFIOConfig();
			config.writerSetCompression("LZW");
			config.writerSetSequential(true);
			config.writerSetFailIfOverwriting(false);
			new ImgSaver(context).saveImg(new BytesLocation(bytes, outputFile.getName()), ImgView.wrap(segmentation), config);
			os.write(bytes.getArray(), 0, bytes.size());
		}
	}

}
