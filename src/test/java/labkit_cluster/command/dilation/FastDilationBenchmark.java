
package labkit_cluster.command.dilation;

import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(value = Mode.AverageTime)
public class FastDilationBenchmark {

	private final Img<UnsignedShortType> image = RandomImgs.seed(42).nextImage(
		new UnsignedShortType(), 1000, 100, 100);
	private final List<Shape> strels = Arrays.asList(new DiamondShape(1));

	@Benchmark
	public void benchmarkDilation() {
		Dilation.dilate(image, strels, 8);
	}

	@Benchmark
	public void benchmarkLoopBuilder() {
		FastDilation.dilate(new DiamondShape(1), image, image);
	}

	public static void main(String... args) throws RunnerException {
		Options options = new OptionsBuilder()
			.include(FastDilationBenchmark.class.getName())
			.build();
		new Runner(options).run();
	}
}
