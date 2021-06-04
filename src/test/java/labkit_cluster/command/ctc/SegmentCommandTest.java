package labkit_cluster.command.ctc;

import labkit_cluster.command.LabkitCommand;

public class SegmentCommandTest {
	public static void main(String... args) {
		LabkitCommand.parseAndExecuteCommandLine("segment",
				"--classifier", "/home/arzt/Datasets/CTC/Fluo-N3DL-TRIF/01/version1.classifier",
				"--image", "/home/arzt/Datasets/CTC/Fluo-N3DL-TRIF/01/t001.tif",
				"--output", "/home/arzt/Datasets/CTC/Fluo-N3DL-TRIF/01_RES/tmp001.tif",
				"--use-gpu");
	}
}
