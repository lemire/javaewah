package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.Arrays;

/**
 * Long array wrapper.
 * Users should not be concerned by this class.
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class LongArray implements Buffer, Cloneable {

    /**
     * Creates a buffer with default size
     */
    public LongArray() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Creates a buffer with explicit size
     * @param bufferSize
     */
    public LongArray(int bufferSize) {
        if(bufferSize < 1) {
            bufferSize = 1;
        }
        this.buffer = new long[bufferSize];
    }
    
    @Override
    public int sizeInWords() {
        return this.actualSizeInWords;
    }

    @Override
    public void ensureCapacity(int capacity) {
        resizeBuffer(capacity - this.actualSizeInWords);
    }

    @Override
    public long getWord(int position) {
        return this.buffer[position];
    }
    
    @Override
    public long getLastWord() {
        return getWord(this.actualSizeInWords - 1);
    }

    @Override
    public void clear() {
        this.actualSizeInWords = 1;
        this.buffer[0] = 0;
    }
    
    @Override
    public void trim() {
        this.buffer = Arrays.copyOf(this.buffer, this.actualSizeInWords);
    }
    
    @Override
    public void setWord(int position, long word) {
        this.buffer[position] = word;
    }
    
    @Override
    public void setLastWord(long word) {
        setWord(this.actualSizeInWords - 1, word);
    }
    
    @Override
    public void push_back(long word) {
        resizeBuffer(1);
        this.buffer[this.actualSizeInWords++] = word;
    }

    @Override
    public void push_back(Buffer buffer, int start, int number) {
        resizeBuffer(number);
        if(buffer instanceof LongArray) {
            long[] data = ((LongArray)buffer).buffer;
            System.arraycopy(data, start, this.buffer, this.actualSizeInWords, number);
        } else {
            for(int i = 0; i < number; ++i) {
                this.buffer[this.actualSizeInWords + i] = buffer.getWord(start + i);
            }
        }
        this.actualSizeInWords += number;
    }

    @Override
    public void negative_push_back(Buffer buffer, int start, int number) {
        resizeBuffer(number);
        for (int i = 0; i < number; ++i) {
            this.buffer[this.actualSizeInWords + i] = ~buffer.getWord(start + i);
        }
        this.actualSizeInWords += number;
    }
    
    @Override
    public void removeLastWord() {
        setWord(--this.actualSizeInWords, 0l);
    }
    
    @Override
    public void negateWord(int position) {
        this.buffer[position] = ~this.buffer[position];
    }
    
    @Override
    public void andWord(int position, long mask) {
        this.buffer[position] &= mask;
    }

    @Override
    public void orWord(int position, long mask) {
        this.buffer[position] |= mask;
    }
    
    @Override
    public void andLastWord(long mask) {
        andWord(this.actualSizeInWords - 1, mask);
    }
    
    @Override
    public void orLastWord(long mask) {
        orWord(this.actualSizeInWords - 1, mask);
    }
    
    @Override
    public void expand(int position, int length) {
        resizeBuffer(length);
        System.arraycopy(this.buffer, position, this.buffer, position + length, this.actualSizeInWords - position);
        this.actualSizeInWords += length;
    }
    
    @Override
    public void collapse(int position, int length) {
        System.arraycopy(this.buffer, position + length, this.buffer, position, this.actualSizeInWords - position - length);
        for(int i = 0; i < length; ++i) {
            removeLastWord();
        }
    }
    
    @Override
    public LongArray clone() {
        LongArray clone = null;
        try {
            clone = (LongArray) super.clone();
            clone.buffer = this.buffer.clone();
            clone.actualSizeInWords = this.actualSizeInWords;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // cannot happen
        }
        return clone;
    }

    @Override
    public void swap(final Buffer other) {
        if(other instanceof LongArray) {
            long[] tmp = this.buffer;
            this.buffer = ((LongArray)other).buffer;
            ((LongArray)other).buffer = tmp;

            int tmp2 = this.actualSizeInWords;
            this.actualSizeInWords = ((LongArray)other).actualSizeInWords;
            ((LongArray)other).actualSizeInWords = tmp2;
        } else {
            long[] tmp = new long[other.sizeInWords()];
            for(int i = 0; i < other.sizeInWords(); ++i) {
                tmp[i] = other.getWord(i);
            }
            int tmp2 = other.sizeInWords();

            other.clear();
            other.removeLastWord();
            other.push_back(this, 0, this.sizeInWords());

            this.buffer = tmp;
            this.actualSizeInWords = tmp2;
        }
    }

    /**
     * Resizes the buffer if the number of words to add exceeds the buffer capacity.
     * @param number the number of words to add
     */
    private void resizeBuffer(int number) {
        int size = newSizeInWords(number);
        if (size >= this.buffer.length) {
            long oldBuffer[] = this.buffer;
            this.buffer = new long[size];
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
     * The buffer (array of 64-bit words)
     */
    private long buffer[] = null;
    
    /**
     * The Constant DEFAULT_BUFFER_SIZE: default memory allocation when the
     * object is constructed.
     */
    private static final int DEFAULT_BUFFER_SIZE = 4;
    
}
