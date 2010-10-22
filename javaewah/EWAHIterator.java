package javaewah;

/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/

public class EWAHIterator {
    RunningLengthWord rlw;
    int size;
    int pointer;

    public EWAHIterator(long[] a, int sizeinwords) {
        rlw = new RunningLengthWord(a,0);
        size = sizeinwords;
        pointer = 0;
    }

    boolean hasNext() {
        return pointer<size;
    }

    RunningLengthWord next() {
        rlw.position = pointer;
        pointer += rlw.getNumberOfLiteralWords() + 1;
        return rlw;
    }

    int dirtyWords()  {
        return pointer-(int)rlw.getNumberOfLiteralWords();
    }

    long[] buffer() {
        return rlw.array;
    }

}
