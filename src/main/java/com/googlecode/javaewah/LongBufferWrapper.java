package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.nio.LongBuffer;

/**
 * java.nio.LongBuffer wrapper.
 * Users should not be concerned by this class.
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class LongBufferWrapper implements Buffer, Cloneable {

    public LongBufferWrapper(LongBuffer buffer) {
        this.buffer = buffer;
    }

    public LongBufferWrapper(LongBuffer slice, int sizeInWords) {
    	  this.buffer = slice;
    	  this.actualSizeInWords = sizeInWords;
		}
    
    @Override
    public int sizeInWords() {
        return this.actualSizeInWords;
    }

    @Override
    public void ensureCapacity(int capacity) {
        if(capacity > buffer.capacity()) {
            throw new RuntimeException("Cannot increase buffer capacity. Current capacity: " + buffer.capacity() + ". New capacity: " + capacity);
        }
    }

    @Override
    public long getWord(int position) {
        return this.buffer.get(position);
    }

    @Override
    public long getLastWord() {
        return getWord(this.actualSizeInWords - 1);
    }

    @Override
    public void clear() {
        this.actualSizeInWords = 1;
        setWord(0, 0);
    }

    @Override
    public void trim() {
    }

    @Override
    public void setWord(int position, long word) {
        this.buffer.put(position, word);
    }

    @Override
    public void setLastWord(long word) {
        setWord(this.actualSizeInWords - 1, word);
    }

    @Override
    public void push_back(long word) {
        setWord(this.actualSizeInWords++, word);
    }

    @Override
    public void push_back(Buffer buffer, int start, int number) {
        for(int i = 0; i < number; ++i) {
            push_back(buffer.getWord(start + i));
        }
    }

    @Override
    public void negative_push_back(Buffer buffer, int start, int number) {
        for(int i = 0; i < number; ++i) {
            push_back(~buffer.getWord(start + i));
        }
    }

    @Override
    public void removeLastWord() {
        setWord(--this.actualSizeInWords, 0l);
    }

    @Override
    public void negateWord(int position) {
        setWord(position, ~getWord(position));
    }

    @Override
    public void andWord(int position, long mask) {
        setWord(position, getWord(position) & mask);
    }

    @Override
    public void orWord(int position, long mask) {
        setWord(position, getWord(position) | mask);
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
        for(int i = this.actualSizeInWords - position - 1; i >= 0; --i) {
            setWord(position + length + i, getWord(position + i));
        }
        this.actualSizeInWords += length;
    }

    @Override
    public void collapse(int position, int length) {
        for(int i = 0; i < this.actualSizeInWords - position - length; ++i) {
            setWord(position + i, getWord(position + length + i));
        }
        for(int i = 0; i < length; ++i) {
            removeLastWord();
        }
    }

    @Override
    public LongBufferWrapper clone() throws CloneNotSupportedException {
       return new LongBufferWrapper(this.buffer, this.actualSizeInWords);
    }

    @Override
    public void swap(final Buffer other) {
        if (other instanceof LongBufferWrapper) {// optimized version
            LongBufferWrapper o = (LongBufferWrapper) other;
            LongBuffer tmp = this.buffer;
            int tmp2 = this.actualSizeInWords;
            this.actualSizeInWords = o.actualSizeInWords;
            this.buffer = o.buffer;
            o.actualSizeInWords = tmp2;
            o.buffer = tmp;
        } else {
            other.swap(this);
        }
    }
    
    /**
     * The actual size in words.
     */
    private int actualSizeInWords = 1;
    
    /**
     * The buffer
     */
    private LongBuffer buffer;
    
}
