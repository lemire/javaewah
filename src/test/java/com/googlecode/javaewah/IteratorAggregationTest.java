package com.googlecode.javaewah;

import static org.junit.Assert.*;
import java.util.Iterator;
import org.junit.Test;
import com.googlecode.javaewah.benchmark.ClusteredDataGenerator;

public class IteratorAggregationTest {
	
	public static Iterator<EWAHCompressedBitmap[]> getCollections() {
		final int N = 10;
		final int nbr = 18;
		final ClusteredDataGenerator cdg = new ClusteredDataGenerator(123);
		return new Iterator<EWAHCompressedBitmap[]>() {
			int sparsity = 1;

			@Override
			public boolean hasNext() {
				return sparsity < 25 - nbr;
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
				sparsity+=3;
				return ewah;
			}

			@Override
			public void remove() {
				// unimplemented
			}
			
		};
		
		
	}
	
	
	
	public static IteratingRLW[]  toIterators(EWAHCompressedBitmap... x ) {
		IteratingRLW[] X = new IteratingRLW[x.length];
		for(int k = 0; k < X.length; ++k) {
			X[k] = new IteratingBufferedRunningLengthWord(  x[k] );
		}
		return X;
	}
	
	@Test
	public void testOr() {	
		Iterator<EWAHCompressedBitmap[]> i = getCollections(); 
		while(i.hasNext() ) {
			EWAHCompressedBitmap[] x = i.next();
			EWAHCompressedBitmap tanswer = EWAHCompressedBitmap.or(x);
			EWAHCompressedBitmap x1 = IteratorUtil.materialize(IteratorAggregation.or(toIterators(x)));
			EWAHCompressedBitmap x2 = IteratorUtil.materialize(IteratorAggregation.bufferedor(toIterators(x)));
			assertTrue(x1.equals(tanswer));
			assertTrue(x2.equals(tanswer));
			assertTrue(x1.equals(x2));
		}
		
	}
	
	
}
