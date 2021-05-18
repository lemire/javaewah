package com.googlecode.javaewah;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IntIterator;
import java.util.BitSet;
// credit @svanmald
public class EWAHBitSetPair {

    private EWAHCompressedBitmap bitmap;
    private BitSet bitSet;

    public EWAHBitSetPair() {
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

    public void or(EWAHBitSetPair other) {
        bitmap = bitmap.or(other.bitmap);
        bitSet.or(other.bitSet);
    }

    public void and(EWAHBitSetPair other) {
        bitmap = bitmap.and(other.bitmap);
        bitSet.and(other.bitSet);
    }

    public void andNot(EWAHBitSetPair other) {
        bitmap = bitmap.andNot(other.bitmap);
        bitSet.andNot(other.bitSet);
    }

    public void xor(EWAHBitSetPair other) {
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
