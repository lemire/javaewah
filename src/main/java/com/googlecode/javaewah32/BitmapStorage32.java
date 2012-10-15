package com.googlecode.javaewah32;


/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */

/**
 * Low level bitset writing methods. 
 *
 * @since 0.5.0
 * @author Daniel Lemire and David McIntosh
 */
public interface BitmapStorage32 {

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * This is normally how you add data to the array. So you add bits in streams
   * of 8*8 bits.
   *
   * @param newdata the word
   * @return the number of words added to the buffer
   */  
  public void add(final int newdata);
  
  /**
   * if you have several dirty words to copy over, this might be faster.
   *
   * @param data the dirty words
   * @param start the starting point in the array
   * @param number the number of dirty words to add
   * @return how many (compressed) words were added to the bitmap
   */
  public void addStreamOfDirtyWords(final int[] data, final int start,
      final int number);
  
  /**
   * For experts: You want to add many
   * zeroes or ones? This is the method you use.
   *
   * @param v zeros or ones
   * @param number how many to words add
   * @return the number of words added to the buffer
   */
  public void addStreamOfEmptyWords(final boolean v, final int number);

  /**
   * Like "addStreamOfDirtyWords" but negates the words being added.
   *
   * @param data the dirty words
   * @param start the starting point in the array
   * @param number the number of dirty words to add
   * @return how many (compressed) words were added to the bitmap
   */
  public void addStreamOfNegatedDirtyWords( int[] data, final int start,
  final int number);
  /**
   * directly set the sizeinbits field
   * @param bits number of bits
   */
  public void setSizeInBits(final int bits);
}
