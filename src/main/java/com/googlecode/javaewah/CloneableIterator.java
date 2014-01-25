package com.googlecode.javaewah;

/**
 * Like a standard Java iterator, except that you can clone it.
 * 
 * @param <E>
 *                the data type of the iterator
 */
public interface CloneableIterator<E> extends Cloneable {

        /**
         * @return whether there is more
         */
        public boolean hasNext();

        /**
         * @return the next element
         */
        public E next();

        /**
         * @return a copy
         * @throws CloneNotSupportedException
         *                 this should never happen in practice
         */
        public CloneableIterator<E> clone() throws CloneNotSupportedException;

}