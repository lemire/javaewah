package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.Stack;


/**
 * The class ReverseEWAHIterator32 represents a special type of efficient iterator
 * iterating over (uncompressed) words of bits in reverse order.
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class ReverseEWAHIterator32 {

    /**
     * Instantiates a new reverse EWAH iterator.
     *
     * @param buffer      the buffer
     */
    public ReverseEWAHIterator32(final Buffer32 buffer) {
        this.pointer = 0;
        this.rlw = new RunningLengthWord32(buffer, this.pointer);
        this.positions = new Stack<Integer>();
        this.positions.ensureCapacity(buffer.sizeInWords());
        while(this.pointer < buffer.sizeInWords()) {
            this.positions.push(this.pointer);
            this.rlw.position = this.pointer;
            this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        }
    }

    /**
     * Access to the buffer
     *
     * @return the buffer
     */
    public Buffer32 buffer() {
        return this.rlw.buffer;
    }

    /**
     * Position of the current running length word.
     *
     * @return the int
     */
    public int position() {
        return this.pointer;
    }

    /**
     * Checks for previous.
     *
     * @return true, if successful
     */
    public boolean hasPrevious() {
        return !this.positions.isEmpty();
    }

    /**
     * Previous running length word.
     *
     * @return the running length word
     */
    public RunningLengthWord32 previous() {
        this.pointer = this.positions.pop();
        this.rlw.position = this.pointer;
        return this.rlw;
    }

    /**
     * The positions of running length words (embedded in the rlw attribute).
     */
    private Stack<Integer> positions;

    /**
     * The pointer representing the location of the current running length word
     * in the array of words (embedded in the rlw attribute).
     */
    private int pointer;

    /**
     * The current running length word.
     */
    protected RunningLengthWord32 rlw;

}
