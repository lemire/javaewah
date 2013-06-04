package com.googlecode.javaewah32;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * @author lemire
 *
 */
public class IteratorAggregation32 {
	/**
	 * @param x iterator to negate
	 * @return negated version of the iterator
	 */
	public static IteratingRLW32 not(final IteratingRLW32 x) {
		return new IteratingRLW32() {

			@Override
			public boolean next() {
				return x.next();
			}

			@Override
			public int getLiteralWordAt(int index) {
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
			public int size() {
				return x.size();
			}

			@Override
			public int getRunningLength() {
				return x.getRunningLength();
			}

			@Override
			public void discardFirstWords(int y) {
				x.discardFirstWords(y);
			}
			
		};
	}

	
	/**
	 * @param al iterators to aggregate
	 * @return and aggregate
	 */
	public static IteratingRLW32 and(final IteratingRLW32... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW32> ll = new LinkedList<IteratingRLW32>();
		for (IteratingRLW32 i : al) 
			ll.add(i);
		final int MAXBUFSIZE = 65536 * al.length ;//512KB per bitmap

		Iterator<EWAHIterator32> i = new Iterator<EWAHIterator32>() {
			EWAHCompressedBitmap32 buffer = new EWAHCompressedBitmap32();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator32 next() {
				buffer.clear();
				IteratorAggregation32.andToContainer(buffer, MAXBUFSIZE,
						ll.get(0), ll.get(1));
				if (ll.size() > 2) {
					Iterator<IteratingRLW32> i = ll.iterator();
					i.next();
					i.next();
					EWAHCompressedBitmap32 tmpbuffer = new EWAHCompressedBitmap32();
					while (i.hasNext()) {
						IteratorAggregation32.andToContainer(tmpbuffer,
								 buffer.getIteratingRLW(), i.next());
						buffer.swap(tmpbuffer);
						tmpbuffer.clear();
					}
				}
				Iterator<IteratingRLW32> i = ll.iterator();
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
		return new BufferedIterator32(i);
	}

	/**
	 * @param x1 first iterator to aggregate
	 * @param x2 second iterator to aggregate
	 * @return xor aggregate
	 */
	public static IteratingRLW32 xor(final IteratingRLW32 x1, final IteratingRLW32 x2) {

		final int MAXBUFSIZE = 65536;
		Iterator<EWAHIterator32> i = new Iterator<EWAHIterator32>() {
			EWAHCompressedBitmap32 buffer = new EWAHCompressedBitmap32();

			@Override
			public boolean hasNext() {
				return x1.size()>0 || x2.size()>0;
			}

			@Override
			public EWAHIterator32 next() {
				buffer.clear();
				IteratorAggregation32.xorToContainer(buffer, MAXBUFSIZE,
						x1, x2);
				return buffer.getEWAHIterator();
			}

			@Override
			public void remove() {
				throw new RuntimeException("unsupported");

			}
		};
		return new BufferedIterator32(i);

	}

	/**
	 * @param al iterators to aggregate
	 * @return or aggregate
	 */
	public static IteratingRLW32 or(final IteratingRLW32... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];

		final int MAXBUFSIZE = 65536;
		final int[] hardbitmap = new int[MAXBUFSIZE];
		final LinkedList<IteratingRLW32> ll = new LinkedList<IteratingRLW32>();
		for (IteratingRLW32 i : al)
			ll.add(i);

		Iterator<EWAHIterator32> i = new Iterator<EWAHIterator32>() {
			EWAHCompressedBitmap32 buffer = new EWAHCompressedBitmap32();

			@Override
			public boolean hasNext() {
				return !ll.isEmpty();
			}

			@Override
			public EWAHIterator32 next() {
				buffer.clear();
				int effective = 0;
				Iterator<IteratingRLW32> i = ll.iterator();
				while (i.hasNext()) {
					IteratingRLW32 rlw = i.next();
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
		return new BufferedIterator32(i);
	}
	
	/**
	 * Write out the content of the iterator, but as if it were all zeros.
	 * 
	 * @param container
	 *            where we write
	 * @param i
	 *            the iterator
	 */
	protected static void dischargeAsEmpty(final BitmapStorage32 container,
			final IteratingRLW32 i) {
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
	protected static int discharge(final BitmapStorage32 container, IteratingRLW32 i, int max) {
		int counter = 0;
		while (i.size() > 0 && counter < max) {
			int L1 = i.getRunningLength();
			if (L1 > 0) {
				if (L1 + counter > max)
					L1 = max - counter;
				container.addStreamOfEmptyWords(i.getRunningBit(), L1);
				counter += L1;
			}
			int L = i.getNumberOfLiteralWords();
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
	protected static int dischargeNegated(final BitmapStorage32 container, IteratingRLW32 i, int max) {
		int counter = 0;
		while (i.size() > 0 && counter < max) {
			int L1 = i.getRunningLength();
			if (L1 > 0) {
				if (L1 + counter > max)
					L1 = max - counter;
				container.addStreamOfEmptyWords(i.getRunningBit(), L1);
				counter += L1;
			}
			int L = i.getNumberOfLiteralWords();
			if(L + counter > max) L = max - counter;	
			for (int k = 0; k < L; ++k) {
				container.add(i.getLiteralWordAt(k));
			}
			counter += L;
			i.discardFirstWords(L+L1);
		}
		return counter;
	}
	
	private static void andToContainer(final BitmapStorage32 container,
			int desiredrlwcount, final IteratingRLW32 rlwi, IteratingRLW32 rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) && (desiredrlwcount-- >0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		          .getRunningLength();
		        final IteratingRLW32 prey = i_is_prey ? rlwi : rlwj;
		        final IteratingRLW32 predator = i_is_prey ? rlwj
		          : rlwi;
		        if (predator.getRunningBit() == false) {
		          container.addStreamOfEmptyWords(false, predator.getRunningLength());
		          prey.discardFirstWords(predator.getRunningLength());
		          predator.discardFirstWords(predator.getRunningLength());
		        } else {
		          final int index = discharge(container, prey, predator.getRunningLength()); 
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

	private static void andToContainer(final BitmapStorage32 container,
			 final IteratingRLW32 rlwi, IteratingRLW32 rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		          .getRunningLength();
		        final IteratingRLW32 prey = i_is_prey ? rlwi : rlwj;
		        final IteratingRLW32 predator = i_is_prey ? rlwj
		          : rlwi;
		        if (predator.getRunningBit() == false) {
		          container.addStreamOfEmptyWords(false, predator.getRunningLength());
		          prey.discardFirstWords(predator.getRunningLength());
		          predator.discardFirstWords(predator.getRunningLength());
		        } else {
		          final int index = discharge(container, prey, predator.getRunningLength()); 
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


	private static void xorToContainer(final BitmapStorage32 container,
			int desiredrlwcount, final IteratingRLW32 rlwi, IteratingRLW32 rlwj) {
		while ((rlwi.size()>0) && (rlwj.size()>0) && (desiredrlwcount-- >0) ) {
		      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
		          final boolean i_is_prey = rlwi.getRunningLength() < rlwj
		            .getRunningLength();
		          final IteratingRLW32 prey = i_is_prey ? rlwi : rlwj;
		          final IteratingRLW32 predator = i_is_prey ? rlwj
		            : rlwi;
		          if (predator.getRunningBit() == false) {
		            int index = discharge(container, prey, predator.getRunningLength()); 
		            container.addStreamOfEmptyWords(false, predator.getRunningLength()
		              - index);
		            predator.discardFirstWords(predator.getRunningLength());
		          } else {
		            int index = dischargeNegated(container, prey, predator.getRunningLength()); 
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

	protected static int inplaceor(int[] bitmap,
			IteratingRLW32 i) {
		int pos = 0;
		int s;
		while ((s = i.size()) > 0) {
			if (pos + s < bitmap.length) {
				final int L = (int) i.getRunningLength();
				if (i.getRunningBit())
					java.util.Arrays.fill(bitmap, pos, pos + L, ~0);
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
                      java.util.Arrays.fill(bitmap, pos, bitmap.length, ~0); 
					}
					i.discardFirstWords(howmany);
					return bitmap.length;
				}
				if (i.getRunningBit())
					java.util.Arrays.fill(bitmap, pos, pos + L, ~0);
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
