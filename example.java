import javaewah.EWAHCompressedBitmap;
import java.util.*;

public class example {
	public static void main(String[] args) {
		    EWAHCompressedBitmap ewahBitmap1 = new EWAHCompressedBitmap();
		    EWAHCompressedBitmap ewahBitmap2 = new EWAHCompressedBitmap();
		    ewahBitmap1.set(0);
		    ewahBitmap1.set(2);
		    ewahBitmap1.set(64);
		    ewahBitmap1.set(1<<30);
		    System.out.println("bitmap 1:");
		    for(int k : ewahBitmap1) System.out.println(k);
		    ewahBitmap2.set(1);
		    ewahBitmap2.set(3);
		    ewahBitmap2.set(64);
		    ewahBitmap2.set(1<<30);
		    System.out.println("bitmap 2:");
		    for(int k : ewahBitmap2) System.out.println(k);
		    System.out.println();
		    System.out.println("bitmap 1 OR bitmap 2:");	
		    EWAHCompressedBitmap orbitmap = ewahBitmap1.or(ewahBitmap2);
		    for(int k : orbitmap) 
		      System.out.println(k);	
		    System.out.println("memory usage: "+orbitmap.sizeInBytes()+" bytes");
		    System.out.println();
            System.out.println("bitmap 1 AND bitmap 2:");
		    EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
		    for(int k : andbitmap) 
		      System.out.println(k);
		    System.out.println("memory usage: "+andbitmap.sizeInBytes()+" bytes");
            System.out.println("bitmap 1 XOR bitmap 2:");
		    EWAHCompressedBitmap xorbitmap = ewahBitmap1.xor(ewahBitmap2);
		    for(int k : xorbitmap) 
		      System.out.println(k);
		    System.out.println("memory usage: "+andbitmap.sizeInBytes()+" bytes");
		    
   }
}