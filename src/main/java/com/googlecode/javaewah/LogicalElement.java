package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * A prototypical model for bitmaps. Used by the class FastAggregation. Users
 * should probably not be concerned by this class.
 *
 * @param <T> the type of element (e.g., a bitmap class)
 * @author Daniel Lemire
 */
public interface LogicalElement<T> {
    /**
     * Compute the bitwise logical and
     *
     * @param le element
     * @return the result of the operation
     */
    T and(T le);

    /**
     * Compute the bitwise logical and not
     *
     * @param le element
     * @return the result of the operation
     */
    T andNot(T le);

    /**
     * Compute the bitwise logical not (in place)
     */
    void not();

    /**
     * Compute the bitwise logical or
     * @param le another element
     * @return the result of the operation
     */
    T or(T le);

    /**
     * How many logical bits does this element represent?
     *
     * @return the number of bits represented by this element
     */
    int sizeInBits();

    /**
     * Should report the storage requirement
     *
     * @return How many bytes
     * @since 0.6.2
     */
    int sizeInBytes();

    /**
     * Compute the bitwise logical Xor
     *
     * @param le element
     * @return the result of the operation
     */
    T xor(T le);

    /**
     * Compute the composition
     *
     * @param le another element
     * @return the result of the operation
     */
    T compose(T le);
}
