
package sc.fiji.labkit.cli;

import bdv.export.ProgressWriterConsole;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import picocli.CommandLine;
import sc.fiji.labkit.ui.utils.HDF5Saver;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This class defines the "create-hdf5" sub command.
 * <p>
 * Opens the given N5 dataset and saves it as BDV HDF5.
 */
@CommandLine.Command(name = "create-partitioned-hdf5",
	description = "Save the segmentation stored in the N5 folder as multiple Big Data Viewer compatible HDF5.")
public class CreatePartitionedHdf5Command implements
	Callable<Optional<Integer>>
{

	@CommandLine.Option(names = { "-N", "--n5" },
		description = "N5 folder that contains the intermediate results.",
		required = true)
	private File n5;

	@CommandLine.Option(names = { "-X", "--xml" },
		description = "Location to store the XML and HDF5 files, that can be opened with BigDataViewer.",
		required = true)
	private File xml;

	@CommandLine.Option(names = { "--header" },
		description = "Only write XML & index HDF5.")
	private boolean onlyHeader;

	@CommandLine.Option(names = { "--partition" },
		description = "Only write the partition with the given index. The index must be between 0 and number of partitions minus 1.")
	private Integer partitionIndex;

	@CommandLine.Option(names = { "--number-of-partitions" },
		description = "Only print the number of partitions.")
	private boolean isPrintNumberOfPartitions;

	@Override
	public Optional<Integer> call() throws Exception {
		RandomAccessibleInterval<UnsignedByteType> n5Image = openN5();
		createOutputDirectory();
		runTask(n5Image);
		return Optional.of(0); // exit code
	}

	private RandomAccessibleInterval<UnsignedByteType> openN5() throws IOException {
		N5FSReader reader = new N5FSReader(n5.getAbsolutePath());
		return N5Utils.open(reader,
				PrepareCommand.N5_DATASET_NAME, new UnsignedByteType());
	}

	private void createOutputDirectory() {
		File directory = xml.getParentFile();
		if(directory != null) directory.mkdirs();
	}

	private void runTask(RandomAccessibleInterval<UnsignedByteType> n5Image) {
		HDF5Saver saver = new HDF5Saver(n5Image, xml.getAbsolutePath());
		saver.setProgressWriter(new ProgressWriterConsole());
		saver.setPartitions(1, 1);
		if (onlyHeader) saver.writeXmlAndHdf5();
		else if (isPrintNumberOfPartitions) System.out.println(saver
			.numberOfPartitions());
		else if (partitionIndex != null) saver.writePartition(partitionIndex);
		else saver.writeAll();
	}

}
