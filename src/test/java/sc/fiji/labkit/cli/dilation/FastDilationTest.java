package sc.fiji.labkit.cli.dilation;

import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.junit.Test;

public class FastDilationTest {

	@Test
	public void testDiamond() {
		Img<UnsignedShortType> image = ArrayImgs.unsignedShorts(
				new short[] { 0, 0, 0, 0, 1, 0, 0, 0, 0 }, 3, 3);
		FastDilation.dilate(new DiamondShape(1), image, image);
		Img<UnsignedShortType> expected = ArrayImgs.unsignedShorts(
				new short[] { 0, 1, 0, 1, 1, 1, 0, 1, 0	}, 3, 3);
		ImgLib2Assert.assertImageEquals(expected, image);
	}

	@Test
	public void testRectangle() {
		Img<UnsignedShortType> image = ArrayImgs.unsignedShorts(5, 5);
		image.randomAccess().setPositionAndGet(2, 2).set(1);
		FastDilation.dilate(new RectangleShape(1, true), image, image);
		Img<UnsignedShortType> expected = ArrayImgs.unsignedShorts(
				new short[] {
						0, 0, 0, 0, 0,
						0, 1, 1, 1, 0,
						0, 1, 1, 1, 0,
						0, 1, 1, 1, 0,
						0, 0, 0, 0, 0,
				},
				5, 5
		);
		ImgLib2Assert.assertImageEquals(expected, image);
	}
}
