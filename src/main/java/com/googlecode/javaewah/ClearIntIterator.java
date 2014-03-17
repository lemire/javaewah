package com.googlecode.javaewah;

import static com.googlecode.javaewah.EWAHCompressedBitmap.wordinbits;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 
 * This class is equivalent to IntIteratorImpl, except that it allows
 * use to iterate over "clear" bits (bits set to 0).
 * 
 * 
 * 
 * @author gssiyankai
 *
 */
final class ClearIntIterator implements IntIterator {

        private final EWAHIterator ewahIter;
        private final int sizeinbits;
        private final long[] ewahBuffer;
        private int position;
        private int runningLength;
        private long word;
        private int wordPosition;
        private int wordLength;
        private int literalPosition;
        private boolean hasnext;

        ClearIntIterator(EWAHIterator ewahIter, int sizeinbits) {
                this.ewahIter = ewahIter;
                this.sizeinbits = sizeinbits;
                this.ewahBuffer = ewahIter.buffer();
                this.hasnext = this.moveToNext();
        }

        public final boolean moveToNext() {
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
                return this.hasnext;
        }

        @Override
        public final int next() {
                final int answer;
                if (runningHasNext()) {
                        answer = this.position++;
                } else {
                        final long T = this.word & -this.word;
                        answer = this.literalPosition + Long.bitCount(T-1);
                        this.word ^= T;
                }
                this.hasnext = this.moveToNext();
                return answer;
        }

        private final void setRunningLengthWord(RunningLengthWord rlw) {
                this.runningLength = wordinbits * (int) rlw.getRunningLength()
                        + this.position;
                if (rlw.getRunningBit()) {
                        this.position = this.runningLength;
                }

                this.wordPosition = this.ewahIter.literalWords();
                this.wordLength = this.wordPosition
                        + rlw.getNumberOfLiteralWords();
        }

        private final boolean runningHasNext() {
                return this.position < this.runningLength;
        }

        private final boolean literalHasNext() {
                while (this.word == 0 && this.wordPosition < this.wordLength) {
                        this.word = ~this.ewahBuffer[this.wordPosition++];
                        if(this.wordPosition == this.wordLength && !this.ewahIter.hasNext()) {
                            final int usedbitsinlast = this.sizeinbits % wordinbits;
                            if (usedbitsinlast > 0) {
                                this.word &= ((~0l) >>> (wordinbits - usedbitsinlast));
                            }
                        }
                        this.literalPosition = this.position;
                        this.position += wordinbits;
                }
                return this.word != 0;
        }
}
