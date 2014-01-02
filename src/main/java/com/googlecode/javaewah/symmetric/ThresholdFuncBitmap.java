package com.googlecode.javaewah.symmetric;

import java.util.Arrays;
import com.googlecode.javaewah.BitmapStorage;

public class ThresholdFuncBitmap extends UpdateableBitmapFunction {
        int min;
        long[] buffers;
        int bufferUsed;

        public ThresholdFuncBitmap(int min) {
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
                        bufferUsed = this.getNumberOfLiterals();
                        if(bufferUsed > buffers.length)
                                buffers = Arrays.copyOf(
                                        buffers,
                                        2 * bufferUsed);
                        for (int i = 0; i < runlength; ++i) {
                                int p = 0;
                                for (EWAHPointer R : this.getLiterals()) {
                                        buffers[p++] = R.iterator
                                                .getLiteralWordAt(i + runbegin
                                                        - R.beginOfRun());
                                }
                                long result = threshold4(deficit,buffers,bufferUsed);
                                // naive position-by-position
                                /*for (int pos = 0; pos < 64; ++pos) {
                                        // take current high-order bits
                                        result <<= 1;
                                        int count = 0;
                                        for (int j = 0; j < bufferUsed; ++j) {
                                                if ((buffers[j] & (1L << 63)) != 0)
                                                        ++count;
                                                buffers[j] <<= 1;
                                        }
                                        if (count >= deficit)
                                                result |= 1;
                                }*/
                                out.add(result);
                        }
                }
        }

        

        private static int[] bufcounters = new int[64];
        private static final int[] zeroes64 = new int[64];

        public static long threshold2buf(int T, long[] buffers, int bufUsed) {
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

        public static long threshold3(int T, long[] buffers, int bufUsed) {
                if (buffers.length == 0)
                        return 0;
                long[] v = new long[T];
                v[0] = buffers[0];
                for (int k = 1; k < bufUsed; ++k) {
                        long c = buffers[k];
                        for (int j = T - 1; j >= 1; --j) {
                                long ttemp = c;
                                ttemp &= v[j - 1];
                                v[j] |= ttemp;
                        }
                        v[0] |= c;
                }
                return v[T - 1];
        }

        public static long threshold4(int T, long[] buffers, int bufUsed) {
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
