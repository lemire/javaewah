package com.googlecode.javaewah.benchmark;

import java.text.DecimalFormat;
import com.googlecode.javaewah.*;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * To benchmark the Boolean threshold aggregate. 
 */
public class BenchmarkThreshold {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(10, 18, 1);
	}

	@SuppressWarnings({ "javadoc"})
	public static void test(int N, int nbr, int repeat) {
		long bogus = 0;

		DecimalFormat df = new DecimalFormat("0.###");
		ClusteredDataGenerator cdg = new ClusteredDataGenerator();
		for (int sparsity = 1; sparsity < 30 - nbr; sparsity++) {
			for (int times = 0; times < 5; ++times) {
				String line = "";
				long bef, aft;
				line += sparsity;
				int[][] data = new int[N][];
				int Max = (1 << (nbr + sparsity));
				int[] inter = cdg.generateClustered(1 << (nbr/2), Max);
				for (int k = 0; k < N; ++k) 
					data[k] = Benchmark.unite2by2(cdg.generateClustered(1 << nbr, Max),inter);
				// building
				EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap();
					for (int x = 0; x < data[k].length; ++x) {
						ewah[k].set(data[k][x]);
					}
					data[k] = null;
				}


				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						for(int T= 1; T<ewahcp.length+1;++T) {
						  EWAHCompressedBitmap ewaht = EWAHCompressedBitmap
								.threshold(T,ewahcp);
						  bogus += ewaht.sizeInBits();
						}
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);



				System.out.println(line);
			}
			System.out.println("# bogus =" + bogus);

		}
	}
}
