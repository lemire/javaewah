package com.googlecode.javaewah32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under APL 2.0.
 */
/**
 * Tests for utility class. Sketchy for now.
 * 
 */
public class IntIteratorOverIteratingRLWTest32 {

        @Test
        // had problems with bitmaps beginning with two consecutive clean runs
        public void testConsecClean() {
                System.out
                        .println("testing int iteration, 2 consec clean runs starting with zeros");
                EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
                for (int i = 64; i < 128; ++i)
                        e.set(i);
                IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(
                        e.getIteratingRLW());
                assertTrue(ii.hasNext());
                int ctr = 0;
                while (ii.hasNext()) {
                        ++ctr;
                        ii.next();
                }
                assertEquals(64, ctr);
        }

        @Test
        public void testConsecCleanStartOnes() {
                System.out
                        .println("testing int iteration, 2 consec clean runs starting with ones");
                EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
                for (int i = 0; i < 2 * 64; ++i)
                        e.set(i);
                for (int i = 4 * 64; i < 5 * 64; ++i)
                        e.set(i);

                IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(
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
                EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();
                for (int i = 1; i < 2 * 64; ++i)
                        e.set(i);
                for (int i = 4 * 64; i < 5 * 64; ++i)
                        e.set(i);

                IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(
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
                EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();

                IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(
                        e.getIteratingRLW());
                assertFalse(ii.hasNext());
        }

        @Test
        public void testRandomish() {
                EWAHCompressedBitmap32 e = new EWAHCompressedBitmap32();

                int upperlimit = 100000;
                for (int i = 0; i < upperlimit; ++i) {
                        double probabilityOfOne = i / (double) (upperlimit / 2);
                        if (probabilityOfOne > 1.0)
                                probabilityOfOne = 1.0;
                        if (Math.random() < probabilityOfOne) {
                                e.set(i);
                        }
                }

                IntIteratorOverIteratingRLW32 ii = new IntIteratorOverIteratingRLW32(
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
