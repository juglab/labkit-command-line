package sc.fiji.labkit.cli;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.test.ImgLib2Assert;
import org.junit.Test;
import org.scijava.Context;
import sc.fiji.labkit.pixel_classification.utils.SingletonContext;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class SegmentCommandTest {

	@Test
	public void test() throws IOException
	{
		File output = File.createTempFile("segmentation", ".tif");
		LabkitCommandTest.runCommandLine("segment", "--image", TestData.blobs, "--classifier", TestData.blobsClassifier,
				"--output", output.getAbsolutePath());
		assertImageFilesEqual( TestData.blobsSegmentation, output.getAbsolutePath() );
	}

	@Test
	public void testDilationAndConnectedComponents() throws IOException
	{
		File output = File.createTempFile("segmentation", ".tif");
		LabkitCommandTest.runCommandLine("segment", "--image", TestData.blobs, "--classifier", TestData.blobsClassifier,
				"--output", output.getAbsolutePath(), "--dilation", "2", "--connected-components");
		assertImageFilesEqual( TestData.blobsResult, output.getAbsolutePath() );
	}

	private void assertImageFilesEqual( String expectedFile, String actualFile ) throws IOException
	{
		Context context = SingletonContext.getInstance();
		DatasetIOService io = context.service( DatasetIOService.class );
		Dataset expected = io.open( expectedFile );
		Dataset actual = io.open( actualFile );
		// We only test the images to be nearly identical, there is a bug in
		// SCIFIO, that changes a few pixel on the border of the image when
		// saving them with LZW compression. That's why we allow 10 pixels to
		// differ in the images.
		// See:
		ImgLib2Assert.assertIntervalEquals( expected, actual);
		long[] counter = {0};
		LoopBuilder.setImages(expected, actual).forEachPixel( (e, a) -> {
			if(e.getRealDouble() != a.getRealDouble())
				counter[0]++;
		} );
		assertTrue(counter[0] < 10);
	}
}
