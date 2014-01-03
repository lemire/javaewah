import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.symmetric.*;
import java.io.*;

/**
 * @author lemire
 *
 */
public class example {
	  
  /**
 * @param args arguments from the command line
 * @throws IOException if an IO error occurs
 */
public static void main(final String[] args) throws java.io.IOException {
    EWAHCompressedBitmap ewahBitmap1 = EWAHCompressedBitmap.bitmapOf(0,2,55,64,1<<30);
    EWAHCompressedBitmap ewahBitmap2 = EWAHCompressedBitmap.bitmapOf(1,3,64,1<<30);
    System.out.println("bitmap 1: "+ewahBitmap1);
    System.out.println("bitmap 2: "+ewahBitmap2);
    // or
    EWAHCompressedBitmap orbitmap = ewahBitmap1.or(ewahBitmap2);
    System.out.println("bitmap 1 OR bitmap 2: "+orbitmap);
    System.out.println("memory usage: " + orbitmap.sizeInBytes() + " bytes");
    // and
    EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
    System.out.println("bitmap 1 AND bitmap 2: "+andbitmap);
    System.out.println("memory usage: " + andbitmap.sizeInBytes() + " bytes");
    // xor
    EWAHCompressedBitmap xorbitmap = ewahBitmap1.xor(ewahBitmap2);
    System.out.println("bitmap 1 XOR bitmap 2:"+xorbitmap);
    System.out.println("memory usage: " + xorbitmap.sizeInBytes() + " bytes");
    // fast aggregation over many bitmaps
    EWAHCompressedBitmap ewahBitmap3 = EWAHCompressedBitmap.bitmapOf(5,55,1<<30);
    EWAHCompressedBitmap ewahBitmap4 = EWAHCompressedBitmap.bitmapOf(4,66,1<<30);
    System.out.println("bitmap 3: "+ewahBitmap3);
    System.out.println("bitmap 4: "+ewahBitmap4);
    andbitmap = EWAHCompressedBitmap.and(ewahBitmap1,ewahBitmap2,
                     ewahBitmap3,ewahBitmap4);
    System.out.println("b1 AND b2 AND b3 AND b4: "+andbitmap);
    // serialization
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // Note: you could use a file output steam instead of ByteArrayOutputStream
    ObjectOutputStream oo = new ObjectOutputStream(bos);
    ewahBitmap1.writeExternal(oo);
    oo.close();
    ewahBitmap1 = null;
    ewahBitmap1 = new EWAHCompressedBitmap();
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ewahBitmap1.readExternal(new ObjectInputStream(bis));
    System.out.println("bitmap 1 (recovered) : "+ewahBitmap1);
    //
    // support for threshold function (new as of version 0.8.0):
    // mark as true a bit that occurs at least T times in the source
    // bitmaps
    //
    EWAHCompressedBitmap threshold2 = EWAHCompressedBitmap.threshold(2, ewahBitmap1,ewahBitmap2,
                     ewahBitmap3,ewahBitmap4); 
    System.out.println("threshold 2 : "+threshold2);


  }

}
