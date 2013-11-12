package com.googlecode.javaewah.benchmark;

import java.text.DecimalFormat;

import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah32.*;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * To benchmark the logical xor aggregate. 
 */
public class BenchmarkXOR32 {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(10, 18, 1);
		//test(2, 22, 1);
	}

	@SuppressWarnings({ "javadoc" })
	public static void test(int N, int nbr, int repeat) {
		long bogus = 0;

		DecimalFormat df = new DecimalFormat("0.###");
		ClusteredDataGenerator cdg = new ClusteredDataGenerator();
		for (int sparsity = 1; sparsity < 30 - nbr; sparsity++) {
			for (int times = 0; times < 2; ++times) {
				String line = "";
				long bef, aft;
				line += sparsity;
				int[][] data = new int[N][];
				int Max = (1 << (nbr + sparsity));
				for (int k = 0; k < N; ++k)
					data[k] = cdg.generateClustered(1 << nbr, Max);
				// building
				EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap32();
					for (int x = 0; x < data[k].length; ++x) {
						ewah[k].set(data[k][x]);
					}
					data[k] = null;
				}
				// sanity check
				if (true) {
					EWAHCompressedBitmap32 answer = ewah[0].xor(ewah[1]);
					for (int k = 2; k < ewah.length; ++k)
						answer = answer.xor(ewah[k]);
					EWAHCompressedBitmap32 ewahor3 = FastAggregation.xor(ewah);
					if (!answer.equals(ewahor3))
						throw new RuntimeException("bug FastAggregation.xor");
					EWAHCompressedBitmap32 ewahor2 = FastAggregation32
							.bufferedxor(65536,ewah);
					if (!answer.equals(ewahor2))
						throw new RuntimeException(
								"bug FastAggregation.bufferedxor ");
					EWAHCompressedBitmap32 iwah = IteratorUtil32.materialize(IteratorAggregation32.bufferedxor(IteratorUtil32.toIterators(ewah)));
					if (!answer.equals(iwah))
						throw new RuntimeException(
								"bug xor it ");

				}

				// logical xor
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap32 ewahor = ewah[0];
						for (int j = 1; j < k + 1; ++j) {
							ewahor = ewahor.xor(ewah[j]);
						}
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);

				// fast logical xor
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap32[] ewahcp = new EWAHCompressedBitmap32[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						EWAHCompressedBitmap32 ewahor = FastAggregation
								.xor(ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);


				// fast logical xor
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap32[] ewahcp = new EWAHCompressedBitmap32[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						EWAHCompressedBitmap32 ewahor = FastAggregation32
								.bufferedxor(65536,ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);
				
				// fast logical xor
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						IteratingRLW32[] ewahcp = new IteratingRLW32[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = new IteratingBufferedRunningLengthWord32(
									ewah[j]);
						}
						IteratingRLW32 ewahor = IteratorAggregation32.bufferedxor(ewahcp);
						int wordcounter = IteratorUtil32.cardinality(ewahor);
						bogus += wordcounter;
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);


				System.out
						.println("# times for: 2by2 FastAggregation.xor  bufferedxor iterator-based");

				System.out.println(line);
			}
			System.out.println("# bogus =" + bogus);

		}
	}
}
