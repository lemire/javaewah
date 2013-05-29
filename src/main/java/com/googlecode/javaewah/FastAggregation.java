package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Fast algorithms to aggregate many bitmaps. These algorithms are just given as
 * reference. They may not be faster than the corresponding methods in the
 * EWAHCompressedBitmap class.
 * 
 * @author Daniel Lemire
 * 
 */
public class FastAggregation {

	public static void inplaceor(long[] bitmap, EWAHIterator i) {
		int pos = 0;
		while (i.hasNext()) {
			RunningLengthWord localrlw = i.next();
			int L = (int) localrlw.getRunningLength();
			if (localrlw.getRunningBit()) {
				for (; L > 0; --L) {
					bitmap[pos++] = ~0l;
				}
			} else
				pos += L;
			int LR = localrlw.getNumberOfLiteralWords();
			for (int k = 0; k < LR; ++k)
				bitmap[pos++] |= i.buffer()[i.literalWords() + k];

		}

	}

	public static int inplaceor(long[] bitmap,
			IteratingRLW i) {
		int pos = 0;
		long s;
		while ((s = i.size()) > 0) {
			if (pos + s < bitmap.length) {
				final int L = (int) i.getRunningLength();
				if (i.getRunningBit())
					java.util.Arrays.fill(bitmap, pos, pos + L, ~0l);
				pos += L;
				final int LR = i.getNumberOfLiteralWords();
				for (int k = 0; k < LR; ++k)
					bitmap[pos++] |= i.getLiteralWordAt(k);
				if (!i.next()) {
					return pos;
				}
			} else {
				int howmany = bitmap.length - pos;
				int L = (int) i.getRunningLength();
				if (pos + L > bitmap.length) {
					if (i.getRunningBit()) {
						java.util.Arrays.fill(bitmap, pos, howmany, ~0l);
					}
					i.discardFirstWords(howmany);
					return bitmap.length;
				}
				if (i.getRunningBit())
					java.util.Arrays.fill(bitmap, pos, pos + L, ~0l);
				pos += L;
				for (int k = 0; pos < bitmap.length; ++k)
					bitmap[pos++] |= i.getLiteralWordAt(k);
				i.discardFirstWords(howmany);
				return pos;
			}
		}
		return pos;
	}

	public static EWAHCompressedBitmap experimentalor(
			final EWAHCompressedBitmap... bitmaps) {
		EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
		experimentalorWithContainer(answer, bitmaps);
		return answer;
	}

	public static void experimentalorWithContainer(
			final BitmapStorage container,
			final EWAHCompressedBitmap... bitmaps) {
		int range = 0;
		for (EWAHCompressedBitmap bitmap : bitmaps) {
			if (bitmap.sizeinbits > range)
				range = bitmap.sizeinbits;
		}

		long[] hardbitmap = new long[(range + 63) / 64];
		for (EWAHCompressedBitmap bitmap : bitmaps) {
			inplaceor(hardbitmap, bitmap.getEWAHIterator());
		}
		for (int k = 0; k < hardbitmap.length; ++k)
			container.add(hardbitmap[k]);
		container.setSizeInBits(range);
	}

	public static EWAHCompressedBitmap smartor(
			final EWAHCompressedBitmap... bitmaps) {
		if (bitmaps.length == 0)
			return new EWAHCompressedBitmap();
		if (bitmaps.length == 1)
			return bitmaps[0];
		int size = 0;
		int sinbits = 0;
		for (EWAHCompressedBitmap b : bitmaps) {
			size += b.sizeInBytes();
			if (sinbits < b.sizeInBits())
				sinbits = b.sizeInBits();
		}
		EWAHCompressedBitmap answer;
		if (size * 8 > sinbits) {
			answer = new EWAHCompressedBitmap();
			bufferedorWithContainer(answer, bitmaps);
		} else {
			answer = bitmaps[0];
			for (int k = 1; k < bitmaps.length; ++k)
				answer.or(bitmaps[0]);
		}
		return answer;
	}

	public static EWAHCompressedBitmap bufferedor(
			final EWAHCompressedBitmap... bitmaps) {
		EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
		bufferedorWithContainer(answer, bitmaps);
		return answer;
	}

	public static void bufferedorWithContainer(final BitmapStorage container,
			final EWAHCompressedBitmap... bitmaps) {
		int range = 0;
		EWAHCompressedBitmap[] sbitmaps = bitmaps.clone();
		Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap>() {
			public int compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) {
				return b.sizeinbits - a.sizeinbits;
			}
		});

		java.util.ArrayList<IteratingBufferedRunningLengthWord> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord>();
		for (EWAHCompressedBitmap bitmap : sbitmaps) {
			if (bitmap.sizeinbits > range)
				range = bitmap.sizeinbits;
			al.add(new IteratingBufferedRunningLengthWord(bitmap));
		}
		final int MAXBUFSIZE = 65536;
		long[] hardbitmap = new long[MAXBUFSIZE];
		int maxr = al.size();
		while (maxr > 0) {
			long effective = 0;
			for (int k = 0; k < maxr; ++k) {
				if (al.get(k).size() > 0) {
					int eff = inplaceor(hardbitmap, al.get(k));
					if (eff > effective)
						effective = eff;
				} else
					maxr = k;
			}
			for (int k = 0; k < effective; ++k)
				container.add(hardbitmap[k]);
			Arrays.fill(hardbitmap, 0);

		}
		container.setSizeInBits(range);
	}

	public static IteratingRLW bufferedor(IteratingRLW... al) {
		final int MAXBUFSIZE = 65536;
		final long[] hardbitmap = new long[MAXBUFSIZE];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for(IteratingRLW i : al) ll.add(i);
		
		
		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();
				long effective = 0;
				Iterator<IteratingRLW> i = ll.iterator();
				while(i.hasNext()) {
					IteratingRLW rlw = i.next();
					if (rlw.size() > 0) {
						int eff = inplaceor(hardbitmap, rlw);
						if (eff > effective)
							effective = eff;
					} else
						i.remove();
				}
				for (int k = 0; k < effective; ++k)
					buffer.add(hardbitmap[k]);
				Arrays.fill(hardbitmap, 0);
				return buffer.getEWAHIterator();
			}

			@Override
			public void remove() {
				throw new RuntimeException("unsupported");
				
			}
			
		};
		return new BufferedIterator(i);
	}
	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the and aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T and(T... bitmaps) {
		// for "and" a priority queue is not needed, but
		// overhead ought to be small
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {
					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps)
			pq.add(x);
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.and(x2));
		}
		return pq.poll();
	}

	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the or aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T or(T... bitmaps) {
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {
					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps) {
			pq.add(x);
		}
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.or(x2));
		}
		return pq.poll();
	}

	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the xor aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T xor(T... bitmaps) {
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {

					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps)
			pq.add(x);
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.xor(x2));
		}
		return pq.poll();
	}
}
