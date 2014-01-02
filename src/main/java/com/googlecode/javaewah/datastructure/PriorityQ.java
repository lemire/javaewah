package com.googlecode.javaewah.datastructure;

import java.util.Comparator;

/**
 * Special-purpose priority queue.
 * does limited error checking
 * and supports toss, buildHeap, poll, peek, percolateDown.
 * @param <T> object type 
 * 
 * @author Owen Kaser
 * @since 0.8.0
 */
public final class PriorityQ<T> {
        T[] a;
        int lastIndex;
        Comparator<T> comp;


        /**
         * Construct a priority queue with a given capacity 
         * 
         * @param maxSize capacity
         * @param c comparator
         */
        @SuppressWarnings("unchecked")
        public PriorityQ(final int maxSize, final Comparator<T> c) {
                a = (T[]) new Object[maxSize + 1];
                lastIndex = 0;
                comp = c;
        }

        /**
         * @return the size of the queue
         */
        public int size() {
                return lastIndex;
        }

        private int compare(T A, T B) {
                return comp.compare(A, B);
        }

        /**
         * Add an element at the end of the queue
         * 
         * @param t element to be added
         */
        public void toss(final T t) {
                a[++lastIndex] = t;
        }

        /**
         * Look at the top of the heap
         * @return the element on top
         */
        public T peek() {
                return a[1];
        }


        /**
         * build the heap...
         */
        public void buildHeap() {
                for (int i = lastIndex / 2; i > 0; --i) {
                        percolateDown(i);
                }
        }


        /**
         * Signals that the element on top of the heap has been updated
         * 
         */
        public void percolateDown() {
                percolateDown(1);
        }
        
        private void percolateDown(int i) {
                T ai = a[i];
                while (true) {
                        int l = 2 * i;
                        int r = l + 1;
                        int smallest = i;

                        if (r <= lastIndex) { // then l also okay
                                if (compare(a[l], ai) < 0) { // l beats i
                                        smallest = l;
                                        if (compare(a[r], a[smallest]) < 0)
                                                smallest = r;
                                } else if (compare(a[r], ai) < 0)
                                        smallest = r;
                        } else {// may have a l, don't have a r
                                if ((l <= lastIndex) && (compare(a[l], ai) < 0))
                                        smallest = l;
                        }
                        if (i != smallest) {
                                // conceptually, swap a[i]& a[smallest]
                                // but as an opt., we use ai and just save at
                                // end
                                // temp = a[i];
                                a[i] = a[smallest]; // move smallest one up into
                                                    // place of i
                                i = smallest;
                        } else {
                                a[smallest] = ai;
                                return;
                        }
                }
        }

        /**
         * Remove the element on top of the heap
         * 
         * @return the element being removed
         */
        public T poll() {
                T ans = a[1];
                a[1] = a[lastIndex--];
                percolateDown(1);
                return ans;
        }

        /**
         * Check whether the heap is empty.
         * 
         * @return true if empty
         */
        public boolean isEmpty() {
                return lastIndex == 0;
        }
}
