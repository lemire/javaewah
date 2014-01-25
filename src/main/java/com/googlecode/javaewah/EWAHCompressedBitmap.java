package com.googlecode.javaewah;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */

import java.util.*;
import java.io.*;

import com.googlecode.javaewah.symmetric.RunningBitmapMerge;
import com.googlecode.javaewah.symmetric.ThresholdFuncBitmap;

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
 * <p>
 * Once constructed, the bitmap is essentially immutable (unless you call the
 * "set" or "add" methods). Thus, it can be safely used in multi-threaded
 * programs.
 * </p>
 * 
 * <p>
 * This implementation being 64-bit, it assumes a 64-bit CPU together with a
 * 64-bit Java Virtual Machine. This same code on a 32-bit machine may not be as
 * fast. There is also a 32-bit version of this code in the class
 * javaewah32.EWAHCompressedBitmap32.
 * </p>
 * <p>
 * For more details, see the following paper:
 * </p>
 * 
 * <ul>
 * <li>Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010. <a
 * href="http://arxiv.org/abs/0901.3751">http://arxiv.org/abs/0901.3751</a></li>
 * </ul>
 * 
 * <p>
 * A 32-bit version of the compressed format was described by Wu et al. and
 * named WBC:
 * </p>
 * 
 * <ul>
 * <li>K. Wu, E. J. Otoo, A. Shoshani, H. Nordberg, Notes on design and
 * implementation of compressed bit vectors, Tech. Rep. LBNL/PUB-3161, Lawrence
 * Berkeley National Laboratory, available from http://crd.lbl.
 * gov/~kewu/ps/PUB-3161.html (2001).</li>
 * </ul>
 * 
 * <p>
 * Probably, the best prior art is the Oracle bitmap compression scheme (BBC):
 * </p>
 * <ul>
 * <li>G. Antoshenkov, Byte-Aligned Bitmap Compression, DCC'95, 1995.</li>
 * </ul>
 * 
 * <p>
 * 1- The authors do not know of any patent infringed by the following
 * implementation. However, similar schemes, like WAH are covered by patents.
 * </p>
 * 
 * 
 * @see com.googlecode.javaewah32.EWAHCompressedBitmap32 EWAHCompressedBitmap32
 * 
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
                this.rlw = new RunningLengthWord(this, 0);
        }

        /**
         * Sets explicitly the buffer size (in 64-bit words). The initial memory
         * usage will be "buffersize * 64". For large poorly compressible
         * bitmaps, using large values may improve performance.
         * 
         * @param buffersize
         *                number of 64-bit words reserved when the object is
         *                created)
         */
        public EWAHCompressedBitmap(final int buffersize) {
                this.buffer = new long[buffersize];
                this.rlw = new RunningLengthWord(this, 0);
        }

        /**
         * Adding words directly to the bitmap (for expert use).
         * 
         * This is normally how you add data to the array. So you add bits in
         * streams of 8*8 bits.
         * 
         * Example: if you add 321, you are have added (in binary notation)
         * 0b101000001, so you have effectively called set(0), set(6), set(8) in
         * sequence.
         * 
         * Since this modifies the bitmap, this method is not thread-safe.
         * 
         * @param newdata
         *                the word
         */
        @Override
        public void add(final long newdata) {
                add(newdata, wordinbits);
        }

        /**
         * Adding words directly to the bitmap (for expert use).
         * 
         * @param newdata
         *                the word
         * @param bitsthatmatter
         *                the number of significant bits (by default it should
         *                be 64)
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
         *                the boolean value
         */
        private void addEmptyWord(final boolean v) {
                final boolean noliteralword = (this.rlw
                        .getNumberOfLiteralWords() == 0);
                final long runlen = this.rlw.getRunningLength();
                if ((noliteralword) && (runlen == 0)) {
                        this.rlw.setRunningBit(v);
                }
                if ((noliteralword)
                        && (this.rlw.getRunningBit() == v)
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
         *                the literal word
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
         * if you have several literal words to copy over, this might be faster.
         * 
         * 
         * @param data
         *                the literal words
         * @param start
         *                the starting point in the array
         * @param number
         *                the number of literal words to add
         */
        @Override
        public void addStreamOfLiteralWords(final long[] data, final int start,
                final int number) {
                int leftovernumber = number;
                while (leftovernumber > 0) {
                        final int NumberOfLiteralWords = this.rlw
                                .getNumberOfLiteralWords();
                        final int whatwecanadd = leftovernumber < RunningLengthWord.largestliteralcount
                                - NumberOfLiteralWords ? leftovernumber
                                : RunningLengthWord.largestliteralcount
                                        - NumberOfLiteralWords;
                        this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords
                                + whatwecanadd);
                        leftovernumber -= whatwecanadd;
                        push_back(data, start, whatwecanadd);
                        this.sizeinbits += whatwecanadd * wordinbits;
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
         * @param v
         *                the boolean value
         * @param number
         *                the number
         */
        @Override
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
                        - runlen ? number
                        : RunningLengthWord.largestrunninglengthcount - runlen;
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
         * Same as addStreamOfLiteralWords, but the words are negated.
         * 
         * @param data
         *                the literal words
         * @param start
         *                the starting point in the array
         * @param number
         *                the number of literal words to add
         */
        @Override
        public void addStreamOfNegatedLiteralWords(final long[] data,
                final int start, final int number) {
                int leftovernumber = number;
                while (leftovernumber > 0) {
                        final int NumberOfLiteralWords = this.rlw
                                .getNumberOfLiteralWords();
                        final int whatwecanadd = leftovernumber < RunningLengthWord.largestliteralcount
                                - NumberOfLiteralWords ? leftovernumber
                                : RunningLengthWord.largestliteralcount
                                        - NumberOfLiteralWords;
                        this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords
                                + whatwecanadd);
                        leftovernumber -= whatwecanadd;
                        negative_push_back(data, start, whatwecanadd);
                        this.sizeinbits += whatwecanadd * wordinbits;
                        if (leftovernumber > 0) {
                                push_back(0);
                                this.rlw.position = this.actualsizeinwords - 1;
                        }
                }
        }

        /**
         * Returns a new compressed bitmap containing the bitwise AND values of
         * the current bitmap with some other bitmap.
         * 
         * The running time is proportional to the sum of the compressed sizes
         * (as reported by sizeInBytes()).
         * 
         * If you are not planning on adding to the resulting bitmap, you may
         * call the trim() method to reduce memory usage.
         * 
         * @since 0.4.3
         * @param a
         *                the other bitmap
         * @return the EWAH compressed bitmap
         */
        @Override
        public EWAHCompressedBitmap and(final EWAHCompressedBitmap a) {
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                container
                        .reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
                                : a.actualsizeinwords);
                andToContainer(a, container);
                return container;
        }

        /**
         * Computes new compressed bitmap containing the bitwise AND values of
         * the current bitmap with some other bitmap.
         * 
         * The running time is proportional to the sum of the compressed sizes
         * (as reported by sizeInBytes()).
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @param container
         *                where we store the result
         */
        public void andToContainer(final EWAHCompressedBitmap a,
                final BitmapStorage container) {
                final EWAHIterator i = a.getEWAHIterator();
                final EWAHIterator j = getEWAHIterator();
                final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(
                        i);
                final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(
                        j);
                while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
                        while ((rlwi.getRunningLength() > 0)
                                || (rlwj.getRunningLength() > 0)) {
                                final boolean i_is_prey = rlwi
                                        .getRunningLength() < rlwj
                                        .getRunningLength();
                                final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
                                        : rlwj;
                                final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
                                        : rlwi;
                                if (predator.getRunningBit() == false) {
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength());
                                        prey.discardFirstWords(predator
                                                .getRunningLength());
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                } else {
                                        final long index = prey.discharge(
                                                container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                }
                        }
                        final int nbre_literal = Math.min(
                                rlwi.getNumberOfLiteralWords(),
                                rlwj.getNumberOfLiteralWords());
                        if (nbre_literal > 0) {
                                for (int k = 0; k < nbre_literal; ++k)
                                        container.add(rlwi.getLiteralWordAt(k)
                                                & rlwj.getLiteralWordAt(k));
                                rlwi.discardFirstWords(nbre_literal);
                                rlwj.discardFirstWords(nbre_literal);
                        }
                }
                if (adjustContainerSizeWhenAggregating) {
                        final boolean i_remains = rlwi.size() > 0;
                        final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi
                                : rlwj;
                        remaining.dischargeAsEmpty(container);
                        container.setSizeInBits(Math.max(sizeInBits(),
                                a.sizeInBits()));
                }
        }

        /**
         * Returns the cardinality of the result of a bitwise AND of the values
         * of the current bitmap with some other bitmap. Avoids needing to
         * allocate an intermediate bitmap to hold the result of the OR.
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @return the cardinality
         */
        public int andCardinality(final EWAHCompressedBitmap a) {
                final BitCounter counter = new BitCounter();
                andToContainer(a, counter);
                return counter.getCount();
        }

        /**
         * Returns a new compressed bitmap containing the bitwise AND NOT values
         * of the current bitmap with some other bitmap.
         * 
         * The running time is proportional to the sum of the compressed sizes
         * (as reported by sizeInBytes()).
         * 
         * If you are not planning on adding to the resulting bitmap, you may
         * call the trim() method to reduce memory usage.
         * 
         * @param a
         *                the other bitmap
         * @return the EWAH compressed bitmap
         */
        @Override
        public EWAHCompressedBitmap andNot(final EWAHCompressedBitmap a) {
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                container
                        .reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords
                                : a.actualsizeinwords);
                andNotToContainer(a, container);
                return container;
        }

        /**
         * Returns a new compressed bitmap containing the bitwise AND NOT values
         * of the current bitmap with some other bitmap. This method is expected
         * to be faster than doing A.and(B.clone().not()).
         * 
         * The running time is proportional to the sum of the compressed sizes
         * (as reported by sizeInBytes()).
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @param container
         *                where to store the result
         */
        public void andNotToContainer(final EWAHCompressedBitmap a,
                final BitmapStorage container) {
                final EWAHIterator i = getEWAHIterator();
                final EWAHIterator j = a.getEWAHIterator();
                final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(
                        i);
                final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(
                        j);
                while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
                        while ((rlwi.getRunningLength() > 0)
                                || (rlwj.getRunningLength() > 0)) {
                                final boolean i_is_prey = rlwi
                                        .getRunningLength() < rlwj
                                        .getRunningLength();
                                final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
                                        : rlwj;
                                final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
                                        : rlwi;
                                if (((predator.getRunningBit() == true) && (i_is_prey))
                                        || ((predator.getRunningBit() == false) && (!i_is_prey))) {
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength());
                                        prey.discardFirstWords(predator
                                                .getRunningLength());
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                } else if (i_is_prey) {
                                        long index = prey.discharge(container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                } else {
                                        long index = prey.dischargeNegated(
                                                container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(true,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                }
                        }
                        final int nbre_literal = Math.min(
                                rlwi.getNumberOfLiteralWords(),
                                rlwj.getNumberOfLiteralWords());
                        if (nbre_literal > 0) {
                                for (int k = 0; k < nbre_literal; ++k)
                                        container.add(rlwi.getLiteralWordAt(k)
                                                & (~rlwj.getLiteralWordAt(k)));
                                rlwi.discardFirstWords(nbre_literal);
                                rlwj.discardFirstWords(nbre_literal);
                        }
                }
                final boolean i_remains = rlwi.size() > 0;
                final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi
                        : rlwj;
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
         * values of the current bitmap with some other bitmap. Avoids needing
         * to allocate an intermediate bitmap to hold the result of the OR.
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @return the cardinality
         */
        public int andNotCardinality(final EWAHCompressedBitmap a) {
                final BitCounter counter = new BitCounter();
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
                final EWAHIterator i = this.getEWAHIterator();
                while (i.hasNext()) {
                        RunningLengthWord localrlw = i.next();
                        if (localrlw.getRunningBit()) {
                                counter += wordinbits
                                        * localrlw.getRunningLength();
                        }
                        for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
                                counter += Long.bitCount(i.buffer()[i
                                        .literalWords() + j]);
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
                // buffer is not fully cleared but any new set operations should
                // overwrite
                // stale data
                this.buffer[0] = 0;
        }

        /*
         * @see java.lang.Object#clone()
         */
        @Override
        public EWAHCompressedBitmap clone()
                throws java.lang.CloneNotSupportedException {
                final EWAHCompressedBitmap clone = (EWAHCompressedBitmap) super
                        .clone();
                clone.buffer = this.buffer.clone();
                clone.rlw = new RunningLengthWord(clone, this.rlw.position);
                clone.actualsizeinwords = this.actualsizeinwords;
                clone.sizeinbits = this.sizeinbits;
                return clone;
        }

        /**
         * Deserialize.
         * 
         * @param in
         *                the DataInput stream
         * @throws IOException
         *                 Signals that an I/O exception has occurred.
         */
        public void deserialize(DataInput in) throws IOException {
                this.sizeinbits = in.readInt();
                this.actualsizeinwords = in.readInt();
                if (this.buffer.length < this.actualsizeinwords) {
                        this.buffer = new long[this.actualsizeinwords];
                }
                for (int k = 0; k < this.actualsizeinwords; ++k)
                        this.buffer[k] = in.readLong();
                this.rlw = new RunningLengthWord(this, in.readInt());
        }

        /**
         * Check to see whether the two compressed bitmaps contain the same set
         * bits.
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o) {
                if (o instanceof EWAHCompressedBitmap) {
                        try {
                                this.xorToContainer((EWAHCompressedBitmap) o,
                                        new NonEmptyVirtualStorage());
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
         *                the boolean value
         * @param number
         *                the number (must be greater than 0)
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
                        - runlen ? number
                        : RunningLengthWord.largestrunninglengthcount - runlen;
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
         * Gets an EWAHIterator over the data. This is a customized iterator
         * which iterates over run length word. For experts only.
         * 
         * @return the EWAHIterator
         */
        public EWAHIterator getEWAHIterator() {
                return new EWAHIterator(this, this.actualsizeinwords);
        }

        /**
         * @return the IteratingRLW iterator corresponding to this bitmap
         */
        public IteratingRLW getIteratingRLW() {
                return new IteratingBufferedRunningLengthWord(this);
        }

        /**
         * get the locations of the true values as one vector. (may use more
         * memory than iterator())
         * 
         * @return the positions
         */
        public List<Integer> getPositions() {
                final ArrayList<Integer> v = new ArrayList<Integer>();
                final EWAHIterator i = this.getEWAHIterator();
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
                                long data = i.buffer()[i.literalWords() + j];
                                while (data != 0) {
                                        final long T = data & -data;
                                        v.add(new Integer(Long.bitCount(T - 1)
                                                + pos));
                                        data ^= T;
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
         * Returns a customized hash code (based on Karp-Rabin). Naturally, if
         * the bitmaps are equal, they will hash to the same value.
         * 
         */
        @Override
        public int hashCode() {
                int karprabin = 0;
                final int B = 31;
                final EWAHIterator i = this.getEWAHIterator();
                while (i.hasNext()) {
                        i.next();
                        if (i.rlw.getRunningBit() == true) {
                                karprabin += B
                                        * karprabin
                                        + (i.rlw.getRunningLength() & ((1l << 32) - 1));
                                karprabin += B * karprabin
                                        + (i.rlw.getRunningLength() >>> 32);
                        }
                        for (int k = 0; k < i.rlw.getNumberOfLiteralWords(); ++k) {
                                karprabin += B
                                        * karprabin
                                        + (this.buffer[i.literalWords() + k] & ((1l << 32) - 1));
                                karprabin += B
                                        * karprabin
                                        + (this.buffer[i.literalWords() + k] >>> 32);
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
         * @since 0.3.2
         * @param a
         *                the other bitmap
         * @return whether they intersect
         */
        public boolean intersects(final EWAHCompressedBitmap a) {
                NonEmptyVirtualStorage nevs = new NonEmptyVirtualStorage();
                try {
                        this.andToContainer(a, nevs);
                } catch (NonEmptyVirtualStorage.NonEmptyException nee) {
                        return true;
                }
                return false;
        }

        /**
         * Iterator over the set bits (this is what most people will want to use
         * to browse the content if they want an iterator). The location of the
         * set bits is returned, in increasing order.
         * 
         * @return the int iterator
         */
        public IntIterator intIterator() {
                return new IntIteratorImpl(this.getEWAHIterator());
        }

        /**
         * iterate over the positions of the true values. This is similar to
         * intIterator(), but it uses Java generics.
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
                                return new Integer(this.under.next());
                        }

                        @Override
                        public void remove() {
                                throw new UnsupportedOperationException(
                                        "bitsets do not support remove");
                        }

                        final private IntIterator under = intIterator();
                };
        }

        /**
         * For internal use.
         * 
         * @param data
         *                the array of words to be added
         * @param start
         *                the starting point
         * @param number
         *                the number of words to add
         */
        private void negative_push_back(final long[] data, final int start,
                final int number) {
                while (this.actualsizeinwords + number >= this.buffer.length) {
                        final long oldbuffer[] = this.buffer;
                        if ((this.actualsizeinwords + number) < 32768)
                                this.buffer = new long[(this.actualsizeinwords + number) * 2];
                        else if ((this.actualsizeinwords + number) * 3 / 2 < this.actualsizeinwords
                                + number) // overflow
                                this.buffer = new long[Integer.MAX_VALUE];
                        else
                                this.buffer = new long[(this.actualsizeinwords + number) * 3 / 2];
                        System.arraycopy(oldbuffer, 0, this.buffer, 0,
                                oldbuffer.length);
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
         */
        @Override
        public void not() {
                final EWAHIterator i = this.getEWAHIterator();
                if (!i.hasNext())
                        return;

                while (true) {
                        final RunningLengthWord rlw1 = i.next();
                        rlw1.setRunningBit(!rlw1.getRunningBit());
                        for (int j = 0; j < rlw1.getNumberOfLiteralWords(); ++j) {
                                i.buffer()[i.literalWords() + j] = ~i.buffer()[i
                                        .literalWords() + j];
                        }

                        if (!i.hasNext()) {// must potentially adjust the last
                                           // literal word
                                final int usedbitsinlast = this.sizeinbits
                                        % wordinbits;
                                if (usedbitsinlast == 0)
                                        return;

                                if (rlw1.getNumberOfLiteralWords() == 0) {
                                        if ((rlw1.getRunningLength() > 0)
                                                && (rlw1.getRunningBit())) {
                                                rlw1.setRunningLength(rlw1
                                                        .getRunningLength() - 1);
                                                this.addLiteralWord((~0l) >>> (wordinbits - usedbitsinlast));
                                        }
                                        return;
                                }
                                i.buffer()[i.literalWords()
                                        + rlw1.getNumberOfLiteralWords() - 1] &= ((~0l) >>> (wordinbits - usedbitsinlast));
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
         * @param a
         *                the other bitmap
         * @return the EWAH compressed bitmap
         */
        @Override
        public EWAHCompressedBitmap or(final EWAHCompressedBitmap a) {
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                container.reserve(this.actualsizeinwords + a.actualsizeinwords);
                orToContainer(a, container);
                return container;
        }

        /**
         * Computes the bitwise or between the current bitmap and the bitmap
         * "a". Stores the result in the container.
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @param container
         *                where we store the result
         */
        public void orToContainer(final EWAHCompressedBitmap a,
                final BitmapStorage container) {
                final EWAHIterator i = a.getEWAHIterator();
                final EWAHIterator j = getEWAHIterator();
                final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(
                        i);
                final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(
                        j);
                while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
                        while ((rlwi.getRunningLength() > 0)
                                || (rlwj.getRunningLength() > 0)) {
                                final boolean i_is_prey = rlwi
                                        .getRunningLength() < rlwj
                                        .getRunningLength();
                                final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
                                        : rlwj;
                                final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
                                        : rlwi;
                                if (predator.getRunningBit() == true) {
                                        container.addStreamOfEmptyWords(true,
                                                predator.getRunningLength());
                                        prey.discardFirstWords(predator
                                                .getRunningLength());
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                } else {
                                        long index = prey.discharge(container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                }
                        }
                        final int nbre_literal = Math.min(
                                rlwi.getNumberOfLiteralWords(),
                                rlwj.getNumberOfLiteralWords());
                        if (nbre_literal > 0) {
                                for (int k = 0; k < nbre_literal; ++k) {
                                        container.add(rlwi.getLiteralWordAt(k)
                                                | rlwj.getLiteralWordAt(k));
                                }
                                rlwi.discardFirstWords(nbre_literal);
                                rlwj.discardFirstWords(nbre_literal);
                        }
                }
                final boolean i_remains = rlwi.size() > 0;
                final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi
                        : rlwj;
                remaining.discharge(container);
                container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
        }

        /**
         * Returns the cardinality of the result of a bitwise OR of the values
         * of the current bitmap with some other bitmap. Avoids needing to
         * allocate an intermediate bitmap to hold the result of the OR.
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @return the cardinality
         */
        public int orCardinality(final EWAHCompressedBitmap a) {
                final BitCounter counter = new BitCounter();
                orToContainer(a, counter);
                return counter.getCount();
        }

        /**
         * For internal use.
         * 
         * @param data
         *                the word to be added
         */
        private void push_back(final long data) {
                if (this.actualsizeinwords == this.buffer.length) {
                        final long oldbuffer[] = this.buffer;
                        if (oldbuffer.length < 32768)
                                this.buffer = new long[oldbuffer.length * 2];
                        else if (oldbuffer.length * 3 / 2 < oldbuffer.length) // overflow
                                this.buffer = new long[Integer.MAX_VALUE];
                        else
                                this.buffer = new long[oldbuffer.length * 3 / 2];
                        System.arraycopy(oldbuffer, 0, this.buffer, 0,
                                oldbuffer.length);
                        this.rlw.parent.buffer = this.buffer;
                }
                this.buffer[this.actualsizeinwords++] = data;
        }

        /**
         * For internal use.
         * 
         * @param data
         *                the array of words to be added
         * @param start
         *                the starting point
         * @param number
         *                the number of words to add
         */
        private void push_back(final long[] data, final int start,
                final int number) {
                if (this.actualsizeinwords + number >= this.buffer.length) {
                        final long oldbuffer[] = this.buffer;
                        if (this.actualsizeinwords + number < 32768)
                                this.buffer = new long[(this.actualsizeinwords + number) * 2];
                        else if ((this.actualsizeinwords + number) * 3 / 2 < this.actualsizeinwords
                                + number) // overflow
                                this.buffer = new long[Integer.MAX_VALUE];
                        else
                                this.buffer = new long[(this.actualsizeinwords + number) * 3 / 2];
                        System.arraycopy(oldbuffer, 0, this.buffer, 0,
                                oldbuffer.length);
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
         * @param size
         *                the number of words to allocate
         * @return True if the operation was a success.
         */
        private boolean reserve(final int size) {
                if (size > this.buffer.length) {
                        final long oldbuffer[] = this.buffer;
                        this.buffer = new long[size];
                        System.arraycopy(oldbuffer, 0, this.buffer, 0,
                                oldbuffer.length);
                        this.rlw.parent.buffer = this.buffer;
                        return true;
                }
                return false;
        }

        /**
         * Serialize.
         * 
         * @param out
         *                the DataOutput stream
         * @throws IOException
         *                 Signals that an I/O exception has occurred.
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
         * Query the value of a single bit. Relying on this method when speed is
         * needed is discouraged. The complexity is linear with the size of the
         * bitmap.
         * 
         * (This implementation is based on zhenjl's Go version of JavaEWAH.)
         * 
         * @param i
         *                the bit we are interested in
         * @return whether the bit is set to true
         */
        public boolean get(final int i) {
                if ((i < 0) || (i >= this.sizeinbits))
                        return false;
                int WordChecked = 0;
                final IteratingRLW j = getIteratingRLW();
                final int wordi = i / wordinbits;
                while (WordChecked <= wordi) {
                        WordChecked += j.getRunningLength();
                        if (wordi < WordChecked) {
                                return j.getRunningBit();
                        }
                        if (wordi < WordChecked + j.getNumberOfLiteralWords()) {
                                final long w = j.getLiteralWordAt(wordi
                                        - WordChecked);
                                return (w & (1l << i)) != 0;
                        }
                        WordChecked += j.getNumberOfLiteralWords();
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
         * @param i
         *                the index
         * @return true if the value was set (always true when i greater or
         *         equal to sizeInBits()).
         * @throws IndexOutOfBoundsException
         *                 if i is negative or greater than Integer.MAX_VALUE -
         *                 64
         */
        public boolean set(final int i) {
                if ((i > Integer.MAX_VALUE - wordinbits) || (i < 0))
                        throw new IndexOutOfBoundsException(
                                "Set values should be between 0 and "
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
                        this.rlw.setNumberOfLiteralWords(this.rlw
                                .getNumberOfLiteralWords() - 1);
                        // next we add one clean word
                        addEmptyWord(true);
                }
                return true;
        }

        /**
         * Set the size in bits. This does not change the compressed bitmap.
         * 
         * @since 0.4.0
         */
        @Override
        public void setSizeInBits(final int size) {
                if ((size + EWAHCompressedBitmap.wordinbits - 1)
                        / EWAHCompressedBitmap.wordinbits != (this.sizeinbits
                        + EWAHCompressedBitmap.wordinbits - 1)
                        / EWAHCompressedBitmap.wordinbits)
                        throw new RuntimeException(
                                "You can only reduce the size of the bitmap within the scope of the last word. To extend the bitmap, please call setSizeInbits(int,boolean).");
                this.sizeinbits = size;
        }

        /**
         * Change the reported size in bits of the *uncompressed* bitmap
         * represented by this compressed bitmap. It may change the underlying
         * compressed bitmap. It is not possible to reduce the sizeInBits, but
         * it can be extended. The new bits are set to false or true depending
         * on the value of defaultvalue.
         * 
         * @param size
         *                the size in bits
         * @param defaultvalue
         *                the default boolean value
         * @return true if the update was possible
         */
        public boolean setSizeInBits(final int size, final boolean defaultvalue) {
                if (size < this.sizeinbits)
                        return false;
                if (defaultvalue == false)
                        extendEmptyBits(this, this.sizeinbits, size);
                else {
                        // next bit could be optimized
                        while (((this.sizeinbits % wordinbits) != 0)
                                && (this.sizeinbits < size)) {
                                this.set(this.sizeinbits);
                        }
                        this.addStreamOfEmptyWords(defaultvalue,
                                (size / wordinbits) - this.sizeinbits
                                        / wordinbits);
                        // next bit could be optimized
                        while (this.sizeinbits < size) {
                                this.set(this.sizeinbits);
                        }
                }
                this.sizeinbits = size;
                return true;
        }

        /**
         * Returns the size in bits of the *uncompressed* bitmap represented by
         * this compressed bitmap. Initially, the sizeInBits is zero. It is
         * extended automatically when you set bits to true.
         * 
         * @return the size in bits
         */
        @Override
        public int sizeInBits() {
                return this.sizeinbits;
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
         * 
         * Compute a Boolean threshold function: bits are true where at least T
         * bitmaps have a true bit.
         * 
         * @since 0.8.1
         * @param T
         *                the threshold
         * @param bitmaps
         *                input data
         * @return the aggregated bitmap
         */
        public static EWAHCompressedBitmap threshold(final int T,
                final EWAHCompressedBitmap... bitmaps) {
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                thresholdWithContainer(container, T, bitmaps);
                return container;
        }

        /**
         * 
         * Compute a Boolean threshold function: bits are true where at least T
         * bitmaps have a true bit.
         * 
         * @since 0.8.1
         * @param T
         *                the threshold
         * @param bitmaps
         *                input data
         * @param container
         *                where we write the aggregated bitmap
         */
        public static void thresholdWithContainer(
                final BitmapStorage container, final int T,
                final EWAHCompressedBitmap... bitmaps) {
                (new RunningBitmapMerge()).symmetric(
                        new ThresholdFuncBitmap(T), container, bitmaps);
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
                final EWAHIterator i = this.getEWAHIterator();
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
                                long data = i.buffer()[i.literalWords() + j];
                                while (data != 0) {
                                        final long T = data & -data;
                                        ans[inanspos++] = Long.bitCount(T - 1)
                                                + pos;
                                        data ^= T;
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
                String ans = " EWAHCompressedBitmap, size in bits = "
                        + this.sizeinbits + " size in words = "
                        + this.actualsizeinwords + "\n";
                final EWAHIterator i = this.getEWAHIterator();
                while (i.hasNext()) {
                        RunningLengthWord localrlw = i.next();
                        if (localrlw.getRunningBit()) {
                                ans += localrlw.getRunningLength() + " 1x11\n";
                        } else {
                                ans += localrlw.getRunningLength() + " 0x00\n";
                        }
                        ans += localrlw.getNumberOfLiteralWords()
                                + " dirties\n";
                        for (int j = 0; j < localrlw.getNumberOfLiteralWords(); ++j) {
                                long data = i.buffer()[i.literalWords() + j];
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
                StringBuffer answer = new StringBuffer();
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
         * @param other
         *                bitmap to swap with
         */
        public void swap(final EWAHCompressedBitmap other) {
                long[] tmp = this.buffer;
                this.buffer = other.buffer;
                other.buffer = tmp;

                int tmp2 = this.rlw.position;
                this.rlw.position = other.rlw.position;
                other.rlw.position = tmp2;

                int tmp3 = this.actualsizeinwords;
                this.actualsizeinwords = other.actualsizeinwords;
                other.actualsizeinwords = tmp3;

                int tmp4 = this.sizeinbits;
                this.sizeinbits = other.sizeinbits;
                other.sizeinbits = tmp4;
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
         * @param a
         *                the other bitmap
         * @return the EWAH compressed bitmap
         */
        @Override
        public EWAHCompressedBitmap xor(final EWAHCompressedBitmap a) {
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
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
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @param container
         *                where we store the result
         */
        public void xorToContainer(final EWAHCompressedBitmap a,
                final BitmapStorage container) {
                final EWAHIterator i = a.getEWAHIterator();
                final EWAHIterator j = getEWAHIterator();
                final IteratingBufferedRunningLengthWord rlwi = new IteratingBufferedRunningLengthWord(
                        i);
                final IteratingBufferedRunningLengthWord rlwj = new IteratingBufferedRunningLengthWord(
                        j);
                while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
                        while ((rlwi.getRunningLength() > 0)
                                || (rlwj.getRunningLength() > 0)) {
                                final boolean i_is_prey = rlwi
                                        .getRunningLength() < rlwj
                                        .getRunningLength();
                                final IteratingBufferedRunningLengthWord prey = i_is_prey ? rlwi
                                        : rlwj;
                                final IteratingBufferedRunningLengthWord predator = i_is_prey ? rlwj
                                        : rlwi;
                                if (predator.getRunningBit() == false) {
                                        long index = prey.discharge(container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(false,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                } else {
                                        long index = prey.dischargeNegated(
                                                container,
                                                predator.getRunningLength());
                                        container.addStreamOfEmptyWords(true,
                                                predator.getRunningLength()
                                                        - index);
                                        predator.discardFirstWords(predator
                                                .getRunningLength());
                                }
                        }
                        final int nbre_literal = Math.min(
                                rlwi.getNumberOfLiteralWords(),
                                rlwj.getNumberOfLiteralWords());
                        if (nbre_literal > 0) {
                                for (int k = 0; k < nbre_literal; ++k)
                                        container.add(rlwi.getLiteralWordAt(k)
                                                ^ rlwj.getLiteralWordAt(k));
                                rlwi.discardFirstWords(nbre_literal);
                                rlwj.discardFirstWords(nbre_literal);
                        }
                }
                final boolean i_remains = rlwi.size() > 0;
                final IteratingBufferedRunningLengthWord remaining = i_remains ? rlwi
                        : rlwj;
                remaining.discharge(container);
                container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
        }

        /**
         * Returns the cardinality of the result of a bitwise XOR of the values
         * of the current bitmap with some other bitmap. Avoids needing to
         * allocate an intermediate bitmap to hold the result of the OR.
         * 
         * @since 0.4.0
         * @param a
         *                the other bitmap
         * @return the cardinality
         */
        public int xorCardinality(final EWAHCompressedBitmap a) {
                final BitCounter counter = new BitCounter();
                xorToContainer(a, counter);
                return counter.getCount();
        }

        /**
         * For internal use. Computes the bitwise and of the provided bitmaps
         * and stores the result in the container.
         * 
         * @param container
         *                where the result is stored
         * @param bitmaps
         *                bitmaps to AND
         * @since 0.4.3
         */
        public static void andWithContainer(final BitmapStorage container,
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        throw new IllegalArgumentException(
                                "Need at least one bitmap");
                if (bitmaps.length == 2) {
                        bitmaps[0].andToContainer(bitmaps[1], container);
                        return;
                }
                EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
                EWAHCompressedBitmap tmp = new EWAHCompressedBitmap();
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
         * @since 0.4.3
         * @param bitmaps
         *                bitmaps to AND together
         * @return result of the AND
         */
        public static EWAHCompressedBitmap and(
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        return bitmaps[0];
                if (bitmaps.length == 2)
                        return bitmaps[0].and(bitmaps[1]);
                EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
                EWAHCompressedBitmap tmp = new EWAHCompressedBitmap();
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
         * of the provided bitmaps. Avoids needing to allocate an intermediate
         * bitmap to hold the result of the AND.
         * 
         * @since 0.4.3
         * @param bitmaps
         *                bitmaps to AND
         * @return the cardinality
         */
        public static int andCardinality(final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        return bitmaps[0].cardinality();
                final BitCounter counter = new BitCounter();
                andWithContainer(counter, bitmaps);
                return counter.getCount();
        }

        /**
         * Return a bitmap with the bit set to true at the given positions. The
         * positions should be given in sorted order.
         * 
         * (This is a convenience method.)
         * 
         * @since 0.4.5
         * @param setbits
         *                list of set bit positions
         * @return the bitmap
         */
        public static EWAHCompressedBitmap bitmapOf(int... setbits) {
                EWAHCompressedBitmap a = new EWAHCompressedBitmap();
                for (int k : setbits)
                        a.set(k);
                return a;
        }

        /**
         * For internal use. This simply adds a stream of words made of zeroes
         * so that we pad to the desired size.
         * 
         * @param storage
         *                bitmap to extend
         * @param currentSize
         *                current size (in bits)
         * @param newSize
         *                new desired size (in bits)
         * @since 0.4.3
         */
        private static void extendEmptyBits(final BitmapStorage storage,
                final int currentSize, final int newSize) {
                final int currentLeftover = currentSize % wordinbits;
                final int finalLeftover = newSize % wordinbits;
                storage.addStreamOfEmptyWords(false, (newSize / wordinbits)
                        - currentSize / wordinbits
                        + (finalLeftover != 0 ? 1 : 0)
                        + (currentLeftover != 0 ? -1 : 0));
        }

        /**
         * Uses an adaptive technique to compute the logical OR. Mostly for
         * internal use.
         * 
         * @param container
         *                where the aggregate is written.
         * @param bitmaps
         *                to be aggregated
         */
        public static void orWithContainer(final BitmapStorage container,
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length < 2)
                        throw new IllegalArgumentException(
                                "You should provide at least two bitmaps, provided "
                                        + bitmaps.length);
                long size = 0L;
                long sinbits = 0L;
                for (EWAHCompressedBitmap b : bitmaps) {
                        size += b.sizeInBytes();
                        if (sinbits < b.sizeInBits())
                                sinbits = b.sizeInBits();
                }
                if (size * 8 > sinbits) {
                        FastAggregation.bufferedorWithContainer(container,
                                65536, bitmaps);
                } else {
                        FastAggregation.orToContainer(container, bitmaps);
                }
        }

        /**
         * Uses an adaptive technique to compute the logical XOR. Mostly for
         * internal use.
         * 
         * @param container
         *                where the aggregate is written.
         * @param bitmaps
         *                to be aggregated
         */
        public static void xorWithContainer(final BitmapStorage container,
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length < 2)
                        throw new IllegalArgumentException(
                                "You should provide at least two bitmaps, provided "
                                        + bitmaps.length);
                long size = 0L;
                long sinbits = 0L;
                for (EWAHCompressedBitmap b : bitmaps) {
                        size += b.sizeInBytes();
                        if (sinbits < b.sizeInBits())
                                sinbits = b.sizeInBits();
                }
                if (size * 8 > sinbits) {
                        FastAggregation.bufferedxorWithContainer(container,
                                65536, bitmaps);
                } else {
                        FastAggregation.xorToContainer(container, bitmaps);
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
         * @since 0.4.0
         * @param bitmaps
         *                bitmaps to OR together
         * @return result of the OR
         */
        public static EWAHCompressedBitmap or(
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        return bitmaps[0];
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                int largestSize = 0;
                for (EWAHCompressedBitmap bitmap : bitmaps) {
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
         * @param bitmaps
         *                bitmaps to XOR together
         * @return result of the XOR
         */
        public static EWAHCompressedBitmap xor(
                final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        return bitmaps[0];
                final EWAHCompressedBitmap container = new EWAHCompressedBitmap();
                int largestSize = 0;
                for (EWAHCompressedBitmap bitmap : bitmaps) {
                        largestSize = Math.max(bitmap.actualsizeinwords,
                                largestSize);
                }
                container.reserve((int) (largestSize * 1.5));
                xorWithContainer(container, bitmaps);
                return container;
        }

        /**
         * Returns the cardinality of the result of a bitwise OR of the values
         * of the provided bitmaps. Avoids needing to allocate an intermediate
         * bitmap to hold the result of the OR.
         * 
         * @since 0.4.0
         * @param bitmaps
         *                bitmaps to OR
         * @return the cardinality
         */
        public static int orCardinality(final EWAHCompressedBitmap... bitmaps) {
                if (bitmaps.length == 1)
                        return bitmaps[0].cardinality();
                final BitCounter counter = new BitCounter();
                orWithContainer(counter, bitmaps);
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
         * The Constant defaultbuffersize: default memory allocation when the
         * object is constructed.
         */
        static final int defaultbuffersize = 4;

        /** whether we adjust after some aggregation by adding in zeroes **/
        public static final boolean adjustContainerSizeWhenAggregating = true;

        /** The Constant wordinbits represents the number of bits in a long. */
        public static final int wordinbits = 64;

}
