
package sc.fiji.labkit.cli.dilation;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Localizables;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class FastDilation {

	public static <T extends IntegerType<T>> RandomAccessibleInterval<T> dilate(Shape shape, RandomAccessibleInterval<T> image, RandomAccessibleInterval<T> output) {
		if (image == output) {
			RandomAccessibleInterval<T> tmp = createSameSizeImage(image);
			copyFromTo(image, tmp);
			dilateDiamondShapeStep2(shape, tmp, image);
		}
		else {
			if (output == null)
				output = createSameSizeImage(image);
			copyFromTo(image, output);
			dilateDiamondShapeStep2(shape, image, output);
		}
		return output;
	}

	@SuppressWarnings("unchecked")
	private static <T extends IntegerType<T>> RandomAccessibleInterval<T> createSameSizeImage(RandomAccessibleInterval<T> image) {
		T type = Util.getTypeFromInterval(image);
		ArrayImgFactory arrayImgFactory = new ArrayImgFactory((NativeType) type);
		RandomAccessibleInterval<T> tmp = arrayImgFactory.create(Intervals.dimensionsAsLongArray(image));
		return tmp;
	}

	private static <T extends IntegerType<T>> void copyFromTo(RandomAccessibleInterval<T> image, RandomAccessibleInterval<T> output) {
		LoopBuilder.setImages(image, output).multiThreaded().forEachPixel((i, o) -> o.set(i));
	}

	private static <T extends IntegerType<T>> void dilateDiamondShapeStep2(Shape shape, RandomAccessibleInterval<T> input,
			RandomAccessibleInterval<T> output)
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
}
