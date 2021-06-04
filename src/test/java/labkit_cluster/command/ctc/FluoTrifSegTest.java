package labkit_cluster.command.ctc;

import labkit_cluster.command.LabkitCommand;

public class FluoTrifSegTest {
	public static void main(String... args) {
		LabkitCommand.parseAndExecuteCommandLine("print-seg", "--xml",
				"/home/arzt/Datasets/CTC/Fluo-N3DL-TRIF/01/segmentation.xml",
				"--ctc",
				"/home/arzt/Datasets/CTC/Fluo-N3DL-TRIF/01_GT/SEG");
	}
}
