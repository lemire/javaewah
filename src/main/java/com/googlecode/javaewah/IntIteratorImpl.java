package com.googlecode.javaewah;

/*
 * Copyright 2012, Google Inc.
 * Licensed under the Apache License, Version 2.0.
 */

import static com.googlecode.javaewah.EWAHCompressedBitmap.wordinbits;

/**
 * The IntIteratorImpl is the 64 bit implementation of the
 * IntIterator interface, which efficiently returns the stream of integers
 * represented by an EWAHIterator.
 *
 * @author Colby Ranger
 * @since 0.5.6
 */
final class IntIteratorImpl implements IntIterator {

  private final EWAHIterator ewahIter;
  private final long[] ewahBuffer;
  private int position;
  private int runningLength;
  private long word;
  private int wordPosition;
  private int wordLength;
  private int literalPosition;
  private boolean hasnext;

  IntIteratorImpl(EWAHIterator ewahIter) {
    this.ewahIter = ewahIter;
    this.ewahBuffer = ewahIter.buffer();
    this.hasnext = this.moveToNext();
  }

  public final boolean moveToNext() {
    while (!runningHasNext() && !literalHasNext()) {
      if (!this.ewahIter.hasNext()) {
        return false;
      }
      setRunningLengthWord(this.ewahIter.next());
    }
    return true;
  }
  
  @Override
public boolean hasNext() {
	  return this.hasnext;
  }

  @Override
public final int next() {
	final int answer;
    if (runningHasNext()) {
    	answer = this.position++;
    } else {
    	final int bit = Long.numberOfTrailingZeros(this.word);
    	this.word ^= (1l << bit);
    	answer =  this.literalPosition + bit;
    }
    this.hasnext = this.moveToNext();
    return answer;
  }

  private final void setRunningLengthWord(RunningLengthWord rlw) {
    this.runningLength = wordinbits * (int) rlw.getRunningLength() + this.position;
    if (!rlw.getRunningBit()) {
      this.position = this.runningLength;
    }

    this.wordPosition = this.ewahIter.literalWords();
    this.wordLength = this.wordPosition + rlw.getNumberOfLiteralWords();
  }

  private final boolean runningHasNext() {
    return this.position < this.runningLength;
  }

  private final boolean literalHasNext() {
    while (this.word == 0 && this.wordPosition < this.wordLength) {
      this.word = this.ewahBuffer[this.wordPosition++];
      this.literalPosition = this.position;
      this.position += wordinbits;
    }
    return this.word != 0;
  }
}
