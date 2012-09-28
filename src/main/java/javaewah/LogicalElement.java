package javaewah;

public interface LogicalElement<T> {
  public T and(T le);
  public T andNot(T le);
  public void not();
  public LogicalElement or(T le);
  public int sizeInBits();

  public T xor(T le);

}
