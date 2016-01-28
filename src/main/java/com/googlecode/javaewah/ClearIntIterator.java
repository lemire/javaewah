package com.googlecode.javaewah;

import static com.googlecode.javaewah.EWAHCompressedBitmap.WORD_IN_BITS;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * This class is equivalent to IntIteratorImpl, except that it allows
 * use to iterate over "clear" bits (bits set to 0).
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class ClearIntIterator implements IntIterator {

    private final EWAHIterator ewahIter;
    private final int sizeInBits;
    private final Buffer buffer;
    private int position;
    private int runningLength;
    private long word;
    private int wordPosition;
    private int wordLength;
    private int literalPosition;
    private boolean hasNext;

    ClearIntIterator(EWAHIterator ewahIter, int sizeInBits) {
        this.ewahIter = ewahIter;
        this.sizeInBits = sizeInBits;
        this.buffer = ewahIter.buffer();
        this.hasNext = this.moveToNext();
    }

    public boolean moveToNext() {
        while (!runningHasNext() && !literalHasNext()) {
            if (!this.ewahIter.hasNext()) {
                return false;
            }
            setRunningLengthWord(this.ewahIter.next());
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public int next() {
        final int answer;
        if (runningHasNext()) {
            answer = this.position++;
        } else {
            final long t = this.word & -this.word;
            answer = this.literalPosition + Long.bitCount(t - 1);
            this.word ^= t;
        }
        this.hasNext = this.moveToNext();
        return answer;
    }

    private void setRunningLengthWord(RunningLengthWord rlw) {
        this.runningLength = Math.min(this.sizeInBits,
                                      WORD_IN_BITS * (int) rlw.getRunningLength() + this.position);
        if (rlw.getRunningBit()) {
            this.position = this.runningLength;
        }

        this.wordPosition = this.ewahIter.literalWords();
        this.wordLength = this.wordPosition + rlw.getNumberOfLiteralWords();
    }

    private boolean runningHasNext() {
        return this.position < this.runningLength;
    }

    private boolean literalHasNext() {
        while (this.word == 0 && this.wordPosition < this.wordLength) {
            this.word = ~this.buffer.getWord(this.wordPosition++);
            if (this.wordPosition == this.wordLength && !this.ewahIter.hasNext()) {
                final int usedBitsInLast = this.sizeInBits % WORD_IN_BITS;
                if (usedBitsInLast > 0) {
                    this.word &= ((~0l) >>> (WORD_IN_BITS - usedBitsInLast));
                }
            }
            this.literalPosition = this.position;
            this.position += WORD_IN_BITS;
        }
        return this.word != 0;
    }
}
