package javaewah;

/*
* Copyright 2009-2011, Daniel Lemire
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

	public boolean hasNext() {
        return this.pointer<this.size;
    }

    public RunningLengthWord next() {
        this.rlw.position = this.pointer;
        this.pointer += this.rlw.getNumberOfLiteralWords() + 1;
        return this.rlw;
    }

    public int dirtyWords()  {
        return this.pointer-(int)this.rlw.getNumberOfLiteralWords();
    }

    public long[] buffer() {
        return this.rlw.array;
    }

}
