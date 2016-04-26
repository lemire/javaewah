package com.googlecode.javaewah32;

import org.junit.Test;

import com.googlecode.javaewah.IntIterator;

import static org.junit.Assert.*;

import java.util.Iterator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Tests for utility class.
 */
@SuppressWarnings("javadoc")
public class IntIteratorOverIteratingRLW32Test {
	@Test
	public void iteratorAggregation() {
		EWAHCompressedBitmap32 e1 = EWAHCompressedBitmap32.bitmapOf(0, 2, 1000, 10001);
		EWAHCompressedBitmap32 e2 = new EWAHCompressedBitmap32();
		for (int k = 64; k < 450; ++k)
			e2.set(k);
		EWAHCompressedBitmap32 e3 = new EWAHCompressedBitmap32();
		for (int k = 64; k < 450; ++k)
			e2.set(400 * k);
		assertEquals(IteratorUtil32.materialize(
				IteratorAggregation32.bufferedand(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation32.bufferedand(1024, e1, e2, e3));
		assertEquals(IteratorUtil32.materialize(
				IteratorAggregation32.bufferedor(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation32.bufferedor(1024, e1, e2, e3));
		assertEquals(IteratorUtil32.materialize(
				IteratorAggregation32.bufferedxor(e1.getIteratingRLW(), e2.getIteratingRLW(), e3.getIteratingRLW())),
				FastAggregation32.bufferedxor(1024, e1, e2, e3));
		assertEquals(IteratorUtil32.materialize(IteratorAggregation32.bufferedand(500, e1.getIteratingRLW(),
				e2.getIteratingRLW(), e3.getIteratingRLW())), FastAggregation32.bufferedand(1024, e1, e2, e3));
		assertEquals(IteratorUtil32.materialize(IteratorAggregation32.bufferedor(500, e1.getIteratingRLW(),
				e2.getIteratingRLW(), e3.getIteratingRLW())), FastAggregation32.bufferedor(1024, e1, e2, e3));
		assertEquals(IteratorUtil32.materialize(IteratorAggregation32.bufferedxor(500, e1.getIteratingRLW(),
				e2.getIteratingRLW(), e3.getIteratingRLW())), FastAggregation32.bufferedxor(1024, e1, e2, e3));
	}
    @Test
    // had problems with bitmaps beginning with two consecutive clean runs
    public void testConsecClean() {
        System.out.println("testing int iteration, 2 consec clean runs starting with zeros");
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
        for (int i = 32; i < 64; ++i)
            e.set(i);
        IntIterator ii = IteratorUtil32.toSetBitsIntIterator(e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(32, ctr);
        Iterator iii = IteratorUtil32.toSetBitsIterator(e.getIteratingRLW());
        assertTrue(iii.hasNext());
        ctr = 0;
        while (iii.hasNext()) {
            ++ctr;
            iii.next();
        }
        assertEquals(32, ctr);

    }
    

    @Test
    public void testMaterialize() {
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
        for (int i = 64; i < 128; ++i)
            e.set(333 * i);
        assertEquals(e.cardinality(), IteratorUtil32.cardinality(e.getIteratingRLW()));
        EWAHCompressedBitmap32 newe = new EWAHCompressedBitmap32();
        IteratorUtil32.materialize(e.getIteratingRLW(), newe);
        assertEquals(e,newe);
        newe.clear();
        IteratorUtil32.materialize(e.getIteratingRLW(), newe,4096);
        assertEquals(e,newe);
    }

    @Test
    public void testConsecCleanStartOnes() {
        System.out
                .println("testing int iteration, 2 consec clean runs starting with ones");
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
        for (int i = 0; i < 2 * 32; ++i)
            e.set(i);
        for (int i = 4 * 32; i < 5 * 32; ++i)
            e.set(i);

        IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(3 * 32, ctr);
    }

    @Test
    public void testStartDirty() {
        System.out.println("testing int iteration, no initial runs");
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
        for (int i = 1; i < 2 * 32; ++i)
            e.set(i);
        for (int i = 4 * 32; i < 5 * 32; ++i)
            e.set(i);

        IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(e.getIteratingRLW());
        assertTrue(ii.hasNext());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }
        assertEquals(3 * 32 - 1, ctr);
    }

    @Test
    public void testEmpty() {
        System.out.println("testing int iteration over empty bitmap");
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();

        IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(e.getIteratingRLW());
        assertFalse(ii.hasNext());
    }

    @Test
    public void testRandomish() {
        EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();

        int upperLimit = 100000;
        for (int i = 0; i < upperLimit; ++i) {
            double probabilityOfOne = i / (double) (upperLimit / 2);
            if (probabilityOfOne > 1.0)
                probabilityOfOne = 1.0;
            if (Math.random() < probabilityOfOne) {
                e.set(i);
            }
        }

        IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(e.getIteratingRLW());
        int ctr = 0;
        while (ii.hasNext()) {
            ++ctr;
            ii.next();
        }

        assertEquals(e.cardinality(), ctr);
        System.out.println("checking int iteration over a var density bitset of size " + e.cardinality());

    }

}
