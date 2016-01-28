package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use.
 *
 * @author Daniel Lemire
 * @since 0.5.0
 */
public final class RunningLengthWord32 implements Cloneable {

    /**
     * Instantiates a new running length word.
     *
     * @param buffer the buffer
     * @param p position in the array where the running length word is
     *          located.
     */
    RunningLengthWord32(final Buffer32 buffer, final int p) {
        this.buffer = buffer;
        this.position = p;
    }

    /**
     * Gets the number of literal words.
     *
     * @return the number of literal words
     */
    public int getNumberOfLiteralWords() {
        return getNumberOfLiteralWords(this.buffer, this.position);
    }

    static int getNumberOfLiteralWords(final Buffer32 buffer, final int position) {
        return (buffer.getWord(position) >>> (1 + RUNNING_LENGTH_BITS));
    }

    /**
     * Gets the running bit.
     *
     * @return the running bit
     */
    public boolean getRunningBit() {
        return getRunningBit(this.buffer, this.position);
    }

    static boolean getRunningBit(final Buffer32 buffer, final int position) {
        return (buffer.getWord(position) & 1) != 0;
    }

    /**
     * Gets the running length.
     *
     * @return the running length
     */
    public int getRunningLength() {
        return getRunningLength(this.buffer, this.position);
    }

    static int getRunningLength(final Buffer32 buffer, final int position) {
        return (buffer.getWord(position) >>> 1) & LARGEST_RUNNING_LENGTH_COUNT;
    }

    /**
     * Sets the number of literal words.
     *
     * @param number the new number of literal words
     */
    public void setNumberOfLiteralWords(final int number) {
        setNumberOfLiteralWords(this.buffer, this.position, number);
    }

    static void setNumberOfLiteralWords(final Buffer32 buffer, final int position, final int number) {
        buffer.orWord(position, NOT_RUNNING_LENGTH_PLUS_RUNNING_BIT);
        buffer.andWord(position, (number << (RUNNING_LENGTH_BITS + 1)) | RUNNING_LENGTH_PLUS_RUNNING_BIT);
    }

    /**
     * Sets the running bit.
     *
     * @param b the new running bit
     */
    public void setRunningBit(final boolean b) {
        setRunningBit(this.buffer, this.position, b);
    }

    static void setRunningBit(final Buffer32 buffer, final int position, final boolean b) {
        if (b)
            buffer.orWord(position, 1);
        else
            buffer.andWord(position, ~1);
    }

    /**
     * Sets the running length.
     *
     * @param number the new running length
     */
    public void setRunningLength(final int number) {
        setRunningLength(this.buffer, this.position, number);
    }

    static void setRunningLength(final Buffer32 buffer, final int position, final int number) {
        buffer.orWord(position, SHIFTED_LARGEST_RUNNING_LENGTH_COUNT);
        buffer.andWord(position, (number << 1) | NOT_SHIFTED_LARGEST_RUNNING_LENGTH_COUNT);
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
        return (RunningLengthWord32) super.clone();
    }

    /**
     * The array of words.
     */
    final Buffer32 buffer;

    /**
     * The position in array.
     */
    int position;

    /**
     * number of bits dedicated to marking of the running length of clean
     * words
     */
    public static final int RUNNING_LENGTH_BITS = 16;

    private static final int LITERAL_BITS = 32 - 1 - RUNNING_LENGTH_BITS;

    /**
     * largest number of literal words in a run.
     */
    public static final int LARGEST_LITERAL_COUNT = (1 << LITERAL_BITS) - 1;

    /**
     * largest number of clean words in a run
     */
    public static final int LARGEST_RUNNING_LENGTH_COUNT = (1 << RUNNING_LENGTH_BITS) - 1;

    private static final int RUNNING_LENGTH_PLUS_RUNNING_BIT = (1 << (RUNNING_LENGTH_BITS + 1)) - 1;

    private static final int SHIFTED_LARGEST_RUNNING_LENGTH_COUNT = LARGEST_RUNNING_LENGTH_COUNT << 1;

    private static final int NOT_RUNNING_LENGTH_PLUS_RUNNING_BIT = ~RUNNING_LENGTH_PLUS_RUNNING_BIT;

    private static final int NOT_SHIFTED_LARGEST_RUNNING_LENGTH_COUNT = ~SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;

}
