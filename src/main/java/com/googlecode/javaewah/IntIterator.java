package com.googlecode.javaewah;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * 
 * The IntIterator interface is used to iterate over a stream of integers.
 * 
 * @author Daniel Lemire
 * @since 0.2.0
 * 
 */
public interface IntIterator {

        /**
         * Is there more?
         * 
         * @return true, if there is more, false otherwise
         */
        public boolean hasNext();

        /**
         * Return the next integer
         * 
         * @return the integer
         */
        public int next();
}
