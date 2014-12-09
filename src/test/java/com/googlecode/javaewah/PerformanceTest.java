package com.googlecode.javaewah;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class PerformanceTest
{
	  @Rule
	  public TestRule benchmarkRun = new BenchmarkRule();
	 
	  @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
	  @Test
	  public void bigunion() throws Exception {
	  	for(int k = 1; k < N ; k+=10) {
				EWAHCompressedBitmap bitmapor = EWAHCompressedBitmap.or(Arrays.copyOf(ewah, k+1));
				bogus += bitmapor.cardinality();
	  	}
	  }
	  
	  @BeforeClass
    public static void prepare()
    {
			for (int k = 0; k < N; ++k) {
				ewah[k] = new EWAHCompressedBitmap();
				for (int x = 0; x < M; ++x) {
					ewah[k].set(x*(N-k+2));
				}
				ewah[k].trim();
			}
    }
	  static int N = 1000;
	  static int M = 1000;

	  static EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
		public static int bogus = 0;
}

