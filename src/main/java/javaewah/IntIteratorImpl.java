package javaewah;

/*
 * Copyright 2012, Google Inc.
 * Licensed under APL 2.0.
 */

import static javaewah.EWAHCompressedBitmap.wordinbits;

/**
 * The IntIteratorImpl implements the 64 bit implementation of the
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

  IntIteratorImpl(EWAHIterator ewahIter) {
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

    int bit = Long.numberOfTrailingZeros(word);
    word ^= (1l << bit);
    return literalPosition + bit;
  }

  private final void setRunningLengthWord(RunningLengthWord rlw) {
    runningLength = wordinbits * (int) rlw.getRunningLength() + position;
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
