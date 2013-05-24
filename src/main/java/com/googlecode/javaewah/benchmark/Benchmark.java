package com.googlecode.javaewah.benchmark;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc. and Veronika Zenz
 * Licensed under APL 2.0.
 */


import java.text.DecimalFormat;
import java.util.List;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.FastAggregation;
import com.googlecode.javaewah.IntIterator;

/**
 * This class is used to benchmark the performance 
 * EWAH.
 * 
 * @author Daniel Lemire
 */
public class Benchmark {

  @SuppressWarnings("javadoc")
  public static void main(String args[]) {
    test(100, 16, 1);
  }

  @SuppressWarnings("javadoc")
  public static void test(int N, int nbr, int repeat) {
    DecimalFormat df = new DecimalFormat("0.###");
    ClusteredDataGenerator cdg = new ClusteredDataGenerator();
    for (int sparsity = 1; sparsity < 30 - nbr; sparsity += 4) {
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
          EWAHCompressedBitmap ewahor = EWAHCompressedBitmap.or(ewahcp);
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
          EWAHCompressedBitmap ewahor = FastAggregation.or(ewahcp);
          bogus += ewahor.sizeInBits();
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
            ewahcp[j] = ewah[j];
          }
          EWAHCompressedBitmap ewahand = EWAHCompressedBitmap.and(ewahcp);
          bogus += ewahand.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      // fast logical and
      bef = System.currentTimeMillis();
      for (int r = 0; r < repeat; ++r)
        for (int k = 0; k < N; ++k) {
          EWAHCompressedBitmap[] ewahcp = new EWAHCompressedBitmap[k + 1];
          for (int j = 0; j < k + 1; ++j) {
            ewahcp[j] = ewah[j];
          }
          EWAHCompressedBitmap ewahand = FastAggregation.and(ewahcp);
          bogus += ewahand.sizeInBits();
        }
      aft = System.currentTimeMillis();
      line += "\t" + df.format((aft - bef) / 1000.0);
      System.out.println("time for building, toArray(), Java iterator, intIterator, logical or (2-by-2), logical or (grouped), FastAggregation.or, and (2-by-2),  logical and (grouped),FastAggregation.and");
      System.out.println(line);
      System.out.println("# bogus =" + bogus);
    }
  }
}
