package com.googlecode.javaewah;

/**
 * A prototypical model for bitmaps. Used by the
 * class FastAggregation. Users should probably not
 * be concerned by this class.
 * 
 * @author Daniel Lemire
 *
 */
public interface LogicalElement<T> {
  /**
   * Compute the bitwise logical and
   * @param another element
   * @return the result of the operation
   */
  public T and(T le);
  
  /**
   * Compute the bitwise logical and not
   * @param another element
   * @return the result of the operation
   */
  public T andNot(T le);

  /**
   * Compute the bitwise logical not (in place)
   */
  public void not();
  @SuppressWarnings("rawtypes")
  /**
   * Compute the bitwise logical or
   * @param another element
   * @return the result of the operation
   */
  public LogicalElement or(T le);
  
  /**
   * How many logical bits does this element represent?
   * 
   * @param another element
   * @return the number of bits represented by this element
   */
  public int sizeInBits();
  
  /**
   * Should report the storage requirement      
   * @return How many bytes
   * @since 0.6.2
   */
  public int sizeInBytes();
  
  /**
   * Compute the bitwise logical Xor
   * @param another element
   * @return the results of the operation
   */
  public T xor(T le);
}
