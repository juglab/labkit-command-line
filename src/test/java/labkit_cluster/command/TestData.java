package labkit_cluster.command;

class TestData {

	static final String imageXml = getPath("/small-t1-head/input.xml");
	static final String classifier = getPath( "/small-t1-head/small-t1-head.classifier");
	static final String n5 = getPath("/small-t1-head/segmentation.n5");

	static final String blobs = getPath("/blobs/blobs.tif");
	static final String blobsClassifier = getPath("/blobs/blobs.classifier");

	static String getPath(String file) {
		return TestData.class.getResource(file).getPath();
	}
}
