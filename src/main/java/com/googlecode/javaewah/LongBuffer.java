package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */


final class LongBuffer {

    public LongBuffer(java.nio.LongBuffer buffer) {
        this.buffer = buffer;
    }
    
    public int sizeInWords() {
        return this.actualSizeInWords;
    }
    
    public long getWord(int position) {
        return this.buffer.get(position);
    }

    public long getLastWord() {
        return getWord(this.actualSizeInWords - 1);
    }

    public long[] getWords() {
        long[] words = new long[this.actualSizeInWords];
        this.buffer.get(words, 0, this.actualSizeInWords);
        return words;
    }

    public void clear() {
        this.actualSizeInWords = 1;
        setWord(0, 0);
    }

    public void setWord(int position, long word) {
        this.buffer.put(position, word);
    }

    public void setLastWord(long word) {
        setWord(this.actualSizeInWords - 1, word);
    }

    public void push_back(long word) {
        setWord(this.actualSizeInWords++, word);
    }

    public void push_back(long[] data, int start, int number) {
        for(int i = 0; i < number; ++i) {
            push_back(data[start + i]);
        }
    }

    public void negative_push_back(long[] data, int start, int number) {
        for(int i = 0; i < number; ++i) {
            push_back(~data[start + i]);
        }
    }

    public void removeLastWord() {
        setWord(--this.actualSizeInWords, 0l);
    }

    public void negateWord(int position) {
        setWord(position, ~getWord(position));
    }

    public void andWord(int position, long mask) {
        setWord(position, getWord(position) & mask);
    }

    public void orWord(int position, long mask) {
        setWord(position, getWord(position) | mask);
    }

    public void andLastWord(long mask) {
        andWord(this.actualSizeInWords - 1, mask);
    }

    public void orLastWord(long mask) {
        orWord(this.actualSizeInWords - 1, mask);
    }

    public void expand(int position, int length) {
        for(int i = this.actualSizeInWords - position - 1; i >= 0; --i) {
            setWord(position + length + i, getWord(position + i));
        }
        this.actualSizeInWords += length;
    }

    public void collapse(int position, int length) {
        for(int i = 0; i < this.actualSizeInWords - position - length; ++i) {
            setWord(position + i, getWord(position + length + i));
        }
        for(int i = 0; i < length; ++i) {
            removeLastWord();
        }
    }
    
    /**
     * The actual size in words.
     */
    private int actualSizeInWords = 1;
    
    /**
     * The buffer
     */
    private final java.nio.LongBuffer buffer;
    
}
