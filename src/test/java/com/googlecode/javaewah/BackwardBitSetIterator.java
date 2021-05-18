package com.googlecode.javaewah;

import java.util.BitSet;
import java.util.Iterator;
// credit @svanmald
public class BackwardBitSetIterator implements Iterator<Integer> {

    private final BitSet bitSet;
    private int next;

    public BackwardBitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        this.next = bitSet.previousSetBit(bitSet.length());
    }

    @Override
    public boolean hasNext() {
        return next != -1;
    }

    @Override
    public Integer next() {
        int current = next;
        next = bitSet.previousSetBit(current - 1);
        return current;
    }
}