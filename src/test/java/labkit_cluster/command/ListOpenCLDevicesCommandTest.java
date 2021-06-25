package labkit_cluster.command;

import org.junit.Test;

public class ListOpenCLDevicesCommandTest {

	@Test
	public void test() {
		LabkitCommandTest.runCommandLine("list-opencl-devices");
	}
}
