package com.googlecode.javaewah;

import java.util.Iterator;

public class IteratorUtil {
	
	public static IntIterator toSetBitsIntIterator(final IteratingRLW i) {
		return new IntIteratorOverIteratingRLW(i);
	}

	public static Iterator<Integer> toSetBitsIterator(final IteratingRLW i) {
		return new Iterator<Integer>() {
			public boolean hasNext() {
				return this.under.hasNext();
			}

			public Integer next() {
				return new Integer(this.under.next());
			}

			public void remove() {
			}

			final private IntIterator under = toSetBitsIntIterator(i);
		};

	}

	public static void materialize(IteratingRLW i, BitmapStorage c) {
		while (true) {
			if (i.getRunningLength() > 0) {
				c.addStreamOfEmptyWords(i.getRunningBit(), i.getRunningLength());
			}
			for (int k = 0; k < i.getNumberOfLiteralWords(); ++k)
				c.add(i.getLiteralWordAt(k));
			if (!i.next())
				break;
		}
	}

	public static int cardinality(IteratingRLW i) {
		int answer = 0;
		while (true) {
			if(i.getRunningBit()) answer += i.getRunningLength() * EWAHCompressedBitmap.wordinbits;
			for (int k = 0; k < i.getNumberOfLiteralWords(); ++k)
				answer += Long.bitCount(i.getLiteralWordAt(k));
			if(!i.next()) break;
		}
		return answer;
	}
	
	public static IteratingRLW[] toIterators(EWAHCompressedBitmap... x) {
		IteratingRLW[] X = new IteratingRLW[x.length];
		for (int k = 0; k < X.length; ++k) {
			X[k] = new IteratingBufferedRunningLengthWord(x[k]);
		}
		return X;
	}
	public static long materialize(IteratingRLW i, BitmapStorage c, long Max) {
		final long origMax = Max;
		while (true) {
			if (i.getRunningLength() > 0) {
				long L = i.getRunningLength();
				if(L > Max) L = Max;
				c.addStreamOfEmptyWords(i.getRunningBit(), L);
				Max -= L;
			}
			long L = i.getNumberOfLiteralWords();
			for (int k = 0; k < L; ++k)
				c.add(i.getLiteralWordAt(k));
			if(Max>0)  {
				if (!i.next())
					break;
			}
				else break;
		}
		return origMax - Max;
	}
	public static EWAHCompressedBitmap materialize(IteratingRLW i) {
		EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
		materialize(i, ewah);
		return ewah;
	}

}
