package javaewah;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon and David McIntosh
 * Licensed under APL 2.0.
 */

/**
 * 
 * The IntIterator interface is used to  iterate over a stream of integers.
 * 
 * @author lemire
 *
 */
public interface IntIterator {
  
  /**
   * Return the next integer
   *
   * @return the integer
   */
  public int next();

  /**
   * Is there more?
   *
   * @return true, if successful
   */
  public boolean hasNext();
}
