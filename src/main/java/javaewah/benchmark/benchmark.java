package javaewah.benchmark;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import javaewah.EWAHCompressedBitmap;
import javaewah.FastAggregation;
import javaewah32.EWAHCompressedBitmap32;

public class benchmark {

  public static void main(String args[]) {
    test(100, 16, 1);
  }

  public static void test(int N, int nbr, int repeat) {
    DecimalFormat df = new DecimalFormat("0.###");
    ClusteredDataGenerator cdg = new ClusteredDataGenerator();
    for (int sparsity = 1; sparsity < 31 - nbr; sparsity += 1) {
      long bogus = 0;
      String line = "";
      long bef, aft;
      line += sparsity;
      int[][] data = new int[N][];
      int Max = (1 << (nbr + sparsity));
      System.out.println("# generating random data...");
      for (int k = 0; k < N; ++k)
        data[k] = cdg.generateClustered(1 << nbr, Max);
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
            ewahcp[j] = ewah[k];
          }
          EWAHCompressedBitmap ewahor = EWAHCompressedBitmap.or(ewahcp);
          bogus += ewahor.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);

      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r) {
        /*
         * PriorityQueue<EWAHCompressedBitmap> pq = new
         * PriorityQueue<EWAHCompressedBitmap>( N, new
         * Comparator<EWAHCompressedBitmap>() { public int
         * compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) { return
         * a.sizeInBits() - b.sizeInBits(); } }); for (EWAHCompressedBitmap x :
         * ewah) pq.add(x); while (pq.size() > 1) { EWAHCompressedBitmap x1 =
         * pq.poll(); EWAHCompressedBitmap x2 = pq.poll(); pq.add(x1.or(x2)); }
         */
        EWAHCompressedBitmap bitmapor = FastAggregation.or(ewah);

        ;// pq.poll();
        bogus += bitmapor.sizeInBits();
      }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);

      // logical and
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          EWAHCompressedBitmap ewahand = ewah[0];
          for (int j = 1; j < k; ++j) {
            ewahand = ewahand.and(ewah[j]);
          }
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // fast logical and
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
          for (int j = 0; j < k + 1; ++j) {
            ewahcp[j] = ewah[k];
          }
          EWAHCompressedBitmap ewahand = EWAHCompressedBitmap.and(ewahcp);
          bogus += ewahand.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);

      // fast logical and
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r) {
        EWAHCompressedBitmap ewahand = FastAggregation.and(ewah);
        bogus += ewahand.sizeInBits();
      }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);

      ewah = null;
      line += "\t";
      // building
      bef = System.currentTimeMillis();
      EWAHCompressedBitmap32[] ewah32 = new EWAHCompressedBitmap32[N];
      int size32 = 0;
      for (int r = 0; r < repeat; ++r)
        size32 = 0;
      for (int k = 0; k < N; ++k) {
        ewah32[k] = new EWAHCompressedBitmap32();
        for (int x = 0; x < data[k].length; ++x) {
          ewah32[k].set(data[k][x]);
        }
        size32 += ewah32[k].sizeInBytes();
      }
      aft = System.currentTimeMillis();
      line += "\t" + size32;
      line += "\t" + df.format((aft - bef) / 1000.0);
      // uncompressing
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          int[] array = ewah32[k].toArray();
          bogus += array.length;
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // uncompressing
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          int[] array = new int[ewah32[k].cardinality()];
          int c = 0;
          for (int x : ewah32[k])
            array[c++] = x;
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // uncompressing
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          List<Integer> L = ewah32[k].getPositions();
          int[] array = new int[L.size()];
          int c = 0;
          for (int x : L)
            array[c++] = x;
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // logical or
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          EWAHCompressedBitmap32 ewahor = ewah32[0];
          for (int j = 1; j < k; ++j) {
            ewahor = ewahor.or(ewah32[j]);
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
            ewahcp[j] = ewah32[k];
          }
          EWAHCompressedBitmap32 ewahor = EWAHCompressedBitmap32.or(ewahcp);
          bogus += ewahor.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // logical and
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          EWAHCompressedBitmap32 ewahand = ewah32[0];
          for (int j = 1; j < k; ++j) {
            ewahand = ewahand.and(ewah32[j]);
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
            ewahcp[j] = ewah32[k];
          }
          EWAHCompressedBitmap32 ewahand = EWAHCompressedBitmap32.and(ewahcp);
          bogus += ewahand.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      ewah32 = null;

      System.out.println(line);
      System.out.println("# bogus =" + bogus);
    }
  }
}
