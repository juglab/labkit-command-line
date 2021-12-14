package sc.fiji.labkit.cli;

import org.scijava.Context;
import org.scijava.ui.UIService;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "show-ui",
		description = "Show a basic Fiji user interface with Labkit installed.")
public class ShowUICommand implements Callable<Optional<Integer>> {


	@Override
	public Optional<Integer> call()  {
		new Context().service(UIService.class).showUI();
		return Optional.empty();
	}
}
