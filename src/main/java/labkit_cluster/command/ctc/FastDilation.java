
package labkit_cluster.command.ctc;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Localizables;
import net.imglib2.view.Views;

public class FastDilation {

	public static Img<UnsignedShortType> dilateDiamondShape(Img<UnsignedShortType> image,
		Img<UnsignedShortType> output)
	{
		return dilate(new DiamondShape(1), image, output);
	}

	public static Img<UnsignedShortType> dilate(Shape shape, Img<UnsignedShortType> image, Img<UnsignedShortType> output) {
		if (image == output) {
			Img<UnsignedShortType> tmp = ArrayImgs.unsignedShorts(Intervals.dimensionsAsLongArray(image));
			copyFromTo(image, tmp);
			dilateDiamondShapeStep2(shape, tmp, image);
		}
		else {
			if (output == null)
				output = ArrayImgs.unsignedShorts(Intervals.dimensionsAsLongArray(image));
			copyFromTo(image, output);
			dilateDiamondShapeStep2(shape, image, output);
		}
		return output;
	}

	private static void copyFromTo(Img<UnsignedShortType> image, Img<UnsignedShortType> output) {
		LoopBuilder.setImages(image, output).multiThreaded().forEachPixel((i, o) -> o.set(i));
	}

	private static void dilateDiamondShapeStep2(Shape shape, Img<UnsignedShortType> input,
			Img<UnsignedShortType> output)
	{
		int n = output.numDimensions();
		Neighborhood<Localizable> neighborhood = shape.neighborhoodsRandomAccessible(Localizables.randomAccessible(n)).randomAccess().setPositionAndGet(new long[n]);
		for(Localizable position : neighborhood) {
			long[] offset = Localizables.asLongArray(position);
			if(isZero(offset))
				continue;
			Interval dest = Intervals.intersect(output, Intervals.translate(input, offset));
			Interval source = Intervals.translateInverse(dest, offset);
			LoopBuilder.setImages(Views.interval(input, source), Views.interval(output, dest)).multiThreaded().forEachPixel((i, o) -> {
				if (i.compareTo(o) > 0)
					o.set(i);
			});
		}
	}

	private static boolean isZero(long[] array) {
		for(long value : array)
			if(value != 0)
				return false;
		return true;
	}

	private static <T> RandomAccessibleInterval<T> crop(RandomAccessibleInterval<T> image, int d,
		int i)
	{
		long[] min = Intervals.minAsLongArray(image);
		long[] max = Intervals.maxAsLongArray(image);
		if (i < 0)
			min[d] += 1;
		if (i > 0)
			max[d] -= 1;
		return Views.interval(image, min, max);
	}

	public static void main(String... args) {
	}
}
