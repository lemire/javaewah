package javaewah;

/*
 * Copyright 2009-2012, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho and and Google Inc.
 * Licensed under APL 2.0.
 */

import java.util.*;
import java.io.*;

/**
 * <p>
 * This implements the patent-free(1) EWAH scheme. Roughly speaking, it is a
 * 64-bit variant of the BBC compression scheme used by Oracle for its bitmap
 * indexes.
 * </p>
 * 
 * <p>
 * The objective of this compression type is to provide some compression, while
 * reducing as much as possible the CPU cycle usage.
 * </p>
 * 
 * 
 * <p>
 * This implementation being 64-bit, it assumes a 64-bit CPU together with a
 * 64-bit Java Virtual Machine. This same code on a 32-bit machine may not be as
 * fast.
 * <p>
 * 
 * <p>
 * There is also a 32-bit version of this code in the class
 * javaewah32.EWAHCompressedBitmap32
 * </p>
 * 
 * @see javaewah32.EWAHCompressedBitmap32
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
 *      <p>
 *      A 32-bit version of the compressed format was described by Wu et al. and
 *      named WBC:
 *      </p>
 * 
 *      <ul>
 *      <li>K. Wu, E. J. Otoo, A. Shoshani, H. Nordberg, Notes on design and
 *      implementation of compressed bit vectors, Tech. Rep. LBNL/PUB-3161,
 *      Lawrence Berkeley National Laboratory, available from http://crd.lbl.
 *      gov/~kewu/ps/PUB-3161.html (2001).</li>
 *      </ul>
 * 
 *      <p>
 *      Probably, the best prior art is the Oracle bitmap compression scheme
 *      (BBC):
 *      </p>
 *      <ul>
 *      <li>G. Antoshenkov, Byte-Aligned Bitmap Compression, DCC'95, 1995.</li>
 *      </ul>
 * 
 *      <p>
 *      1- The authors do not know of any patent infringed by the following
 *      implementation. However, similar schemes, like WAH are covered by
 *      patents.
 *      </p>
 * 
 * @since 0.1.0
 */
public final class EWAHCompressedBitmap implements Cloneable, Externalizable,
  Iterable<Integer>, BitmapStorage, LogicalElement<EWAHCompressedBitmap> {

  /**
   * Creates an empty bitmap (no bit set to true).
   */
  public EWAHCompressedBitmap() {
    this.buffer = new long[defaultbuffersize];
    this.rlw = new RunningLengthWord(this.buffer, 0);
  }

  /**
   * Sets explicitly the buffer size (in 64-bit words). The initial memory usage
   * will be "buffersize * 64". For large poorly compressible bitmaps, using
   * large values may improve performance.
   * 
   * @param buffersize
   *          number of 64-bit words reserved when the object is created)
   */
  public EWAHCompressedBitmap(final int buffersize) {
    this.buffer = new long[buffersize];
    this.rlw = new RunningLengthWord(this.buffer, 0);
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
  public void add(final long newdata) {
    add(newdata, wordinbits);
  }

  /**
   * Adding words directly to the bitmap (for expert use).
   * 
   * @param newdata
   *          the word
   * @param bitsthatmatter
   *          the number of significant bits (by default it should be 64)
   */
  public void add(final long newdata, final int bitsthatmatter) {
    this.sizeinbits += bitsthatmatter;
    if (newdata == 0) {
      addEmptyWord(false);
    } else if (newdata == ~0l) {
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
   */
  private void addEmptyWord(final boolean v) {
    final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
    final long runlen = this.rlw.getRunningLength();
    if ((noliteralword) && (runlen == 0)) {
      this.rlw.setRunningBit(v);
    }
    if ((noliteralword) && (this.rlw.getRunningBit() == v)
      && (runlen < RunningLengthWord.largestrunninglengthcount)) {
      this.rlw.setRunningLength(runlen + 1);
      return;
    }
    push_back(0);
    this.rlw.position = this.actualsizeinwords - 1;
    this.rlw.setRunningBit(v);
    this.rlw.setRunningLength(1);
    return;
  }

  /**
   * For internal use.
   * 
   * @param newdata
   *          the dirty word
   */
  private void addLiteralWord(final long newdata) {
    final int numbersofar = this.rlw.getNumberOfLiteralWords();
    if (numbersofar >= RunningLengthWord.largestliteralcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      this.rlw.setNumberOfLiteralWords(1);
      push_back(newdata);
    }
    this.rlw.setNumberOfLiteralWords(numbersofar + 1);
    push_back(newdata);
  }

  /**
   * if you have several dirty words to copy over, this might be faster.
   * 
   * 
   * @param data
   *          the dirty words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of dirty words to add
   */
  public void addStreamOfDirtyWords(final long[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfDirtyWords(data, start + whatwecanadd, leftovernumber);
    }
    return;
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
  public void addStreamOfEmptyWords(final boolean v, long number) {
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
    final long runlen = this.rlw.getRunningLength();
    final long whatwecanadd = number < RunningLengthWord.largestrunninglengthcount
      - runlen ? number : RunningLengthWord.largestrunninglengthcount - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;
    while (number >= RunningLengthWord.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord.largestrunninglengthcount);
      number -= RunningLengthWord.largestrunninglengthcount;
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
   * Same as addStreamOfDirtyWords, but the words are negated.
   * 
   * @param data
   *          the dirty words
   * @param start
   *          the starting point in the array
   * @param number
   *          the number of dirty words to add
   */
  public void addStreamOfNegatedDirtyWords(final long[] data, final int start,
    final int number) {
    if (number == 0)
      return;
    final int NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
    final int whatwecanadd = number < RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords ? number : RunningLengthWord.largestliteralcount
      - NumberOfLiteralWords;
    this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords + whatwecanadd);
    final int leftovernumber = number - whatwecanadd;
    negative_push_back(data, start, whatwecanadd);
    this.sizeinbits += whatwecanadd * wordinbits;
    if (leftovernumber > 0) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      addStreamOfDirtyWords(data, start + whatwecanadd, leftovernumber);
    }
    return;
  }

  /**
   * Returns a new compressed bitmap containing the bitwise AND values of the
   * current bitmap with some other bitmap.
   * 
   * The running time is proportional to the sum of the compressed sizes (as
   * reported by sizeInBytes()).
   * 
   * @since 0.4.3
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  public EWAHCompressedBitmap and(final EWAHCompressedBitmap a) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
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
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void and(final EWAHCompressedBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
    }
    // at this point, this is safe:
    BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
    BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord predator = i_is_prey ? rlwj : rlwi;
      if (prey.getRunningBit() == false) {
        container.addStreamOfEmptyWords(false, prey.RunningLength);
        predator.discardFirstWords(prey.RunningLength);
        prey.RunningLength = 0;
      } else {
        // we have a stream of 1x11
        final long predatorrl = predator.getRunningLength();
        final long preyrl = prey.getRunningLength();
        final long tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.dirtywordoffset
          + (i_is_prey ? j.dirtyWords() : i.dirtyWords());
        container.addStreamOfDirtyWords(i_is_prey ? j.buffer() : i.buffer(),
          dw_predator, (int) (preyrl - tobediscarded));// cast is safe
        predator.discardFirstWords(preyrl);
        prey.RunningLength = 0;
      }
      final long predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// cast to int is safe
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfEmptyWords(false, tobediscarded);
        } else {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int dw_prey = prey.dirtywordoffset
            + (i_is_prey ? i.dirtyWords() : j.dirtyWords());
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// cast is safe
          container.addStreamOfDirtyWords(i_is_prey ? i.buffer() : j.buffer(),
            dw_prey, tobediscarded);
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
        }
      }
      // all that is left to do now is to AND the dirty words
      final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
      if (nbre_dirty_prey > 0) {
        for (int k = 0; k < nbre_dirty_prey; ++k) {
          if (i_is_prey)
            container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords() + k]
              & j.buffer()[predator.dirtywordoffset + j.dirtyWords() + k]);
          else
            container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()
              + k]
              & j.buffer()[prey.dirtywordoffset + j.dirtyWords() + k]);
        }
        predator.discardFirstWords(nbre_dirty_prey);
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
      dischargeAsEmpty(rlwi, i, container);
    if (rlwj != null)
      dischargeAsEmpty(rlwj, j, container);
    container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
  }

  /**
   * Returns the cardinality of the result of a bitwise AND of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int andCardinality(final EWAHCompressedBitmap a) {
    final BitCounter counter = new BitCounter();
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
  public EWAHCompressedBitmap andNot(final EWAHCompressedBitmap a) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
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
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the EWAH compressed bitmap
   */
  private void andNot(final EWAHCompressedBitmap a,
    final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
    }
    // at this point, this is safe:
    BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
    rlwi.setRunningBit(!rlwi.getRunningBit());
    BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord predator = i_is_prey ? rlwj : rlwi;

      if (prey.getRunningBit() == false) {
        container.addStreamOfEmptyWords(false, prey.RunningLength);
        predator.discardFirstWords(prey.RunningLength);
        prey.RunningLength = 0;
      } else {
        // we have a stream of 1x11
        final long predatorrl = predator.getRunningLength();
        final long preyrl = prey.getRunningLength();
        final long tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.dirtywordoffset
          + (i_is_prey ? j.dirtyWords() : i.dirtyWords());
        if (i_is_prey)
          container.addStreamOfDirtyWords(j.buffer(), dw_predator,
            (int) (preyrl - tobediscarded));// cast is safe
        else
          container.addStreamOfNegatedDirtyWords(i.buffer(), dw_predator,
            (int) (preyrl - tobediscarded));// cast is safe
        predator.discardFirstWords(preyrl);
        prey.RunningLength = 0;
      }
      final long predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl; // cast to int is safe
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfEmptyWords(false, tobediscarded);
        } else {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int dw_prey = prey.dirtywordoffset
            + (i_is_prey ? i.dirtyWords() : j.dirtyWords());
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl; // cast is safe
          if (i_is_prey)
            container.addStreamOfNegatedDirtyWords(i.buffer(), dw_prey,
              tobediscarded);
          else
            container.addStreamOfDirtyWords(j.buffer(), dw_prey, tobediscarded);
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
        }
      }
      // all that is left to do now is to AND the dirty words
      final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
      if (nbre_dirty_prey > 0) {
        for (int k = 0; k < nbre_dirty_prey; ++k) {
          if (i_is_prey)
            container.add((~i.buffer()[prey.dirtywordoffset + i.dirtyWords()
              + k])
              & j.buffer()[predator.dirtywordoffset + j.dirtyWords() + k]);
          else
            container.add((~i.buffer()[predator.dirtywordoffset
              + i.dirtyWords() + k])
              & j.buffer()[prey.dirtywordoffset + j.dirtyWords() + k]);
        }
        predator.discardFirstWords(nbre_dirty_prey);
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
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int andNotCardinality(final EWAHCompressedBitmap a) {
    final BitCounter counter = new BitCounter();
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        counter += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        counter += Long.bitCount(i.buffer()[i.dirtyWords() + j]);
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
    final EWAHCompressedBitmap clone = (EWAHCompressedBitmap) super.clone();
    clone.buffer = this.buffer.clone();
    clone.rlw = new RunningLengthWord(clone.buffer, this.rlw.position);
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
      this.buffer = new long[this.actualsizeinwords];
    }
    for (int k = 0; k < this.actualsizeinwords; ++k)
      this.buffer[k] = in.readLong();
    this.rlw = new RunningLengthWord(this.buffer, in.readInt());
  }

  /**
   * Check to see whether the two compressed bitmaps contain the same set bits.
   * 
   * @author Colby Ranger
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof EWAHCompressedBitmap) {
      try {
        this.xor((EWAHCompressedBitmap) o, new NonEmptyVirtualStorage());
        return true;
      } catch (NonEmptyVirtualStorage.NonEmptyException e) {
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
   * @return nothing
   */
  private void fastaddStreamOfEmptyWords(final boolean v, long number) {
    if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
      this.rlw.setRunningBit(v);
    } else if ((this.rlw.getNumberOfLiteralWords() != 0)
      || (this.rlw.getRunningBit() != v)) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
    }

    final long runlen = this.rlw.getRunningLength();
    final long whatwecanadd = number < RunningLengthWord.largestrunninglengthcount
      - runlen ? number : RunningLengthWord.largestrunninglengthcount - runlen;
    this.rlw.setRunningLength(runlen + whatwecanadd);
    number -= whatwecanadd;

    while (number >= RunningLengthWord.largestrunninglengthcount) {
      push_back(0);
      this.rlw.position = this.actualsizeinwords - 1;
      if (v)
        this.rlw.setRunningBit(v);
      this.rlw.setRunningLength(RunningLengthWord.largestrunninglengthcount);
      number -= RunningLengthWord.largestrunninglengthcount;
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
  private EWAHIterator getEWAHIterator() {
    return new EWAHIterator(this.buffer, this.actualsizeinwords);
  }

  /**
   * get the locations of the true values as one vector. (may use more memory
   * than iterator())
   * 
   * @return the positions
   */
  public List<Integer> getPositions() {
    final ArrayList<Integer> v = new ArrayList<Integer>();
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    int pos = 0;
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        for (int j = 0; j < localrlw.getRunningLength(); ++j) {
          for (int c = 0; c < wordinbits; ++c)
            v.add(new Integer(pos++));
        }
      } else {
        pos += wordinbits * localrlw.getRunningLength();
      }
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.dirtyWords() + j];
        while (data != 0) {
          final int ntz = Long.numberOfTrailingZeros(data);
          data ^= (1l << ntz);
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while( i.hasNext() ) {
      i.next();
      if (i.rlw.getRunningBit() == true) {
        karprabin += B * karprabin
          + (i.rlw.getRunningLength() & ((1l << 32) - 1));
        karprabin += B * karprabin + (i.rlw.getRunningLength() >>> 32);
      }
      for (int k = 0; k <  i.rlw.getNumberOfLiteralWords(); ++k) {
        karprabin += B * karprabin + (this.buffer[i.dirtyWords() + k] & ((1l << 32) - 1));
        karprabin += B * karprabin + (this.buffer[i.dirtyWords() + k] >>> 32);
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
   * @since 0.3.2
   * @param a
   *          the other bitmap
   * @return whether they intersect
   */
  public boolean intersects(final EWAHCompressedBitmap a) {
    NonEmptyVirtualStorage nevs = new NonEmptyVirtualStorage();
    try {
      this.and(a, nevs);
    } catch (NonEmptyVirtualStorage.NonEmptyException nee) {
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
    return new IntIteratorImpl(
        new EWAHIterator(this.buffer, this.actualsizeinwords));
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
  private void negative_push_back(final long[] data, final int start,
    final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    if (!i.hasNext())
      return;
    while (true) {
      final RunningLengthWord rlw1 = i.next();
      rlw1.setRunningBit(!rlw1.getRunningBit());
      for (int j = 0; j < rlw1.getNumberOfLiteralWords(); ++j) {
        i.buffer()[i.dirtyWords() + j] = ~i.buffer()[i.dirtyWords() + j];
      }
      if (!i.hasNext()) {// must potentially adjust the last dirty word
        if (rlw1.getNumberOfLiteralWords() == 0)
          return;
        final int usedbitsinlast = this.sizeinbits % wordinbits;
        if (usedbitsinlast == 0)
          return;
        i.buffer()[i.dirtyWords() + rlw1.getNumberOfLiteralWords() - 1] &= ((~0l) >>> (wordinbits - usedbitsinlast));
        return;
      }
    }
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
  public EWAHCompressedBitmap or(final EWAHCompressedBitmap a) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
    container.reserve(this.actualsizeinwords + a.actualsizeinwords);
    or(a, container);
    return container;
  }

  /**
   * Computes the bitwise or between the current bitmap and the bitmap "a".
   * Stores the result in the container.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void or(final EWAHCompressedBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
      return;
    }
    // at this point, this is safe:
    BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
    BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
    // RunningLength;
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord predator = i_is_prey ? rlwj : rlwi;
      if (prey.getRunningBit() == false) {
        final long predatorrl = predator.getRunningLength();
        final long preyrl = prey.getRunningLength();
        final long tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.dirtywordoffset
          + (i_is_prey ? j.dirtyWords() : i.dirtyWords());
        container.addStreamOfDirtyWords(i_is_prey ? j.buffer() : i.buffer(),
          dw_predator, (int) (preyrl - tobediscarded));// cast is safe
        predator.discardFirstWords(preyrl);
        prey.discardFirstWords(preyrl);
        prey.RunningLength = 0;
      } else {
        // we have a stream of 1x11
        container.addStreamOfEmptyWords(true, prey.RunningLength);
        predator.discardFirstWords(prey.RunningLength);
        prey.RunningLength = 0;
      }
      long predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// the cast to int is safe here
          final int dw_prey = prey.dirtywordoffset
            + (i_is_prey ? i.dirtyWords() : j.dirtyWords());
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfDirtyWords(i_is_prey ? i.buffer() : j.buffer(),
            dw_prey, tobediscarded);
        } else {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// cast to int is safe
          container.addStreamOfEmptyWords(true, tobediscarded);
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
        }
      }
      // all that is left to do now is to OR the dirty words
      final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
      if (nbre_dirty_prey > 0) {
        for (int k = 0; k < nbre_dirty_prey; ++k) {
          if (i_is_prey)
            container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords() + k]
              | j.buffer()[predator.dirtywordoffset + j.dirtyWords() + k]);
          else
            container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()
              + k]
              | j.buffer()[prey.dirtywordoffset + j.dirtyWords() + k]);
        }
        predator.discardFirstWords(nbre_dirty_prey);
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
   * Returns the cardinality of the result of a bitwise OR of the values of the
   * current bitmap with some other bitmap. Avoids needing to allocate an
   * intermediate bitmap to hold the result of the OR.
   * 
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int orCardinality(final EWAHCompressedBitmap a) {
    final BitCounter counter = new BitCounter();
    or(a, counter);
    return counter.getCount();
  }

  /**
   * For internal use.
   * 
   * @param data
   *          the word to be added
   */
  private void push_back(final long data) {
    if (this.actualsizeinwords == this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
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
  private void push_back(final long[] data, final int start, final int number) {
    while (this.actualsizeinwords + number >= this.buffer.length) {
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[oldbuffer.length * 2];
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
      final long oldbuffer[] = this.buffer;
      this.buffer = new long[size];
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
      out.writeLong(this.buffer[k]);
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
   *           if i is negative or greater than Integer.MAX_VALUE - 64
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
      addLiteralWord(1l << (i % wordinbits));
      return true;
    }
    if (this.rlw.getNumberOfLiteralWords() == 0) {
      this.rlw.setRunningLength(this.rlw.getRunningLength() - 1);
      addLiteralWord(1l << (i % wordinbits));
      return true;
    }
    this.buffer[this.actualsizeinwords - 1] |= 1l << (i % wordinbits);
    if (this.buffer[this.actualsizeinwords - 1] == ~0l) {
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
   * @since 0.4.0
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
      final long newdata = (1l << leftover) + ((1l << leftover) - 1);
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
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
        long data = i.buffer()[i.dirtyWords() + j];
        if (!usetrailingzeros) {
          for (int c = 0; c < wordinbits; ++c) {
            if ((data & (1l << c)) != 0)
              ans[inanspos++] = c + pos;
          }
          pos += wordinbits;
        } else {
          while (data != 0) {
            final int ntz = Long.numberOfTrailingZeros(data);
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
      if (localrlw.getRunningBit()) {
        ans += localrlw.getRunningLength() + " 1x11\n";
      } else {
        ans += localrlw.getRunningLength() + " 0x00\n";
      }
      ans += localrlw.getNumberOfLiteralWords() + " dirties\n";
      for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
        long data = i.buffer()[i.dirtyWords() + j];
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
    final EWAHIterator i = new EWAHIterator(this.buffer, this.actualsizeinwords);
    while (i.hasNext()) {
      RunningLengthWord localrlw = i.next();
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
  public EWAHCompressedBitmap xor(final EWAHCompressedBitmap a) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
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
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @param container
   *          where we store the result
   */
  private void xor(final EWAHCompressedBitmap a, final BitmapStorage container) {
    final EWAHIterator i = a.getEWAHIterator();
    final EWAHIterator j = getEWAHIterator();
    if (!(i.hasNext() && j.hasNext())) {// this never happens...
      container.setSizeInBits(sizeInBits());
    }
    // at this point, this is safe:
    BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
    BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
    while (true) {
      final boolean i_is_prey = rlwi.size() < rlwj.size();
      final BufferedRunningLengthWord prey = i_is_prey ? rlwi : rlwj;
      final BufferedRunningLengthWord predator = i_is_prey ? rlwj : rlwi;
      if (prey.getRunningBit() == false) {
        final long predatorrl = predator.getRunningLength();
        final long preyrl = prey.getRunningLength();
        final long tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container
          .addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
        final int dw_predator = predator.dirtywordoffset
          + (i_is_prey ? j.dirtyWords() : i.dirtyWords());
        container.addStreamOfDirtyWords(i_is_prey ? j.buffer() : i.buffer(),
          dw_predator, (int) (preyrl - tobediscarded)); // cast is safe
        predator.discardFirstWords(preyrl);
        prey.discardFirstWords(preyrl);
      } else {
        // we have a stream of 1x11
        final long predatorrl = predator.getRunningLength();
        final long preyrl = prey.getRunningLength();
        final long tobediscarded = (predatorrl >= preyrl) ? preyrl : predatorrl;
        container.addStreamOfEmptyWords(!predator.getRunningBit(),
          tobediscarded);
        final int dw_predator = predator.dirtywordoffset
          + (i_is_prey ? j.dirtyWords() : i.dirtyWords());
        final long[] buf = i_is_prey ? j.buffer() : i.buffer();
        for (int k = 0; k < preyrl - tobediscarded; ++k)
          container.add(~buf[k + dw_predator]);
        predator.discardFirstWords(preyrl);
        prey.discardFirstWords(preyrl);
      }
      final long predatorrl = predator.getRunningLength();
      if (predatorrl > 0) {
        if (predator.getRunningBit() == false) {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// the cast to int is safe
          final int dw_prey = prey.dirtywordoffset
            + (i_is_prey ? i.dirtyWords() : j.dirtyWords());
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          container.addStreamOfDirtyWords(i_is_prey ? i.buffer() : j.buffer(),
            dw_prey, tobediscarded);
        } else {
          final int nbre_dirty_prey = prey.getNumberOfLiteralWords();
          final int tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey
            : (int) predatorrl;// cast to (int) is safe
          final int dw_prey = prey.dirtywordoffset
            + (i_is_prey ? i.dirtyWords() : j.dirtyWords());
          predator.discardFirstWords(tobediscarded);
          prey.discardFirstWords(tobediscarded);
          final long[] buf = i_is_prey ? i.buffer() : j.buffer();
          for (int k = 0; k < tobediscarded; ++k)
            container.add(~buf[k + dw_prey]);
        }
      }
      // all that is left to do now is to AND the dirty words
      final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
      if (nbre_dirty_prey > 0) {
        for (int k = 0; k < nbre_dirty_prey; ++k) {
          if (i_is_prey)
            container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords() + k]
              ^ j.buffer()[predator.dirtywordoffset + j.dirtyWords() + k]);
          else
            container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()
              + k]
              ^ j.buffer()[prey.dirtywordoffset + j.dirtyWords() + k]);
        }
        predator.discardFirstWords(nbre_dirty_prey);
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
   * @since 0.4.0
   * @param a
   *          the other bitmap
   * @return the cardinality
   */
  public int xorCardinality(final EWAHCompressedBitmap a) {
    final BitCounter counter = new BitCounter();
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
   * @since 0.4.3
   */
  private static void and(final BitmapStorage container,
    final EWAHCompressedBitmap... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].and(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in ascending order by sizeinbits. When we exhaust the
    // first bitmap the rest
    // of the result is zeros.
    final EWAHCompressedBitmap[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<EWAHCompressedBitmap>() {
      public int compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) {
        return a.sizeinbits < b.sizeinbits ? -1
          : a.sizeinbits == b.sizeinbits ? 0 : 1;
      }
    });

    int maxSize = sortedBitmaps[sortedBitmaps.length - 1].sizeinbits;

    final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
    for (int i = 0; i < sortedBitmaps.length; i++) {
      EWAHIterator iterator = sortedBitmaps[i].getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[i] = new IteratingBufferedRunningLengthWord(iterator);
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
      long maxZeroRl = 0;
      long minOneRl = Long.MAX_VALUE;
      long minSize = Long.MAX_VALUE;
      int numEmptyRl = 0;

      if (rlws[0].size() == 0) {
        extendEmptyBits(container, sortedBitmaps[0].sizeinbits, maxSize);
        break;
      }

      for (IteratingBufferedRunningLengthWord rlw : rlws) {
        long size = rlw.size();
        minSize = Math.min(minSize, size);

        if (!rlw.getRunningBit()) {
          long rl = rlw.getRunningLength();
          maxZeroRl = Math.max(maxZeroRl, rl);
          minOneRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          long rl = rlw.getRunningLength();
          minOneRl = Math.min(minOneRl, rl);
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        }
      }

      if (maxZeroRl > 0) {
        container.addStreamOfEmptyWords(false, maxZeroRl);
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
          rlw.discardFirstWords(maxZeroRl);
        }
      } else if (minOneRl > 0) {
        container.addStreamOfEmptyWords(true, minOneRl);
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
          rlw.discardFirstWords(minOneRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has dirty words to process and the rest have a run of
          // 1's we can write them out here
          IteratingBufferedRunningLengthWord emptyRl = null;
          long minNonEmptyRl = Long.MAX_VALUE;
          for (IteratingBufferedRunningLengthWord rlw : rlws) {
            long rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          long wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeDirtyWords((int) wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          long word = ~0l;
          for (IteratingBufferedRunningLengthWord rlw : rlws) {
            if (rlw.getRunningLength() <= index) {
              word &= rlw.getDirtyWordAt(index - (int) rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (IteratingBufferedRunningLengthWord rlw : rlws) {
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
   * @since 0.4.3
   * @param bitmaps
   *          bitmaps to AND together
   * @return result of the AND
   */
  public static EWAHCompressedBitmap and(final EWAHCompressedBitmap... bitmaps) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
    int largestSize = 0;
    for (EWAHCompressedBitmap bitmap : bitmaps) {
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
   * @since 0.4.3
   * @param bitmaps
   *          bitmaps to AND
   * @return the cardinality
   */
  public static int andCardinality(final EWAHCompressedBitmap... bitmaps) {
    final BitCounter counter = new BitCounter();
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
  public static EWAHCompressedBitmap bitmapOf(int ... setbits) {
    EWAHCompressedBitmap a = new EWAHCompressedBitmap();
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
  protected static void discharge(final BufferedRunningLengthWord initialWord,
    final EWAHIterator iterator, final BitmapStorage container) {
    BufferedRunningLengthWord runningLengthWord = initialWord;
    for (;;) {
      final long runningLength = runningLengthWord.getRunningLength();
      container.addStreamOfEmptyWords(runningLengthWord.getRunningBit(),
        runningLength);
      container.addStreamOfDirtyWords(iterator.buffer(), iterator.dirtyWords()
        + runningLengthWord.dirtywordoffset,
        runningLengthWord.getNumberOfLiteralWords());
      if (!iterator.hasNext())
        break;
      runningLengthWord = new BufferedRunningLengthWord(iterator.next());
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
    final BufferedRunningLengthWord initialWord, final EWAHIterator iterator,
    final BitmapStorage container) {
    BufferedRunningLengthWord runningLengthWord = initialWord;
    for (;;) {
      final long runningLength = runningLengthWord.getRunningLength();
      container.addStreamOfEmptyWords(false,
        runningLength + runningLengthWord.getNumberOfLiteralWords());
      if (!iterator.hasNext())
        break;
      runningLengthWord = new BufferedRunningLengthWord(iterator.next());
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
   * @since 0.4.3
   */
  private static void extendEmptyBits(final BitmapStorage storage,
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
   * 
   * @since 0.4.0
   */
  private static void or(final BitmapStorage container,
    final EWAHCompressedBitmap... bitmaps) {
    if (bitmaps.length == 2) {
      // should be more efficient
      bitmaps[0].or(bitmaps[1], container);
      return;
    }

    // Sort the bitmaps in descending order by sizeinbits. We will exhaust the
    // sorted bitmaps from right to left.
    final EWAHCompressedBitmap[] sortedBitmaps = bitmaps.clone();
    Arrays.sort(sortedBitmaps, new Comparator<EWAHCompressedBitmap>() {
      public int compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) {
        return a.sizeinbits < b.sizeinbits ? 1
          : a.sizeinbits == b.sizeinbits ? 0 : -1;
      }
    });

    final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
    int maxAvailablePos = 0;
    for (EWAHCompressedBitmap bitmap : sortedBitmaps) {
      EWAHIterator iterator = bitmap.getEWAHIterator();
      if (iterator.hasNext()) {
        rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord(
          iterator);
      }
    }

    if (maxAvailablePos == 0) { // this never happens...
      container.setSizeInBits(0);
      return;
    }

    int maxSize = sortedBitmaps[0].sizeinbits;

    while (true) {
      long maxOneRl = 0;
      long minZeroRl = Long.MAX_VALUE;
      long minSize = Long.MAX_VALUE;
      int numEmptyRl = 0;
      for (int i = 0; i < maxAvailablePos; i++) {
        IteratingBufferedRunningLengthWord rlw = rlws[i];
        long size = rlw.size();
        if (size == 0) {
          maxAvailablePos = i;
          break;
        }
        minSize = Math.min(minSize, size);

        if (rlw.getRunningBit()) {
          long rl = rlw.getRunningLength();
          maxOneRl = Math.max(maxOneRl, rl);
          minZeroRl = 0;
          if (rl == 0 && size > 0) {
            numEmptyRl++;
          }
        } else {
          long rl = rlw.getRunningLength();
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
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(maxOneRl);
        }
      } else if (minZeroRl > 0) {
        container.addStreamOfEmptyWords(false, minZeroRl);
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(minZeroRl);
        }
      } else {
        int index = 0;

        if (numEmptyRl == 1) {
          // if one rlw has dirty words to process and the rest have a run of
          // 0's we can write them out here
          IteratingBufferedRunningLengthWord emptyRl = null;
          long minNonEmptyRl = Long.MAX_VALUE;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord rlw = rlws[i];
            long rl = rlw.getRunningLength();
            if (rl == 0) {
              assert emptyRl == null;
              emptyRl = rlw;
            } else {
              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
            }
          }
          long wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
          if (emptyRl != null)
            emptyRl.writeDirtyWords((int) wordsToWrite, container);
          index += wordsToWrite;
        }

        while (index < minSize) {
          long word = 0;
          for (int i = 0; i < maxAvailablePos; i++) {
            IteratingBufferedRunningLengthWord rlw = rlws[i];
            if (rlw.getRunningLength() <= index) {
              word |= rlw.getDirtyWordAt(index - (int) rlw.getRunningLength());
            }
          }
          container.add(word);
          index++;
        }
        for (int i = 0; i < maxAvailablePos; i++) {
          IteratingBufferedRunningLengthWord rlw = rlws[i];
          rlw.discardFirstWords(minSize);
        }
      }
    }
    container.setSizeInBits(maxSize);
  }

  /**
   * Returns a new compressed bitmap containing the bitwise OR values of the
   * provided bitmaps.
   * 
   * @since 0.4.0
   * @param bitmaps
   *          bitmaps to OR together
   * @return result of the OR
   */
  public static EWAHCompressedBitmap or(final EWAHCompressedBitmap... bitmaps) {
    final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
    int largestSize = 0;
    for (EWAHCompressedBitmap bitmap : bitmaps) {
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
   * @since 0.4.0
   * @param bitmaps
   *          bitmaps to OR
   * @return the cardinality
   */
  public static int orCardinality(final EWAHCompressedBitmap... bitmaps) {
    final BitCounter counter = new BitCounter();
    or(counter, bitmaps);
    return counter.getCount();
  }

  /** The actual size in words. */
  int actualsizeinwords = 1;

  /** The buffer (array of 64-bit words) */
  long buffer[] = null;

  /** The current (last) running length word. */
  RunningLengthWord rlw = null;

  /** sizeinbits: number of bits in the (uncompressed) bitmap. */
  int sizeinbits = 0;

  /**
   * The Constant defaultbuffersize: default memory allocation when the object
   * is constructed.
   */
  static final int defaultbuffersize = 4;

  /** optimization option **/
  public static final boolean usetrailingzeros = true;

  /** The Constant wordinbits represents the number of bits in a long. */
  public static final int wordinbits = 64;

}
