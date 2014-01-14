package com.googlecode.javaewah.symmetric;

import java.util.ArrayList;
import java.util.Arrays;
import com.googlecode.javaewah.BitmapStorage;

/**
 * A threshold Boolean function returns true if the number of true values exceed
 * a threshold. It is a symmetric Boolean function.
 * 
 * This object is not thread safe: you should use one function per thread.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @author Daniel Lemire
 * @since 0.8.0
 * 
 */
public final class ThresholdFuncBitmap extends UpdateableBitmapFunction {
        private int min;
        private long[] buffers;
        private int bufferUsed;
        private int[] bufcounters = new int[64];
        private static final int[] zeroes64 = new int[64];

        /**
         * Construction a threshold function with a given threshold
         * 
         * @param min
         *                threshold
         */
        public ThresholdFuncBitmap(final int min) {
                super();
                this.min = min;
                buffers = new long[16];
                bufferUsed = 0;
        }


        @Override
        public void dispatch(BitmapStorage out, int runbegin, int runend) {
                final int runlength = runend - runbegin;
                if (hammingWeight >= min) {
                        out.addStreamOfEmptyWords(true, runlength);
                        return;
                } else if (litWeight + hammingWeight < min) {
                        out.addStreamOfEmptyWords(false, runlength);
                } else {
                        final int deficit = min - hammingWeight;
                        if (deficit == 1) {
                                orLiterals(out, runbegin, runlength);
                                return;
                        }
                        bufferUsed = this.getNumberOfLiterals();
                        if (bufferUsed == deficit) {
                                andLiterals(out, runbegin, runlength);
                        } else {
                                generalLiterals(deficit, out, runbegin,
                                        runlength);
                        }
                }
        }

        private long threshold2buf(final int T, final long[] buffers, final int bufUsed) {
                long result = 0L;
                final int[] counters = bufcounters;
                System.arraycopy(zeroes64, 0, counters, 0, 64);
                for (int k = 0; k < bufUsed; ++k) {
                        long bitset = buffers[k];
                        while (bitset != 0) {
                                long t = bitset & -bitset;
                                counters[Long.bitCount(t - 1)]++;
                                bitset ^= t;
                        }
                }
                for (int pos = 0; pos < 64; ++pos) {
                        if (counters[pos] >= T)
                                result |= (1L << pos);
                }
                return result;
        }

        private static long threshold3(final int T, final long[] buffers, final int bufUsed) {
                if (buffers.length == 0)
                        return 0;
                final long[] v = new long[T];
                v[0] = buffers[0];
                for (int k = 1; k < bufUsed; ++k) {
                        final long c = buffers[k];
                        final int m = Math.min(T - 1, k);
                        for (int j = m; j >= 1; --j) {
                                v[j] |= (c & v[j - 1]);
                        }
                        v[0] |= c;
                }
                return v[T - 1];
        }

        private long threshold4(final int T, final long[] buffers, final int bufUsed) {
                if (T >= 128)
                        return threshold2buf(T, buffers, bufUsed);
                int B = 0;
                for (int k = 0; k < bufUsed; ++k)
                        B += Long.bitCount(buffers[k]);
                if (2 * B >= bufUsed * T)
                        return threshold3(T, buffers, bufUsed);
                else
                        return threshold2buf(T, buffers, bufUsed);
        }


        private final void orLiterals(final BitmapStorage out, final int runbegin,
                final int runlength) {
                for (int i = 0; i < runlength; ++i) {
                        long w = 0;
                        for (EWAHPointer R : this.getLiterals()) {
                                w |= R.iterator.getLiteralWordAt(i + runbegin
                                        - R.beginOfRun());
                        }
                        out.add(w);
                }
        }

        private final void andLiterals(final BitmapStorage out, final int runbegin,
                final int runlength) {
                for (int i = 0; i < runlength; ++i) {
                        long w = ~0;
                        for (EWAHPointer R : this.getLiterals()) {
                                w &= R.iterator.getLiteralWordAt(i + runbegin
                                        - R.beginOfRun());
                        }
                        out.add(w);
                }
        }

        private final void generalLiterals(final int deficit, final BitmapStorage out,
                final int runbegin, final int runlength) {
                if (bufferUsed > buffers.length)
                        buffers = Arrays.copyOf(buffers, 2 * bufferUsed);
                for (int i = 0; i < runlength; ++i) {
                        int p = 0;
                        for (EWAHPointer R : this.getLiterals()) {
                                buffers[p++] = R.iterator.getLiteralWordAt(i
                                        + runbegin - R.beginOfRun());
                        }
                        out.add(threshold4(deficit, buffers, bufferUsed));
                }
        }

}
