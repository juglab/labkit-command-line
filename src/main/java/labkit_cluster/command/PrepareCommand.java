
package labkit_cluster.command;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.ImgPlusViewsOld;
import net.imglib2.labkit.inputimage.SpimDataToImgPlus;
import net.imglib2.labkit.segmentation.Segmenter;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.scijava.Context;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This class defines the "prepare" sub command.
 * <p>
 * It creates a N5 dataset, with the same number of pixels as the input image.
 * This N5 dataset can later be used to store the result of the segmentation.
 */
@CommandLine.Command(name = "prepare",
	description = "Creates and initializes the N5 folder, that is required to run the segmentation.")
public class PrepareCommand implements Callable<Optional<Integer>> {

	static final String N5_DATASET_NAME = "segmentation";

	@CommandLine.Option(names = { "--image", "-I" }, required = true,
		description = "Image to be segmented.")
	private File imageXml;

	@CommandLine.Option(names = { "--classifier" }, required = true,
			description = "Classifier that was trained using the FIJI Labkit plugin.")
	private File classifier;

	@CommandLine.Option(names = { "--n5", "-N" }, required = true,
		description = "N5 folder that will be created.")
	private File n5;

	@Override
	public Optional<Integer> call() throws Exception {
		ImgPlus< ? > image = SpimDataToImgPlus.open(imageXml
			.getAbsolutePath(), 0);
		Segmenter segmenter = openSegmenter( classifier.getAbsolutePath() );
		int[] cellDimensions = segmenter.suggestCellSize(image);
		long[] imageDimensions = imageDimensionsWithoutChannelAxis(image);
		N5Writer writer = new N5FSWriter(n5.getAbsolutePath());
		writer.createDataset(N5_DATASET_NAME, imageDimensions,
			cellDimensions, DataType.UINT8, new GzipCompression());
		return Optional.of(0); // exit code
	}

	private long[] imageDimensionsWithoutChannelAxis(ImgPlus< ? > image) {
		if (ImgPlusViewsOld.hasAxis(image, Axes.CHANNEL))
			image = ImgPlusViewsOld.hyperSlice(image, Axes.CHANNEL, 0);
		return Intervals.dimensionsAsLongArray(image);
	}

	private static TrainableSegmentationSegmenter openSegmenter( String classifier )
	{
		TrainableSegmentationSegmenter segmenter =
				new TrainableSegmentationSegmenter(new Context());
		segmenter.openModel(classifier);
		return segmenter;
	}
}
