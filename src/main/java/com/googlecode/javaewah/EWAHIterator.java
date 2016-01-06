package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * The class EWAHIterator represents a special type of efficient iterator
 * iterating over (uncompressed) words of bits. It is not meant for end users.
 *
 * @author Daniel Lemire
 * @since 0.1.0
 */
public final class EWAHIterator implements Cloneable {

    /**
     * Instantiates a new EWAH iterator.
     *
     * @param buffer      the buffer
     */
    public EWAHIterator(final Buffer buffer) {
        this.rlw = new RunningLengthWord(buffer, 0);
        this.size = buffer.sizeInWords();
        this.pointer = 0;
    }

    private EWAHIterator(int pointer, RunningLengthWord rlw, int size){
    	this.pointer = pointer;
    	this.rlw = rlw;
    	this.size = size;    	
    }

    /**
     * Allow expert developers to instantiate an EWAHIterator.
     *
     * @param bitmap we want to iterate over
     * @return an iterator
     */
    public static EWAHIterator getEWAHIterator(EWAHCompressedBitmap bitmap) {
        return bitmap.getEWAHIterator();
    }

    /**
     * Access to the buffer
     *
     * @return the buffer
     */
    public Buffer buffer() {
        return this.rlw.buffer;
    }

    /**
     * Position of the literal words represented by this running length
     * word.
     *
     * @return the int
     */
    public int literalWords() {
        return this.pointer - this.rlw.getNumberOfLiteralWords();
    }

    /**
     * Checks for next.
     *
     * @return true, if successful
     */
    public boolean hasNext() {
        return this.pointer < this.size;
    }

    /**
     * Next running length word.
     *
     * @return the running length word
     */
    public RunningLengthWord next() {
        this.rlw.position = this.pointer;
        this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        return this.rlw;
    }

    @Override
    public EWAHIterator clone() throws CloneNotSupportedException {
        return new EWAHIterator(pointer,rlw.clone(),size);
    }

    /**
     * The pointer represent the location of the current running length word
     * in the array of words (embedded in the rlw attribute).
     */
    private int pointer;

    /**
     * The current running length word.
     */
    final RunningLengthWord rlw;

    /**
     * The size in words.
     */
    private final int size;

}
