package com.googlecode.javaewah32;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import java.util.BitSet;
import com.googlecode.javaewah.BackwardBitSetIterator;
import com.googlecode.javaewah.ForwardBitSetIterator;
// credit @svanmald
public class EWAH32BitSetPair {

    private EWAHCompressedBitmap bitmap;
    private BitSet bitSet;

    public EWAH32BitSetPair() {
        bitmap = new EWAHCompressedBitmap();
        bitSet = new BitSet();
    }

    public void validate() {
        assert bitmap.cardinality() == bitSet.cardinality();
        ForwardBitSetIterator forwardBitSetIterator = new ForwardBitSetIterator(bitSet);
        for (Integer current : bitmap) {
            Integer next = forwardBitSetIterator.next();
            assert bitmap.get(current);
            assert next.equals(current);
        }

        BackwardBitSetIterator backwardBitSetIterator = new BackwardBitSetIterator(bitSet);
        IntIterator reverseIterator = bitmap.reverseIntIterator();
        while (reverseIterator.hasNext()) {
            int nextBitMap = reverseIterator.next();
            Integer nextBitSet = backwardBitSetIterator.next();
            assert nextBitSet == nextBitMap;
        }
        assert !backwardBitSetIterator.hasNext();

        EWAHCompressedBitmap result = new EWAHCompressedBitmap().or(bitmap);
        assert result.equals(bitmap);
        assert bitmap.equals(result);
        assert bitmap.isEmpty() || bitmap.getFirstSetBit() == bitmap.iterator().next();
    }

    public void or(EWAH32BitSetPair other) {
        bitmap = bitmap.or(other.bitmap);
        bitSet.or(other.bitSet);
    }

    public void and(EWAH32BitSetPair other) {
        bitmap = bitmap.and(other.bitmap);
        bitSet.and(other.bitSet);
    }

    public void andNot(EWAH32BitSetPair other) {
        bitmap = bitmap.andNot(other.bitmap);
        bitSet.andNot(other.bitSet);
    }

    public void xor(EWAH32BitSetPair other) {
        bitmap = bitmap.xor(other.bitmap);
        bitSet.xor(other.bitSet);
    }

    public void set(int value) {
        bitSet.set(value);
        bitmap.set(value);
    }

    public void clear(int value) {
        bitSet.clear(value);
        bitmap.clear(value);
    }
}
