package com.googlecode.javaewah;

import java.util.Arrays;

final class LongArray implements Cloneable {

    public LongArray() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    public LongArray(int bufferSize) {
        if(bufferSize < 1) {
            bufferSize = 1;
        }
        this.buffer = new long[bufferSize];
    }
    
    public int sizeInWords() {
        return this.actualSizeInWords;
    }
    
    public long getWord(int position) {
        return this.buffer[position];
    }
    
    public long getLastWord() {
        return getWord(this.actualSizeInWords - 1);
    }
    
    public long[] getWords() {
        return this.buffer;        
    }
    
    public void clear() {
        this.actualSizeInWords = 1;
        // buffer is not fully cleared but any new set operations should
        // overwrite
        // stale data
        this.buffer[0] = 0;
    }
    
    public void trim() {
        this.buffer = Arrays.copyOf(this.buffer, this.actualSizeInWords);
    }
    
    public void setWord(int position, long word) {
        this.buffer[position] = word;
    }
    
    public void setLastWord(long word) {
        setWord(this.actualSizeInWords - 1, word);
    }
    
    public void push_back(long data) {
        int size = newSizeInWords(1);
        if (size >= this.buffer.length) {
            long oldBuffer[] = this.buffer;
            this.buffer = new long[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);
        }
        this.buffer[this.actualSizeInWords++] = data;
    }

    public void push_back(long[] data, int start, int number) {
        int size = newSizeInWords(number);
        if (size >= this.buffer.length) {
            long oldBuffer[] = this.buffer;
            this.buffer = new long[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);
        }
        System.arraycopy(data, start, this.buffer, this.actualSizeInWords, number);
        this.actualSizeInWords += number;
    }
    
    public void negative_push_back(long[] data, int start, int number) {
        int size = newSizeInWords(number);
        if (size >= this.buffer.length) {
            long oldBuffer[] = this.buffer;
            this.buffer = new long[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);
        }
        for (int i = 0; i < number; ++i) {
            this.buffer[this.actualSizeInWords + i] = ~data[start + i];
        }
        this.actualSizeInWords += number;
    }
    
    public void removeLastWord() {
        setWord(--this.actualSizeInWords, 0l);
    }
    
    public void negateWord(int position) {
        this.buffer[position] = ~this.buffer[position];
    }
    
    public void andWord(int position, long mask) {
        this.buffer[position] &= mask;
    }
    
    public void orWord(int position, long mask) {
        this.buffer[position] |= mask;
    }
    
    public void andLastWord(long mask) {
        andWord(this.actualSizeInWords - 1, mask);
    }
    
    public void orLastWord(long mask) {
        orWord(this.actualSizeInWords - 1, mask);
    }
    
    public void expand(int position, int length) {
        int size = newSizeInWords(length);
        long oldBuffer[] = this.buffer;
        if(size >= this.buffer.length) {
            this.buffer = new long[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0, position);
        }
        System.arraycopy(oldBuffer, position, this.buffer, position + length, this.actualSizeInWords - position);
        this.actualSizeInWords += length;
    }
    
    public void collapse(int position, int length) {
        System.arraycopy(this.buffer, position + length, this.buffer, position, this.actualSizeInWords - position - length);
        for(int i = 0; i < length; ++i) {
            removeLastWord();
        }
    }
    
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
    
    /**
     * For internal use.
     *
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
