
package labkit_cluster.command;

import bdv.util.BdvFunctions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This class defines the "show" sub command.
 * <p>
 * Uses BigDataViewer to show the content of the given N5 dataset.
 */
@CommandLine.Command(name = "show",
	description = "Show the segmentation stored in the N5 folder using Big Data Viewer.")
public class ShowCommand implements Callable<Optional<Integer>> {

	@CommandLine.Option(names = { "--n5" },
		description = "N5 folder that was used to store the segmentation.",
		required = true)
	private File n5;

	@Override
	public Optional<Integer> call() throws Exception {
		N5FSReader reader = new N5FSReader(n5.getAbsolutePath());
		RandomAccessibleInterval<UnsignedByteType> result = N5Utils.open(reader,
			PrepareCommand.N5_DATASET_NAME, new UnsignedByteType());
		BdvFunctions.show(result, "N5").setDisplayRange(0, 5);
		return Optional.empty(); // No exit code, because System exit should not be
		// called
	}
}
