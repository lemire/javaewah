package javaewah;

/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among others.
*/

public class RunningLengthWord {

    RunningLengthWord(long[] a, int p) {
        array = a;
        position = p;
    }
    public long getNumberOfLiteralWords() {
        return  array[position] >>> (1+runninglengthbits);
    }
    public void setNumberOfLiteralWords(long number) {
        array[position] |= notrunninglengthplusrunningbit;
        array[position] &= (number << (runninglengthbits +1) ) |runninglengthplusrunningbit;
    }
    public void setRunningBit(boolean b) {
        if(b) array[position] |= 1l;
        else array[position] &= ~1l;
    }
    public boolean getRunningBit() {
        return (array[position] & 1) != 0;
    }
    public long getRunningLength() {
        return (array[position] >>> 1) & largestrunninglengthcount ;
    }
    public void setRunningLength(long number) {
        array[position] |= shiftedlargestrunninglengthcount;
        array[position] &= (number << 1) | notshiftedlargestrunninglengthcount;
    }

    public long  size() {
        return getRunningLength() + getNumberOfLiteralWords();
    }

    public String toString() {
        return "running bit = "+getRunningBit() +" running length = "+
               getRunningLength() + " number of lit. words "+ getNumberOfLiteralWords();
    }

    public void discardFirstWords(long x) {
        long rl = getRunningLength() ;
        if(rl >= x) {
            setRunningLength(rl - x);
            assert getRunningLength() == rl-x;
            return;
        }
        x -= rl;
        setRunningLength(0);
        assert getRunningLength() == 0;
        long old = getNumberOfLiteralWords() ;
        assert old >= x;
        setNumberOfLiteralWords(old - x);
        assert old-x == getNumberOfLiteralWords();
    }

    public long[] array;
    public int position;
    public static int runninglengthbits = 32;
    public static int literalbits = 64 - 1 - runninglengthbits;
    public static long largestliteralcount = (1l<<literalbits) - 1;
    public static long largestrunninglengthcount = (1l<<runninglengthbits)-1;
    public static long shiftedlargestrunninglengthcount = largestrunninglengthcount<<1;
    public static long notshiftedlargestrunninglengthcount = ~shiftedlargestrunninglengthcount;
    public static long runninglengthplusrunningbit = (1l<<(runninglengthbits+1)) - 1;
    public static long notrunninglengthplusrunningbit =~runninglengthplusrunningbit;
    public static long notlargestrunninglengthcount =~largestrunninglengthcount;
}