package labkit_cluster.command;

import net.imglib2.trainable_segmentation.gpu.api.GpuPool;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class GpuTest {

	@Test
	public void testSegmentationOnGpu() throws IOException {
		assumeTrue("No OpenCL device available.", GpuPool.isGpuAvailable());
		Path tmpN5 = Files.createTempDirectory("test-dataset");
		run("prepare", "--image", TestData.imageXml,
				"--classifier", TestData.classifier,
				"--n5", tmpN5.toString(),
				"--use-gpu");
		run("segment-chunk", "--image", TestData.imageXml,
				"--classifier", TestData.classifier,
				"--n5", tmpN5.toString(),
				"--chunks", "2",
				"--index", "0",
				"--use-gpu");
	}

	private void run(String... args) {
		Optional< Integer > exitCode = LabkitCommand.parseAndExecuteCommandLine(args);
		assertEquals(Optional.of(0), exitCode);
	}
}
