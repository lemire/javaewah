package com.googlecode.javaewah32;

/**
 * @author lemire
 *
 */
public interface IteratingRLW32 {
	 /**
	 * @return whether there is more
	 */
	public boolean next() ;
	 /**
	 * @param index where the literal word is
	 * @return the literal word at the given index.
	 */
	public int getLiteralWordAt(int index);
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
	public int size() ;
	  /**
	 * @return length of the run of fill words
	 */
	public int getRunningLength() ;
	  /**
	 * @param x the number of words to discard
	 */
	public void discardFirstWords(int x);
}
