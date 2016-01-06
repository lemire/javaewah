package com.googlecode.javaewah32;


/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * The class EWAHIterator represents a special type of efficient iterator
 * iterating over (uncompressed) words of bits.
 *
 * @author Daniel Lemire
 * @since 0.5.0
 */
public final class EWAHIterator32 implements Cloneable {

    /**
     * Instantiates a new eWAH iterator.
     *
     * @param buffer      the buffer
     */
    public EWAHIterator32(final Buffer32 buffer) {
        this.rlw = new RunningLengthWord32(buffer, 0);
        this.size = buffer.sizeInWords();
        this.pointer = 0;
    }

    private EWAHIterator32(int pointer, RunningLengthWord32 rlw, int size){
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
    public static EWAHIterator32 getEWAHIterator(
            EWAHCompressedBitmap32 bitmap) {
        return bitmap.getEWAHIterator();
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
    public RunningLengthWord32 next() {
        this.rlw.position = this.pointer;
        this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        return this.rlw;
    }

    @Override
    public EWAHIterator32 clone() throws CloneNotSupportedException {
        return new EWAHIterator32(pointer,rlw.clone(),size);
    }

    /**
     * The pointer represent the location of the current running length word
     * in the array of words (embedded in the rlw attribute).
     */
    private int pointer;

    /**
     * The current running length word.
     */
    final RunningLengthWord32 rlw;

    /**
     * The size in words.
     */
    private final int size;

}
