package com.googlecode.javaewah32.symmetric;

import com.googlecode.javaewah32.IteratingBufferedRunningLengthWord32;

/**
 * Wrapper around an IteratingBufferedRunningLengthWord used by the
 * RunningBitmapMerge class.
 *
 * @author Daniel Lemire
 * @since 0.8.2
 */
public final class EWAHPointer32 implements Comparable<EWAHPointer32> {
    private int endrun;
    private final int pos;
    private boolean isLiteral;
    private boolean value;
    private boolean dead = false;

    /**
     * Underlying iterator
     */
    public final IteratingBufferedRunningLengthWord32 iterator;

    /**
     * Construct a pointer over an IteratingBufferedRunningLengthWord.
     *
     * @param previousEndRun word where the previous run ended
     * @param rw             the iterator
     * @param pos            current position (in word)
     */
    public EWAHPointer32(final int previousEndRun,
                         final IteratingBufferedRunningLengthWord32 rw, final int pos) {
        this.pos = pos;
        this.iterator = rw;
        if (this.iterator.getRunningLength() > 0) {
            this.endrun = previousEndRun
                    + this.iterator.getRunningLength();
            this.isLiteral = false;
            this.value = this.iterator.getRunningBit();
        } else if (this.iterator.getNumberOfLiteralWords() > 0) {
            this.isLiteral = true;
            this.endrun = previousEndRun
                    + this.iterator.getNumberOfLiteralWords();
        } else {
            this.endrun = previousEndRun;
            this.dead = true;
        }
    }

    /**
     * @return the end of the current run
     */
    public int endOfRun() {
        return this.endrun;
    }

    /**
     * @return the beginning of the current run
     */
    public int beginOfRun() {
        if (this.isLiteral)
            return this.endrun
                    - this.iterator.getNumberOfLiteralWords();
        return (this.endrun - this.iterator.getRunningLength());
    }

    /**
     * Process the next run
     */
    public void parseNextRun() {
        if ((this.isLiteral)
                || (this.iterator.getNumberOfLiteralWords() == 0)) {
            // no choice, must load next runs
            this.iterator.discardFirstWords(this.iterator.size());
            if (this.iterator.getRunningLength() > 0) {
                this.endrun += this.iterator
                        .getRunningLength();
                this.isLiteral = false;
                this.value = this.iterator.getRunningBit();
            } else if (this.iterator.getNumberOfLiteralWords() > 0) {
                this.isLiteral = true;
                this.endrun += this.iterator
                        .getNumberOfLiteralWords();
            } else {
                this.dead = true;
            }

        } else {
            this.isLiteral = true;
            this.endrun += this.iterator.getNumberOfLiteralWords();
        }

    }

    /**
     * @return true if there is no more data
     */
    public boolean hasNoData() {
        return this.dead;
    }

    /**
     * @param f call the function with the current information
     */
    public void callbackUpdate(final UpdateableBitmapFunction32 f) {
        if (this.dead)
            f.setZero(this.pos);
        else if (this.isLiteral)
            f.setLiteral(this.pos);
        else if (this.value)
            f.setOne(this.pos);
        else
            f.setZero(this.pos);
    }

    @Override
    public int compareTo(EWAHPointer32 other) {
        return this.endrun - other.endrun;
    }
}
