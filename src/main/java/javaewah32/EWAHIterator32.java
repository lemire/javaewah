package javaewah32;


/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */

/**
 * The class EWAHIterator represents a special type of
 * efficient iterator iterating over (uncompressed) words of bits.
 *
 * @author Daniel Lemire
 * @since 0.5.0
 *
 */
public final class EWAHIterator32 {
  
  /** The current running length word. */
  RunningLengthWord32 rlw;
  
  /** The size in words. */
  int size;
  
  /** The pointer represent the location of the current running length
   *  word in the array of words (embedded in the rlw attribute). */
  int pointer;

  /**
   * Instantiates a new eWAH iterator.
   *
   * @param a the array of words
   * @param sizeinwords the number of words that are significant in the array of words
   */
  public EWAHIterator32(final int[] a, final int sizeinwords) {
    this.rlw = new RunningLengthWord32(a, 0);
    this.size = sizeinwords;
    this.pointer = 0;
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

  /**
   * Position of the dirty words represented by this running length word.
   *
   * @return the int
   */
  public int dirtyWords() {
    return this.pointer - this.rlw.getNumberOfLiteralWords();
  }

  /**
   * Access to the array of words
   *
   * @return the int[]
   */
  public int[] buffer() {
    return this.rlw.array;
  }

}
