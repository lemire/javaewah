package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah32.*;

/**
 * Generic interface to compute symmetric Boolean functions.
 * 
 * @see <a
 *      href="http://en.wikipedia.org/wiki/Symmetric_Boolean_function">http://en.wikipedia.org/wiki/Symmetric_Boolean_function</a>
 * @author Daniel Lemire
 * @since 0.8.2
 **/
public interface BitmapSymmetricAlgorithm32 {
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
        public void symmetric(UpdateableBitmapFunction32 f, BitmapStorage32 out,
                EWAHCompressedBitmap32... set);
}
