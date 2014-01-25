package com.googlecode.javaewah.datastructure;

import java.util.Arrays;

/**
 * This is an optimized version of Java's BitSet.
 * 
 * @author Daniel Lemire
 * @since 0.8.0
 **/
public class StaticBitSet {
        long[] data;

        /**
         * Construct a bitset with the specified number of bits (initially all
         * false). The number of bits is rounded up to the nearest multiple of
         * 64.
         * 
         * @param sizeinbits
         *                the size in bits
         */
        public StaticBitSet(final int sizeinbits) {
                this.data = new long[(sizeinbits + 63) / 64];
        }

        /**
         * Query the size
         * 
         * @return the size in bits.
         */
        public int size() {
                return this.data.length * 64;
        }

        /**
         * Resize the bitset
         * 
         * @param sizeinbits
         *                new number of bits
         */
        public void resize(int sizeinbits) {
                this.data = Arrays.copyOf(this.data, (sizeinbits + 63) / 64);
        }

        /**
         * Reset all bits to false
         */
        public void clear() {
                Arrays.fill(this.data, 0);
        }

        /**
         * Compute the number of bits set to 1
         * 
         * @return the number of bits
         */
        public int cardinality() {
                int sum = 0;
                for (long l : this.data)
                        sum += Long.bitCount(l);
                return sum;
        }

        /**
         * @param i
         *                index
         * @return value of the bit
         */
        public boolean get(final int i) {
                return (this.data[i / 64] & (1l << (i % 64))) != 0;
        }

        /**
         * Set to true
         * 
         * @param i
         *                index of the bit
         */
        public void set(final int i) {
                this.data[i / 64] |= (1l << (i % 64));
        }

        /**
         * Set to false
         * 
         * @param i
         *                index of the bit
         */
        public void unset(final int i) {
                this.data[i / 64] &= ~(1l << (i % 64));
        }

        /**
         * @param i
         *                index
         * @param b
         *                value of the bit
         */
        public void set(final int i, final boolean b) {
                if (b)
                        set(i);
                else
                        unset(i);
        }

        /**
         * Usage: for(int i=bs.nextSetBit(0); i&gt;=0; i=bs.nextSetBit(i+1)) {
         * operate on index i here }
         * 
         * @param i
         *                current set bit
         * @return next set bit or -1
         */
        public int nextSetBit(final int i) {
                int x = i / 64;
                if (x >= this.data.length)
                        return -1;
                long w = this.data[x];
                w >>>= (i % 64);
                if (w != 0) {
                        return i + Long.numberOfTrailingZeros(w);
                }
                ++x;
                for (; x < this.data.length; ++x) {
                        if (this.data[x] != 0) {
                                return x
                                        * 64
                                        + Long.numberOfTrailingZeros(this.data[x]);
                        }
                }
                return -1;
        }

        /**
         * Usage: for(int i=bs.nextUnsetBit(0); i&gt;=0; i=bs.nextUnsetBit(i+1))
         * { operate on index i here }
         * 
         * @param i
         *                current unset bit
         * @return next unset bit or -1
         */
        public int nextUnsetBit(final int i) {
                int x = i / 64;
                if (x >= this.data.length)
                        return -1;
                long w = ~this.data[x];
                w >>>= (i % 64);
                if (w != 0) {
                        return i + Long.numberOfTrailingZeros(w);
                }
                ++x;
                for (; x < this.data.length; ++x) {
                        if (this.data[x] != ~0) {
                                return x
                                        * 64
                                        + Long.numberOfTrailingZeros(~this.data[x]);
                        }
                }
                return -1;
        }

}
