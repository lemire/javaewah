package com.googlecode.javaewah;

import static org.junit.Assert.*;
import java.util.Iterator;
import org.junit.Test;
import com.googlecode.javaewah.benchmark.ClusteredDataGenerator;

/**
 * @author lemire
 *
 */
public class IteratorAggregationTest {

	/**
	 * @param N Number of bitmaps to generate in each set
	 * @return an iterator over sets of bitmaps
	 */
	public static Iterator<EWAHCompressedBitmap[]> getCollections(final int N) {
		final int nbr = 3;
		final ClusteredDataGenerator cdg = new ClusteredDataGenerator(123);
		return new Iterator<EWAHCompressedBitmap[]>() {
			int sparsity = 1;

			@Override
			public boolean hasNext() {
				return sparsity < 5;
			}

			@Override
			public EWAHCompressedBitmap[] next() {
				int[][] data = new int[N][];
				int Max = (1 << (nbr + sparsity));
				for (int k = 0; k < N; ++k)
					data[k] = cdg.generateClustered(1 << nbr, Max);
				EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
				for (int k = 0; k < N; ++k) {
					ewah[k] = new EWAHCompressedBitmap();
					for (int x = 0; x < data[k].length; ++x) {
						ewah[k].set(data[k][x]);
					}
					data[k] = null;
				}
				sparsity += 3;
				return ewah;
			}

			@Override
			public void remove() {
				// unimplemented
			}

		};

	}

	/**
	 * 
	 */
	@Test
	public void testAnd() {
		for (int N = 1; N < 10; ++N) {
			System.out.println("testAnd N = " + N);
			Iterator<EWAHCompressedBitmap[]> i = getCollections(N);
			while (i.hasNext()) {
				EWAHCompressedBitmap[] x = i.next();
				EWAHCompressedBitmap tanswer = EWAHCompressedBitmap.and(x);
				EWAHCompressedBitmap x1 = IteratorUtil
						.materialize(IteratorAggregation.and(IteratorUtil
								.toIterators(x)));
				assertTrue(x1.equals(tanswer));
			}
			System.gc();
		}

	}

	/**
	 * 
	 */
	@Test
	public void testOr() {
		for (int N = 1; N < 10; ++N) {
			System.out.println("testOr N = " + N);
			Iterator<EWAHCompressedBitmap[]> i = getCollections(N);
			while (i.hasNext()) {
				EWAHCompressedBitmap[] x = i.next();
				EWAHCompressedBitmap tanswer = EWAHCompressedBitmap.or(x);
				EWAHCompressedBitmap x1 = IteratorUtil
						.materialize(IteratorAggregation.or(IteratorUtil
								.toIterators(x)));
				//EWAHCompressedBitmap x2 = IteratorUtil
					//	.materialize(IteratorAggregation
						//		.bufferedor(IteratorUtil.toIterators(x)));
				assertTrue(x1.equals(tanswer));
			//	assertTrue(x2.equals(tanswer));
				//assertTrue(x1.equals(x2));
			}
			System.gc();
		}
	}

	/**
	 * 
	 */
	@Test
	public void testXor() {
		System.out.println("testXor ");
		Iterator<EWAHCompressedBitmap[]> i = getCollections(2);
		while (i.hasNext()) {
			EWAHCompressedBitmap[] x = i.next();
			EWAHCompressedBitmap tanswer = x[0].xor(x[1]);
			EWAHCompressedBitmap x1 = IteratorUtil
					.materialize(IteratorAggregation.xor(
							x[0].getIteratingRLW(), x[1].getIteratingRLW()));
			assertTrue(x1.equals(tanswer));
		}
		System.gc();
	}

}
