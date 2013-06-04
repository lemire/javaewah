package com.googlecode.javaewah;


/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under APL 2.0.
 */

/**
 * The class EWAHIterator represents a special type of
 * efficient iterator iterating over (uncompressed) words of bits.
 * It is not meant for end users.
 * @author Daniel Lemire
 * @since 0.1.0
 *
 */
public final class EWAHIterator {
  
  /**
   * Instantiates a new EWAH iterator.
   *
   * @param a the array of words
   * @param sizeinwords the number of words that are significant in the array of words
   */
  public EWAHIterator(final long[] a, final int sizeinwords) {
    this.rlw = new RunningLengthWord(a, 0);
    this.size = sizeinwords;
    this.pointer = 0;
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
   * Access to the array of words
   *
   * @return the long[]
   */
  public long[] buffer() {
    return this.rlw.array;
  }
  
  /**
   * Position of the literal words represented by this running length word.
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

  /** The pointer represent the location of the current running length
   *  word in the array of words (embedded in the rlw attribute). */
  int pointer;

  /** The current running length word. */
  RunningLengthWord rlw;

  /** The size in words. */
  int size;

}
