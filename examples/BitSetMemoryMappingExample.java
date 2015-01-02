import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.googlecode.javaewah.datastructure.BitSet;
import com.googlecode.javaewah.datastructure.ImmutableBitSet;


public class BitSetMemoryMappingExample {
    
    public static void main(String[] args) throws IOException {
  		File tmpfile = File.createTempFile("javaewah", "bin");
  		tmpfile.deleteOnExit();
  		final FileOutputStream fos = new FileOutputStream(tmpfile);
  		BitSet Bitmap = BitSet.bitmapOf(0, 2, 55, 64, 512);
  		System.out.println("Created the bitmap " + Bitmap);
  		Bitmap.serialize(new DataOutputStream(fos));
  		long totalcount = fos.getChannel().position();
  		System.out.println("Serialized total count = " + totalcount + " bytes");
  		fos.close();
  		RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
  		ByteBuffer bb = memoryMappedFile.getChannel().map(
  				FileChannel.MapMode.READ_ONLY, 0, totalcount);
  		ImmutableBitSet mapped = new ImmutableBitSet(bb.asLongBuffer());
  		System.out.println("Mapped the bitmap " + mapped);
  		memoryMappedFile.close();
  		if (!mapped.equals(Bitmap))
  			throw new RuntimeException("Will not happen");
    }
}
