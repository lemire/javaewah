package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * The ChunkIterator interface is used to iterate over chunks of ones or zeros.
 *
 * @author Gregory Ssi-Yan-Kai
 */
public interface ChunkIterator {

    /**
     * Is there more?
     *
     * @return true, if there is more, false otherwise
     */
    boolean hasNext();

    /**
     * Return the next bit
     *
     * @return the bit
     */
    boolean nextBit();

    /**
     * Return the length of the next bit
     *
     * @return the length
     */
    int nextLength();

    /**
     * Move the iterator at the next different bit
     */
    void move();

    /**
     * Move the iterator at the next ith bit
     *
     * @param bits  the number of bits to skip
     */
    void move(int bits);

}
