package labkit_cluster.command.ctc;

import io.scif.img.ImgOpener;
import io.scif.img.ImgSaver;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.trainable_segmentation.utils.SingletonContext;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "print-seg", description = "WIP, print the seg score for my dataset.")
public class FluoTrifSeg implements Callable<Optional<Integer>> {

	private static final ImgOpener opener = new ImgOpener();

	@CommandLine.Option(names = { "-X", "--xml" },
			description = "BDV xml of the segmentation.",
			required = true)
	private File xml;

	@CommandLine.Option(names = { "--ctc" },
			description = "Directory that contains the CTC ground truth TIFs.",
			required = true)
	private File ctcDataset;

	@Override
	public Optional<Integer> call() {
		SpimDataInputImage image = new SpimDataInputImage(xml.toString(), 0);
		double segScore = getSegScore(image, ctcDataset.toPath());
		System.out.println(segScore);
		return Optional.of(0);
	}

	private static double getSegScore(SpimDataInputImage image, Path ctcDataset) {
		try {
			SEG seg = new SEG();
			Files.list(ctcDataset).forEach(file -> measureFrame(seg, image, file));
			return seg.getScore();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static ValuePair<Integer, Integer> extractFrameAndSliceFromFilename(Path path) {
		try {
			Pattern pattern = Pattern.compile("man_seg_(\\d\\d\\d)_(\\d\\d\\d)\\.tif");
			Matcher mr = pattern.matcher(path.getFileName().toString());
			if (mr.find())
				return new ValuePair<>(Integer.parseInt(mr.group(1)), Integer.parseInt(mr.group(2)));
		}
		catch (NumberFormatException e) { }
		return null;
	}

	private static void measureFrame(SEG seg, SpimDataInputImage image, Path groundTruthFileName) {
		ValuePair<Integer, Integer> indices = extractFrameAndSliceFromFilename(groundTruthFileName);
		if(indices == null)
			return;
		Img<? extends IntegerType<?>> groundTruth = openGroundTruth(groundTruthFileName);
		RandomAccessibleInterval<IntType> instanceSegmentation2 = openSegmentation(image, indices.getA(), indices.getB());
		//new ImgSaver(SingletonContext.getInstance()).saveImg(groundTruthFileName.toString() + "2.tif", ImgView.wrap(instanceSegmentation2));
		seg.addFrame(groundTruth, instanceSegmentation2);
	}

	private static RandomAccessibleInterval<IntType> openSegmentation(SpimDataInputImage image, int frameIndex, int sliceIndex) {
		int segmentation_frame = getCorrespondingSegmentationFrame(frameIndex);
		RandomAccessibleInterval<? extends IntegerType<?>> frame = Cast.unchecked(Views.hyperSlice(image.imageForSegmentation(), 3, segmentation_frame));
		RandomAccessibleInterval<? extends IntegerType<?>> actualSlice = cropVolume(frame, sliceIndex);
		Img<IntType> instanceSegmentation = connectedComponents(actualSlice);
		Img<IntType> dilate = dilateComponents(instanceSegmentation);
		return Views.hyperSlice(dilate, 2, 4);
	}

	private static RandomAccessibleInterval<? extends IntegerType<?>> cropVolume(RandomAccessibleInterval<? extends IntegerType<?>> frame, int sliceIndex) {
		long width = frame.dimension(0);
		long height = frame.dimension(1);
		return Views.zeroMin(Views.interval(frame, new long[]{0,0, sliceIndex -4}, new long[]{width -1, height-1, sliceIndex +4}));
	}

	private static Img<IntType> dilateComponents(Img<IntType> instanceSegmentation) {
//		List<Shape> structuringElements = Arrays.asList( // dataset part & version1.classifier & 4 diamond -> 0.8360
//				new DiamondShape(1),
//				new DiamondShape(1),
//				new DiamondShape(1),
//				new DiamondShape(1));
		List<Shape> structuringElements = Arrays.asList( // dataset part & version1.classifier & 2 diamond + rec -> 0.9979
				new DiamondShape(1),
				new DiamondShape(1),
				new RectangleShape(1, false));
		return Dilation.dilate(instanceSegmentation, structuringElements, 8);
	}

	private static Img<? extends IntegerType<?>> openGroundTruth(Path groundTruthFileName) {
		return Cast.unchecked(opener.openImgs(groundTruthFileName.toString()).get(0).getImg());
	}

	private static int getCorrespondingSegmentationFrame(int gt_frame) {
		return gt_frame;
//		switch (gt_frame) {
//			case 0 : return 0;
//			case 1 : return 1;
//			case 10: return 2;
//			case 20: return 3;
//			case 30: return 4;
//			case 50: return 6;
//		}
//		throw new UnsupportedOperationException("Frame not expected.");
	}

	private static Img<IntType> connectedComponents(RandomAccessibleInterval<? extends IntegerType<?>> actualSlice) {
		Img<IntType> instanceSegmentation = ArrayImgs.ints(Intervals.dimensionsAsLongArray(actualSlice));
		ConnectedComponents.labelAllConnectedComponents(Cast.unchecked(actualSlice), instanceSegmentation, ConnectedComponents.StructuringElement.FOUR_CONNECTED);
		return instanceSegmentation;
	}
}
