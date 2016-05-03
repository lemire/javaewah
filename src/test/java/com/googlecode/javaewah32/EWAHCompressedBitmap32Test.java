package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import com.googlecode.javaewah.ChunkIterator;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah.IntIterator;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.IntBuffer;
import java.util.*;

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.maxSizeInBits;
import static com.googlecode.javaewah32.EWAHCompressedBitmap32.WORD_IN_BITS;

/**
 * This class is used for basic unit testing.
 */
@SuppressWarnings("javadoc")
public class EWAHCompressedBitmap32Test {
	
	@Test
	public void swaptest() {
		EWAHCompressedBitmap32 x = EWAHCompressedBitmap32.bitmapOf(1,2,3);
		EWAHCompressedBitmap32 y = EWAHCompressedBitmap32.bitmapOf(1,2,3,4);
		x.swap(y);
		Assert.assertEquals(x.cardinality(),4);
		Assert.assertEquals(y.cardinality(),3);
	}

    @Test
    public void shiftByWordSizeBits() {
        int[] positions = { 10, 11, 12, 13 };
        EWAHCompressedBitmap32 bm1 = EWAHCompressedBitmap32.bitmapOf(positions);
        EWAHCompressedBitmap32 bm2 = bm1.shift(WORD_IN_BITS);

        EWAHCompressedBitmap32 bm3 = EWAHCompressedBitmap32.bitmapOf();
        for (int pos : positions) {
            bm3.set(pos + WORD_IN_BITS);
        }
        Assert.assertEquals(bm3, bm2);
    }

    @Test
    public void shiftbug001() {
        EWAHCompressedBitmap32 bm1 = EWAHCompressedBitmap32.bitmapOf(10, 11, 12, 13);
        EWAHCompressedBitmap32 bm2 =  bm1.shift(1);

        EWAHCompressedBitmap32 bm3 = bm1.or(bm2);
        EWAHCompressedBitmap32 bm4 = EWAHCompressedBitmap32.bitmapOf(10,11,12,13,14);
        Assert.assertEquals(bm3, bm4);
    }

    
    @Test
    public void shiftbug002() {
        EWAHCompressedBitmap32 bm1 = EWAHCompressedBitmap32.bitmapOf(10, 11, 12, 13, 31);
        EWAHCompressedBitmap32 bm2 =  bm1.shift(1);

        EWAHCompressedBitmap32 bm3 = bm1.or(bm2);
        EWAHCompressedBitmap32 bm4 = EWAHCompressedBitmap32.bitmapOf(10,11,12,13,14, 31, 32);
        Assert.assertEquals(bm3, bm4);
    }
    
    
    @Test
    public void shiftbug003() {
        EWAHCompressedBitmap32 bm1 = EWAHCompressedBitmap32.bitmapOf(10, 11, 12, 13, 30);
        EWAHCompressedBitmap32 bm2 =  bm1.shift(1);

        EWAHCompressedBitmap32 bm3 = bm1.or(bm2);
        EWAHCompressedBitmap32 bm4 = EWAHCompressedBitmap32.bitmapOf(10,11,12,13,14, 30, 31);
        Assert.assertEquals(bm3, bm4);
    }

    @Test
    public void shiftbug004() {
        EWAHCompressedBitmap32 bm1 = EWAHCompressedBitmap32.bitmapOf(10, 11, 12, 13, 32);
        EWAHCompressedBitmap32 bm2 =  bm1.shift(1);

        EWAHCompressedBitmap32 bm3 = bm1.or(bm2);
        EWAHCompressedBitmap32 bm4 = EWAHCompressedBitmap32.bitmapOf(10,11,12,13,14, 32, 33);
        Assert.assertEquals(bm3, bm4);
    }

    
    @Test
    public void issue54() {
        EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        EWAHCompressedBitmap32 b2 = EWAHCompressedBitmap32.bitmapOf(0,1,2,3,5,8,13,21,34,55,89,144,233,377,610);
        Assert.assertEquals(4, b1.xorCardinality(b2));
    }

    @Test
    public void andNotCardinality() {
        EWAHCompressedBitmap32 b = EWAHCompressedBitmap32.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        Assert.assertEquals(0, b.andNotCardinality(b));
    }

    @Test
    public void getFirstSetBit() {
        EWAHCompressedBitmap32 b = EWAHCompressedBitmap32.bitmapOf();
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
            EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf(0, 1, 3, 199, 666);
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
        EWAHCompressedBitmap32 ewahBitmap = EWAHCompressedBitmap32.bitmapOf(0, 2, 55,
                64, 1 << 30);
        Assert.assertTrue(ewahBitmap.equals(ewahBitmap));
    }

    @Test
    public void notEqualTo() {
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf(0,1,2,3,5,8,13,21,34,55,89);
        EWAHCompressedBitmap32 b2 = EWAHCompressedBitmap32.bitmapOf(0,1,2,3,5,8,13,21,34,55,89,144,233,377,610);
        Assert.assertFalse(b1.equals(b2));
    }

    @Test
    public void safeSerialization() throws IOException {
        EWAHCompressedBitmap32 ewahBitmap = EWAHCompressedBitmap32.bitmapOf(0, 2, 55,
                64, 1 << 30);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Note: you could use a file output steam instead of ByteArrayOutputStream
        ewahBitmap.serialize(new DataOutputStream(bos));
        EWAHCompressedBitmap32 ewahBitmapnew = new EWAHCompressedBitmap32();
        byte[] bout = bos.toByteArray();
        ewahBitmapnew.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
        assertEquals(ewahBitmapnew, ewahBitmap);
        Assert.assertEquals(ewahBitmapnew.serializedSizeInBytes(), ewahBitmap.serializedSizeInBytes());
    }

    @Test
    public void simpleTestWithIntBuffer() {
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32(IntBuffer.wrap(new int[10]));

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

        bitmap.swap(EWAHCompressedBitmap32.bitmapOf(1));
        Assert.assertEquals(1, bitmap.cardinality());
    }

    @Test
    public void andCompressedSize() {
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf();
        EWAHCompressedBitmap32 b2 = EWAHCompressedBitmap32.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.set(1);
        b2.set(WORD_IN_BITS+1);
        
        EWAHCompressedBitmap32 result = b1.and(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void orCompressedSize() {
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf();
        EWAHCompressedBitmap32 b2 = EWAHCompressedBitmap32.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.setSizeInBits(1, false);
        b2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap32 result = b1.or(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void xorCompressedSize() {
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf();
        EWAHCompressedBitmap32 b2 = EWAHCompressedBitmap32.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);
        b2.setSizeInBits(1, false);
        b2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap32 result = b1.xor(b2);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void andNotCompressedSize() {
        EWAHCompressedBitmap32 b1 = EWAHCompressedBitmap32.bitmapOf();
        
        b1.set(0);
        b1.set(WORD_IN_BITS);

        EWAHCompressedBitmap32 result = b1.andNot(b1);
        Assert.assertEquals(2 * WORD_IN_BITS / 8, result.sizeInBytes());
    }
    
    @Test
    public void testBug091() {
        String v1 = "0000000000000000000000000000000000000000000000000000000000111101";
        String v2 = "0000000000000000001111011111111111111111111111111110001111000000";

        EWAHCompressedBitmap32 bm1 = strToBitmap(v1);
        EWAHCompressedBitmap32 bm2 = strToBitmap(v2);

        bm1 = bm1.and(bm2); // bm1 should now have no bit set

        EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
        bm.setSizeInBits(bm1.sizeInBits(), false); // Create a bitmap with no bit set

        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(bm.sizeInBits(),bm1.sizeInBits());
        Assert.assertTrue(bm.equals(bm1));
    }
    
    private EWAHCompressedBitmap32 strToBitmap(String str) {
        EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
        bm.setSizeInBits(8, false); // Create a bitmap with no bit set

        EWAHCompressedBitmap32 bm1 = bm.clone();
        bm1.not(); // Create a bitmap with all bits set
        bm1 = bm1.and(bm); // Clear all bits

        Assert.assertEquals(0,bm.cardinality());
        Assert.assertEquals(0,bm1.cardinality());
        Assert.assertEquals(bm.sizeInBits(),bm1.sizeInBits());
        Assert.assertTrue(bm.equals(bm1));
    }

    @Test
    public void testBug090b() throws Exception {
        EWAHCompressedBitmap32 bm1 = new EWAHCompressedBitmap32();
        bm1.setSizeInBits(8, false); // Create a bitmap with no bit set
        System.out.println(bm1.toDebugString());
        EWAHCompressedBitmap32 bm2 = new EWAHCompressedBitmap32();
        bm2.setSizeInBits(32, false); // Create a bitmap with no bit set
        EWAHCompressedBitmap32 bm3 = new EWAHCompressedBitmap32();
        Assert.assertTrue(bm1.equals(bm2));
        Assert.assertTrue(bm2.equals(bm1));
        Assert.assertTrue(bm2.equals(bm3));
        Assert.assertTrue(bm3.equals(bm2));
        Assert.assertTrue(bm1.equals(bm3));
        Assert.assertTrue(bm3.equals(bm1));
    }

    @Test
    public void shiftTest() {
        System.out.println("testing shifts");
        for (int k = 2; k <= 4096; k *= 2) {
            int[] bitsToSet = createSortedIntArrayOfBitsToSet(k,
                    434455 + 5 * k);
            EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
            for (int i : bitsToSet) {
                ewah.set(i);
            }
            for(int b = 0; b < 128; ++b) {
                EWAHCompressedBitmap32 ewahs = ewah.shift(b);
                int[] sb = ewahs.toArray();
                for(int z = 0; z < sb.length; ++z)
                    if(sb[z] != bitsToSet[z] + b) throw new RuntimeException("bug");
            }
            for(int z = 0; z < 256;++z) {
                ewah.set(z);
            }
            bitsToSet = ewah.toArray();
            for(int b = 0; b < 128; ++b) {
                EWAHCompressedBitmap32 ewahs = ewah.shift(b);
                int[] sb = ewahs.toArray();
                for(int z = 0; z < sb.length; ++z)
                    if(sb[z] != bitsToSet[z] + b) throw new RuntimeException("bug");
            }
        }
    }
    
    @Test
    public void testBug090c() throws Exception {
        EWAHCompressedBitmap32 bm1 = new EWAHCompressedBitmap32();
        bm1.setSizeInBits(8, false); // Create a bitmap with no bit set
        System.out.println(bm1.toDebugString());
        EWAHCompressedBitmap32 bm2 = new EWAHCompressedBitmap32();
        bm2.setSizeInBits(64, false); // Create a bitmap with no bit set
        EWAHCompressedBitmap32 bm3 = new EWAHCompressedBitmap32();
        Assert.assertEquals(bm1.hashCode(), bm2.hashCode());
        Assert.assertEquals(bm3.hashCode(), bm2.hashCode());
    }

    
    @Test
    public void jugovacTest() {
        EWAHCompressedBitmap32 bm1 = new EWAHCompressedBitmap32(1);
        bm1.set(1);
        EWAHCompressedBitmap32 bm2 = new EWAHCompressedBitmap32(0);
        bm1.andCardinality(bm2);
    }
    
    @Test
    public void setBitsInDecreasingOrder() {
        int[] positions = new int[] { 0, 1, 2, 3, 5, 8, 13, 21 };
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
    public void setOutOfOrderStressTest() {
        System.out.println("out-of-order stress test");
        int n = 10 * WORD_IN_BITS;
        for(int k = 0; k < 100; ++k) {
            List<Integer> positions = new ArrayList<Integer>(n);
            for (int i = 0; i < n; ++i) {
                positions.add(i);
            }
            Collections.shuffle(positions);
            EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
    public void setBitsInDecreasingOrderWithWordPrefix() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.set(WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS/2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void setBitInWordsOfZerosWithWordPrefix() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.set(0);
        bitmap.setSizeInBits(3* WORD_IN_BITS, false);
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i< WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i= WORD_IN_BITS; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordSuffix() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-1, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS+1, true);
        bitmap.setSizeInBits(3 * WORD_IN_BITS, false);
        bitmap.trim();
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i= WORD_IN_BITS-1; i<2*WORD_IN_BITS+1; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(4 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixOfZeros() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(2 * WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i= WORD_IN_BITS; i<2*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(2 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixOfOnes() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        for(int i= WORD_IN_BITS*3/2; i<3*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixAndSuffixOfOnes() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2+1, false);
        bitmap.setSizeInBits(3*WORD_IN_BITS, true);
        bitmap.set(WORD_IN_BITS + WORD_IN_BITS/2);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i<3* WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setUniqueClearBitWithWordPrefixAndWordSuffixOfOnes() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-1, false);
        bitmap.setSizeInBits(2*WORD_IN_BITS-1, true);
        bitmap.setSizeInBits(2*WORD_IN_BITS, false);
        bitmap.setSizeInBits(3*WORD_IN_BITS, true);
        bitmap.set(2*WORD_IN_BITS-1);
        IntIterator iterator = bitmap.intIterator();
        for(int i= WORD_IN_BITS-1; i<3*WORD_IN_BITS; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(3 * WORD_IN_BITS / 8, bitmap.sizeInBytes());
    }

    @Test
    public void setInIncreasingOrderStressTest() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf();
        for(int i = WORD_IN_BITS / 2; i < WORD_IN_BITS; ++i) {
            bitmap1.set(i);
        }
        for(int i = WORD_IN_BITS * 3 / 2; i < WORD_IN_BITS * 2; ++i) {
            bitmap1.set(i);
        }

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf();
        for(int i = 0; i < WORD_IN_BITS / 2; ++i) {
            bitmap1.set(i);
        }
        for(int i = WORD_IN_BITS; i < WORD_IN_BITS * 3 / 2; ++i) {
            bitmap1.set(i);
        }

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf();
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
                        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS/2, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        bitmap.setSizeInBits(WORD_IN_BITS + WORD_IN_BITS/2, true);
        IntIterator iterator = bitmap.intIterator();
        for(int i=0; i< WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i= WORD_IN_BITS; i< WORD_IN_BITS + WORD_IN_BITS/2; ++i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIterator() {
        int[] positions = new int[] { 0, 1, 2, 3, 5, 8, 13, 21 };
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf(positions);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i=positions.length-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(positions[i], iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfOnes() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i= WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfZeros() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        IntIterator iterator = bitmap.reverseIntIterator();
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverBitmapsOfOnesAndZeros() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS-10, true);
        bitmap.setSizeInBits(WORD_IN_BITS, false);
        IntIterator iterator = bitmap.reverseIntIterator();
        for(int i= WORD_IN_BITS-10; i>0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i-1, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverMultipleRLWs() {
        EWAHCompressedBitmap32 b = EWAHCompressedBitmap32.bitmapOf(1000, 100000, 100000 + WORD_IN_BITS);
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
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();
        b.setSizeInBits(WORD_IN_BITS, true);
        b.set(WORD_IN_BITS + 5);

        IntIterator iterator = b.reverseIntIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(WORD_IN_BITS+5, iterator.next());
        for(int i= WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void reverseIntIteratorOverConsecutiveLiteralsInSameRunningLengthWord() {
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();
        b.setSizeInBits(WORD_IN_BITS, true);
        b.setSizeInBits(2*WORD_IN_BITS, false);
        b.setSizeInBits(3*WORD_IN_BITS, true);
        b.set(3*WORD_IN_BITS+5);
        b.set(5*WORD_IN_BITS-1);

        IntIterator iterator = b.reverseIntIterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(5*WORD_IN_BITS - 1, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(3*WORD_IN_BITS+5, iterator.next());
        for(int i=3*WORD_IN_BITS-1; i>=2*WORD_IN_BITS; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        for(int i= WORD_IN_BITS-1; i>=0; --i) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(i, iterator.next());
        }
        Assert.assertFalse(iterator.hasNext());
    }
    
    @Test
    public void isEmpty() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(1000, false);
        Assert.assertTrue(bitmap.isEmpty());
        bitmap.set(1001);
        Assert.assertFalse(bitmap.isEmpty());
    }

    @Test
    public void issue58() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf(52344, 52344 + 9);
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf(243, 260, 1000);
        ChunkIterator iter = bitmap.chunkIterator();
        iter.move(245);
        Assert.assertEquals(15, iter.nextLength());
    }

    @Test
    public void issue61() {
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.set(210696);
        bitmap.set(210984);
        bitmap.set(210985);
        ChunkIterator iter = bitmap.chunkIterator();
        iter.move(210984);
        Assert.assertEquals(2, iter.nextLength());

        bitmap = new EWAHCompressedBitmap32();
        bitmap.set(210696);
        bitmap.set(210698);
        bitmap.set(210699);
        iter = bitmap.chunkIterator();
        iter.move(210698);
        Assert.assertEquals(2, iter.nextLength());
    }

    @Test
    public void chunkIterator() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf(0, 1, 2, 3, 4, 7, 8, 9, 10);

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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
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
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf(1, 3, 4);
        bitmap1.setSizeInBits(5, false);

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf(0, 2);

        EWAHCompressedBitmap32 result = bitmap1.compose(bitmap2);

        Assert.assertEquals(5, result.sizeInBits());
        Assert.assertEquals(2, result.cardinality());
        Assert.assertEquals(Integer.valueOf(1), result.toList().get(0));
        Assert.assertEquals(Integer.valueOf(4), result.toList().get(1));
    }

    @Test
    public void composeBitmapOfOnesWithItself() {
        EWAHCompressedBitmap32 bitmap = EWAHCompressedBitmap32.bitmapOf();
        bitmap.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap32 result = bitmap.compose(bitmap);

        Assert.assertEquals(bitmap, result);
    }

    @Test
    public void composeBitmapOfZerosAndOnesWithBitmapOfOnes() {
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf();
        bitmap1.setSizeInBits(WORD_IN_BITS, false);
        bitmap1.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS, true);

        EWAHCompressedBitmap32 result = bitmap1.compose(bitmap2);

        Assert.assertEquals(bitmap1, result);
    }

    @Test
    public void composeBitmapOfOnesWithBitmapOfZerosAndOnes() {
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf();
        bitmap1.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf();
        bitmap2.setSizeInBits(WORD_IN_BITS, false);
        bitmap2.setSizeInBits(2 * WORD_IN_BITS, true);

        EWAHCompressedBitmap32 result = bitmap1.compose(bitmap2);

        Assert.assertEquals(bitmap2, result);
    }

    @Test
    public void composeBitmapWithBitmapOfZeros() {
        EWAHCompressedBitmap32 bitmap1 = EWAHCompressedBitmap32.bitmapOf(1, 3, 4, 9);
        bitmap1.setSizeInBits(WORD_IN_BITS, false);

        EWAHCompressedBitmap32 bitmap2 = EWAHCompressedBitmap32.bitmapOf();
        bitmap2.setSizeInBits(5, false);

        EWAHCompressedBitmap32 result = bitmap1.compose(bitmap2);

        Assert.assertEquals(0, result.cardinality());
        Assert.assertEquals(WORD_IN_BITS, result.sizeInBits());
    }

    @Test
    public void testAstesana() throws Exception {
        for (int k = 5; k < 256; ++k) {
            EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
            bm.set(1);
            bm.setSizeInBits(k, false);
            EWAHCompressedBitmap32 bm1 = bm.clone();
            bm1.not();
            EWAHCompressedBitmap32 x = bm1.and(bm1);
            Assert.assertEquals(x.cardinality(),k-1);
            x = bm1.andNot(bm1);
            Assert.assertEquals(x.cardinality(),0);
            x = bm1.xor(bm1);
            Assert.assertEquals(x.cardinality(),0);
            x = bm1.or(bm1);
            Assert.assertEquals(x.cardinality(),k-1);
        }
    }
    
    @Test
    public void testAstesana2() {
        for (int k = 1; k < 256; ++k) {

            // Create two equivalent bitmaps
            EWAHCompressedBitmap32 bm = new EWAHCompressedBitmap32();
            bm.set(0);
            bm.setSizeInBits(k, false);
            EWAHCompressedBitmap32 bm3 = new EWAHCompressedBitmap32();
            bm3.set(0);
            bm3.setSizeInBits(k, false);

            // Perform two negation 
            // -> should change nothing
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
        EWAHCompressedBitmap32 x = EWAHCompressedBitmap32.bitmapOf(1, 3, 7, 8, 10);
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
        EWAHCompressedBitmap32 x = EWAHCompressedBitmap32.bitmapOf();
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
            EWAHCompressedBitmap32 x = new EWAHCompressedBitmap32();
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

    public static Iterator toIterator(final EWAHCompressedBitmap32[] bitmaps) {
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

        EWAHCompressedBitmap32[] bitmaps = new EWAHCompressedBitmap32[data.length];
        
        for (int i = 0; i < bitmaps.length; ++i) {
        	bitmaps[i] = new EWAHCompressedBitmap32();
            for (int j : data[i]) {
            	bitmaps[i].set(j);
            }
            bitmaps[i].setSizeInBits(1000, false);
        }
        EWAHCompressedBitmap32 and1 = FastAggregation32.bufferedand(1024, bitmaps[0],bitmaps[1],bitmaps[2]);
        EWAHCompressedBitmap32 and2 = new  EWAHCompressedBitmap32();
        FastAggregation32.bufferedandWithContainer(and2, 32, bitmaps[0],bitmaps[1],bitmaps[2]);
        EWAHCompressedBitmap32 and3 = EWAHCompressedBitmap32.and(bitmaps[0],bitmaps[1],bitmaps[2]);
        System.out.println(and1.sizeInBits());
        System.out.println(and2.sizeInBits());
        System.out.println(and3.sizeInBits());
        assertEqualsPositions(and1, and2);
        assertEqualsPositions(and2, and3);
    }


    @Test
    public void fastagg() {
        int[][] data = {{}, {5, 6, 7, 8, 9}, {1}, {2}};

        EWAHCompressedBitmap32[] bitmaps = new EWAHCompressedBitmap32[data.length];
        
        for (int i = 0; i < bitmaps.length; ++i) {
        	bitmaps[i] = new EWAHCompressedBitmap32();
            for (int j : data[i]) {
            	bitmaps[i].set(j);
            }
            bitmaps[i].setSizeInBits(1000, false);
        }
        EWAHCompressedBitmap32 or1 = FastAggregation32.bufferedor(1024, bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap32 or2 = FastAggregation32.or(bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap32 or3 = FastAggregation32.bufferedor(1024, bitmaps);
        EWAHCompressedBitmap32 or4 = FastAggregation32.or(bitmaps);
        EWAHCompressedBitmap32 or5 = FastAggregation32.or(toIterator(bitmaps));
        EWAHCompressedBitmap32 or6 = new EWAHCompressedBitmap32();
        FastAggregation32.orToContainer(or6,  bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);

        assertEquals(or1,or2);
        assertEquals(or2,or3);        
        assertEquals(or3,or4);        
        assertEquals(or4,or5);        
        assertEquals(or5,or6);       

        EWAHCompressedBitmap32 xor1 = FastAggregation32.bufferedxor(1024, bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap32 xor2 = FastAggregation32.xor(bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);
        EWAHCompressedBitmap32 xor3 = FastAggregation32.bufferedxor(1024, bitmaps);
        EWAHCompressedBitmap32 xor4 = FastAggregation32.xor(bitmaps);
        EWAHCompressedBitmap32 xor5 = FastAggregation32.xor(toIterator(bitmaps));
        EWAHCompressedBitmap32 xor6 = new EWAHCompressedBitmap32();
        FastAggregation32.xorToContainer(xor6,  bitmaps[0],bitmaps[1],bitmaps[2],bitmaps[3]);

        assertEquals(xor1,xor2);
        assertEquals(xor2,xor3);        
        assertEquals(xor3,xor4);        
        assertEquals(xor4,xor5);        
        assertEquals(xor5,xor6);       
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

        EWAHCompressedBitmap32[] toBeOred = new EWAHCompressedBitmap32[data.length];
        Set<Integer> bruteForceAnswer = new HashSet<Integer>();
        for (int i = 0; i < toBeOred.length; ++i) {
            toBeOred[i] = new EWAHCompressedBitmap32();
            for (int j : data[i]) {
                toBeOred[i].set(j);
                bruteForceAnswer.add(j);
            }
            toBeOred[i].setSizeInBits(1000, false);
        }

        long rightcard = bruteForceAnswer.size();
        EWAHCompressedBitmap32 foo = new EWAHCompressedBitmap32();
        FastAggregation32.orToContainer(foo, toBeOred);
        Assert.assertEquals(rightcard, foo.cardinality());
        EWAHCompressedBitmap32 e1 = FastAggregation.or(toBeOred);
        Assert.assertEquals(rightcard, e1.cardinality());
        EWAHCompressedBitmap32 e2 = FastAggregation32.bufferedor(65536,
                toBeOred);
        Assert.assertEquals(rightcard, e2.cardinality());
    }

    @Test
    public void testSizeInBitsWithAnd() {
        System.out.println("testing SizeInBitsWithAnd");
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBitsWithinLastWord(10);
        b.setSizeInBitsWithinLastWord(10);

        EWAHCompressedBitmap32 and = a.and(b);
        Assert.assertEquals(10, and.sizeInBits());
        EWAHCompressedBitmap32 and2 = EWAHCompressedBitmap32.and(a, b);
        Assert.assertEquals(10, and2.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithAndNot() {
        System.out.println("testing SizeInBitsWithAndNot");
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBitsWithinLastWord(10);
        b.setSizeInBitsWithinLastWord(10);

        EWAHCompressedBitmap32 and = a.andNot(b);
        Assert.assertEquals(10, and.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithOr() {
        System.out.println("testing SizeInBitsWithOr");
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBitsWithinLastWord(10);
        b.setSizeInBitsWithinLastWord(10);

        EWAHCompressedBitmap32 or = a.or(b);
        Assert.assertEquals(10, or.sizeInBits());
        EWAHCompressedBitmap32 or2 = EWAHCompressedBitmap32.or(a, b);
        Assert.assertEquals(10, or2.sizeInBits());
    }

    @Test
    public void testSizeInBitsWithXor() {
        System.out.println("testing SizeInBitsWithXor");
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();

        a.set(1);
        a.set(2);
        a.set(3);

        b.set(3);
        b.set(4);
        b.set(5);

        a.setSizeInBitsWithinLastWord(10);
        b.setSizeInBitsWithinLastWord(10);

        EWAHCompressedBitmap32 xor = a.xor(b);
        Assert.assertEquals(10, xor.sizeInBits());
        EWAHCompressedBitmap32 xor2 = EWAHCompressedBitmap32.xor(a, b);
        Assert.assertEquals(10, xor2.sizeInBits());
    }

    @Test
    public void testDebugSetSizeInBitsTest() {
        System.out.println("testing DebugSetSizeInBits");
        EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();

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
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
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
            EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
            for (int i : bitsToSet) {
                ewah.set(i);
            }
            equal(ewah.iterator(), bitsToSet);
        }
    }

    /**
     * Test submitted by Gregory Ssi-Yan-Kai
     */
    @Test
    public void SsiYanKaiTest() {
        System.out.println("testing SsiYanKaiTest");
        EWAHCompressedBitmap32 a = EWAHCompressedBitmap32.bitmapOf(
                39935, 39936, 39937, 39938, 39939, 39940, 39941, 39942,
                39943, 39944, 39945, 39946, 39947, 39948, 39949, 39950,
                39951, 39952, 39953, 39954, 39955, 39956, 39957, 39958,
                39959, 39960, 39961, 39962, 39963, 39964, 39965, 39966,
                39967, 39968, 39969, 39970, 39971, 39972, 39973, 39974,
                39975, 39976, 39977, 39978, 39979, 39980, 39981, 39982,
                39983, 39984, 39985, 39986, 39987, 39988, 39989, 39990,
                39991, 39992, 39993, 39994, 39995, 39996, 39997, 39998,
                39999, 40000, 40001, 40002, 40003, 40004, 40005, 40006,
                40007, 40008, 40009, 40010, 40011, 40012, 40013, 40014,
                40015, 40016, 40017, 40018, 40019, 40020, 40021, 40022,
                40023, 40024, 40025, 40026, 40027, 40028, 40029, 40030,
                40031, 40032, 40033, 40034, 40035, 40036, 40037, 40038,
                40039, 40040, 40041, 40042, 40043, 40044, 40045, 40046,
                40047, 40048, 40049, 40050, 40051, 40052, 40053, 40054,
                40055, 40056, 40057, 40058, 40059, 40060, 40061, 40062,
                40063, 40064, 40065, 40066, 40067, 40068, 40069, 40070,
                40071, 40072, 40073, 40074, 40075, 40076, 40077, 40078,
                40079, 40080, 40081, 40082, 40083, 40084, 40085, 40086,
                40087, 40088, 40089, 40090, 40091, 40092, 40093, 40094,
                40095, 40096, 40097, 40098, 40099, 40100);
        EWAHCompressedBitmap32 b = EWAHCompressedBitmap32.bitmapOf(
                39935, 39936, 39937, 39938, 39939, 39940, 39941, 39942,
                39943, 39944, 39945, 39946, 39947, 39948, 39949, 39950,
                39951, 39952, 39953, 39954, 39955, 39956, 39957, 39958,
                39959, 39960, 39961, 39962, 39963, 39964, 39965, 39966,
                39967, 39968, 39969, 39970, 39971, 39972, 39973, 39974,
                39975, 39976, 39977, 39978, 39979, 39980, 39981, 39982,
                39983, 39984, 39985, 39986, 39987, 39988, 39989, 39990,
                39991, 39992, 39993, 39994, 39995, 39996, 39997, 39998,
                39999, 270000);
        LinkedHashSet<Integer> aPositions = new LinkedHashSet<Integer>(
                a.toList());
        int intersection = 0;
        EWAHCompressedBitmap32 inter = new EWAHCompressedBitmap32();
        LinkedHashSet<Integer> bPositions = new LinkedHashSet<Integer>(
                b.toList());
        for (Integer integer : bPositions) {
            if (aPositions.contains(integer)) {
                inter.set(integer);
                ++intersection;
            }
        }
        inter.setSizeInBits(maxSizeInBits(a, b), false);
        EWAHCompressedBitmap32 and2 = a.and(b);
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
        EWAHCompressedBitmap32 aa = new EWAHCompressedBitmap32();
        int[] val = {55400, 1000000, 1000128};
        for (int aVal : val) {
            aa.set(aVal);
            bitsetaa.set(aVal);
        }
        equal(aa, bitsetaa);
        BitSet bitsetab = new BitSet();
        EWAHCompressedBitmap32 ab = new EWAHCompressedBitmap32();
        for (int i = 4096; i < (4096 + 5); i++) {
            ab.set(i);
            bitsetab.set(i);
        }
        ab.set(99000);
        bitsetab.set(99000);
        ab.set(1000130);
        bitsetab.set(1000130);
        equal(ab, bitsetab);
        EWAHCompressedBitmap32 bb = aa.or(ab);
        EWAHCompressedBitmap32 bbAnd = aa.and(ab);
        EWAHCompressedBitmap32 abnot = ab.clone();
        abnot.not();
        EWAHCompressedBitmap32 bbAnd2 = aa.andNot(abnot);
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
        EWAHCompressedBitmap32 bitmap1 = new EWAHCompressedBitmap32();
        bitmap1.set(35);
        EWAHCompressedBitmap32 bitmap2 = new EWAHCompressedBitmap32();
        bitmap2.set(35);
        bitmap2.set(130);

        EWAHCompressedBitmap32 resultBitmap = bitmap1.and(bitmap2);
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
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.set(Integer.MAX_VALUE - 32);
        // System.out.format("Total Items %d\n", bitmap.cardinality());
        Assert.assertTrue(bitmap.cardinality() == 1);
    }

    /**
     * Test clear function
     */
    @Test
    public void testClear() {
        System.out.println("testing Clear");
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
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
        int zero = 0;
        int specialval = 1 | (1 << 4) | (1 << 31);
        int notzero = ~zero;
        EWAHCompressedBitmap32 myarray1 = new EWAHCompressedBitmap32();
        myarray1.addWord(zero);
        myarray1.addWord(zero);
        myarray1.addWord(zero);
        myarray1.addWord(specialval);
        myarray1.addWord(specialval);
        myarray1.addWord(notzero);
        myarray1.addWord(zero);
        Assert.assertEquals(myarray1.toList().size(), 6 + 32);
        EWAHCompressedBitmap32 myarray2 = new EWAHCompressedBitmap32();
        myarray2.addWord(zero);
        myarray2.addWord(specialval);
        myarray2.addWord(specialval);
        myarray2.addWord(notzero);
        myarray2.addWord(zero);
        myarray2.addWord(zero);
        myarray2.addWord(zero);
        Assert.assertEquals(myarray2.toList().size(), 6 + 32);
        List<Integer> data1 = myarray1.toList();
        List<Integer> data2 = myarray2.toList();
        ArrayList<Integer> logicalor = new ArrayList<Integer>();
        {
            HashSet<Integer> tmp = new HashSet<Integer>();
            tmp.addAll(data1);
            tmp.addAll(data2);
            logicalor.addAll(tmp);
        }
        Collections.sort(logicalor);
        ArrayList<Integer> logicaland = new ArrayList<Integer>();
        logicaland.addAll(data1);
        logicaland.retainAll(data2);
        Collections.sort(logicaland);
        EWAHCompressedBitmap32 arrayand = myarray1.and(myarray2);
        Assert.assertTrue(arrayand.toList().equals(logicaland));
        EWAHCompressedBitmap32 arrayor = myarray1.or(myarray2);
        Assert.assertTrue(arrayor.toList().equals(logicalor));
        EWAHCompressedBitmap32 arrayandbis = myarray2.and(myarray1);
        Assert.assertTrue(arrayandbis.toList().equals(logicaland));
        EWAHCompressedBitmap32 arrayorbis = myarray2.or(myarray1);
        Assert.assertTrue(arrayorbis.toList().equals(logicalor));
        EWAHCompressedBitmap32 x = new EWAHCompressedBitmap32();
        for (Integer i : myarray1.toList()) {
            x.set(i);
        }
        Assert.assertTrue(x.toList().equals(
                myarray1.toList()));
        x = new EWAHCompressedBitmap32();
        for (Integer i : myarray2.toList()) {
            x.set(i);
        }
        Assert.assertTrue(x.toList().equals(
                myarray2.toList()));
        x = new EWAHCompressedBitmap32();
        for (Iterator<Integer> k = myarray1.iterator(); k.hasNext(); ) {
            x.set(extracted(k));
        }
        Assert.assertTrue(x.toList().equals(
                myarray1.toList()));
        x = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 ewcb = new EWAHCompressedBitmap32();
        int[] val = {5, 4400, 44600, 55400, 1000000};
        for (int aVal : val) {
            ewcb.set(aVal);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bos);
        ewcb.writeExternal(oo);
        oo.close();
        ewcb = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 myarray1 = new EWAHCompressedBitmap32();
        int N = 1024;
        for (int i = 0; i < N; ++i) {
            myarray1.set(Integer.MAX_VALUE - 32 - N + i);
            Assert.assertTrue(myarray1.cardinality() == i + 1);
            int[] val = myarray1.toArray();
            Assert.assertTrue(val[0] == Integer.MAX_VALUE - 32 - N);
        }
    }

    /**
     * Test the intersects method
     */
    @Test
    public void testIntersectsMethod() {
        System.out.println("testing Intersets Bug");
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.set(1);
        EWAHCompressedBitmap32 bitmap2 = new EWAHCompressedBitmap32();
        bitmap2.set(1);
        bitmap2.set(11);
        bitmap2.set(111);
        bitmap2.set(1111111);
        bitmap2.set(11111111);
        Assert.assertTrue(bitmap.intersects(bitmap2));
        Assert.assertTrue(bitmap2.intersects(bitmap));

        EWAHCompressedBitmap32 bitmap3 = new EWAHCompressedBitmap32();
        bitmap3.set(101);
        EWAHCompressedBitmap32 bitmap4 = new EWAHCompressedBitmap32();
        for (int i = 0; i < 100; i++) {
            bitmap4.set(i);
        }
        Assert.assertFalse(bitmap3.intersects(bitmap4));
        Assert.assertFalse(bitmap4.intersects(bitmap3));

        EWAHCompressedBitmap32 bitmap5 = new EWAHCompressedBitmap32();
        bitmap5.set(0);
        bitmap5.set(10);
        bitmap5.set(20);
        EWAHCompressedBitmap32 bitmap6 = new EWAHCompressedBitmap32();
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

        EWAHCompressedBitmap32 bitmap7 = new EWAHCompressedBitmap32();
        bitmap7.set(1);
        bitmap7.set(10);
        bitmap7.set(20);
        bitmap7.set(1111111);
        bitmap7.set(11111111);
        EWAHCompressedBitmap32 bitmap8 = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 myarray1 = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[1024];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap32();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap32 answer = ewah[0];
        for (int k = 1; k < ewah.length; ++k)
            answer = answer.and(ewah[k]);
        // result should be empty
        if (answer.toList().size() != 0)
            System.out.println(answer.toDebugString());
        Assert.assertTrue(answer.toList().size() == 0);
        Assert.assertTrue(EWAHCompressedBitmap32.and(ewah)
                .toList().size() == 0);
    }

    /**
     * Test massive and not.
     */
    @Test
    public void testMassiveAndNot() throws Exception {
        System.out.println("testing massive and not");
        final int N = 1024;
        EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap32();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap32 answer = ewah[0];
        EWAHCompressedBitmap32 answer2 = ewah[0];
        for (int k = 1; k < ewah.length; ++k) {
            answer = answer.andNot(ewah[k]);
            EWAHCompressedBitmap32 copy = ewah[k].clone();
            copy.not();
            answer2.and(copy);
            assertEqualsPositions(answer, answer2);
        }
    }

    @Test
    public void testsetSizeInBits() {
        System.out.println("testing setSizeInBits");
        for (int k = 0; k < 4096; ++k) {
            EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
            ewah.setSizeInBits(k, false);
            Assert.assertEquals(ewah.sizeInBits(), k);
            Assert.assertEquals(ewah.cardinality(), 0);
            EWAHCompressedBitmap32 ewah2 = new EWAHCompressedBitmap32();
            ewah2.setSizeInBits(k, false);
            Assert.assertEquals(ewah2.sizeInBits(), k);
            Assert.assertEquals(ewah2.cardinality(), 0);
            EWAHCompressedBitmap32 ewah3 = new EWAHCompressedBitmap32();
            for (int i = 0; i < k; ++i) {
                ewah3.set(i);
            }
            Assert.assertEquals(ewah3.sizeInBits(), k);
            Assert.assertEquals(ewah3.cardinality(), k);
            EWAHCompressedBitmap32 ewah4 = new EWAHCompressedBitmap32();
            ewah4.setSizeInBits(k, true);
            Assert.assertEquals(ewah4.sizeInBits(), k);
            Assert.assertEquals(ewah4.cardinality(), k);
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
            EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
            BitSet[] bset = new BitSet[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap32();
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
            EWAHCompressedBitmap32 answer = ewah[0];
            BitSet bitsetanswer = bset[0];
            for (int k = 1; k < ewah.length; ++k) {
                EWAHCompressedBitmap32 tmp = answer.or(ewah[k]);
                bitsetanswer.or(bset[k]);
                answer = tmp;
                assertEqualsPositions(bitsetanswer, answer);
            }
            assertEqualsPositions(bitsetanswer, answer);
            assertEqualsPositions(bitsetanswer,
                    EWAHCompressedBitmap32.or(ewah));
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

    /**
     * Test massive xor.
     */
    @Test
    public void testMassiveXOR() {
        System.out
                .println("testing massive xor (can take a couple of minutes)");
        final int N = 16;
        EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
        BitSet[] bset = new BitSet[N];
        for (int k = 0; k < ewah.length; ++k)
            ewah[k] = new EWAHCompressedBitmap32();
        for (int k = 0; k < bset.length; ++k)
            bset[k] = new BitSet();
        for (int k = 0; k < 30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
            bset[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap32 answer = ewah[0];
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
        EWAHCompressedBitmap32 bitmap1 = new EWAHCompressedBitmap32();
        bitmap1.addStreamOfEmptyWords(true, 1000);
        EWAHCompressedBitmap32 bitmap2 = new EWAHCompressedBitmap32();
        bitmap2.addStreamOfEmptyWords(true, 2000);
        EWAHCompressedBitmap32 bitmap3 = new EWAHCompressedBitmap32();
        bitmap3.set(500);
        bitmap3.set(502);
        bitmap3.set(504);

        assertAndEquals(bitmap1, bitmap2, bitmap3);

        // equal
        bitmap1 = new EWAHCompressedBitmap32();
        bitmap1.set(35);
        bitmap2 = new EWAHCompressedBitmap32();
        bitmap2.set(35);
        bitmap3 = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 resultBitmap = EWAHCompressedBitmap32
                .and(bitmap1, bitmap2, bitmap3);
        resultBitmap.set(131);

        bitmap1.set(131);
        assertEquals(bitmap1, resultBitmap);

        final int N = 128;
        for (int howmany = 512; howmany <= 10000; howmany *= 2) {
            EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap32();
            for (int k = 0; k < howmany; ++k) {
                ewah[(k + 2 * k * k) % ewah.length].set(k);
            }
            for (int k = 1; k <= ewah.length; ++k) {
                EWAHCompressedBitmap32[] shortewah = new EWAHCompressedBitmap32[k];
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
        EWAHCompressedBitmap32 bitmap1 = new EWAHCompressedBitmap32();
        bitmap1.set(1000);
        EWAHCompressedBitmap32 bitmap2 = new EWAHCompressedBitmap32();
        bitmap2.set(2000);
        EWAHCompressedBitmap32 bitmap3 = new EWAHCompressedBitmap32();
        bitmap3.set(500);
        bitmap3.set(502);
        bitmap3.set(504);

        EWAHCompressedBitmap32 expected = bitmap1.or(bitmap2).or(
                bitmap3);

        assertEquals(expected,
                EWAHCompressedBitmap32.or(bitmap1, bitmap2, bitmap3));

        final int N = 128;
        for (int howmany = 512; howmany <= 10000; howmany *= 2) {
            EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
            for (int k = 0; k < ewah.length; ++k)
                ewah[k] = new EWAHCompressedBitmap32();
            for (int k = 0; k < howmany; ++k) {
                ewah[(k + 2 * k * k) % ewah.length].set(k);
            }
            for (int k = 1; k <= ewah.length; ++k) {
                EWAHCompressedBitmap32[] shortewah = new EWAHCompressedBitmap32[k];
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
        EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
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
            EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
            for (int i = 0; i < N; i++) {
                bitmap.set(i);
            }
            bitmap.set(1025);
            bitmap.set(1026);
            Assert.assertEquals(N + 2, bitmap.cardinality());
            EWAHCompressedBitmap32 orbitmap = bitmap.or(bitmap);
            assertEquals(orbitmap, bitmap);
            Assert.assertEquals(N + 2, orbitmap.cardinality());
            if (N + 2 != bitmap
                    .orCardinality(new EWAHCompressedBitmap32())) {
                System.out.println("N = " + N);
                System.out.println(bitmap.toDebugString());
                System.out.println("cardinality = "
                        + bitmap.cardinality());
                System.out
                        .println("orCardinality = "
                                + bitmap.orCardinality(new EWAHCompressedBitmap32()));
            }

            Assert.assertEquals(N + 2, bitmap
                    .orCardinality(new EWAHCompressedBitmap32()));
        }
    }

    /**
     * Test sets and gets.
     */
    @Test
    public void testSetGet() {
        System.out.println("testing EWAH set/get");
        EWAHCompressedBitmap32 ewcb = new EWAHCompressedBitmap32();
        int[] val = {5, 4400, 44600, 55400, 1000000};
        for (int aVal : val) {
            ewcb.set(aVal);
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
        EWAHCompressedBitmap32 ewcb = EWAHCompressedBitmap32.bitmapOf(
                50, 70).and(EWAHCompressedBitmap32.bitmapOf(50, 1000));
        EWAHCompressedBitmap32 expected = EWAHCompressedBitmap32.bitmapOf(50);
        expected.setSizeInBits(1000, false);
        Assert.assertEquals(expected, ewcb);
        Assert.assertEquals(expected
                .hashCode(), ewcb.hashCode());
        ewcb.addWord(~0);
        EWAHCompressedBitmap32 ewcb2 = ewcb.clone();
        ewcb2.addWord(0);
        Assert.assertEquals(ewcb
                .hashCode(), ewcb2.hashCode());

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
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
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
            EWAHCompressedBitmap32 cBitMap = new EWAHCompressedBitmap32();
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
            EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
            EWAHCompressedBitmap32 b = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        a.set(410018);
        a.set(410019);
        a.set(410020);
        a.set(410021);
        a.set(410022);
        a.set(410023);

        EWAHCompressedBitmap32 b;

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
            final EWAHCompressedBitmap32 ewahBitmap1 = new EWAHCompressedBitmap32();
            final BitSet jdkBitmap1 = new BitSet();
            final EWAHCompressedBitmap32 ewahBitmap2 = new EWAHCompressedBitmap32();
            final BitSet jdkBitmap2 = new BitSet();
            final EWAHCompressedBitmap32 ewahBitmap3 = new EWAHCompressedBitmap32();
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
                final EWAHCompressedBitmap32 xorEwahBitmap = ewahBitmap1
                        .xor(ewahBitmap2);
                final BitSet xorJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                xorJdkBitmap.xor(jdkBitmap2);
                assertEquals(xorJdkBitmap, xorEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap32 andEwahBitmap = ewahBitmap1
                        .and(ewahBitmap2);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap32 andEwahBitmap = ewahBitmap2
                        .and(ewahBitmap1);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
                assertEquals(andJdkBitmap,
                        EWAHCompressedBitmap32.and(ewahBitmap1,
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
                        EWAHCompressedBitmap32.and(ewahBitmap1,
                                ewahBitmap2, ewahBitmap3)
                );
                assertEquals(andJdkBitmap,
                        EWAHCompressedBitmap32.and(ewahBitmap3,
                                ewahBitmap2, ewahBitmap1)
                );
                Assert.assertEquals(andJdkBitmap.cardinality(),
                        EWAHCompressedBitmap32.andCardinality(
                                ewahBitmap1, ewahBitmap2,
                                ewahBitmap3)
                );
            }
            // AND NOT
            {
                final EWAHCompressedBitmap32 andNotEwahBitmap = ewahBitmap1
                        .andNot(ewahBitmap2);
                final BitSet andNotJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                andNotJdkBitmap.andNot(jdkBitmap2);
                assertEquals(andNotJdkBitmap, andNotEwahBitmap);
            }
            // AND NOT
            {
                final EWAHCompressedBitmap32 andNotEwahBitmap = ewahBitmap2
                        .andNot(ewahBitmap1);
                final BitSet andNotJdkBitmap = (BitSet) jdkBitmap2
                        .clone();
                andNotJdkBitmap.andNot(jdkBitmap1);
                assertEquals(andNotJdkBitmap, andNotEwahBitmap);
            }
            // OR
            {
                final EWAHCompressedBitmap32 orEwahBitmap = ewahBitmap1
                        .or(ewahBitmap2);
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1
                        .clone();
                orJdkBitmap.or(jdkBitmap2);
                assertEquals(orJdkBitmap, orEwahBitmap);
                assertEquals(orJdkBitmap,
                        EWAHCompressedBitmap32.or(ewahBitmap1,
                                ewahBitmap2)
                );
                Assert.assertEquals(orEwahBitmap.cardinality(),
                        ewahBitmap1.orCardinality(ewahBitmap2));
            }
            // OR
            {
                final EWAHCompressedBitmap32 orEwahBitmap = ewahBitmap2
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
                        EWAHCompressedBitmap32.or(ewahBitmap1,
                                ewahBitmap2, ewahBitmap3)
                );
                assertEquals(orJdkBitmap,
                        EWAHCompressedBitmap32.or(ewahBitmap3,
                                ewahBitmap2, ewahBitmap1)
                );
                Assert.assertEquals(orJdkBitmap.cardinality(),
                        EWAHCompressedBitmap32.orCardinality(
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
        EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
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
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.setSizeInBits(1, false);
        bitmap.not();
        Assert.assertEquals(1, bitmap.cardinality());
    }

    @Test
    public void testHasNextSafe() {
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.set(0);
        IntIterator it = bitmap.intIterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(0, it.next());
    }

    @Test
    public void testHasNextSafe2() {
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.set(0);
        IntIterator it = bitmap.intIterator();
        Assert.assertEquals(0, it.next());
    }

    @Test
    public void testInfiniteLoop() {
        System.out.println("Testing for an infinite loop");
        EWAHCompressedBitmap32 b1 = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b2 = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 b3 = new EWAHCompressedBitmap32();
        b3.setSizeInBits(5, false);
        b1.set(2);
        b2.set(4);
        EWAHCompressedBitmap32.and(b1, b2, b3);
        EWAHCompressedBitmap32.or(b1, b2, b3);

    }

    @Test
    public void testSizeInBits2() {
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
        bitmap.setSizeInBits(1, true);
        bitmap.not();
        Assert.assertEquals(0, bitmap.cardinality());
    }

    private static void assertAndEquals(EWAHCompressedBitmap32... bitmaps) {
        EWAHCompressedBitmap32 expected = bitmaps[0];
        for (int i = 1; i < bitmaps.length; i++) {
            expected = expected.and(bitmaps[i]);
        }
        Assert.assertTrue(expected.equals(EWAHCompressedBitmap32
                .and(bitmaps)));
    }

    private static void assertEquals(EWAHCompressedBitmap32 expected,
                                     EWAHCompressedBitmap32 actual) {
        Assert.assertEquals(expected.sizeInBits(), actual.sizeInBits());
        assertEqualsPositions(expected, actual);
    }

    private static void assertOrEquals(EWAHCompressedBitmap32... bitmaps) {
        EWAHCompressedBitmap32 expected = bitmaps[0];
        for (int i = 1; i < bitmaps.length; i++) {
            expected = expected.or(bitmaps[i]);
        }
        assertEquals(expected, EWAHCompressedBitmap32.or(bitmaps));
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
        EWAHCompressedBitmap32 bitmap = new EWAHCompressedBitmap32();
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
                                  EWAHCompressedBitmap32 ewahBitmap) {
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
                             EWAHCompressedBitmap32 ewahBitmap) {
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
                                     EWAHCompressedBitmap32 ewahBitmap) {
        final ArrayList<Integer> positions = new ArrayList<Integer>();
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
                                      EWAHCompressedBitmap32 ewahBitmap) {
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
    static void assertEqualsPositions(EWAHCompressedBitmap32 ewahBitmap1,
                                      EWAHCompressedBitmap32 ewahBitmap2) {
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
    static void equal(EWAHCompressedBitmap32 x, BitSet y) {
        Assert.assertEquals(x.cardinality(), y.cardinality());
        for (int i : x.toList())
            Assert.assertTrue(y.get(i));
    }
    

    @Test
    public void insertTest() {
        EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
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
