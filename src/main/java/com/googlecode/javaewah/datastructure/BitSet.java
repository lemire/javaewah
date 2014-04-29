package com.googlecode.javaewah.datastructure;

import com.googlecode.javaewah.IntIterator;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;

/**
 * <p>This is an optimized version of Java's BitSet. In many cases, it can be used
 * as a drop-in replacement.</p>
 * 
 * <p>It differs from the basic Java BitSet class in the following ways:</p>
 * <ul>
 * <li>You can iterate over set bits using a simpler syntax <code>for(int bs: myBitset)</code>.</li>
 * <li>You can compute the cardinality of an intersection and union without writing it out
 * or modifying your BitSets (see methods such as andcardinality).</li>
 * <li>You can recover wasted memory with trim().</li>
 * </ul>
 *
 * @author Daniel Lemire
 * @since 0.8.0
 */
public class BitSet implements Cloneable, Iterable<Integer>, Externalizable {
    /**
     * Construct a bitset with the specified number of bits (initially all
     * false). The number of bits is rounded up to the nearest multiple of
     * 64.
     *
     * @param sizeInBits the size in bits
     */
    public BitSet(final int sizeInBits) {
        this.data = new long[(sizeInBits + 63) / 64];
    }

    /**
     * Compute bitwise AND.
     *
     * @param bs other bitset
     */
    public void and(BitSet bs) {
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            this.data[k] &= bs.data[k];
        }
    }

    /**
     * Compute cardinality of bitwise AND.
     * 
     * The current bitmap is modified. Consider calling trim()
     * to recover wasted memory afterward.
     *
     * @param bs other bitset
     * @return cardinality
     */
    public int andcardinality(BitSet bs) {
        int sum = 0;
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            sum += Long.bitCount(this.data[k] & bs.data[k]);
        }
        return sum;
    }

    /**
     * Compute bitwise AND NOT.
     * 
     * The current bitmap is modified. Consider calling trim()
     * to recover wasted memory afterward.
     *
     * @param bs other bitset
     */
    public void andNot(BitSet bs) {
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            this.data[k] &= ~bs.data[k];
        }
    }

    /**
     * Compute cardinality of bitwise AND NOT.
     *
     * @param bs other bitset
     * @return cardinality
     */
    public int andNotcardinality(BitSet bs) {
        int sum = 0;
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
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
     * Reset all bits to false. This might be wasteful: a better
     * approach is to create a new empty bitmap.
     */
    public void clear() {
        Arrays.fill(this.data, 0);
    }

    @Override
    public BitSet clone() throws CloneNotSupportedException {
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
        BitSet bs = (BitSet) o;
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            if (this.data[k] != bs.data[k]) return false;
        }
        BitSet longer = bs.size() < this.size() ? this : bs;
        for (int k = Math.min(this.data.length, bs.data.length); k < Math
                .max(this.data.length, bs.data.length); ++k) {
            if (longer.data[k] != 0) return false;
        }
        return true;
    }

    /**
     * Check whether a bitset contains a set bit.
     *
     * @return true if no set bit is found
     */
    public boolean empty() {
        for (long l : this.data)
            if (l != 0) return false;
        return true;
    }

    /**
     * @param i index
     * @return value of the bit
     */
    public boolean get(final int i) {
        return (this.data[i / 64] & (1l << (i % 64))) != 0;
    }

    @Override
    public int hashCode() {
        int b = 31;
        int hash1 = 0;
        int hash2 = 0;
        for (long aData : this.data) {
            if (aData != 0) {
                hash1 = hash1 * b + (int) aData;
                hash2 = hash2 * b + (int) (aData >>> 32);
            }
        }
        return hash1 ^ hash2;
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

            private int i = BitSet.this.nextSetBit(0);

            private int j;

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
                return this.j;
            }

            @Override
            public void remove() {
                BitSet.this.unset(this.j);
            }

            private int i = BitSet.this.nextSetBit(0);

            private int j;
        };
    }

    /**
     * Checks whether two bitsets intersect.
     *
     * @param bs other bitset
     * @return true if they have a non-empty intersection (result of AND)
     */
    public boolean intersects(BitSet bs) {
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            if ((this.data[k] & bs.data[k]) != 0) return true;
        }
        return false;
    }

    /**
     * Usage: for(int i=bs.nextSetBit(0); i&gt;=0; i=bs.nextSetBit(i+1)) {
     * operate on index i here }
     *
     * @param i current set bit
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
     * @param i current unset bit
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
     * Compute bitwise OR.
     * 
     * The current bitmap is modified. Consider calling trim()
     * to recover wasted memory afterward.
     *
     * @param bs other bitset
     */
    public void or(BitSet bs) {
        if (this.size() < bs.size())
            this.resize(bs.size());
        for (int k = 0; k < this.data.length; ++k) {
            this.data[k] |= bs.data[k];
        }
    }

    /**
     * Compute cardinality of bitwise OR.
     * 
     * BitSets are not modified.
     *
     * @param bs other bitset
     * @return cardinality
     */
    public int orcardinality(BitSet bs) {
        int sum = 0;
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            sum += Long.bitCount(this.data[k] | bs.data[k]);
        }
        BitSet longer = bs.size() < this.size() ? this : bs;
        for (int k = Math.min(this.data.length, bs.data.length); k < Math
                .max(this.data.length, bs.data.length); ++k) {
            sum += Long.bitCount(longer.data[k]);
        }
        return sum;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        int length = in.readInt();
        this.data = new long[length];
        for (int k = 0; k < length; ++k)
            this.data[k] = in.readLong();
    }

    /**
     * Resize the bitset
     *
     * @param sizeInBits new number of bits
     */
    public void resize(int sizeInBits) {
        this.data = Arrays.copyOf(this.data, (sizeInBits + 63) / 64);
    }

    /**
     * Set to true
     *
     * @param i index of the bit
     */
    public void set(final int i) {
        this.data[i / 64] |= (1l << (i % 64));
    }

    /**
     * @param i index
     * @param b value of the bit
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
     * Recovers wasted memory
     */
    public void trim() {
        for (int k = this.data.length - 1; k >= 0; --k)
            if (this.data[k] != 0) {
                if (k + 1 < this.data.length)
                    this.data = Arrays.copyOf(this.data, k + 1);
                return;
            }
        this.data = new long[0];
    }

    /**
     * Set to false
     *
     * @param i index of the bit
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

            private int i = BitSet.this.nextUnsetBit(0);

            private int j;
        };
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.data.length);
        for (long w : this.data)
            out.writeLong(w);
    }

    /**
     * Compute bitwise XOR.
     * 
     * The current bitmap is modified. Consider calling trim()
     * to recover wasted memory afterward.
     *
     * @param bs other bitset
     */
    public void xor(BitSet bs) {
        if (this.size() < bs.size())
            this.resize(bs.size());
        for (int k = 0; k < this.data.length; ++k) {
            this.data[k] ^= bs.data[k];
        }
    }

    /**
     * Compute cardinality of bitwise XOR.
     * 
     * BitSets are not modified.
     *
     * @param bs other bitset
     * @return cardinality
     */
    public int xorcardinality(BitSet bs) {
        int sum = 0;
        for (int k = 0; k < Math.min(this.data.length, bs.data.length); ++k) {
            sum += Long.bitCount(this.data[k] ^ bs.data[k]);
        }
        BitSet longer = bs.size() < this.size() ? this : bs;

        int start = Math.min(this.data.length, bs.data.length);
        int end = Math.max(this.data.length, bs.data.length);
        for (int k = start; k < end; ++k) {
            sum += Long.bitCount(longer.data[k]);
        }

        return sum;
    }

    private long[] data;

    static final long serialVersionUID = 7997698588986878753L;

}
