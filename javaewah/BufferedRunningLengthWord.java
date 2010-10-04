package javaewah;

/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among others.
*/

public class BufferedRunningLengthWord {

    public BufferedRunningLengthWord(RunningLengthWord rlw) {
        val = rlw.array[rlw.position];
    }
    public BufferedRunningLengthWord(long a) {
        val = a;
    }
    public long getNumberOfLiteralWords() {
        return  val >>> (1+runninglengthbits);
    }
    public void setNumberOfLiteralWords(long number) {
        val |= notrunninglengthplusrunningbit;
        val &= (number << (runninglengthbits +1) ) |runninglengthplusrunningbit;
    }
    public void setRunningBit(boolean b) {
        if(b) val |= 1l;
        else val &= ~1l;
    }
    public boolean getRunningBit() {
        return (val & 1) != 0;
    }
    public long getRunningLength() {
        return (val >>> 1) & largestrunninglengthcount ;
    }
    public void setRunningLength(long number) {
        val |= shiftedlargestrunninglengthcount;
        val &= (number << 1) | notshiftedlargestrunninglengthcount;
    }

    public long  size() {
        return getRunningLength() + getNumberOfLiteralWords();
    }

    public String toString() {
        return "running bit = "+getRunningBit() +" running length = "+getRunningLength() + " number of lit. words "+ getNumberOfLiteralWords();
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
        dirtywordoffset += x; 
        setNumberOfLiteralWords(old - x);
        assert old-x == getNumberOfLiteralWords();
    }

    public long val;
    int dirtywordoffset= 0;
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