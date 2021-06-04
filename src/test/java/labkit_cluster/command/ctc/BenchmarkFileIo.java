package labkit_cluster.command.ctc;

import ij.IJ;
import ij.ImagePlus;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import loci.formats.FormatException;
import loci.formats.meta.DummyMetadata;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.ome.OMEXMLMetadataImpl;
import loci.formats.out.TiffWriter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.trainable_segmentation.utils.SingletonContext;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.StopWatch;
import ome.units.quantity.ElectricPotential;
import ome.units.quantity.Frequency;
import ome.units.quantity.Length;
import ome.units.quantity.Power;
import ome.units.quantity.Pressure;
import ome.units.quantity.Temperature;
import ome.units.quantity.Time;
import ome.xml.meta.MetadataRoot;
import ome.xml.model.AffineTransform;
import ome.xml.model.MapPair;
import ome.xml.model.enums.AcquisitionMode;
import ome.xml.model.enums.ArcType;
import ome.xml.model.enums.Binning;
import ome.xml.model.enums.Compression;
import ome.xml.model.enums.ContrastMethod;
import ome.xml.model.enums.Correction;
import ome.xml.model.enums.DetectorType;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.ExperimentType;
import ome.xml.model.enums.FilamentType;
import ome.xml.model.enums.FillRule;
import ome.xml.model.enums.FilterType;
import ome.xml.model.enums.FontFamily;
import ome.xml.model.enums.FontStyle;
import ome.xml.model.enums.IlluminationType;
import ome.xml.model.enums.Immersion;
import ome.xml.model.enums.LaserMedium;
import ome.xml.model.enums.LaserType;
import ome.xml.model.enums.Marker;
import ome.xml.model.enums.Medium;
import ome.xml.model.enums.MicrobeamManipulationType;
import ome.xml.model.enums.MicroscopeType;
import ome.xml.model.enums.NamingConvention;
import ome.xml.model.enums.PixelType;
import ome.xml.model.enums.Pulse;
import ome.xml.model.primitives.Color;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.NonNegativeLong;
import ome.xml.model.primitives.PercentFraction;
import ome.xml.model.primitives.PositiveInteger;
import ome.xml.model.primitives.Timestamp;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.io.location.BytesLocation;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.io.nio.ByteBufferByteBank;
import org.scijava.util.ByteArray;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BenchmarkFileIo {

	Context context = SingletonContext.getInstance();
	Path file = Paths.get("/run/user/1000/gvfs/sftp:host=mack,user=arzt/projects/Labkit/test.tif");

	@Test
	public void testBuffered() throws IOException {
		ImgSaver imgSaver = new ImgSaver(context);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(1000, 1000, 100);
		StopWatch watch = StopWatch.createAndStart();
		SCIFIOConfig scifioConfig = new SCIFIOConfig();
		ByteArray bytebank = new ByteArray();
		Location location = new BytesLocation(bytebank, "test.tif");
		imgSaver.saveImg(location, img, scifioConfig.writerSetCompression("LZW"));
		try(FileOutputStream os = new FileOutputStream(file.toFile())) {
			os.write(bytebank.getArray(), 0, bytebank.size());
		}
		System.out.println(watch);
		System.out.println(Files.size(file));
	}

	@Test
	public void testDirect() throws IOException {
		ImgSaver imgSaver = new ImgSaver(context);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(1000, 1000, 100);
		StopWatch watch = StopWatch.createAndStart();
		SCIFIOConfig scifioConfig = new SCIFIOConfig();
		imgSaver.saveImg(new FileLocation(file.toUri()), img, scifioConfig.writerSetCompression("LZW").writerSetFailIfOverwriting(false));
		System.out.println(watch);
		System.out.println(Files.size(file));
	}

	@Test
	public void testIJ() throws IOException {
		ImagePlus image = IJ.createImage("empty", "8bit", 1000, 1000, 100);
		StopWatch watch = StopWatch.createAndStart();
		IJ.saveAsTiff(image, file.toString());
		System.out.println(watch);
		System.out.println(Files.size(file));
	}

//	@Test
//	public void testTIFWriter() throws IOException, FormatException {
//		TiffWriter writer = new TiffWriter("test.tif", null);
//		OMEXMLMetadataImpl metadata = new OMEXMLMetadataImpl();
//		writer.setMetadataRetrieve(metadata);
//		writer.setCompression(TiffWriter.COMPRESSION_LZW);
//		writer.setTileSizeX(1000);
//		writer.setTileSizeY(1000);
//		for (int i = 0; i < 1000; i++) {
//			writer.savePlane(0, new byte[1000_000]);
//		}
//	}
}
