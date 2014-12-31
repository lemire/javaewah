package com.googlecode.javaewah32;

import java.io.File;
import java.util.Arrays;

import com.carrotsearch.junitbenchmarks.IResultsConsumer;
import com.carrotsearch.junitbenchmarks.WriterConsumer;
import com.carrotsearch.junitbenchmarks.h2.H2Consumer;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class PerformanceTest {

    static final IResultsConsumer CONSOLE_CONSUMER = new WriterConsumer();
    static final IResultsConsumer H2_CONSUMER = new H2Consumer(new File("PerformanceBenchmarks"));

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule(CONSOLE_CONSUMER, H2_CONSUMER);

    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void bigunion() throws Exception {
        for (int k = 1; k < N; k += 10) {
            EWAHCompressedBitmap32 bitmapor = EWAHCompressedBitmap32.or(Arrays.copyOf(ewah, k + 1));
            bogus += bitmapor.cardinality();
        }
    }

    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void toarray() throws Exception {
        for (int k = 1; k < N * 100; ++k) {
            bogus += ewah[k % N].toArray().length;
        }
    }

    @BenchmarkOptions(benchmarkRounds = 3, warmupRounds = 3)
    @Test
    public void cardinality() throws Exception {
        for (int k = 1; k < N * 100; ++k) {
            bogus += ewah[k % N].cardinality();
        }
    }


    @BeforeClass
    public static void prepare() {
        for (int k = 0; k < N; ++k) {
            ewah[k] = new EWAHCompressedBitmap32();
            for (int x = 0; x < M; ++x) {
                ewah[k].set(x * (N - k + 2));
            }
            ewah[k].trim();
        }
    }

    static int N = 1000;
    static int M = 1000;

    static EWAHCompressedBitmap32[] ewah = new EWAHCompressedBitmap32[N];
    public static int bogus = 0;
}
