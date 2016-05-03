package com.googlecode.javaewah.datastructure;

import static org.junit.Assert.*;

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

import junit.framework.Assert;

import org.junit.Test;

import com.googlecode.javaewah.IntIterator;


public class BitSetTest
{
	
	
	public static ImmutableBitSet toImmutableBitSet(BitSet b) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		b.serialize(new DataOutputStream(bos));
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		ImmutableBitSet rmap = new ImmutableBitSet(bb.asLongBuffer());
		System.out.println("bitmap 1 (mapped) : " + rmap);
		if (!rmap.equals(b))
			throw new RuntimeException("Will not happen");
		return rmap;	
	}
	@Test
	public void simpleImmuExample() throws IOException {
		ImmutableBitSet Bitmap1 = toImmutableBitSet(BitSet.bitmapOf(0, 2, 55, 64, 512));
		ImmutableBitSet Bitmap2 = toImmutableBitSet(BitSet.bitmapOf(1, 3, 64, 512));
		System.out.println("bitmap 1: " + Bitmap1);
		System.out.println("bitmap 2: " + Bitmap2);
		assertEquals(Bitmap1.cardinality(),5);
		assertEquals(Bitmap2.cardinality(),4);
		assertFalse(Bitmap1.hashCode()==Bitmap2.hashCode());
		IntIterator is = Bitmap1.intIterator();
		int c1 = 0;
		while(is.hasNext()) {
			c1++;
			is.next();
		}
		assertEquals(Bitmap1.cardinality(),c1);

		IntIterator iu = Bitmap1.unsetIntIterator();
		int c2 = 0;
		while(iu.hasNext()) {
			c2++;
			iu.next();
		}
		assertEquals(Bitmap1.getNumberOfWords() * 64 - Bitmap1.cardinality(),c2);
	}

	@Test
	public void simpleExample() throws IOException {
		BitSet Bitmap1 = BitSet.bitmapOf(0, 2, 55, 64, 512);
		BitSet Bitmap2 = BitSet.bitmapOf(1, 3, 64, 512);
		Bitmap1.trim();
		Bitmap2.trim();
		assertTrue(Bitmap1.intersects(Bitmap2));
		assertFalse(Bitmap1.hashCode()==Bitmap2.hashCode());
		System.out.println("bitmap 1: " + Bitmap1);
		System.out.println("bitmap 2: " + Bitmap2);
		// or
		BitSet orbitmap = Bitmap1.clone();
		int orcard = Bitmap1.orcardinality(Bitmap2);
		orbitmap.or(Bitmap2);
		assertEquals(orbitmap.cardinality(),orcard);
		System.out.println("bitmap 1 OR bitmap 2: " + orbitmap);
		// and
		BitSet andbitmap = Bitmap1.clone();
		int andcard = Bitmap1.andcardinality(Bitmap2);
		andbitmap.and(Bitmap2);
		assertEquals(andbitmap.cardinality(),andcard);
		System.out.println("bitmap 1 AND bitmap 2: " + andbitmap);
		// xor
		BitSet xorbitmap = Bitmap1.clone();
		int xorcard = Bitmap1.xorcardinality(Bitmap2);
		xorbitmap.xor(Bitmap2);
		assertEquals(xorbitmap.cardinality(),xorcard);
		System.out.println("bitmap 1 XOR bitmap 2:" + xorbitmap);
		BitSet andnotbitmap = Bitmap1.clone();
		int andnotcard = Bitmap1.andNotcardinality(Bitmap2);
		andnotbitmap.andNot(Bitmap2);
		assertEquals(andnotbitmap.cardinality(),andnotcard);
		System.out.println("bitmap 1 ANDNOT bitmap 2:" + andnotbitmap);

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
		IntIterator is = Bitmap1.intIterator();
		int c1 = 0;
		while(is.hasNext()) {
			c1++;
			is.next();
		}
		assertEquals(Bitmap1.cardinality(),c1);

		IntIterator iu = Bitmap1.unsetIntIterator();
		int c2 = 0;
		while(iu.hasNext()) {
			c2++;
			iu.next();
		}
		assertEquals(Bitmap1.getNumberOfWords() * 64 - Bitmap1.cardinality(),c2);
		Bitmap1.clear();
		assertEquals(Bitmap1.cardinality(),0);
	}
	
	@Test
	public void testFlipRanges() throws IOException {
		int N = 256;
		for(int end = 1; end < N; ++end ) {
			for(int start = 0; start< end; ++start) {
				BitSet bs1 = new BitSet(N);
				for(int k = start; k < end; ++k) {
					bs1.flip(k);
				}
				BitSet bs2 = new BitSet(N);
				bs2.flip(start, end);
				Assert.assertEquals(bs2.cardinality(), end-start);
				Assert.assertEquals(bs1, bs2);
			}
		}
	}

	@Test
	public void testSetRanges() throws IOException {
		int N = 256;
		for(int end = 1; end < N; ++end ) {
			for(int start = 0; start< end; ++start) {
				BitSet bs1 = new BitSet(N);
				for(int k = start; k < end; ++k) {
					bs1.set(k);
				}
				BitSet bs2 = new BitSet(N);
				bs2.set(start, end);
				Assert.assertEquals(bs1, bs2);
			}
		}
	}
	

	@Test
	public void testClearRanges() throws IOException {
		int N = 256;
		for(int end = 1; end < N; ++end ) {
			for(int start = 0; start< end; ++start) {
				BitSet bs1 = new BitSet(N);
				bs1.set(0, N);
				for(int k = start; k < end; ++k) {
					bs1.clear(k);
				}
				BitSet bs2 = new BitSet(N);
				bs2.set(0, N);
				bs2.clear(start, end);
				Assert.assertEquals(bs1, bs2);
			}
		}
	}

	
	@Test
	public void serializationExample() throws IOException {
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
		assertEquals(mapped.size(),Bitmap.size());
		assertEquals(mapped.empty(),Bitmap.empty());
		for(int k = 0; k <= 512; ++k)
		  assertEquals(mapped.get(k),Bitmap.get(k));

		assertTrue(mapped.asBitSet().equals(Bitmap));
		assertTrue(mapped.clone().asBitSet().equals(Bitmap));
		BitSet t = new BitSet();
		t.resize(mapped.size());
		for(int i : mapped)
			t.set(i);
		assertTrue(t.equals(Bitmap));
		

	}

}
