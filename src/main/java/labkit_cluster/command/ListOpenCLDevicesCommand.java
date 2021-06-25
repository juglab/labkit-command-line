package labkit_cluster.command;

import net.haesleinhuepf.clij.CLIJ;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "list-opencl-devices")
public class ListOpenCLDevicesCommand implements Callable<Optional<Integer>> {

	@Override
	public Optional<Integer> call() throws Exception {
		List<String> names = CLIJ.getAvailableDeviceNames();
		names.forEach(System.out::println);
		return Optional.of(0);
	}
}
