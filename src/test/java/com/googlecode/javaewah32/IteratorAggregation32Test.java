package com.googlecode.javaewah32;

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.maxSizeInBits;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import com.googlecode.javaewah.synth.ClusteredDataGenerator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Tests specifically for iterators.
 */
public class IteratorAggregation32Test {

    /**
     * @param N   number of bitmaps to generate in each set
     * @param nbr parameter determining the size of the arrays (in a log
     *            scale)
     * @return an iterator over sets of bitmaps
     */
    public static Iterator<EWAHCompressedBitmap32[]> getCollections(
            final int N, final int nbr) {
        final ClusteredDataGenerator cdg = new ClusteredDataGenerator(
                123);
        return new Iterator<EWAHCompressedBitmap32[]>() {
            int sparsity = 1;

            @Override
            public boolean hasNext() {
                return this.sparsity < 5;
            }

            @Override
            public EWAHCompressedBitmap32[] next() {
                int[][] data = new int[N][];
                int Max = (1 << (nbr + this.sparsity));
                for (int k = 0; k < N; ++k)
                    data[k] = cdg.generateClustered(
                            1 << nbr, Max);
                EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
                for (int k = 0; k < N; ++k) {
                    ewah[k] = new EWAHCompressedBitmap32();
                    for (int x = 0; x < data[k].length; ++x) {
                        ewah[k].set(data[k][x]);
                    }
                    data[k] = null;
                }
                this.sparsity += 3;
                return ewah;
            }

            @Override
            public void remove() {
                // unimplemented
            }

        };

    }

    /**
     *
     */
    @Test
    public void testAnd() {
        for (int N = 1; N < 10; ++N) {
            System.out.println("testAnd N = " + N);
            Iterator<EWAHCompressedBitmap32[]> i = getCollections(
                    N, 3);
            while (i.hasNext()) {
                EWAHCompressedBitmap32[] x = i.next();
                EWAHCompressedBitmap32 tanswer = EWAHCompressedBitmap32
                        .and(x);
                EWAHCompressedBitmap32 x1 = IteratorUtil32
                        .materialize(IteratorAggregation32
                                .bufferedand(IteratorUtil32
                                        .toIterators(x)));
                x1.setSizeInBits(maxSizeInBits(x), false);
                x1.setSizeInBitsWithinLastWord(maxSizeInBits(x));
                assertTrue(x1.equals(tanswer));
            }
            System.gc();
        }

    }

    /**
     *
     */
    @Test
    public void testOr() {
        for (int N = 1; N < 10; ++N) {
            System.out.println("testOr N = " + N);
            Iterator<EWAHCompressedBitmap32[]> i = getCollections(
                    N, 3);
            while (i.hasNext()) {
                EWAHCompressedBitmap32[] x = i.next();
                EWAHCompressedBitmap32 tanswer = EWAHCompressedBitmap32
                        .or(x);
                EWAHCompressedBitmap32 x1 = IteratorUtil32
                        .materialize(IteratorAggregation32
                                .bufferedor(IteratorUtil32
                                        .toIterators(x)));
                assertTrue(x1.equals(tanswer));
            }
            System.gc();
        }
    }

    /**
     *
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testWideOr() {
        for (int nbr = 3; nbr <= 24; nbr += 3) {
            for (int N = 100; N < 1000; N += 100) {
                System.out.println("testWideOr N = " + N);
                Iterator<EWAHCompressedBitmap32[]> i = getCollections(
                        N, 3);
                while (i.hasNext()) {
                    EWAHCompressedBitmap32[] x = i.next();
                    EWAHCompressedBitmap32 tanswer = EWAHCompressedBitmap32
                            .or(x);
                    EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
                    FastAggregation32
                            .orToContainer(
                                    container, x);
                    assertTrue(container.equals(tanswer));
                    EWAHCompressedBitmap32 x1 = IteratorUtil32
                            .materialize(IteratorAggregation32
                                    .bufferedor(IteratorUtil32
                                            .toIterators(x)));
                    assertTrue(x1.equals(tanswer));
                }
                System.gc();
            }
        }
    }

    /**
     *
     */
    @Test
    public void testXor() {
        System.out.println("testXor ");
        Iterator<EWAHCompressedBitmap32[]> i = getCollections(2, 3);
        while (i.hasNext()) {
            EWAHCompressedBitmap32[] x = i.next();
            EWAHCompressedBitmap32 tanswer = x[0].xor(x[1]);
            EWAHCompressedBitmap32 x1 = IteratorUtil32
                    .materialize(IteratorAggregation32.bufferedxor(
                            x[0].getIteratingRLW(),
                            x[1].getIteratingRLW()));
            assertTrue(x1.equals(tanswer));
        }
        System.gc();
    }
    
    /**
    *
    */
   @Test
   public void testMat() throws Exception {
       System.out.println("testMat ");
       EWAHCompressedBitmap32 b = EWAHCompressedBitmap32.bitmapOf(0,3);
       EWAHCompressedBitmap32 n = IteratorUtil32.materialize(b.getIteratingRLW());
       assertTrue(n.sizeInBits() == 32);
       n.setSizeInBitsWithinLastWord(b.sizeInBits());
       assertTrue(n.sizeInBits() == b.sizeInBits());
       assertTrue(n.equals(b));
       EWAHCompressedBitmap32 neg = IteratorUtil32.materialize(IteratorAggregation32.not(b.getIteratingRLW()));
       neg.setSizeInBitsWithinLastWord(b.sizeInBits());
       EWAHCompressedBitmap32 x= b.clone();
       x.not();
       assertTrue(x.equals(neg));
       for(int k = 145; k<1024; ++k)
           b.set(k);
       n = IteratorUtil32.materialize(b.getIteratingRLW());
       assertTrue(n.sizeInBits()/64 * 64 == n.sizeInBits());
       n.setSizeInBitsWithinLastWord(b.sizeInBits());
       assertTrue(n.sizeInBits() == b.sizeInBits());
       assertTrue(n.equals(b));
       neg = IteratorUtil32.materialize(IteratorAggregation32.not(b.getIteratingRLW()));
       neg.setSizeInBitsWithinLastWord(b.sizeInBits());
       x= b.clone();
       x.not();
       assertTrue(x.equals(neg));
   }

}
