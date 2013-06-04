package com.googlecode.javaewah;

/**
 * @author lemire
 *
 */
public interface IteratingRLW {
	 /**
	 * @return whether there is more
	 */
	public boolean next() ;
	 /**
	 * @param index where the literal word is
	 * @return the literal word at the given index.
	 */
	public long getLiteralWordAt(int index);
	 /**
	 * @return the number of literal (non-fill) words
	 */
	public int getNumberOfLiteralWords() ;
	 /**
	 * @return the bit used for the fill bits
	 */
	public boolean getRunningBit() ;
	  /**
	 * @return sum of getRunningLength() and getNumberOfLiteralWords() 
	 */
	public long size() ;
	  /**
	 * @return length of the run of fill words
	 */
	public long getRunningLength() ;
	  /**
	 * @param x the number of words to discard
	 */
	public void discardFirstWords(long x);

    public IteratingRLW clone() throws CloneNotSupportedException;
}
