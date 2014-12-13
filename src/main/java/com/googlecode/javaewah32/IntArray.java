package com.googlecode.javaewah32;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.Arrays;

/**
 * Int array wrapper class.
 * Users should not be concerned by this class.
 *
 * @author Gregory Ssi-Yan-Kai
 */
class IntArray implements Cloneable {

    /**
     * Creates a buffer with default size
     */
    public IntArray() {
        this(DEFAULT_BUFFER_SIZE);
    }
 
    /**
     * Creates a buffer with explicit size
     * @param bufferSize
     */
    public IntArray(int bufferSize) {
        if(bufferSize < 1) {
            bufferSize = 1;
        }
        this.buffer = new int[bufferSize];
    }
    
    /**
     * Returns the actual size in words
     */
    public int sizeInWords() {
        return this.actualSizeInWords;
    }

    /**
     * Returns the word at a given position
     * @param position
     * @return the word
     */
    public int getWord(int position) {
        return this.buffer[position];
    }

    /**
     * Returns the last word of the buffer
     * @return the last word
     */
    public int getLastWord() {
        return getWord(this.actualSizeInWords - 1);
    }

    /**
     * Returns the words contained in the buffer
     * @return the long array
     */
    public int[] getWords() {
        return this.buffer;
    }
    
    /**
     * Resets the buffer
     * The buffer is not fully cleared and any new set operations should
     * overwrite stale data
     */
    public void clear() {
        this.actualSizeInWords = 1;
        this.buffer[0] = 0;
    }
    
    /**
     * Reduces the internal buffer to its minimal allowable size.
     * This can free memory.
     */
    public void trim() {
        this.buffer = Arrays.copyOf(this.buffer, this.actualSizeInWords);
    }
    
    /**
     * Replaces the word at the given position in the buffer with
     * the specified word.
     * @param position
     * @param word
     */
    public void setWord(int position, int word) {
        this.buffer[position] = word;
    }
    
    /**
     * Replaces the last word in the buffer with
     * the specified word.
     * @param word
     */
    public void setLastWord(int word) {
        setWord(this.actualSizeInWords - 1, word);
    }
    
    /**
     * Appends the specified word to the end of the buffer
     * @param word
     */
    public void push_back(int word) {
        resizeBuffer(1);
        this.buffer[this.actualSizeInWords++] = word;
    }

    /**
     * Appends the specified array of words to the end of the buffer.
     * @param data   the array of words
     * @param start  the position of the first word to add
     * @param number the number of words to add
     */
    public void push_back(int[] data, int start, int number) {
        resizeBuffer(number);
        System.arraycopy(data, start, this.buffer, this.actualSizeInWords, number);
        this.actualSizeInWords += number;
    }
    
    /**
     * Same as push_back, but the words are negated.
     *
     * @param data   the array of words
     * @param start  the position of the first word to add
     * @param number the number of words to add
     */
    public void negative_push_back(int[] data, int start, int number) {
        resizeBuffer(number);
        for (int i = 0; i < number; ++i) {
            this.buffer[this.actualSizeInWords + i] = ~data[start + i];
        }
        this.actualSizeInWords += number;
    }
    
    /**
     * Removes the last word from the buffer
     */
    public void removeLastWord() {
        setWord(--this.actualSizeInWords, 0);
    }
    
    /**
     * Negates the word at the given position in the buffer
     * @param position
     */
    public void negateWord(int position) {
        this.buffer[position] = ~this.buffer[position];
    }
    
    /**
     * Replaces the word at the given position in the buffer
     * with its bitwise-and with the given mask.
     * @param position
     * @param mask
     */
    public void andWord(int position, int mask) {
        this.buffer[position] &= mask;
    }
    
    /**
     * Replaces the word at the given position in the buffer
     * with its bitwise-or with the given mask.
     * @param position
     * @param mask
     */
    public void orWord(int position, int mask) {
        this.buffer[position] |= mask;
    }
    
    /**
     * Replaces the last word position in the buffer
     * with its bitwise-and with the given mask.
     * @param mask
     */
    public void andLastWord(int mask) {
        andWord(this.actualSizeInWords - 1, mask);
    }
    
    /**
     * Replaces the last word position in the buffer
     * with its bitwise-or with the given mask.
     * @param mask
     */
    public void orLastWord(int mask) {
        orWord(this.actualSizeInWords - 1, mask);
    }
    
    /**
     * Expands the buffer by adding the given number of words at the given position.
     * The added words may contain stale data.
     * @param position the position of the buffer where to add words
     * @param length   the number of words to add
     */
    public void expand(int position, int length) {
        resizeBuffer(length);
        System.arraycopy(this.buffer, position, this.buffer, position + length, this.actualSizeInWords - position);
        this.actualSizeInWords += length;
    }
    
    /**
     * Removes a given number of words at the given position in the buffer.
     * The freed words at the end of the buffer are properly cleaned.
     * @param position the position of the buffer where to add words
     * @param length   the number of words to add
     */
    public void collapse(int position, int length) {
        System.arraycopy(this.buffer, position + length, this.buffer, position, this.actualSizeInWords - position - length);
        for(int i = 0; i < length; ++i) {
            removeLastWord();
        }
    }
    
    /**
     * Creates and returns a copy of the buffer
     */
    public IntArray clone() {
        IntArray clone = null;
        try {
            clone = (IntArray) super.clone();
            clone.buffer = this.buffer.clone();
            clone.actualSizeInWords = this.actualSizeInWords;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // cannot happen
        }
        return clone;
    }
    
    /**
     * Resizes the buffer if the number of words to add exceeds the buffer capacity.
     * @param number the number of words to add
     */
    private void resizeBuffer(int number) {
        int size = newSizeInWords(number);
        if (size >= this.buffer.length) {
            int oldBuffer[] = this.buffer;
            this.buffer = new int[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);
        }
    }

    /**
     * Returns the resulting buffer size in words given the number of words to add.
     * @param number the number of words to add
     */
    private int newSizeInWords(int number) {
        int size = this.actualSizeInWords + number;
        if (size >= this.buffer.length) {
            if (size < 32768)
                size = size * 2;
            else if (size * 3 / 2 < size) // overflow
                size = Integer.MAX_VALUE;
            else
                size = size * 3 / 2;
        }
        return size;
    }
    
    /**
     * The actual size in words.
     */
    private int actualSizeInWords = 1;
    
    /**
     * The buffer (array of 32-bit words)
     */
    private int buffer[] = null;
    
    /**
     * The Constant DEFAULT_BUFFER_SIZE: default memory allocation when the
     * object is constructed.
     */
    private static final int DEFAULT_BUFFER_SIZE = 4;
    
}
