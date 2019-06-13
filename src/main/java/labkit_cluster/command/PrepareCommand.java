
package labkit_cluster.command;

import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.utils.LabkitUtils;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
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
@CommandLine.Command(name = "prepare", description = "Creates and initializes the N5 folder, that is required to run the segmentation.")
public class PrepareCommand implements Callable<Optional<Integer>> {

	static final String N5_DATASET_NAME = "segmentation";

	@CommandLine.Option(names = {"--image", "-I"}, required = true, description = "Image to be segmented.")
	private File imageXml;

	@CommandLine.Option(names = {"--n5", "-N"}, required = true, description = "N5 folder that will be created.")
	private File n5;

	@Override
	public Optional<Integer> call() throws Exception {
		SpimDataInputImage image = new SpimDataInputImage(imageXml.getAbsolutePath(), 0);
		CellGrid grid = LabkitUtils.suggestGrid(image.interval(), image.isTimeSeries());
		N5Writer writer = new N5FSWriter(n5.getAbsolutePath());
		int[] cellDimensions = new int[grid.numDimensions()];
		grid.cellDimensions(cellDimensions);
		writer.createDataset(N5_DATASET_NAME, grid.getImgDimensions(), cellDimensions, DataType.UINT8,
				new GzipCompression());
		return Optional.of(0); // exit code
	}
}
