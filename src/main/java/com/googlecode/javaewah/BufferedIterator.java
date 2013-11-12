package com.googlecode.javaewah;


/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * This class can be used to iterate over blocks of bitmap data.
 * 
 * @author Daniel Lemire
 *
 */
public class BufferedIterator implements IteratingRLW  {
	  /**
	   * Instantiates a new iterating buffered running length word.
	   *
	   * @param iterator iterator
	   */
	  public BufferedIterator(final CloneableIterator<EWAHIterator> iterator) {
		this.masteriterator = iterator;
		if(this.masteriterator.hasNext()) {
			this.iterator = this.masteriterator.next();
			this.brlw = new BufferedRunningLengthWord(this.iterator.next());
			this.literalWordStartPosition = this.iterator.literalWords() + this.brlw.literalwordoffset;
			this.buffer = this.iterator.buffer();
		}
	  }
	  
	  
	  /**
	   * Discard first words, iterating to the next running length word if needed.
	   *
	   * @param x the number of words to be discarded
	   */
	  @Override
	public void discardFirstWords(long x) {
	    while (x > 0) {
	      if (this.brlw.RunningLength > x) {
	        this.brlw.RunningLength -= x;
	        return;
	      }
	      x -= this.brlw.RunningLength;
	      this.brlw.RunningLength = 0;
	      long toDiscard = x > this.brlw.NumberOfLiteralWords ? this.brlw.NumberOfLiteralWords : x;
	    
	      this.literalWordStartPosition += toDiscard;
	      this.brlw.NumberOfLiteralWords -= toDiscard;
	      x -= toDiscard;
	      if ((x > 0) || (this.brlw.size() == 0)) {
	        if (!this.next()) {
	          break;
	        }
	      }
	    }
	  }
	  /**
	   * Move to the next RunningLengthWord
	   * @return whether the move was possible
	   */
	  @Override
	public boolean next() {
		  if (!this.iterator.hasNext()) {
			  if(!reload()) {
				  this.brlw.NumberOfLiteralWords = 0;
				  this.brlw.RunningLength = 0;
				  return false;
			  }
		  }  
		  this.brlw.reset(this.iterator.next());
	      this.literalWordStartPosition = this.iterator.literalWords(); //  + this.brlw.literalwordoffset ==0
	      return true;
	  }
	  private boolean reload() {
		  if(!this.masteriterator.hasNext()) {
			  return false;
		  }
		  this.iterator = this.masteriterator.next();
		  this.buffer = this.iterator.buffer();
		  return true;
	  }


	  /**
	   * Get the nth literal word for the current running length word 
	   * @param index zero based index
	   * @return the literal word
	   */
	  @Override
	public long getLiteralWordAt(int index) {
	    return this.buffer[this.literalWordStartPosition + index];
	  }

	  /**
	   * Gets the number of literal words for the current running length word.
	   *
	   * @return the number of literal words
	   */
	  @Override
	public int getNumberOfLiteralWords() {
	    return this.brlw.NumberOfLiteralWords;
	  }

	  /**
	   * Gets the running bit.
	   *
	   * @return the running bit
	   */
	  @Override
	public boolean getRunningBit() {
	    return this.brlw.RunningBit;
	  }
	  
	  /**
	   * Gets the running length.
	   *
	   * @return the running length
	   */
	  @Override
	public long getRunningLength() {
	    return this.brlw.RunningLength;
	  }
	  
	  /**
	   * Size in uncompressed words of the current running length word.
	   *
	   * @return the size
	   */
	  @Override
	public long size() {
	    return this.brlw.size();
	  }


	  @Override
	public BufferedIterator clone() throws CloneNotSupportedException {
		  BufferedIterator answer = (BufferedIterator) super.clone();
		  answer.brlw = this.brlw.clone();
		  answer.buffer = this.buffer;
		  answer.iterator = this.iterator.clone();
		  answer.literalWordStartPosition = this.literalWordStartPosition;
		  answer.masteriterator = this.masteriterator.clone();
		  return answer;
	  }

	  private BufferedRunningLengthWord brlw;
	  private long[] buffer;
	  private int literalWordStartPosition;
	  private EWAHIterator iterator;
	  private CloneableIterator<EWAHIterator> masteriterator;
	}