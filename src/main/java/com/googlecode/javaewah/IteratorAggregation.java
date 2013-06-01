package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class IteratorAggregation {
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

	
	public static IteratingRLW and(IteratingRLW... al) {
		if (al.length == 0)
			throw new IllegalArgumentException("Need at least one iterator");
		if (al.length == 1)
			return al[0];
		final LinkedList<IteratingRLW> ll = new LinkedList<IteratingRLW>();
		for (IteratingRLW i : al) 
			ll.add(i);
		final int MAXBUFSIZE = 65536;

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
					while (i.hasNext()) {
						EWAHCompressedBitmap tmpbuffer = new EWAHCompressedBitmap();
						IteratorAggregation.andToContainer(tmpbuffer,
								MAXBUFSIZE, buffer.getIteratingRLW(), i.next());
						buffer = tmpbuffer;
						
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
				if(ll.size() == 1) {
					IteratorUtil.materialize(ll.get(0),buffer,MAXBUFSIZE);
					if(ll.get(0).size() == 0) ll.clear();
				} else if(ll.size() == 2) {
					orToContainer(buffer,MAXBUFSIZE, ll.get(0), ll.get(1));
					if(ll.get(1).size()==0) ll.remove(1);
					if(ll.get(0).size()==0) ll.remove(0);
				} else  orToContainer(buffer,MAXBUFSIZE, ll);
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

	public static void dischargeNegated(final BitmapStorage container, IteratingRLW i) {
		while (i.size() > 0) {
			if (i.getRunningLength() > 0) {
				container.addStreamOfEmptyWords(!(i.getRunningBit()),
						i.getRunningLength());
			}
			for (int k = 0; k < i.getNumberOfLiteralWords(); ++k) {
				container.add(~i.getLiteralWordAt(k));
			}
		}
	}
	/**
	   * Write out up to max words, returns how many were written
	   * @param container target for writes
	   * @param i source of data
	   * @param max maximal number of writes
	   * @return how many written
	   */

	public static int discharge(final BitmapStorage container, IteratingRLW i, long max) {
		int counter = 0;
		while (i.size() > 0 && counter < max) {
			if (i.getRunningLength() > 0) {
				long L = i.getRunningLength();
				if(L + counter > max) L = max - counter;
				container.addStreamOfEmptyWords(i.getRunningBit(),
						L);
				counter += L;
			}
			long L = i.getNumberOfLiteralWords();
			if(L + counter > max) L = max - counter;	
			for (int k = 0; k < L; ++k) {
				container.add(i.getLiteralWordAt(k));
			}
			counter += L;
		}
		return counter;
	}
	public static int dischargeNegated(final BitmapStorage container, IteratingRLW i, long max) {
		int counter = 0;
		while (i.size() > 0 && counter < max) {
			if (i.getRunningLength() > 0) {
				long L = i.getRunningLength();
				if(L + counter > max) L = max - counter;
				container.addStreamOfEmptyWords(!i.getRunningBit(),
						L);
				counter += L;
			}
			long L = i.getNumberOfLiteralWords();
			if(L + counter > max) L = max - counter;	
			for (int k = 0; k < L; ++k) {
				container.add(~i.getLiteralWordAt(k));
			}
			counter += L;
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
		          for (int k = 0; k < nbre_literal; ++k)
		            container.add(rlwi.getLiteralWordAt(k) ^ rlwj.getLiteralWordAt(k));
		          rlwi.discardFirstWords(nbre_literal);
		          rlwj.discardFirstWords(nbre_literal);
		        }
		    }      
	}
	private static void orToContainer(final BitmapStorage container,
			int desiredrlwcount, final IteratingRLW rlwi, IteratingRLW rlwj) {
			    while ((rlwi.size()>0) && (rlwj.size()>0)) {
			      while ((rlwi.getRunningLength() > 0) || (rlwj.getRunningLength() > 0)) {
			        final boolean i_is_prey = rlwi.getRunningLength() < rlwj
			          .getRunningLength();
			        final IteratingRLW prey = i_is_prey ? rlwi
			          : rlwj;
			        final IteratingRLW predator = i_is_prey ? rlwj
			          : rlwi;
			        if (predator.getRunningBit() == true) {
			          container.addStreamOfEmptyWords(true, predator.getRunningLength());
			          prey.discardFirstWords(predator.getRunningLength());
			          predator.discardFirstWords(predator.getRunningLength());
			        } else {
			          long index = discharge(container,prey, predator.getRunningLength());
			          container.addStreamOfEmptyWords(false, predator.getRunningLength()
			            - index);
			          predator.discardFirstWords(predator.getRunningLength());
			        }
			      }
			      final int nbre_literal = Math.min(rlwi.getNumberOfLiteralWords(),
			        rlwj.getNumberOfLiteralWords());
			      if (nbre_literal > 0) {
			        for (int k = 0; k < nbre_literal; ++k) {
			          container.add(rlwi.getLiteralWordAt(k) | rlwj.getLiteralWordAt(k));
			        }
			        rlwi.discardFirstWords(nbre_literal);
			        rlwj.discardFirstWords(nbre_literal);
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
