package com.googlecode.javaewah.datastructure;

import java.util.Comparator;

/**
 * Special-purpose priority queue. Does limited error checking and supports
 * toss, buildHeap, poll, peek, percolateDown. It is faster than the equivalent
 * class from java.util.
 *
 * @param <T> object type
 * @author Owen Kaser
 * @since 0.8.0
 */
public final class PriorityQ<T> {
    final T[] a;
    int lastIndex;
    final Comparator<T> comp;

    /**
     * Construct a priority queue with a given capacity
     *
     * @param maxSize capacity
     * @param c       comparator
     */
    @SuppressWarnings("unchecked")
    public PriorityQ(final int maxSize, final Comparator<T> c) {
        this.a = (T[]) new Object[maxSize + 1];
        this.lastIndex = 0;
        this.comp = c;
    }

    /**
     * @return the size of the queue
     */
    public int size() {
        return this.lastIndex;
    }

    private int compare(T a, T b) {
        return this.comp.compare(a, b);
    }

    /**
     * Add an element at the end of the queue
     *
     * @param t element to be added
     */
    public void toss(final T t) {
        this.a[++this.lastIndex] = t;
    }

    /**
     * Look at the top of the heap
     *
     * @return the element on top
     */
    public T peek() {
        return this.a[1];
    }

    /**
     * build the heap...
     */
    public void buildHeap() {
        for (int i = this.lastIndex / 2; i > 0; --i) {
            percolateDown(i);
        }
    }

    /**
     * Signals that the element on top of the heap has been updated
     */
    public void percolateDown() {
        percolateDown(1);
    }

    private void percolateDown(int i) {
        T ai = this.a[i];
        while (true) {
            int l = 2 * i;
            int r = l + 1;
            int smallest = i;

            if (r <= this.lastIndex) { // then l also okay
                if (compare(this.a[l], ai) < 0) { // l beats i
                    smallest = l;
                    if (compare(this.a[r], this.a[smallest]) < 0)
                        smallest = r;
                } else if (compare(this.a[r], ai) < 0)
                    smallest = r;
            } else {// may have a l, don't have a r
                if ((l <= this.lastIndex)
                        && (compare(this.a[l], ai) < 0))
                    smallest = l;
            }
            if (i != smallest) {
                // conceptually, swap a[i]& a[smallest]
                // but as an opt., we use ai and just save at
                // end
                // temp = a[i];
                this.a[i] = this.a[smallest]; // move smallest
                // one up into
                // place of i
                i = smallest;
            } else {
                this.a[smallest] = ai;
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
        T ans = this.a[1];
        this.a[1] = this.a[this.lastIndex--];
        percolateDown(1);
        return ans;
    }

    /**
     * Check whether the heap is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.lastIndex == 0;
    }
}
