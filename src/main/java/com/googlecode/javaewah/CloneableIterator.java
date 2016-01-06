package com.googlecode.javaewah;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Like a standard Java iterator, except that you can clone it.
 *
 * @param <E> the data type of the iterator
 */
public interface CloneableIterator<E> extends Cloneable {

    /**
     * @return whether there is more
     */
    boolean hasNext();

    /**
     * @return the next element
     */
    E next();

    /**
     * @return a copy
     * @throws CloneNotSupportedException this should never happen in practice
     */
    CloneableIterator<E> clone() throws CloneNotSupportedException;

}
