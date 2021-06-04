package labkit_cluster.command.ctc;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import io.scif.img.ImgOpener;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.numeric.ARGBType;
import org.junit.Test;

public class Compare {

	public static void main(String... args) {
		ImgOpener opener = new ImgOpener();
		ImgPlus<?> image1 = opener.openImg("/home/arzt/tmp/ctc_upload/t000.tif");
		ImgPlus<?> image2 = opener.openImg("/home/arzt/tmp/ctc_upload/mask000.tif");
		BdvStackSource<?> s1 = BdvFunctions.show(image1, "t");
		s1.setColor(new ARGBType(0xffff0000));
		BdvStackSource<?> s2 = BdvFunctions.show(image2, "mask", BdvOptions.options().addTo(s1.getBdvHandle()));
		s2.setColor(new ARGBType(0xffff0000));
		long[] count = new long[1];
		LoopBuilder.setImages(image1, image2).forEachPixel((x, y) -> {
			if(!x.equals(y))
				count[0]++;
		});
		System.out.println(count[0]);
	}
}
