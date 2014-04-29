package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah32.BitmapStorage32;

import java.util.Arrays;

/**
 * A threshold Boolean function returns true if the number of true values exceed
 * a threshold. It is a symmetric Boolean function.
 * 
 * This class implements an algorithm described in the following paper:
 * 
 * Owen Kaser and Daniel Lemire, Compressed bitmap indexes: beyond unions and intersections
 * <a href="http://arxiv.org/abs/1402.4466">http://arxiv.org/abs/1402.4466</a>
 * 
 * It is not thread safe: you should use one object per thread.
 *
 * @author Daniel Lemire
 * @see <a
 * href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @since 0.8.2
 */
public final class ThresholdFuncBitmap32 extends UpdateableBitmapFunction32 {
    private final int min;
    private int[] buffers;
    private int bufferUsed;
    private final int[] bufcounters = new int[64];
    private static final int[] zeroes64 = new int[64];

    /**
     * Construction a threshold function with a given threshold
     *
     * @param min threshold
     */
    public ThresholdFuncBitmap32(final int min) {
        super();
        this.min = min;
        this.buffers = new int[16];
        this.bufferUsed = 0;
    }

    @Override
    public void dispatch(BitmapStorage32 out, int runBegin, int runend) {
        final int runLength = runend - runBegin;
        if (this.hammingWeight >= this.min) {
            out.addStreamOfEmptyWords(true, runLength);
        } else if (this.litWeight + this.hammingWeight < this.min) {
            out.addStreamOfEmptyWords(false, runLength);
        } else {
            final int deficit = this.min - this.hammingWeight;
            if (deficit == 1) {
                orLiterals(out, runBegin, runLength);
                return;
            }
            this.bufferUsed = this.getNumberOfLiterals();
            if (this.bufferUsed == deficit) {
                andLiterals(out, runBegin, runLength);
            } else {
                generalLiterals(deficit, out, runBegin,
                        runLength);
            }
        }
    }

    private int threshold2buf(final int t, final int[] buf,
                              final int bufUsed) {
        int result = 0;
        final int[] counters = this.bufcounters;
        System.arraycopy(zeroes64, 0, counters, 0, 64);
        for (int k = 0; k < bufUsed; ++k) {
            int bitset = buf[k];
            while (bitset != 0) {
                int t2 = bitset & -bitset;
                counters[Integer.bitCount(t2 - 1)]++;
                bitset ^= t2;
            }
        }
        for (int pos = 0; pos < 64; ++pos) {
            if (counters[pos] >= t)
                result |= (1L << pos);
        }
        return result;
    }

    private static int threshold3(final int t, final int[] buffers, final int bufUsed) {
        if (buffers.length == 0)
            return 0;
        final int[] v = new int[t];
        v[0] = buffers[0];
        for (int k = 1; k < bufUsed; ++k) {
            final int c = buffers[k];
            final int m = Math.min(t - 1, k);
            for (int j = m; j >= 1; --j) {
                v[j] |= (c & v[j - 1]);
            }
            v[0] |= c;
        }
        return v[t - 1];
    }

    private int threshold4(final int t, final int[] buf, final int bufUsed) {
        if (t >= 128)
            return threshold2buf(t, buf, bufUsed);
        int b = 0;
        for (int k = 0; k < bufUsed; ++k)
            b += Integer.bitCount(buf[k]);

        if (2 * b >= bufUsed * t)
            return threshold3(t, buf, bufUsed);
        else
            return threshold2buf(t, buf, bufUsed);
    }

    private void orLiterals(final BitmapStorage32 out, final int runBegin, final int runLength) {
        for (int i = 0; i < runLength; ++i) {
            int w = 0;
            for (EWAHPointer32 r : this.getLiterals()) {
                w |= r.iterator.getLiteralWordAt(i + runBegin - r.beginOfRun());
            }
            out.addWord(w);
        }
    }

    private void andLiterals(final BitmapStorage32 out, final int runBegin, final int runLength) {
        for (int i = 0; i < runLength; ++i) {
            int w = ~0;
            for (EWAHPointer32 r : this.getLiterals()) {
                w &= r.iterator.getLiteralWordAt(i + runBegin - r.beginOfRun());
            }
            out.addWord(w);
        }
    }

    private void generalLiterals(final int deficit, final BitmapStorage32 out,
                                 final int runBegin, final int runLength) {
        if (this.bufferUsed > this.buffers.length)
            this.buffers = Arrays.copyOf(this.buffers, 2 * this.bufferUsed);
        for (int i = 0; i < runLength; ++i) {
            int p = 0;
            for (EWAHPointer32 r : this.getLiterals()) {
                this.buffers[p++] = r.iterator.getLiteralWordAt(i + runBegin - r.beginOfRun());
            }
            out.addWord(threshold4(deficit, this.buffers, this.bufferUsed));
        }
    }
}
