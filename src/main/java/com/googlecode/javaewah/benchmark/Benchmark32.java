package com.googlecode.javaewah.benchmark;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */

import java.text.DecimalFormat;
import java.util.List;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah.IntIterator;
import com.googlecode.javaewah32.IteratingRLW32;
import com.googlecode.javaewah32.IteratorAggregation32;
import com.googlecode.javaewah32.IteratorUtil32;

/**
 * This class is used to benchmark the performance EWAH.
 * 
 * @author Daniel Lemire
 */
public class Benchmark32 {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(100, 16, 1);
//		test(2, 24, 1);
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
				data[k] = Benchmark.unite2by2(cdg.generateClustered(1 << nbr, Max),inter);
			System.out.println("# generating random data... ok.");
			// building
			bef = System.currentTimeMillis();
			EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
			int size = 0;
			for (int r = 0; r < repeat; ++r) {
				size = 0;
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap32();
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
					EWAHCompressedBitmap32 ewahor = ewah[0];
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
					EWAHCompressedBitmap32[] ewahcp = new EWAHCompressedBitmap32[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap32 ewahor = EWAHCompressedBitmap32
							.or(ewahcp);
					bogus += ewahor.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);

			// fast logical or
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap32[] ewahcp = new EWAHCompressedBitmap32[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap32 ewahor = FastAggregation.or(ewahcp);
					bogus += ewahor.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);
			// fast logical or
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					IteratingRLW32[] ewahcp = new IteratingRLW32[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j].getIteratingRLW();
					}
					IteratingRLW32 ewahor = IteratorAggregation32.bufferedor(ewahcp);
					bogus +=  IteratorUtil32.materialize(ewahor).sizeInBits();
				}
			aft = System.currentTimeMillis();

			line += "\t" + df.format((aft - bef) / 1000.0);
			line += "\t\t\t";
			// logical and
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					EWAHCompressedBitmap32 ewahand = ewah[0];
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
					EWAHCompressedBitmap32[] ewahcp = new EWAHCompressedBitmap32[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j];
					}
					EWAHCompressedBitmap32 ewahand = EWAHCompressedBitmap32
							.and(ewahcp);
					bogus += ewahand.sizeInBits();
				}
			aft = System.currentTimeMillis();
			line += "\t" + df.format((aft - bef) / 1000.0);


			// fast logical and
			bef = System.currentTimeMillis();
			for (int r = 0; r < repeat; ++r)
				for (int k = 0; k < N; ++k) {
					IteratingRLW32[] ewahcp = new IteratingRLW32[k + 1];
					for (int j = 0; j < k + 1; ++j) {
						ewahcp[j] = ewah[j].getIteratingRLW();
					}
					IteratingRLW32 ewahand = IteratorAggregation32.bufferedand(ewahcp);
					bogus += IteratorUtil32.materialize(ewahand).sizeInBits();
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
