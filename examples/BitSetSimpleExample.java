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


public class BitSetSimpleExample {
	public static void main(String[] args) throws IOException {
		BitSet Bitmap1 = BitSet.bitmapOf(0, 2, 55, 64, 512);
		BitSet Bitmap2 = BitSet.bitmapOf(1, 3, 64, 512);
		System.out.println("bitmap 1: " + Bitmap1);
		System.out.println("bitmap 2: " + Bitmap2);
		// or
		BitSet orbitmap = Bitmap1.clone();
		orbitmap.or(Bitmap2);
		System.out.println("bitmap 1 OR bitmap 2: " + orbitmap);
		// and
		BitSet andbitmap = Bitmap1.clone();
		andbitmap.and(Bitmap2);
		System.out.println("bitmap 1 AND bitmap 2: " + andbitmap);
		// xor
		BitSet xorbitmap = Bitmap1.clone();
		xorbitmap.xor(Bitmap2);
		System.out.println("bitmap 1 XOR bitmap 2:" + xorbitmap);
		// serialization
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// Note: you could use a file output steam instead of ByteArrayOutputStream
		Bitmap1.serialize(new DataOutputStream(bos));
		BitSet Bitmap1new = new BitSet();
		byte[] bout = bos.toByteArray();
		Bitmap1new.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
		System.out.println("bitmap 1 (recovered) : " + Bitmap1new);
		if (!Bitmap1.equals(Bitmap1new))
			throw new RuntimeException("Will not happen");
		//
		// we can use a ByteBuffer as backend for a bitmap
		// which allows memory-mapped bitmaps
		//
		ByteBuffer bb = ByteBuffer.wrap(bout);
		ImmutableBitSet rmap = new ImmutableBitSet(bb.asLongBuffer());
		System.out.println("bitmap 1 (mapped) : " + rmap);

		if (!rmap.equals(Bitmap1))
			throw new RuntimeException("Will not happen");

		
	}
}
