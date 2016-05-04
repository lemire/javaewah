package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Mostly for internal use. Similar to BufferedRunningLengthWord32, but
 * automatically advances to the next BufferedRunningLengthWord32 as words are
 * discarded.
 *
 * @author Daniel Lemire and David McIntosh
 * @since 0.5.0
 */
public final class IteratingBufferedRunningLengthWord32 implements
        IteratingRLW32, Cloneable {
    /**
     * Instantiates a new iterating buffered running length word.
     *
     * @param iterator iterator
     */
    public IteratingBufferedRunningLengthWord32(
            final EWAHIterator32 iterator) {
        this.iterator = iterator;
        this.brlw = new BufferedRunningLengthWord32(
                this.iterator.next());
        this.literalWordStartPosition = this.iterator.literalWords()
                + this.brlw.literalWordOffset;
        this.buffer = this.iterator.buffer();
    }

    /**
     * Instantiates a new iterating buffered running length word.
     *
     * @param bitmap over which we want to iterate
     */
    public IteratingBufferedRunningLengthWord32(
            final EWAHCompressedBitmap32 bitmap) {
        this(EWAHIterator32.getEWAHIterator(bitmap));
    }
    /**
     * Discard first words, iterating to the next running length word if
     * needed.
     *
     * @param x the x
     */
    @Override
    public void discardFirstWords(int x) {
        while (x > 0) {
            if (this.brlw.RunningLength > x) {
                this.brlw.RunningLength -= x;
                return;
            }
            x -= this.brlw.RunningLength;
            this.brlw.RunningLength = 0;
            int toDiscard = x > this.brlw.NumberOfLiteralWords ? this.brlw.NumberOfLiteralWords
                    : x;

            this.literalWordStartPosition += toDiscard;
            this.brlw.NumberOfLiteralWords -= toDiscard;
            x -= toDiscard;
            if ((x > 0) || (this.brlw.size() == 0)) {
                if (!this.iterator.hasNext()) {
                    break;
                }
                this.brlw.reset(this.iterator.next());
                this.literalWordStartPosition = this.iterator
                        .literalWords();
            }
        }
    }
    
    @Override
  public void discardLiteralWords(int x) {
      this.literalWordStartPosition += x;
    this.brlw.NumberOfLiteralWords -= x;
    if (this.brlw.NumberOfLiteralWords == 0) {
      if (!this.iterator.hasNext()) {
        return;
      }
      this.brlw.reset(this.iterator.next());
      this.literalWordStartPosition = this.iterator.literalWords();
    }    
  }


    @Override
    public void discardRunningWords() {
        this.brlw.RunningLength = 0;
        if (this.brlw.getNumberOfLiteralWords() == 0)
            this.next();
    }

    /**
     * Write out up to max words, returns how many were written
     *
     * @param container target for writes
     * @param max       maximal number of writes
     * @return how many written
     */
    public int discharge(BitmapStorage32 container, int max) {
      int index = 0;
      while (true) {
        if (index + getRunningLength() > max) {
          final int offset = max - index;
          container.addStreamOfEmptyWords(getRunningBit(), offset);
          this.brlw.RunningLength -= offset;
          return max;
        }
        container.addStreamOfEmptyWords(getRunningBit(), getRunningLength());
        index += getRunningLength();
        if (getNumberOfLiteralWords() + index > max) {
          final int offset = max - index;
          writeLiteralWords(offset, container);
          this.brlw.RunningLength = 0;
          this.brlw.NumberOfLiteralWords -= offset;
          this.literalWordStartPosition += offset;
          return max;
        }
        writeLiteralWords(getNumberOfLiteralWords(), container);
        index += getNumberOfLiteralWords();
        if(!next()) break;
      }
      return index;
    }


    /**
     * Write out up to max words (negated), returns how many were written
     *
     * @param container target for writes
     * @param max       maximal number of writes
     * @return how many written
     */
    public int dischargeNegated(BitmapStorage32 container, int max) {
        int index = 0;
        while ((index < max) && (size() > 0)) {
            // first run
            int pl = getRunningLength();
            if (index + pl > max) {
                pl = max - index;
            }
            container.addStreamOfEmptyWords(!getRunningBit(), pl);
            index += pl;
            int pd = getNumberOfLiteralWords();
            if (pd + index > max) {
                pd = max - index;
            }
            writeNegatedLiteralWords(pd, container);
            discardFirstWords(pl + pd);
            index += pd;
        }
        return index;
    }

    /**
     * Move to the next RunningLengthWord
     *
     * @return whether the move was possible
     */
    @Override
    public boolean next() {
        if (!this.iterator.hasNext()) {
            this.brlw.NumberOfLiteralWords = 0;
            this.brlw.RunningLength = 0;
            return false;
        }
        this.brlw.reset(this.iterator.next());
        this.literalWordStartPosition = this.iterator.literalWords(); // +
        // this.brlw.literalWordOffset
        // ==0
        return true;
    }

    /**
     * Write out the remain words, transforming them to zeroes.
     *
     * @param container target for writes
     */
    public void dischargeAsEmpty(BitmapStorage32 container) {
        while (size() > 0) {
            container.addStreamOfEmptyWords(false, size());
            discardFirstWords(size());
        }
    }

    /**
     * Write out the remaining words
     *
     * @param container target for writes
     */
    public void discharge(BitmapStorage32 container) {
        // fix the offset
        this.brlw.literalWordOffset = this.literalWordStartPosition
                - this.iterator.literalWords();
        discharge(this.brlw, this.iterator, container);
    }

    /**
     * Get the nth literal word for the current running length word
     *
     * @param index zero based index
     * @return the literal word
     */
    @Override
    public int getLiteralWordAt(int index) {
        return this.buffer.getWord(this.literalWordStartPosition + index);
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
    public int getRunningLength() {
        return this.brlw.RunningLength;
    }

    /**
     * Size in uncompressed words of the current running length word.
     *
     * @return the int
     */
    @Override
    public int size() {
        return this.brlw.size();
    }

    /**
     * write the first N literal words to the target bitmap. Does not
     * discard the words or perform iteration.
     *
     * @param numWords  number of words to be written
     * @param container where we write the data
     */
    public void writeLiteralWords(int numWords, BitmapStorage32 container) {
        container.addStreamOfLiteralWords(this.buffer,
                this.literalWordStartPosition, numWords);
    }

    /**
     * write the first N literal words (negated) to the target bitmap. Does
     * not discard the words or perform iteration.
     *
     * @param numWords  number of words to be written
     * @param container where we write the data
     */
    public void writeNegatedLiteralWords(int numWords,
                                         BitmapStorage32 container) {
        container.addStreamOfNegatedLiteralWords(this.buffer,
                this.literalWordStartPosition, numWords);
    }

    /**
     * For internal use. (One could use the non-static discharge method
     * instead, but we expect them to be slower.)
     *
     * @param initialWord the initial word
     * @param iterator    the iterator
     * @param container   the container
     */
    protected static void discharge(
            final BufferedRunningLengthWord32 initialWord,
            final EWAHIterator32 iterator, final BitmapStorage32 container) {
        BufferedRunningLengthWord32 runningLengthWord = initialWord;
        for (; ; ) {
            final int runningLength = runningLengthWord
                    .getRunningLength();
            container.addStreamOfEmptyWords(
                    runningLengthWord.getRunningBit(),
                    runningLength);
            container.addStreamOfLiteralWords(iterator.buffer(),
                    iterator.literalWords()
                            + runningLengthWord.literalWordOffset,
                    runningLengthWord.getNumberOfLiteralWords()
            );
            if (!iterator.hasNext())
                break;
            runningLengthWord = new BufferedRunningLengthWord32(
                    iterator.next());
        }
    }

    @Override
    public IteratingBufferedRunningLengthWord32 clone()
            throws CloneNotSupportedException {
        IteratingBufferedRunningLengthWord32 answer = (IteratingBufferedRunningLengthWord32) super
                .clone();
        answer.brlw = this.brlw.clone();
        answer.iterator = this.iterator.clone();
        return answer;
    }

    private BufferedRunningLengthWord32 brlw;
    private final Buffer32 buffer;
    private int literalWordStartPosition;
    private EWAHIterator32 iterator;
  

}
