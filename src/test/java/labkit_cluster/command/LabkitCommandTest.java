
package labkit_cluster.command;

import io.scif.SCIFIO;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.DefaultDataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LabkitCommandTest {

	private static final String imageXml = getPath("/small-t1-head/input.xml");

	private static final String classifier = getPath(
		"/small-t1-head/small-t1-head.classifier");

	private static final String n5 = getPath("/small-t1-head/segmentation.n5");

	@Test
	public void testPrepare() throws IOException {
		Path tmpN5 = prepare();
		List<String> files = Arrays.asList(tmpN5.toFile().list());
		assertTrue(files.contains("attributes.json"));
		assertTrue(files.contains("segmentation"));
	}

	private static Path prepare() throws IOException {
		Path tmpN5 = Files.createTempDirectory("test-dataset");
		runCommandLine("prepare", "--image", imageXml, "--n5", tmpN5.toString());
		return tmpN5;
	}

	@Test
	public void testSegmentRange() throws IOException {
		Path tmpN5 = prepare();
		runCommandLine("segment-chunk", "--image", imageXml, "--classifier",
			classifier, "--n5", tmpN5.toString(), "--chunks", "2", "--index", "0");
		runCommandLine("segment-chunk", "--image", imageXml, "--classifier",
			classifier, "--n5", tmpN5.toString(), "--chunks", "2", "--index", "1");
		assertTrue(tmpN5.resolve(PrepareCommand.N5_DATASET_NAME).resolve("0/0/0")
			.toFile().exists());
	}

	@Test
	public void testSaveHdf5() throws IOException {
		File file = File.createTempFile("test-data", ".xml");
		assertTrue(file.delete());
		runCommandLine("create-hdf5", "--n5", n5, "--xml", file.getAbsolutePath());
		assertTrue(file.exists());
	}

	@Test
	public void testSavePartitionedHdf5() throws IOException {
		File directory = Files.createTempDirectory("test-partitioned-hdf5")
			.toFile();
		File xml = new File(directory, "test.xml");
		runCommandLine("create-partitioned-hdf5", "--n5", n5, "--xml", xml
			.getAbsolutePath());
		assertTrue(xml.exists());
		assertTrue(new File(directory, "test.h5").exists());
		assertTrue(new File(directory, "test-00-00.h5").exists());
	}

	@Test
	public void testSavePartitionedHdf5Partition() throws IOException {
		File directory = Files.createTempDirectory("test-partitioned-hdf5")
			.toFile();
		File xml = new File(directory, "test.xml");
		runCommandLine("create-partitioned-hdf5", "--n5", n5, "--xml", xml
			.getAbsolutePath(), "--partition", "0");
		assertFalse(xml.exists());
		assertFalse(new File(directory, "test.h5").exists());
		assertTrue(new File(directory, "test-00-00.h5").exists());
	}

	@Test
	public void testSavePartitionedHdf5Header() throws IOException {
		File directory = Files.createTempDirectory("test-partitioned-hdf5")
			.toFile();
		File xml = new File(directory, "test.xml");
		runCommandLine("create-partitioned-hdf5", "--n5", n5, "--xml", xml
			.getAbsolutePath(), "--header");
		assertTrue(xml.exists());
		assertTrue(new File(directory, "test.h5").exists());
		assertFalse(new File(directory, "test-00-00.h5").exists());
	}

	@Test
	public void testSavePartitionedHdf5Number() throws IOException {
		File directory = Files.createTempDirectory("test-partitioned-hdf5")
			.toFile();
		File xml = new File(directory, "test.xml");
		runCommandLine("create-partitioned-hdf5", "--n5", n5, "--xml", xml
			.getAbsolutePath(), "--number-of-partitions");
		assertFalse(xml.exists());
		assertFalse(new File(directory, "test.h5").exists());
		assertFalse(new File(directory, "test-00-00.h5").exists());
	}

	public static void main(String... args) {
		LabkitCommand.main("show", "--n5", n5);
	}

	private static String getPath(String file) {
		return LabkitCommandTest.class.getResource(file).getPath();
	}

	private static void runCommandLine(String... args) {
		Optional<Integer> exitCode = LabkitCommand.parseAndExecuteCommandLine(args);
		assertEquals(Optional.of(0), exitCode);
	}

	@Test
	public void test2d() throws IOException {
		testDataset("2d");
	}

	@Test
	public void test3d() throws IOException {
		testDataset("3d");
	}

	@Test
	public void test5d() throws IOException {
		testDataset("5d");
	}

	@Test
	public void test2dPlusTime() throws IOException {
		testDataset("2d_time");
	}

	@Test
	public void test3dPlusTime() throws IOException {
		testDataset("3d_time");
	}

	private void testDataset(String folder) throws IOException {
		String imageXml = getPath("/" + folder + "/export.xml");
		String classifier = getPath("/" + folder + "/test.classifier");
		Path tmpN5 = Files.createTempDirectory("test-n5");
		Path tmpHDF5 = Files.createTempFile("test-", ".xml");
		runCommandLine("prepare", "--image", imageXml, "--n5", tmpN5.toString());
		runCommandLine("segment-chunk", "--image", imageXml, "--classifier",
			classifier, "--n5", tmpN5.toString(), "--chunks", "1", "--index", "0");
		runCommandLine("create-hdf5", "--n5", tmpN5.toString(), "--xml", tmpHDF5
			.toString());
	}
}
