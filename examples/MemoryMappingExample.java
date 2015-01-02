import com.googlecode.javaewah.EWAHCompressedBitmap;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;


public class MemoryMappingExample {
    
    public static void main(String[] args) throws IOException {
        File tmpfile = File.createTempFile("javaewah", "bin");
        tmpfile.deleteOnExit();
        final FileOutputStream fos = new FileOutputStream(tmpfile);
        EWAHCompressedBitmap ewahBitmap = EWAHCompressedBitmap.bitmapOf(0, 2, 55,
                                64, 1 << 30);
        System.out.println("Created the bitmap "+ewahBitmap);
        ewahBitmap.serialize(new DataOutputStream(fos));
        long totalcount = fos.getChannel().position();
        System.out.println("Serialized total count = "+totalcount+" bytes");
        fos.close();
        RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
        ByteBuffer bb = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, totalcount);
        EWAHCompressedBitmap mapped = new EWAHCompressedBitmap(bb);
        System.out.println("Mapped the bitmap "+mapped);
        memoryMappedFile.close();
    }
}
