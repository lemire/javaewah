package com.googlecode.javaewah.benchmark;

import java.text.DecimalFormat;

import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah32.*;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * To benchmark the logical or (union) aggregate. 
 */
public class BenchmarkUnion32 {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(10, 18, 1);
	}

	@SuppressWarnings({ "javadoc", "deprecation" })
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
				if(true){
					EWAHCompressedBitmap32 answer = ewah[0].or(ewah[1]);
					for(int k = 2; k < ewah.length; ++k) 
                                            answer = answer.or(ewah[k]);

					EWAHCompressedBitmap32 ewahor = EWAHCompressedBitmap32
							.or(ewah);
					if(!answer.equals(ewahor)) throw new RuntimeException("bug EWAHCompressedBitmap.or");
					EWAHCompressedBitmap32 ewahor3 = FastAggregation
							.or(ewah);
					if(!ewahor.equals(ewahor3)) throw new RuntimeException("bug FastAggregation.or");
					EWAHCompressedBitmap32 ewahor2 = FastAggregation32
							.bufferedor(65536,ewah);
					if(!ewahor.equals(ewahor2)) throw new RuntimeException("bug FastAggregation.bufferedor ");

				}
				
				// logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap32 ewahor = ewah[0];
						for (int j = 1; j < k + 1; ++j) {
							ewahor = ewahor.or(ewah[j]);
						}
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
						EWAHCompressedBitmap32 ewahor = FastAggregation
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
						EWAHCompressedBitmap32 ewahor = FastAggregation32
								.bufferedor(65536,ewahcp);
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
						EWAHCompressedBitmap32 x = new EWAHCompressedBitmap32();
						FastAggregation32.legacy_orWithContainer(x, ewahcp);
						bogus += x.sizeInBits();
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);
				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						IteratingRLW32[] ewahcp = new IteratingRLW32[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = new IteratingBufferedRunningLengthWord32(ewah[j]);
						}
						IteratingRLW32 ewahor = IteratorAggregation32
								.bufferedor(ewahcp);
						int wordcounter = IteratorUtil32.cardinality(ewahor);
						bogus += wordcounter;
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);

				
				System.out
						.println("# times for: 2by2 EWAHCompressedBitmap.or FastAggregation.or experimentalor bufferedor legacygroupedor iterator-bufferedor");

				System.out.println(line);
			}
			System.out.println("# bogus =" + bogus);

		}
	}
}
