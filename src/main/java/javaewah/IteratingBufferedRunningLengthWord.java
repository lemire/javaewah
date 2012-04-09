package javaewah;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh and Robert Becho
 * Licensed under APL 2.0.
 */
/**
 * Mostly for internal use. Similar to BufferedRunningLengthWord, but automatically
 * advances to the next BufferedRunningLengthWord as words are discarded.
 *
 * @since 0.4.0
 * @author David McIntosh
 */
public class IteratingBufferedRunningLengthWord {
  /**
   * Instantiates a new iterating buffered running length word.
   *
   * @param iterator iterator
   */
  public IteratingBufferedRunningLengthWord(final EWAHIterator iterator) {
    this.iterator = iterator;
    this.brlw = new BufferedRunningLengthWord(this.iterator.next());
    this.dirtyWordStartPosition = this.iterator.dirtyWords() + this.brlw.dirtywordoffset;
    this.buffer = this.iterator.buffer();
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
  public long getRunningLength() {
    return this.brlw.RunningLength;
  }

  /**
   * Size in uncompressed words of the current running length word.
   *
   * @return the long
   */
  public long size() {
    return this.brlw.size();
  }

  /**
   * Get the nth dirty word for the current running length word 
   * @param index zero based index
   * @return the dirty word
   */
  public long getDirtyWordAt(int index) {
    return this.buffer[this.dirtyWordStartPosition + index];
  }
  
  /**
   * write the first N dirty words to the target bitmap.  Does not discard the words or perform iteration.
   * @param numWords
   * @param container
   */
  public void writeDirtyWords(int numWords, BitmapStorage container) {
    container.addStreamOfDirtyWords(this.buffer, this.dirtyWordStartPosition, numWords);
  }
  
  /**
   * Discard first words, iterating to the next running length word if needed.
   *
   * @param x the x
   */
  public void discardFirstWords(long x) {
    
    while (x > 0) {
      if (this.brlw.RunningLength >= x) {
        this.brlw.RunningLength -= x;
        return;
      }
      x -= this.brlw.RunningLength;
      this.brlw.RunningLength = 0;
      long toDiscard = x > this.brlw.NumberOfLiteralWords ? this.brlw.NumberOfLiteralWords : x;
    
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
  public void discharge(BitmapStorage container) {
    // fix the offset
    this.brlw.dirtywordoffset = this.dirtyWordStartPosition - this.iterator.dirtyWords();
    EWAHCompressedBitmap.discharge(this.brlw, this.iterator, container);
  }
  
  private long[] buffer;
  private int dirtyWordStartPosition;
  private BufferedRunningLengthWord brlw;
  private EWAHIterator iterator;
}
