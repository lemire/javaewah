package com.googlecode.javaewah32;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah.LogicalElement;
import com.googlecode.javaewah32.symmetric.RunningBitmapMerge32;
import com.googlecode.javaewah32.symmetric.ThresholdFuncBitmap32;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * This implements the patent-free EWAH scheme. Roughly speaking, it is a 32-bit
 * variant of the BBC compression scheme used by Oracle for its bitmap indexes.
 * </p>
 * 
 * <p>
 * In contrast with the 64-bit EWAH scheme (javaewah.EWAHCompressedBitmap), you
 * can expect this class to compress better, but to be slower at processing the
 * data. In effect, there is a trade-off between memory usage and performances.
 * </p>
 * 
 * <p>Here is a code sample to illustrate usage:</p>
 * <pre>
 * EWAHCompressedBitmap32 ewahBitmap1 = EWAHCompressedBitmap32.bitmapOf(0, 2, 55, 64,
 *         1 &lt;&lt; 30);
 * EWAHCompressedBitmap32 ewahBitmap2 = EWAHCompressedBitmap32.bitmapOf(1, 3, 64,
 *         1 &lt;&lt; 30);
 * EWAHCompressedBitmap32 ewahBitmap3 = EWAHCompressedBitmap32
 *         .bitmapOf(5, 55, 1 &lt;&lt; 30);
 * EWAHCompressedBitmap32 ewahBitmap4 = EWAHCompressedBitmap32
 *         .bitmapOf(4, 66, 1 &lt;&lt; 30);
 * EWAHCompressedBitmap32 orBitmap = ewahBitmap1.or(ewahBitmap2);
 * EWAHCompressedBitmap32 andbitmap = ewahBitmap1.and(ewahBitmap2);
 * EWAHCompressedBitmap32 xorbitmap = ewahBitmap1.xor(ewahBitmap2);
 * andbitmap = EWAHCompressedBitmap32.and(ewahBitmap1, ewahBitmap2, ewahBitmap3,
 *         ewahBitmap4);
 * ByteArrayOutputStream bos = new ByteArrayOutputStream();
 * ObjectOutputStream oo = new ObjectOutputStream(bos);
 * ewahBitmap1.writeExternal(oo);
 * oo.close();
 * ewahBitmap1 = null;
 * ewahBitmap1 = new EWAHCompressedBitmap32();
 * ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
 * ewahBitmap1.readExternal(new ObjectInputStream(bis));
 * EWAHCompressedBitmap32 threshold2 = EWAHCompressedBitmap32.threshold(2,
 *         ewahBitmap1, ewahBitmap2, ewahBitmap3, ewahBitmap4);
 * </pre>
 * 
 * <p>
 * The objective of this compression type is to provide some compression, while
 * reducing as much as possible the CPU cycle usage.
 * </p>
 * 
 * <p>
 * Once constructed, the bitmap is essentially immutable (unless you call the
 * "set" or "add" methods). Thus, it can be safely used in multi-threaded
 * programs.
 * </p>
 * <p>
 * For more details, see the following papers:
 * </p>
 * 
 * <ul>
 * <li>Daniel Lemire, Owen Kaser, Kamel Aouiche, <a
 * href="http://arxiv.org/abs/0901.3751">Sorting improves word-aligned bitmap
 * indexes</a>. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.</li>
 * <li> Owen Kaser and Daniel Lemire, Compressed bitmap indexes: beyond unions and intersections
 * <a href="http://arxiv.org/abs/1402.4466">http://arxiv.org/abs/1402.4466</a></li>
 * </ul>
 *
 * @see com.googlecode.javaewah.EWAHCompressedBitmap EWAHCompressedBitmap
 * @since 0.5.0
 */
public final class EWAHCompressedBitmap32 implements Cloneable, Externalizable,
        Iterable<Integer>, BitmapStorage32,
        LogicalElement<EWAHCompressedBitmap32> {

    /**
     * Creates an empty bitmap (no bit set to true).
     */
    public EWAHCompressedBitmap32() {
        this.buffer = new int[DEFAULT_BUFFER_SIZE];
        this.rlw = new RunningLengthWord32(this, 0);
    }

    /**
     * Sets explicitly the buffer size (in 32-bit words). The initial memory
     * usage will be "bufferSize * 32". For large poorly compressible
     * bitmaps, using large values may improve performance.
     *
     * @param bufferSize number of 32-bit words reserved when the object is
     *                   created)
     */
    public EWAHCompressedBitmap32(final int bufferSize) {
        this.buffer = new int[bufferSize];
        this.rlw = new RunningLengthWord32(this, 0);

    }

    /**
     * @param newData the word
     * @deprecated use addWord() instead.
     */
    @Deprecated
    public void add(final int newData) {
        addWord(newData);
    }

    /**
     * @param newData        the word
     * @param bitsThatMatter the number of significant bits (by default it should
     *                       be 64)
     * @deprecated use addWord() instead.
     */
    @Deprecated
    public void add(final int newData, final int bitsThatMatter) {
        addWord(newData, bitsThatMatter);
    }


    /**
     * Adding words directly to the bitmap (for expert use).
     * 
     * This method adds bits in words of 4*8 bits. It is not to
     * be confused with the set method which sets individual bits.
     * 
     * Most users will want the set method.
     * 
     * Example: if you add word 321 to an empty bitmap, you are have
     * added (in binary notation) 0b101000001, so you have effectively
     * called set(0), set(6), set(8) in sequence.
     * 
     * Since this modifies the bitmap, this method is not thread-safe.
     * 
     * API change: prior to version 0.8.3, this method was called add.
     *
     * @param newData the word
     */
    @Override
    public void addWord(final int newData) {
        addWord(newData, wordinbits);
    }

    /**
     * Adding words directly to the bitmap (for expert use). Since this
     * modifies the bitmap, this method is not thread-safe.
     * 
     * API change: prior to version 0.8.3, this method was called add.
     *
     * @param newData        the word
     * @param bitsThatMatter the number of significant bits (by default it should
     *                       be 32)
     */
    public void addWord(final int newData, final int bitsThatMatter) {
        this.sizeInBits += bitsThatMatter;
        if (newData == 0) {
            addEmptyWord(false);
        } else if (newData == ~0) {
            addEmptyWord(true);
        } else {
            addLiteralWord(newData);
        }
    }

    /**
     * For internal use.
     *
     * @param v the boolean value
     * @return the storage cost of the addition
     */
    private int addEmptyWord(final boolean v) {
        final boolean noliteralword = (this.rlw
                .getNumberOfLiteralWords() == 0);
        final int runlen = this.rlw.getRunningLength();
        if ((noliteralword) && (runlen == 0)) {
            this.rlw.setRunningBit(v);
        }
        if ((noliteralword)
                && (this.rlw.getRunningBit() == v)
                && (runlen < RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT)) {
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
     * @param newData the literal word
     * @return the storage cost of the addition
     */
    private int addLiteralWord(final int newData) {
    	final int numbersofar = this.rlw.getNumberOfLiteralWords();
        if (numbersofar >= RunningLengthWord32.LARGEST_LITERAL_COUNT) {
            push_back(0);
            this.rlw.position = this.actualsizeinwords - 1;
            this.rlw.setNumberOfLiteralWords(1);
            push_back(newData);
            return 2;
        }
        this.rlw.setNumberOfLiteralWords(numbersofar + 1);
        push_back(newData);
        return 1;
    }

    /**
     * if you have several literal words to copy over, this might be faster.
     * 
     * Since this modifies the bitmap, this method is not thread-safe.
     *
     * @param data   the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    @Override
    public void addStreamOfLiteralWords(final int[] data, final int start,
                                        final int number) {
        int leftovernumber = number;
        while (leftovernumber > 0) {
            final int numberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
            final int whatWeCanAdd = leftovernumber < RunningLengthWord32.LARGEST_LITERAL_COUNT
                    - numberOfLiteralWords ? leftovernumber
                    : RunningLengthWord32.LARGEST_LITERAL_COUNT
                    - numberOfLiteralWords;
            this.rlw.setNumberOfLiteralWords(numberOfLiteralWords + whatWeCanAdd);
            leftovernumber -= whatWeCanAdd;
            push_back(data, start, whatWeCanAdd);
            this.sizeInBits += whatWeCanAdd * wordinbits;
            if (leftovernumber > 0) {
                push_back(0);
                this.rlw.position = this.actualsizeinwords - 1;
            }
        }
    }

    /**
     * For experts: You want to add many zeroes or ones? This is the method
     * you use.
     * 
     * Since this modifies the bitmap, this method is not thread-safe.
     *
     * @param v      the boolean value
     * @param number the number
     */
    @Override
    public void addStreamOfEmptyWords(final boolean v, int number) {
        if (number == 0)
            return;
        this.sizeInBits += number * wordinbits;
        if ((this.rlw.getRunningBit() != v) && (this.rlw.size() == 0)) {
            this.rlw.setRunningBit(v);
        } else if ((this.rlw.getNumberOfLiteralWords() != 0)
                || (this.rlw.getRunningBit() != v)) {
            push_back(0);
            this.rlw.position = this.actualsizeinwords - 1;
            if (v)
                this.rlw.setRunningBit(true);
        }
        final int runLen = this.rlw.getRunningLength();
        final int whatWeCanAdd = number < RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT
                - runLen ? number
                : RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT
                - runLen;
        this.rlw.setRunningLength(runLen + whatWeCanAdd);
        number -= whatWeCanAdd;
        while (number >= RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT) {
            push_back(0);
            this.rlw.position = this.actualsizeinwords - 1;
            if (v)
                this.rlw.setRunningBit(true);
            this.rlw.setRunningLength(RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT);
            number -= RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT;
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
     * Since this modifies the bitmap, this method is not thread-safe.
     *
     * @param data   the literal words
     * @param start  the starting point in the array
     * @param number the number of literal words to add
     */
    @Override
    public void addStreamOfNegatedLiteralWords(final int[] data,
                                               final int start, final int number) {
        int leftovernumber = number;
        while (leftovernumber > 0) {
            final int NumberOfLiteralWords = this.rlw
                    .getNumberOfLiteralWords();
            final int whatwecanadd = leftovernumber < RunningLengthWord32.LARGEST_LITERAL_COUNT
                    - NumberOfLiteralWords ? leftovernumber
                    : RunningLengthWord32.LARGEST_LITERAL_COUNT
                    - NumberOfLiteralWords;
            this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords
                    + whatwecanadd);
            leftovernumber -= whatwecanadd;
            negative_push_back(data, start, whatwecanadd);
            this.sizeInBits += whatwecanadd * wordinbits;
            if (leftovernumber > 0) {
                push_back(0);
                this.rlw.position = this.actualsizeinwords - 1;
            }
        }
    }

    /**
     * Returns a new compressed bitmap containing the bitwise AND values of
     * the current bitmap with some other bitmap. The current bitmap
     * is not modified.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     *
     * @param a the other bitmap (it will not be modified)
     * @return the EWAH compressed bitmap
     */
    @Override
    public EWAHCompressedBitmap32 and(final EWAHCompressedBitmap32 a) {
        int size = this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
                : a.actualsizeinwords;
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32(size);
        andToContainer(a, container);
        return container;
    }

    /**
     * Computes new compressed bitmap containing the bitwise AND values of
     * the current bitmap with some other bitmap.
     * The current bitmap is not modified.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * The content of the container is overwritten.
     *
     * @param a         the other bitmap  (it will not be modified)
     * @param container where we store the result
     * @since 0.4.0
     */
    public void andToContainer(final EWAHCompressedBitmap32 a,
                               final BitmapStorage32 container) {
        container.clear();
        final EWAHIterator32 i = a.getEWAHIterator();
        final EWAHIterator32 j = getEWAHIterator();
        final IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(
                i);
        final IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(
                j);
        while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
            while ((rlwi.getRunningLength() > 0)
                    || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi
                        .getRunningLength() < rlwj
                        .getRunningLength();
                final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
                        : rlwj;
                final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
                        : rlwi;
                if (!predator.getRunningBit()) {
                    container.addStreamOfEmptyWords(false,
                            predator.getRunningLength());
                    prey.discardFirstWords(predator
                            .getRunningLength());
                } else {
                    final int index = prey.discharge(
                            container,
                            predator.getRunningLength());
                    container.addStreamOfEmptyWords(false,
                            predator.getRunningLength()
                                    - index
                    );
                }
                predator.discardRunningWords();
            }
            final int nbre_literal = Math.min(
                    rlwi.getNumberOfLiteralWords(),
                    rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k)
                            & rlwj.getLiteralWordAt(k));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
        if (adjustContainerSizeWhenAggregating) {
            final boolean i_remains = rlwi.size() > 0;
            final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi
                    : rlwj;
            remaining.dischargeAsEmpty(container);
            container.setSizeInBits(Math.max(sizeInBits(),
                    a.sizeInBits()));
        }
    }

    /**
     * Returns the cardinality of the result of a bitwise AND of the values
     * of the current bitmap with some other bitmap. Avoids
     * allocating an intermediate bitmap to hold the result of the OR.
     * The current bitmap is not modified.
     *
     * @param a the other bitmap  (it will not be modified)
     * @return the cardinality
     */
    public int andCardinality(final EWAHCompressedBitmap32 a) {
        final BitCounter32 counter = new BitCounter32();
        andToContainer(a, counter);
        return counter.getCount();
    }

    /**
     * Returns a new compressed bitmap containing the bitwise AND NOT values
     * of the current bitmap with some other bitmap. The current bitmap
     * is not modified.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     *
     * @param a the other bitmap  (it will not be modified)
     * @return the EWAH compressed bitmap
     */
    @Override
    public EWAHCompressedBitmap32 andNot(final EWAHCompressedBitmap32 a) {
        int size = this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
                : a.actualsizeinwords;
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32(size);
        andNotToContainer(a, container);
        return container;
    }

    /**
     * Returns a new compressed bitmap containing the bitwise AND NOT values
     * of the current bitmap with some other bitmap. The current bitmap
     * is not modified.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * The content of the container is overwritten.
     *
     * @param a         the other bitmap  (it will not be modified)
     * @param container where we store the result
     */
    public void andNotToContainer(final EWAHCompressedBitmap32 a,
                                  final BitmapStorage32 container) {
        container.clear();
        final EWAHIterator32 i = getEWAHIterator();
        final EWAHIterator32 j = a.getEWAHIterator();
        final IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(
                i);
        final IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(
                j);
        while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
            while ((rlwi.getRunningLength() > 0)
                    || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi
                        .getRunningLength() < rlwj
                        .getRunningLength();
                final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
                        : rlwj;
                final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
                        : rlwi;
                if (((predator.getRunningBit()) && (i_is_prey))
                        || ((!predator.getRunningBit()) && (!i_is_prey))) {
                    container.addStreamOfEmptyWords(false,
                            predator.getRunningLength());
                    prey.discardFirstWords(predator
                            .getRunningLength());
                } else if (i_is_prey) {
                    final int index = prey.discharge(container,
                            predator.getRunningLength());
                    container.addStreamOfEmptyWords(false,
                            predator.getRunningLength()
                                    - index
                    );
                } else {
                    final int index = prey.dischargeNegated(
                            container,
                            predator.getRunningLength());
                    container.addStreamOfEmptyWords(true,
                            predator.getRunningLength()
                                    - index
                    );
                }
                predator.discardRunningWords();
            }
            final int nbre_literal = Math.min(
                    rlwi.getNumberOfLiteralWords(),
                    rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k)
                            & (~rlwj.getLiteralWordAt(k)));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
        final boolean i_remains = rlwi.size() > 0;
        final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi : rlwj;
        if (i_remains)
            remaining.discharge(container);
        else if (adjustContainerSizeWhenAggregating)
            remaining.dischargeAsEmpty(container);
        if (adjustContainerSizeWhenAggregating)
            container.setSizeInBits(Math.max(sizeInBits(),
                    a.sizeInBits()));

    }

    /**
     * Returns the cardinality of the result of a bitwise AND NOT of the
     * values of the current bitmap with some other bitmap. Avoids allocating
     * an intermediate bitmap to hold the result of the OR.
     * The current bitmap is not modified.
     *
     * @param a the other bitmap  (it will not be modified)
     * @return the cardinality
     */
    public int andNotCardinality(final EWAHCompressedBitmap32 a) {
        final BitCounter32 counter = new BitCounter32();
        andNotToContainer(a, counter);
        return counter.getCount();
    }

    /**
     * reports the number of bits set to true. Running time is proportional
     * to compressed size (as reported by sizeInBytes).
     *
     * @return the number of bits set to true
     */
    public int cardinality() {
        int counter = 0;
        final EWAHIterator32 i = this.getEWAHIterator();
        while (i.hasNext()) {
            RunningLengthWord32 localrlw = i.next();
            if (localrlw.getRunningBit()) {
                counter += wordinbits
                        * localrlw.getRunningLength();
            }
            for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
                counter += Integer.bitCount(i.buffer()[i
                        .literalWords() + j]);
            }
        }
        return counter;
    }

    /**
     * Clear any set bits and set size in bits back to 0
     */
    @Override
    public void clear() {
        this.sizeInBits = 0;
        this.actualsizeinwords = 1;
        this.rlw.position = 0;
        // buffer is not fully cleared but any new set operations should
        // overwrite
        // stale data
        this.buffer[0] = 0;
    }

    /*
     * @see java.lang.Object#clone()
     */
    @Override
    public EWAHCompressedBitmap32 clone() {
        EWAHCompressedBitmap32 clone = null;
        try {
            clone = (EWAHCompressedBitmap32) super
                    .clone();
            clone.buffer = this.buffer.clone();
            clone.actualsizeinwords = this.actualsizeinwords;
            clone.sizeInBits = this.sizeInBits;
            clone.rlw = new RunningLengthWord32(clone,this.rlw.position);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // cannot happen
        }
        return clone;
    }

    /**
     * Deserialize.
     *
     * @param in the DataInput stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void deserialize(DataInput in) throws IOException {
        this.sizeInBits = in.readInt();
        this.actualsizeinwords = in.readInt();
        if (this.buffer.length < this.actualsizeinwords) {
            this.buffer = new int[this.actualsizeinwords];
        }
        for (int k = 0; k < this.actualsizeinwords; ++k)
            this.buffer[k] = in.readInt();
        this.rlw = new RunningLengthWord32(this, in.readInt());
    }

    /**
     * Check to see whether the two compressed bitmaps contain the same set
     * bits.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof EWAHCompressedBitmap32) {
            try {
                this.xorToContainer((EWAHCompressedBitmap32) o,
                        new NonEmptyVirtualStorage32());
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
     * This method does not update sizeInBits.
     *
     * @param v      the boolean value
     * @param number the number (must be greater than 0)
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
        final int whatwecanadd = number < RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT
                - runlen ? number
                : RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT
                - runlen;
        this.rlw.setRunningLength(runlen + whatwecanadd);
        number -= whatwecanadd;
        while (number >= RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT) {
            push_back(0);
            this.rlw.position = this.actualsizeinwords - 1;
            if (v)
                this.rlw.setRunningBit(v);
            this.rlw.setRunningLength(RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT);
            number -= RunningLengthWord32.LARGEST_RUNNING_LENGTH_COUNT;
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
     * Gets an EWAHIterator over the data. This is a customized iterator
     * which iterates over run length words. For experts only.
     * 
     * The current bitmap is not modified.
     *
     * @return the EWAHIterator
     */
    public EWAHIterator32 getEWAHIterator() {
        return new EWAHIterator32(this, this.actualsizeinwords);
    }

    /**
     * Gets an IteratingRLW to iterate over the data. For experts only.
     * 
     * The current bitmap is not modified.
     *
     * @return the IteratingRLW iterator corresponding to this bitmap
     */
    public IteratingRLW32 getIteratingRLW() {
        return new IteratingBufferedRunningLengthWord32(this);
    }

    /**
     * @return a list
     * @deprecated use toList() instead.
     */
    @Deprecated
    public List<Integer> getPositions() {
        return toList();
    }

    /**
     * Gets the locations of the true values as one list. (May use more
     * memory than iterator().)
     * 
     * The current bitmap is not modified.
     * 
     * API change: prior to version 0.8.3, this method was called getPositions.
     *
     * @return the positions
     */
    public List<Integer> toList() {
        final ArrayList<Integer> v = new ArrayList<Integer>();
        final EWAHIterator32 i = this.getEWAHIterator();
        int pos = 0;
        while (i.hasNext()) {
            RunningLengthWord32 localrlw = i.next();
            if (localrlw.getRunningBit()) {
                for (int j = 0; j < localrlw.getRunningLength(); ++j) {
                    for (int c = 0; c < wordinbits; ++c)
                        v.add(pos++);
                }
            } else {
                pos += wordinbits * localrlw.getRunningLength();
            }
            for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
                int data = i.buffer()[i.literalWords() + j];
                while (data != 0) {
                    final int T = data & -data;
                    v.add(Integer.bitCount(T - 1)
                            + pos);
                    data ^= T;
                }
                pos += wordinbits;
            }
        }
        while ((v.size() > 0)
                && (v.get(v.size() - 1) >= this.sizeInBits))
            v.remove(v.size() - 1);
        return v;
    }

    /**
     * Returns a customized hash code (based on Karp-Rabin). Naturally, if
     * the bitmaps are equal, they will hash to the same value.
     * 
     * The current bitmap is not modified.
     */
    @Override
    public int hashCode() {
        int karprabin = 0;
        final int B = 31;
        final EWAHIterator32 i = this.getEWAHIterator();
        while (i.hasNext()) {
            i.next();
            if (i.rlw.getRunningBit()) {
                karprabin += B * karprabin
                        + i.rlw.getRunningLength();
            }
            for (int k = 0; k < i.rlw.getNumberOfLiteralWords(); ++k) {
                karprabin += B * karprabin
                        + this.buffer[k + i.literalWords()];
            }
        }
        return karprabin;
    }

    /**
     * Return true if the two EWAHCompressedBitmap have both at least one
     * true bit in the same position. Equivalently, you could call "and" and
     * check whether there is a set bit, but intersects will run faster if
     * you don't need the result of the "and" operation.
     * 
     * The current bitmap is not modified.
     *
     * @param a the other bitmap (it will not be modified)
     * @return whether they intersect
     */
    public boolean intersects(final EWAHCompressedBitmap32 a) {
        NonEmptyVirtualStorage32 nevs = new NonEmptyVirtualStorage32();
        try {
            this.andToContainer(a, nevs);
        } catch (NonEmptyVirtualStorage32.NonEmptyException nee) {
            return true;
        }
        return false;
    }

    /**
     * Iterator over the set bits (this is what most people will want to use
     * to browse the content if they want an iterator). The location of the
     * set bits is returned, in increasing order.
     * 
     * The current bitmap is not modified.
     *
     * @return the int iterator
     */
    public IntIterator intIterator() {
        return new IntIteratorImpl32(this.getEWAHIterator());
    }

    /**
     * Iterator over the clear bits. The location of the clear bits is
     * returned, in increasing order.
     * 
     * The current bitmap is not modified.
     *
     * @return the int iterator
     */
    public IntIterator clearIntIterator() {
        return new ClearIntIterator32(this.getEWAHIterator(), this.sizeInBits);
    }

    /**
     * Iterates over the positions of the true values. This is similar to
     * intIterator(), but it uses Java generics.
     * 
     * The current bitmap is not modified.
     *
     * @return the iterator
     */
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return this.under.hasNext();
            }

            @Override
            public Integer next() {
                return this.under.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException(
                        "bitsets do not support remove");
            }

            private final IntIterator under = intIterator();
        };
    }

    /**
     * For internal use.
     *
     * @param data   the array of words to be added
     * @param start  the starting point
     * @param number the number of words to add
     */
    private void negative_push_back(final int[] data, final int start,
                                    final int number) {
        while (this.actualsizeinwords + number >= this.buffer.length) {
            final int oldBuffer[] = this.buffer;
            if (this.actualsizeinwords + number < 32768)
                this.buffer = new int[(this.actualsizeinwords + number) * 2];
            else if ((this.actualsizeinwords + number) * 3 / 2 < this.actualsizeinwords
                    + number)
                this.buffer = new int[Integer.MAX_VALUE];
            else
                this.buffer = new int[(this.actualsizeinwords + number) * 3 / 2];
            System.arraycopy(oldBuffer, 0, this.buffer, 0,
                    oldBuffer.length);
            this.rlw.parent.buffer = this.buffer;
        }
        for (int k = 0; k < number; ++k)
            this.buffer[this.actualsizeinwords + k] = ~data[start
                    + k];
        this.actualsizeinwords += number;
    }

    /**
     * Negate (bitwise) the current bitmap. To get a negated copy, do
     * EWAHCompressedBitmap x= ((EWAHCompressedBitmap) mybitmap.clone());
     * x.not();
     * 
     * The running time is proportional to the compressed size (as reported
     * by sizeInBytes()).
     * 
     * Because this method modifies the bitmap, it is not thread-safe.
     */
    @Override
    public void not() {
        final EWAHIterator32 i = this.getEWAHIterator();
        if (!i.hasNext())
            return;
        while (true) {
            final RunningLengthWord32 rlw1 = i.next();
            rlw1.setRunningBit(!rlw1.getRunningBit());
            for (int j = 0; j < rlw1.getNumberOfLiteralWords(); ++j) {
                i.buffer()[i.literalWords() + j] = ~i.buffer()[i
                        .literalWords() + j];
            }
            if (!i.hasNext()) {// must potentially adjust the last
                // literal word
                final int usedbitsinlast = this.sizeInBits
                        % wordinbits;
                if (usedbitsinlast == 0)
                    return;
                if (rlw1.getNumberOfLiteralWords() == 0) {
                    if ((rlw1.getRunningLength() > 0)
                            && (rlw1.getRunningBit())) {
						if ((rlw1.getRunningLength() == 1)
								&& (rlw1.position > 0)) {
							// we need to prune ending
							final EWAHIterator32 j = this.getEWAHIterator();
							int newrlwpos = this.rlw.position;
							while (j.hasNext()) {
								RunningLengthWord32 r = j.next();								
								if (r.position < rlw1.position) { 
									newrlwpos = r.position;
								} else break;
							}
							this.rlw.position = newrlwpos;
							this.actualsizeinwords -= 1;
						} else
							rlw1.setRunningLength(rlw1.getRunningLength() - 1);
                        this.addLiteralWord((~0) >>> (wordinbits - usedbitsinlast));
                        
                    }
                    return;
                }
                i.buffer()[i.literalWords()
                        + rlw1.getNumberOfLiteralWords() - 1] &= ((~0) >>> (wordinbits - usedbitsinlast));
                if(i.buffer()[i.literalWords()
                              + rlw1.getNumberOfLiteralWords() - 1] == 0) {
                	this.rlw.setNumberOfLiteralWords(this.rlw.getNumberOfLiteralWords()-1);
                	this.actualsizeinwords -= 1;
                	this.addEmptyWord(false);
                }
                return;
            }
        }
    }

    /**
     * Returns a new compressed bitmap containing the bitwise OR values of
     * the current bitmap with some other bitmap.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     * 
     * The current bitmap is not modified.
     *
     * @param a the other bitmap (it will not be modified)
     * @return the EWAH compressed bitmap
     */
    @Override
    public EWAHCompressedBitmap32 or(final EWAHCompressedBitmap32 a) {
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        orToContainer(a, container);
        return container;
    }

    /**
     * Computes the bitwise or between the current bitmap and the bitmap
     * "a". Stores the result in the container.
     * 
     * The current bitmap is not modified.
     * 
     * The content of the container is overwritten.
     *
     * @param a         the other bitmap (it will not be modified)
     * @param container where we store the result
     */
    public void orToContainer(final EWAHCompressedBitmap32 a,
                              final BitmapStorage32 container) {
        container.clear();
        final EWAHIterator32 i = a.getEWAHIterator();
        final EWAHIterator32 j = getEWAHIterator();
        final IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(
                i);
        final IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(
                j);
        while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
            while ((rlwi.getRunningLength() > 0)
                    || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi
                        .getRunningLength() < rlwj
                        .getRunningLength();
                final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
                        : rlwj;
                final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
                        : rlwi;
                if (predator.getRunningBit()) {
                    container.addStreamOfEmptyWords(true,
                            predator.getRunningLength());
                    prey.discardFirstWords(predator
                            .getRunningLength());
                } else {
                    final int index = prey.discharge(container,
                            predator.getRunningLength());
                    container.addStreamOfEmptyWords(false,
                            predator.getRunningLength()
                                    - index
                    );
                }
                predator.discardRunningWords();
            }
            final int nbre_literal = Math.min(
                    rlwi.getNumberOfLiteralWords(),
                    rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                for (int k = 0; k < nbre_literal; ++k) {
                    container.addWord(rlwi.getLiteralWordAt(k)
                            | rlwj.getLiteralWordAt(k));
                }
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
        if ((rlwj.size() > 0) && (rlwi.size() > 0)) throw new RuntimeException("fds");
        final boolean i_remains = rlwi.size() > 0;
        final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi
                : rlwj;
        remaining.discharge(container);
        container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
    }

    /**
     * Returns the cardinality of the result of a bitwise OR of the values
     * of the current bitmap with some other bitmap. Avoids allocating
     * an intermediate bitmap to hold the result of the OR.
     * 
     * The current bitmap is not modified.
     *
     * @param a the other bitmap (it will not be modified)
     * @return the cardinality
     */
    public int orCardinality(final EWAHCompressedBitmap32 a) {
        final BitCounter32 counter = new BitCounter32();
        orToContainer(a, counter);
        return counter.getCount();
    }

    /**
     * For internal use.
     *
     * @param data the word to be added
     */
    private void push_back(final int data) {
        if (this.actualsizeinwords == this.buffer.length) {
            final int oldBuffer[] = this.buffer;
            if (oldBuffer.length < 32768)
                this.buffer = new int[oldBuffer.length * 2];
            else if (oldBuffer.length * 3 / 2 < oldBuffer.length)
                this.buffer = new int[Integer.MAX_VALUE];
            else
                this.buffer = new int[oldBuffer.length * 3 / 2];
            System.arraycopy(oldBuffer, 0, this.buffer, 0,
                    oldBuffer.length);
            this.rlw.parent.buffer = this.buffer;
        }
        this.buffer[this.actualsizeinwords++] = data;
    }

    /**
     * For internal use.
     *
     * @param data   the array of words to be added
     * @param start  the starting point
     * @param number the number of words to add
     */
    private void push_back(final int[] data, final int start,
                           final int number) {
        if (this.actualsizeinwords + number >= this.buffer.length) {
            final int oldBuffer[] = this.buffer;
            if (this.actualsizeinwords + number < 32768)
                this.buffer = new int[(this.actualsizeinwords + number) * 2];
            else if ((this.actualsizeinwords + number) * 3 / 2 < this.actualsizeinwords
                    + number) // overflow
                this.buffer = new int[Integer.MAX_VALUE];
            else
                this.buffer = new int[(this.actualsizeinwords + number) * 3 / 2];
            System.arraycopy(oldBuffer, 0, this.buffer, 0,
                    oldBuffer.length);
            this.rlw.parent.buffer = this.buffer;
        }
        System.arraycopy(data, start, this.buffer,
                this.actualsizeinwords, number);
        this.actualsizeinwords += number;
    }

    /*
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException {
        deserialize(in);
    }

    /**
     * For internal use (trading off memory for speed).
     *
     * @param size the number of words to allocate
     * @return True if the operation was a success.
     */
    private boolean reserve(final int size) {
        if (size > this.buffer.length) {
            final int oldBuffer[] = this.buffer;
            this.buffer = new int[size];
            System.arraycopy(oldBuffer, 0, this.buffer, 0,
                    oldBuffer.length);
            this.rlw.parent.buffer = this.buffer;
            return true;
        }
        return false;
    }

    /**
     * Serialize.
     * 
     * The current bitmap is not modified.
     *
     * @param out the DataOutput stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void serialize(DataOutput out) throws IOException {
        out.writeInt(this.sizeInBits);
        out.writeInt(this.actualsizeinwords);
        for (int k = 0; k < this.actualsizeinwords; ++k)
            out.writeInt(this.buffer[k]);
        out.writeInt(this.rlw.position);
    }

    /**
     * Report the number of bytes required to serialize this bitmap.
     * 
     * The current bitmap is not modified.
     *
     * @return the size in bytes
     */
    public int serializedSizeInBytes() {
        return this.sizeInBytes() + 3 * 4;
    }

    /**
     * Query the value of a single bit. Relying on this method when speed is
     * needed is discouraged. The complexity is linear with the size of the
     * bitmap.
     * 
     * (This implementation is based on zhenjl's Go version of JavaEWAH.)
     * 
     * The current bitmap is not modified.
     *
     * @param i the bit we are interested in
     * @return whether the bit is set to true
     */
    public boolean get(final int i) {
        if ((i < 0) || (i >= this.sizeInBits))
            return false;
        int wordChecked = 0;
        final IteratingRLW32 j = getIteratingRLW();
        final int wordi = i / wordinbits;
        while (wordChecked <= wordi) {
            wordChecked += j.getRunningLength();
            if (wordi < wordChecked) {
                return j.getRunningBit();
            }
            if (wordi < wordChecked + j.getNumberOfLiteralWords()) {
                final int w = j.getLiteralWordAt(wordi
                        - wordChecked);
                return (w & (1 << i)) != 0;
            }
            wordChecked += j.getNumberOfLiteralWords();
            j.next();
        }
        return false;
    }

    /**
     * Set the bit at position i to true, the bits must be set in (strictly)
     * increasing order. For example, set(15) and then set(7) will fail. You
     * must do set(7) and then set(15).
     * 
     * Since this modifies the bitmap, this method is not thread-safe.
     *
     * @param i the index
     * @return true if the value was set (always true when i is greater or
     * equal to sizeInBits()).
     * @throws IndexOutOfBoundsException if i is negative or greater than Integer.MAX_VALUE -
     *                                   32
     */
    public boolean set(final int i) {
        if ((i > Integer.MAX_VALUE - wordinbits) || (i < 0))
            throw new IndexOutOfBoundsException(
                    "Set values should be between 0 and "
                            + (Integer.MAX_VALUE - wordinbits)
            );
        if (i < this.sizeInBits)
            return false;
        // distance in words:
        final int dist = (i + wordinbits) / wordinbits
                - (this.sizeInBits + wordinbits - 1) / wordinbits;
        this.sizeInBits = i + 1;
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
            this.rlw.setNumberOfLiteralWords(this.rlw
                    .getNumberOfLiteralWords() - 1);
            // next we add one clean word
            addEmptyWord(true);
        }
        return true;
    }

    /**
     * Set the size in bits. This does not change the compressed bitmap.
     */
    @Override
    public void setSizeInBits(final int size) {
        if ((size + EWAHCompressedBitmap32.wordinbits - 1)
                / EWAHCompressedBitmap32.wordinbits != (this.sizeInBits
                + EWAHCompressedBitmap32.wordinbits - 1)
                / EWAHCompressedBitmap32.wordinbits)
            throw new RuntimeException(
                    "You can only reduce the size of the bitmap within the scope of the last word. To extend the bitmap, please call setSizeInbits(int,boolean): "
                            + size + " " + this.sizeInBits
            );
        this.sizeInBits = size;
    }

    /**
     * Change the reported size in bits of the *uncompressed* bitmap
     * represented by this compressed bitmap. It may change the underlying
     * compressed bitmap. It is not possible to reduce the sizeInBits, but
     * it can be extended. The new bits are set to false or true depending
     * on the value of defaultValue.
     * 
     * This method is not thread-safe.
     *
     * @param size         the size in bits
     * @param defaultValue the default boolean value
     * @return true if the update was possible
     */
    public boolean setSizeInBits(final int size, final boolean defaultValue) {
        if (size < this.sizeInBits)
            return false;
        if (!defaultValue)
            extendEmptyBits(this, this.sizeInBits, size);
        else {
            // next bit could be optimized
            while (((this.sizeInBits % wordinbits) != 0)
                    && (this.sizeInBits < size)) {
                this.set(this.sizeInBits);
            }
            this.addStreamOfEmptyWords(defaultValue,
                    (size / wordinbits) - this.sizeInBits
                            / wordinbits
            );
            // next bit could be optimized
            while (this.sizeInBits < size) {
                this.set(this.sizeInBits);
            }
        }

        this.sizeInBits = size;
        return true;
    }

    /**
     * Returns the size in bits of the *uncompressed* bitmap represented by
     * this compressed bitmap. Initially, the sizeInBits is zero. It is
     * extended automatically when you set bits to true.
     * 
     * The current bitmap is not modified.
     *
     * @return the size in bits
     */
    @Override
    public int sizeInBits() {
        return this.sizeInBits;
    }

    /**
     * Report the *compressed* size of the bitmap (equivalent to memory
     * usage, after accounting for some overhead).
     *
     * @return the size in bytes
     */
    @Override
    public int sizeInBytes() {
        return this.actualsizeinwords * (wordinbits / 8);
    }


    /**
     * Compute a Boolean threshold function: bits are true where at least T
     * bitmaps have a true bit.
     *
     * @param t       the threshold
     * @param bitmaps input data
     * @return the aggregated bitmap
     * @since 0.8.2
     */
    public static EWAHCompressedBitmap32 threshold(final int t,
                                                   final EWAHCompressedBitmap32... bitmaps) {
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
        thresholdWithContainer(container, t, bitmaps);
        return container;
    }

    /**
     * Compute a Boolean threshold function: bits are true where at least T
     * bitmaps have a true bit.
     * 
     * The content of the container is overwritten.
     *
     * @param t         the threshold
     * @param bitmaps   input data
     * @param container where we write the aggregated bitmap
     * @since 0.8.2
     */
    public static void thresholdWithContainer(
            final BitmapStorage32 container, final int t,
            final EWAHCompressedBitmap32... bitmaps) {
        (new RunningBitmapMerge32()).symmetric(
                new ThresholdFuncBitmap32(t), container, bitmaps);
    }


    /**
     * Populate an array of (sorted integers) corresponding to the location
     * of the set bits.
     *
     * @return the array containing the location of the set bits
     */
    public int[] toArray() {
        int[] ans = new int[this.cardinality()];
        int inanspos = 0;
        int pos = 0;
        final EWAHIterator32 i = this.getEWAHIterator();
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

                while (data != 0) {
                    final int t = data & -data;
                    ans[inanspos++] = Integer.bitCount(t - 1) + pos;
                    data ^= t;
                }
                pos += wordinbits;

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
        StringBuffer sb = new StringBuffer(" EWAHCompressedBitmap, size in bits = ");
        sb.append(this.sizeInBits).append(" size in words = ");
        sb.append(this.actualsizeinwords).append("\n");
        final EWAHIterator32 i = this.getEWAHIterator();
        while (i.hasNext()) {
            RunningLengthWord32 localrlw = i.next();
            if (localrlw.getRunningBit()) {
                sb.append(localrlw.getRunningLength()).append(" 1x11\n");
            } else {
                sb.append(localrlw.getRunningLength()).append(" 0x00\n");
            }
            sb.append(localrlw.getNumberOfLiteralWords()).append(" dirties\n");
            for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
                int data = i.buffer()[i.literalWords() + j];
                sb.append("\t").append(data).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * A string describing the bitmap.
     *
     * @return the string
     */
    @Override
    public String toString() {
        StringBuilder answer = new StringBuilder();
        IntIterator i = this.intIterator();
        answer.append("{");
        if (i.hasNext())
            answer.append(i.next());
        while (i.hasNext()) {
            answer.append(",");
            answer.append(i.next());
        }
        answer.append("}");
        return answer.toString();
    }

    /**
     * swap the content of the bitmap with another.
     *
     * @param other bitmap to swap with
     */
    public void swap(final EWAHCompressedBitmap32 other) {
        int[] tmp = this.buffer;
        this.buffer = other.buffer;
        other.buffer = tmp;

        int tmp2 = this.rlw.position;
        this.rlw.position = other.rlw.position;
        other.rlw.position = tmp2;

        int tmp3 = this.actualsizeinwords;
        this.actualsizeinwords = other.actualsizeinwords;
        other.actualsizeinwords = tmp3;

        int tmp4 = this.sizeInBits;
        this.sizeInBits = other.sizeInBits;
        other.sizeInBits = tmp4;
    }

    /**
     * Reduce the internal buffer to its minimal allowable size (given by
     * this.actualsizeinwords). This can free memory.
     */
    public void trim() {
        this.buffer = Arrays
                .copyOf(this.buffer, this.actualsizeinwords);
    }

    /*
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        serialize(out);
    }

    /**
     * Returns a new compressed bitmap containing the bitwise XOR values of
     * the current bitmap with some other bitmap.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     * 
     * The current bitmap is not modified.
     *
     * @param a the other bitmap (it will not be modified)
     * @return the EWAH compressed bitmap
     */
    @Override
    public EWAHCompressedBitmap32 xor(final EWAHCompressedBitmap32 a) {
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        xorToContainer(a, container);
        return container;
    }

    /**
     * Computes a new compressed bitmap containing the bitwise XOR values of
     * the current bitmap with some other bitmap.
     * 
     * The running time is proportional to the sum of the compressed sizes
     * (as reported by sizeInBytes()).
     * 
     * The current bitmap is not modified.
     * 
     * The content of the container is overwritten.
     *
     * @param a         the other bitmap (it will not be modified)
     * @param container where we store the result
     */
    public void xorToContainer(final EWAHCompressedBitmap32 a,
                               final BitmapStorage32 container) {
        container.clear();
        final EWAHIterator32 i = a.getEWAHIterator();
        final EWAHIterator32 j = getEWAHIterator();
        final IteratingBufferedRunningLengthWord32 rlwi = new IteratingBufferedRunningLengthWord32(
                i);
        final IteratingBufferedRunningLengthWord32 rlwj = new IteratingBufferedRunningLengthWord32(
                j);
        while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
            while ((rlwi.getRunningLength() > 0)
                    || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi
                        .getRunningLength() < rlwj
                        .getRunningLength();
                final IteratingBufferedRunningLengthWord32 prey = i_is_prey ? rlwi
                        : rlwj;
                final IteratingBufferedRunningLengthWord32 predator = i_is_prey ? rlwj
                        : rlwi;
                final int index = (!predator.getRunningBit()) ? prey.discharge(container,
                        predator.getRunningLength()) : prey.dischargeNegated(
                        container,
                        predator.getRunningLength());
                container.addStreamOfEmptyWords(predator.getRunningBit(),
                        predator.getRunningLength()
                                - index
                );
                predator.discardRunningWords();
            }
            final int nbre_literal = Math.min(
                    rlwi.getNumberOfLiteralWords(),
                    rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k)
                            ^ rlwj.getLiteralWordAt(k));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
        final boolean i_remains = rlwi.size() > 0;
        final IteratingBufferedRunningLengthWord32 remaining = i_remains ? rlwi
                : rlwj;
        remaining.discharge(container);
        container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
    }

    /**
     * Returns the cardinality of the result of a bitwise XOR of the values
     * of the current bitmap with some other bitmap. Avoids allocating an
     * intermediate bitmap to hold the result of the OR.
     * 
     * The current bitmap is not modified.
     *
     * @param a the other bitmap (it will not be modified)
     * @return the cardinality
     */
    public int xorCardinality(final EWAHCompressedBitmap32 a) {
        final BitCounter32 counter = new BitCounter32();
        xorToContainer(a, counter);
        return counter.getCount();
    }

    /**
     * For internal use. Computes the bitwise and of the provided bitmaps
     * and stores the result in the container.
     * 
     * The content of the container is overwritten.
     *
     * @param container where the result is stored
     * @param bitmaps   bitmaps to AND
     */
    public static void andWithContainer(final BitmapStorage32 container,
                                        final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            throw new IllegalArgumentException(
                    "Need at least one bitmap");
        if (bitmaps.length == 2) {
            bitmaps[0].andToContainer(bitmaps[1], container);
            return;
        }
        EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 tmp = new EWAHCompressedBitmap32();
        bitmaps[0].andToContainer(bitmaps[1], answer);
        for (int k = 2; k < bitmaps.length - 1; ++k) {
            answer.andToContainer(bitmaps[k], tmp);
            tmp.swap(answer);
            tmp.clear();
        }
        answer.andToContainer(bitmaps[bitmaps.length - 1], container);
    }

    /**
     * Returns a new compressed bitmap containing the bitwise AND values of
     * the provided bitmaps.
     * 
     * It may or may not be faster than doing the aggregation two-by-two
     * (A.and(B).and(C)).
     * 
     * If only one bitmap is provided, it is returned as is.
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     *
     * @param bitmaps bitmaps to AND together
     * @return result of the AND
     */
    public static EWAHCompressedBitmap32 and(
            final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            return bitmaps[0];
        if (bitmaps.length == 2)
            return bitmaps[0].and(bitmaps[1]);
        EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
        EWAHCompressedBitmap32 tmp = new EWAHCompressedBitmap32();
        bitmaps[0].andToContainer(bitmaps[1], answer);
        for (int k = 2; k < bitmaps.length; ++k) {
            answer.andToContainer(bitmaps[k], tmp);
            tmp.swap(answer);
            tmp.clear();
        }
        return answer;
    }

    /**
     * Returns the cardinality of the result of a bitwise AND of the values
     * of the provided bitmaps. Avoids allocating an intermediate
     * bitmap to hold the result of the AND.
     *
     * @param bitmaps bitmaps to AND
     * @return the cardinality
     */
    public static int andCardinality(
            final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            return bitmaps[0].cardinality();
        final BitCounter32 counter = new BitCounter32();
        andWithContainer(counter, bitmaps);
        return counter.getCount();
    }

    /**
     * Return a bitmap with the bit set to true at the given positions. The
     * positions should be given in sorted order.
     * 
     * (This is a convenience method.)
     *
     * @param setbits list of set bit positions
     * @return the bitmap
     * @since 0.4.5
     */
    public static EWAHCompressedBitmap32 bitmapOf(int... setbits) {
        EWAHCompressedBitmap32 a = new EWAHCompressedBitmap32();
        for (int k : setbits)
            a.set(k);
        return a;
    }

    /**
     * For internal use. This simply adds a stream of words made of zeroes
     * so that we pad to the desired size.
     *
     * @param storage     bitmap to extend
     * @param currentSize current size (in bits)
     * @param newSize     new desired size (in bits)
     */
    private static void extendEmptyBits(final BitmapStorage32 storage,
                                        final int currentSize, final int newSize) {
        final int currentLeftover = currentSize % wordinbits;
        final int finalLeftover = newSize % wordinbits;
        storage.addStreamOfEmptyWords(false, (newSize / wordinbits)
                - currentSize / wordinbits
                + (finalLeftover != 0 ? 1 : 0)
                + (currentLeftover != 0 ? -1 : 0));
    }

    /**
     * For internal use. Computes the bitwise or of the provided bitmaps and
     * stores the result in the container.
     * 
     * The content of the container is overwritten.
     *
     * @param container where store the result
     * @param bitmaps   to be aggregated
     */
    public static void orWithContainer(final BitmapStorage32 container,
                                       final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length < 2)
            throw new IllegalArgumentException(
                    "You should provide at least two bitmaps, provided "
                            + bitmaps.length
            );
        int size = 0;
        int sinbits = 0;
        for (EWAHCompressedBitmap32 b : bitmaps) {
            size += b.sizeInBytes();
            if (sinbits < b.sizeInBits())
                sinbits = b.sizeInBits();
        }
        if (size * 8 > sinbits) {
            FastAggregation32.bufferedorWithContainer(container,
                    65536, bitmaps);
        } else {
            FastAggregation32.orToContainer(container, bitmaps);
        }
    }

    /**
     * For internal use. Computes the bitwise xor of the provided bitmaps
     * and stores the result in the container.
     * 
     * The content of the container is overwritten.
     *
     * @param container where store the result
     * @param bitmaps   to be aggregated
     */
    public static void xorWithContainer(final BitmapStorage32 container,
                                        final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length < 2)
            throw new IllegalArgumentException(
                    "You should provide at least two bitmaps, provided "
                            + bitmaps.length
            );
        int size = 0;
        int sinbits = 0;
        for (EWAHCompressedBitmap32 b : bitmaps) {
            size += b.sizeInBytes();
            if (sinbits < b.sizeInBits())
                sinbits = b.sizeInBits();
        }
        if (size * 8 > sinbits) {
            FastAggregation32.bufferedxorWithContainer(container,
                    65536, bitmaps);
        } else {
            FastAggregation32.xorToContainer(container, bitmaps);
        }
    }

    /**
     * Returns a new compressed bitmap containing the bitwise OR values of
     * the provided bitmaps. This is typically faster than doing the
     * aggregation two-by-two (A.or(B).or(C).or(D)).
     * 
     * If only one bitmap is provided, it is returned as is.
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     *
     * @param bitmaps bitmaps to OR together
     * @return result of the OR
     */
    public static EWAHCompressedBitmap32 or(
            final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            return bitmaps[0];
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
        int largestSize = 0;
        for (EWAHCompressedBitmap32 bitmap : bitmaps) {
            largestSize = Math.max(bitmap.actualsizeinwords,
                    largestSize);
        }
        container.reserve((int) (largestSize * 1.5));
        orWithContainer(container, bitmaps);
        return container;
    }

    /**
     * Returns a new compressed bitmap containing the bitwise XOR values of
     * the provided bitmaps. This is typically faster than doing the
     * aggregation two-by-two (A.xor(B).xor(C).xor(D)).
     * 
     * If only one bitmap is provided, it is returned as is.
     * 
     * If you are not planning on adding to the resulting bitmap, you may
     * call the trim() method to reduce memory usage.
     *
     * @param bitmaps bitmaps to XOR together
     * @return result of the XOR
     */
    public static EWAHCompressedBitmap32 xor(
            final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            return bitmaps[0];
        final EWAHCompressedBitmap32 container = new EWAHCompressedBitmap32();
        int largestSize = 0;
        for (EWAHCompressedBitmap32 bitmap : bitmaps) {
            largestSize = Math.max(bitmap.actualsizeinwords,
                    largestSize);
        }
        container.reserve((int) (largestSize * 1.5));
        xorWithContainer(container, bitmaps);
        return container;
    }

    /**
     * Returns the cardinality of the result of a bitwise OR of the values
     * of the provided bitmaps. Avoids allocating an intermediate
     * bitmap to hold the result of the OR.
     *
     * @param bitmaps bitmaps to OR
     * @return the cardinality
     */
    public static int orCardinality(final EWAHCompressedBitmap32... bitmaps) {
        if (bitmaps.length == 1)
            return bitmaps[0].cardinality();
        final BitCounter32 counter = new BitCounter32();
        orWithContainer(counter, bitmaps);
        return counter.getCount();
    }

    /**
     * The actual size in words.
     */
    int actualsizeinwords = 1;

    /**
     * The buffer (array of 32-bit words)
     */
    int buffer[] = null;

    /**
     * The current (last) running length word.
     */
    RunningLengthWord32 rlw = null;

    /**
     * sizeInBits: number of bits in the (uncompressed) bitmap.
     */
    int sizeInBits = 0;

    /**
     * The Constant DEFAULT_BUFFER_SIZE: default memory allocation when the
     * object is constructed.
     */
    static final int DEFAULT_BUFFER_SIZE = 4;


    /**
     * whether we adjust after some aggregation by adding in zeroes *
     */
    public static final boolean adjustContainerSizeWhenAggregating = true;

    /**
     * The Constant WORD_IN_BITS represents the number of bits in a int.
     */
    public static final int wordinbits = 32;

    static final long serialVersionUID = 1L;
}
