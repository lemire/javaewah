package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author lemire
 *
 */
public class IteratorAggregation {
	/**
	 * @param x iterator to negate
	 * @return negated version of the iterator
	 */
	public static IteratingRLW not(final IteratingRLW x) {
		return new IteratingRLW() {

			@Override
			public boolean next() {
				return x.next();
			}

			@Override
			public long getLiteralWordAt(int index) {
				return ~x.getLiteralWordAt(index);
			}

			@Override
			public int getNumberOfLiteralWords() {
				return x.getNumberOfLiteralWords();
			}

			@Override
			public boolean getRunningBit() {
				return ! x.getRunningBit();
			}

			@Override
			public long size() {
				return x.size();
			}

			@Override
			public long getRunningLength() {
				return x.getRunningLength();
			}

			@Override
			public void discardFirstWords(long y) {
				x.discardFirstWords(y);
			}
			
		};
	}

	
	/**
	 * @param al set of iterators to aggregate
	 * @return and aggregate
	 */
	public static IteratingRLW and(final IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al) 
			ll.add(i);
		final int MAXBUFSIZE = 65536 * al.length;//512KB per bitmap

		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();
				IteratorAggregation.andToContainer(buffer, MAXBUFSIZE,
						ll.get(0), ll.get(1));
				if (ll.size() > 2) {
					Iterator<IteratingRLW> i = ll.iterator();
					i.next();
					i.next();
					EWAHCompressedBitmap tmpbuffer = new EWAHCompressedBitmap();
					while (i.hasNext() && buffer.sizeInBytes() > 0) {
						IteratorAggregation.andToContainer(tmpbuffer,
								 buffer.getIteratingRLW(), i.next());
						buffer.swap(tmpbuffer);
						tmpbuffer.clear();
					}
				}
				Iterator<IteratingRLW> i = ll.iterator();
				while(i.hasNext()) {
					if(i.next().size() == 0) {
						ll.clear();
						break;
					}
				}
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
	 * @param x1 first iterator to aggregate
	 * @param x2 second iterator to aggregate
	 * @return xor aggregate
	 */
	public static IteratingRLW xor(final IteratingRLW x1, final IteratingRLW x2) {

		final int MAXBUFSIZE = 65536;
		Iterator<EWAHIterator> i = new Iterator<EWAHIterator>() {
			EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();

			@Override
			public boolean hasNext() {
				return x1.size()>0 || x2.size()>0;
			}

			@Override
			public EWAHIterator next() {
				buffer.clear();
				IteratorAggregation.xorToContainer(buffer, MAXBUFSIZE,
						x1, x2);
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
	 * @param al iterators to aggregate
	 * @return or aggregate
	 */
	public static IteratingRLW or(final IteratingRLW... al) {
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

	/**
	 * Write out the content of the iterator, but as if it were all zeros.
	 * 
	 * @param container
	 *            where we write
	 * @param i
	 *            the iterator
	 */
	protected static void dischargeAsEmpty(final BitmapStorage container,
			final IteratingRLW i) {
		while (i.size() > 0) {
			container.addStreamOfEmptyWords(false, i.size());
			i.next();

		}
	}
	
	/**
	   * Write out up to max words, returns how many were written
	   * @param container target for writes
	   * @param i source of data
	   * @param max maximal number of writes
	   * @return how many written
	   */

	protected static long discharge(final BitmapStorage container, IteratingRLW i, long max) {
		long counter = 0;
		while (i.size() > 0 && counter < max) {
			long L1 = i.getRunningLength();
			if (L1 > 0) {
				if (L1 + counter > max)
					L1 = max - counter;
				container.addStreamOfEmptyWords(i.getRunningBit(), L1);
				counter += L1;
			}
			long L = i.getNumberOfLiteralWords();
			if(L + counter > max) L = max - counter;	
			for (int k = 0; k < L; ++k) {
				container.add(i.getLiteralWordAt(k));
			}
			counter += L;
			i.discardFirstWords(L+L1);
		}
		return counter;
	}
	

	/**
	   * Write out up to max negated words, returns how many were written
	   * @param container target for writes
	   * @param i source of data
	   * @param max maximal number of writes
	   * @return how many written
	   */
	protected static long dischargeNegated(final BitmapStorage container, IteratingRLW i, long max) {
		long counter = 0;
		while (i.size() > 0 && counter < max) {
			long L1 = i.getRunningLength();
			if (L1 > 0) {
				if (L1 + counter > max)
					L1 = max - counter;
				container.addStreamOfEmptyWords(!i.getRunningBit(), L1);
				counter += L1;
			}
			long L = i.getNumberOfLiteralWords();
			if(L + counter > max) L = max - counter;	
			for (int k = 0; k < L; ++k) {
				container.add(~i.getLiteralWordAt(k));
			}
			counter += L;
			i.discardFirstWords(L+L1);
		}
		return counter;
	}
	
	private static void andToContainer(final BitmapStorage container,
			int desiredrlwcount, final IteratingRLW rlwi, IteratingRLW rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) && (desiredrlwcount-- >0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		          .getRunningLength();
		        final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
		        final IteratingRLW predator = i_is_prey ? rlwj
		          : rlwi;
		        if (predator.getRunningBit() == false) {
		          container.addStreamOfEmptyWords(false, predator.getRunningLength());
		          prey.discardFirstWords(predator.getRunningLength());
		          predator.discardFirstWords(predator.getRunningLength());
		        } else {
		          final long index = discharge(container, prey, predator.getRunningLength()); 
		          container.addStreamOfEmptyWords(false, predator.getRunningLength()
		            - index);
		          predator.discardFirstWords(predator.getRunningLength());
		        }
		      }
		      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
		        rlwj.getNumberOfLiteralWords());
		      if (nbre_literal > 0) {
				desiredrlwcount -= nbre_literal;
		        for (int k = 0; k < nbre_literal; ++k)
		          container.add(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
		        rlwi.discardFirstWords(nbre_literal);
		        rlwj.discardFirstWords(nbre_literal);
		      }
		    }      
	}

	private static void andToContainer(final BitmapStorage container,
			 final IteratingRLW rlwi, IteratingRLW rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		          .getRunningLength();
		        final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
		        final IteratingRLW predator = i_is_prey ? rlwj
		          : rlwi;
		        if (predator.getRunningBit() == false) {
		          container.addStreamOfEmptyWords(false, predator.getRunningLength());
		          prey.discardFirstWords(predator.getRunningLength());
		          predator.discardFirstWords(predator.getRunningLength());
		        } else {
		          final long index = discharge(container, prey, predator.getRunningLength()); 
		          container.addStreamOfEmptyWords(false, predator.getRunningLength()
		            - index);
		          predator.discardFirstWords(predator.getRunningLength());
		        }
		      }
		      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
		        rlwj.getNumberOfLiteralWords());
		      if (nbre_literal > 0) {
		        for (int k = 0; k < nbre_literal; ++k)
		          container.add(rlwi.getLiteralWordAt(k) & rlwj.getLiteralWordAt(k));
		        rlwi.discardFirstWords(nbre_literal);
		        rlwj.discardFirstWords(nbre_literal);
		      }
		    }      
	}


	private static void xorToContainer(final BitmapStorage container,
			int desiredrlwcount, final IteratingRLW rlwi, IteratingRLW rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) && (desiredrlwcount-- >0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		          final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		            .getRunningLength();
		          final IteratingRLW prey = i_is_prey ? rlwi : rlwj;
		          final IteratingRLW predator = i_is_prey ? rlwj
		            : rlwi;
		          if (predator.getRunningBit() == false) {
		            long index = discharge(container, prey, predator.getRunningLength()); 
		            container.addStreamOfEmptyWords(false, predator.getRunningLength()
		              - index);
		            predator.discardFirstWords(predator.getRunningLength());
		          } else {
		            long index = dischargeNegated(container, prey, predator.getRunningLength()); 
		            container.addStreamOfEmptyWords(true, predator.getRunningLength()
		              - index);
		            predator.discardFirstWords(predator.getRunningLength());
		          }
		        }
		        final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
		          rlwj.getNumberOfLiteralWords());
		        if (nbre_literal > 0) {
		          desiredrlwcount -= nbre_literal;
		          for (int k = 0; k < nbre_literal; ++k)
		            container.add(rlwi.getLiteralWordAt(k) ^ rlwj.getLiteralWordAt(k));
		          rlwi.discardFirstWords(nbre_literal);
		          rlwj.discardFirstWords(nbre_literal);
		        }
		    }      
	}

	protected static int inplaceor(long[] bitmap,
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
                      java.util.Arrays.fill(bitmap, pos, bitmap.length, ~0l); 
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
