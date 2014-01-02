package com.googlecode.javaewah.benchmark;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah.IteratingRLW;
import com.googlecode.javaewah.IteratorAggregation;
import com.googlecode.javaewah.IteratorUtil;

/**
 * This class is used to benchmark the performance EWAH.
 * 
 * @author Daniel Lemire
 */
public class Benchmark {
	
	/**
	 * Compute the union between two sorted arrays
	 * @param set1 first sorted array
	 * @param set2 second sorted array
	 * @return merged array
	 */
	static public int[] unite2by2(final int[] set1, final int[] set2) {
		int pos = 0;
		int k1 = 0, k2 = 0;
		if (0 == set1.length)
			return Arrays.copyOf(set2, set2.length);
		if (0 == set2.length)
			return Arrays.copyOf(set1, set1.length);
		int[] buffer = new int[set1.length + set2.length];
		while (true) {
			if (set1[k1] < set2[k2]) {
				buffer[pos++] = set1[k1];
				++k1;
				if (k1 >= set1.length) {
					for (; k2 < set2.length; ++k2)
						buffer[pos++] = set2[k2];
					break;
				}
			} else if (set1[k1] == set2[k2]) {
				buffer[pos++] = set1[k1];
				++k1;
				++k2;
				if (k1 >= set1.length) {
					for (; k2 < set2.length; ++k2)
						buffer[pos++] = set2[k2];
					break;
				}
				if (k2 >= set2.length) {
					for (; k1 < set1.length; ++k1)
						buffer[pos++] = set1[k1];
					break;
				}
			} else {// if (set1[k1]>set2[k2]) {
				buffer[pos++] = set2[k2];
				++k2;
				if (k2 >= set2.length) {
					for (; k1 < set1.length; ++k1)
						buffer[pos++] = set1[k1];
					break;
				}
			}
		}
		return Arrays.copyOf(buffer, pos);
	}


	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		//test(2, 24, 1);
		test(100, 16, 1);
	}

	@SuppressWarnings("javadoc")
	public static void test(int N, int nbr, int repeat) {
		DecimalFormat df = new DecimalFormat("0.###");
		ClusteredDataGenerator cdg = new ClusteredDataGenerator();
		for (int sparsity = 1; sparsity < 30 - nbr; sparsity += 2) {
			long bogus = 0;
			String line = "";
			long bef, aft;
			line += sparsity;
			int[][] data = new int[N][];
			int Max = (1 << (nbr + sparsity));
			System.out.println("# generating random data...");
			int[] inter = cdg.generateClustered(1 << (nbr/2), Max);
			for (int k = 0; k < N; ++k) 
				data[k] = unite2by2(cdg.generateClustered(1 << nbr, Max),inter);
			System.out.println("# generating random data... ok.");
			// building
			bef = System.currentTimeMillis();
			EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
			int size = 0;
			for (int r = 0; r < repeat; ++r) {
				size = 0;
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap();
					for (int x = 0; x < data[k].length; ++x) {
						ewah[k].set(data[k][x]);
					}
					size += ewah[k].sizeInBytes();
				}
			}
			aft = System.currentTimeMillis();
			line += "\t" + size;
			line += "\t" + df.format((aft - bef) / 1000.0);
			// uncompressing
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					int[] array = ewah[k].toArray();
					bogus += array.length;
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// uncompressing
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					int[] array = new int[ewah[k].cardinality()];
					int c = 0;
					for (int x : ewah[k])
						array[c++] = x;
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// uncompressing
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					List<Integer> L = ewah[k].getPositions();
					int[] array = new int[L.size()];
					int c = 0;
					for (int x : L)
						array[c++] = x;
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// uncompressing
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					IntIterator iter = ewah[k].intIterator();
					while (iter.hasNext()) {
						bogus += iter.next();
					}
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			line += "\t\t\t";
			// logical or
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap ewahor = ewah[0];
					for (int j = 1; j < k + 1; ++j) {
						ewahor = ewahor.or(ewah[j]);
					}
					bogus += ewahor.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// fast logical or
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap ewahor = EWAHCompressedBitmap
							.or(ewahcp);
					bogus += ewahor.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);

			// fast logical or
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap ewahor = FastAggregation.or(ewahcp);
					bogus += ewahor.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// fast logical or
			// run sanity check
			for (int k = 0; k < N; ++k) {
				IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
				for (int j = 0; j < k + 1; ++j) {
					ewahcp[j] = ewah[j].getIteratingRLW();
				}
				IteratingRLW ewahor = IteratorAggregation.bufferedor(ewahcp);
				EWAHCompressedBitmap ewahorp = EWAHCompressedBitmap.or(Arrays.copyOf(ewah, k+1));
				EWAHCompressedBitmap mewahor = IteratorUtil.materialize(ewahor);
				if(!ewahorp.equals(mewahor)) throw new RuntimeException("bug");
			}
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j].getIteratingRLW();
					}
					IteratingRLW ewahor = IteratorAggregation.bufferedor(ewahcp);
					bogus +=  IteratorUtil.materialize(ewahor).sizeInBits();
				}
			aft = System.currentTimeMillis();

			line += "\t" + df.format((aft - bef) / 1000.0);
			line += "\t\t\t";
			// logical and
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap ewahand = ewah[0];
					for (int j = 1; j < k + 1; ++j) {
						ewahand = ewahand.and(ewah[j]);
					}
					bogus += ewahand.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// fast logical and
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap ewahand = EWAHCompressedBitmap
							.and(ewahcp);
					bogus += ewahand.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);

			for (int k = 0; k < N; ++k) {
				IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
				for (int j = 0; j < k + 1; ++j) {
					ewahcp[j] = ewah[j].getIteratingRLW();
				}
				IteratingRLW ewahand = IteratorAggregation.bufferedand(ewahcp);
				EWAHCompressedBitmap ewahandp = EWAHCompressedBitmap.and(Arrays.copyOf(ewah, k+1));
				EWAHCompressedBitmap mewahand =  IteratorUtil.materialize(ewahand);
				if(!ewahandp.equals(mewahand)) throw new RuntimeException("bug");
			}
			// fast logical and
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j].getIteratingRLW();
					}
					IteratingRLW ewahand = IteratorAggregation.bufferedand(ewahcp);
					bogus += IteratorUtil.materialize(ewahand).sizeInBits();
				}
			aft = System.currentTimeMillis();

			line += "\t" + df.format((aft - bef) / 1000.0);

			
			System.out
					.println("time for building, toArray(), Java iterator, intIterator,\t\t\t logical or (2-by-2), logical or (grouped), FastAggregation.or, iterator-based or, \t\t\t (2-by-2) and,  logical and (grouped), iterator-based and");
			System.out.println(line);
			System.out.println("# bogus =" + bogus);
		}
	}
}
