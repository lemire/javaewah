package com.googlecode.javaewah32;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use.
 * 
 * @since 0.5.0
 * @author Daniel Lemire
 */
public final class RunningLengthWord32 implements Cloneable {

        /**
         * Instantiates a new running length word.
         * 
         * @param a
         *                an array of 32-bit words
         * @param p
         *                position in the array where the running length word is
         *                located.
         */
        RunningLengthWord32(final EWAHCompressedBitmap32 a, final int p) {
                this.parent = a;
                this.position = p;
        }

        /**
         * Gets the number of literal words.
         * 
         * @return the number of literal words
         */
        public int getNumberOfLiteralWords() {
                return (this.parent.buffer[this.position] >>> (1 + runninglengthbits));
        }

        /**
         * Gets the running bit.
         * 
         * @return the running bit
         */
        public boolean getRunningBit() {
                return (this.parent.buffer[this.position] & 1) != 0;
        }

        /**
         * Gets the running length.
         * 
         * @return the running length
         */
        public int getRunningLength() {
                return (this.parent.buffer[this.position] >>> 1)
                        & largestrunninglengthcount;
        }

        /**
         * Sets the number of literal words.
         * 
         * @param number
         *                the new number of literal words
         */
        public void setNumberOfLiteralWords(final int number) {
                this.parent.buffer[this.position] |= notrunninglengthplusrunningbit;
                this.parent.buffer[this.position] &= (number << (runninglengthbits + 1))
                        | runninglengthplusrunningbit;
        }

        /**
         * Sets the running bit.
         * 
         * @param b
         *                the new running bit
         */
        public void setRunningBit(final boolean b) {
                if (b)
                        this.parent.buffer[this.position] |= 1;
                else
                        this.parent.buffer[this.position] &= ~1;
        }

        /**
         * Sets the running length.
         * 
         * @param number
         *                the new running length
         */
        public void setRunningLength(final int number) {
                this.parent.buffer[this.position] |= shiftedlargestrunninglengthcount;
                this.parent.buffer[this.position] &= (number << 1)
                        | notshiftedlargestrunninglengthcount;
        }

        /**
         * Return the size in uncompressed words represented by this running
         * length word.
         * 
         * @return the int
         */
        public int size() {
                return getRunningLength() + getNumberOfLiteralWords();
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
        public RunningLengthWord32 clone() throws CloneNotSupportedException {
                RunningLengthWord32 answer;
                answer = (RunningLengthWord32) super.clone();
                answer.parent = this.parent;
                answer.position = this.position;
                return answer;
        }

        /** The array of words. */
        public EWAHCompressedBitmap32 parent;

        /** The position in array. */
        public int position;

        /**
         * number of bits dedicated to marking of the running length of clean
         * words
         */
        public static final int runninglengthbits = 16;

        private static final int literalbits = 32 - 1 - runninglengthbits;

        /** largest number of literal words in a run. */
        public static final int largestliteralcount = (1 << literalbits) - 1;

        /** largest number of clean words in a run */
        public static final int largestrunninglengthcount = (1 << runninglengthbits) - 1;

        private static final int runninglengthplusrunningbit = (1 << (runninglengthbits + 1)) - 1;

        private static final int shiftedlargestrunninglengthcount = largestrunninglengthcount << 1;

        private static final int notrunninglengthplusrunningbit = ~runninglengthplusrunningbit;

        private static final int notshiftedlargestrunninglengthcount = ~shiftedlargestrunninglengthcount;

}