package com.googlecode.javaewah;

import java.util.BitSet;
import java.util.Iterator;
// credit @svanmald
public class ForwardBitSetIterator implements Iterator<Integer> {

    private final BitSet bitSet;
    private int next;

    public ForwardBitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        this.next = bitSet.nextSetBit(0);
    }


    @Override
    public boolean hasNext() {
        return next != -1;
    }

    @Override
    public Integer next() {
        int current = next;
        next = bitSet.nextSetBit(next + 1);
        return current;
    }
}