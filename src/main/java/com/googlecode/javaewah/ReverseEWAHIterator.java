package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.Stack;

/**
 * The class ReverseEWAHIterator represents a special type of efficient iterator
 * iterating over (uncompressed) words of bits in reverse order.
 *
 * @author Gregory Ssi-Yan-Kai
 */
final class ReverseEWAHIterator {

    /**
     * Instantiates a new reverse EWAH iterator.
     *
     * @param a           the array of words
     * @param sizeInWords the number of words that are significant in the array of words
     */
    public ReverseEWAHIterator(final EWAHCompressedBitmap a, final int sizeInWords) {
        this.pointer = 0;
        this.rlw = new RunningLengthWord(a, this.pointer);
        this.positions = new Stack<Integer>();
        this.positions.ensureCapacity(sizeInWords);
        while(this.pointer < sizeInWords) {
            this.positions.push(this.pointer);
            this.rlw.position = this.pointer;
            this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        }
    }

    /**
     * Access to the array of words
     *
     * @return the long[]
     */
    public long[] buffer() {
        return this.rlw.parent.buffer;
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
    public RunningLengthWord previous() {
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
    protected RunningLengthWord rlw;

}
