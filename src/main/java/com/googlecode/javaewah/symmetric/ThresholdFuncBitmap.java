package com.googlecode.javaewah.symmetric;

import java.util.ArrayList;
import java.util.Arrays;
import com.googlecode.javaewah.BitmapStorage;

/**
 * A threshold Boolean function returns true if the number of true values exceed a
 * threshold. It is a symmetric Boolean function.
 * 
 * This object is not thread safe: you should use one function per thread.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @author Daniel Lemire
 * @since 0.8.0
 *
 */
public class ThresholdFuncBitmap extends UpdateableBitmapFunction {
        int min;
        long[] buffers;
        int bufferUsed;
        ArrayList<EWAHPointer> litbuffer = new ArrayList<EWAHPointer>();

        /**
         * Construction a threshold function with a given threshold 
         * @param min threshold
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
                        int deficit = min - hammingWeight;
                        litbuffer.clear();
                        this.fillWithLiterals(litbuffer);
                        bufferUsed = litbuffer.size();
                        // trivial case where there is just one lit. word (we copy it)
                        if(bufferUsed == 1) {
                                EWAHPointer R = this.litbuffer.get(0);
                                for (int i = 0; i < runlength; ++i) {
                                        out.add(R.iterator.getLiteralWordAt(i + runbegin
                                                - R.beginOfRun()));
                                }       
                                return;
                        }
                        // next if deficit is 1 we need to compute OR, this can be done fast
                        if(deficit == 1) {
                                long[] w = new long[runlength];
                                for (EWAHPointer R : this.litbuffer) {
                                        for (int i = 0; i < runlength; ++i) {
                                                w[i] |= R.iterator
                                                        .getLiteralWordAt(i + runbegin
                                                                - R.beginOfRun());
                                        }
                                }
                                for (int i = 0; i < runlength; ++i) {
                                        out.add(w[i]);
                                }
                                return;
                        } 
                        // next if deficit is bufferUsed, we use AND
                        if(bufferUsed == deficit) {
                                long[] w = new long[runlength];
                                for (int i = 0; i < runlength; ++i) {
                                        w[i] = litbuffer.get(0).iterator
                                                .getLiteralWordAt(i + runbegin
                                                        - litbuffer.get(0).beginOfRun());
                                }
                                for(int k = 1; k < litbuffer.size(); ++k) {
                                        for (int i = 0; i < runlength; ++i) {
                                                w[i] &= litbuffer.get(k).iterator
                                                        .getLiteralWordAt(i + runbegin
                                                                - litbuffer.get(k).beginOfRun());
                                        }
                                }
                                for (int i = 0; i < runlength; ++i) {
                                        out.add(w[i]);
                                }
                                
                                return;
                        }
                        // general case
                        if(bufferUsed > buffers.length)
                                buffers = Arrays.copyOf(
                                        buffers,
                                        2 * bufferUsed);
                        for (int i = 0; i < runlength; ++i) {
                                int p = 0;
                                for (EWAHPointer R : litbuffer) {
                                        buffers[p++] = R.iterator
                                                .getLiteralWordAt(i + runbegin
                                                        - R.beginOfRun());
                                }
                                long result = threshold4(deficit,buffers,bufferUsed);
                                out.add(result);
                        }
                }
        }

        

        private  int[] bufcounters = new int[64];
        private static final int[] zeroes64 = new int[64];

        private  long threshold2buf(int T, long[] buffers, int bufUsed) {
                long result = 0L;
                int[] counters = bufcounters;
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

        private  long threshold3(int T, long[] buffers, int bufUsed) {
                if (buffers.length == 0)
                        return 0;
                long[] v = new long[T];
                v[0] = buffers[0];
                for (int k = 1; k < bufUsed; ++k) {
                        long c = buffers[k];
                        final int m = Math.min(T-1, k);
                        for (int j = m; j >= 1; --j) {
                                v[j] |= (c & v[j - 1]);
                        }
                        v[0] |= c;
                }
                return v[T - 1];
        }

        private long threshold4(int T, long[] buffers, int bufUsed) {
                if (T >= 128)
                        return threshold2buf(T, buffers,bufUsed);
                int B = 0;
                for (int k = 0; k < bufUsed; ++k)
                        B += Long.bitCount(buffers[k]);
                if (2 * B >= bufUsed * T)
                        return threshold3(T, buffers,bufUsed);
                else
                        return threshold2buf(T, buffers,bufUsed);
        }
}
