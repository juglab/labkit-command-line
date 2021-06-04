
package labkit_cluster.command;

import labkit_cluster.command.ctc.FluoTrifSeg;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * This is the main class of this project. It uses the PicoCli command line
 * parser, to either show the usage or execute one of the sub commands:
 * {@link PrepareCommand}, {@link SegmentChunkCommand}, {@link CreateHdf5Command},
 * {@link ShowCommand}
 */
@CommandLine.Command(name = LabkitCommand.COMMAND_NAME, subcommands = {
	PrepareCommand.class, SegmentChunkCommand.class, ShowCommand.class,
	CreateHdf5Command.class, CreatePartitionedHdf5Command.class,
	FluoTrifSeg.class, SegmentCommand.class},
	description = "Labkit command line tool for the segmentation of large files.")
public class LabkitCommand implements Callable<Optional<Integer>> {

	static final String COMMAND_NAME = "java -jar labkit-command-line.jar";

	// method that is executed by PicoCli, if no sub command is given.
	@Override
	public Optional<Integer> call() throws Exception {
		showUsage();
		return Optional.of(1); // exit code
	}

	private void showUsage() {
		CommandLine.usage(new LabkitCommand(), System.err);
		showExample();
	}

	private void showExample() {
		System.err.println();
		System.err.println("Usage Example:");
		System.err.println();
		System.err.println(
			"  The following shell commands will segment the image \"input.xml\" using the classifier \"input.classifier\".");
		System.err.println(
			"  The image must be stored in Big Data Viewer format (HDF5 + XML).");
		System.err.println(
			"  The classifier should be trained and saved with the Labkit FIJI plugin.");
		System.err.println(
			"  The segmentation is performed in three chunks \"--chunks 3\".");
		System.err.println(
			"  It's possible to distribute the segmentation to a cluster,");
		System.err.println(
			"  by processing each chunk on a different cluster node.");
		System.err.println(
			"  In reality the number of chunks should correspond, to the number of cluster nodes used.");
		System.err.println();
		System.err.println("  Preparation:      " + COMMAND_NAME +
			" prepare --image input.xml --classifier input.classifier --n5 tmp.n5");
		showSegmentExample(0);
		showSegmentExample(1);
		showSegmentExample(2);
		System.err.println("  Store results:    " + COMMAND_NAME +
			" create-hdf5 --n5 tmp.n5 --xml output.xml");
	}

	private void showSegmentExample(int index) {
		System.err.println("  Chunk " + (index + 1) + ":          " + COMMAND_NAME +
			" segment-chunk --image input.xml --classifier input.classifier --n5 tmp.n5 --chunks 3 --index " +
			index);
	}

	public static void main(String... args) {
		try {
			Optional<Integer> exitCode = parseAndExecuteCommandLine(args);
			exitCode.ifPresent(System::exit);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(3);
		}
	}

	public static Optional<Integer> parseAndExecuteCommandLine(String... args) {
		List<Object> exitCodes = new CommandLine(new LabkitCommand())
			.parseWithHandlers(new CommandLine.RunLast(), CommandLine
				.defaultExceptionHandler().andExit(1), args);
		@SuppressWarnings("unchecked")
		Optional<Integer> exitCode = (Optional<Integer>) exitCodes.get(0);
		return exitCode;
	}
}
