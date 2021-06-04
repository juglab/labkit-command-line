package labkit_cluster.command.ctc;

import com.google.common.util.concurrent.AtomicDouble;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The SEG metrics computes the IoU (a.k.a. Jaccard index) metrics between
 * ground-truth labels and the prediction labels that have an overlap percentage
 * greater than 0.5. The final score is averaged over all ground-truth labels.
 * In the context of instance segmentation, the labels are the pixel values.
 * <p>
 * The metrics is computed over all dimensions, meaning that the score over a 3D
 * stack will be calculated by considering that all pixels in the 3D stack with
 * equal pixel value belong to the same label. For slice-wise scoring, run the
 * metrics on each slice individually.
 * <p>
 * Finally, pixels with value 0 are considered background and are ignored during
 * the metrics calculation. If both images are only background, then the metrics
 * score is 1.
 * <p>
 * Reference: Ulman, V., Maška, M., Magnusson, K. et al. An objective comparison
 * of cell-tracking algorithms. Nat Methods 14, 1141–1152 (2017).
 *
 * @author Joran Deschamps
 * @see <a href=
 *      "https://github.com/CellTrackingChallenge/CTC-FijiPlugins">Original
 *      implementation by Martin Maška and Vladimír Ulman</a>
 */
public class SEG {

	private AtomicDouble sumScore = new AtomicDouble(0);
	private AtomicInteger numberOfGroundTruthSegments = new AtomicInteger(0);

	/**
	 * Compute the accuracy score between labels of a predicted and of a
	 * ground-truth image.
	 *
	 * @param groundTruth Ground-truth image
	 * @param prediction Predicted image
	 * @return Metrics score
	 */
	public void addFrame(
		RandomAccessibleInterval<? extends IntegerType<?>> groundTruth,
		RandomAccessibleInterval<? extends IntegerType<?>> prediction)
	{

		if (!Arrays.equals(groundTruth.dimensionsAsLongArray(), prediction.dimensionsAsLongArray()))
			throw new IllegalArgumentException("Image dimensions must match.");

		final ConfusionMatrix confusionMatrix = new ConfusionMatrix(groundTruth, prediction);
		double[][] costMatrix = computeCostMatrix(confusionMatrix);
		addFrame(costMatrix);
	}

	public double getScore() {
		return sumScore.get() / numberOfGroundTruthSegments.get();
	}

	private static double[][] computeCostMatrix(ConfusionMatrix cM) {
		int M = cM.getNumberGroundTruthLabels();
		int N = cM.getNumberPredictionLabels();

		// empty cost matrix
		// make sure to obtain a rectangular matrix, with Npred > Ngt, in order
		// to avoid empty assignments if using Munkres-Kuhn
		double[][] costMatrix = new double[M][Math.max(M + 1, N)];

		// fill in cost matrix
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				costMatrix[i][j] = getLocalIoUScore(cM, i, j);
			}
		}

		return costMatrix;
	}

	private static double getLocalIoUScore(ConfusionMatrix cM, int i, int j) {
		double intersection = cM.getIntersection(i, j);
		double gtSize = cM.getGroundTruthLabelSize(i);

		double overlap = intersection / gtSize;
		if (overlap > 0.5) {
			double predSize = cM.getPredictionLabelSize(j);

			return intersection / (gtSize + predSize - intersection);
		}
		else {
			return 0.;
		}
	}

	private void addFrame(double[][] costMatrix) {
		if (costMatrix.length != 0 && costMatrix[0].length != 0) {
			final int M = costMatrix.length;
			final int N = costMatrix[0].length;
			double sum = 0;

			for (int i = 0; i < M; i++) {
				for (int j = 0; j < N; j++) {
					sum += costMatrix[i][j];
				}
			}

			sumScore.addAndGet(sum);
			numberOfGroundTruthSegments.addAndGet(M);
		}
	}
}
