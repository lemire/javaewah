package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use. Similar to RunningLengthWord, but can be modified
 * without access to the array, and has faster access.
 *
 * @author Daniel Lemire
 * @since 0.5.0
 */
public final class BufferedRunningLengthWord32 implements Cloneable {

    /**
     * Instantiates a new buffered running length word.
     *
     * @param a the word
     */
    public BufferedRunningLengthWord32(final int a) {
        this.NumberOfLiteralWords = (a >>> (1 + RunningLengthWord32.RUNNING_LENGTH_BITS));
        this.RunningBit = (a & 1) != 0;
        this.RunningLength = ((a >>> 1) & RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT);
    }

    /**
     * Instantiates a new buffered running length word.
     *
     * @param rlw the rlw
     */
    public BufferedRunningLengthWord32(final RunningLengthWord32 rlw) {
        this(rlw.buffer.getWord(rlw.position));
    }

    /**
     * Discard first words.
     *
     * @param x the number of words to be discarded
     */
    public void discardFirstWords(int x) {
        if (this.RunningLength >= x) {
            this.RunningLength -= x;
            return;
        }
        x -= this.RunningLength;
        this.RunningLength = 0;
        this.literalWordOffset += x;
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
    public int getRunningLength() {
        return this.RunningLength;
    }

    /**
     * Reset the values using the provided word.
     *
     * @param a the word
     */
    public void reset(final int a) {
        this.NumberOfLiteralWords = (a >>> (1 + RunningLengthWord32.RUNNING_LENGTH_BITS));
        this.RunningBit = (a & 1) != 0;
        this.RunningLength = ((a >>> 1) & RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT);
        this.literalWordOffset = 0;
    }

    /**
     * Reset the values of this running length word so that it has the same
     * values as the other running length word.
     *
     * @param rlw the other running length word
     */
    public void reset(final RunningLengthWord32 rlw) {
        reset(rlw.buffer.getWord(rlw.position));
    }

    /**
     * Sets the number of literal words.
     *
     * @param number the new number of literal words
     */
    public void setNumberOfLiteralWords(final int number) {
        this.NumberOfLiteralWords = number;
    }

    /**
     * Sets the running bit.
     *
     * @param b the new running bit
     */
    public void setRunningBit(final boolean b) {
        this.RunningBit = b;
    }

    /**
     * Sets the running length.
     *
     * @param number the new running length
     */
    public void setRunningLength(final int number) {
        this.RunningLength = number;
    }

    /**
     * Size in uncompressed words.
     *
     * @return the int
     */
    public int size() {
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
    public BufferedRunningLengthWord32 clone()
            throws CloneNotSupportedException {
        BufferedRunningLengthWord32 answer = (BufferedRunningLengthWord32) super
                .clone();
        answer.literalWordOffset = this.literalWordOffset;
        answer.NumberOfLiteralWords = this.NumberOfLiteralWords;
        answer.RunningBit = this.RunningBit;
        answer.RunningLength = this.RunningLength;
        return answer;
    }

    /**
     * how many literal words have we read so far?
     */
    public int literalWordOffset = 0;

    /**
     * The Number of literal words.
     */
    protected int NumberOfLiteralWords;

    /**
     * The Running bit.
     */
    public boolean RunningBit;

    /**
     * The Running length.
     */
    public int RunningLength;

}
