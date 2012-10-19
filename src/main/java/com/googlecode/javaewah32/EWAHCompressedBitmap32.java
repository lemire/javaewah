package com.googlecode.javaewah32;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, and Google Inc.
 * Licensed under APL 2.0.
 */

import java.util.*;
import java.io.*;

import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah.LogicalElement;


/**
 * <p>
 * This implements the patent-free EWAH scheme. Roughly speaking, it is a 32-bit
 * variant of the BBC compression scheme used by Oracle for its bitmap indexes.
 * </p>
 * 
 * <p>
 * In contrast witht the 64-bit EWAH scheme (javaewah.EWAHCompressedBitmap), you
 * can expect this class to compress better, but to be slower at processing the
 * data. In effect, there is a trade-off between memory usage and performances.
 * </p>
 * 
 * <p>
 * In contrast with the 64-bit EWAH scheme (javaewah.EWAHCompressedBitmap), you
 * can expect this class to compress better, but to be slower at processing the
 * data. In effect, there is a trade-off between memory usage and performances.
 * </p>
 * 
 * @see com.googlecode.javaewah.EWAHCompressedBitmap
 * 
 *      <p>
 *      The objective of this compression type is to provide some compression,
 *      while reducing as much as possible the CPU cycle usage.
 *      </p>
 * 
 * 
 *      <p>
 *      For more details, see the following paper:
 *      </p>
 * 
 *      <ul>
 *      <li>Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves
 *      word-aligned bitmap indexes. Data & Knowledge Engineering 69 (1), pages
 *      3-28, 2010. http://arxiv.org/abs/0901.3751</li>
 *      </ul>
 * 
 * @since 0.5.0
 */
public final class EWAHCompressedBitmap32 implements Cloneable, Externalizable,
  Iterable<Integer>, BitmapStorage32, LogicalElement<EWAHCompressedBitmap32> {

  /**
   * Creates an empty bitmap (no bit set to true).
   */
  public EWAHCompressedBitmap32() {
    this.buffer = new int[defaultbuffersize];
    this.rlw = new RunningLengthWord32(this.buffer, 0);
  }

  /**
   * Sets explicitly the buffer size (in 32-bit words). The initial memory usage
   * will be "buffersize * 32". For large poorly compressible bitmaps, using
   * large values may improve performance.
   * 
   * @param buffersize
   *          number of 32-bit words reserved when the object is created)
   */
  public EWAHCompressedBitmap32(final int buffersize) {
    this.buffer = new int[buffersize];
    this.rlw = new RunningLengthWord32(this.buffer, 0);
  }

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * This is normally how you add data to the array. So you add bits in streams
   * of 8*8 bits.
   * 
   * @param newdata
   *          the word
   */
  public void add(final int newdata) {
    add(newdata, wordinbits);
  }

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * @param newdata
   *          the word
   * @param bitsthatmatter
   *          the number of significant bits (by default it should be 32)
   */
  public void add(final int newdata, final int bitsthatmatter) {
    this.sizeinbits += bitsthatmatter;
    if (newdata == 0) {
      addEmptyWord(false);
    } else if (newdata == ~0) {
      addEmptyWord(true);
    } else {
      addLiteralWord(newdata);
    }
  }

  /**
   * For internal use.
   * 
   * @param v
   *          the boolean value
   * @return the storage cost of the addition
   */
  private int addEmptyWord(final boolean v) {
    final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
    final int runlen = this.rlw.getRunningLength();
    if ((noliteralword) && (runlen == 0)) {
      this.rlw.setRunningBit(v);
    }
    if ((noliteralword) && (this.rlw.getRunningBit() == v)
      && (runlen < RunningLengthWord32.largestrunninglengthcount)) {
      this.rlw.setRunningLength(runlen + 1);
      return 0;
    }
    push_back(0);
    this.rlw.position = this.actualsizeinwords - 1;
    this.rlw.setRunningBit(v);
    this.rlw.setRunningLength(1);
    return 1;
  }

  /**
   * For internal use.
   * 
   * @param newdata
   *          the literal word
   * @return the storage cost of the addition
   */
  private int addLiteralWord(final int newdata) {
    final int numbersofar = this.rlw.getNumberOfLiteralWords();
    if (numbersofar >= RunningLengthWord32.largestliteralcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      this.rlw.setNumberOfLiteralWords(1);
      push_back(newdata);
      return 2;
    }
    this.rlw.setNumberOfLiteralWords(numbersofar + 1);
    push_back(newdata);
    return 1;
  }

  /**
   * if you have several literal words to copy over, this might be faster.
   * 
   * 
   * @param data
   *          the literal words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of literal words to add
   */
  public void addStreamOfLiteralWords(final int[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord32.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord32.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfLiteralWords(data, start + whatwecanadd, leftovernumber);
    }
  }

  /**
   * For experts: You want to add many zeroes or ones? This is the method you
   * use.
   * 
   * @param v
   *          the boolean value
   * @param number
   *          the number
   */
  public void addStreamOfEmptyWords(final boolean v, int number) {
    if (number == 0)
      return;
    this.sizeinbits += number * wordinbits;
    if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
      this.rlw.setRunningBit(v);
    } else if ((this.rlw.getNumberOfLiteralWords() != 0)
      || (this.rlw.getRunningBit() != v)) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
    }
    final int runlen = this.rlw.getRunningLength();
    final int whatwecanadd = number < RunningLengthWord32.largestrunninglengthcount
      - runlen ? number : RunningLengthWord32.largestrunninglengthcount
      - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;
    while (number >= RunningLengthWord32.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord32.largestrunninglengthcount);
      number -= RunningLengthWord32.largestrunninglengthcount;
    }
    if (number > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(number);
    }
  }

  /**
   * Same as addStreamOfLiteralWords, but the words are negated.
   * 
   * @param data
   *          the literal words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of literal words to add
   */
  public void addStreamOfNegatedLiteralWords(final int[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord32.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord32.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    negative_push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfLiteralWords(data, start + whatwecanadd, leftovernumber);
    }
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public EWAHCompressedBitmap32 and(final EWAHCompressedBitmap32 a) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    container
      .reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
        : a.actualsizeinwords);
    and(a, container);
    return container;
  }

  /**
   * Computes new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  /**
   * Computes new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void and(final EWAHCompressedBitmap32 a, final BitmapStorage32 container) {
    final EWAHIterator32 i = a.getEWAHIterator();
    final EWAHIterator32 j = getEWAHIterator();
    IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(i);
    IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(j);
    while ((rlwi.size()>0) && (rlwj.size()>0)) {
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
          .getRunningLength();
        final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
          : rlwj;
        final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
          : rlwi;
        if (predator.getRunningBit() == false) {
          container.addStreamOfEmptyWords(false, predator.getRunningLength());
          prey.discardFirstWords(predator.getRunningLength());
          predator.discardFirstWords(predator.getRunningLength());
        } else {
          int index = 0;
          while ((index < predator.getRunningLength()) && (prey.size() > 0)) {
            int pl = prey.getRunningLength();
            if (index + pl > predator.getRunningLength()) {
              pl = predator.getRunningLength() - index;
            }
            container.addStreamOfEmptyWords(prey.getRunningBit(), pl);
            prey.discardFirstWords(pl);
            index += pl;
            int pd = prey.getNumberOfLiteralWords();
            if (pd + index > predator.getRunningLength()) {
              pd = predator.getRunningLength() - index;
            }
            prey.writeLiteralWords(pd, container);
            prey.discardFirstWords(pd);
            index += pd;
          }
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k)
          container.add(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }
    boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi : rlwj;
    while(remaining.size()>0) {
      container.addStreamOfEmptyWords(false, remaining.size());
      remaining.discardFirstWords(remaining.size());
    }
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }


  /**
   * Returns the cardinality of the result of a bitwise AND of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int andCardinality(final EWAHCompressedBitmap32 a) {
    final BitCounter32 counter = new BitCounter32();
    and(a, counter);
    return counter.getCount();
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND NOT values of
   * the current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public EWAHCompressedBitmap32 andNot(final EWAHCompressedBitmap32 a) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    container
      .reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
        : a.actualsizeinwords);
    andNot(a, container);
    return container;
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND NOT values of
   * the current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  private void andNot(final EWAHCompressedBitmap32 a,
    final BitmapStorage32 container) {
    final EWAHIterator32 i = a.getEWAHIterator();
    final EWAHIterator32 j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
    }
    // at this point, this is safe:
    BufferedRunningLengthWord32 rlwi = new BufferedRunningLengthWord32(i.next());
    rlwi.setRunningBit(!rlwi.getRunningBit());
    BufferedRunningLengthWord32 rlwj = new BufferedRunningLengthWord32(j.next());
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord32 prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord32 predator = i_is_prey ? rlwj : rlwi;

      if (prey.getRunningBit() == false) {
        container.addStreamOfEmptyWords(false, prey.RunningLength);
        predator.discardFirstWords(prey.RunningLength);
        prey.RunningLength = 0;
      } else {
        // we have a stream of 1x11
        final int predatorrl = predator.getRunningLength();
        final int preyrl = prey.getRunningLength();
        final int tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.literalwordoffset
          + (i_is_prey ? j.literalWords() : i.literalWords());
        if (i_is_prey)
          container.addStreamOfLiteralWords(j.buffer(), dw_predator, preyrl
            - tobediscarded);
        else
          container.addStreamOfNegatedLiteralWords(i.buffer(), dw_predator,
            preyrl - tobediscarded);
        predator.discardFirstWords(preyrl);
        prey.RunningLength = 0;
      }
      final int predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_literal_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_literal_prey) ? nbre_literal_prey
            : predatorrl;
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfEmptyWords(false, tobediscarded);
        } else {
          final int nbre_literal_prey = prey.getNumberOfLiteralWords();
          final int dw_prey = prey.literalwordoffset
            + (i_is_prey ? i.literalWords() : j.literalWords());
          final int tobediscarded = (predatorrl >= nbre_literal_prey) ? nbre_literal_prey
            : predatorrl;
          if (i_is_prey)
            container.addStreamOfNegatedLiteralWords(i.buffer(), dw_prey,
              tobediscarded);
          else
            container.addStreamOfLiteralWords(j.buffer(), dw_prey, tobediscarded);
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
        }
      }
      // all that is left to do now is to AND the literal words
      final int nbre_literal_prey = prey.getNumberOfLiteralWords();
      if (nbre_literal_prey > 0) {
        for (int k = 0; k < nbre_literal_prey; ++k) {
          if (i_is_prey)
            container.add((~i.buffer()[prey.literalwordoffset + i.literalWords()
              + k])
              & j.buffer()[predator.literalwordoffset + j.literalWords() + k]);
          else
            container.add((~i.buffer()[predator.literalwordoffset
              + i.literalWords() + k])
              & j.buffer()[prey.literalwordoffset + j.literalWords() + k]);
        }
        predator.discardFirstWords(nbre_literal_prey);
      }
      if (i_is_prey) {
        if (!i.hasNext()) {
          rlwi = null;
          break;
        }
        rlwi.reset(i.next());
        rlwi.setRunningBit(!rlwi.getRunningBit());
      } else {
        if (!j.hasNext()) {
          rlwj = null;
          break;
        }
        rlwj.reset(j.next());
      }
    }
    if (rlwi != null)
      dischargeAsEmpty(rlwi, i, container);
    if (rlwj != null)
      discharge(rlwj, j, container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }

  /**
   * Returns the cardinality of the result of a bitwise AND NOT of the values of
   * the current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int andNotCardinality(final EWAHCompressedBitmap32 a) {
    final BitCounter32 counter = new BitCounter32();
    andNot(a, counter);
    return counter.getCount();
  }

  /**
   * reports the number of bits set to true. Running time is proportional to
   * compressed size (as reported by sizeInBytes).
   * 
   * @return the number of bits set to true
   */
  public int cardinality() {
    int counter = 0;
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord32 localrlw = i.next();
      if (localrlw.getRunningBit()) {
        counter += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        counter += Integer.bitCount(i.buffer()[i.literalWords() + j]);
      }
    }
    return counter;
  }

  /**
   * Clear any set bits and set size in bits back to 0
   */
  public void clear() {
    this.sizeinbits = 0;
    this.actualsizeinwords = 1;
    this.rlw.position = 0;
    // buffer is not fully cleared but any new set operations should overwrite
    // stale data
    this.buffer[0] = 0;
  }

  /*
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() throws java.lang.CloneNotSupportedException {
    final EWAHCompressedBitmap32 clone = (EWAHCompressedBitmap32) super.clone();
    clone.buffer = this.buffer.clone();
    clone.actualsizeinwords = this.actualsizeinwords;
    clone.sizeinbits = this.sizeinbits;
    return clone;
  }

  /**
   * Deserialize.
   * 
   * @param in
   *          the DataInput stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void deserialize(DataInput in) throws IOException {
    this.sizeinbits = in.readInt();
    this.actualsizeinwords = in.readInt();
    if (this.buffer.length < this.actualsizeinwords) {
      this.buffer = new int[this.actualsizeinwords];
    }
    for (int k = 0; k < this.actualsizeinwords; ++k)
      this.buffer[k] = in.readInt();
    this.rlw = new RunningLengthWord32(this.buffer, in.readInt());
  }

  /**
   * Check to see whether the two compressed bitmaps contain the same set bits.
   * 
   * @author Colby Ranger
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof EWAHCompressedBitmap32) {
      try {
        this.xor((EWAHCompressedBitmap32) o, new NonEmptyVirtualStorage32());
        return true;
      } catch (NonEmptyVirtualStorage32.NonEmptyException e) {
        return false;
      }
    }
    return false;
  }

  /**
   * For experts: You want to add many zeroes or ones faster?
   * 
   * This method does not update sizeinbits.
   * 
   * @param v
   *          the boolean value
   * @param number
   *          the number (must be greater than 0)
   */
  private void fastaddStreamOfEmptyWords(final boolean v, int number) {
    if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
      this.rlw.setRunningBit(v);
    } else if ((this.rlw.getNumberOfLiteralWords() != 0)
      || (this.rlw.getRunningBit() != v)) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
    }
    final int runlen = this.rlw.getRunningLength();
    final int whatwecanadd = number < RunningLengthWord32.largestrunninglengthcount
      - runlen ? number : RunningLengthWord32.largestrunninglengthcount
      - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;
    while (number >= RunningLengthWord32.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord32.largestrunninglengthcount);
      number -= RunningLengthWord32.largestrunninglengthcount;
    }
    if (number > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(number);
    }
  }

  /**
   * Gets an EWAHIterator over the data. This is a customized iterator which
   * iterates over run length word. For experts only.
   * 
   * @return the EWAHIterator
   */
  private EWAHIterator32 getEWAHIterator() {
    return new EWAHIterator32(this.buffer, this.actualsizeinwords);
  }

  /**
   * get the locations of the true values as one vector. (may use more memory
   * than iterator())
   * 
   * @return the positions
   */
  public List<Integer> getPositions() {
    final ArrayList<Integer> v = new ArrayList<Integer>();
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    int pos = 0;
    while (i.hasNext()) {
      RunningLengthWord32 localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c)
            v.add(new Integer(pos++));
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        int data = i.buffer()[i.literalWords() + j];
        while (data != 0) {
          final int ntz = Integer.numberOfTrailingZeros(data);
          data ^= (1 << ntz);
          v.add(new Integer(ntz + pos));
        }
        pos += wordinbits;
      }
    }
    while ((v.size() > 0)
      && (v.get(v.size() - 1).intValue() >= this.sizeinbits))
      v.remove(v.size() - 1);
    return v;
  }

  /**
   * Returns a customized hash code (based on Karp-Rabin). Naturally, if the
   * bitmaps are equal, they will hash to the same value.
   * 
   */
  @Override
  public int hashCode() {
    int karprabin = 0;
    final int B = 31;
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    while( i.hasNext() ) {
      i.next();
      if (i.rlw.getRunningBit() == true) {
        karprabin += B * karprabin + i.rlw.getRunningLength();
      }
      for (int k = 0; k < i.rlw.getNumberOfLiteralWords(); ++k) {
        karprabin += B * karprabin + this.buffer[k + i.literalWords()];
      }
    }
    return karprabin;
  }

  /**
   * Return true if the two EWAHCompressedBitmap have both at least one true bit
   * in the same position. Equivalently, you could call "and" and check whether
   * there is a set bit, but intersects will run faster if you don't need the
   * result of the "and" operation.
   * 
   * @param a
   *          the other bitmap
   * @return whether they intersect
   */
  public boolean intersects(final EWAHCompressedBitmap32 a) {
    NonEmptyVirtualStorage32 nevs = new NonEmptyVirtualStorage32();
    try {
      this.and(a, nevs);
    } catch (NonEmptyVirtualStorage32.NonEmptyException nee) {
      return true;
    }
    return false;
  }

  /**
   * Iterator over the set bits (this is what most people will want to use to
   * browse the content if they want an iterator). The location of the set bits
   * is returned, in increasing order.
   * 
   * @return the int iterator
   */
  public IntIterator intIterator() {
    return new IntIteratorImpl32(
        new EWAHIterator32(this.buffer, this.actualsizeinwords));
  }

  /**
   * iterate over the positions of the true values. This is similar to
   * intIterator(), but it uses Java generics.
   * 
   * @return the iterator
   */
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      public boolean hasNext() {
        return this.under.hasNext();
      }

      public Integer next() {
        return new Integer(this.under.next());
      }

      public void remove() {
        throw new UnsupportedOperationException("bitsets do not support remove");
      }

      final private IntIterator under = intIterator();
    };
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the array of words to be added
   * @param start
   *          the starting point
   * @param number
   *          the number of words to add
   */
  private void negative_push_back(final int[] data, final int start,
    final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final int oldbuffer[] = this.buffer;
      this.buffer = new int[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    for (int k = 0; k < number; ++k)
      this.buffer[this.actualsizeinwords + k] = ~data[start + k];
    this.actualsizeinwords += number;
  }

  /**
   * Negate (bitwise) the current bitmap. To get a negated copy, do
   * ((EWAHCompressedBitmap) mybitmap.clone()).not();
   * 
   * The running time is proportional to the compressed size (as reported by
   * sizeInBytes()).
   * 
   */
  public void not() {
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    if (!i.hasNext())
      return;
    while (true) {
      final RunningLengthWord32 rlw1 = i.next();
      rlw1.setRunningBit(!rlw1.getRunningBit());
      for (int j = 0; j < rlw1.getNumberOfLiteralWords(); ++j) {
        i.buffer()[i.literalWords() + j] = ~i.buffer()[i.literalWords() + j];
      }
      if (!i.hasNext()) {// must potentially adjust the last literal word
        if (rlw1.getNumberOfLiteralWords() == 0)
          return;
        final int usedbitsinlast = this.sizeinbits % wordinbits;
        if (usedbitsinlast == 0)
          return;
        i.buffer()[i.literalWords() + rlw1.getNumberOfLiteralWords() - 1] &= ((~0) >>> (wordinbits - usedbitsinlast));
        return;
      }
    }
  }

  public int oldaddStreamOfEmptyWords(final boolean v, final int number) {
    if (number == 0)
      return 0;
    final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
    final int runlen = this.rlw.getRunningLength();
    if ((noliteralword) && (runlen == 0)) {
      this.rlw.setRunningBit(v);
    }
    int wordsadded = 0;
    if ((noliteralword) && (this.rlw.getRunningBit() == v)
      && (runlen < RunningLengthWord32.largestrunninglengthcount)) {
      int whatwecanadd = number < RunningLengthWord32.largestrunninglengthcount
        - runlen ? number : RunningLengthWord32.largestrunninglengthcount
        - runlen;
      this.rlw.setRunningLength(runlen + whatwecanadd);
      this.sizeinbits += whatwecanadd * wordinbits;
      if (number - whatwecanadd > 0)
        addStreamOfEmptyWords(v, number - whatwecanadd);
    } else {
      push_back(0);
      ++wordsadded;
      this.rlw.position = this.actualsizeinwords - 1;
      final int whatwecanadd = number < RunningLengthWord32.largestrunninglengthcount ? number
        : RunningLengthWord32.largestrunninglengthcount;
      this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(whatwecanadd);
      this.sizeinbits += whatwecanadd * wordinbits;
      if (number - whatwecanadd > 0)
        addStreamOfEmptyWords(v, number - whatwecanadd);
    }
    return wordsadded;
  }

  /**
   * Returns a new compressed bitmap containing the bitwise OR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public EWAHCompressedBitmap32 or(final EWAHCompressedBitmap32 a) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    container.reserve(this.actualsizeinwords + a.actualsizeinwords);
    or(a, container);
    return container;
  }

  /**
   * Computes the bitwise or between the current bitmap and the bitmap "a".
   * Stores the result in the container.
   * 
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void or(final EWAHCompressedBitmap32 a, final BitmapStorage32 container) {
    final EWAHIterator32 i = a.getEWAHIterator();
    final EWAHIterator32 j = getEWAHIterator();
    IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(i);
    IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(j);
    while ((rlwi.size()>0) && (rlwj.size()>0)) {
      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
          .getRunningLength();
        final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
          : rlwj;
        final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
          : rlwi;
        if (predator.getRunningBit() == true) {
          container.addStreamOfEmptyWords(true, predator.getRunningLength());
          prey.discardFirstWords(predator.getRunningLength());
          predator.discardFirstWords(predator.getRunningLength());
        } else {
          int index = 0;
          while ((index < predator.getRunningLength()) && (prey.size() > 0)) {

            int pl = prey.getRunningLength();
            if (index + pl > predator.getRunningLength()) {
              pl = predator.getRunningLength() - index;
            }
            container.addStreamOfEmptyWords(prey.getRunningBit(), pl);
            prey.discardFirstWords(pl);
            index += pl;
            int pd = prey.getNumberOfLiteralWords();
            if (pd + index > predator.getRunningLength()) {
              pd = predator.getRunningLength() - index;
            }
            prey.writeLiteralWords(pd, container);
            prey.discardFirstWords(pd);
            index += pd;

          }
          container.addStreamOfEmptyWords(false, predator.getRunningLength()
            - index);
          predator.discardFirstWords(predator.getRunningLength());
        }
      }
      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
        rlwj.getNumberOfLiteralWords());
      if (nbre_literal > 0) {
        for (int k = 0; k < nbre_literal; ++k) {
          container.add(rlwi.getLiteralWordAt(k) | rlwj.getLiteralWordAt(k));
        }
        rlwi.discardFirstWords(nbre_literal);
        rlwj.discardFirstWords(nbre_literal);
      }
    }
    boolean i_remains = rlwi.size()>0;
    final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi : rlwj;
    remaining.discharge(container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }
    /**
   * Returns the cardinality of the result of a bitwise OR of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int orCardinality(final EWAHCompressedBitmap32 a) {
    final BitCounter32 counter = new BitCounter32();
    or(a, counter);
    return counter.getCount();
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the word to be added
   */
  private void push_back(final int data) {
    if (this.actualsizeinwords == this.buffer.length) {
      final int oldbuffer[] = this.buffer;
      this.buffer = new int[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    this.buffer[this.actualsizeinwords++] = data;
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the array of words to be added
   * @param start
   *          the starting point
   * @param number
   *          the number of words to add
   */
  private void push_back(final int[] data, final int start, final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final int oldbuffer[] = this.buffer;
      this.buffer = new int[oldbuffer.length * 2];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
    }
    System.arraycopy(data, start, this.buffer, this.actualsizeinwords, number);
    this.actualsizeinwords += number;
  }

  /*
   * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
   */
  public void readExternal(ObjectInput in) throws IOException {
    deserialize(in);
  }

  /**
   * For internal use (trading off memory for speed).
   * 
   * @param size
   *          the number of words to allocate
   * @return True if the operation was a success.
   */
  private boolean reserve(final int size) {
    if (size > this.buffer.length) {
      final int oldbuffer[] = this.buffer;
      this.buffer = new int[size];
      System.arraycopy(oldbuffer, 0, this.buffer, 0, oldbuffer.length);
      this.rlw.array = this.buffer;
      return true;
    }
    return false;
  }

  /**
   * Serialize.
   * 
   * @param out
   *          the DataOutput stream
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void serialize(DataOutput out) throws IOException {
    out.writeInt(this.sizeinbits);
    out.writeInt(this.actualsizeinwords);
    for (int k = 0; k < this.actualsizeinwords; ++k)
      out.writeInt(this.buffer[k]);
    out.writeInt(this.rlw.position);
  }

  /**
   * Report the size required to serialize this bitmap
   * 
   * @return the size in bytes
   */
  public int serializedSizeInBytes() {
    return this.sizeInBytes() + 3 * 4;
  }

  /**
   * set the bit at position i to true, the bits must be set in increasing
   * order. For example, set(15) and then set(7) will fail. You must do set(7)
   * and then set(15).
   * 
   * @param i
   *          the index
   * @return true if the value was set (always true when i>= sizeInBits()).
   * @throws IndexOutOfBoundsException
   *           if i is negative or greater than Integer.MAX_VALUE - 32
   */

  public boolean set(final int i) {
    if ((i > Integer.MAX_VALUE - wordinbits) || (i < 0))
      throw new IndexOutOfBoundsException("Set values should be between 0 and "
        + (Integer.MAX_VALUE - wordinbits));
    if (i < this.sizeinbits)
      return false;
    // distance in words:
    final int dist = (i + wordinbits) / wordinbits
      - (this.sizeinbits + wordinbits - 1) / wordinbits;
    this.sizeinbits = i + 1;
    if (dist > 0) {// easy
      if (dist > 1)
        fastaddStreamOfEmptyWords(false, dist - 1);
      addLiteralWord(1 << (i % wordinbits));
      return true;
    }
    if (this.rlw.getNumberOfLiteralWords() == 0) {
      this.rlw.setRunningLength(this.rlw.getRunningLength() - 1);
      addLiteralWord(1 << (i % wordinbits));
      return true;
    }
    this.buffer[this.actualsizeinwords - 1] |= 1 << (i % wordinbits);
    if (this.buffer[this.actualsizeinwords - 1] == ~0) {
      this.buffer[this.actualsizeinwords - 1] = 0;
      --this.actualsizeinwords;
      this.rlw.setNumberOfLiteralWords(this.rlw.getNumberOfLiteralWords() - 1);
      // next we add one clean word
      addEmptyWord(true);
    }
    return true;
  }

  /**
   * set the size in bits
   * 
   */
  public void setSizeInBits(final int size) {
    this.sizeinbits = size;
  }

  /**
   * Change the reported size in bits of the *uncompressed* bitmap represented
   * by this compressed bitmap. It is not possible to reduce the sizeInBits, but
   * it can be extended. The new bits are set to false or true depending on the
   * value of defaultvalue.
   * 
   * @param size
   *          the size in bits
   * @param defaultvalue
   *          the default boolean value
   * @return true if the update was possible
   */
  public boolean setSizeInBits(final int size, final boolean defaultvalue) {
    if (size < this.sizeinbits)
      return false;
    // next loop could be optimized further
    if (defaultvalue)
      while (((this.sizeinbits % wordinbits) != 0) && (this.sizeinbits < size)) {
        this.set(this.sizeinbits);
      }

    if (defaultvalue == false)
      extendEmptyBits(this, this.sizeinbits, size);
    else {
      final int leftover = size % wordinbits;
      this.addStreamOfEmptyWords(defaultvalue, (size / wordinbits)
        - this.sizeinbits / wordinbits);
      final int newdata = (1 << leftover) + ((1 << leftover) - 1);
      this.addLiteralWord(newdata);
    }
    this.sizeinbits = size;
    return true;
  }

  /**
   * Returns the size in bits of the *uncompressed* bitmap represented by this
   * compressed bitmap. Initially, the sizeInBits is zero. It is extended
   * automatically when you set bits to true.
   * 
   * @return the size in bits
   */
  public int sizeInBits() {
    return this.sizeinbits;
  }

  /**
   * Report the *compressed* size of the bitmap (equivalent to memory usage,
   * after accounting for some overhead).
   * 
   * @return the size in bytes
   */
  public int sizeInBytes() {
    return this.actualsizeinwords * (wordinbits / 8);
  }

  /**
   * Populate an array of (sorted integers) corresponding to the location of the
   * set bits.
   * 
   * @return the array containing the location of the set bits
   */
  public int[] toArray() {
    int[] ans = new int[this.cardinality()];
    int inanspos = 0;
    int pos = 0;
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord32 localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c) {
            ans[inanspos++] = pos++;
          }
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        int data = i.buffer()[i.literalWords() + j];
        if (!usetrailingzeros) {
          for (int c = 0; c < wordinbits; ++c) {
            if ((data & (1 << c)) != 0)
              ans[inanspos++] = c + pos;
          }
          pos += wordinbits;
        } else {
          while (data != 0) {
            final int ntz = Integer.numberOfTrailingZeros(data);
            data ^= (1l << ntz);
            ans[inanspos++] = ntz + pos;
          }
          pos += wordinbits;
        }
      }
    }
    return ans;

  }

  /**
   * A more detailed string describing the bitmap (useful for debugging).
   * 
   * @return the string
   */
  public String toDebugString() {
    String ans = " EWAHCompressedBitmap, size in bits = " + this.sizeinbits
      + " size in words = " + this.actualsizeinwords + "\n";
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord32 localrlw = i.next();
      if (localrlw.getRunningBit()) {
        ans += localrlw.getRunningLength() + " 1x11\n";
      } else {
        ans += localrlw.getRunningLength() + " 0x00\n";
      }
      ans += localrlw.getNumberOfLiteralWords() + " dirties\n";
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        int data = i.buffer()[i.literalWords() + j];
        ans += "\t" + data + "\n";
      }
    }
    return ans;
  }

  /**
   * A string describing the bitmap.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    String ans = " EWAHCompressedBitmap, size in bits = " + this.sizeinbits
      + " size in words = " + this.actualsizeinwords + "\n";
    final EWAHIterator32 i = new EWAHIterator32(this.buffer,
      this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord32 localrlw = i.next();
      if (localrlw.getRunningBit()) {
        ans += localrlw.getRunningLength() + " 1x11\n";
      } else {
        ans += localrlw.getRunningLength() + " 0x00\n";
      }
      ans += localrlw.getNumberOfLiteralWords() + " dirties\n";
    }
    return ans;
  }

  /*
   * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    serialize(out);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise XOR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public EWAHCompressedBitmap32 xor(final EWAHCompressedBitmap32 a) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    container.reserve(this.actualsizeinwords + a.actualsizeinwords);
    xor(a, container);
    return container;
  }

  /**
   * Computes a new compressed bitmap containing the bitwise XOR values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void xor(final EWAHCompressedBitmap32 a,
    final BitmapStorage32 container) {
    final EWAHIterator32 i = a.getEWAHIterator();
    final EWAHIterator32 j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
    }
    // at this point, this is safe:
    BufferedRunningLengthWord32 rlwi = new BufferedRunningLengthWord32(i.next());
    BufferedRunningLengthWord32 rlwj = new BufferedRunningLengthWord32(j.next());
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord32 prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord32 predator = i_is_prey ? rlwj : rlwi;
      if (prey.getRunningBit() == false) {
        final int predatorrl = predator.getRunningLength();
        final int preyrl = prey.getRunningLength();
        final int tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.literalwordoffset
          + (i_is_prey ? j.literalWords() : i.literalWords());
        container.addStreamOfLiteralWords(i_is_prey ? j.buffer() : i.buffer(),
          dw_predator, preyrl - tobediscarded);
        predator.discardFirstWords(preyrl);
        prey.discardFirstWords(preyrl);
      } else {
        // we have a stream of 1x11
        final int predatorrl = predator.getRunningLength();
        final int preyrl = prey.getRunningLength();
        final int tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container.addStreamOfEmptyWords(!predator.getRunningBit(),
          tobediscarded);
        final int dw_predator = predator.literalwordoffset
          + (i_is_prey ? j.literalWords() : i.literalWords());
        final int[] buf = i_is_prey ? j.buffer() : i.buffer();
        for (int k = 0; k < preyrl - tobediscarded; ++k)
          container.add(~buf[k + dw_predator]);
        predator.discardFirstWords(preyrl);
        prey.discardFirstWords(preyrl);
      }
      final int predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_literal_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_literal_prey) ? nbre_literal_prey
            : predatorrl;
          final int dw_prey = prey.literalwordoffset
            + (i_is_prey ? i.literalWords() : j.literalWords());
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfLiteralWords(i_is_prey ? i.buffer() : j.buffer(),
            dw_prey, tobediscarded);
        } else {
          final int nbre_literal_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_literal_prey) ? nbre_literal_prey
            : predatorrl;
          final int dw_prey = prey.literalwordoffset
            + (i_is_prey ? i.literalWords() : j.literalWords());
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          final int[] buf = i_is_prey ? i.buffer() : j.buffer();
          for (int k = 0; k < tobediscarded; ++k)
            container.add(~buf[k + dw_prey]);
        }
      }
      // all that is left to do now is to AND the literal words
      final int nbre_literal_prey = prey.getNumberOfLiteralWords();
      if (nbre_literal_prey > 0) {
        for (int k = 0; k < nbre_literal_prey; ++k) {
          if (i_is_prey)
            container.add(i.buffer()[prey.literalwordoffset + i.literalWords() + k]
              ^ j.buffer()[predator.literalwordoffset + j.literalWords() + k]);
          else
            container.add(i.buffer()[predator.literalwordoffset + i.literalWords()
              + k]
              ^ j.buffer()[prey.literalwordoffset + j.literalWords() + k]);
        }
        predator.discardFirstWords(nbre_literal_prey);
      }
      if (i_is_prey) {
        if (!i.hasNext()) {
          rlwi = null;
          break;
        }
        rlwi.reset(i.next());
      } else {
        if (!j.hasNext()) {
          rlwj = null;
          break;
        }
        rlwj.reset(j.next());
      }
    }
    if (rlwi != null)
      discharge(rlwi, i, container);
    if (rlwj != null)
      discharge(rlwj, j, container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }

  /**
   * Returns the cardinality of the result of a bitwise XOR of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int xorCardinality(final EWAHCompressedBitmap32 a) {
    final BitCounter32 counter = new BitCounter32();
    xor(a, counter);
    return counter.getCount();
  }

  /**
   * For internal use. Computes the bitwise and of the provided bitmaps and
   * stores the result in the container.
   * 
   * @param container
   *          where the result is stored
   * @param bitmaps
   *          bitmaps to AND
   */
  private static void and(final BitmapStorage32 container,
    final EWAHCompressedBitmap32... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].and(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in ascending order by sizeinbits. When we exhaust the
    // first bitmap the rest
    // of the result is zeros.
    final EWAHCompressedBitmap32[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<EWAHCompressedBitmap32>() {
      public int compare(EWAHCompressedBitmap32 a, EWAHCompressedBitmap32 b) {
        return a.sizeinbits < b.sizeinbits ? -1
          : a.sizeinbits == b.sizeinbits ? 0 : 1;
      }
    });

    int maxSize = sortedBitmaps[sortedBitmaps.length - 1].sizeinbits;

    final IteratingBufferedRunningLengthWord32[] rlws = new IteratingBufferedRunningLengthWord32[bitmaps.length];
    for (int i = 0; i < sortedBitmaps.length; i++) {
      EWAHIterator32 iterator = sortedBitmaps[i].getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[i] = new IteratingBufferedRunningLengthWord32(iterator);
      } else {
        // this never happens...
        if (maxSize > 0) {
          extendEmptyBits(container, 0, maxSize);
        }
        container.setSizeInBits(maxSize);
        return;
      }
    }

    while (true) {
      int maxZeroRl = 0;
      int minOneRl = Integer.MAX_VALUE;
      int minSize = Integer.MAX_VALUE;
      int numEmptyRl = 0;

      if (rlws[0].size() == 0) {
        extendEmptyBits(container, sortedBitmaps[0].sizeinbits, maxSize);
        break;
      }

      for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
        int size = rlw.size();
        minSize = Math.min(minSize, size);

        if (!rlw.getRunningBit()) {
          int rl = rlw.getRunningLength();
          maxZeroRl = Math.max(maxZeroRl, rl);
          minOneRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          int rl = rlw.getRunningLength();
          minOneRl = Math.min(minOneRl, rl);
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        }
      }

      if (maxZeroRl > 0) {
        container.addStreamOfEmptyWords(false, maxZeroRl);
        for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
          rlw.discardFirstWords(maxZeroRl);
        }
      } else if (minOneRl > 0) {
        container.addStreamOfEmptyWords(true, minOneRl);
        for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
          rlw.discardFirstWords(minOneRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has literal words to process and the rest have a run of
          // 1's we can write them out here
          IteratingBufferedRunningLengthWord32 emptyRl = null;
          int minNonEmptyRl = Integer.MAX_VALUE;
          for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
            int rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          int wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeLiteralWords(wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          int word = ~0;
          for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
            if (rlw.getRunningLength() <= index) {
              word &= rlw.getLiteralWordAt(index - rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (IteratingBufferedRunningLengthWord32 rlw : rlws) {
          rlw.discardFirstWords(minSize);
        }
      }
    }
    container.setSizeInBits(maxSize);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND values of the
   * provided bitmaps.
   * 
   * It may or may not be faster than doing the aggregation two-by-two (A.and(B).and(C)). 
   * 
   * @param bitmaps
   *          bitmaps to AND together
   * @return result of the AND
   */
  public static EWAHCompressedBitmap32 and(
    final EWAHCompressedBitmap32... bitmaps) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    int largestSize = 0;
    for (EWAHCompressedBitmap32 bitmap : bitmaps) {
      largestSize = Math.max(bitmap.actualsizeinwords, largestSize);
    }
    container.reserve((int) (largestSize * 1.5));
    and(container, bitmaps);
    return container;
  }

  /**
   * Returns the cardinality of the result of a bitwise AND of the values of the
   * provided bitmaps. Avoids needing to allocate an intermediate bitmap to hold
   * the result of the AND.
   * 
   * @param bitmaps
   *          bitmaps to AND
   * @return the cardinality
   */
  public static int andCardinality(final EWAHCompressedBitmap32... bitmaps) {
    final BitCounter32 counter = new BitCounter32();
    and(counter, bitmaps);
    return counter.getCount();
  }
  

  /**
   * Return a bitmap with the bit set to true at the given
   * positions. The positions should be given in sorted order.
   * 
   * (This is a convenience method.)
   * 
   * @since 0.4.5
   * @param setbits list of set bit positions
   * @return the bitmap
   */
  public static EWAHCompressedBitmap32 bitmapOf(int ... setbits) {
    EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
    for (int k : setbits)
      a.set(k);
    return a;
  }

  /**
   * For internal use.
   * 
   * @param initialWord
   *          the initial word
   * @param iterator
   *          the iterator
   * @param container
   *          the container
   */
  protected static void discharge(
    final BufferedRunningLengthWord32 initialWord,
    final EWAHIterator32 iterator, final BitmapStorage32 container) {
    BufferedRunningLengthWord32 runningLengthWord = initialWord;
    for (;;) {
      final int runningLength = runningLengthWord.getRunningLength();
      container.addStreamOfEmptyWords(runningLengthWord.getRunningBit(),
        runningLength);
      container.addStreamOfLiteralWords(iterator.buffer(), iterator.literalWords()
        + runningLengthWord.literalwordoffset,
        runningLengthWord.getNumberOfLiteralWords());
      if (!iterator.hasNext())
        break;
      runningLengthWord = new BufferedRunningLengthWord32(iterator.next());
    }
  }

  /**
   * For internal use.
   * 
   * @param initialWord
   *          the initial word
   * @param iterator
   *          the iterator
   * @param container
   *          the container
   */
  private static void dischargeAsEmpty(
    final BufferedRunningLengthWord32 initialWord,
    final EWAHIterator32 iterator, final BitmapStorage32 container) {
    BufferedRunningLengthWord32 runningLengthWord = initialWord;
    for (;;) {
      final int runningLength = runningLengthWord.getRunningLength();
      container.addStreamOfEmptyWords(false,
        runningLength + runningLengthWord.getNumberOfLiteralWords());
      if (!iterator.hasNext())
        break;
      runningLengthWord = new BufferedRunningLengthWord32(iterator.next());
    }
  }

  /**
   * For internal use. This simply adds a stream of words made of zeroes so that
   * we pad to the desired size.
   * 
   * @param storage
   *          bitmap to extend
   * @param currentSize
   *          current size (in bits)
   * @param newSize
   *          new desired size (in bits)
   */
  private static void extendEmptyBits(final BitmapStorage32 storage,
    final int currentSize, final int newSize) {
    final int currentLeftover = currentSize % wordinbits;
    final int finalLeftover = newSize % wordinbits;
    storage.addStreamOfEmptyWords(false, (newSize / wordinbits) - currentSize
      / wordinbits + (finalLeftover != 0 ? 1 : 0)
      + (currentLeftover != 0 ? -1 : 0));
  }

  /**
   * For internal use. Computes the bitwise or of the provided bitmaps and
   * stores the result in the container.
   */
  private static void or(final BitmapStorage32 container,
    final EWAHCompressedBitmap32... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].or(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in descending order by sizeinbits. We will exhaust the
    // sorted bitmaps from right to left.
    final EWAHCompressedBitmap32[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<EWAHCompressedBitmap32>() {
      public int compare(EWAHCompressedBitmap32 a, EWAHCompressedBitmap32 b) {
        return a.sizeinbits < b.sizeinbits ? 1
          : a.sizeinbits == b.sizeinbits ? 0 : -1;
      }
    });

    final IteratingBufferedRunningLengthWord32[] rlws = new IteratingBufferedRunningLengthWord32[bitmaps.length];
    int maxAvailablePos = 0;
    for (EWAHCompressedBitmap32 bitmap : sortedBitmaps) {
      EWAHIterator32 iterator = bitmap.getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord32(
          iterator);
      }
    }

    if (maxAvailablePos == 0) { // this never happens...
      container.setSizeInBits(0);
      return;
    }

    int maxSize = sortedBitmaps[0].sizeinbits;

    while (true) {
      int maxOneRl = 0;
      int minZeroRl = Integer.MAX_VALUE;
      int minSize = Integer.MAX_VALUE;
      int numEmptyRl = 0;
      for (int i = 0; i < maxAvailablePos; i++) {
        IteratingBufferedRunningLengthWord32 rlw = rlws[i];
        int size = rlw.size();
        if (size == 0) {
          maxAvailablePos = i;
          break;
        }
        minSize = Math.min(minSize, size);

        if (rlw.getRunningBit()) {
          int rl = rlw.getRunningLength();
          maxOneRl = Math.max(maxOneRl, rl);
          minZeroRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          int rl = rlw.getRunningLength();
          minZeroRl = Math.min(minZeroRl, rl);
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        }
      }

      if (maxAvailablePos == 0) {
        break;
      } else if (maxAvailablePos == 1) {
        // only one bitmap is left so just write the rest of it out
        rlws[0].discharge(container);
        break;
      }

      if (maxOneRl > 0) {
        container.addStreamOfEmptyWords(true, maxOneRl);
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord32 rlw = rlws[i];
          rlw.discardFirstWords(maxOneRl);
        }
      } else if (minZeroRl > 0) {
        container.addStreamOfEmptyWords(false, minZeroRl);
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord32 rlw = rlws[i];
          rlw.discardFirstWords(minZeroRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has literal words to process and the rest have a run of
          // 0's we can write them out here
          IteratingBufferedRunningLengthWord32 emptyRl = null;
          int minNonEmptyRl = Integer.MAX_VALUE;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord32 rlw = rlws[i];
            int rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          int wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeLiteralWords(wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          int word = 0;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord32 rlw = rlws[i];
            if (rlw.getRunningLength() <= index) {
              word |= rlw.getLiteralWordAt(index - rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord32 rlw = rlws[i];
          rlw.discardFirstWords(minSize);
        }
      }
    }
    container.setSizeInBits(maxSize);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise OR values of the
   * provided bitmaps.  This is typically faster than doing the aggregation
   * two-by-two (A.or(B).or(C).or(D)).
   * 
   * @param bitmaps
   *          bitmaps to OR together
   * @return result of the OR
   */
  public static EWAHCompressedBitmap32 or(
    final EWAHCompressedBitmap32... bitmaps) {
    final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
    int largestSize = 0;
    for (EWAHCompressedBitmap32 bitmap : bitmaps) {
      largestSize = Math.max(bitmap.actualsizeinwords, largestSize);
    }
    container.reserve((int) (largestSize * 1.5));
    or(container, bitmaps);
    return container;
  }

  /**
   * Returns the cardinality of the result of a bitwise OR of the values of the
   * provided bitmaps. Avoids needing to allocate an intermediate bitmap to hold
   * the result of the OR.
   * 
   * @param bitmaps
   *          bitmaps to OR
   * @return the cardinality
   */
  public static int orCardinality(final EWAHCompressedBitmap32... bitmaps) {
    final BitCounter32 counter = new BitCounter32();
    or(counter, bitmaps);
    return counter.getCount();
  }

  /** The actual size in words. */
  int actualsizeinwords = 1;

  /** The buffer (array of 32-bit words) */
  int buffer[] = null;

  /** The current (last) running length word. */
  RunningLengthWord32 rlw = null;

  /** sizeinbits: number of bits in the (uncompressed) bitmap. */
  int sizeinbits = 0;

  /**
   * The Constant defaultbuffersize: default memory allocation when the object
   * is constructed.
   */
  static final int defaultbuffersize = 4;

  /** optimization option **/
  public static final boolean usetrailingzeros = true;

  /** The Constant wordinbits represents the number of bits in a int. */
  public static final int wordinbits = 32;

}
