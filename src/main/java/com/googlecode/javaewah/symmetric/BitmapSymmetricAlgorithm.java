package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.*;

/**
 * Generic interface to compute symmetric Boolean functions.
 * 
 * @see <a
 *      href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @author Daniel Lemire
 * @since 0.8.0
 **/
public interface BitmapSymmetricAlgorithm {
        /**
         * Compute a Boolean symmetric query.
         * 
         * @param f
         *                symmetric boolean function to be processed
         * @param out
         *                the result of the query
         * @param set
         *                the inputs
         */
        public void symmetric(UpdateableBitmapFunction f, BitmapStorage out,
                EWAHCompressedBitmap... set);
}
