package com.googlecode.javaewah.symmetric;

/**
 * This is meant to be a crude version of OwenDanielsMerge.java 
 * running on top of JavaEWAH.
 * 
 * 
 * @author lemire
 *
 */
// import java.util.PriorityQueue;
import java.util.Comparator;

import com.googlecode.javaewah.*; // see "lib" directory 
import com.googlecode.javaewah.datastructure.PriorityQ;

public class RunningBitmapMerge implements BitmapSymmetricAlgorithm {

        @Override
        public void symmetric(UpdateableBitmapFunction f, BitmapStorage out,
                EWAHCompressedBitmap... set) {
                final PriorityQ<EWAHPointer> H = new PriorityQ<EWAHPointer>(
                        set.length, new Comparator<EWAHPointer>() {
                                @Override
                                public int compare(EWAHPointer arg0,
                                        EWAHPointer arg1) {
                                        return arg0.compareTo(arg1);
                                }
                        });
                f.resize(set.length);

                for (int k = 0; k < set.length; ++k) {
                        final EWAHPointer x = new EWAHPointer(0,
                                new IteratingBufferedRunningLengthWord(set[k]),
                                k);
                        if (x.hasNoData())
                                continue;
                        f.rw[k] = x;
                        x.callbackUpdate(f);
                        H.toss(x);
                }
                H.buildHeap(); // just in case we use an insane number of inputs

                int lasta = 0;
                if (H.isEmpty())
                        return;
                mainloop: while (true) { // goes until no more active inputs
                        final int a = H.peek().endOfRun();
                        // I suppose we have a run of length a - lasta here.
                        f.dispatch(out, lasta, a);
                        lasta = a;

                        while (H.peek().endOfRun() == a) {
                                final EWAHPointer p = H.peek();
                                p.parseNextRun();
                                p.callbackUpdate(f);
                                if (p.hasNoData()) {
                                        H.poll(); // we just remove it
                                        if (H.isEmpty())
                                                break mainloop;
                                } else {
                                        H.percolateDown(1); // since we have
                                                            // increased the key
                                }
                        }
                }
        }

}
