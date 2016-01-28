package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * The IntIterator interface is used to iterate over a stream of integers.
 *
 * @author Daniel Lemire
 * @since 0.2.0
 */
public interface IntIterator {

    /**
     * Is there more?
     *
     * @return true, if there is more, false otherwise
     */
    boolean hasNext();

    /**
     * Return the next integer
     *
     * @return the integer
     */
    int next();
}
