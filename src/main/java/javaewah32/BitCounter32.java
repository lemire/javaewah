package javaewah32;



/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */
/**
 * BitCounter is a fake bitset data structure. Instead of storing the actual data,
 * it only records the number of set bits.
 *
 * @since 0.5.0
 * @author Daniel Lemire and David McIntosh
 */

public final class BitCounter32 implements BitmapStorage32 {

  private int oneBits;
  
  
  /**
   * Virtually add words directly to the bitmap
   *
   * @param newdata the word
   */  
  // @Override : causes problems with Java 1.5
  public void add(final int newdata) {
    this.oneBits += Integer.bitCount(newdata);
  }


  /**
   * virtually add many 
   * zeroes or ones.
   *
   * @param v zeros or ones
   * @param number how many to words add
   */
  // @Override : causes problems with Java 1.5
  public void addStreamOfEmptyWords(boolean v, int number) {
    if (v) {
      this.oneBits += number * EWAHCompressedBitmap32.wordinbits;
    }
  }

  /**
   * virtually add several  dirty words.
   *
   * @param data the dirty words
   * @param start the starting point in the array
   * @param number the number of dirty words to add
   */
  // @Override : causes problems with Java 1.5
  public void addStreamOfDirtyWords(int[] data, int start, int number) {
    for(int i=start;i<start+number;i++) {
      add(data[i]);      
    }
  }
  
  /**
   * virtually add several negated dirty words.
   *
   * @param data the dirty words
   * @param start the starting point in the array
   * @param number the number of dirty words to add
   */
  // @Override : causes problems with Java 1.5
  public void addStreamOfNegatedDirtyWords(int[] data, int start,
    int number) {
    for(int i=start;i<start+number;i++) {
      add(~data[i]);      
    }
  }  
  
  /**
   * should directly set the sizeinbits field, but is effectively ignored in this class.
   *
   * @param bits number of bits
   */
  // @Override : causes problems with Java 1.5
  public void setSizeInBits(int bits) {
    // no action
  }

  /**
   * As you act on this class, it records the number of set (true) bits. 
   *
   * @return number of set bits
   */
  public int getCount() {
    return this.oneBits;
  }


  
}
