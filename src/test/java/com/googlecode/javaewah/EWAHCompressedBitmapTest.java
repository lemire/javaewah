package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static com.googlecode.javaewah.EWAHCompressedBitmap.maxSizeInBits;
import static com.googlecode.javaewah.EWAHCompressedBitmap.WORD_IN_BITS;

/**
 * This class is used for basic unit testing.
 */
@SuppressWarnings("javadoc")
public class EWAHCompressedBitmapTest {
	
	@Test
	public void swaptest() {
		EWAHCompressedBitmap x = EWAHCompressedBitmap.bitmapOf(1,2,3);
		EWAHCompressedBitmap y = EWAHCompressedBitmap.bitmapOf(1,2,3,4);
		x.swap(y);
		Assert.assertEquals(x.cardinality(),4);
		Assert.assertEquals(y.cardinality(),3);
	}


    @Test
    public void shiftByWordSizeBits() {
        int[] positions = { 10, 11, 12, 13 };
        EWAHCompressedBitmap bm1 = EWAHCompressedBitmap.bitmapOf(positions);
        EWAHCompressedBitmap bm2 = bm1.shift(WORD_IN_BITS);

        EWAHCompressedBitmap bm3 = EWAHCompressedBitmap.bitmapOf();
        for (int pos : positions) {
            bm3.set(pos + WORD_IN_BITS);
        }
        Assert.assertEquals(bm3, bm2);
    }

    @Test
    public void shiftbug001() {
        EWAHCompressedBitmap bm1 = EWAHCompressedBitmap.bitmapOf(10, 11, 12, 13);
        EWAHCompressedBitmap bm2 =  bm1.shift(1);

        EWAHCompressedBitmap bm3 = bm1.or(bm2);
        EWAHCompressedBitmap bm4 = EWAHCompressedBitmap.bitmapOf(10,11,12,13,14);
        Assert.assertEquals(bm3, bm4);
    }
    
    @Test
    public void shiftbug002() {
        EWAHCompressedBitmap bm1 = EWAHCompressedBitmap.bitmapOf(10, 11, 12, 13, 63);
        EWAHCompressedBitmap bm2 =  bm1.shift(1);

        EWAHCompressedBitmap bm3 = bm1.or(bm2);
        EWAHCompressedBitmap bm4 = EWAHCompressedBitmap.bitmapOf(10,11,12,13,14, 63, 64);
        Assert.assertEquals(bm3, bm4);
    }
    
    @Test
    public void shiftbug003() {
        EWAHCompressedBitmap bm1 = EWAHCompressedBitmap.bitmapOf(10, 11, 12, 13, 62);
        EWAHCompressedBitmap bm2 =  bm1.shift(1);

        EWAHCompressedBitmap bm3 = bm1.or(bm2);
        EWAHCompressedBitmap bm4 = EWAHCompressedBitmap.bitmapOf(10,11,12,13,14, 62, 63);
        Assert.assertEquals(bm3, bm4);
    }

    @Test
    public void shiftbug004() {
        EWAHCompressedBitmap bm1 = EWAHCompressedBitmap.bitmapOf(10, 11, 12, 13, 64);
        EWAHCompressedBitmap bm2 =  bm1.shift(1);

        EWAHCompressedBitmap bm3 = bm1.or(bm2);
        EWAHCompressedBitmap bm4 = EWAHCompressedBitmap.bitmapOf(10,11,12,13,14, 64, 65);
        Assert.assertEquals(bm3, bm4);
    }

    
    @Test
    public void example() throws Exception {
        EWAHCompressedBitmap ewahBitmap1 = EWAHCompressedBitmap.bitmapOf(0, 2, 55, 64, 1 << 30);
        EWAHCompressedBitmap ewahBitmap2 = EWAHCompressedBitmap.bitmapOf(1, 3, 64,
                1 << 30);
        System.out.println("bitmap 1: " + ewahBitmap1);
        System.out.println("bitmap 2: " + ewahBitmap2);
        // or
        EWAHCompressedBitmap orbitmap = ewahBitmap1.or(ewahBitmap2);
        System.out.println("bitmap 1 OR bitmap 2: " + orbitmap);
        System.out.println("memory usage: " + orbitmap.sizeInBytes() + " bytes");
        // and
        EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
        System.out.println("bitmap 1 AND bitmap 2: " + andbitmap);
        System.out.println("memory usage: " + andbitmap.sizeInBytes() + " bytes");
        // xor
        EWAHCompressedBitmap xorbitmap = ewahBitmap1.xor(ewahBitmap2);
        System.out.println("bitmap 1 XOR bitmap 2:" + xorbitmap);
        System.out.println("memory usage: " + xorbitmap.sizeInBytes() + " bytes");
        // fast aggregation over many bitmaps
        EWAHCompressedBitmap ewahBitmap3 = EWAHCompressedBitmap.bitmapOf(5, 55,
                1 << 30);
        EWAHCompressedBitmap ewahBitmap4 = EWAHCompressedBitmap.bitmapOf(4, 66,
                1 << 30);
        System.out.println("bitmap 3: " + ewahBitmap3);
        System.out.println("bitmap 4: " + ewahBitmap4);
        andbitmap = EWAHCompressedBitmap.and(ewahBitmap1, ewahBitmap2, ewahBitmap3,
                ewahBitmap4);
        System.out.println("b1 AND b2 AND b3 AND b4: " + andbitmap);
        // serialization
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Note: you could use a file output steam instead of ByteArrayOutputStream
        ewahBitmap1.serialize(new DataOutputStream(bos));
        EWAHCompressedBitmap ewahBitmap1new = new EWAHCompressedBitmap();
        byte[] bout = bos.toByteArray();
        ewahBitmap1new.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
        System.out.println("bitmap 1 (recovered) : " + ewahBitmap1new);
        if (!ewahBitmap1.equals(ewahBitmap1new)) throw new RuntimeException("Will not happen");
        //
        // we can use a ByteBuffer as backend for a bitmap
        // which allows memory-mapped bitmaps
        //
        ByteBuffer bb = ByteBuffer.wrap(bout);
        EWAHCompressedBitmap rmap = new EWAHCompressedBitmap(bb);
        System.out.println("bitmap 1 (mapped) : " + rmap);

        if (!rmap.equals(ewahBitmap1)) throw new RuntimeException("Will not happen");
        //
        // support for threshold function (new as of version 0.8.0):
        // mark as true a bit that occurs at least T times in the source
        // bitmaps
        //
        EWAHCompressedBitmap threshold2 = EWAHCompressedBitmap.threshold(2,
                ewahBitmap1, ewahBitmap2, ewahBitmap3, ewahBitmap4);
        System.out.println("threshold 2 : " + threshold2);

    }

    @Test
    public void issue54() {
        EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
        for (int i = 1500; i <1600; i ++) {
            bm.set(i);
        }
        for (int i = 1500; i < 1535; i ++) {
            bm.clear(i);
        }
        bm.clear(1535);
        Assert.assertFalse(bm.isEmpty());
    }

    @Test
    public void xorCardinality() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        EWAHCompressedBitmap b2 = EWAHCompressedBitmap.bitmapOf(0,1,2,3,5,8,13,21,34,55,89,144,233,377,610);
        Assert.assertEquals(4, b1.xorCardinality(b2));
    }

    @Test
    public void andNotCardinality() {
        EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        Assert.assertEquals(0, b.andNotCardinality(b));
    }

    @Test
    public void getFirstSetBit() {
        EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf();
        Assert.assertEquals(-1, b.getFirstSetBit());
        b.set(0);
        Assert.assertEquals(0, b.getFirstSetBit());
        b.clear();
        b.setSizeInBits(WORD_IN_BITS, false);
        b.setSizeInBits(2*WORD_IN_BITS, true);
        Assert.assertEquals(WORD_IN_BITS, b.getFirstSetBit());
    }

    @Test
    public void clearStressTest() {
        System.out.println("clear stress test");
        int n = 10 * WORD_IN_BITS;
        for (int k = 0; k < 100; ++k) {
            List<Integer> setPositions = new ArrayList<Integer>(n);
            List<Integer> clearPositions = new ArrayList<Integer>(n);
            for (int i = 0; i < n; ++i) {
                setPositions.add(i);
                clearPositions.add(i);
            }
            Collections.shuffle(setPositions);
            Collections.shuffle(clearPositions);
            EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
            for (int i = 0; i < n; ++i) {
                bitmap.set(setPositions.get(i));
                bitmap.clear(clearPositions.get(i));
            }
            for (int i = 0; i < n; ++i) {
                bitmap.clear(i);
            }
            Assert.assertEquals(0, bitmap.cardinality());
            Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
        }
    }

    @Test
    public void clear() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf(0, 1, 3, 199, 666);
        Assert.assertEquals(667, bitmap.sizeInBits());
        bitmap.clear(900);
        Assert.assertEquals(901, bitmap.sizeInBits());
        for (int i = 667; i < 901; ++i) {
            Assert.assertFalse(bitmap.get(i));
        }
        Assert.assertTrue(bitmap.get(199));
        bitmap.clear(199);
        Assert.assertFalse(bitmap.get(199));
    }

    @Test
    public void equalToSelf() {
        EWAHCompressedBitmap ewahBitmap = EWAHCompressedBitmap.bitmapOf(0, 2, 55,
                64, 1 << 30);
        Assert.assertTrue(ewahBitmap.equals(ewahBitmap));
    }

    @Test
    public void notEqualTo() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        EWAHCompressedBitmap b2 = EWAHCompressedBitmap.bitmapOf(0,1,2,3,5,8,13,21,34,55,89,144,233,377,610);
        Assert.assertFalse(b1.equals(b2));
    }

    @Test
    public void safeSerialization() throws IOException {
        EWAHCompressedBitmap ewahBitmap = EWAHCompressedBitmap.bitmapOf(0, 2, 55,
                64, 1 << 30);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Note: you could use a file output steam instead of ByteArrayOutputStream
        ewahBitmap.serialize(new DataOutputStream(bos));
        EWAHCompressedBitmap ewahBitmapnew = new EWAHCompressedBitmap();
        byte[] bout = bos.toByteArray();
        ewahBitmapnew.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
        assertEquals(ewahBitmapnew, ewahBitmap);
        Assert.assertEquals(ewahBitmapnew.serializedSizeInBytes(), ewahBitmap.serializedSizeInBytes());
    }

    @Test
    public void simpleTestWithLongBuffer() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap(LongBuffer.wrap(new long[10]));

        int maxPosition = 666;
        int[] positions = new int[] { 1, maxPosition, 99, 5 };
        for (int position : positions) {
            bitmap.set(position);
        }

        Assert.assertEquals(positions.length, bitmap.cardinality());

        int[] sortedPositions = positions.clone();
        Arrays.sort(sortedPositions);
        Assert.assertArrayEquals(sortedPositions, bitmap.toArray());

        bitmap.not();
        Assert.assertEquals(maxPosition+1-positions.length, bitmap.cardinality());

        for (int i = 0; i <= maxPosition; i++) {
            bitmap.set(i);
        }
        Assert.assertEquals(maxPosition + 1, bitmap.cardinality());

        bitmap.clear();
        Assert.assertEquals(0, bitmap.cardinality());

        bitmap.swap(EWAHCompressedBitmap.bitmapOf(1));
        Assert.assertEquals(1, bitmap.cardinality());
    }

    @Test
    public void andCompressedSize() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf();
        EWAHCompressedBitmap b2 = EWAHCompressedBitmap.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.set(1);
        b2.set(WORD_IN_BITS+1);
        
        EWAHCompressedBitmap result = b1.and(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void orCompressedSize() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf();
        EWAHCompressedBitmap b2 = EWAHCompressedBitmap.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.setSizeInBits(1, false);
        b2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap result = b1.or(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void xorCompressedSize() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf();
        EWAHCompressedBitmap b2 = EWAHCompressedBitmap.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.setSizeInBits(1, false);
        b2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap result = b1.xor(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void andNotCompressedSize() {
        EWAHCompressedBitmap b1 = EWAHCompressedBitmap.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);

        EWAHCompressedBitmap result = b1.andNot(b1);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void testBug091() {
        String v1 = "0000000000000000000000000000000000000000000000000000000000111101";
        String v2 = "0000000000000000001111011111111111111111111111111110001111000000";

        EWAHCompressedBitmap bm1 = strToBitmap(v1);
        EWAHCompressedBitmap bm2 = strToBitmap(v2);

        bm1 = bm1.and(bm2); // bm1 should now have no bit set

        EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
        bm.setSizeInBits(bm1.sizeInBits(), false); // Create a bitmap with no bit set

        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(bm.sizeInBits(),bm1.sizeInBits());
        Assert.assertTrue(bm.equals(bm1));
    }

    private EWAHCompressedBitmap strToBitmap(String str) {
        EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i)=='1') {
                bm.set(i);
            }
        }
        bm.setSizeInBits(str.length(), false);
        return bm;
    }
    
    @Test
    public void testBug090() throws Exception {
        EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
        bm.setSizeInBits(8, false); // Create a bitmap with no bit set

        EWAHCompressedBitmap bm1 = bm.clone();
        bm1.not(); // Create a bitmap with all bits set
        bm1 = bm1.and(bm); // Clear all bits

        Assert.assertEquals(0,bm.cardinality());
        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(bm.sizeInBits(),bm1.sizeInBits());
        Assert.assertTrue(bm.equals(bm1));
    }

    @Test
    public void testBug090b() throws Exception {
        EWAHCompressedBitmap bm1 = new EWAHCompressedBitmap();
        bm1.setSizeInBits(8, false); // Create a bitmap with no bit set
        System.out.println(bm1.toDebugString());
        EWAHCompressedBitmap bm2 = new EWAHCompressedBitmap();
        bm2.setSizeInBits(64, false); // Create a bitmap with no bit set
        EWAHCompressedBitmap bm3 = new EWAHCompressedBitmap();
        Assert.assertTrue(bm1.equals(bm2));
        Assert.assertTrue(bm2.equals(bm1));
        Assert.assertTrue(bm2.equals(bm3));
        Assert.assertTrue(bm3.equals(bm2));
        Assert.assertTrue(bm1.equals(bm3));
        Assert.assertTrue(bm3.equals(bm1));
    }
    

    @Test
    public void testBug090c() throws Exception {
        EWAHCompressedBitmap bm1 = new EWAHCompressedBitmap();
        bm1.setSizeInBits(8, false); // Create a bitmap with no bit set
        System.out.println(bm1.toDebugString());
        EWAHCompressedBitmap bm2 = new EWAHCompressedBitmap();
        bm2.setSizeInBits(64, false); // Create a bitmap with no bit set
        EWAHCompressedBitmap bm3 = new EWAHCompressedBitmap();
        Assert.assertEquals(bm1.hashCode(), bm2.hashCode());
        Assert.assertEquals(bm3.hashCode(), bm2.hashCode());
    }
    

    
    @Test
    public void jugovacTest() {
        EWAHCompressedBitmap bm1 = new EWAHCompressedBitmap(1);
        bm1.set(1);
        EWAHCompressedBitmap bm2 = new EWAHCompressedBitmap(0);
        bm1.andCardinality(bm2);
    }
    
    @Test
    public void setOutOfOrderStressTest() {
        System.out.println("out-of-order stress test");
        int n = 10 * WORD_IN_BITS;
        for(int k = 0; k < 100; ++k) {
            List<Integer> positions = new ArrayList<Integer>(n);
            for (int i = 0; i < n; ++i) {
                positions.add(i);
            }
            Collections.shuffle(positions);
            EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
            for (int position : positions) {
                bitmap.set(position);
            }
            IntIterator iterator = bitmap.intIterator();
            for (int i = 0; i < n; ++i) {
                Assert.assertTrue(iterator.hasNext());
                Assert.assertEquals(i, iterator.next());
            }
            Assert.assertFalse(iterator.hasNext());
            Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
        }
    }

    @Test
    public void setOutOfOrder() {
        int[][] positionsArray = new int[][]{
            new int[] { 111, 94, 17, 116, 100, 4, 72, 125, 112, 130, 8, 141, 45, 33, 171, 122, 128, 104, 102, 62, 115, 37, 96, 77, 165, 168, 52, 58, 47, 59, 49, 69, 185, 97, 151, 28, 29, 3, 61, 88, 135, 164, 178, 120, 144, 73, 155, 180, 140, 74, 20, 161, 143, 92, 85, 71, 63, 123, 147, 75, 15, 34, 105, 93, 158, 167, 86, 1, 127, 160, 133, 31, 53, 99, 129, 21, 44, 81, 27, 38, 11, 172, 66, 118, 57, 54, 36, 32, 159, 106, 12, 114, 132, 174, 9, 184, 121, 134, 181, 191, 190, 64, 65, 55, 156, 173, 109, 51, 170, 7, 146, 157, 182, 23, 10, 24, 2, 119, 25, 101, 84, 154, 179, 67, 46, 177, 40, 107, 14, 68, 79, 82, 22, 137, 124, 48, 0, 110, 148, 18, 188, 30, 169, 60, 145, 35, 89, 19, 90, 153, 163, 41, 70, 113, 108, 39, 152, 117, 175, 6, 43, 50, 80, 26, 95, 83, 126, 103, 91, 183, 16, 150, 131, 78, 189, 136, 5, 149, 187, 87, 176, 142, 42, 138, 186, 139, 56, 166, 98, 76, 13, 162 },
            new int[] { 160, 146, 144, 19, 94, 135, 109, 150, 133, 158, 168, 8, 151, 115, 91, 167, 147, 54, 126, 110, 155, 163, 52, 80, 9, 38, 48, 66, 21, 174, 49, 77, 165, 114, 149, 71, 86, 41, 185, 101, 180, 57, 96, 112, 67, 68, 184, 69, 148, 100, 27, 164, 0, 2, 90, 111, 72, 159, 3, 127, 83, 122, 128, 75, 34, 182, 123, 179, 130, 172, 26, 58, 120, 42, 102, 87, 16, 97, 39, 121, 161, 169, 145, 171, 17, 103, 37, 25, 117, 46, 53, 84, 125, 23, 1, 141, 137, 186, 30, 50, 190, 131, 191, 56, 107, 178, 12, 82, 18, 44, 113, 116, 92, 51, 134, 156, 15, 153, 14, 188, 162, 106, 20, 10, 175, 157, 124, 13, 189, 88, 119, 136, 176, 139, 187, 170, 173, 65, 24, 40, 181, 61, 47, 35, 154, 132, 142, 62, 45, 183, 138, 95, 6, 152, 93, 4, 73, 5, 98, 28, 11, 166, 81, 99, 32, 22, 79, 60, 33, 85, 63, 76, 105, 143, 55, 36, 43, 59, 118, 7, 108, 64, 104, 74, 89, 129, 177, 29, 140, 78, 31, 70 },
            new int[] { 156, 104, 167, 70, 102, 151, 87, 186, 169, 172, 158, 73, 85, 89, 103, 79, 38, 81, 66, 42, 126, 61, 157, 185, 120, 149, 32, 114, 170, 155, 91, 127, 96, 30, 4, 165, 39, 0, 56, 129, 171, 82, 108, 26, 123, 55, 152, 184, 137, 118, 147, 134, 116, 14, 113, 177, 1, 58, 35, 63, 150, 41, 19, 101, 23, 43, 49, 180, 141, 176, 153, 37, 12, 122, 145, 112, 27, 97, 105, 20, 133, 109, 6, 50, 51, 106, 11, 125, 25, 139, 9, 52, 54, 138, 95, 166, 62, 189, 173, 124, 48, 100, 47, 128, 159, 31, 45, 44, 72, 17, 130, 88, 119, 67, 140, 76, 98, 13, 131, 78, 117, 16, 190, 28, 143, 187, 183, 5, 18, 164, 7, 24, 115, 121, 34, 160, 178, 99, 162, 111, 146, 174, 69, 77, 161, 53, 65, 64, 188, 181, 8, 29, 10, 80, 110, 90, 132, 40, 86, 135, 179, 3, 148, 92, 33, 2, 59, 107, 60, 93, 191, 74, 84, 182, 83, 15, 154, 175, 36, 46, 57, 136, 21, 163, 71, 142, 94, 144, 75, 22, 168, 68 },
            new int[] { 186, 71, 66, 157, 163, 135, 38, 160, 105, 28, 173, 106, 6, 177, 22, 73, 2, 11, 110, 108, 26, 139, 56, 137, 17, 129, 166, 24, 33, 12, 51, 96, 44, 74, 87, 72, 99, 156, 9, 84, 172, 167, 150, 153, 134, 30, 115, 102, 158, 76, 170, 55, 65, 162, 54, 14, 53, 154, 70, 161, 75, 159, 176, 10, 111, 100, 133, 37, 93, 175, 67, 83, 86, 169, 147, 149, 138, 82, 103, 164, 97, 124, 1, 16, 155, 116, 118, 112, 143, 98, 91, 13, 101, 180, 19, 34, 35, 127, 39, 152, 42, 61, 68, 168, 104, 141, 184, 185, 15, 64, 128, 32, 21, 49, 25, 89, 171, 81, 183, 181, 40, 5, 125, 78, 189, 109, 4, 113, 178, 114, 121, 62, 63, 79, 0, 43, 142, 36, 119, 47, 122, 148, 41, 92, 187, 131, 48, 45, 132, 69, 182, 90, 59, 126, 60, 130, 29, 57, 18, 94, 120, 136, 27, 46, 151, 179, 190, 3, 107, 52, 88, 77, 174, 95, 165, 31, 145, 188, 23, 80, 8, 85, 117, 144, 50, 123, 146, 20, 58, 140, 7, 191 },
            new int[] { 187, 9, 174, 56, 26, 81, 132, 156, 103, 100, 79, 137, 117, 123, 157, 68, 61, 167, 98, 0, 77, 39, 65, 34, 48, 72, 74, 181, 146, 70, 5, 138, 80, 90, 86, 46, 37, 53, 89, 83, 45, 121, 166, 11, 171, 58, 125, 142, 64, 92, 108, 59, 71, 127, 135, 188, 14, 150, 173, 55, 158, 136, 99, 10, 112, 116, 155, 151, 145, 38, 54, 35, 101, 12, 3, 107, 180, 178, 22, 84, 183, 154, 102, 104, 190, 159, 170, 47, 115, 111, 88, 131, 140, 124, 149, 6, 168, 133, 28, 139, 82, 91, 160, 27, 126, 78, 130, 41, 134, 164, 163, 51, 19, 23, 17, 60, 189, 20, 42, 114, 13, 118, 97, 30, 147, 76, 24, 93, 110, 44, 50, 176, 7, 16, 87, 63, 67, 69, 113, 15, 185, 148, 62, 33, 95, 169, 25, 57, 161, 182, 120, 21, 36, 94, 1, 128, 75, 175, 66, 184, 31, 73, 153, 52, 129, 152, 85, 49, 119, 32, 4, 40, 2, 8, 177, 109, 96, 29, 43, 179, 18, 105, 141, 186, 106, 162, 165, 122, 143, 172, 144, 191 },
            new int[] { 219, 226, 72, 129, 131, 249, 140, 213, 245, 240, 28, 250, 212, 87, 42, 112, 69, 94, 125, 165, 215, 30, 197, 247, 39, 171, 16, 3, 101, 147, 54, 149, 89, 236, 15, 77, 141, 246, 36, 6, 104, 85, 248, 8, 66, 119, 23, 2, 123, 91, 229, 61, 68, 223, 124, 135, 158, 218, 177, 251, 71, 75, 26, 217, 120, 180, 188, 64, 80, 100, 252, 208, 45, 130, 52, 44, 31, 216, 167, 152, 84, 126, 142, 224, 65, 154, 127, 113, 92, 170, 74, 108, 67, 57, 17, 201, 78, 32, 244, 194, 157, 121, 103, 122, 48, 232, 117, 34, 178, 46, 179, 231, 95, 211, 183, 110, 162, 7, 186, 196, 148, 187, 93, 173, 47, 88, 156, 172, 73, 204, 139, 41, 132, 58, 159, 90, 109, 4, 70, 5, 176, 99, 160, 184, 150, 18, 133, 106, 199, 168, 161, 118, 63, 145, 11, 20, 10, 144, 207, 174, 230, 102, 51, 253, 37, 225, 243, 22, 151, 128, 175, 242, 182, 220, 206, 136, 40, 190, 254, 235, 195, 27, 35, 19, 62, 21, 81, 1, 198, 56, 163, 193, 155, 53, 205, 203, 241, 214, 169, 134, 192, 233, 50, 210, 164, 97, 221, 185, 13, 255, 227, 83, 96, 209, 146, 114, 143, 237, 107, 105, 115, 166, 200, 222, 59, 76, 29, 153, 43, 14, 181, 79, 189, 24, 228, 38, 86, 0, 116, 238, 234, 55, 98, 137, 12, 202, 191, 111, 33, 49, 25, 9, 138, 60, 82, 239 },
            new int[] { 261, 182, 37, 161, 47, 240, 214, 124, 167, 233, 110, 83, 310, 209, 198, 206, 201, 219, 177, 82, 210, 107, 163, 16, 200, 53, 71, 20, 193, 158, 183, 106, 138, 290, 19, 55, 313, 197, 123, 125, 257, 92, 104, 60, 234, 139, 218, 223, 88, 276, 127, 259, 148, 297, 145, 38, 302, 260, 118, 282, 314, 100, 23, 153, 288, 121, 241, 316, 165, 168, 98, 24, 238, 244, 89, 278, 255, 237, 99, 277, 306, 61, 222, 27, 191, 215, 298, 43, 87, 51, 293, 129, 70, 25, 180, 190, 132, 133, 149, 94, 79, 21, 73, 181, 225, 131, 44, 249, 119, 95, 195, 69, 204, 315, 187, 54, 81, 134, 164, 284, 30, 232, 52, 160, 235, 64, 226, 171, 205, 262, 236, 300, 309, 304, 156, 263, 10, 286, 221, 96, 50, 289, 189, 212, 143, 254, 256, 49, 147, 75, 318, 85, 169, 185, 248, 1, 18, 6, 15, 295, 159, 162, 112, 301, 292, 36, 97, 247, 146, 59, 32, 155, 157, 178, 33, 22, 103, 128, 170, 108, 65, 220, 188, 203, 229, 130, 253, 117, 230, 243, 287, 273, 57, 68, 246, 109, 40, 56, 274, 46, 12, 285, 45, 242, 245, 126, 258, 41, 144, 17, 58, 213, 62, 252, 194, 217, 122, 102, 279, 72, 305, 266, 216, 303, 35, 283, 39, 137, 269, 272, 312, 4, 151, 2, 172, 13, 294, 296, 186, 114, 250, 101, 224, 3, 77, 141, 111, 67, 74, 184, 307, 115, 0, 271, 227, 311, 78, 28, 299, 63, 308, 150, 208, 211, 317, 116, 5, 239, 202, 135, 84, 142, 86, 80, 192, 251, 42, 199, 34, 281, 9, 93, 8, 136, 264, 174, 231, 175, 275, 280, 207, 48, 228, 90, 268, 76, 113, 179, 140, 11, 173, 120, 166, 265, 152, 291, 176, 91, 196, 66, 154, 26, 270, 7, 267, 319, 31, 105, 14, 29 },
            new int[] { 306, 159, 36, 192, 263, 107, 119, 109, 140, 297, 275, 261, 259, 139, 283, 211, 148, 317, 262, 91, 11, 278, 301, 216, 232, 168, 12, 133, 116, 66, 88, 95, 154, 46, 312, 136, 229, 242, 218, 53, 38, 213, 127, 32, 247, 130, 84, 31, 137, 93, 251, 179, 238, 220, 106, 26, 298, 239, 18, 111, 44, 103, 45, 118, 292, 276, 59, 20, 308, 196, 141, 67, 78, 72, 172, 212, 255, 288, 160, 289, 69, 209, 47, 187, 303, 117, 181, 104, 43, 210, 79, 222, 113, 315, 296, 290, 285, 264, 17, 129, 99, 149, 2, 138, 175, 295, 55, 206, 16, 299, 71, 167, 62, 123, 50, 215, 246, 157, 164, 236, 266, 319, 144, 221, 7, 92, 75, 51, 152, 282, 200, 57, 49, 271, 134, 186, 56, 70, 170, 97, 199, 300, 98, 169, 314, 128, 195, 318, 267, 10, 22, 219, 272, 189, 258, 226, 42, 87, 76, 73, 153, 178, 183, 110, 9, 23, 155, 205, 286, 126, 241, 256, 214, 94, 250, 21, 142, 8, 80, 176, 102, 19, 161, 132, 163, 177, 194, 174, 120, 284, 52, 171, 124, 61, 150, 1, 166, 6, 231, 240, 307, 291, 101, 277, 162, 228, 89, 54, 207, 217, 85, 108, 245, 184, 74, 305, 237, 77, 235, 146, 65, 253, 281, 304, 27, 4, 294, 33, 203, 112, 40, 224, 29, 165, 249, 100, 293, 105, 243, 13, 197, 310, 63, 311, 135, 96, 173, 68, 257, 156, 114, 5, 35, 260, 90, 15, 145, 143, 122, 287, 248, 244, 24, 225, 0, 268, 14, 234, 188, 201, 279, 86, 60, 313, 230, 39, 227, 208, 28, 233, 25, 198, 302, 58, 191, 202, 309, 316, 48, 254, 37, 131, 252, 151, 81, 182, 204, 82, 185, 125, 115, 34, 269, 190, 158, 83, 147, 30, 121, 273, 280, 64, 180, 193, 274, 265, 3, 41, 223, 270 },
            new int[] { 633, 145, 267, 188, 75, 528, 160, 305, 459, 455, 530, 186, 359, 181, 437, 250, 180, 325, 147, 473, 87, 510, 465, 280, 166, 120, 453, 128, 566, 33, 608, 253, 350, 522, 430, 351, 360, 580, 45, 51, 544, 555, 457, 597, 213, 400, 390, 513, 438, 313, 37, 616, 57, 311, 436, 100, 228, 108, 533, 1, 396, 462, 342, 378, 297, 148, 216, 211, 304, 146, 546, 46, 262, 290, 71, 639, 201, 624, 178, 303, 254, 487, 468, 344, 506, 451, 369, 420, 195, 444, 107, 50, 592, 12, 326, 259, 13, 227, 634, 270, 226, 276, 570, 524, 194, 190, 90, 394, 101, 606, 542, 229, 340, 581, 541, 578, 118, 301, 5, 16, 501, 14, 158, 466, 551, 636, 231, 320, 193, 222, 625, 152, 112, 134, 167, 287, 199, 189, 610, 440, 110, 554, 89, 408, 35, 365, 138, 419, 22, 483, 157, 122, 214, 514, 316, 247, 371, 109, 91, 500, 206, 237, 63, 170, 495, 163, 352, 523, 449, 384, 29, 418, 88, 536, 426, 432, 44, 635, 605, 347, 192, 489, 590, 310, 0, 271, 337, 185, 516, 234, 15, 150, 79, 210, 235, 613, 480, 161, 21, 355, 175, 56, 169, 38, 572, 637, 607, 65, 503, 467, 401, 261, 505, 539, 402, 255, 34, 171, 97, 98, 174, 59, 176, 383, 596, 593, 464, 431, 604, 92, 266, 476, 286, 472, 114, 260, 27, 84, 336, 332, 2, 386, 519, 525, 559, 232, 308, 124, 439, 353, 545, 416, 600, 212, 73, 575, 284, 299, 252, 385, 441, 69, 531, 275, 30, 427, 583, 269, 488, 187, 300, 263, 623, 411, 484, 52, 67, 405, 393, 595, 338, 611, 99, 333, 534, 617, 39, 8, 568, 362, 202, 17, 111, 615, 603, 567, 560, 397, 452, 279, 191, 507, 143, 508, 442, 47, 31, 535, 272, 322, 509, 258, 168, 309, 130, 149, 103, 184, 485, 571, 461, 526, 105, 155, 218, 448, 407, 285, 93, 95, 78, 446, 282, 215, 42, 406, 104, 348, 4, 72, 433, 323, 106, 377, 594, 20, 589, 74, 520, 458, 302, 293, 85, 470, 403, 577, 298, 60, 498, 217, 454, 409, 26, 208, 10, 629, 370, 291, 387, 225, 238, 140, 358, 131, 66, 321, 354, 179, 329, 380, 502, 435, 312, 242, 159, 584, 561, 547, 53, 24, 492, 64, 58, 249, 317, 241, 494, 497, 294, 562, 248, 621, 364, 288, 246, 314, 307, 256, 129, 239, 81, 388, 586, 557, 345, 587, 154, 251, 49, 173, 481, 40, 563, 550, 598, 511, 278, 389, 243, 162, 413, 517, 372, 32, 601, 632, 521, 612, 343, 477, 126, 392, 428, 41, 86, 61, 196, 376, 327, 295, 331, 478, 132, 638, 83, 77, 102, 3, 62, 11, 373, 23, 113, 141, 200, 619, 335, 76, 264, 177, 349, 375, 283, 356, 198, 491, 151, 512, 203, 532, 346, 6, 482, 165, 334, 582, 543, 475, 127, 391, 423, 142, 548, 54, 172, 527, 306, 588, 490, 28, 289, 374, 553, 319, 183, 136, 115, 399, 585, 443, 315, 245, 123, 257, 412, 445, 627, 631, 43, 620, 236, 277, 339, 504, 220, 499, 153, 156, 424, 7, 55, 456, 296, 558, 515, 496, 367, 569, 469, 121, 240, 119, 538, 207, 363, 628, 125, 205, 460, 96, 133, 268, 265, 518, 18, 164, 281, 209, 556, 137, 9, 224, 630, 182, 94, 361, 48, 565, 471, 395, 341, 573, 463, 434, 429, 382, 139, 273, 116, 80, 410, 197, 415, 366, 537, 421, 564, 486, 599, 618, 274, 576, 493, 19, 422, 25, 529, 552, 135, 219, 36, 602, 379, 591, 626, 357, 447, 292, 223, 381, 479, 233, 368, 230, 425, 574, 70, 474, 244, 82, 324, 417, 450, 221, 404, 540, 549, 328, 414, 622, 614, 117, 398, 579, 609, 204, 68, 330, 144, 318 },
        };
        for(int[] positions : positionsArray) {
            EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
            for (int position : positions) {
                bitmap.set(position);
                Assert.assertTrue(bitmap.toList().contains(position));
            }
            IntIterator iterator = bitmap.intIterator();
            for (int i = 0; i < positions.length; ++i) {
                Assert.assertTrue(iterator.hasNext());
                Assert.assertEquals(i, iterator.next());
            }
            Assert.assertFalse(iterator.hasNext());
            Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
        }
    }

    @Test
    public void setBitsInDecreasingOrder() {
        int[] positions = new int[] { 0, 1, 2, 3, 5, 8, 13, 21 };
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        for(int i=positions.length-1; i>=0; --i) {
            Assert.assertTrue(bitmap.set(positions[i]));
        }
        IntIterator iterator = bitmap.intIterator();
        for(int position : positions) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(position, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitsInDecreasingOrderWithWordPrefix() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.set(10);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS + 10);
        bitmap.set(WORD_IN_BITS + 5);
        IntIterator iterator = bitmap.intIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(10, iterator.next());
        Assert.assertEquals(WORD_IN_BITS + 5, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS + 10, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitsInDecreasingOrderWithWordPrefixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + 10);
        bitmap.set(WORD_IN_BITS + 5);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS + 5, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS + 10, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitsInDecreasingOrderWithWordPrefixOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS + 10);
        bitmap.set(WORD_IN_BITS + 5);
        IntIterator iterator = bitmap.intIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS + 5, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS + 10, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitInWordOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS/2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitInWordsOfZerosWithWordPrefix() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.set(0);
        bitmap.setSizeInBits(3*WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS*3/2);
        IntIterator iterator = bitmap.intIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(0, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS*3/2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setUniqueClearBit() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefix() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i=WORD_IN_BITS; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordSuffix() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(WORD_IN_BITS+1, true);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS+1; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(2 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixAndWordSuffix() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-1, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS+1, true);
        bitmap.setSizeInBits(3 * WORD_IN_BITS, false);
        bitmap.trim();
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=WORD_IN_BITS-1; i<2*WORD_IN_BITS+1; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(4 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=WORD_IN_BITS; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(2 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordSuffixOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        bitmap.setSizeInBits(2*WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(2 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordSuffixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2*WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordSuffixAndWordSuffixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS / 2, false);
        bitmap.setSizeInBits(2*WORD_IN_BITS, true);
        bitmap.setSizeInBits(3*WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i=WORD_IN_BITS*3/2; i<3*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixAndSuffixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(3*WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<3*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixAndWordSuffixOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-1, false);
        bitmap.setSizeInBits(2*WORD_IN_BITS-1, true);
        bitmap.setSizeInBits(2*WORD_IN_BITS, false);
        bitmap.setSizeInBits(3*WORD_IN_BITS, true);
        bitmap.set(2*WORD_IN_BITS-1);
        IntIterator iterator = bitmap.intIterator();
        for(int i=WORD_IN_BITS-1; i<3*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setInIncreasingOrderStressTest() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        for (int i=0; i< 10 * WORD_IN_BITS; ++i) {
            bitmap.set(i);
            IntIterator iterator = bitmap.intIterator();
            for (int j=0; j<=i; ++j) {
                Assert.assertTrue(iterator.hasNext());
                Assert.assertEquals(j, iterator.next());
            }
            Assert.assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void compareSetAndSetSizeInBits() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf();
        for(int i = WORD_IN_BITS / 2; i < WORD_IN_BITS; ++i) {
            bitmap1.set(i);
        }
        for(int i = WORD_IN_BITS * 3 / 2; i < WORD_IN_BITS * 2; ++i) {
            bitmap1.set(i);
        }

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS / 2, false);
        bitmap2.setSizeInBits(WORD_IN_BITS, true);
        bitmap2.setSizeInBits(WORD_IN_BITS * 3 / 2, false);
        bitmap2.setSizeInBits(WORD_IN_BITS * 2, true);

        Assert.assertEquals(bitmap1, bitmap2);
        Assert.assertEquals(bitmap1.buffer.sizeInWords(), bitmap2.buffer.sizeInWords());
        for (int i = 0; i < bitmap1.buffer.sizeInWords(); i++) {
            Assert.assertEquals(bitmap1.buffer.getWord(i), bitmap2.buffer.getWord(i));
        }
    }

    @Test
    public void compareSetAndSetSizeInBits2() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf();
        for(int i = 0; i < WORD_IN_BITS / 2; ++i) {
            bitmap1.set(i);
        }
        for(int i = WORD_IN_BITS; i < WORD_IN_BITS * 3 / 2; ++i) {
            bitmap1.set(i);
        }

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS / 2, true);
        bitmap2.setSizeInBits(WORD_IN_BITS, false);
        bitmap2.setSizeInBits(WORD_IN_BITS * 3 / 2, true);

        Assert.assertEquals(bitmap1, bitmap2);
        Assert.assertEquals(bitmap1.buffer.sizeInWords(), bitmap2.buffer.sizeInWords());
        for (int i = 0; i < bitmap1.buffer.sizeInWords(); i++) {
            Assert.assertEquals(bitmap1.buffer.getWord(i), bitmap2.buffer.getWord(i));
        }
    }

    @Test
    public void setSizeInBitsStressTest() {
        for (int i = 0; i < 10 * WORD_IN_BITS; ++i) {
            for (int j = i; j < i + 10 * WORD_IN_BITS; ++j) {
                for (boolean a : Arrays.asList(true, false)) {
                    for (boolean b : Arrays.asList(true, false)) {
                        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
                        bitmap.setSizeInBits(i, a);
                        bitmap.setSizeInBits(j, b);
                        IntIterator iterator = bitmap.intIterator();
                        if (a) {
                            for (int k = 0; k < i; ++k) {
                                Assert.assertTrue(iterator.hasNext());
                                Assert.assertEquals(k, iterator.next());
                            }
                        }
                        if (b) {
                            for (int k = i; k < j; ++k) {
                                Assert.assertTrue(iterator.hasNext());
                                Assert.assertEquals(k, iterator.next());
                            }
                        }
                        Assert.assertFalse(iterator.hasNext());
                    }
                }
            }
        }
    }

    @Test
    public void setSizeInBits() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i=WORD_IN_BITS; i<WORD_IN_BITS + WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIterator() {
        int[] positions = new int[] { 0, 1, 2, 3, 5, 8, 13, 21 };
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf(positions);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i=positions.length-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(positions[i], iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i=WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        IntIterator iterator = bitmap.reverseIntIterator();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfOnesAndZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-10, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i=WORD_IN_BITS-10; i>0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i-1, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverMultipleRLWs() {
        EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf(1000, 100000, 100000 + WORD_IN_BITS);
        IntIterator iterator = b.reverseIntIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(100000 + WORD_IN_BITS, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(100000, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(1000, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverMixedRunningLengthWords() {
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        b.setSizeInBits(WORD_IN_BITS, true);
        b.set(WORD_IN_BITS+5);

        IntIterator iterator = b.reverseIntIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS+5, iterator.next());
        for(int i=WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverConsecutiveLiteralsInSameRunningLengthWord() {
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();
        b.setSizeInBits(WORD_IN_BITS, true);
        b.setSizeInBits(2*WORD_IN_BITS, false);
        b.setSizeInBits(3*WORD_IN_BITS, true);
        b.set(3*WORD_IN_BITS+5);
        b.set(5*WORD_IN_BITS-1);

        IntIterator iterator = b.reverseIntIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(5*WORD_IN_BITS-1, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(3*WORD_IN_BITS+5, iterator.next());
        for(int i=3*WORD_IN_BITS-1; i>=2*WORD_IN_BITS; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i=WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void isEmpty() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(1000, false);
        Assert.assertTrue(bitmap.isEmpty());
        bitmap.set(1001);
        Assert.assertFalse(bitmap.isEmpty());
    }

    @Test
    public void issue58() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf(52344, 52344 + 9);
        ChunkIterator iterator = bitmap.chunkIterator();

        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(52344, iterator.nextLength());
        iterator.move(iterator.nextLength());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(1, iterator.nextLength());
        iterator.move(iterator.nextLength());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(8, iterator.nextLength());
        iterator.move(iterator.nextLength());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(1, iterator.nextLength());
        iterator.move(iterator.nextLength());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void issue59() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf(243, 260, 1000);
        ChunkIterator iter = bitmap.chunkIterator();
        iter.move(245);
        Assert.assertEquals(15, iter.nextLength());
    }

    @Test
    public void issue61() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(210696);
        bitmap.set(210984);
        bitmap.set(210985);
        ChunkIterator iter = bitmap.chunkIterator();
        iter.move(210984);
        Assert.assertEquals(2, iter.nextLength());

        bitmap = new EWAHCompressedBitmap();
        bitmap.set(210696);
        bitmap.set(210698);
        bitmap.set(210699);
        iter = bitmap.chunkIterator();
        iter.move(210698);
        Assert.assertEquals(2, iter.nextLength());
    }

    @Test
    public void chunkIterator() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf(0, 1, 2, 3, 4, 7, 8, 9, 10);

        ChunkIterator iterator = bitmap.chunkIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(5, iterator.nextLength());
        iterator.move(2);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(3, iterator.nextLength());
        iterator.move();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(2, iterator.nextLength());
        iterator.move(5);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(1, iterator.nextLength());
        iterator.move();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void chunkIteratorOverBitmapOfZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);

        ChunkIterator iterator = bitmap.chunkIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(WORD_IN_BITS, iterator.nextLength());
        iterator.move();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void chunkIteratorOverBitmapOfZerosAndOnes() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS + 10, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);

        ChunkIterator iterator = bitmap.chunkIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(WORD_IN_BITS + 10, iterator.nextLength());
        iterator.move();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(WORD_IN_BITS - 10, iterator.nextLength());
        iterator.move();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void chunkIteratorOverBitmapOfOnesAndZeros() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS - 10, true);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, false);

        ChunkIterator iterator = bitmap.chunkIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.nextBit());
        Assert.assertEquals(WORD_IN_BITS - 10, iterator.nextLength());
        iterator.move();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertFalse(iterator.nextBit());
        Assert.assertEquals(WORD_IN_BITS + 10, iterator.nextLength());
        iterator.move();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void simpleCompose() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf(1, 3, 4);
        bitmap1.setSizeInBits(5, false);

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf(0, 2);

        EWAHCompressedBitmap result = bitmap1.compose(bitmap2);

        Assert.assertEquals(5, result.sizeInBits());
        Assert.assertEquals(2, result.cardinality());
        Assert.assertEquals(Integer.valueOf(1), result.toList().get(0));
        Assert.assertEquals(Integer.valueOf(4), result.toList().get(1));
    }

    @Test
    public void composeBitmapOfOnesWithItself() {
        EWAHCompressedBitmap bitmap = EWAHCompressedBitmap.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap result = bitmap.compose(bitmap);

        Assert.assertEquals(bitmap, result);
    }

    @Test
    public void composeBitmapOfZerosAndOnesWithBitmapOfOnes() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf();
        bitmap1.setSizeInBits(WORD_IN_BITS, false);
        bitmap1.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap result = bitmap1.compose(bitmap2);

        Assert.assertEquals(bitmap1, result);
    }

    @Test
    public void composeBitmapOfOnesWithBitmapOfZerosAndOnes() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf();
        bitmap1.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS, false);
        bitmap2.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap result = bitmap1.compose(bitmap2);

        Assert.assertEquals(bitmap2, result);
    }

    @Test
    public void composeBitmapWithBitmapOfZeros() {
        EWAHCompressedBitmap bitmap1 = EWAHCompressedBitmap.bitmapOf(1, 3, 4, 9);
        bitmap1.setSizeInBits(WORD_IN_BITS, false);

        EWAHCompressedBitmap bitmap2 = EWAHCompressedBitmap.bitmapOf();
        bitmap2.setSizeInBits(5, false);

        EWAHCompressedBitmap result = bitmap1.compose(bitmap2);

        Assert.assertEquals(0, result.cardinality());
        Assert.assertEquals(WORD_IN_BITS, result.sizeInBits());
    }

    @Test
    public void testAstesana() throws Exception {
        for(int k = 5; k < 256; ++k) {
            EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
            bm.set(1);
            bm.setSizeInBits(k, false);
            EWAHCompressedBitmap bm1 = bm.clone();
            bm1.not();
            EWAHCompressedBitmap x = bm1.and(bm1);
            Assert.assertEquals(x.cardinality(), k-1);
            x = bm1.andNot(bm1);
            Assert.assertEquals(x.cardinality(), 0);
            x = bm1.xor(bm1);
            Assert.assertEquals(x.cardinality(), 0);
            x = bm1.or(bm1);
            Assert.assertEquals(x.cardinality(), k-1);
        }
    }
    @Test
    public void testAstesana2() {
        for (int k = 1; k < 256; ++k) {
            // Create two equivalent bitmaps
            EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
            bm.set(0);
            bm.setSizeInBits(k, false);
            EWAHCompressedBitmap bm3 = new EWAHCompressedBitmap();
            bm3.set(0);
            bm3.setSizeInBits(k, false);
            // Perform two negation ->
            // should change nothing
            bm.not();
            bm.not();
            // Verify it changes nothing
            Assert.assertArrayEquals(bm.toArray(), bm3.toArray());
            Assert.assertEquals(bm.sizeInBits(), bm3.sizeInBits());

            Assert.assertTrue(bm.equals(bm3));
        }
    }

    @Test
    public void clearIntIterator() {
        EWAHCompressedBitmap x = EWAHCompressedBitmap.bitmapOf(1, 3, 7, 8, 10);
        x.setSizeInBits(500, true);
        x.setSizeInBits(501, false);
        x.setSizeInBits(1000, true);
        x.set(1001);
        IntIterator iterator = x.clearIntIterator();
        for (int i : Arrays.asList(0, 2, 4, 5, 6, 9, 500, 1000)) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void clearIntIteratorOverBitmapOfZeros() {
        EWAHCompressedBitmap x = EWAHCompressedBitmap.bitmapOf();
        x.setSizeInBits(WORD_IN_BITS, false);
        IntIterator iterator = x.clearIntIterator();
        for (int i = 0; i < WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testGet() {
        for (int gap = 29; gap < 10000; gap *= 10) {
            EWAHCompressedBitmap x = new EWAHCompressedBitmap();
            for (int k = 0; k < 100; ++k)
                x.set(k * gap);
            for (int k = 0; k < 100 * gap; ++k)
                if (x.get(k)) {
                    if (k % gap != 0)
                        throw new RuntimeException(
                                "spotted an extra set bit at "
                                        + k + " gap = "
                                        + gap
                        );
                } else if (k % gap == 0)
                    throw new RuntimeException(
                            "missed a set bit " + k
                                    + " gap = " + gap
                    );
        }
    }

    @SuppressWarnings({"deprecation", "boxing"})
    @Test
    public void OKaserBugReportJuly2013() {
        System.out.println("testing OKaserBugReportJuly2013");
        int[][] data = {{}, {5, 6, 7, 8, 9}, {1}, {2},
                {2, 5, 7}, {1}, {2}, {1, 6, 9},
                {1, 3, 4, 6, 8, 9}, {1, 3, 4, 6, 8, 9},
                {1, 3, 6, 8, 9}, {2, 5, 7}, {2, 5, 7},
                {1, 3, 9}, {3, 8, 9}};

        EWAHCompressedBitmap[] toBeOred = new EWAHCompressedBitmap[data.length];
        Set<Integer> bruteForceAnswer = new HashSet<Integer>();
        for (int i = 0; i < toBeOred.length; ++i) {
            toBeOred[i] = new EWAHCompressedBitmap();
            for (int j : data[i]) {
                toBeOred[i].set(j);
                bruteForceAnswer.add(j);
            }
            toBeOred[i].setSizeInBits(1000, false);
        }
        long rightcard = bruteForceAnswer.size();
        EWAHCompressedBitmap e1 = FastAggregation.or(toBeOred);
        Assert.assertEquals(rightcard, e1.cardinality());
        EWAHCompressedBitmap e2 = FastAggregation.bufferedor(65536,
                toBeOred);
        Assert.assertEquals(rightcard, e2.cardinality());
        EWAHCompressedBitmap foo = new EWAHCompressedBitmap();
        FastAggregation.orToContainer(foo, toBeOred);
        Assert.assertEquals(rightcard, foo.cardinality());
    }
    
    public static Iterator toIterator(final EWAHCompressedBitmap[] bitmaps) {
    	return new Iterator() {
    		int k = 0;

			@Override
			public boolean hasNext() {
				return k < bitmaps.length;
			}

			@Override
			public Object next() {
				return bitmaps[k++];
			}
			
			@Override
			public void remove() {
				// nothing
			}
    	};
    }
    
    @Test
    public void fastand() {
        int[][] data = { {5, 6, 7, 8, 9}, {1, 5}, {2, 5}};

        EWAHCompressedBitmap[] bitmaps = new EWAHCompressedBitmap[data.length];
        
        for (int i = 0; i < bitmaps.length; ++i) {
        	bitmaps[i] = new EWAHCompressedBitmap();
            for (int j : data[i]) {
            	bitmaps[i].set(j);
            }
            bitmaps[i].setSizeInBits(1000, false);
        }
        EWAHCompressedBitmap and1 = FastAggregation.bufferedand(1024, bitmaps[0],bitmaps[1],bitmaps[2]);
        EWAHCompressedBitmap and2 = new  EWAHCompressedBitmap();
        FastAggregation.bufferedandWithContainer(and2, 32, bitmaps[0],bitmaps[1],bitmaps[2]);
        EWAHCompressedBitmap and3 = EWAHCompressedBitmap.and(bitmaps[0],bitmaps[1],bitmaps[2]);
        System.out.println(and1.sizeInBits());
        System.out.println(and2.sizeInBits());
        System.out.println(and3.sizeInBits());
        assertEqualsPositions(and1, and2);
        assertEqualsPositions(and2, and3);
    }


    @Test
    public void fastagg() {
        int[][] data = {{}, {5, 6, 7, 8, 9}, {1}, {2}};

        EWAHCompressedBitmap[] bitmaps = new EWAHCompressedBitmap[data.length];
        
        for (int i = 0; i < bitmaps.length; ++i) {
        	bitmaps[i] = new EWAHCompressedBitmap();
            for (int j : data[i]) {
            	bitmaps[i].set(j);
            }
            bitmaps[i].setSizeInBits(1000, false);
        }
        
        EWAHCompressedBitmap or1 = FastAggregation.bufferedor(1024, bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap or2 = FastAggregation.or(bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap or3 = FastAggregation.bufferedor(1024, bitmaps);
        EWAHCompressedBitmap or4 = FastAggregation.or(bitmaps);
        EWAHCompressedBitmap or5 = FastAggregation.or(toIterator(bitmaps));
        EWAHCompressedBitmap or6 = new EWAHCompressedBitmap();
        FastAggregation.orToContainer(or6,  bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);

        assertEquals(or1,or2);
        assertEquals(or2,or3);        
        assertEquals(or3,or4);        
        assertEquals(or4,or5);       
        assertEquals(or5,or6);       

        EWAHCompressedBitmap xor1 = FastAggregation.bufferedxor(1024, bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap xor2 = FastAggregation.xor(bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap xor3 = FastAggregation.bufferedxor(1024, bitmaps);
        EWAHCompressedBitmap xor4 = FastAggregation.xor(bitmaps);
        EWAHCompressedBitmap xor5 = FastAggregation.xor(toIterator(bitmaps));
        EWAHCompressedBitmap xor6 = new EWAHCompressedBitmap();
        FastAggregation.orToContainer(xor6,  bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);

        assertEquals(xor1,xor2);
        assertEquals(xor2,xor3);        
        assertEquals(xor3,xor4);        
        assertEquals(xor4,xor5);        
        assertEquals(xor5,xor6);       
    }

    @Test
    public void testSizeInBitsWithAnd() {
        System.out.println("testing SizeInBitsWithAnd");
        EWAHCompressedBitmap a = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBits(10, false);
        b.setSizeInBits(10, false);

        EWAHCompressedBitmap and = a.and(b);
        Assert.assertEquals(10, and.sizeInBits());
        EWAHCompressedBitmap and2 = EWAHCompressedBitmap.and(a, b);
        Assert.assertEquals(10, and2.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithAndNot() {
        System.out.println("testing SizeInBitsWithAndNot");
        EWAHCompressedBitmap a = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBits(10, false);
        b.setSizeInBits(10, false);

        EWAHCompressedBitmap and = a.andNot(b);
        Assert.assertEquals(10, and.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithOr() {
        System.out.println("testing SizeInBitsWithOr");
        EWAHCompressedBitmap a = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBits(10, false);
        b.setSizeInBits(10, false);

        EWAHCompressedBitmap or = a.or(b);
        Assert.assertEquals(10, or.sizeInBits());
        EWAHCompressedBitmap or2 = EWAHCompressedBitmap.or(a, b);
        Assert.assertEquals(10, or2.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithXor() {
        System.out.println("testing SizeInBitsWithXor");
        EWAHCompressedBitmap a = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBits(10, false);
        b.setSizeInBits(10, false);

        EWAHCompressedBitmap xor = a.xor(b);
        Assert.assertEquals(10, xor.sizeInBits());
        EWAHCompressedBitmap xor2 = EWAHCompressedBitmap.xor(a, b);
        Assert.assertEquals(10, xor2.sizeInBits());
    }

    @Test
    public void testDebugSetSizeInBitsTest() {
        System.out.println("testing DebugSetSizeInBits");
        EWAHCompressedBitmap b = new EWAHCompressedBitmap();

        b.set(4);

        b.setSizeInBits(6, true);

        List<Integer> positions = b.toList();

        Assert.assertEquals(2, positions.size());
        Assert.assertEquals(Integer.valueOf(4), positions.get(0));
        Assert.assertEquals(Integer.valueOf(5), positions.get(1));

        Iterator<Integer> iterator = b.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(Integer.valueOf(4), iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(Integer.valueOf(5), iterator.next());
        Assert.assertFalse(iterator.hasNext());

        IntIterator intIterator = b.intIterator();
        Assert.assertTrue(intIterator.hasNext());
        Assert.assertEquals(4, intIterator.next());
        Assert.assertTrue(intIterator.hasNext());
        Assert.assertEquals(5, intIterator.next());
        Assert.assertFalse(intIterator.hasNext());

    }

    /**
     * Created: 2/4/11 6:03 PM By: Arnon Moscona.
     */
    @Test
    public void EwahIteratorProblem() {
        System.out.println("testing ArnonMoscona");
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        for (int i = 9434560; i <= 9435159; i++) {
            bitmap.set(i);
        }
        IntIterator iterator = bitmap.intIterator();
        List<Integer> v = bitmap.toList();
        int[] array = bitmap.toArray();
        for (int k = 0; k < v.size(); ++k) {
            Assert.assertTrue(array[k] == v.get(k));
            Assert.assertTrue(iterator.hasNext());
            final int ival = iterator.next();
            final int vval = v.get(k);
            Assert.assertTrue(ival == vval);
        }
        Assert.assertTrue(!iterator.hasNext());
        //
        for (int k = 2; k <= 1024; k *= 2) {
            int[] bitsToSet = createSortedIntArrayOfBitsToSet(k,
                    434455 + 5 * k);
            EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
            for (int i : bitsToSet) {
                ewah.set(i);
            }
            equal(ewah.iterator(), bitsToSet);
        }
    }

    
    @Test
    public void shiftTest() {
        System.out.println("testing shifts");
        for (int k = 2; k <= 4096; k *= 2) {
            int[] bitsToSet = createSortedIntArrayOfBitsToSet(k,
                    434455 + 5 * k);
            EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
            for (int i : bitsToSet) {
                ewah.set(i);
            }
            for(int b = 0; b < 128; ++b) {
                EWAHCompressedBitmap ewahs = ewah.shift(b);
                int[] sb = ewahs.toArray();
                for(int z = 0; z < sb.length; ++z)
                    if(sb[z] != bitsToSet[z] + b) throw new RuntimeException("bug");
            }
            for(int z = 0; z < 256;++z) {
                ewah.set(z);
            }
            bitsToSet = ewah.toArray();
            for(int b = 0; b < 128; ++b) {
                EWAHCompressedBitmap ewahs = ewah.shift(b);
                int[] sb = ewahs.toArray();
                for(int z = 0; z < sb.length; ++z)
                    if(sb[z] != bitsToSet[z] + b) throw new RuntimeException("bug");
            }
        }
    }
    /**
     * Test submitted by Gregory Ssi-Yan-Kai
     */
    @Test
    public void SsiYanKaiTest() {
        System.out.println("testing SsiYanKaiTest");
        EWAHCompressedBitmap a = EWAHCompressedBitmap.bitmapOf(39935,
                39936, 39937, 39938, 39939, 39940, 39941, 39942, 39943,
                39944, 39945, 39946, 39947, 39948, 39949, 39950, 39951,
                39952, 39953, 39954, 39955, 39956, 39957, 39958, 39959,
                39960, 39961, 39962, 39963, 39964, 39965, 39966, 39967,
                39968, 39969, 39970, 39971, 39972, 39973, 39974, 39975,
                39976, 39977, 39978, 39979, 39980, 39981, 39982, 39983,
                39984, 39985, 39986, 39987, 39988, 39989, 39990, 39991,
                39992, 39993, 39994, 39995, 39996, 39997, 39998, 39999,
                40000, 40001, 40002, 40003, 40004, 40005, 40006, 40007,
                40008, 40009, 40010, 40011, 40012, 40013, 40014, 40015,
                40016, 40017, 40018, 40019, 40020, 40021, 40022, 40023,
                40024, 40025, 40026, 40027, 40028, 40029, 40030, 40031,
                40032, 40033, 40034, 40035, 40036, 40037, 40038, 40039,
                40040, 40041, 40042, 40043, 40044, 40045, 40046, 40047,
                40048, 40049, 40050, 40051, 40052, 40053, 40054, 40055,
                40056, 40057, 40058, 40059, 40060, 40061, 40062, 40063,
                40064, 40065, 40066, 40067, 40068, 40069, 40070, 40071,
                40072, 40073, 40074, 40075, 40076, 40077, 40078, 40079,
                40080, 40081, 40082, 40083, 40084, 40085, 40086, 40087,
                40088, 40089, 40090, 40091, 40092, 40093, 40094, 40095,
                40096, 40097, 40098, 40099, 40100);
        EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf(39935,
                39936, 39937, 39938, 39939, 39940, 39941, 39942, 39943,
                39944, 39945, 39946, 39947, 39948, 39949, 39950, 39951,
                39952, 39953, 39954, 39955, 39956, 39957, 39958, 39959,
                39960, 39961, 39962, 39963, 39964, 39965, 39966, 39967,
                39968, 39969, 39970, 39971, 39972, 39973, 39974, 39975,
                39976, 39977, 39978, 39979, 39980, 39981, 39982, 39983,
                39984, 39985, 39986, 39987, 39988, 39989, 39990, 39991,
                39992, 39993, 39994, 39995, 39996, 39997, 39998, 39999,
                270000);
        LinkedHashSet<Integer> aPositions = new LinkedHashSet<Integer>(
                a.toList());
        int intersection = 0;
        EWAHCompressedBitmap inter = new EWAHCompressedBitmap();
        LinkedHashSet<Integer> bPositions = new LinkedHashSet<Integer>(
                b.toList());
        for (Integer integer : bPositions) {
            if (aPositions.contains(integer)) {
                inter.set(integer);
                ++intersection;
            }
        }
        inter.setSizeInBits(maxSizeInBits(a, b), false);
        EWAHCompressedBitmap and2 = a.and(b);
        if (!and2.equals(inter))
            throw new RuntimeException("intersections don't match");
        if (intersection != and2.cardinality())
            throw new RuntimeException("cardinalities don't match");
    }

    /**
     * Test inspired by William Habermaas.
     */
    @Test
    public void habermaasTest() throws Exception {
        System.out.println("testing habermaasTest");
        BitSet bitsetaa = new BitSet();
        EWAHCompressedBitmap aa = new EWAHCompressedBitmap();
        int[] val = {55400, 1000000, 1000128};
        for (int k = 0; k < val.length; ++k) {
            aa.set(val[k]);
            bitsetaa.set(val[k]);
        }
        equal(aa, bitsetaa);
        BitSet bitsetab = new BitSet();
        EWAHCompressedBitmap ab = new EWAHCompressedBitmap();
        for (int i = 4096; i < (4096 + 5); i++) {
            ab.set(i);
            bitsetab.set(i);
        }
        ab.set(99000);
        bitsetab.set(99000);
        ab.set(1000130);
        bitsetab.set(1000130);
        equal(ab, bitsetab);
        EWAHCompressedBitmap bb = aa.or(ab);
        EWAHCompressedBitmap bbAnd = aa.and(ab);
        EWAHCompressedBitmap abnot = ab.clone();
        abnot.not();
        EWAHCompressedBitmap bbAnd2 = aa.andNot(abnot);
        assertEquals(bbAnd2, bbAnd);
        BitSet bitsetbb = (BitSet) bitsetaa.clone();
        bitsetbb.or(bitsetab);
        BitSet bitsetbbAnd = (BitSet) bitsetaa.clone();
        bitsetbbAnd.and(bitsetab);
        equal(bbAnd, bitsetbbAnd);
        equal(bb, bitsetbb);
    }

    @Test
    public void testAndResultAppend() {
        System.out.println("testing AndResultAppend");
        EWAHCompressedBitmap bitmap1 = new EWAHCompressedBitmap();
        bitmap1.set(35);
        EWAHCompressedBitmap bitmap2 = new EWAHCompressedBitmap();
        bitmap2.set(35);
        bitmap2.set(130);

        EWAHCompressedBitmap resultBitmap = bitmap1.and(bitmap2);
        resultBitmap.set(131);

        bitmap1.set(131);
        assertEquals(bitmap1, resultBitmap);
    }

    /**
     * Test cardinality.
     */
    @Test
    public void testCardinality() {
        System.out.println("testing EWAH cardinality");
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(Integer.MAX_VALUE - 64);
        // System.out.format("Total Items %d\n", bitmap.cardinality());
        Assert.assertTrue(bitmap.cardinality() == 1);
    }

    /**
     * Test clear function
     */
    @Test
    public void testClear() {
        System.out.println("testing Clear");
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(5);
        bitmap.clear();
        bitmap.set(7);
        Assert.assertTrue(1 == bitmap.cardinality());
        Assert.assertTrue(1 == bitmap.toList().size());
        Assert.assertTrue(1 == bitmap.toArray().length);
        Assert.assertTrue(7 == bitmap.toList().get(0));
        Assert.assertTrue(7 == bitmap.toArray()[0]);
        bitmap.clear();
        bitmap.set(5000);
        Assert.assertTrue(1 == bitmap.cardinality());
        Assert.assertTrue(1 == bitmap.toList().size());
        Assert.assertTrue(1 == bitmap.toArray().length);
        Assert.assertTrue(5000 == bitmap.toList().get(0));
        bitmap.set(5001);
        bitmap.set(5005);
        bitmap.set(5100);
        bitmap.set(5500);
        bitmap.clear();
        bitmap.set(5);
        bitmap.set(7);
        bitmap.set(1000);
        bitmap.set(1001);
        Assert.assertTrue(4 == bitmap.cardinality());
        List<Integer> positions = bitmap.toList();
        Assert.assertTrue(4 == positions.size());
        Assert.assertTrue(5 == positions.get(0));
        Assert.assertTrue(7 == positions.get(1));
        Assert.assertTrue(1000 == positions.get(2));
        Assert.assertTrue(1001 == positions.get(3));
    }

    /**
     * Test ewah compressed bitmap.
     */
    @Test
    public void testEWAHCompressedBitmap() {
        System.out.println("testing EWAH");
        long zero = 0;
        long specialval = 1l | (1l << 4) | (1l << 63);
        long notzero = ~zero;
        EWAHCompressedBitmap myarray1 = new EWAHCompressedBitmap();
        myarray1.addWord(zero);
        myarray1.addWord(zero);
        myarray1.addWord(zero);
        myarray1.addWord(specialval);
        myarray1.addWord(specialval);
        myarray1.addWord(notzero);
        myarray1.addWord(zero);
        Assert.assertEquals(myarray1.toList().size(), 6 + 64);
        EWAHCompressedBitmap myarray2 = new EWAHCompressedBitmap();
        myarray2.addWord(zero);
        myarray2.addWord(specialval);
        myarray2.addWord(specialval);
        myarray2.addWord(notzero);
        myarray2.addWord(zero);
        myarray2.addWord(zero);
        myarray2.addWord(zero);
        Assert.assertEquals(myarray2.toList().size(), 6 + 64);
        List<Integer> data1 = myarray1.toList();
        List<Integer> data2 = myarray2.toList();
        Vector<Integer> logicalor = new Vector<Integer>();
        {
            HashSet<Integer> tmp = new HashSet<Integer>();
            tmp.addAll(data1);
            tmp.addAll(data2);
            logicalor.addAll(tmp);
        }
        Collections.sort(logicalor);
        Vector<Integer> logicaland = new Vector<Integer>();
        logicaland.addAll(data1);
        logicaland.retainAll(data2);
        Collections.sort(logicaland);
        EWAHCompressedBitmap arrayand = myarray1.and(myarray2);
        Assert.assertTrue(arrayand.toList().equals(logicaland));
        EWAHCompressedBitmap arrayor = myarray1.or(myarray2);
        Assert.assertTrue(arrayor.toList().equals(logicalor));
        EWAHCompressedBitmap arrayandbis = myarray2.and(myarray1);
        Assert.assertTrue(arrayandbis.toList().equals(logicaland));
        EWAHCompressedBitmap arrayorbis = myarray2.or(myarray1);
        Assert.assertTrue(arrayorbis.toList().equals(logicalor));
        EWAHCompressedBitmap x = new EWAHCompressedBitmap();
        for (Integer i : myarray1.toList()) {
            x.set(i);
        }
        Assert.assertTrue(x.toList().equals(
                myarray1.toList()));
        x = new EWAHCompressedBitmap();
        for (Integer i : myarray2.toList()) {
            x.set(i);
        }
        Assert.assertTrue(x.toList().equals(
                myarray2.toList()));
        x = new EWAHCompressedBitmap();
        for (Iterator<Integer> k = myarray1.iterator(); k.hasNext(); ) {
            x.set(extracted(k));
        }
        Assert.assertTrue(x.toList().equals(
                myarray1.toList()));
        x = new EWAHCompressedBitmap();
        for (Iterator<Integer> k = myarray2.iterator(); k.hasNext(); ) {
            x.set(extracted(k));
        }
        Assert.assertTrue(x.toList().equals(
                myarray2.toList()));
    }

    /**
     * Test externalization.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testExternalization() throws Exception {
        System.out.println("testing EWAH externalization");
        EWAHCompressedBitmap ewcb = new EWAHCompressedBitmap();
        int[] val = {5, 4400, 44600, 55400, 1000000};
        for (int k = 0; k < val.length; ++k) {
            ewcb.set(val[k]);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bos);
        ewcb.writeExternal(oo);
        oo.close();
        ewcb = new EWAHCompressedBitmap();
        ByteArrayInputStream bis = new ByteArrayInputStream(
                bos.toByteArray());
        ewcb.readExternal(new ObjectInputStream(bis));
        List<Integer> result = ewcb.toList();
        Assert.assertTrue(val.length == result.size());
        for (int k = 0; k < val.length; ++k) {
            Assert.assertTrue(result.get(k) == val[k]);
        }
    }

    @Test
    public void testExtremeRange() {
        System.out.println("testing EWAH at its extreme range");
        int N = 1024;
        EWAHCompressedBitmap myarray1 = new EWAHCompressedBitmap();
        for (int i = 0; i < N; ++i) {
            myarray1.set(Integer.MAX_VALUE - 64 - N + i);
            Assert.assertTrue(myarray1.cardinality() == i + 1);
            int[] val = myarray1.toArray();
            Assert.assertTrue(val[0] == Integer.MAX_VALUE - 64 - N);
        }
    }

    /**
     * Test the intersects method
     */
    @Test
    public void testIntersectsMethod() {
        System.out.println("testing Intersets Bug");
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(1);
        EWAHCompressedBitmap bitmap2 = new EWAHCompressedBitmap();
        bitmap2.set(1);
        bitmap2.set(11);
        bitmap2.set(111);
        bitmap2.set(1111111);
        bitmap2.set(11111111);
        Assert.assertTrue(bitmap.intersects(bitmap2));
        Assert.assertTrue(bitmap2.intersects(bitmap));

        EWAHCompressedBitmap bitmap3 = new EWAHCompressedBitmap();
        bitmap3.set(101);
        EWAHCompressedBitmap bitmap4 = new EWAHCompressedBitmap();
        for (int i = 0; i < 100; i++) {
            bitmap4.set(i);
        }
        Assert.assertFalse(bitmap3.intersects(bitmap4));
        Assert.assertFalse(bitmap4.intersects(bitmap3));

        EWAHCompressedBitmap bitmap5 = new EWAHCompressedBitmap();
        bitmap5.set(0);
        bitmap5.set(10);
        bitmap5.set(20);
        EWAHCompressedBitmap bitmap6 = new EWAHCompressedBitmap();
        bitmap6.set(1);
        bitmap6.set(11);
        bitmap6.set(21);
        bitmap6.set(1111111);
        bitmap6.set(11111111);
        Assert.assertFalse(bitmap5.intersects(bitmap6));
        Assert.assertFalse(bitmap6.intersects(bitmap5));

        bitmap5.set(21);
        Assert.assertTrue(bitmap5.intersects(bitmap6));
        Assert.assertTrue(bitmap6.intersects(bitmap5));

        EWAHCompressedBitmap bitmap7 = new EWAHCompressedBitmap();
        bitmap7.set(1);
        bitmap7.set(10);
        bitmap7.set(20);
        bitmap7.set(1111111);
        bitmap7.set(11111111);
        EWAHCompressedBitmap bitmap8 = new EWAHCompressedBitmap();
        for (int i = 0; i < 1000; i++) {
            if (i != 1 && i != 10 && i != 20) {
                bitmap8.set(i);
            }
        }
        Assert.assertFalse(bitmap7.intersects(bitmap8));
        Assert.assertFalse(bitmap8.intersects(bitmap7));
    }

    /**
     * as per renaud.delbru, Feb 12, 2009 this might throw an error out of
     * bound exception.
     */
    @Test
    public void testLargeEWAHCompressedBitmap() {
        System.out.println("testing EWAH over a large array");
        EWAHCompressedBitmap myarray1 = new EWAHCompressedBitmap();
        int N = 11000000;
        for (int i = 0; i < N; ++i) {
            myarray1.set(i);
        }
        Assert.assertTrue(myarray1.sizeInBits() == N);
    }

    /**
     * Test massive and.
     */
    @Test
    public void testMassiveAnd() {
        System.out.println("testing massive logical and");
        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[1024];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap answer = ewah[0];
        for (int k = 1; k < ewah.length; ++k)
            answer = answer.and(ewah[k]);
        // result should be empty
        if (answer.toList().size() != 0)
            System.out.println(answer.toDebugString());
        Assert.assertTrue(answer.toList().size() == 0);
        Assert.assertTrue(EWAHCompressedBitmap.and(ewah).toList()
                .size() == 0);
    }

    /**
     * Test massive and not.
     */
    @Test
    public void testMassiveAndNot() throws Exception {
        System.out.println("testing massive and not");
        final int N = 1024;
        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap answer = ewah[0];
        EWAHCompressedBitmap answer2 = ewah[0];
        for (int k = 1; k < ewah.length; ++k) {
            answer = answer.andNot(ewah[k]);
            EWAHCompressedBitmap copy = ewah[k].clone();
            copy.not();
            answer2.and(copy);
            assertEqualsPositions(answer, answer2);
        }
    }

    /**
     * Test massive or.
     */
    @Test
    public void testMassiveOr() {
        System.out
                .println("testing massive logical or (can take a couple of minutes)");
        final int N = 128;
        for (int howmany = 512; howmany <= 10000; howmany *= 2) {
            EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
            BitSet[] bset = new BitSet[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap();
            for (int k = 0; k < bset.length; ++k)
                bset[k] = new BitSet();
            for (int k = 0; k < N; ++k)
                assertEqualsPositions(bset[k], ewah[k]);
            for (int k = 0; k < howmany; ++k) {
                ewah[(k + 2 * k * k) % ewah.length].set(k);
                bset[(k + 2 * k * k) % ewah.length].set(k);
            }
            for (int k = 0; k < N; ++k)
                assertEqualsPositions(bset[k], ewah[k]);
            EWAHCompressedBitmap answer = ewah[0];
            BitSet bitsetanswer = bset[0];
            for (int k = 1; k < ewah.length; ++k) {
                EWAHCompressedBitmap tmp = answer.or(ewah[k]);
                bitsetanswer.or(bset[k]);
                answer = tmp;
                assertEqualsPositions(bitsetanswer, answer);
            }
            assertEqualsPositions(bitsetanswer, answer);
            assertEqualsPositions(bitsetanswer,
                    EWAHCompressedBitmap.or(ewah));
            int k = 0;
            for (int j : answer) {
                if (k != j)
                    System.out.println(answer
                            .toDebugString());
                Assert.assertEquals(k, j);
                k += 1;
            }
        }
    }

    @Test
    public void testsetSizeInBits() {
        System.out.println("testing setSizeInBits");
        for (int k = 0; k < 4096; ++k) {
            EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
            ewah.setSizeInBits(k, false);
            Assert.assertEquals(ewah.sizeInBits(), k);
            Assert.assertEquals(ewah.cardinality(), 0);
            EWAHCompressedBitmap ewah2 = new EWAHCompressedBitmap();
            ewah2.setSizeInBits(k, false);
            Assert.assertEquals(ewah2.sizeInBits(), k);
            Assert.assertEquals(ewah2.cardinality(), 0);
            EWAHCompressedBitmap ewah3 = new EWAHCompressedBitmap();
            for (int i = 0; i < k; ++i) {
                ewah3.set(i);
            }
            Assert.assertEquals(ewah3.sizeInBits(), k);
            Assert.assertEquals(ewah3.cardinality(), k);
            EWAHCompressedBitmap ewah4 = new EWAHCompressedBitmap();
            ewah4.setSizeInBits(k, true);
            Assert.assertEquals(ewah4.sizeInBits(), k);
            Assert.assertEquals(ewah4.cardinality(), k);
        }
    }

    /**
     * Test massive xor.
     */
    @Test
    public void testMassiveXOR() {
        System.out
                .println("testing massive xor (can take a couple of minutes)");
        final int N = 16;
        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
        BitSet[] bset = new BitSet[N];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap();
        for (int k = 0; k < bset.length; ++k)
            bset[k] = new BitSet();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
            bset[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap answer = ewah[0];
        BitSet bitsetanswer = bset[0];
        for (int k = 1; k < ewah.length; ++k) {
            answer = answer.xor(ewah[k]);
            bitsetanswer.xor(bset[k]);
            assertEqualsPositions(bitsetanswer, answer);
        }
        int k = 0;
        for (int j : answer) {
            if (k != j)
                System.out.println(answer.toDebugString());
            Assert.assertEquals(k, j);
            k += 1;
        }
    }

    @Test
    public void testMultiAnd() {
        System.out.println("testing MultiAnd");
        // test bitmap3 has a literal word while bitmap1/2 have a run of
        // 1
        EWAHCompressedBitmap bitmap1 = new EWAHCompressedBitmap();
        bitmap1.addStreamOfEmptyWords(true, 1000);
        EWAHCompressedBitmap bitmap2 = new EWAHCompressedBitmap();
        bitmap2.addStreamOfEmptyWords(true, 2000);
        EWAHCompressedBitmap bitmap3 = new EWAHCompressedBitmap();
        bitmap3.set(500);
        bitmap3.set(502);
        bitmap3.set(504);

        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // equal
        bitmap1 = new EWAHCompressedBitmap();
        bitmap1.set(35);
        bitmap2 = new EWAHCompressedBitmap();
        bitmap2.set(35);
        bitmap3 = new EWAHCompressedBitmap();
        bitmap3.set(35);
        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // same number of words for each
        bitmap3.set(63);
        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // one word bigger
        bitmap3.set(64);
        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // two words bigger
        bitmap3.set(130);
        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // test that result can still be appended to
        EWAHCompressedBitmap resultBitmap = EWAHCompressedBitmap.and(
                bitmap1, bitmap2, bitmap3);

        resultBitmap.set(131);

        bitmap1.set(131);
        assertEquals(bitmap1, resultBitmap);

        final int N = 128;
        for (int howmany = 512; howmany <= 10000; howmany *= 2) {
            EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap();
            for (int k = 0; k < howmany; ++k) {
                ewah[(k + 2 * k * k) % ewah.length].set(k);
            }
            for (int k = 1; k <= ewah.length; ++k) {
                EWAHCompressedBitmap[] shortewah = new EWAHCompressedBitmap[k];
                System.arraycopy(ewah, 0, shortewah, 0, k);
                assertAndEquals(shortewah);
            }
        }
    }

    @Test
    public void testMultiOr() {
        System.out.println("testing MultiOr");
        // test bitmap3 has a literal word while bitmap1/2 have a run of
        // 0
        EWAHCompressedBitmap bitmap1 = new EWAHCompressedBitmap();
        bitmap1.set(1000);
        EWAHCompressedBitmap bitmap2 = new EWAHCompressedBitmap();
        bitmap2.set(2000);
        EWAHCompressedBitmap bitmap3 = new EWAHCompressedBitmap();
        bitmap3.set(500);
        bitmap3.set(502);
        bitmap3.set(504);

        EWAHCompressedBitmap expected = bitmap1.or(bitmap2).or(bitmap3);

        assertEquals(expected,
                EWAHCompressedBitmap.or(bitmap1, bitmap2, bitmap3));

        final int N = 128;
        for (int howmany = 512; howmany <= 10000; howmany *= 2) {
            EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap();
            for (int k = 0; k < howmany; ++k) {
                ewah[(k + 2 * k * k) % ewah.length].set(k);
            }
            for (int k = 1; k <= ewah.length; ++k) {
                EWAHCompressedBitmap[] shortewah = new EWAHCompressedBitmap[k];
                System.arraycopy(ewah, 0, shortewah, 0, k);
                assertOrEquals(shortewah);
            }
        }

    }

    /**
     * Test not. (Based on an idea by Ciaran Jessup)
     */
    @Test
    public void testNot() {
        System.out.println("testing not");
        EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
        for (int i = 0; i <= 184; ++i) {
            ewah.set(i);
        }
        Assert.assertEquals(ewah.cardinality(), 185);
        ewah.not();
        Assert.assertEquals(ewah.cardinality(), 0);
    }

    @Test
    public void testOrCardinality() {
        System.out.println("testing Or Cardinality");
        for (int N = 0; N < 1024; ++N) {
            EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
            for (int i = 0; i < N; i++) {
                bitmap.set(i);
            }
            bitmap.set(1025);
            bitmap.set(1026);
            Assert.assertEquals(N + 2, bitmap.cardinality());
            EWAHCompressedBitmap orbitmap = bitmap.or(bitmap);
            assertEquals(orbitmap, bitmap);
            Assert.assertEquals(N + 2, orbitmap.cardinality());

            Assert.assertEquals(N + 2, bitmap
                    .orCardinality(new EWAHCompressedBitmap()));
        }
    }

    /**
     * Test sets and gets.
     */
    @Test
    public void testSetGet() {
        System.out.println("testing EWAH set/get");
        EWAHCompressedBitmap ewcb = new EWAHCompressedBitmap();
        int[] val = {5, 4400, 44600, 55400, 1000000};
        for (int k = 0; k < val.length; ++k) {
            ewcb.set(val[k]);
        }
        List<Integer> result = ewcb.toList();
        Assert.assertTrue(val.length == result.size());
        for (int k = 0; k < val.length; ++k) {
            Assert.assertEquals(result.get(k).intValue(), val[k]);
        }
    }

    @Test
    public void testHashCode() throws Exception {
        System.out.println("testing hashCode");
        EWAHCompressedBitmap ewcb = EWAHCompressedBitmap.bitmapOf(50,
                70).and(EWAHCompressedBitmap.bitmapOf(50, 1000));
        EWAHCompressedBitmap expectedBitmap = EWAHCompressedBitmap.bitmapOf(50);
        expectedBitmap.setSizeInBits(1000, false);
        Assert.assertEquals(expectedBitmap, ewcb);
        Assert.assertEquals(expectedBitmap.hashCode(), ewcb.hashCode());
        ewcb.addWord(~0l);
        EWAHCompressedBitmap ewcb2 = ewcb.clone();
        ewcb2.addWord(0);
        Assert.assertEquals(ewcb.hashCode(), ewcb2.hashCode());

    }

    @Test
    public void testSetSizeInBits() {
        System.out.println("testing SetSizeInBits");
        testSetSizeInBits(130, 131);
        testSetSizeInBits(63, 64);
        testSetSizeInBits(64, 65);
        testSetSizeInBits(64, 128);
        testSetSizeInBits(35, 131);
        testSetSizeInBits(130, 400);
        testSetSizeInBits(130, 191);
        testSetSizeInBits(130, 192);
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(31);
        bitmap.setSizeInBits(130, false);
        bitmap.set(131);
        BitSet jdkBitmap = new BitSet();
        jdkBitmap.set(31);
        jdkBitmap.set(131);
        assertEquals(jdkBitmap, bitmap);
    }

    /**
     * Test with parameters.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testWithParameters() throws IOException {
        System.out
                .println("These tests can run for several minutes. Please be patient.");
        for (int k = 2; k < 1 << 24; k *= 8)
            shouldSetBits(k);
        PolizziTest(64);
        PolizziTest(128);
        PolizziTest(256);
        System.out.println("Your code is probably ok.");
    }

    /**
     * Pseudo-non-deterministic test inspired by S.J.vanSchaik. (Yes,
     * non-deterministic tests are bad, but the test is actually
     * deterministic.)
     */
    @Test
    public void vanSchaikTest() {
        System.out
                .println("testing vanSchaikTest (this takes some time)");
        final int totalNumBits = 32768;
        final double odds = 0.9;
        Random rand = new Random(323232323);
        for (int t = 0; t < 100; t++) {
            int numBitsSet = 0;
            EWAHCompressedBitmap cBitMap = new EWAHCompressedBitmap();
            for (int i = 0; i < totalNumBits; i++) {
                if (rand.nextDouble() < odds) {
                    cBitMap.set(i);
                    numBitsSet++;
                }
            }
            Assert.assertEquals(cBitMap.cardinality(), numBitsSet);
        }

    }

    /**
     * Function used in a test inspired by Federico Fissore.
     *
     * @param size the number of set bits
     * @param seed the random seed
     * @return the pseudo-random array int[]
     */
    public static int[] createSortedIntArrayOfBitsToSet(int size, int seed) {
        Random random = new Random(seed);
        // build raw int array
        int[] bits = new int[size];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = random.nextInt(TEST_BS_SIZE);
        }
        // might generate duplicates
        Arrays.sort(bits);
        // first count how many distinct values
        int counter = 0;
        int oldx = -1;
        for (int x : bits) {
            if (x != oldx)
                ++counter;
            oldx = x;
        }
        // then construct new array
        int[] answer = new int[counter];
        counter = 0;
        oldx = -1;
        for (int x : bits) {
            if (x != oldx) {
                answer[counter] = x;
                ++counter;
            }
            oldx = x;
        }
        return answer;
    }

    /**
     * Test inspired by Bilal Tayara
     */
    @Test
    public void TayaraTest() {
        System.out.println("Tayara test");
        for (int offset = 64; offset < (1 << 30); offset *= 2) {
            EWAHCompressedBitmap a = new EWAHCompressedBitmap();
            EWAHCompressedBitmap b = new EWAHCompressedBitmap();
            for (int k = 0; k < 64; ++k) {
                a.set(offset + k);
                b.set(offset + k);
            }
            if (!a.and(b).equals(a))
                throw new RuntimeException("bug");
            if (!a.or(b).equals(a))
                throw new RuntimeException("bug");
        }
    }

    @Test
    public void TestCloneEwahCompressedBitArray() throws Exception {
        System.out.println("testing EWAH clone");
        EWAHCompressedBitmap a = new EWAHCompressedBitmap();
        a.set(410018);
        a.set(410019);
        a.set(410020);
        a.set(410021);
        a.set(410022);
        a.set(410023);

        EWAHCompressedBitmap b;

        b = a.clone();

        a.setSizeInBits(487123, false);
        b.setSizeInBits(487123, false);

        Assert.assertTrue(a.equals(b));
    }

    /**
     * a non-deterministic test proposed by Marc Polizzi.
     *
     * @param maxlength the maximum uncompressed size of the bitmap
     */
    public static void PolizziTest(int maxlength) {
        System.out.println("Polizzi test with max length = "
                + maxlength);
        for (int k = 0; k < 10000; ++k) {
            final Random rnd = new Random();
            final EWAHCompressedBitmap ewahBitmap1 = new EWAHCompressedBitmap();
            final BitSet jdkBitmap1 = new BitSet();
            final EWAHCompressedBitmap ewahBitmap2 = new EWAHCompressedBitmap();
            final BitSet jdkBitmap2 = new BitSet();
            final EWAHCompressedBitmap ewahBitmap3 = new EWAHCompressedBitmap();
            final BitSet jdkBitmap3 = new BitSet();
            final int len = rnd.nextInt(maxlength);
            for (int pos = 0; pos < len; pos++) { // random ***
                // number of bits
                // set ***
                if (rnd.nextInt(7) == 0) { // random ***
                    // increasing ***
                    // values
                    ewahBitmap1.set(pos);
                    jdkBitmap1.set(pos);
                }
                if (rnd.nextInt(11) == 0) { // random ***
                    // increasing ***
                    // values
                    ewahBitmap2.set(pos);
                    jdkBitmap2.set(pos);
                }
                if (rnd.nextInt(7) == 0) { // random ***
                    // increasing ***
                    // values
                    ewahBitmap3.set(pos);
                    jdkBitmap3.set(pos);
                }
            }
            assertEquals(jdkBitmap1, ewahBitmap1);
            assertEquals(jdkBitmap2, ewahBitmap2);
            assertEquals(jdkBitmap3, ewahBitmap3);
            // XOR
            {
                final EWAHCompressedBitmap xorEwahBitmap = ewahBitmap1
                        .xor(ewahBitmap2);
                final BitSet xorJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                xorJdkBitmap.xor(jdkBitmap2);
                assertEquals(xorJdkBitmap, xorEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap andEwahBitmap = ewahBitmap1
                        .and(ewahBitmap2);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap andEwahBitmap = ewahBitmap2
                        .and(ewahBitmap1);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
                assertEquals(andJdkBitmap,
                        EWAHCompressedBitmap.and(ewahBitmap1,
                                ewahBitmap2)
                );
            }
            // MULTI AND
            {
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andJdkBitmap.and(jdkBitmap2);
                andJdkBitmap.and(jdkBitmap3);
                assertEquals(andJdkBitmap,
                        EWAHCompressedBitmap.and(ewahBitmap1,
                                ewahBitmap2, ewahBitmap3)
                );
                assertEquals(andJdkBitmap,
                        EWAHCompressedBitmap.and(ewahBitmap3,
                                ewahBitmap2, ewahBitmap1)
                );
                Assert.assertEquals(andJdkBitmap.cardinality(),
                        EWAHCompressedBitmap.andCardinality(
                                ewahBitmap1, ewahBitmap2,
                                ewahBitmap3)
                );
            }
            // AND NOT
            {
                final EWAHCompressedBitmap andNotEwahBitmap = ewahBitmap1
                        .andNot(ewahBitmap2);
                final BitSet andNotJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andNotJdkBitmap.andNot(jdkBitmap2);
                assertEquals(andNotJdkBitmap, andNotEwahBitmap);
            }
            // AND NOT
            {
                final EWAHCompressedBitmap andNotEwahBitmap = ewahBitmap2
                        .andNot(ewahBitmap1);
                final BitSet andNotJdkBitmap = (BitSet) jdkBitmap2
                        .clone();
                andNotJdkBitmap.andNot(jdkBitmap1);
                assertEquals(andNotJdkBitmap, andNotEwahBitmap);
            }
            // OR
            {
                final EWAHCompressedBitmap orEwahBitmap = ewahBitmap1
                        .or(ewahBitmap2);
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                orJdkBitmap.or(jdkBitmap2);
                assertEquals(orJdkBitmap, orEwahBitmap);
                assertEquals(orJdkBitmap,
                        EWAHCompressedBitmap.or(ewahBitmap1,
                                ewahBitmap2)
                );
                Assert.assertEquals(orEwahBitmap.cardinality(),
                        ewahBitmap1.orCardinality(ewahBitmap2));
            }
            // OR
            {
                final EWAHCompressedBitmap orEwahBitmap = ewahBitmap2
                        .or(ewahBitmap1);
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                orJdkBitmap.or(jdkBitmap2);
                assertEquals(orJdkBitmap, orEwahBitmap);
            }
            // MULTI OR
            {
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                orJdkBitmap.or(jdkBitmap2);
                orJdkBitmap.or(jdkBitmap3);
                assertEquals(orJdkBitmap,
                        EWAHCompressedBitmap.or(ewahBitmap1,
                                ewahBitmap2, ewahBitmap3)
                );
                assertEquals(orJdkBitmap,
                        EWAHCompressedBitmap.or(ewahBitmap3,
                                ewahBitmap2, ewahBitmap1)
                );
                Assert.assertEquals(orJdkBitmap.cardinality(),
                        EWAHCompressedBitmap.orCardinality(
                                ewahBitmap1, ewahBitmap2,
                                ewahBitmap3)
                );
            }
        }
    }

    /**
     * Pseudo-non-deterministic test inspired by Federico Fissore.
     *
     * @param length the number of set bits in a bitmap
     */
    public static void shouldSetBits(int length) {
        System.out.println("testing shouldSetBits " + length);
        int[] bitsToSet = createSortedIntArrayOfBitsToSet(length,
                434222);
        EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
        System.out.println(" ... setting " + bitsToSet.length
                + " values");
        for (int i : bitsToSet) {
            ewah.set(i);
        }
        System.out.println(" ... verifying " + bitsToSet.length
                + " values");
        equal(ewah.iterator(), bitsToSet);
        System.out.println(" ... checking cardinality");
        Assert.assertEquals(bitsToSet.length, ewah.cardinality());
    }

    @Test
    public void testSizeInBits1() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.setSizeInBits(1, false);
        bitmap.not();
        Assert.assertEquals(1, bitmap.cardinality());
    }

    @Test
    public void testHasNextSafe() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(0);
        IntIterator it = bitmap.intIterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(0, it.next());
    }

    @Test
    public void testHasNextSafe2() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(0);
        IntIterator it = bitmap.intIterator();
        Assert.assertEquals(0, it.next());
    }

    @Test
    public void testInfiniteLoop() {
        System.out.println("Testing for an infinite loop");
        EWAHCompressedBitmap b1 = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b2 = new EWAHCompressedBitmap();
        EWAHCompressedBitmap b3 = new EWAHCompressedBitmap();
        b3.setSizeInBits(5, false);
        b1.set(2);
        b2.set(4);
        EWAHCompressedBitmap.and(b1, b2, b3);
        EWAHCompressedBitmap.or(b1, b2, b3);
    }

    @Test
    public void testSizeInBits2() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.setSizeInBits(1, true);
        bitmap.not();
        Assert.assertEquals(0, bitmap.cardinality());
    }

    private static void assertAndEquals(EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap expected = bitmaps[0];
        for (int i = 1; i < bitmaps.length; i++) {
            expected = expected.and(bitmaps[i]);
        }
        Assert.assertTrue(expected.equals(EWAHCompressedBitmap
                .and(bitmaps)));
    }

    private static void assertEquals(EWAHCompressedBitmap expected,
                                     EWAHCompressedBitmap actual) {
        Assert.assertEquals(expected.sizeInBits(), actual.sizeInBits());
        assertEqualsPositions(expected, actual);
    }

    private static void assertOrEquals(EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap expected = bitmaps[0];
        for (int i = 1; i < bitmaps.length; i++) {
            expected = expected.or(bitmaps[i]);
        }
        assertEquals(expected, EWAHCompressedBitmap.or(bitmaps));
    }

    /**
     * Extracted.
     *
     * @param bits the bits
     * @return the integer
     */
    private static Integer extracted(final Iterator<Integer> bits) {
        return bits.next();
    }

    private static void testSetSizeInBits(int size, int nextBit) {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.setSizeInBits(size, false);
        bitmap.set(nextBit);
        BitSet jdkBitmap = new BitSet();
        jdkBitmap.set(nextBit);
        assertEquals(jdkBitmap, bitmap);
    }

    /**
     * Assess equality between an uncompressed bitmap and a compressed one,
     * part of a test contributed by Marc Polizzi
     *
     * @param jdkBitmap  the uncompressed bitmap
     * @param ewahBitmap the compressed bitmap
     */
    static void assertCardinality(BitSet jdkBitmap,
                                  EWAHCompressedBitmap ewahBitmap) {
        final int c1 = jdkBitmap.cardinality();
        final int c2 = ewahBitmap.cardinality();
        Assert.assertEquals(c1, c2);
    }

    /**
     * Assess equality between an uncompressed bitmap and a compressed one,
     * part of a test contributed by Marc Polizzi.
     *
     * @param jdkBitmap  the uncompressed bitmap
     * @param ewahBitmap the compressed bitmap
     */
    static void assertEquals(BitSet jdkBitmap,
                             EWAHCompressedBitmap ewahBitmap) {
        assertEqualsIterator(jdkBitmap, ewahBitmap);
        assertEqualsPositions(jdkBitmap, ewahBitmap);
        assertCardinality(jdkBitmap, ewahBitmap);
    }

    static void assertEquals(int[] v, List<Integer> p) {
        assertEquals(p, v);
    }

    static void assertEquals(List<Integer> p, int[] v) {
        if (v.length != p.size())
            throw new RuntimeException("Different lengths   "
                    + v.length + " " + p.size());
        for (int k = 0; k < v.length; ++k)
            if (v[k] != p.get(k))
                throw new RuntimeException("expected equal at "
                        + k + " " + v[k] + " " + p.get(k));
    }

    //

    /**
     * Assess equality between an uncompressed bitmap and a compressed one,
     * part of a test contributed by Marc Polizzi
     *
     * @param jdkBitmap  the jdk bitmap
     * @param ewahBitmap the ewah bitmap
     */
    static void assertEqualsIterator(BitSet jdkBitmap,
                                     EWAHCompressedBitmap ewahBitmap) {
        final Vector<Integer> positions = new Vector<Integer>();
        final Iterator<Integer> bits = ewahBitmap.iterator();
        while (bits.hasNext()) {
            final int bit = extracted(bits);
            Assert.assertTrue(jdkBitmap.get(bit));
            positions.add(bit);
        }
        for (int pos = jdkBitmap.nextSetBit(0); pos >= 0; pos = jdkBitmap
                .nextSetBit(pos + 1)) {
            if (!positions.contains(new Integer(pos))) {
                throw new RuntimeException(
                        "iterator: bitset got different bits");
            }
        }
    }

    // part of a test contributed by Marc Polizzi

    /**
     * Assert equals positions.
     *
     * @param jdkBitmap  the jdk bitmap
     * @param ewahBitmap the ewah bitmap
     */
    static void assertEqualsPositions(BitSet jdkBitmap,
                                      EWAHCompressedBitmap ewahBitmap) {
        final List<Integer> positions = ewahBitmap.toList();
        for (int position : positions) {
            if (!jdkBitmap.get(position)) {
                throw new RuntimeException(
                        "positions: bitset got different bits");
            }
        }
        for (int pos = jdkBitmap.nextSetBit(0); pos >= 0; pos = jdkBitmap
                .nextSetBit(pos + 1)) {
            if (!positions.contains(new Integer(pos))) {
                throw new RuntimeException(
                        "positions: bitset got different bits");
            }
        }
        // we check again
        final int[] fastpositions = ewahBitmap.toArray();
        for (int position : fastpositions) {
            if (!jdkBitmap.get(position)) {
                throw new RuntimeException(
                        "positions: bitset got different bits with toArray");
            }
        }
        for (int pos = jdkBitmap.nextSetBit(0); pos >= 0; pos = jdkBitmap
                .nextSetBit(pos + 1)) {
            int index = Arrays.binarySearch(fastpositions, pos);
            if (index < 0)
                throw new RuntimeException(
                        "positions: bitset got different bits with toArray");
            if (fastpositions[index] != pos)
                throw new RuntimeException(
                        "positions: bitset got different bits with toArray");
        }
    }

    /**
     * Assert equals positions.
     *
     * @param ewahBitmap1 the ewah bitmap1
     * @param ewahBitmap2 the ewah bitmap2
     */
    static void assertEqualsPositions(EWAHCompressedBitmap ewahBitmap1,
                                      EWAHCompressedBitmap ewahBitmap2) {
        final List<Integer> positions1 = ewahBitmap1.toList();
        final List<Integer> positions2 = ewahBitmap2.toList();
        if (!positions1.equals(positions2))
            throw new RuntimeException(
                    "positions: alternative got different bits (two bitmaps)");
        //
        final int[] fastpositions1 = ewahBitmap1.toArray();
        assertEquals(fastpositions1, positions1);
        final int[] fastpositions2 = ewahBitmap2.toArray();
        assertEquals(fastpositions2, positions2);
        if (!Arrays.equals(fastpositions1, fastpositions2))
            throw new RuntimeException(
                    "positions: alternative got different bits with toArray but not with toList (two bitmaps)");
    }

    /**
     * Convenience function to assess equality between a compressed bitset
     * and an uncompressed bitset
     *
     * @param x the compressed bitset/bitmap
     * @param y the uncompressed bitset/bitmap
     */
    static void equal(EWAHCompressedBitmap x, BitSet y) {
        Assert.assertEquals(x.cardinality(), y.cardinality());
        for (int i : x.toList())
            Assert.assertTrue(y.get(i));
    }

    
    @Test
    public void insertTest() {
        EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
        for(int k = 0; k < 1<<20; ++k)
          ewah.addLiteralWord(0xF0);
        Assert.assertEquals(ewah.cardinality(), 4 * (1<<20));
    }
    
    /**
     * Convenience function to assess equality between an array and an
     * iterator over Integers
     *
     * @param i     the iterator
     * @param array the array
     */
    static void equal(Iterator<Integer> i, int[] array) {
        int cursor = 0;
        while (i.hasNext()) {
            int x = extracted(i);
            int y = array[cursor++];
            Assert.assertEquals(x, y);
        }
    }

    /**
     * The Constant MEGA: a large integer.
     */
    private static final int MEGA = 8 * 1024 * 1024;

    /**
     * The Constant TEST_BS_SIZE: used to represent the size of a large
     * bitmap.
     */
    private static final int TEST_BS_SIZE = 8 * MEGA;
}
