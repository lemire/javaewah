package com.googlecode.javaewah.symmetric;

import java.util.Iterator;
import com.googlecode.javaewah.datastructure.StaticBitSet;
import com.googlecode.javaewah.BitmapStorage;

/**
 * 
 * This is a Java specification for an "updatable" Boolean function meant to
 * run over EWAH bitmaps.
 *  
 *  Reference: 
 *  
 *  Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 *  bitmap indexes. Data & Knowledge Engineering 69 (1), pages 3-28, 2010. 
 * 
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
        public int getNumberOfLiterals() {
                return litwlist.cardinality();
        }

        /**
         * Goes through the literals.
         * @return an iterator
         */
        public Iterable<EWAHPointer> getLiterals() {
                return new Iterable<EWAHPointer>() {

                        @Override
                        public Iterator<EWAHPointer> iterator() {
                                return new Iterator<EWAHPointer>() {
                                        int k = litwlist.nextSetBit(0);

                                        @Override
                                        public boolean hasNext() {
                                                return k >= 0;
                                        }

                                        @Override
                                        public EWAHPointer next() {
                                                EWAHPointer answer = rw[k];
                                                k = litwlist.nextSetBit(k + 1);
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
         * @param newsize the number of inputs
         */
        public void resize(final int newsize) {
                rw = java.util.Arrays.copyOf(rw, newsize);
                litwlist.resize(newsize);
                b = java.util.Arrays.copyOf(b, newsize);
        }


        /**
         * @param pos position of a literal
         */
        public void setLiteral(final int pos) {
                if (!litwlist.get(pos)) {
                        litwlist.set(pos);
                        litWeight++;
                        if (b[pos]) {
                                b[pos] = false;
                                --hammingWeight;
                        }
                }
        }

        /**
         * @param pos position where a literal was removed
         */
        public void clearLiteral(final int pos) {
                if (litwlist.get(pos)) {
                        // litwlist.unset(pos);
                        litwlist.set(pos, false);
                        litWeight--;
                }
        }

        /**
         * @param pos position where a zero word was added
         */
        public void setZero(final int pos) {
                if (b[pos]) {
                        b[pos] = false;
                        --hammingWeight;
                } else {
                        clearLiteral(pos);
                }
        }

        /**
         * @param pos position were a 11...1 word was added
         */
        public void setOne(final int pos) {
                if (!b[pos]) {
                        clearLiteral(pos);
                        b[pos] = true;
                        ++hammingWeight;
                }
        }

        /**
         * Writes out the answer.
         * 
         * @param out output buffer
         * @param runbegin beginning of the run
         * @param runend end of the run
         */
        public abstract void dispatch(BitmapStorage out, int runbegin,
                int runend);

}
