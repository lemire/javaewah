package com.googlecode.javaewah.symmetric;

import java.util.Iterator;
import java.util.List;

import com.googlecode.javaewah.datastructure.StaticBitSet;
import com.googlecode.javaewah.BitmapStorage;

/**
 * 
 * This is a Java specification for an "updatable" Boolean function meant to run
 * over EWAH bitmaps.
 * 
 * Reference:
 * 
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 * 
 * @since 0.8.0
 * @author Daniel Lemire
 * 
 */
public abstract class UpdateableBitmapFunction {
        EWAHPointer[] rw = new EWAHPointer[0];
        int hammingWeight = 0;
        int litWeight = 0;
        boolean[] b = new boolean[0];
        final StaticBitSet litwlist = new StaticBitSet(0);

        UpdateableBitmapFunction() {
        }

        /**
         * @return the current number of literal words
         */
        public final int getNumberOfLiterals() {
                return this.litwlist.cardinality();
        }

        /**
         * Goes through the literals.
         * 
         * @return an iterator
         */
        public final Iterable<EWAHPointer> getLiterals() {
                return new Iterable<EWAHPointer>() {

                        @Override
                        public Iterator<EWAHPointer> iterator() {
                                return new Iterator<EWAHPointer>() {
                                        int k = UpdateableBitmapFunction.this.litwlist
                                                .nextSetBit(0);

                                        @Override
                                        public boolean hasNext() {
                                                return this.k >= 0;
                                        }

                                        @Override
                                        public EWAHPointer next() {
                                                EWAHPointer answer = UpdateableBitmapFunction.this.rw[this.k];
                                                this.k = UpdateableBitmapFunction.this.litwlist
                                                        .nextSetBit(this.k + 1);
                                                return answer;
                                        }

                                        @Override
                                        public void remove() {
                                                throw new RuntimeException(
                                                        "N/A");
                                        }
                                };
                        }
                };
        }

        /**
         * append to the list the literal words as EWAHPointer
         * 
         * @param container
         *                where we write
         */
        public final void fillWithLiterals(final List<EWAHPointer> container) {
                for (int k = this.litwlist.nextSetBit(0); k >= 0; k = this.litwlist
                        .nextSetBit(k + 1)) {
                        container.add(this.rw[k]);
                }
        }

        /**
         * @param newsize
         *                the number of inputs
         */
        public final void resize(final int newsize) {
                this.rw = java.util.Arrays.copyOf(this.rw, newsize);
                this.litwlist.resize(newsize);
                this.b = java.util.Arrays.copyOf(this.b, newsize);
        }

        /**
         * @param pos
         *                position of a literal
         */
        public void setLiteral(final int pos) {
                if (!this.litwlist.get(pos)) {
                        this.litwlist.set(pos);
                        this.litWeight++;
                        if (this.b[pos]) {
                                this.b[pos] = false;
                                --this.hammingWeight;
                        }
                }
        }

        /**
         * @param pos
         *                position where a literal was removed
         */
        public void clearLiteral(final int pos) {
                if (this.litwlist.get(pos)) {
                        // litwlist.unset(pos);
                        this.litwlist.set(pos, false);
                        this.litWeight--;
                }
        }

        /**
         * @param pos
         *                position where a zero word was added
         */
        public final void setZero(final int pos) {
                if (this.b[pos]) {
                        this.b[pos] = false;
                        --this.hammingWeight;
                } else {
                        clearLiteral(pos);
                }
        }

        /**
         * @param pos
         *                position were a 11...1 word was added
         */
        public final void setOne(final int pos) {
                if (!this.b[pos]) {
                        clearLiteral(pos);
                        this.b[pos] = true;
                        ++this.hammingWeight;
                }
        }

        /**
         * Writes out the answer.
         * 
         * @param out
         *                output buffer
         * @param runbegin
         *                beginning of the run
         * @param runend
         *                end of the run
         */
        public abstract void dispatch(BitmapStorage out, int runbegin,
                int runend);

}
