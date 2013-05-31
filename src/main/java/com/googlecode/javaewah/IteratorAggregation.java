package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class IteratorAggregation {
/*
	public static IteratingRLW and(IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			ll.add(i);

		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();

			}

			@Override
			public void remove() {
				throw new RuntimeException("unsupported");

			}
		};
	}

	public static IteratingRLW xor(IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			ll.add(i);

		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();

			}

			@Override
			public void remove() {
				throw new RuntimeException("unsupported");

			}
		};
	}
*/
	
	public static IteratingRLW or(IteratingRLW... al) {
		final int MAXBUFSIZE = 65536;

		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			ll.add(i);

		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();
				orToContainer(buffer,MAXBUFSIZE, ll);
				return buffer.getEWAHIterator();
			}

			@Override
			public void remove() {
				throw new RuntimeException("unsupported");
			}
		};
		return new BufferedIterator(i);
	}

	public static IteratingRLW bufferedor(IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];

		final int MAXBUFSIZE = 65536;
		final long[] hardbitmap = new long[MAXBUFSIZE];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			ll.add(i);

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
				while (i.hasNext()) {
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

	public static void discharge(final BitmapStorage container, IteratingRLW i) {
		while (i.size() > 0) {
			if (i.getRunningLength() > 0) {
				container.addStreamOfEmptyWords(i.getRunningBit(),
						i.getRunningLength());
			}
			for (int k = 0; k < i.getNumberOfLiteralWords(); ++k) {
				container.add(i.getLiteralWordAt(k));
			}
		}

	}

	/**
	 * Will try to fill in the container with roughly desiredrlwcount
	 * running length words, though the exact count may vary.
	 * @param container
	 * @param desiredrlwcount
	 * @param bitmaps
	 */
	private static void orToContainer(final BitmapStorage container,
			int desiredrlwcount, final LinkedList<IteratingRLW> rlws) {
		// TODO: special case for when there is 1 or 2 bitmaps


		while (desiredrlwcount-- > 0) {
			long maxOneRl = 0;
			long minZeroRl = Long.MAX_VALUE;
			long minSize = Long.MAX_VALUE;
			int numEmptyRl = 0;

			Iterator<IteratingRLW> i = rlws.iterator();
			while (i.hasNext()) {
				IteratingRLW rlw = i.next();
				long size = rlw.size();
				if (size == 0) {
					i.remove();
					continue;
				}
				minSize = Math.min(minSize, size);

				if (rlw.getRunningBit()) {
					long rl = rlw.getRunningLength();
					maxOneRl = Math.max(maxOneRl, rl);
					minZeroRl = 0;
					if (rl == 0 && size > 0) {
						numEmptyRl++;
					}
				} else {
					long rl = rlw.getRunningLength();
					minZeroRl = Math.min(minZeroRl, rl);
					if (rl == 0 && size > 0) {
						numEmptyRl++;
					}
				}
			}

			if (rlws.size() == 0) {
				break;
			} else if (rlws.size() == 1) {
				// only one bitmap is left so just write the rest of it out
				discharge(container, rlws.getFirst());
				break;
			}

			if (maxOneRl > 0) {
				container.addStreamOfEmptyWords(true, maxOneRl);
				for (IteratingRLW x : rlws)
					x.discardFirstWords(maxOneRl);
			} else if (minZeroRl > 0) {
				container.addStreamOfEmptyWords(false, minZeroRl);
				for (IteratingRLW x : rlws)
					x.discardFirstWords(minZeroRl);
			} else {
				int index = 0;

				if (numEmptyRl == 1) {
					// if one rlw has literal words to process and the rest have
					// a run of
					// 0's we can write them out here
					IteratingRLW emptyRl = null;
					long minNonEmptyRl = Long.MAX_VALUE;
					for (IteratingRLW rlw : rlws) {
						long rl = rlw.getRunningLength();
						if (rl == 0) {
							emptyRl = rlw;
						} else {
							minNonEmptyRl = Math.min(minNonEmptyRl, rl);
						}
					}
					long wordsToWrite = minNonEmptyRl > minSize ? minSize
							: minNonEmptyRl;
					if (emptyRl != null) {
						for (int k = 0; k < wordsToWrite; ++k)
							container.add(emptyRl.getLiteralWordAt(k));
					}
					index += wordsToWrite;
				}

				while (index < minSize) {
					long word = 0;
					for (IteratingRLW rlw : rlws) {

						if (rlw.getRunningLength() <= index) {
							word |= rlw.getLiteralWordAt(index
									- (int) rlw.getRunningLength());
						}
					}
					container.add(word);
					index++;
				}
				for (IteratingRLW rlw : rlws) {

					rlw.discardFirstWords(minSize);
				}
			}
		}
	}

	static int inplaceor(long[] bitmap,
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


}
