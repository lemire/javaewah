package com.googlecode.javaewah.datastructure;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import com.googlecode.javaewah.IntIterator;

/**
 * This is an optimized version of Java's BitSet. In many cases, it can be used
 * as a drop-in replacement. 
 * 
 * It differs from the basic Java BitSet class in the following ways:
 * <ul>
 * <li>It only aggregate BitSets having the same number of bits. This is 
 * the desired behavior in many cases where BitSets are supposed to span
 * the same number of bits and differences are indicative of a programming
 * issue. You can always resize your BitSets.</li>
 * <li>You can iterate over set bits using a simpler syntax <code>for(int bs: mybitset)</code>.</li>
 * <li>You can compute the cardinality of an intersection and union without writing it out
 * or modifying your BitSets (see methods such as andcardinality).</li>
 * </ul>
 * 
 * @author Daniel Lemire
 * @since 0.8.0
 **/
public class BitSet implements Cloneable, Iterable<Integer>, Externalizable {
        /**
         * Construct a bitset with the specified number of bits (initially all
         * false). The number of bits is rounded up to the nearest multiple of
         * 64.
         * 
         * @param sizeinbits
         *                the size in bits
         */
        public BitSet(final int sizeinbits) {
                this.data = new long[(sizeinbits + 63) / 64];
        }

        /**
         * Compute bitwise AND, assumes that both bitsets have the same length.
         * 
         * @param bs
         *                other bitset
         */
        public void and(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                for (int k = 0; k < this.data.length; ++k) {
                        this.data[k] &= bs.data[k];
                }
        }

        /**
         * Compute cardinality of bitwise AND, assumes that both bitsets have
         * the same length.
         * 
         * @param bs
         *                other bitset
         * @return cardinality
         */
        public int andcardinality(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                int sum = 0;
                for (int k = 0; k < this.data.length; ++k) {
                        sum += Long.bitCount(this.data[k] & bs.data[k]);
                }
                return sum;
        }

        /**
         * Compute bitwise AND NOT, assumes that both bitsets have the same
         * length.
         * 
         * @param bs
         *                other bitset
         */
        public void andNot(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                for (int k = 0; k < this.data.length; ++k) {
                        this.data[k] &= ~bs.data[k];
                }
        }

        /**
         * Compute cardinality of bitwise AND NOT, assumes that both bitsets
         * have the same length.
         * 
         * @param bs
         *                other bitset
         * @return cardinality
         */
        public int andNotcardinality(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                int sum = 0;
                for (int k = 0; k < this.data.length; ++k) {
                        sum += Long.bitCount(this.data[k] & (~bs.data[k]));
                }
                return sum;
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
         * Reset all bits to false
         */
        public void clear() {
                Arrays.fill(this.data, 0);
        }

        @Override
        public BitSet clone()  {
                BitSet b;
                try {
                        b = (BitSet) super.clone();
                        b.data = Arrays.copyOf(this.data, this.data.length);
                        return b;
                } catch (CloneNotSupportedException e) {
                        return null;
                }
        }

        @Override
        public boolean equals(Object o) {
                if (!(o instanceof BitSet))
                        return false;
                return Arrays.equals(this.data, ((BitSet) o).data);
        }

        /**
         * @param i
         *                index
         * @return value of the bit
         */
        public boolean get(final int i) {
                return (this.data[i / 64] & (1l << (i % 64))) != 0;
        }

        @Override
        public int hashCode() {
                return Arrays.hashCode(this.data);
        }

        /**
         * Iterate over the set bits
         * 
         * @return an iterator
         */
        public IntIterator intIterator() {
                return new IntIterator() {
                        @Override
                        public boolean hasNext() {
                                return this.i >= 0;
                        }

                        @Override
                        public int next() {
                                this.j = this.i;
                                this.i = BitSet.this.nextSetBit(this.i + 1);
                                return this.j;
                        }

                        int i = BitSet.this.nextSetBit(0);

                        int j;

                };
        }

        @Override
        public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                        @Override
                        public boolean hasNext() {
                                return this.i >= 0;
                        }

                        @Override
                        public Integer next() {
                                this.j = this.i;
                                this.i = BitSet.this.nextSetBit(this.i + 1);
                                return new Integer(this.j);
                        }

                        @Override
                        public void remove() {
                                BitSet.this.unset(this.j);
                        }

                        int i = BitSet.this.nextSetBit(0);

                        int j;

                };
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

        /**
         * Compute bitwise OR, assumes that both bitsets have the same length.
         * 
         * @param bs
         *                other bitset
         */
        public void or(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                for (int k = 0; k < this.data.length; ++k) {
                        this.data[k] |= bs.data[k];
                }
        }

        /**
         * Compute cardinality of bitwise OR, assumes that both bitsets have the
         * same length.
         * 
         * @param bs
         *                other bitset
         * @return cardinality
         */
        public int orcardinality(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                int sum = 0;
                for (int k = 0; k < this.data.length; ++k) {
                        sum += Long.bitCount(this.data[k] | bs.data[k]);
                }
                return sum;
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
                int length = in.readInt();
                this.data = new long[length];
                for(int k = 0; k < length; ++k)
                        this.data[k] = in.readLong();
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
         * Set to true
         * 
         * @param i
         *                index of the bit
         */
        public void set(final int i) {
                this.data[i / 64] |= (1l << (i % 64));
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
         * Query the size
         * 
         * @return the size in bits.
         */
        public int size() {
                return this.data.length * 64;
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
         * Iterate over the unset bits
         * 
         * @return an iterator
         */
        public IntIterator unsetIntIterator() {
                return new IntIterator() {
                        @Override
                        public boolean hasNext() {
                                return this.i >= 0;
                        }

                        @Override
                        public int next() {
                                this.j = this.i;
                                this.i = BitSet.this.nextUnsetBit(this.i + 1);
                                return this.j;
                        }

                        int i = BitSet.this.nextUnsetBit(0);

                        int j;

                };
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
                out.writeInt(this.data.length);
                for(long w: this.data)
                        out.writeLong(w);
        }

        /**
         * Compute bitwise XOR, assumes that both bitsets have the same length.
         * 
         * @param bs
         *                other bitset
         */
        public void xor(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                for (int k = 0; k < this.data.length; ++k) {
                        this.data[k] ^= bs.data[k];
                }
        }
        
        /**
         * Compute cardinality of bitwise XOR, assumes that both bitsets have
         * the same length.
         * 
         * @param bs
         *                other bitset
         * @return cardinality
         */
        public int xorcardinality(BitSet bs) {
                if (this.data.length != bs.data.length)
                        throw new IllegalArgumentException(
                                "incompatible bitsets");
                int sum = 0;
                for (int k = 0; k < this.data.length; ++k) {
                        sum += Long.bitCount(this.data[k] ^ bs.data[k]);
                }
                return sum;
        }

        long[] data;

        static final long serialVersionUID = 7997698588986878753L;

}
