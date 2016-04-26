package com.googlecode.javaewah;

import org.junit.Test;


import static org.junit.Assert.*;
/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.Iterator;

/**
 * Tests for utility class.
 */
@SuppressWarnings("javadoc")
public class IntIteratorOverIteratingRLWTest {
	
	@Test
	public void iteratorAggregation() {
		EWAHCompressedBitmap e1 = EWAHCompressedBitmap.bitmapOf(0, 2, 1000, 10001);
		EWAHCompressedBitmap e2 = new EWAHCompressedBitmap();
		for (int k = 64; k < 450; ++k)
			e2.set(k);
		EWAHCompressedBitmap e3 = new EWAHCompressedBitmap();
		for (int k = 64; k < 450; ++k)
			e2.set(400 * k);
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedand(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedand(1024, e1, e2, e3));
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedor(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedor(1024, e1, e2, e3));
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedxor(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedxor(1024, e1, e2, e3));
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedand(500, e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedand(1024, e1, e2, e3));
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedor(500, e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedor(1024, e1, e2, e3));
		assertEquals(IteratorUtil.materialize(
				IteratorAggregation.bufferedxor(500, e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation.bufferedxor(1024, e1, e2, e3));
	}

    @Test
    // had problems with bitmaps beginning with two consecutive clean runs
    public void testConsecClean() {
        System.out
                .println("testing int iteration, 2 consec clean runs starting with zeros");
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();
        for (int i = 64; i < 128; ++i)
            e.set(i);
        
        IntIterator ii = IteratorUtil.toSetBitsIntIterator(e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(64, ctr);
        Iterator iii = IteratorUtil.toSetBitsIterator(e.getIteratingRLW());
        assertTrue(iii.hasNext());
        ctr = 0;
        while (iii.hasNext()) {
            ++ctr;
            iii.next();
        }
        assertEquals(64, ctr);

    }
    
    @Test
    public void testMaterialize() {
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();
        for (int i = 64; i < 128; ++i)
            e.set(333 * i);
        assertEquals(e.cardinality(), IteratorUtil.cardinality(e.getIteratingRLW()));
        EWAHCompressedBitmap newe = new EWAHCompressedBitmap();
        IteratorUtil.materialize(e.getIteratingRLW(), newe);
        assertEquals(e,newe);
        newe.clear();
        IteratorUtil.materialize(e.getIteratingRLW(), newe,4096);
        assertEquals(e,newe);
    }

    @Test
    public void testConsecCleanStartOnes() {
        System.out
                .println("testing int iteration, 2 consec clean runs starting with ones");
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();
        for (int i = 0; i < 2 * 64; ++i)
            e.set(i);
        for (int i = 4 * 64; i < 5 * 64; ++i)
            e.set(i);

        IntIteratorOverIteratingRLW ii = new IntIteratorOverIteratingRLW(
                e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(3 * 64, ctr);
    }

    @Test
    public void testStartDirty() {
        System.out.println("testing int iteration, no initial runs");
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();
        for (int i = 1; i < 2 * 64; ++i)
            e.set(i);
        for (int i = 4 * 64; i < 5 * 64; ++i)
            e.set(i);

        IntIteratorOverIteratingRLW ii = new IntIteratorOverIteratingRLW(
                e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(3 * 64 - 1, ctr);
    }

    @Test
    public void testEmpty() {
        System.out.println("testing int iteration over empty bitmap");
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();

        IntIteratorOverIteratingRLW ii = new IntIteratorOverIteratingRLW(
                e.getIteratingRLW());
        assertFalse(ii.hasNext());
    }

    @Test
    public void testRandomish() {
        EWAHCompressedBitmap e = new EWAHCompressedBitmap();

        int upperlimit = 100000;
        for (int i = 0; i < upperlimit; ++i) {
            double probabilityOfOne = i / (double) (upperlimit / 2);
            if (probabilityOfOne > 1.0)
                probabilityOfOne = 1.0;
            if (Math.random() < probabilityOfOne) {
                e.set(i);
            }
        }

        IntIteratorOverIteratingRLW ii = new IntIteratorOverIteratingRLW(
                e.getIteratingRLW());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }

        assertEquals(e.cardinality(), ctr);
        System.out
                .println("checking int iteration over a var density bitset of size "
                        + e.cardinality());

    }

}
