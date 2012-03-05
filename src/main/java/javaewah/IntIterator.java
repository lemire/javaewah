package javaewah;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */

/**
 * 
 * The IntIterator interface is used to  iterate over a stream of integers.
 * 
 * @author Daniel Lemire
 * @since 0.2.0
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
   * @return true, if there is more, false otherwise
   */
  public boolean hasNext();
}
