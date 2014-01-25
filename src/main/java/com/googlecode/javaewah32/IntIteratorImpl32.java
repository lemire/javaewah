package com.googlecode.javaewah32;

/*
 * Copyright 2012, Google Inc.  
 * Licensed under the Apache License, Version 2.0.
 */

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.wordinbits;

import com.googlecode.javaewah.IntIterator;

/**
 * The IntIteratorImpl32 is the 32 bit implementation of the IntIterator
 * interface, which efficiently returns the stream of integers represented by an
 * EWAHIterator32.
 * 
 * @author Colby Ranger
 * @since 0.5.6
 */
final class IntIteratorImpl32 implements IntIterator {

        private final EWAHIterator32 ewahIter;
        private final int[] ewahBuffer;
        private int position;
        private int runningLength;
        private int word;
        private int wordPosition;
        private int wordLength;
        private int literalPosition;
        private boolean hasnext;

        IntIteratorImpl32(EWAHIterator32 ewahIter) {
                this.ewahIter = ewahIter;
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
        public final boolean hasNext() {
                return this.hasnext;
        }

        @Override
        public final int next() {
                final int answer;
                if (runningHasNext()) {
                        answer = this.position++;
                } else {
                        final int T = this.word & -this.word;
                        answer = this.literalPosition + Integer.bitCount(T - 1);
                        this.word ^= T;
                }
                this.hasnext = this.moveToNext();
                return answer;
        }

        private final void setRunningLengthWord(RunningLengthWord32 rlw) {
                this.runningLength = wordinbits * rlw.getRunningLength()
                        + this.position;
                if (!rlw.getRunningBit()) {
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
                        this.word = this.ewahBuffer[this.wordPosition++];
                        this.literalPosition = this.position;
                        this.position += wordinbits;
                }
                return this.word != 0;
        }
}
