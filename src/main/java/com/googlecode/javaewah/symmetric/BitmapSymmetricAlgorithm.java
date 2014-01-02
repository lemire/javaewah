package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.*;

/**
 * Generic interface to compute symmetric boolean query.
 * @author Daniel Lemire
 *
 */
public interface BitmapSymmetricAlgorithm {
        /**
         * Compute a Boolean symmetric query.
         * 
         * @param f symmetric boolean function to be processed
         * @param out the result of the query
         * @param set the inputs
         */
        public void symmetric(UpdateableBitmapFunction f, BitmapStorage out, EWAHCompressedBitmap ... set);
}
