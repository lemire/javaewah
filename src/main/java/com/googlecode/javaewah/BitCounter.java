package com.googlecode.javaewah;
/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * BitCounter is a fake bitset data structure. Instead of storing the actual
 * data, it only records the number of set bits.
 *
 * @author David McIntosh
 * @since 0.4.0
 */

public final class BitCounter implements BitmapStorage {

    /**
     * Virtually add words directly to the bitmap
     *
     * @param newData the word
     */
    @Override
    public void addWord(final long newData) {
        this.oneBits += Long.bitCount(newData);
    }
    
    /**
     * Virtually add literal words directly to the bitmap
     *
     * @param newData the word
     */
    @Override
    public void addLiteralWord(final long newData) {
        this.oneBits += Long.bitCount(newData);
    }

    /**
     * virtually add several literal words.
     *
     * @param buffer the buffer wrapping the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    @Override
    public void addStreamOfLiteralWords(final Buffer buffer, int start, int number) {
        for (int i = start; i < start + number; i++) {
            addLiteralWord(buffer.getWord(i));
        }
    }

    /**
     * virtually add many zeroes or ones.
     *
     * @param v      zeros or ones
     * @param number how many to words add
     */
    @Override
    public void addStreamOfEmptyWords(boolean v, long number) {
        if (v) {
            this.oneBits += (int)(number * EWAHCompressedBitmap.WORD_IN_BITS);
        }
    }

    /**
     * virtually add several negated literal words.
     *
     * @param buffer the buffer wrapping the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    @Override
    public void addStreamOfNegatedLiteralWords(final Buffer buffer, int start, int number) {
        for (int i = start; i < start + number; i++) {
            addLiteralWord(~buffer.getWord(i));
        }
    }

    @Override
    public void clear() {
        this.oneBits = 0;
    }

    /**
     * As you act on this class, it records the number of set (true) bits.
     *
     * @return number of set bits
     */
    public int getCount() {
        return this.oneBits;
    }

    /**
     * should directly set the sizeInBits field, but is effectively ignored
     * in this class.
     *
     * @param bits number of bits
     */
    // @Override : causes problems with Java 1.5
    @Override
    public void setSizeInBitsWithinLastWord(int bits) {
        // no action
    }

    private int oneBits;
}
