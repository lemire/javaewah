package com.googlecode.javaewah.datastructure;

import java.nio.LongBuffer;
import java.util.Iterator;

import com.googlecode.javaewah.IntIterator;

/**
 * 
 * This is an immutable version of the BitSet class in this same package.
 * It is meant to be used with memory-file mapping.
 * 
 * <pre>final FileOutputStream fos = new FileOutputStream(tmpfile);
 *	BitSet Bitmap = BitSet.bitmapOf(0, 2, 55, 64, 512);
 *	Bitmap.serialize(new DataOutputStream(fos));
 *		RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
 *		ByteBuffer bb = memoryMappedFile.getChannel().map(
 *				FileChannel.MapMode.READ_ONLY, 0, totalcount);
 *		ImmutableBitSet mapped = new ImmutableBitSet(bb.asLongBuffer());</pre>
 *
 */
public class ImmutableBitSet  implements Cloneable, Iterable<Integer>,WordArray {
  /**
   * Construct a ImmutableBitSet from the content of the LongBuffer 
   * which should have been initialized with BitSet.serialize (from the BitSet in this
   * same package).
   * 
   * The input is not modified.
   *
   * @param bs the data source
   */
  public ImmutableBitSet(final LongBuffer bs) {
  	int length = (int) bs.get(0);
  	LongBuffer copy = bs.slice();
  	copy.position(1);
  	data = copy.slice();
  	data.limit(length);
  }
  
  /**
   * Get a copy of this ImmutableBitSet as a mutable BitSet.
   * @return a copy
   */
  public BitSet asBitSet() {
  	BitSet bs = new BitSet(this.size());
  	this.data.rewind();
  	this.data.get(bs.data, 0, bs.data.length);
		return bs;
  }

  /**
   * Compute the number of bits set to 1
   *
   * @return the number of bits
   */
  public int cardinality() {
      int sum = 0;
      int length = this.data.limit();
      for(int k = 0; k < length; ++k)
          sum += Long.bitCount(this.data.get(k));
      return sum;
  }

  @Override
  public ImmutableBitSet clone()  {
  	  ImmutableBitSet b;
      try {
          b = (ImmutableBitSet) super.clone();
          b.data = this.data.duplicate();
          return b;
      } catch (CloneNotSupportedException e) {
          return null;
      }
  }

	@Override
  public boolean equals(Object o) {
		if (o instanceof WordArray) {
			WordArray bs = (WordArray) o;
			for (int k = 0; k < Math.min(this.getNumberOfWords(),
					bs.getNumberOfWords()); ++k) {
				if (this.getWord(k) != bs.getWord(k))
					return false;
			}
			WordArray longer = bs.getNumberOfWords() < this.getNumberOfWords() ? this
					: bs;
			for (int k = Math.min(this.getNumberOfWords(),
					bs.getNumberOfWords()); k < Math.max(this.getNumberOfWords(),
	  					bs.getNumberOfWords()); ++k) {
				if (longer.getWord(k) != 0) {
					return false;
				}
			}
			return true;
		}
		return false;    
	}

  /**
   * Check whether a bitset contains a set bit.
   *
   * @return true if no set bit is found
   */
  public boolean empty() {
    int length = this.data.limit();
    for(int k = 0; k < length; ++k)
          if (this.data.get(k) != 0) return false;
      return true;
  }
  
  /**
   * get value of bit i
   * 
   * @param i index
   * @return value of the bit
   */
  public boolean get(final int i) {
      return (this.data.get(i / 64) & (1l << (i % 64))) != 0;
  }

  @Override
  public int hashCode() {
      int b = 31;
      long hash = 0;
      int length = this.data.limit();
      for(int k = 0; k < length; ++k) {
      	  long aData = this.data.get(k);
          hash = hash * b + aData;
      }
      return (int) hash;
  }

  /**
   * Iterate over the set bits
   *
   * @return an iterator
   */
  public IntIterator intIterator() {
      return new IntIterator() {
          @Override
          public boolean hasNext() {
              return this.i >= 0;
          }

          @Override
          public int next() {
              this.j = this.i;
              this.i = ImmutableBitSet.this.nextSetBit(this.i + 1);
              return this.j;
          }

          private int i = ImmutableBitSet.this.nextSetBit(0);

          private int j;

      };
  }

  @Override
  public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {
          @Override
          public boolean hasNext() {
              return this.i >= 0;
          }

          @Override
          public Integer next() {
              this.j = this.i;
              this.i = ImmutableBitSet.this.nextSetBit(this.i + 1);
              return this.j;
          }

          @Override
          public void remove() {
          	throw new RuntimeException("Object is immutable");
          }

          private int i = ImmutableBitSet.this.nextSetBit(0);

          private int j;
      };
  }

  /**
   * Checks whether two bitsets intersect.
   *
   * @param bs other bitset
   * @return true if they have a non-empty intersection (result of AND)
   */
  public boolean intersects(BitSet bs) {
      for (int k = 0; k < Math.min(this.data.limit(), bs.data.length); ++k) {
          if ((this.data.get(k) & bs.data[k]) != 0) return true;
      }
      return false;
  }

  /**
   * Usage: for(int i=bs.nextSetBit(0); i&gt;=0; i=bs.nextSetBit(i+1)) {
   * operate on index i here }
   *
   * @param i current set bit
   * @return next set bit or -1
   */
  public int nextSetBit(final int i) {
      int x = i / 64;
      if (x >= this.data.limit())
          return -1;
      long w = this.data.get(x);
      w >>>= (i % 64);
      if (w != 0) {
          return i + Long.numberOfTrailingZeros(w);
      }
      ++x;
      for (; x < this.data.limit(); ++x) {
          if (this.data.get(x) != 0) {
              return x
                      * 64
                      + Long.numberOfTrailingZeros(this.data.get(x));
          }
      }
      return -1;
  }

  /**
   * Usage: for(int i=bs.nextUnsetBit(0); i&gt;=0; i=bs.nextUnsetBit(i+1))
   * { operate on index i here }
   *
   * @param i current unset bit
   * @return next unset bit or -1
   */
  public int nextUnsetBit(final int i) {
      int x = i / 64;
      if (x >= this.data.limit())
          return -1;
      long w = ~this.data.get(x);
      w >>>= (i % 64);
      if (w != 0) {
          return i + Long.numberOfTrailingZeros(w);
      }
      ++x;
      for (; x < this.data.limit(); ++x) {
          if (this.data.get(x) != ~0) {
              return x
                      * 64
                      + Long.numberOfTrailingZeros(~this.data.get(x));
          }
      }
      return -1;
  }



  /**
   * Query the size
   *
   * @return the size in bits.
   */
  public int size() {
      return this.data.limit() * 64;
  }


  /**
   * Iterate over the unset bits
   *
   * @return an iterator
   */
  public IntIterator unsetIntIterator() {
      return new IntIterator() {
          @Override
          public boolean hasNext() {
              return this.i >= 0;
          }

          @Override
          public int next() {
              this.j = this.i;
              this.i = ImmutableBitSet.this.nextUnsetBit(this.i + 1);
              return this.j;
          }

          private int i = ImmutableBitSet.this.nextUnsetBit(0);

          private int j;
      };
  }


	@Override
	public int getNumberOfWords() {
		return data.limit();
	}

	@Override
	public long getWord(int index) {
		return data.get(index);
	}
  @Override
  public String toString() {
      StringBuilder answer = new StringBuilder();
      IntIterator i = this.intIterator();
      answer.append("{");
      if (i.hasNext())
          answer.append(i.next());
      while (i.hasNext()) {
          answer.append(",");
          answer.append(i.next());
      }
      answer.append("}");
      return answer.toString();
  }

  private LongBuffer data;





}
