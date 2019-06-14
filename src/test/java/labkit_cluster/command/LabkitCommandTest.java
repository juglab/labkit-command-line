
package labkit_cluster.command;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabkitCommandTest {

	private static final String imageXml = LabkitCommandTest.class.getResource("/small-t1-head/input.xml").getPath();

	private static final String classifier = LabkitCommandTest.class
			.getResource("/small-t1-head/small-t1-head.classifier").getPath();

	private static final String n5 = LabkitCommandTest.class.getResource("/small-t1-head/segmentation.n5").getPath();

	@Test
	public void testPrepare() throws IOException {
		Path output = createOutputN5();
		List<String> files = Arrays.asList(output.toFile().list());
		assertTrue(files.contains("attributes.json"));
		assertTrue(files.contains("segmentation"));
	}

	private static Path createOutputN5() throws IOException {
		Path path = Files.createTempDirectory("test-dataset");
		assertExitCodeZero( LabkitCommand.parseAndExecuteCommandLine("prepare", "--image", imageXml, "--n5",
				path.toString()));
		return path;
	}

	@Test
	public void testSegmentRange() throws IOException {
		Path output = createOutputN5();
		assertExitCodeZero( LabkitCommand.parseAndExecuteCommandLine("segment-chunk", "--image", imageXml,
				"--classifier", classifier, "--n5", output.toString(), "--chunks", "2", "--index", "0"));
		assertExitCodeZero( LabkitCommand.parseAndExecuteCommandLine("segment-chunk", "--image", imageXml,
				"--classifier", classifier, "--n5", output.toString(), "--chunks", "2", "--index", "1"));
		assertTrue(output.resolve(PrepareCommand.N5_DATASET_NAME).resolve("0/0/0").toFile().exists());
	}

	@Test
	public void testSaveHdf5() throws IOException {
		File file = File.createTempFile("test-data", ".xml");
		assertTrue(file.delete());
		assertExitCodeZero( LabkitCommand.parseAndExecuteCommandLine("create-hdf5", "--n5", n5, "--xml",
				file.getAbsolutePath()));
		assertTrue(file.exists());
	}

	private static void assertExitCodeZero(Optional<Integer> exitCode) {
		assertEquals(Optional.of(0), exitCode);
	}

	public static void main(String... args) {
		LabkitCommand.main("show", "--n5", n5);
	}

}
