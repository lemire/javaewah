package com.googlecode.javaewah32;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */



/**
 * Mostly for internal use. Similar to RunningLengthWord, but can
 * be modified without access to the array, and has faster access.
 *
 * @author Daniel Lemire
 * @since 0.5.0
 *
 */
public final class BufferedRunningLengthWord32 {

  /**
   * Instantiates a new buffered running length word.
   *
   * @param a the word
   */
  public BufferedRunningLengthWord32(final int a) {
    this.NumberOfLiteralWords =  (a >>> (1 + RunningLengthWord32.runninglengthbits));
    this.RunningBit = (a & 1) != 0;
    this.RunningLength =  ((a >>> 1) & RunningLengthWord32.largestrunninglengthcount);
  }

  /**
   * Instantiates a new buffered running length word.
   *
   * @param rlw the rlw
   */
  public BufferedRunningLengthWord32(final RunningLengthWord32 rlw) {
    this(rlw.array[rlw.position]);
  }

  /**
   * Discard first words.
   *
   * @param x the x
   */
  public void discardFirstWords(int x) {
    if (this.RunningLength >= x) {
      this.RunningLength -= x;
      return;
    }
    x -= this.RunningLength;
    this.RunningLength = 0;
    this.dirtywordoffset += x;
    this.NumberOfLiteralWords -= x;
  }

  /**
   * Gets the number of literal words.
   *
   * @return the number of literal words
   */
  public int getNumberOfLiteralWords() {
    return this.NumberOfLiteralWords;
  }

  /**
   * Gets the running bit.
   *
   * @return the running bit
   */
  public boolean getRunningBit() {
    return this.RunningBit;
  }

  /**
   * Gets the running length.
   *
   * @return the running length
   */
  public int getRunningLength() {
    return this.RunningLength;
  }

  /**
   * Reset the values using the provided word.
   *
   * @param a the word
   */
  public void reset(final int a) {
    this.NumberOfLiteralWords = (a >>> (1 + RunningLengthWord32.runninglengthbits));
    this.RunningBit = (a & 1) != 0;
    this.RunningLength = ((a >>> 1) & RunningLengthWord32.largestrunninglengthcount);
    this.dirtywordoffset = 0;
  }

  /**
   * Reset the values of this running length word so that it has the same values
   * as the other running length word.
   *
   * @param rlw the other running length word 
   */
  public void reset(final RunningLengthWord32 rlw) {
    reset(rlw.array[rlw.position]);
  }

  /**
   * Sets the number of literal words.
   *
   * @param number the new number of literal words
   */
  public void setNumberOfLiteralWords(final int number) {
    this.NumberOfLiteralWords = number;
  }

  /**
   * Sets the running bit.
   *
   * @param b the new running bit
   */
  public void setRunningBit(final boolean b) {
    this.RunningBit = b;
  }

  /**
   * Sets the running length.
   *
   * @param number the new running length
   */
  public void setRunningLength(final int number) {
    this.RunningLength = number;
  }

  /**
   * Size in uncompressed words.
   *
   * @return the int
   */
  public int size() {
    return this.RunningLength + this.NumberOfLiteralWords;
  }

  /* 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "running bit = " + getRunningBit() + " running length = "
      + getRunningLength() + " number of lit. words "
      + getNumberOfLiteralWords();
  }

  /** how many dirty words have we read so far? */
  public int dirtywordoffset = 0;
  
  /** The Number of literal words. */
  public int NumberOfLiteralWords;
  
  /** The Running bit. */
  public boolean RunningBit;
  
  /** The Running length. */
  public int RunningLength;

  
}