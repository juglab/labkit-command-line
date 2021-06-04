
package labkit_cluster.command;

import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import io.scif.services.DatasetIOService;
import labkit_cluster.command.ctc.FastDilation;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labkit.plugin.SegmentImageWithLabkitPlugin;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.io.location.BytesLocation;
import org.scijava.util.ByteArray;
import picocli.CommandLine;

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
	private boolean use_gpu = false;

	@Override
	public Optional<Integer> call() throws Exception {
		Context context = new Context();
		// services
		DatasetIOService datasetIoService = context.service(DatasetIOService.class);
		CommandService commandService = context.service(CommandService.class);
		System.out.println("open image");
		Dataset input = datasetIoService.open(imageFile.getAbsolutePath());
		System.out.println("segment");
		RandomAccessibleInterval<UnsignedShortType> segmentation = Cast.unchecked(commandService.run(
				SegmentImageWithLabkitPlugin.class, true, "input", input,
				"segmenter_file", classifier.getAbsolutePath(), "use_gpu", use_gpu)
				.get().getOutput("output"));
		input = null;
		System.out.println("connected component analysis");
		Img<UnsignedShortType> cca = ArrayImgs.unsignedShorts(Intervals.dimensionsAsLongArray(segmentation));
		ConnectedComponents.labelAllConnectedComponents(segmentation, cca,
				ConnectedComponents.StructuringElement.FOUR_CONNECTED, Executors.newFixedThreadPool(8));
		segmentation = null;
		System.out.println("dilation");
		Img<UnsignedShortType> tmp = ArrayImgs.unsignedShorts(Intervals.dimensionsAsLongArray(cca));
		System.out.println("step 1");
		FastDilation.dilate(new DiamondShape(1), cca, tmp);
		System.out.println("step 2");
		FastDilation.dilate(new RectangleShape(1, true), tmp, cca);
		System.out.println("step 3");
		FastDilation.dilate(new DiamondShape(1), cca, tmp);
		System.out.println("write output");
		writeImage(context, tmp);
		System.out.println("done");
		return Optional.of(0);
	}

	private void writeImage(Context context, Img<UnsignedShortType> tmp) throws IOException {
		try(FileOutputStream os = new FileOutputStream(outputFile)) {
			ByteArray bytes = new ByteArray();
			SCIFIOConfig config = new SCIFIOConfig();
			config.writerSetCompression("LZW");
			config.writerSetSequential(true);
			config.writerSetFailIfOverwriting(false);
			new ImgSaver(context).saveImg(new BytesLocation(bytes, outputFile.getName()), tmp, config);
			os.write(bytes.getArray(), 0, bytes.size());
		}
	}

}
