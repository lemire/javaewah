package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import com.googlecode.javaewah.IntIterator;

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.WORD_IN_BITS;

/**
 * The ReverseIntIterator32 is the 32 bit implementation of the IntIterator
 * interface, which efficiently returns the stream of integers represented by a
 * ReverseEWAHIterator32 in reverse order.
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class ReverseIntIterator32 implements IntIterator {

    private final ReverseEWAHIterator32 ewahIter;
    private final int sizeInBits;
    private final Buffer32 buffer;
    private int position;
    private boolean runningBit;
    private int runningLength;
    private int word;
    private int wordPosition;
    private int wordLength;
    private int literalPosition;
    private boolean hasNext;

    ReverseIntIterator32(ReverseEWAHIterator32 ewahIter, int sizeInBits) {
        this.ewahIter = ewahIter;
        this.sizeInBits = sizeInBits;
        this.buffer = ewahIter.buffer();
        this.runningLength = sizeInBits - 1;
        this.hasNext = this.moveToPreviousRLW();
    }

    @Override
    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public int next() {
        final int answer;
        if (literalHasNext()) {
            final int t = this.word & -this.word;
            answer = this.literalPosition - Integer.bitCount(t - 1);
            this.word ^= t;
        } else {
            answer = this.position--;
        }
        this.hasNext = this.moveToPreviousRLW();
        return answer;
    }

    private boolean moveToPreviousRLW() {
        while (!literalHasNext() && !runningHasNext()) {
            if (!this.ewahIter.hasPrevious()) {
                return false;
            }
            setRLW(this.ewahIter.previous());
        }
        return true;
    }

    private void setRLW(RunningLengthWord32 rlw) {
        this.wordLength = rlw.getNumberOfLiteralWords();
        this.wordPosition = this.ewahIter.position();
        this.position = this.runningLength;
        this.runningLength -= WORD_IN_BITS * (rlw.getRunningLength() + this.wordLength);
        if (this.position == this.sizeInBits - 1) {
            final int usedBitsInLast = this.sizeInBits % WORD_IN_BITS;
            if(usedBitsInLast > 0) {
                this.runningLength += WORD_IN_BITS - usedBitsInLast;
                if (this.wordLength > 0) {
                    this.word = Integer.reverse(this.buffer.getWord(this.wordPosition + this.wordLength--));
                    this.word = (this.word >>> (WORD_IN_BITS - usedBitsInLast));
                    this.literalPosition = this.position;
                    this.position -= usedBitsInLast;
                }
            }
        }
        this.runningBit = rlw.getRunningBit();
    }

    private boolean runningHasNext() {
        return this.runningBit && this.runningLength < this.position;
    }

    private boolean literalHasNext() {
        while (this.word == 0 && this.wordLength > 0) {
            this.word = Integer.reverse(this.buffer.getWord(this.wordPosition + this.wordLength--));
            this.literalPosition = this.position;
            this.position -= WORD_IN_BITS;
        }
        return this.word != 0;
    }

}
