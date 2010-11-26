package javaewah;

/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/

public class EWAHIterator {
    RunningLengthWord rlw;
    int size;
    int pointer;

    public EWAHIterator(final long[] a, final int sizeinwords) {
        this.rlw = new RunningLengthWord(a,0);
        this.size = sizeinwords;
        this.pointer = 0;
    }

    boolean hasNext() {
        return this.pointer<this.size;
    }

    RunningLengthWord next() {
        this.rlw.position = this.pointer;
        this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        return this.rlw;
    }

    int dirtyWords()  {
        return this.pointer-(int)this.rlw.getNumberOfLiteralWords();
    }

    long[] buffer() {
        return this.rlw.array;
    }

}
