package com.googlecode.javaewah;

import java.util.Iterator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Convenience functions for working over iterators
 */
public final class IteratorUtil {

    /** Private constructor to prevent instantiation */
    private IteratorUtil() {}

    /**
     * @param i iterator we wish to iterate over
     * @return an iterator over the set bits corresponding to the iterator
     */
    public static IntIterator toSetBitsIntIterator(final IteratingRLW i) {
        return new IntIteratorOverIteratingRLW(i);
    }

    /**
     * @param i iterator we wish to iterate over
     * @return an iterator over the set bits corresponding to the iterator
     */
    public static Iterator<Integer> toSetBitsIterator(final IteratingRLW i) {
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
     * Generate a bitmap from an iterator.
     * 
     * 
     *
     * @param i iterator we wish to materialize
     * @param c where we write
     */
    public static void materialize(final IteratingRLW i,
                                   final BitmapStorage c) {
        while (true) {
            if (i.getRunningLength() > 0) {
                c.addStreamOfEmptyWords(i.getRunningBit(), i.getRunningLength());
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
    public static int cardinality(final IteratingRLW i) {
        int answer = 0;
        while (true) {
            if (i.getRunningBit())
                answer += (int) (i.getRunningLength() * EWAHCompressedBitmap.WORD_IN_BITS);
            int lw = i.getNumberOfLiteralWords();
            for (int k = 0; k < lw ; ++k)
                answer += Long.bitCount(i.getLiteralWordAt(k));
            if (!i.next())
                break;
        }
        return answer;
    }

    /**
     * @param x set of bitmaps
     * @return an array of iterators corresponding to the array of bitmaps
     */
    public static IteratingRLW[] toIterators(
            final EWAHCompressedBitmap... x) {
        IteratingRLW[] X = new IteratingRLW[x.length];
        for (int k = 0; k < X.length; ++k) {
            X[k] = new IteratingBufferedRunningLengthWord(x[k]);
        }
        return X;
    }

    /**
     * Turn an iterator into a bitmap.
     *
     * @param i   iterator we wish to materialize
     * @param c   where we write
     * @param max maximum number of words we wish to materialize
     * @return how many words were actually materialized
     */
    public static long materialize(final IteratingRLW i,
                                   final BitmapStorage c, long max) {
        final long origMax = max;
        while (true) {
            if (i.getRunningLength() > 0) {
                long L = i.getRunningLength();
                if (L > max)
                    L = max;
                c.addStreamOfEmptyWords(i.getRunningBit(), L);
                max -= L;
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
     * Turn an iterator into a bitmap.
     * 
     * This can be used to effectively clone a bitmap in the following
     * manner:
     * 
     *  <code>
     *  EWAHCompressedBitmap n = IteratorUtil.materialize(bitmap.getIteratingRLW()));
     *  n.setSizeInBitsWithinLastWord(bitmap.sizeInBits());
     *  </code>
     *
     * @param i iterator we wish to materialize
     * @return materialized version of the iterator
     */
    public static EWAHCompressedBitmap materialize(final IteratingRLW i) {
        EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
        materialize(i, ewah);
        return ewah;
    }
}
