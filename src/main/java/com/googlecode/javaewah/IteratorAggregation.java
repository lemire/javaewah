package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Set of helper functions to aggregate bitmaps.
 */
public final class IteratorAggregation {

    /** Private constructor to prevent instantiation */
    private IteratorAggregation() {}

    /**
     * @param x iterator to negate
     * @return negated version of the iterator
     */
    public static IteratingRLW not(final IteratingRLW x) {
        return new IteratingRLW() {

            @Override
            public boolean next() {
                return x.next();
            }

            @Override
            public long getLiteralWordAt(int index) {
                return ~x.getLiteralWordAt(index);
            }

            @Override
            public int getNumberOfLiteralWords() {
                return x.getNumberOfLiteralWords();
            }

            @Override
            public boolean getRunningBit() {
                return !x.getRunningBit();
            }

            @Override
            public long size() {
                return x.size();
            }

            @Override
            public long getRunningLength() {
                return x.getRunningLength();
            }

            @Override
            public void discardFirstWords(long y) {
                x.discardFirstWords(y);
            }

            @Override
            public void discardRunningWords() {
                x.discardRunningWords();
            }

            @Override
            public IteratingRLW clone()
                    throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

			@Override
			public void discardLiteralWords(long y) {
				x.discardLiteralWords(y);
			}
        };
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return and aggregate
     */
    public static IteratingRLW bufferedand(final IteratingRLW... al) {
        return bufferedand(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      set of iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words (per input iterator)
     * @return and aggregate
     */
    public static IteratingRLW bufferedand(final int bufSize, final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];
        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);
        return new BufferedIterator(new BufferedAndIterator(basell,
                bufSize));
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return or aggregate
     */
    public static IteratingRLW bufferedor(final IteratingRLW... al) {
        return bufferedor(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in
     *                64-bit words
     * @return or aggregate
     */
    public static IteratingRLW bufferedor(final int bufSize,
                                          final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);
        return new BufferedIterator(new BufferedORIterator(basell, bufSize));
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al set of iterators to aggregate
     * @return xor aggregate
     */
    public static IteratingRLW bufferedxor(final IteratingRLW... al) {
        return bufferedxor(DEFAULT_MAX_BUF_SIZE, al);
    }

    /**
     * Aggregate the iterators using a bitmap buffer.
     *
     * @param al      iterators to aggregate
     * @param bufSize size of the internal buffer used by the iterator in 64-bit words
     * @return xor aggregate
     */
    public static IteratingRLW bufferedxor(final int bufSize, final IteratingRLW... al) {
        if (al.length == 0)
            throw new IllegalArgumentException("Need at least one iterator");
        if (al.length == 1)
            return al[0];

        final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
        Collections.addAll(basell, al);

        return new BufferedIterator(new BufferedXORIterator(basell, bufSize));
    }

    /**
     * Write out the content of the iterator, but as if it were all zeros.
     *
     * @param container where we write
     * @param i         the iterator
     */
    protected static void dischargeAsEmpty(final BitmapStorage container,
                                           final IteratingRLW i) {
        while (i.size() > 0) {
            container.addStreamOfEmptyWords(false, i.size());
            i.next();
        }
    }

    /**
     * Write out up to max words, returns how many were written
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */

    protected static long discharge(final BitmapStorage container, IteratingRLW i, long max) {
        long counter = 0;
        while (i.size() > 0 && counter < max) {
            long l1 = i.getRunningLength();
            if (l1 > 0) {
                if (l1 + counter > max)
                    l1 = max - counter;
                container.addStreamOfEmptyWords(i.getRunningBit(), l1);
                counter += l1;
            }
            long l = i.getNumberOfLiteralWords();
            if (l + counter > max)
                l = max - counter;
            for (int k = 0; k < l; ++k) {
                container.addWord(i.getLiteralWordAt(k));
            }
            counter += l;
            i.discardFirstWords(l + l1);
        }
        return counter;
    }

    /**
     * Write out up to max negated words, returns how many were written
     *
     * @param container target for writes
     * @param i         source of data
     * @param max       maximal number of writes
     * @return how many written
     */
    protected static long dischargeNegated(final BitmapStorage container, IteratingRLW i, long max) {
        long counter = 0;
        while (i.size() > 0 && counter < max) {
            long l1 = i.getRunningLength();
            if (l1 > 0) {
                if (l1 + counter > max)
                    l1 = max - counter;
                container.addStreamOfEmptyWords(!i.getRunningBit(), l1);
                counter += l1;
            }
            long l = i.getNumberOfLiteralWords();
            if (l + counter > max)
                l = max - counter;
            for (int k = 0; k < l; ++k) {
                container.addWord(~i.getLiteralWordAt(k));
            }
            counter += l;
            i.discardFirstWords(l + l1);
        }
        return counter;
    }

    static void andToContainer(final BitmapStorage container,
                               int desiredrlwcount, final IteratingRLW rlwi, IteratingRLW rlwj) {
        while ((rlwi.size() > 0) && (rlwj.size() > 0)
                && (desiredrlwcount-- > 0)) {
            while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi.getRunningLength() < rlwj.getRunningLength();
                final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
                final IteratingRLW predator = i_is_prey ? rlwj : rlwi;
                if (!predator.getRunningBit()) {
                    container.addStreamOfEmptyWords(false, predator.getRunningLength());
                    prey.discardFirstWords(predator.getRunningLength());
                    predator.discardFirstWords(predator.getRunningLength());
                } else {
                    final long index = discharge(container, prey, predator.getRunningLength());
                    container.addStreamOfEmptyWords(false, predator.getRunningLength() - index);
                    predator.discardFirstWords(predator.getRunningLength());
                }
            }
            final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(), rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                desiredrlwcount -= nbre_literal;
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
    }

    static void andToContainer(final BitmapStorage container,
                               final IteratingRLW rlwi, IteratingRLW rlwj) {
        while ((rlwi.size() > 0) && (rlwj.size() > 0)) {
            while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi.getRunningLength() < rlwj.getRunningLength();
                final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
                final IteratingRLW predator = i_is_prey ? rlwj : rlwi;
                if (!predator.getRunningBit()) {
                    container.addStreamOfEmptyWords(false, predator.getRunningLength());
                    prey.discardFirstWords(predator.getRunningLength());
                    predator.discardFirstWords(predator.getRunningLength());
                } else {
                    final long index = discharge(container, prey, predator.getRunningLength());
                    container.addStreamOfEmptyWords(false, predator.getRunningLength() - index);
                    predator.discardFirstWords(predator.getRunningLength());
                }
            }
            final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(), rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
    }

    /**
     * Compute the first few words of the XOR aggregate between two
     * iterators.
     *
     * @param container       where to write
     * @param desiredrlwcount number of words to be written (max)
     * @param rlwi            first iterator to aggregate
     * @param rlwj            second iterator to aggregate
     */
    public static void xorToContainer(final BitmapStorage container,
                                      int desiredrlwcount, final IteratingRLW rlwi,
                                      final IteratingRLW rlwj) {
        while ((rlwi.size() > 0) && (rlwj.size() > 0)
                && (desiredrlwcount-- > 0)) {
            while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
                final boolean i_is_prey = rlwi.getRunningLength() < rlwj.getRunningLength();
                final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
                final IteratingRLW predator = i_is_prey ? rlwj : rlwi;
                if (!predator.getRunningBit()) {
                    long index = discharge(container, prey, predator.getRunningLength());
                    container.addStreamOfEmptyWords(false, predator.getRunningLength() - index);
                    predator.discardFirstWords(predator.getRunningLength());
                } else {
                    long index = dischargeNegated(container, prey, predator.getRunningLength());
                    container.addStreamOfEmptyWords(true, predator.getRunningLength() - index);
                    predator.discardFirstWords(predator.getRunningLength());
                }
            }
            final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(), rlwj.getNumberOfLiteralWords());
            if (nbre_literal > 0) {
                desiredrlwcount -= nbre_literal;
                for (int k = 0; k < nbre_literal; ++k)
                    container.addWord(rlwi.getLiteralWordAt(k) ^ rlwj.getLiteralWordAt(k));
                rlwi.discardFirstWords(nbre_literal);
                rlwj.discardFirstWords(nbre_literal);
            }
        }
    }

    protected static int inplaceor(long[] bitmap, IteratingRLW i) {

        int pos = 0;
        long s;
        while ((s = i.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int L = (int) i.getRunningLength();
                if (i.getRunningBit())
                    java.util.Arrays.fill(bitmap, pos, pos + L, ~0l);
                pos += L;
                final int LR = i.getNumberOfLiteralWords();

                for (int k = 0; k < LR; ++k)
                    bitmap[pos++] |= i.getLiteralWordAt(k);
                if (!i.next()) {
                    return pos;
                }
            } else {
                int howmany = bitmap.length - pos;
                int l = (int) i.getRunningLength();

                if (pos + l > bitmap.length) {
                    if (i.getRunningBit()) {
                        java.util.Arrays.fill(bitmap, pos, bitmap.length, ~0l);
                    }
                    i.discardFirstWords(howmany);
                    return bitmap.length;
                }
                if (i.getRunningBit())
                    java.util.Arrays.fill(bitmap, pos, pos + l, ~0l);
                pos += l;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] |= i.getLiteralWordAt(k);
                i.discardFirstWords(howmany);
                return pos;
            }
        }
        return pos;
    }

    protected static int inplacexor(long[] bitmap, IteratingRLW i) {
        int pos = 0;
        long s;
        while ((s = i.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int L = (int) i.getRunningLength();
                if (i.getRunningBit()) {
                    for (int k = pos; k < pos + L; ++k)
                        bitmap[k] = ~bitmap[k];
                }
                pos += L;
                final int LR = i.getNumberOfLiteralWords();
                for (int k = 0; k < LR; ++k)
                    bitmap[pos++] ^= i.getLiteralWordAt(k);
                if (!i.next()) {
                    return pos;
                }
            } else {
                int howMany = bitmap.length - pos;
                int l = (int) i.getRunningLength();
                if (pos + l > bitmap.length) {
                    if (i.getRunningBit()) {
                        for (int k = pos; k < bitmap.length; ++k)
                            bitmap[k] = ~bitmap[k];
                    }
                    i.discardFirstWords(howMany);
                    return bitmap.length;
                }
                if (i.getRunningBit())
                    for (int k = pos; k < pos + l; ++k)
                        bitmap[k] = ~bitmap[k];
                pos += l;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] ^= i.getLiteralWordAt(k);
                i.discardFirstWords(howMany);
                return pos;
            }
        }
        return pos;
    }

    protected static int inplaceand(long[] bitmap, IteratingRLW i) {
        int pos = 0;
        long s;
        while ((s = i.size()) > 0) {
            if (pos + s < bitmap.length) {
                final int L = (int) i.getRunningLength();
                if (!i.getRunningBit()) {
                    for (int k = pos; k < pos + L; ++k)
                        bitmap[k] = 0;
                }
                pos += L;
                final int LR = i.getNumberOfLiteralWords();
                for (int k = 0; k < LR; ++k)
                    bitmap[pos++] &= i.getLiteralWordAt(k);
                if (!i.next()) {
                    return pos;
                }
            } else {
                int howmany = bitmap.length - pos;
                int l = (int) i.getRunningLength();
                if (pos + l > bitmap.length) {
                    if (!i.getRunningBit()) {
                        for (int k = pos; k < bitmap.length; ++k)
                            bitmap[k] = 0;
                    }
                    i.discardFirstWords(howmany);
                    return bitmap.length;
                }
                if (!i.getRunningBit())
                    for (int k = pos; k < pos + l; ++k)
                        bitmap[k] = 0;
                pos += l;
                for (int k = 0; pos < bitmap.length; ++k)
                    bitmap[pos++] &= i.getLiteralWordAt(k);
                i.discardFirstWords(howmany);
                return pos;
            }
        }
        return pos;
    }

    /**
     * An optimization option. Larger values may improve speed, but at the
     * expense of memory.
     */
    public static final int DEFAULT_MAX_BUF_SIZE = 65536;
}

class BufferedORIterator implements CloneableIterator<EWAHIterator> {
    final EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
    final long[] hardBitmap;
    final LinkedList<IteratingRLW> ll;

    BufferedORIterator(LinkedList<IteratingRLW> basell, int bufSize) {
        this.ll = basell;
        this.hardBitmap = new long[bufSize];
    }

    @Override
    public BufferedXORIterator clone() throws CloneNotSupportedException {
        BufferedXORIterator answer = (BufferedXORIterator) super
                .clone();
        answer.buffer = this.buffer.clone();
        answer.hardbitmap = this.hardBitmap.clone();
        answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
        return answer;
    }

    @Override
    public boolean hasNext() {
        return !this.ll.isEmpty();
    }

    @Override
    public EWAHIterator next() {
        this.buffer.clear();
        long effective = 0;
        Iterator<IteratingRLW> i = this.ll.iterator();
        while (i.hasNext()) {
            IteratingRLW rlw = i.next();
            if (rlw.size() > 0) {
                int eff = IteratorAggregation.inplaceor(this.hardBitmap, rlw);
                if (eff > effective)
                    effective = eff;
            } else
                i.remove();
        }
        for (int k = 0; k < effective; ++k) {
            this.buffer.addWord(this.hardBitmap[k]);
        }

        Arrays.fill(this.hardBitmap, 0);
        return this.buffer.getEWAHIterator();
    }
}

class BufferedXORIterator implements CloneableIterator<EWAHIterator> {
    EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
    long[] hardbitmap;
    LinkedList<IteratingRLW> ll;

    BufferedXORIterator(LinkedList<IteratingRLW> basell, int bufSize) {
        this.ll = basell;
        this.hardbitmap = new long[bufSize];
    }

    @Override
    public BufferedXORIterator clone() throws CloneNotSupportedException {
        BufferedXORIterator answer = (BufferedXORIterator) super.clone();
        answer.buffer = this.buffer.clone();
        answer.hardbitmap = this.hardbitmap.clone();
        answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
        return answer;
    }

    @Override
    public boolean hasNext() {
        return !this.ll.isEmpty();
    }

    @Override
    public EWAHIterator next() {
        this.buffer.clear();
        long effective = 0;
        Iterator<IteratingRLW> i = this.ll.iterator();
        while (i.hasNext()) {
            IteratingRLW rlw = i.next();
            if (rlw.size() > 0) {
                int eff = IteratorAggregation.inplacexor(this.hardbitmap, rlw);
                if (eff > effective)
                    effective = eff;
            } else
                i.remove();
        }
        for (int k = 0; k < effective; ++k)
            this.buffer.addWord(this.hardbitmap[k]);
        Arrays.fill(this.hardbitmap, 0);
        return this.buffer.getEWAHIterator();
    }
}

class BufferedAndIterator implements CloneableIterator<EWAHIterator> {
    EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
    LinkedList<IteratingRLW> ll;
    final int bufferSize;

    public BufferedAndIterator(LinkedList<IteratingRLW> basell, int bufSize) {
        this.ll = basell;
        this.bufferSize = bufSize;
    }

    @Override
    public boolean hasNext() {
        return !this.ll.isEmpty();
    }

    @Override
    public BufferedAndIterator clone() throws CloneNotSupportedException {
        BufferedAndIterator answer = (BufferedAndIterator) super.clone();
        answer.buffer = this.buffer.clone();
        answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
        return answer;
    }

    @Override
    public EWAHIterator next() {
        this.buffer.clear();
        IteratorAggregation.andToContainer(this.buffer, this.bufferSize * this.ll.size(),
                this.ll.get(0), this.ll.get(1));
        if (this.ll.size() > 2) {
            Iterator<IteratingRLW> i = this.ll.iterator();
            i.next();
            i.next();
            EWAHCompressedBitmap tmpbuffer = new EWAHCompressedBitmap();
            while (i.hasNext() && this.buffer.sizeInBytes() > 0) {
                IteratorAggregation.andToContainer(tmpbuffer, this.buffer.getIteratingRLW(), i.next());
                this.buffer.swap(tmpbuffer);
                tmpbuffer.clear();
            }
        }
        for (IteratingRLW aLl : this.ll) {
            if (aLl.size() == 0) {
                this.ll.clear();
                break;
            }
        }
        return this.buffer.getEWAHIterator();
    }
}
