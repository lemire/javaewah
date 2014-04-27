package com.googlecode.javaewah32;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
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
     * @param a an array of 32-bit words
     * @param p position in the array where the running length word is
     *          located.
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
        return (this.parent.buffer[this.position] >>> (1 + RUNNING_LENGTH_BITS));
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
                & LARGEST_RUNNING_LENGTH_COUNT;
    }

    /**
     * Sets the number of literal words.
     *
     * @param number the new number of literal words
     */
    public void setNumberOfLiteralWords(final int number) {
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
            this.parent.buffer[this.position] |= 1;
        else
            this.parent.buffer[this.position] &= ~1;
    }

    /**
     * Sets the running length.
     *
     * @param number the new running length
     */
    public void setRunningLength(final int number) {
        this.parent.buffer[this.position] |= SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;
        this.parent.buffer[this.position] &= (number << 1)
                | NOT_SHIFTED_LARGEST_RUNNING_LENGTH_COUNT;
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
    public final EWAHCompressedBitmap32 parent;

    /**
     * The position in array.
     */
    protected int position;

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