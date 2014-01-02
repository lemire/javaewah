package com.googlecode.javaewah.datastructure;

import java.util.Comparator;
import java.util.PriorityQueue; // for testing only
import java.util.List; // for testing only
import java.util.ArrayList; // for testing only

// special purpose priority queue.
// does limited error checking
// supports toss, buildHeap, poll, peek, percolateDown
// The final operation is the main reason we need this
// java.util.PriorityQueue was being used by polling and then reinserting
// the item with a new priority.  This would force a downward percolation
// and then an upward one.

public final class PriorityQ<T> {
        T[] a;
        int lastIndex;
        Comparator<T> comp;


        @SuppressWarnings("unchecked")
        public PriorityQ(int maxSize, Comparator<T> c) {
                a = (T[]) new Object[maxSize + 1];
                lastIndex = 0;
                comp = c;
        }

        public int size() {
                return lastIndex;
        }

        private int compare(T A, T B) {
                return comp.compare(A, B);
        }

        public void toss(T t) {
                a[++lastIndex] = t;
        }

        public T peek() {
                return a[1];
        }

        public T get(int i) {
                return a[i + 1];
        }

        public void buildHeap() {
                for (int i = lastIndex / 2; i > 0; --i) {
                        percolateDown(i);
                }
        }


        // this operation is used heavily
        public void percolateDown(int i) {
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

        public T poll() {
                T ans = a[1];
                a[1] = a[lastIndex--];
                percolateDown(1);
                return ans;
        }

        public boolean isEmpty() {
                return lastIndex == 0;
        }

        private T sneak(int i) {
                return a[i];
        }
}
