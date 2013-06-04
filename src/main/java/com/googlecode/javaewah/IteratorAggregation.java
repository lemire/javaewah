package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under APL 2.0.
 */

/**
 * Set of helper functions to aggregate bitmaps.
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

                           public IteratingRLW clone() throws CloneNotSupportedException {
                               throw new CloneNotSupportedException();
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
		final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al) 
			basell.add(i);
		return new BufferedIterator(new AndIt(basell));
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

		final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			basell.add(i);
		return new BufferedIterator(new ORIt(basell));
	}

	/**
	 * @param al iterators to aggregate
	 * @return xor aggregate
	 */
	public static IteratingRLW xor(final IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];


		final LinkedList<IteratingRLW> basell = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al)
			basell.add(i);

		return new BufferedIterator(new XORIt(basell));
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
	
	static void andToContainer(final BitmapStorage container,
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

	static void andToContainer(final BitmapStorage container,
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


	/**
	 * Compute the first few words of the XOR aggregate between two iterators. 
	 * 
	 * @param container where to write
	 * @param desiredrlwcount number of words to be written (max)
	 * @param rlwi first iterator to aggregate
	 * @param rlwj second iterator to aggregate
	 */
	public static void xorToContainer(final BitmapStorage container,
			int desiredrlwcount, final IteratingRLW rlwi, final IteratingRLW rlwj) {
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

	protected static int inplacexor(long[] bitmap,
			IteratingRLW i) {
		int pos = 0;
		long s;
		while ((s = i.size()) > 0) {
			if (pos + s < bitmap.length) {
				final int L = (int) i.getRunningLength();
				if (i.getRunningBit()) {
					for(int k = pos ; k < pos + L; ++k)
						bitmap[k] = ~bitmap[k];
				}
				pos += L;
				final int LR = i.getNumberOfLiteralWords();
				for (int k = 0; k < LR; ++k)
					bitmap[pos++] ^= i.getLiteralWordAt(k);
				if (!i.next()) {
					return pos;
				}
			} else {
				int howmany = bitmap.length - pos;
				int L = (int) i.getRunningLength();
				if (pos + L > bitmap.length) {
					if (i.getRunningBit()) {
						for(int k = pos ; k < bitmap.length; ++k)
							bitmap[k] = ~bitmap[k];
					}
					i.discardFirstWords(howmany);
					return bitmap.length;
				}
				if (i.getRunningBit())
					for(int k = pos ; k < pos + L; ++k)
						bitmap[k] = ~bitmap[k];
				pos += L;
				for (int k = 0; pos < bitmap.length; ++k)
					bitmap[pos++] ^= i.getLiteralWordAt(k);
				i.discardFirstWords(howmany);
				return pos;
			}
		}
		return pos;
	}
	protected static int inplaceand(long[] bitmap,
			IteratingRLW i) {
		int pos = 0;
		long s;
		while ((s = i.size()) > 0) {
			if (pos + s < bitmap.length) {
				final int L = (int) i.getRunningLength();
				if (!i.getRunningBit()) {
					for(int k = pos ; k < pos + L; ++k)
						bitmap[k] = 0;
				}
				pos += L;
				final int LR = i.getNumberOfLiteralWords();
				for (int k = 0; k < LR; ++k)
					bitmap[pos++] &= i.getLiteralWordAt(k);
				if (!i.next()) {
					return pos;
				}
			} else {
				int howmany = bitmap.length - pos;
				int L = (int) i.getRunningLength();
				if (pos + L > bitmap.length) {
					if (!i.getRunningBit()) {
						for(int k = pos ; k < bitmap.length; ++k)
							bitmap[k] = 0;
					}
					i.discardFirstWords(howmany);
					return bitmap.length;
				}
				if (!i.getRunningBit())
					for(int k = pos ; k < pos + L; ++k)
						bitmap[k] = 0;
				pos += L;
				for (int k = 0; pos < bitmap.length; ++k)
					bitmap[pos++] &= i.getLiteralWordAt(k);
				i.discardFirstWords(howmany);
				return pos;
			}
		}
		return pos;
	}

	/**
	 * An optimization option. Larger values may improve speed, but at
	 * the expense of memory.
	 */
	public final static int MAXBUFSIZE = 65536;
}
class ORIt implements CloneableIterator<EWAHIterator> {
	EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
	long[] hardbitmap = new long[IteratorAggregation.MAXBUFSIZE];
    LinkedList<IteratingRLW> ll;
	
	ORIt(LinkedList<IteratingRLW>  basell) {
		ll = basell;
	}
	
	@SuppressWarnings("unchecked")
	public XORIt clone() throws CloneNotSupportedException {
		XORIt answer = (XORIt) super.clone();
		answer.buffer = this.buffer.clone();
		answer.hardbitmap = this.hardbitmap.clone();
		answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
		return answer;
	}

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
				int eff = IteratorAggregation.inplaceor(hardbitmap, rlw);
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
};

class XORIt implements CloneableIterator<EWAHIterator> {
	EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
	long[] hardbitmap = new long[IteratorAggregation.MAXBUFSIZE];
    LinkedList<IteratingRLW> ll;
	
	XORIt(LinkedList<IteratingRLW>  basell) {
		ll = basell;
	}
	
	@SuppressWarnings("unchecked")
	public XORIt clone() throws CloneNotSupportedException {
		XORIt answer = (XORIt) super.clone();
		answer.buffer = this.buffer.clone();
		answer.hardbitmap = this.hardbitmap.clone();
		answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
		return answer;
	}

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
				int eff = IteratorAggregation.inplacexor(hardbitmap, rlw);
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
};


class AndIt implements CloneableIterator<EWAHIterator> {
	EWAHCompressedBitmap buffer = new EWAHCompressedBitmap();
	LinkedList<IteratingRLW> ll;
	
	public AndIt(LinkedList<IteratingRLW> basell) {
		ll = basell;
		
	}
	
	@Override
	public boolean hasNext() {
		return !ll.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	public AndIt clone() throws CloneNotSupportedException {
		AndIt answer = (AndIt) super.clone();
		answer.buffer = this.buffer.clone();
		answer.ll = (LinkedList<IteratingRLW>) this.ll.clone();
		return answer;
	}

	@Override
	public EWAHIterator next() {
		buffer.clear();
		IteratorAggregation.andToContainer(buffer, IteratorAggregation.MAXBUFSIZE * ll.size(),
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

};
