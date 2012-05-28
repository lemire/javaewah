package javaewah;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */

/**
 * Low level bitset writing methods.
 * 
 * @since 0.4.0
 * @author David McIntosh
 */
public interface BitmapStorage {

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * This is normally how you add data to the array. So you add bits in streams
   * of 8*8 bits.
   * 
   * @param newdata
   *          the word
   */
  public void add(final long newdata);

  /**
   * For experts: You want to add many zeroes or ones? This is the method you
   * use.
   * 
   * @param v
   *          zeros or ones
   * @param number
   *          how many to words add
   */
  public void addStreamOfEmptyWords(final boolean v, final long number);

  /**
   * if you have several dirty words to copy over, this might be faster.
   * 
   * @param data
   *          the dirty words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of dirty words to add
   */
  public void addStreamOfDirtyWords(final long[] data, final int start,
    final int number);

  /**
   * Like "addStreamOfDirtyWords" but negates the words being added.
   * 
   * @param data
   *          the dirty words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of dirty words to add
   */
  public void addStreamOfNegatedDirtyWords(long[] data, final int start,
    final int number);

  /**
   * directly set the sizeinbits field
   * 
   * @param bits
   *          number of bits
   */
  public void setSizeInBits(final int bits);
}
