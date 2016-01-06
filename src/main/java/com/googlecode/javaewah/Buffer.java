package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Buffer interface.
 * Users should not be concerned by this class.
 *
 * @author Gregory Ssi-Yan-Kai
 */
interface Buffer {

    /**
     * Returns the actual size in words
     */
    int sizeInWords();

    /**
     * Increases the size of the buffer if necessary
     */
    void ensureCapacity(int capacity);

    /**
     * Returns the word at a given position
     * @param position
     * @return the word
     */
    long getWord(int position);

    /**
     * Returns the last word of the buffer
     * @return the last word
     */
    long getLastWord();

    /**
     * Resets the buffer
     * The buffer is not fully cleared and any new set operations should
     * overwrite stale data
     */
    void clear();

    /**
     * Reduces the internal buffer to its minimal allowable size.
     * This can free memory.
     */
    void trim();

    /**
     * Replaces the word at the given position in the buffer with
     * the specified word.
     * @param position
     * @param word
     */
    void setWord(int position, long word);

    /**
     * Replaces the last word in the buffer with
     * the specified word.
     * @param word
     */
    void setLastWord(long word);

    /**
     * Appends the specified word to the end of the buffer
     * @param word
     */
    void push_back(long word);

    /**
     * Appends the specified buffer words to the end of the buffer.
     * @param buffer the buffer
     * @param start  the position of the first word to add
     * @param number the number of words to add
     */
    void push_back(Buffer buffer, int start, int number);

    /**
     * Same as push_back, but the words are negated.
     *
     * @param buffer the buffer
     * @param start  the position of the first word to add
     * @param number the number of words to add
     */
    void negative_push_back(Buffer buffer, int start, int number);

    /**
     * Removes the last word from the buffer
     */
    void removeLastWord();

    /**
     * Negates the word at the given position in the buffer
     * @param position
     */
    void negateWord(int position);

    /**
     * Replaces the word at the given position in the buffer
     * with its bitwise-and with the given mask.
     * @param position
     * @param mask
     */
    void andWord(int position, long mask);

    /**
     * Replaces the word at the given position in the buffer
     * with its bitwise-or with the given mask.
     * @param position
     * @param mask
     */
    void orWord(int position, long mask);

    /**
     * Replaces the last word position in the buffer
     * with its bitwise-and with the given mask.
     * @param mask
     */
    void andLastWord(long mask);

    /**
     * Replaces the last word position in the buffer
     * with its bitwise-or with the given mask.
     * @param mask
     */
    void orLastWord(long mask);

    /**
     * Expands the buffer by adding the given number of words at the given position.
     * The added words may contain stale data.
     * @param position the position of the buffer where to add words
     * @param length   the number of words to add
     */
    void expand(int position, int length);

    /**
     * Removes a given number of words at the given position in the buffer.
     * The freed words at the end of the buffer are properly cleaned.
     * @param position the position of the buffer where to add words
     * @param length   the number of words to add
     */
    void collapse(int position, int length);

    /**
     * Creates and returns a copy of the buffer
     */
    Buffer clone() throws CloneNotSupportedException;

    /**
     * Swap the content of the buffer with another.
     *
     * @param other buffer to swap with
     */
    void swap(Buffer other);

}
