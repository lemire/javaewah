package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import org.junit.Assert;
import org.junit.Test;


import java.io.*;
import java.util.*;

import static com.googlecode.javaewah.EWAHCompressedBitmap.WORD_IN_BITS;

/**
 * This class is used for basic unit testing.
 */
@SuppressWarnings("javadoc")
public class EWAHCompressedBitmapTest {

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
    public void testAstesana() {
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
    public void testClearIntIterator() {
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
        x.setSizeInBits(3, false);
        IntIterator iterator = x.clearIntIterator();
        for (int i : Arrays.asList(0, 1, 2)) {
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
        FastAggregation.legacy_orWithContainer(foo, toBeOred);
        Assert.assertEquals(rightcard, foo.cardinality());
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
        EWAHCompressedBitmap and2 = a.and(b);
        if (!and2.equals(inter))
            throw new RuntimeException("intersections don't match");
        if (intersection != and2.cardinality())
            throw new RuntimeException("cardinalities don't match");
    }

    /**
     * Test inspired by William Habermaas.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Test
    public void habermaasTest() {
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
    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Test
    public void testExternalization() throws IOException {
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
    public void testMassiveAndNot() {
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
            Assert.assertEquals(ewah.sizeInBits, k);
            Assert.assertEquals(ewah.cardinality(), 0);
            EWAHCompressedBitmap ewah2 = new EWAHCompressedBitmap();
            ewah2.setSizeInBits(k, false);
            Assert.assertEquals(ewah2.sizeInBits, k);
            Assert.assertEquals(ewah2.cardinality(), 0);
            EWAHCompressedBitmap ewah3 = new EWAHCompressedBitmap();
            for (int i = 0; i < k; ++i) {
                ewah3.set(i);
            }
            Assert.assertEquals(ewah3.sizeInBits, k);
            Assert.assertEquals(ewah3.cardinality(), k);
            EWAHCompressedBitmap ewah4 = new EWAHCompressedBitmap();
            ewah4.setSizeInBits(k, true);
            Assert.assertEquals(ewah4.sizeInBits, k);
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
    @SuppressWarnings("ForLoopReplaceableByForEach")
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
    public void testHashCode() {
        System.out.println("testing hashCode");
        EWAHCompressedBitmap ewcb = EWAHCompressedBitmap.bitmapOf(50,
                70).and(EWAHCompressedBitmap.bitmapOf(50, 1000));
        Assert.assertEquals(EWAHCompressedBitmap.bitmapOf(50), ewcb);
        Assert.assertEquals(EWAHCompressedBitmap.bitmapOf(50)
                .hashCode(), ewcb.hashCode());
        ewcb.addWord(~0l);
        EWAHCompressedBitmap ewcb2 = ewcb.clone();
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
    public void TestCloneEwahCompressedBitArray() {
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
