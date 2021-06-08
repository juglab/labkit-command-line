package labkit_cluster.command;

import ij.ImagePlus;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SegmentCommandTest {
	@Test
	public void test() throws IOException {
		File output = File.createTempFile("segmentation", ".tif");
		output.delete();
		assertFalse(output.exists());
		LabkitCommandTest.runCommandLine("segment", "--image", TestData.blobs, "--classifier", TestData.blobsClassifier,
				"--output", output.getAbsolutePath(), "--dilation", "2", "--connected-components");
		//showImage(output);
		assertTrue(output.exists());
	}

	private void showImage(File output) {
		ImagePlus imagePlus = new ImagePlus(output.getAbsolutePath());
		imagePlus.show();
		while(imagePlus.isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
