package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;

/**
 * 
 * @author lemire
 * 
 */

public final class EWAHPointer implements Comparable<EWAHPointer> {
        private int endrun;
        private int pos;
        private boolean isliteral;
        private boolean value;
        private boolean dead = false;
        public IteratingBufferedRunningLengthWord iterator;

        public EWAHPointer(int previousendrun,
                IteratingBufferedRunningLengthWord rw, int pos) {
                this.pos = pos;
                this.iterator = rw;
                if (this.iterator.getRunningLength() > 0) {
                        this.endrun = previousendrun
                                + (int) this.iterator.getRunningLength();
                        isliteral = false;
                        value = this.iterator.getRunningBit();
                } else if (this.iterator.getNumberOfLiteralWords() > 0) {
                        isliteral = true;
                        this.endrun = previousendrun
                                + this.iterator.getNumberOfLiteralWords();
                } else {
                        this.endrun = previousendrun;
                        this.dead = true;
                }
        }

        public int endOfRun() {
                return this.endrun;
        }

        public int beginOfRun() {
                if (this.isliteral)
                        return this.endrun
                                - this.iterator.getNumberOfLiteralWords();
                return (int) (this.endrun - this.iterator.getRunningLength());
        }

        public void parseNextRun() {
                if ((this.isliteral)
                        || (this.iterator.getNumberOfLiteralWords() == 0)) {
                        // no choice, must load next runs
                        this.iterator.discardFirstWords(this.iterator.size()); 
                        if (this.iterator.getRunningLength() > 0) {
                                this.endrun += (int) this.iterator
                                        .getRunningLength();
                                this.isliteral = false;
                                this.value = this.iterator.getRunningBit();
                        } else if (this.iterator.getNumberOfLiteralWords() > 0) {
                                this.isliteral = true;
                                this.endrun += this.iterator
                                        .getNumberOfLiteralWords();
                        } else {
                                this.dead = true;
                        }

                } else {
                        this.isliteral = true;
                        this.endrun += this.iterator.getNumberOfLiteralWords();
                }

        }

        public boolean hasNoData() {
                return this.dead;
        }

        public void callbackUpdate(UpdateableBitmapFunction f) {
                if (this.dead)
                        f.setZero(this.pos);
                else if (this.isliteral)
                        f.setLiteral(this.pos);
                else if (this.value)
                        f.setOne(this.pos);
                else
                        f.setZero(this.pos);
        }

        @Override
        public int compareTo(EWAHPointer other) {
                return endrun - other.endrun;
        }
}
