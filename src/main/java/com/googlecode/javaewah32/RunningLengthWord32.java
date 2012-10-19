package com.googlecode.javaewah32;


/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */

/**
 * Mostly for internal use.
 *
 * @since 0.5.0
 * @author Daniel Lemire
 */
public final class RunningLengthWord32 {

  /**
   * Instantiates a new running length word.
   *
   * @param a an array of 32-bit words
   * @param p position in the array where the running length word is located.
   */
  RunningLengthWord32(final int[] a, final int p) {
    this.array = a;
    this.position = p;
  }

  /**
   * Gets the number of literal words.
   *
   * @return the number of literal words
   */
  public int getNumberOfLiteralWords() {
    return (this.array[this.position] >>> (1 + runninglengthbits));
  }

  /**
   * Gets the running bit.
   *
   * @return the running bit
   */
  public boolean getRunningBit() {
    return (this.array[this.position] & 1) != 0;
  }

  /**
   * Gets the running length.
   *
   * @return the running length
   */
  public int getRunningLength() {
    return (this.array[this.position] >>> 1) & largestrunninglengthcount;
  }

  /**
   * Sets the number of literal words.
   *
   * @param number the new number of literal words
   */
  public void setNumberOfLiteralWords(final int number) {
    this.array[this.position] |= notrunninglengthplusrunningbit;
    this.array[this.position] &= (number << (runninglengthbits + 1))
      | runninglengthplusrunningbit;
  }

  /**
   * Sets the running bit.
   *
   * @param b the new running bit
   */
  public void setRunningBit(final boolean b) {
    if (b)
      this.array[this.position] |= 1;
    else
      this.array[this.position] &= ~1;
  }

  /**
   * Sets the running length.
   *
   * @param number the new running length
   */
  public void setRunningLength(final int number) {
    this.array[this.position] |= shiftedlargestrunninglengthcount;
    this.array[this.position] &= (number << 1)
      | notshiftedlargestrunninglengthcount;
  }

  /**
   * Return the size in uncompressed words represented by
   * this running length word.
   *
   * @return the int
   */
  public int size() {
    return getRunningLength() + getNumberOfLiteralWords();
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


  /** The array of words. */
  public int[] array;
  
  /** The position in array. */
  public int position;

  /** number of bits dedicated to marking  of the running length of clean words */  
  public static final int runninglengthbits = 16;
  
  private static final int literalbits = 32 - 1 - runninglengthbits;
  
  /** largest number of literal words in a run. */
  public static final int largestliteralcount = (1 << literalbits) - 1;
  
  /** largest number of clean words in a run */
  public static final int largestrunninglengthcount = (1 << runninglengthbits) - 1;
  
  private static final int runninglengthplusrunningbit = (1 << (runninglengthbits + 1)) - 1;
  
  private static final int shiftedlargestrunninglengthcount = largestrunninglengthcount << 1;
  
  private static final int notrunninglengthplusrunningbit = ~runninglengthplusrunningbit;
  
  private static final int notshiftedlargestrunninglengthcount = ~shiftedlargestrunninglengthcount;
  
}