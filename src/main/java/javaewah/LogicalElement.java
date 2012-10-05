package javaewah;

/**
 * A prototypical model for bitmaps. Used by the
 * class FastAggregation. Users should probably not
 * be concerned by this class.
 * 
 * @author Daniel Lemire
 *
 */
public interface LogicalElement<T> {
  public T and(T le);
  public T andNot(T le);
  public void not();
  public LogicalElement or(T le);
  public int sizeInBits();
  public T xor(T le);
}
