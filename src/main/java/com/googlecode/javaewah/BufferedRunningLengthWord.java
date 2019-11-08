package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use. Similar to RunningLengthWord, but can be modified
 * without access to the array, and has faster access.
 *
 * @author Daniel Lemire
 * @since 0.1.0
 */
public final class BufferedRunningLengthWord implements Cloneable {

    /**
     * Instantiates a new buffered running length word.
     *
     * @param a the word
     */
    public BufferedRunningLengthWord(final long a) {
        this.numberOfLiteralWords = (int) (a >>> (1 + RunningLengthWord.RUNNING_LENGTH_BITS));
        this.runningBit = (a & 1) != 0;
        this.runningLength = (int) ((a >>> 1) & RunningLengthWord.LARGEST_RUNNING_LENGTH_COUNT);
    }

    /**
     * Instantiates a new buffered running length word.
     *
     * @param rlw the rlw
     */
    public BufferedRunningLengthWord(final RunningLengthWord rlw) {
        this(rlw.buffer.getWord(rlw.position));
    }

    /**
     * Discard first words.
     *
     * @param x the x
     */
    public void discardFirstWords(long x) {
        if (this.runningLength >= x) {
            this.runningLength -= x;
            return;
        }
        x -= this.runningLength;
        this.runningLength = 0;
        this.literalWordOffset += (int) x;
        this.numberOfLiteralWords -= (int) x;
    }

    /**
     * Gets the number of literal words.
     *
     * @return the number of literal words
     */
    public int getNumberOfLiteralWords() {
        return this.numberOfLiteralWords;
    }

    /**
     * Gets the running bit.
     *
     * @return the running bit
     */
    public boolean getRunningBit() {
        return this.runningBit;
    }

    /**
     * Gets the running length.
     *
     * @return the running length
     */
    public long getRunningLength() {
        return this.runningLength;
    }

    /**
     * Reset the values using the provided word.
     *
     * @param a the word
     */
    public void reset(final long a) {
        this.numberOfLiteralWords = (int) (a >>> (1 + RunningLengthWord.RUNNING_LENGTH_BITS));
        this.runningBit = (a & 1) != 0;
        this.runningLength = (int) ((a >>> 1) & RunningLengthWord.LARGEST_RUNNING_LENGTH_COUNT);
        this.literalWordOffset = 0;
    }

    /**
     * Reset the values of this running length word so that it has the same
     * values as the other running length word.
     *
     * @param rlw the other running length word
     */
    public void reset(final RunningLengthWord rlw) {
        reset(rlw.buffer.getWord(rlw.position));
    }

    /**
     * Sets the number of literal words.
     *
     * @param number the new number of literal words
     */
    public void setNumberOfLiteralWords(final int number) {
        this.numberOfLiteralWords = number;
    }

    /**
     * Sets the running bit.
     *
     * @param b the new running bit
     */
    public void setRunningBit(final boolean b) {
        this.runningBit = b;
    }

    /**
     * Sets the running length.
     *
     * @param number the new running length
     */
    public void setRunningLength(final long number) {
        this.runningLength = number;
    }

    /**
     * Size in uncompressed words.
     *
     * @return the long
     */
    public long size() {
        return this.runningLength + this.numberOfLiteralWords;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "running bit = " + getRunningBit() +
               " running length = " + getRunningLength() +
               " number of lit. words " + getNumberOfLiteralWords();
    }

    @Override
    public BufferedRunningLengthWord clone() throws CloneNotSupportedException {
        BufferedRunningLengthWord answer = (BufferedRunningLengthWord) super.clone();
        answer.literalWordOffset = this.literalWordOffset;
        answer.numberOfLiteralWords = this.numberOfLiteralWords;
        answer.runningBit = this.runningBit;
        answer.runningLength = this.runningLength;
        return answer;
    }

    /**
     * how many literal words have we read so far?
     */
    protected int literalWordOffset = 0;

    /**
     * The Number of literal words.
     */
    protected int numberOfLiteralWords;

    /**
     * The Running bit.
     */
    protected boolean runningBit;

    /**
     * The Running length.
     */
    protected long runningLength;
}
