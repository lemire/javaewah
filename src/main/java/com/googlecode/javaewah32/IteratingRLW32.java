package com.googlecode.javaewah32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
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
    boolean next();

    /**
     * @param index where the literal word is
     * @return the literal word at the given index.
     */
    int getLiteralWordAt(int index);

    /**
     * @return the number of literal (non-fill) words
     */
    int getNumberOfLiteralWords();

    /**
     * @return the bit used for the fill bits
     */
    boolean getRunningBit();

    /**
     * @return sum of getRunningLength() and getNumberOfLiteralWords()
     */
    int size();

    /**
     * @return length of the run of fill words
     */
    int getRunningLength();

    /**
     * @param x the number of words to discard
     */
    void discardFirstWords(int x);

    /**
     * Discard all running words
     */
    void discardRunningWords();
    
    /**
     * Discard x literal words (assumes that there is no running word)
     * @param x the number of words to discard
     */
    void discardLiteralWords(int x);

    /**
     * @return a copy of the iterator
     * @throws CloneNotSupportedException this should not be thrown in theory
     */
    IteratingRLW32 clone() throws CloneNotSupportedException;
}
