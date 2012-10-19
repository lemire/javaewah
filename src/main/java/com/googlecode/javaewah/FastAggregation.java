package com.googlecode.javaewah;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Fast algorithms to aggregate many bitmaps.
 * These algorithms are just given as reference.
 * They may not be faster than the corresponding
 * methods in the EWAHCompressedBitmap class.
 * 
 * @author Daniel Lemire
 *
 */
public class FastAggregation {
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends LogicalElement> T and(T...bitmaps) {
    // for "and" a priority queue is not needed, but
    // overhead ought to be small
    PriorityQueue<T> pq = new PriorityQueue<T>(
      bitmaps.length, new Comparator<T>() {   
        public int compare(T a, T b) {
          return a.sizeInBytes() - b.sizeInBytes();
        }
      });
    for (T x : bitmaps)
      pq.add(x);
    while (pq.size() > 1) {
      T x1 = pq.poll();
      T x2 = pq.poll();
      pq.add( (T) x1.and(x2));
    }
    return pq.poll();
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends LogicalElement> T or(T...bitmaps) {
    PriorityQueue<T> pq = new PriorityQueue<T>(
      bitmaps.length, new Comparator<T>() {
        public int compare(T a, T b) {
          return a.sizeInBytes() - b.sizeInBytes();
        }
      });
    for (T x : bitmaps) {
      pq.add(x);
    }
    while (pq.size() > 1) {
      T x1 = pq.poll();
      T x2 = pq.poll();
      pq.add( (T) x1.or(x2));
    }
    return pq.poll();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends LogicalElement> T xor(T...bitmaps) {
    PriorityQueue<T> pq = new PriorityQueue<T>(
      bitmaps.length, new Comparator<T>() {
     
        public int compare(T a, T b) {
          return a.sizeInBytes() - b.sizeInBytes();
        }
      });
    for (T x : bitmaps)
      pq.add(x);
    while (pq.size() > 1) {
      T x1 = pq.poll();
      T x2 = pq.poll();
      pq.add( (T) x1.xor(x2));
    }
    return pq.poll();
  }
}
