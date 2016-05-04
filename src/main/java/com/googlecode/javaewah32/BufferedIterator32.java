package com.googlecode.javaewah32;

import com.googlecode.javaewah.CloneableIterator;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * This class can be used to iterate over blocks of bitmap data.
 *
 * @author Daniel Lemire
 */
public class BufferedIterator32 implements IteratingRLW32, Cloneable {
    /**
     * Instantiates a new iterating buffered running length word.
     *
     * @param iterator iterator
     */
    public BufferedIterator32(
            final CloneableIterator<EWAHIterator32> iterator) {
        this.masterIterator = iterator;
        if (this.masterIterator.hasNext()) {
            iteratingBrlw = new IteratingBufferedRunningLengthWord32(this.masterIterator.next());
        }
    }

    /**
     * Discard first words, iterating to the next running length word if
     * needed.
     *
     * @param x the number of words to be discarded
     */
    @Override
    public void discardFirstWords(int x) {
        while (x > 0) {
            if (this.iteratingBrlw.getRunningLength() > x) {
                this.iteratingBrlw.discardFirstWords(x);
                return;
            }
            this.iteratingBrlw.discardFirstWords(this.iteratingBrlw.getRunningLength());
            x -= this.iteratingBrlw.getRunningLength();
            int toDiscard = x > this.iteratingBrlw.getNumberOfLiteralWords()
                    ? this.iteratingBrlw.getNumberOfLiteralWords()
                    : x;

            this.iteratingBrlw.discardFirstWords(toDiscard);
            x -= toDiscard;
            if ((x > 0) || (this.iteratingBrlw.size() == 0)) {
                if (!this.next()) {
                    break;
                }
            }
        }
    }
    
	@Override
	public void discardLiteralWords(int x) {
		this.iteratingBrlw.discardLiteralWords(x);
        if (this.iteratingBrlw.getNumberOfLiteralWords() == 0)
            this.next();
	}


    @Override
    public void discardRunningWords() {
        this.iteratingBrlw.discardRunningWords();
        if (this.iteratingBrlw.getNumberOfLiteralWords() == 0)
            this.next();
    }

    /**
     * Move to the next RunningLengthWord
     *
     * @return whether the move was possible
     */
    @Override
    public boolean next() {
        if (!this.iteratingBrlw.next()) {
            if (!this.masterIterator.hasNext()) {
                return false;
            } else {
                this.iteratingBrlw = new IteratingBufferedRunningLengthWord32(this.masterIterator.next());
            }
        }
        return true;
    }

    /**
     * Get the nth literal word for the current running length word
     *
     * @param index zero based index
     * @return the literal word
     */
    @Override
    public int getLiteralWordAt(int index) {
        return this.iteratingBrlw.getLiteralWordAt(index);
    }

    /**
     * Gets the number of literal words for the current running length word.
     *
     * @return the number of literal words
     */
    @Override
    public int getNumberOfLiteralWords() {
        return this.iteratingBrlw.getNumberOfLiteralWords();
    }

    /**
     * Gets the running bit.
     *RunningBit
     * @return the running bit
     */
    @Override
    public boolean getRunningBit() {
        return this.iteratingBrlw.getRunningBit();
    }

    /**
     * Gets the running length.
     *
     * @return the running length
     */
    @Override
    public int getRunningLength() {
        return this.iteratingBrlw.getRunningLength();
    }

    /**
     * Size in uncompressed words of the current running length word.
     *
     * @return the size
     */
    @Override
    public int size() {
        return this.iteratingBrlw.size();
    }

    @Override
    public BufferedIterator32 clone() throws CloneNotSupportedException {
        BufferedIterator32 answer = (BufferedIterator32) super.clone();
        answer.iteratingBrlw = this.iteratingBrlw.clone();
        answer.masterIterator = this.masterIterator.clone();
        return answer;
    }

    private IteratingBufferedRunningLengthWord32 iteratingBrlw;
    private CloneableIterator<EWAHIterator32> masterIterator;

}
