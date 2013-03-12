package com.googlecode.javaewah32;


/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */
/**
 * This is a BitmapStorage that can be used to determine quickly
 * if the result of an operation is non-trivial... that is, whether
 * there will be at least on set bit.
 * 
 * @since 0.5.0
 * @author Daniel Lemire
 *
 */
public class NonEmptyVirtualStorage32 implements BitmapStorage32 {
  static class NonEmptyException extends RuntimeException {
    private static final long serialVersionUID = 1L;    
  }
  /**
   * If the word to be added is non-zero, a NonEmptyException exception is thrown.
   * @see com.googlecode.javaewah.BitmapStorage#add(int)
   */
  public void add(int newdata) {
    if(newdata!=0) throw new NonEmptyException(); 
  }

  /**
   * throws a NonEmptyException exception when number > 0
   * 
   * @see com.googlecode.javaewah.BitmapStorage#addStreamOfLiteralWords(int[], int, int)
   */
  public void addStreamOfLiteralWords(int[] data, int start, int number) {
    if (number > 0){
      throw new NonEmptyException();
    }
  }

  /**
   * If the boolean value is true and number>0, then it throws a NonEmptyException exception,
   * otherwise, nothing happens.
   * 
   * @see com.googlecode.javaewah.BitmapStorage#addStreamOfEmptyWords(boolean, int)
   */
  public void addStreamOfEmptyWords(boolean v, int number) {
    if(v && (number>0)) throw new NonEmptyException(); 
  }

  /**
   * throws a NonEmptyException exception when number > 0
   * 
   * @see com.googlecode.javaewah.BitmapStorage#addStreamOfNegatedLiteralWords(int[], int, int)
   */
  public void addStreamOfNegatedLiteralWords(int[] data, int start, int number) {
    if (number > 0){
      throw new NonEmptyException();
    }
  }

  /**
   * Does nothing.
   * 
   * @see com.googlecode.javaewah.BitmapStorage#setSizeInBits(int)
   */
  public void setSizeInBits(int bits) {
  }

}
