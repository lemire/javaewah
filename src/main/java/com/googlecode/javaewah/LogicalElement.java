package com.googlecode.javaewah;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * A prototypical model for bitmaps. Used by the class FastAggregation. Users
 * should probably not be concerned by this class.
 * 
 * @author Daniel Lemire
 * @param <T>
 *                the type of element (e.g., a bitmap class)
 * 
 */
public interface LogicalElement<T> {
        /**
         * Compute the bitwise logical and
         * 
         * @param le
         *                element
         * @return the result of the operation
         */
        public T and(T le);

        /**
         * Compute the bitwise logical and not
         * 
         * @param le
         *                element
         * @return the result of the operation
         */
        public T andNot(T le);

        /**
         * Compute the bitwise logical not (in place)
         */
        public void not();

        @SuppressWarnings({ "rawtypes", "javadoc" })
        /**
         * Compute the bitwise logical or
         * @param le another element
         * @return the result of the operation
         */
        public LogicalElement or(T le);

        /**
         * How many logical bits does this element represent?
         * 
         * @return the number of bits represented by this element
         */
        public int sizeInBits();

        /**
         * Should report the storage requirement
         * 
         * @return How many bytes
         * @since 0.6.2
         */
        public int sizeInBytes();

        /**
         * Compute the bitwise logical Xor
         * 
         * @param le
         *                element
         * @return the results of the operation
         */
        public T xor(T le);
}
