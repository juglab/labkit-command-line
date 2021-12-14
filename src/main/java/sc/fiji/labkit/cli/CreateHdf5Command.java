
package sc.fiji.labkit.cli;

import bdv.export.ProgressWriterConsole;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import picocli.CommandLine;
import sc.fiji.labkit.ui.utils.HDF5Saver;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This class defines the "create-hdf5" sub command.
 * <p>
 * Opens the given N5 dataset and saves it as BDV HDF5.
 */
@CommandLine.Command(name = "create-hdf5",
	description = "Save the segmentation stored in the N5 folder as Big Data Viewer compatible HDF5.")
public class CreateHdf5Command implements Callable<Optional<Integer>> {

	@CommandLine.Option(names = { "-N", "--n5" },
		description = "N5 folder that contains the intermediate results.",
		required = true)
	private File n5;

	@CommandLine.Option(names = { "-X", "--xml" },
		description = "Location to store the XML and HDF5 files, that can be opened with BigDataViewer.",
		required = true)
	private File xml;

	@Override
	public Optional<Integer> call() throws Exception {
		N5FSReader reader = new N5FSReader(n5.getAbsolutePath());
		RandomAccessibleInterval<UnsignedByteType> result = N5Utils.open(reader,
			PrepareCommand.N5_DATASET_NAME, new UnsignedByteType());
		HDF5Saver saver = new HDF5Saver(result, xml.getAbsolutePath());
		saver.setProgressWriter(new ProgressWriterConsole());
		saver.writeAll();
		return Optional.of(0); // exit code
	}
}
