package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.BitmapStorage;

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
 * @since 0.8.0
 */
public final class ThresholdFuncBitmap extends UpdateableBitmapFunction {
    private final int min;
    private long[] buffers;
    private int bufferUsed;
    private final int[] bufCounters = new int[64];
    private static final int[] zeroes64 = new int[64];

    /**
     * Construction a threshold function with a given threshold
     *
     * @param min threshold
     */
    public ThresholdFuncBitmap(final int min) {
        super();
        this.min = min;
        this.buffers = new long[16];
        this.bufferUsed = 0;
    }

    @Override
    public void dispatch(BitmapStorage out, int runBegin, int runEnd) {
        final int runLength = runEnd - runBegin;
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
                generalLiterals(deficit, out, runBegin, runLength);
            }
        }
    }

    private long threshold2buf(final int t, final long[] buf, final int bufUsed) {
        long result = 0L;
        final int[] counters = this.bufCounters;
        System.arraycopy(zeroes64, 0, counters, 0, 64);
        for (int k = 0; k < bufUsed; ++k) {
            long bitset = buf[k];
            while (bitset != 0) {
                long t2 = bitset & -bitset;
                counters[Long.bitCount(t2 - 1)]++;
                bitset ^= t2;
            }
        }
        for (int pos = 0; pos < 64; ++pos) {
            if (counters[pos] >= t)
                result |= (1L << pos);
        }
        return result;
    }

    private static long threshold3(final int t, final long[] buffers, final int bufUsed) {
        if (buffers.length == 0)
            return 0;
        final long[] v = new long[t];
        v[0] = buffers[0];
        for (int k = 1; k < bufUsed; ++k) {
            final long c = buffers[k];
            final int m = Math.min(t - 1, k);
            for (int j = m; j >= 1; --j) {
                v[j] |= (c & v[j - 1]);
            }
            v[0] |= c;
        }
        return v[t - 1];
    }

    private long threshold4(final int T, final long[] buf, final int bufUsed) {
        if (T >= 128)
            return threshold2buf(T, buf, bufUsed);
        int B = 0;
        for (int k = 0; k < bufUsed; ++k)
            B += Long.bitCount(buf[k]);
        if (2 * B >= bufUsed * T)
            return threshold3(T, buf, bufUsed);//looped
        return threshold2buf(T, buf, bufUsed);//scancount
    }

    private void orLiterals(final BitmapStorage out, final int runBegin, final int runLength) {
        for (int i = 0; i < runLength; ++i) {
            long w = 0;
            for (EWAHPointer R : this.getLiterals()) {
                w |= R.iterator.getLiteralWordAt(i + runBegin - R.beginOfRun());
            }
            out.addWord(w);
        }
    }

    private void andLiterals(final BitmapStorage out, final int runBegin, final int runLength) {
        for (int i = 0; i < runLength; ++i) {
            long w = ~0;
            for (EWAHPointer R : this.getLiterals()) {
                w &= R.iterator.getLiteralWordAt(i + runBegin - R.beginOfRun());
            }
            out.addWord(w);
        }
    }

    private void generalLiterals(final int deficit,
                                       final BitmapStorage out, final int runBegin, final int runLength) {
        if (this.bufferUsed > this.buffers.length)
            this.buffers = Arrays.copyOf(this.buffers, 2 * this.bufferUsed);
        for (int i = 0; i < runLength; ++i) {
            int p = 0;
            for (EWAHPointer R : this.getLiterals()) {
                this.buffers[p++] = R.iterator.getLiteralWordAt(i + runBegin - R.beginOfRun());
            }
            out.addWord(threshold4(deficit, this.buffers, this.bufferUsed));
        }
    }

}
