package com.googlecode.javaewah;

import static com.googlecode.javaewah.EWAHCompressedBitmap.WORD_IN_BITS;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Implementation of an IntIterator over an IteratingRLW.
 */
public class IntIteratorOverIteratingRLW implements IntIterator {
    final IteratingRLW parent;
    private int position;
    private int runningLength;
    private long word;
    private int wordPosition;
    private int wordLength;
    private int literalPosition;
    private boolean hasNext;

    /**
     * @param p iterator we wish to iterate over
     */
    public IntIteratorOverIteratingRLW(final IteratingRLW p) {
        this.parent = p;
        this.position = 0;
        setupForCurrentRunningLengthWord();
        this.hasNext = moveToNext();
    }

    /**
     * @return whether we could find another set bit; don't move if there is
     * an unprocessed value
     */
    private boolean moveToNext() {
        while (!runningHasNext() && !literalHasNext()) {
            if (this.parent.next())
                setupForCurrentRunningLengthWord();
            else
                return false;
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public final int next() {
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

    private void setupForCurrentRunningLengthWord() {
        this.runningLength = WORD_IN_BITS * (int) this.parent.getRunningLength() + this.position;

        if (!this.parent.getRunningBit()) {
            this.position = this.runningLength;
        }
        this.wordPosition = 0;
        this.wordLength = this.parent.getNumberOfLiteralWords();
    }

    private boolean runningHasNext() {
        return this.position < this.runningLength;
    }

    private boolean literalHasNext() {
        while (this.word == 0 && this.wordPosition < this.wordLength) {
            this.word = this.parent.getLiteralWordAt(this.wordPosition++);
            this.literalPosition = this.position;
            this.position += WORD_IN_BITS;
        }
        return this.word != 0;
    }
}
