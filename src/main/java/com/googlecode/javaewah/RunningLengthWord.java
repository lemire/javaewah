package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use.
 *
 * @author Daniel Lemire
 * @since 0.1.0
 */
public final class RunningLengthWord implements Cloneable {

    /**
     * Instantiates a new running length word.
     *
     * @param a an array of 64-bit words
     * @param p position in the array where the running length word is
     *          located.
     */
    RunningLengthWord(final EWAHCompressedBitmap a, final int p) {
        this.parent = a;
        this.position = p;
    }

    /**
     * Gets the number of literal words.
     *
     * @return the number of literal words
     */
    public int getNumberOfLiteralWords() {
        return (int) (this.parent.buffer[this.position] >>> (1 + RUNNING_LENGTH_BITS));
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
    public long getRunningLength() {
        return (this.parent.buffer[this.position] >>> 1) & LARGEST_RUNNING_LENGTH_COUNT;
    }

    /**
     * Sets the number of literal words.
     *
     * @param number the new number of literal words
     */
    public void setNumberOfLiteralWords(final long number) {
        this.parent.buffer[this.position] |= NOT_RUNNING_LENGTH_PLUS_RUNNING_BIT;
        this.parent.buffer[this.position] &= (number << (RUNNING_LENGTH_BITS + 1))
                | RUNNING_LENGTH_PLUS_RUNNING_BIT;
    }

    /**
     * Sets the running bit.
     *
     * @param b the new running bit
     */
    public void setRunningBit(final boolean b) {
        if (b)
            this.parent.buffer[this.position] |= 1l;
        else
            this.parent.buffer[this.position] &= ~1l;
    }

    /**
     * Sets the running length.
     *
     * @param number the new running length
     */
    public void setRunningLength(final long number) {
        this.parent.buffer[this.position] |= SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;
        this.parent.buffer[this.position] &= (number << 1)
                | NOT_SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;
    }

    /**
     * Return the size in uncompressed words represented by this running
     * length word.
     *
     * @return the size
     */
    public long size() {
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
    public RunningLengthWord clone() throws CloneNotSupportedException {
        RunningLengthWord answer;
        answer = (RunningLengthWord) super.clone();
        answer.parent = this.parent;
        answer.position = this.position;
        return answer;
    }

    /**
     * The array of words.
     */
    public EWAHCompressedBitmap parent;

    /**
     * The position in array.
     */
    public int position;

    /**
     * number of bits dedicated to marking of the running length of clean
     * words
     */
    public static final int RUNNING_LENGTH_BITS = 32;

    private static final int LITERAL_BITS = 64 - 1 - RUNNING_LENGTH_BITS;

    /**
     * largest number of literal words in a run.
     */
    public static final int LARGEST_LITERAL_COUNT = (1 << LITERAL_BITS) - 1;

    /**
     * largest number of clean words in a run
     */
    public static final long LARGEST_RUNNING_LENGTH_COUNT = (1l << RUNNING_LENGTH_BITS) - 1;

    private static final long RUNNING_LENGTH_PLUS_RUNNING_BIT = (1l << (RUNNING_LENGTH_BITS + 1)) - 1;

    private static final long SHIFTED_LARGEST_RUNNING_LENGTH_COUNT = LARGEST_RUNNING_LENGTH_COUNT << 1;

    private static final long NOT_RUNNING_LENGTH_PLUS_RUNNING_BIT = ~RUNNING_LENGTH_PLUS_RUNNING_BIT;

    private static final long NOT_SHIFTED_LARGEST_RUNNING_LENGTH_COUNT = ~SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;

}