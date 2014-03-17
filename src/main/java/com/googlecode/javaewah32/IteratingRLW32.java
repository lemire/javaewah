package com.googlecode.javaewah32;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * High-level iterator over a compressed bitmap.
 * 
 */
public interface IteratingRLW32 {
        /**
         * @return whether there is more
         */
        public boolean next();

        /**
         * @param index
         *                where the literal word is
         * @return the literal word at the given index.
         */
        public int getLiteralWordAt(int index);

        /**
         * @return the number of literal (non-fill) words
         */
        public int getNumberOfLiteralWords();

        /**
         * @return the bit used for the fill bits
         */
        public boolean getRunningBit();

        /**
         * @return sum of getRunningLength() and getNumberOfLiteralWords()
         */
        public int size();

        /**
         * @return length of the run of fill words
         */
        public int getRunningLength();

        /**
         * @param x
         *                the number of words to discard
         */
        public void discardFirstWords(int x);
        
        /**
         * Discard all running words
         */
        public void discardRunningWords();
        
        /**
         * @return a copy of the iterator
         * @throws CloneNotSupportedException
         *                 this should not be thrown in theory
         */
        public IteratingRLW32 clone() throws CloneNotSupportedException;

}
