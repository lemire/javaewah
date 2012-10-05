package javaewah32;

/*
 * Copyright 2012, Google Inc.
 * Licensed under APL 2.0.
 */

import static javaewah32.EWAHCompressedBitmap32.wordinbits;

import javaewah.IntIterator;

/**
 * The IntIteratorImpl32 implements the 32 bit implementation of the
 * IntIterator interface, which efficiently returns the stream of integers
 * represented by an EWAHIterator32.
 *
 * @author Colby Ranger
 * @since 0.5.6
 */
final class IntIteratorImpl32 implements IntIterator {

  private final EWAHIterator32 ewahIter;
  private final int[] ewahBuffer;

  private int position;
  private int runningLength;
  private int word;
  private int wordPosition;
  private int wordLength;
  private int literalPosition;

  IntIteratorImpl32(EWAHIterator32 ewahIter) {
    this.ewahIter = ewahIter;
    this.ewahBuffer = ewahIter.buffer();
  }

  public final boolean hasNext() {
    while (!runningHasNext() && !literalHasNext()) {
      if (!ewahIter.hasNext()) {
        return false;
      }

      setRunningLengthWord(ewahIter.next());
    }
    return true;
  }

  public final int next() {
    if (runningHasNext()) {
      return position++;
    }

    int bit = Integer.numberOfTrailingZeros(word);
    word ^= (1l << bit);
    return literalPosition + bit;
  }

  private final void setRunningLengthWord(RunningLengthWord32 rlw) {
    runningLength = wordinbits * rlw.getRunningLength() + position;
    if (!rlw.getRunningBit()) {
      position = runningLength;
    }

    wordPosition = ewahIter.dirtyWords();
    wordLength = wordPosition + rlw.getNumberOfLiteralWords();
  }

  private final boolean runningHasNext() {
    return position < runningLength;
  }

  private final boolean literalHasNext() {
    while (word == 0 && wordPosition < wordLength) {
      word = ewahBuffer[wordPosition++];
      literalPosition = position;
      position += wordinbits;
    }
    return word != 0;
  }
}
