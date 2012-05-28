package javaewah32;


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
  class NonEmptyException extends RuntimeException {
    private static final long serialVersionUID = 1L;    
  }
  /**
   * If the word to be added is non-zero, a NonEmptyException exception is thrown.
   * @see javaewah.BitmapStorage#add(int)
   */
  public void add(int newdata) {
    if(newdata!=0) throw new NonEmptyException(); 
  }

  /**
   * If the boolean value is true, then it throws a NonEmptyException exception,
   * otherwise, nothing happens.
   * 
   * @see javaewah.BitmapStorage#addStreamOfEmptyWords(boolean, int)
   */
  public void addStreamOfEmptyWords(boolean v, int number) {
    if(v) throw new NonEmptyException(); 
  }

  /**
   * throws a NonEmptyException exception
   * 
   * @see javaewah.BitmapStorage#addStreamOfDirtyWords(int[], int, int)
   */
  public void addStreamOfDirtyWords(int[] data, int start, int number) {
    throw new NonEmptyException();
  }

  /**
   * throws a NonEmptyException exception
   * 
   * @see javaewah.BitmapStorage#addStreamOfNegatedDirtyWords(int[], int, int)
   */
  public void addStreamOfNegatedDirtyWords(int[] data, int start, int number) {
    throw new NonEmptyException();
  }

  /**
   * Does nothing.
   * 
   * @see javaewah.BitmapStorage#setSizeInBits(int)
   */
  public void setSizeInBits(int bits) {
  }

}
