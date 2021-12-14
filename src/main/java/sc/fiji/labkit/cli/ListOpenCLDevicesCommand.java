package sc.fiji.labkit.cli;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.exceptions.OpenCLException;
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "list-opencl-devices")
public class ListOpenCLDevicesCommand implements Callable<Optional<Integer>> {

	@Override
	public Optional<Integer> call() throws Exception {
		try {
			List<String> names = CLIJ.getAvailableDeviceNames();
			names.forEach(System.out::println);
			return Optional.of(0);
		} catch (OpenCLException e) {
			if (e.getErrorCode() == -1001) {
				System.err.println("No OpenCL device available.");
				return Optional.of(1);
			}
			throw e;
		}
	}
}
