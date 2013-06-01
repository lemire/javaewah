package com.googlecode.javaewah.benchmark;

import java.text.DecimalFormat;
import com.googlecode.javaewah.*;

/**
 * @author lemire
 *
 */
public class BenchmarkUnion {

	@SuppressWarnings("javadoc")
	public static void main(String args[]) {
		test(10, 18, 1);
	}

	@SuppressWarnings("javadoc")
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
				//System.out.println("# generating random data...");
				for (int k = 0; k < N; ++k)
					data[k] = cdg.generateClustered(1 << nbr, Max);
				//System.out.println("# generating random data... ok.");
				// building
				EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
//				int size = 0;
					for (int k = 0; k < N; ++k) {
						ewah[k] = new EWAHCompressedBitmap();
						for (int x = 0; x < data[k].length; ++x) {
							ewah[k].set(data[k][x]);
						}
						data[k] = null;
	//					size += ewah[k].sizeInBytes();
					}
				// sanity check
				if(true){
					EWAHCompressedBitmap ewahor = EWAHCompressedBitmap
							.or(ewah);

					EWAHCompressedBitmap ewahor3 = FastAggregation
							.or(ewah);
					if(!ewahor.equals(ewahor3)) throw new RuntimeException("bug FastAggregation.or");
					//System.out.println("# sanity check ok");
					EWAHCompressedBitmap ewahor2 = FastAggregation
							.bufferedor(ewah);
					if(!ewahor.equals(ewahor2)) throw new RuntimeException("bug FastAggregation.bufferedor ");

				}
				
				// logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap ewahor = ewah[0];
						for (int j = 1; j < k; ++j) {
							ewahor = ewahor.or(ewah[j]);
						}
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
						EWAHCompressedBitmap ewahor = FastAggregation
								.or(ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();
				line += "\t" + df.format((aft - bef) / 1000.0);

				line += "\t" + df.format((aft - bef) / 1000.0);

				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = ewah[j];
						}
						EWAHCompressedBitmap ewahor = FastAggregation
								.bufferedor(ewahcp);
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
						EWAHCompressedBitmap ewahor = FastAggregation
								.smartor(ewahcp);
						bogus += ewahor.sizeInBits();
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);
				// fast logical or
				bef = System.currentTimeMillis();
				for (int r = 0; r < repeat; ++r)
					for (int k = 0; k < N; ++k) {
						IteratingRLW[] ewahcp = new IteratingRLW[k + 1];
						for (int j = 0; j < k + 1; ++j) {
							ewahcp[j] = new IteratingBufferedRunningLengthWord(ewah[j]);
						}
						IteratingRLW ewahor = IteratorAggregation
								.or(ewahcp);
						int wordcounter = IteratorUtil.cardinality(ewahor);
						bogus += wordcounter;
					}
				aft = System.currentTimeMillis();

				line += "\t" + df.format((aft - bef) / 1000.0);

				
				System.out
						.println("# times for: 2by2 EWAHCompressedBitmap.or FastAggregation.or experimentalor bufferedor smartor iterator-bufferedor");

				System.out.println(line);
			}
			System.out.println("# bogus =" + bogus);

		}
	}
}