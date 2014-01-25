package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;

/**
 * Wrapper around an IteratingBufferedRunningLengthWord used by the
 * RunningBitmapMerge class.
 * 
 * @author Daniel Lemire
 * @since 0.8.0
 */
public final class EWAHPointer implements Comparable<EWAHPointer> {
        private int endrun;
        private int pos;
        private boolean isliteral;
        private boolean value;
        private boolean dead = false;
        /**
         * Underlying iterator
         */
        public IteratingBufferedRunningLengthWord iterator;

        /**
         * Construct a pointer over an IteratingBufferedRunningLengthWord.
         * 
         * @param previousendrun
         *                word where the previous run ended
         * @param rw
         *                the iterator
         * @param pos
         *                current position (in word)
         */
        public EWAHPointer(final int previousendrun,
                final IteratingBufferedRunningLengthWord rw, final int pos) {
                this.pos = pos;
                this.iterator = rw;
                if (this.iterator.getRunningLength() > 0) {
                        this.endrun = previousendrun
                                + (int) this.iterator.getRunningLength();
                        this.isliteral = false;
                        this.value = this.iterator.getRunningBit();
                } else if (this.iterator.getNumberOfLiteralWords() > 0) {
                        this.isliteral = true;
                        this.endrun = previousendrun
                                + this.iterator.getNumberOfLiteralWords();
                } else {
                        this.endrun = previousendrun;
                        this.dead = true;
                }
        }

        /**
         * @return the end of the current run
         */
        public int endOfRun() {
                return this.endrun;
        }

        /**
         * @return the beginning of the current run
         */
        public int beginOfRun() {
                if (this.isliteral)
                        return this.endrun
                                - this.iterator.getNumberOfLiteralWords();
                return (int) (this.endrun - this.iterator.getRunningLength());
        }

        /**
         * Process the next run
         */
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

        /**
         * @return true if there is no more data
         */
        public boolean hasNoData() {
                return this.dead;
        }

        /**
         * @param f
         *                call the function with the current information
         */
        public void callbackUpdate(final UpdateableBitmapFunction f) {
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
                return this.endrun - other.endrun;
        }
}
