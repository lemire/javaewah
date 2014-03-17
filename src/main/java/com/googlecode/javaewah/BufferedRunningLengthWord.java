package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use. Similar to RunningLengthWord, but can be modified
 * without access to the array, and has faster access.
 * 
 * @author Daniel Lemire
 * @since 0.1.0
 * 
 */
public final class BufferedRunningLengthWord implements Cloneable {

        /**
         * Instantiates a new buffered running length word.
         * 
         * @param a
         *                the word
         */
        public BufferedRunningLengthWord(final long a) {
                this.NumberOfLiteralWords = (int) (a >>> (1 + RunningLengthWord.runninglengthbits));
                this.RunningBit = (a & 1) != 0;
                this.RunningLength = (int) ((a >>> 1) & RunningLengthWord.largestrunninglengthcount);
        }

        /**
         * Instantiates a new buffered running length word.
         * 
         * @param rlw
         *                the rlw
         */
        public BufferedRunningLengthWord(final RunningLengthWord rlw) {
                this(rlw.parent.buffer[rlw.position]);
        }

        /**
         * Discard first words.
         * 
         * @param x
         *                the x
         */
        public void discardFirstWords(long x) {
                if (this.RunningLength >= x) {
                        this.RunningLength -= x;
                        return;
                }
                x -= this.RunningLength;
                this.RunningLength = 0;
                this.literalwordoffset += x;
                this.NumberOfLiteralWords -= x;
        }

        /**
         * Gets the number of literal words.
         * 
         * @return the number of literal words
         */
        public int getNumberOfLiteralWords() {
                return this.NumberOfLiteralWords;
        }

        /**
         * Gets the running bit.
         * 
         * @return the running bit
         */
        public boolean getRunningBit() {
                return this.RunningBit;
        }

        /**
         * Gets the running length.
         * 
         * @return the running length
         */
        public long getRunningLength() {
                return this.RunningLength;
        }

        /**
         * Reset the values using the provided word.
         * 
         * @param a
         *                the word
         */
        public void reset(final long a) {
                this.NumberOfLiteralWords = (int) (a >>> (1 + RunningLengthWord.runninglengthbits));
                this.RunningBit = (a & 1) != 0;
                this.RunningLength = (int) ((a >>> 1) & RunningLengthWord.largestrunninglengthcount);
                this.literalwordoffset = 0;
        }

        /**
         * Reset the values of this running length word so that it has the same
         * values as the other running length word.
         * 
         * @param rlw
         *                the other running length word
         */
        public void reset(final RunningLengthWord rlw) {
                reset(rlw.parent.buffer[rlw.position]);
        }

        /**
         * Sets the number of literal words.
         * 
         * @param number
         *                the new number of literal words
         */
        public void setNumberOfLiteralWords(final int number) {
                this.NumberOfLiteralWords = number;
        }

        /**
         * Sets the running bit.
         * 
         * @param b
         *                the new running bit
         */
        public void setRunningBit(final boolean b) {
                this.RunningBit = b;
        }

        /**
         * Sets the running length.
         * 
         * @param number
         *                the new running length
         */
        public void setRunningLength(final long number) {
                this.RunningLength = number;
        }

        /**
         * Size in uncompressed words.
         * 
         * @return the long
         */
        public long size() {
                return this.RunningLength + this.NumberOfLiteralWords;
        }

        /*
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
                return "running bit = " + getRunningBit()
                        + " running length = " + getRunningLength()
                        + " number of lit. words " + getNumberOfLiteralWords();
        }

        @Override
        public BufferedRunningLengthWord clone()
                throws CloneNotSupportedException {
                BufferedRunningLengthWord answer = (BufferedRunningLengthWord) super
                        .clone();
                answer.literalwordoffset = this.literalwordoffset;
                answer.NumberOfLiteralWords = this.NumberOfLiteralWords;
                answer.RunningBit = this.RunningBit;
                answer.RunningLength = this.RunningLength;
                return answer;
        }

        /** how many literal words have we read so far? */
        public int literalwordoffset = 0;

        /** The Number of literal words. */
        public int NumberOfLiteralWords;

        /** The Running bit. */
        public boolean RunningBit;

        /** The Running length. */
        public long RunningLength;

}