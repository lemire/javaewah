package com.googlecode.javaewah32;

import com.googlecode.javaewah.IntIterator;

import java.util.Iterator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Convenience functions for working over iterators
 */
public final class IteratorUtil32 {

    /** Private constructor to prevent instantiation */
    private IteratorUtil32() {}

    /**
     * @param i iterator we wish to iterate over
     * @return an iterator over the set bits corresponding to the iterator
     */
    public static IntIterator toSetBitsIntIterator(final IteratingRLW32 i) {
        return new IntIteratorOverIteratingRLW32(i);
    }

    /**
     * @param i iterator we wish to iterate over
     * @return an iterator over the set bits corresponding to the iterator
     */
    public static Iterator<Integer> toSetBitsIterator(final IteratingRLW32 i) {
        return new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return this.under.hasNext();
            }

            @Override
            public Integer next() {
                return this.under.next();
            }

            @Override
            public void remove() {
            }

            private final IntIterator under = toSetBitsIntIterator(i);
        };

    }

    /**
     * Turn an iterator into a bitmap
     *
     * @param i iterator we wish to materialize
     * @param c where we write
     */
    public static void materialize(final IteratingRLW32 i,
                                   final BitmapStorage32 c) {
        while (true) {
            if (i.getRunningLength() > 0) {
                c.addStreamOfEmptyWords(i.getRunningBit(),
                        i.getRunningLength());
            }
            int il = i.getNumberOfLiteralWords();
            for (int k = 0; k < il ; ++k)
                c.addWord(i.getLiteralWordAt(k));
            if (!i.next())
                break;
        }
    }

    /**
     * @param i iterator we wish to iterate over
     * @return the cardinality (number of set bits) corresponding to the
     * iterator
     */
    public static int cardinality(final IteratingRLW32 i) {
        int answer = 0;
        while (true) {
            if (i.getRunningBit())
                answer += i.getRunningLength()
                        * EWAHCompressedBitmap32.WORD_IN_BITS;
            int il = i.getNumberOfLiteralWords();
            for (int k = 0; k < il; ++k)
                answer += Integer.bitCount(i.getLiteralWordAt(k));
            if (!i.next())
                break;
        }
        return answer;
    }

    /**
     * @param x set of bitmaps we wish to iterate over
     * @return an array of iterators corresponding to the array of bitmaps
     */
    public static IteratingRLW32[] toIterators(
            final EWAHCompressedBitmap32... x) {
        IteratingRLW32[] X = new IteratingRLW32[x.length];
        for (int k = 0; k < X.length; ++k) {
            X[k] = new IteratingBufferedRunningLengthWord32(x[k]);
        }
        return X;
    }

    /**
     * Turn an iterator into a bitmap
     *
     * @param i   iterator we wish to materialize
     * @param c   where we write
     * @param max maximum number of words to materialize
     * @return how many words were actually materialized
     */
    public static long materialize(final IteratingRLW32 i, final BitmapStorage32 c, int max) {
        final int origMax = max;
        while (true) {
            if (i.getRunningLength() > 0) {
                int l = i.getRunningLength();
                if (l > max)
                    l = max;
                c.addStreamOfEmptyWords(i.getRunningBit(), l);
                max -= l;
            }
            long L = i.getNumberOfLiteralWords();
            for (int k = 0; k < L; ++k)
                c.addWord(i.getLiteralWordAt(k));
            if (max > 0) {
                if (!i.next())
                    break;
            } else
                break;
        }
        return origMax - max;
    }

    /**
     * Turn an iterator into a bitmap
     * 
     * 
     * This can be used to effectively clone a bitmap in the following
     * manner:
     * 
     *  <code>
     *  EWAHCompressedBitmap32 n = IteratorUtil32.materialize(bitmap.getIteratingRLW()));
     *  n.setSizeInBitsWithinLastWord(bitmap.sizeInBits());
     *  </code>
     *
     * @param i iterator we wish to materialize
     * @return materialized version of the iterator
     */
    public static EWAHCompressedBitmap32 materialize(final IteratingRLW32 i) {
        EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
        materialize(i, ewah);
        return ewah;
    }

}
