package javaewah;

/*
* Copyright 2009-2011, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/

/**
 * Mostly for internal use.
 */
public final class RunningLengthWord {

    RunningLengthWord(final long[] a, final int p) {
        this.array = a;
        this.position = p;
    }
    public int getNumberOfLiteralWords() {
        return  (int) (this.array[this.position] >>> (1+runninglengthbits));
    }
    public void setNumberOfLiteralWords(final long number) {
        this.array[this.position] |= notrunninglengthplusrunningbit;
        this.array[this.position] &= (number << (runninglengthbits +1) ) |runninglengthplusrunningbit;
    }
    public void setRunningBit(final boolean b) {
        if(b) this.array[this.position] |= 1l;
        else this.array[this.position] &= ~1l;
    }
    public boolean getRunningBit() {
        return (this.array[this.position] & 1) != 0;
    }
    public long getRunningLength() {
        return (this.array[this.position] >>> 1) & largestrunninglengthcount ;
    }
    public void setRunningLength(final long number) {
        this.array[this.position] |= shiftedlargestrunninglengthcount;
        this.array[this.position] &= (number << 1) | notshiftedlargestrunninglengthcount;
    }

    public long  size() {
        return getRunningLength() + getNumberOfLiteralWords();
    }

    @Override
	public String toString() {
        return "running bit = "+getRunningBit() +" running length = "+
               getRunningLength() + " number of lit. words "+ getNumberOfLiteralWords();
    }

    /*public void discardFirstWords(long x) {
        long rl = getRunningLength() ;
        if(rl >= x) {
            setRunningLength(rl - x);
            assert getRunningLength() == rl-x;
            return;
        }
        x -= rl;
        setRunningLength(0);
        assert getRunningLength() == 0;
        final long old = getNumberOfLiteralWords() ;
        assert old >= x;
        setNumberOfLiteralWords(old - x);
        assert old-x == getNumberOfLiteralWords();
    }*/

    public long[] array;
    public int position;
    public static final int runninglengthbits = 32;
    public static final int literalbits = 64 - 1 - runninglengthbits;
    public static final long largestliteralcount = (1l<<literalbits) - 1;
    public static final long largestrunninglengthcount = (1l<<runninglengthbits)-1;
    public static final long shiftedlargestrunninglengthcount = largestrunninglengthcount<<1;
    public static final long notshiftedlargestrunninglengthcount = ~shiftedlargestrunninglengthcount;
    public static final long runninglengthplusrunningbit = (1l<<(runninglengthbits+1)) - 1;
    public static final long notrunninglengthplusrunningbit =~runninglengthplusrunningbit;
    public static final long notlargestrunninglengthcount =~largestrunninglengthcount;
}