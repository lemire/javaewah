package javaewah;

/*
* Copyright 2009-2011, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/

public final class BufferedRunningLengthWord {

    public BufferedRunningLengthWord(final RunningLengthWord rlw) {
    	this(rlw.array[rlw.position]);
    }
    public BufferedRunningLengthWord(final long a) {
    	this.NumberOfLiteralWords = (int) (a >>> (1+runninglengthbits));
        this.RunningBit = (a & 1) != 0;
        this.RunningLength = (int) ((a >>> 1) & largestrunninglengthcount) ;
    }
    public void reset(final RunningLengthWord rlw) {
    	reset(rlw.array[rlw.position]);
    }   
    public void reset(final long a) {
    	this.NumberOfLiteralWords = (int) (a >>> (1+runninglengthbits));
        this.RunningBit = (a & 1) != 0;
        this.RunningLength = (int) ((a >>> 1) & largestrunninglengthcount) ; 
        this.dirtywordoffset = 0;
    }
    
    public int getNumberOfLiteralWords() {
        return  this.NumberOfLiteralWords;
    }
    public void setNumberOfLiteralWords(final int number) {
    	this.NumberOfLiteralWords = number;
    }
    public void setRunningBit(final boolean b) {
    	this.RunningBit = b;
    }
    public boolean getRunningBit() {
    	return this.RunningBit;
    }
    public long getRunningLength() {
    	return this.RunningLength;
    }
    public void setRunningLength(final long number) {
    	this.RunningLength = number;
    }

    public long  size() {
        return this.RunningLength + this.NumberOfLiteralWords;
    }

    @Override
	public String toString() {
        return "running bit = "+getRunningBit() +" running length = "+getRunningLength() + " number of lit. words "+ getNumberOfLiteralWords();
    }

    public void discardFirstWords(long x) {
        if(this.RunningLength >= x) {
        	this.RunningLength -= x;
            return;
        }
        x -= this.RunningLength;
        this.RunningLength = 0;
        this.dirtywordoffset += x;
        this.NumberOfLiteralWords -= x;
    }

	public int NumberOfLiteralWords;
    public boolean RunningBit;
    public long RunningLength;
    public int dirtywordoffset= 0;
    
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