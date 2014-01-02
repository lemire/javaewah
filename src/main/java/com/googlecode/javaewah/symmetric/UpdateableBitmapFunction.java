package com.googlecode.javaewah.symmetric;

import java.util.Iterator;
import com.googlecode.javaewah.datastructure.StaticBitSet;
import com.googlecode.javaewah.BitmapStorage;

/**
 * 
 * @author lemire
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
        
        public int getNumberOfLiterals() {
                return litwlist.cardinality();
        }

        /**
         * Goes through the literals.
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

        public void resize(int newsize) {
                rw = java.util.Arrays.copyOf(rw, newsize);
                litwlist.resize(newsize);
                b = java.util.Arrays.copyOf(b, newsize);
        }

        public int size() {
                return rw.length;
        }

        public void setLiteral(int pos) {
                if (!litwlist.get(pos)) {
                        litwlist.set(pos);
                        litWeight++;
                        if (b[pos]) {
                                b[pos] = false;
                                --hammingWeight;
                        }
                }
        }

        public void clearLiteral(int pos) {
                if (litwlist.get(pos)) {
                        // litwlist.unset(pos);
                        litwlist.set(pos, false);
                        litWeight--;
                }
        }

        public void setZero(int pos) {
                if (b[pos]) {
                        b[pos] = false;
                        --hammingWeight;
                } else {
                        clearLiteral(pos);
                }
        }

        public void setOne(int pos) {
                if (!b[pos]) {
                        clearLiteral(pos);
                        b[pos] = true;
                        ++hammingWeight;
                }
        }

        public abstract void dispatch(BitmapStorage out, int runbegin,
                int runend);

}
