import javaewah.EWAHCompressedBitmap;
import java.io.*;

public class example {
	
  public static String toSetBitString(EWAHCompressedBitmap b) {
    String s = "";
    for (int k : b)
      s+= k+" ";
    return s;
  }
  
  public static void main(String[] args) throws java.io.IOException {
    EWAHCompressedBitmap ewahBitmap1 = EWAHCompressedBitmap.bitmapOf(0,2,64,1<<30);
    EWAHCompressedBitmap ewahBitmap2 = EWAHCompressedBitmap.bitmapOf(1,3,64,1<<30);
    System.out.println("bitmap 1: "+toSetBitString(ewahBitmap1));
    System.out.println("bitmap 2: "+toSetBitString(ewahBitmap2));
    // or
    EWAHCompressedBitmap orbitmap = ewahBitmap1.or(ewahBitmap2);
    System.out.println("bitmap 1 OR bitmap 2: "+toSetBitString(orbitmap));
    System.out.println("memory usage: " + orbitmap.sizeInBytes() + " bytes");
    // and
    EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
    System.out.println("bitmap 1 AND bitmap 2: "+toSetBitString(andbitmap));
    System.out.println("memory usage: " + andbitmap.sizeInBytes() + " bytes");
    // xor
    EWAHCompressedBitmap xorbitmap = ewahBitmap1.xor(ewahBitmap2);
    System.out.println("bitmap 1 XOR bitmap 2:"+toSetBitString(andbitmap));
    System.out.println("memory usage: " + andbitmap.sizeInBytes() + " bytes");
    // fast aggregation over many bitmaps
    EWAHCompressedBitmap ewahBitmap3 = EWAHCompressedBitmap.bitmapOf(55,5,1<<30);
    EWAHCompressedBitmap ewahBitmap4 = EWAHCompressedBitmap.bitmapOf(4,66,1<<30);
    System.out.println("bitmap 3: "+toSetBitString(ewahBitmap3));
    System.out.println("bitmap 4: "+toSetBitString(ewahBitmap4));
    andbitmap = EWAHCompressedBitmap.and(ewahBitmap1,ewahBitmap2,
                     ewahBitmap3,ewahBitmap4);
    System.out.println("b1 AND b2 AND b3 AND b4: "+toSetBitString(andbitmap));
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
    System.out.println("bitmap 1 (recovered) : "+toSetBitString(ewahBitmap1));
  }
}