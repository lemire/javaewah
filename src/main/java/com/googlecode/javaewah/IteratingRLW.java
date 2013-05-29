package com.googlecode.javaewah;

public interface IteratingRLW {
	 public boolean next() ;
	 public long getLiteralWordAt(int index);
	 public int getNumberOfLiteralWords() ;
	 public boolean getRunningBit() ;
	  public long size() ;
	  public long getRunningLength() ;
	  public void discardFirstWords(long x);
}
