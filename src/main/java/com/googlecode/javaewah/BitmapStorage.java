package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Low level bitset writing methods.
 *
 * @author David McIntosh
 * @since 0.4.0
 */
public interface BitmapStorage {

    /**
     * Adding words directly to the bitmap (for expert use).
     * 
     * This is normally how you add data to the array. So you add bits in
     * streams of 8*8 bits.
     *
     * @param newData the word
     */
    void addWord(final long newData);
    
    /**
     * Adding literal words directly to the bitmap (for expert use).
     *
     * @param newData the word
     */
    void addLiteralWord(final long newData);

    /**
     * if you have several literal words to copy over, this might be faster.
     *
     * @param buffer the buffer wrapping the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    void addStreamOfLiteralWords(final Buffer buffer, final int start, final int number);

    /**
     * For experts: You want to add many zeroes or ones? This is the method
     * you use.
     *
     * @param v      zeros or ones
     * @param number how many to words add
     */
    void addStreamOfEmptyWords(final boolean v, final long number);

    /**
     * Like "addStreamOfLiteralWords" but negates the words being added.
     *
     * @param buffer the buffer wrapping the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    void addStreamOfNegatedLiteralWords(final Buffer buffer, final int start, final int number);

    /**
     * Empties the container.
     */
    void clear();

    /**
     * Sets the size in bits of the bitmap as an *uncompressed* bitmap.
     * Normally, this is used to reduce the size of the bitmaps within
     * the scope of the last word. Specifically, this means that
     * (sizeInBits()+63)/64 must be equal to (size +63)/64.
     * If needed, the bitmap can be further padded with zeroes. 
     *  
     * @param size         the size in bits
     */
    void setSizeInBitsWithinLastWord(final int size);
}
