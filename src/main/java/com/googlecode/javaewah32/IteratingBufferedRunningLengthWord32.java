package com.googlecode.javaewah32;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */
/**
 * Mostly for internal use. Similar to BufferedRunningLengthWord32, but automatically
 * advances to the next BufferedRunningLengthWord32 as words are discarded.
 *
 * @since 0.5.0
 * @author Daniel Lemire and David McIntosh
 */
public class IteratingBufferedRunningLengthWord32 {
  /**
   * Instantiates a new iterating buffered running length word.
   *
   * @param iterator iterator
   */
  public IteratingBufferedRunningLengthWord32(final EWAHIterator32 iterator) {
    this.iterator = iterator;
    this.brlw = new BufferedRunningLengthWord32(this.iterator.next());
    this.dirtyWordStartPosition = this.iterator.dirtyWords() + this.brlw.dirtywordoffset;
    this.buffer = this.iterator.buffer();
  }

  /**
   * Discard first words, iterating to the next running length word if needed.
   *
   * @param x the x
   */
  public void discardFirstWords(int x) {
    
    while (x > 0) {
      if (this.brlw.RunningLength >= x) {
        this.brlw.RunningLength -= x;
        return;
      }
      x -= this.brlw.RunningLength;
      this.brlw.RunningLength = 0;
      int toDiscard = x > this.brlw.NumberOfLiteralWords ? this.brlw.NumberOfLiteralWords : x;
    
      this.dirtyWordStartPosition += toDiscard;
      this.brlw.NumberOfLiteralWords -= toDiscard;
      x -= toDiscard;
      if (x > 0 || this.brlw.size() == 0) {
        if (!this.iterator.hasNext()) {
          break;
        }
        this.brlw.reset(this.iterator.next());
        this.dirtyWordStartPosition = this.iterator.dirtyWords() + this.brlw.dirtywordoffset;
      }
    }
  }

  /**
   * Write out the remaining words
   * @param container target for writes
   */
  public void discharge(BitmapStorage32 container) {
    // fix the offset
    this.brlw.dirtywordoffset = this.dirtyWordStartPosition - this.iterator.dirtyWords();
    EWAHCompressedBitmap32.discharge(this.brlw, this.iterator, container);
  }

  /**
   * Get the nth dirty word for the current running length word 
   * @param index zero based index
   * @return the dirty word
   */
  public int getDirtyWordAt(int index) {
    return this.buffer[this.dirtyWordStartPosition + index];
  }

  /**
   * Gets the number of literal words for the current running length word.
   *
   * @return the number of literal words
   */
  public int getNumberOfLiteralWords() {
    return this.brlw.NumberOfLiteralWords;
  }

  /**
   * Gets the running bit.
   *
   * @return the running bit
   */
  public boolean getRunningBit() {
    return this.brlw.RunningBit;
  }
  
  /**
   * Gets the running length.
   *
   * @return the running length
   */
  public int getRunningLength() {
    return this.brlw.RunningLength;
  }
  
  /**
   * Size in uncompressed words of the current running length word.
   *
   * @return the int
   */
  public int size() {
    return this.brlw.size();
  }
  
  /**
   * write the first N dirty words to the target bitmap.  Does not discard the words or perform iteration.
   * @param numWords
   * @param container
   */
  public void writeDirtyWords(int numWords, BitmapStorage32 container) {
    container.addStreamOfDirtyWords(this.buffer, this.dirtyWordStartPosition, numWords);
  }
  
  private BufferedRunningLengthWord32 brlw;
  private int[] buffer;
  private int dirtyWordStartPosition;
  private EWAHIterator32 iterator;
}
