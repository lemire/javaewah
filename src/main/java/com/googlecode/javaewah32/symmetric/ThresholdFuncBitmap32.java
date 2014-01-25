package com.googlecode.javaewah32.symmetric;

import java.util.Arrays;
import com.googlecode.javaewah32.BitmapStorage32;

/**
 * A threshold Boolean function returns true if the number of true values exceed
 * a threshold. It is a symmetric Boolean function.
 * 
 * This object is not thread safe: you should use one function per thread.
 * 
 * @see <a
 *      href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @author Daniel Lemire
 * @since 0.8.2
 * 
 */
public final class ThresholdFuncBitmap32 extends UpdateableBitmapFunction32 {
        private int min;
        private int[] buffers;
        private int bufferUsed;
        private int[] bufcounters = new int[64];
        private static final int[] zeroes64 = new int[64];

        /**
         * Construction a threshold function with a given threshold
         * 
         * @param min
         *                threshold
         */
        public ThresholdFuncBitmap32(final int min) {
                super();
                this.min = min;
                this.buffers = new int[16];
                this.bufferUsed = 0;
        }

        @Override
        public void dispatch(BitmapStorage32 out, int runbegin, int runend) {
                final int runlength = runend - runbegin;
                if (this.hammingWeight >= this.min) {
                        out.addStreamOfEmptyWords(true, runlength);
                        return;
                } else if (this.litWeight + this.hammingWeight < this.min) {
                        out.addStreamOfEmptyWords(false, runlength);
                } else {
                        final int deficit = this.min - this.hammingWeight;
                        if (deficit == 1) {
                                orLiterals(out, runbegin, runlength);
                                return;
                        }
                        this.bufferUsed = this.getNumberOfLiterals();
                        if (this.bufferUsed == deficit) {
                                andLiterals(out, runbegin, runlength);
                        } else {
                                generalLiterals(deficit, out, runbegin,
                                        runlength);
                        }
                }
        }

        private int threshold2buf(final int T, final int[] buf,
                final int bufUsed) {
                int result = 0;
                final int[] counters = this.bufcounters;
                System.arraycopy(zeroes64, 0, counters, 0, 64);
                for (int k = 0; k < bufUsed; ++k) {
                        int bitset = buf[k];
                        while (bitset != 0) {
                                int t = bitset & -bitset;
                                counters[Integer.bitCount(t - 1)]++;
                                bitset ^= t;
                        }
                }
                for (int pos = 0; pos < 64; ++pos) {
                        if (counters[pos] >= T)
                                result |= (1L << pos);
                }
                return result;
        }

        private static int threshold3(final int T, final int[] buffers,
                final int bufUsed) {
                if (buffers.length == 0)
                        return 0;
                final int[] v = new int[T];
                v[0] = buffers[0];
                for (int k = 1; k < bufUsed; ++k) {
                        final int c = buffers[k];
                        final int m = Math.min(T - 1, k);
                        for (int j = m; j >= 1; --j) {
                                v[j] |= (c & v[j - 1]);
                        }
                        v[0] |= c;
                }
                return v[T - 1];
        }

        private int threshold4(final int T, final int[] buf, final int bufUsed) {
                if (T >= 128)
                        return threshold2buf(T, buf, bufUsed);
                int B = 0;
                for (int k = 0; k < bufUsed; ++k)
                        B += Integer.bitCount(buf[k]);
                if (2 * B >= bufUsed * T)
                        return threshold3(T, buf, bufUsed);
                return threshold2buf(T, buf, bufUsed);
        }

        private final void orLiterals(final BitmapStorage32 out,
                final int runbegin, final int runlength) {
                for (int i = 0; i < runlength; ++i) {
                        int w = 0;
                        for (EWAHPointer32 R : this.getLiterals()) {
                                w |= R.iterator.getLiteralWordAt(i + runbegin
                                        - R.beginOfRun());
                        }
                        out.add(w);
                }
        }

        private final void andLiterals(final BitmapStorage32 out,
                final int runbegin, final int runlength) {
                for (int i = 0; i < runlength; ++i) {
                        int w = ~0;
                        for (EWAHPointer32 R : this.getLiterals()) {
                                w &= R.iterator.getLiteralWordAt(i + runbegin
                                        - R.beginOfRun());
                        }
                        out.add(w);
                }
        }

        private final void generalLiterals(final int deficit,
                final BitmapStorage32 out, final int runbegin, final int runlength) {
                if (this.bufferUsed > this.buffers.length)
                        this.buffers = Arrays.copyOf(this.buffers,
                                2 * this.bufferUsed);
                for (int i = 0; i < runlength; ++i) {
                        int p = 0;
                        for (EWAHPointer32 R : this.getLiterals()) {
                                this.buffers[p++] = R.iterator
                                        .getLiteralWordAt(i + runbegin
                                                - R.beginOfRun());
                        }
                        out.add(threshold4(deficit, this.buffers,
                                this.bufferUsed));
                }
        }

}
