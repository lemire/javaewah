package com.googlecode.javaewah32;

import java.util.Iterator;

import com.googlecode.javaewah.IntIterator;

/**
 * @author lemire
 *
 */
public class IteratorUtil32 {
	
	/**
	 * @param i
	 * @return an iterator over the set bits corresponding to the iterator
	 */
	public static IntIterator toSetBitsIntIterator(final IteratingRLW32 i) {
		return new IntIteratorOverIteratingRLW32(i);
	}

	/**
	 * @param i
	 * @return an iterator over the set bits corresponding to the iterator
	 */
	public static Iterator<Integer> toSetBitsIterator(final IteratingRLW32 i) {
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

	/**
	 * @param i
	 * @param c
	 */
	public static void materialize(final IteratingRLW32 i, final BitmapStorage32 c) {
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

	/**
	 * @param i
	 * @return the cardinality (number of set bits) corresponding to the iterator
	 */
	public static int cardinality(final IteratingRLW32 i) {
		int answer = 0;
		while (true) {
			if(i.getRunningBit()) answer += i.getRunningLength() * EWAHCompressedBitmap32.wordinbits;
			for (int k = 0; k < i.getNumberOfLiteralWords(); ++k)
				answer += Long.bitCount(i.getLiteralWordAt(k));
			if(!i.next()) break;
		}
		return answer;
	}
	
	/**
	 * @param x
	 * @return an array of iterators corresponding to the array of bitmaps
	 */
	public static IteratingRLW32[] toIterators(final EWAHCompressedBitmap32... x) {
		IteratingRLW32[] X = new IteratingRLW32[x.length];
		for (int k = 0; k < X.length; ++k) {
			X[k] = new IteratingBufferedRunningLengthWord32(x[k]);
		}
		return X;
	}
	/**
	 * @param i
	 * @param c
	 * @param Max
	 * @return how many words were actually materialized
	 */
	public static long materialize(final IteratingRLW32 i, final BitmapStorage32 c, int Max) {
		final int origMax = Max;
		while (true) {
			if (i.getRunningLength() > 0) {
				int L = i.getRunningLength();
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
	/**
	 * @param i
	 * @return materialized version of the iterator
	 */
	public static EWAHCompressedBitmap32 materialize(final IteratingRLW32 i) {
		EWAHCompressedBitmap32 ewah = new EWAHCompressedBitmap32();
		materialize(i, ewah);
		return ewah;
	}

}
