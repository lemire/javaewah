package com.googlecode.javaewah.symmetric;

import java.util.Comparator;
import com.googlecode.javaewah.*;
import com.googlecode.javaewah.datastructure.PriorityQ;

/**
 * This is an implementation of the RunningBitmapMerge algorithm running on top
 * of JavaEWAH. It is well suited to computing symmetric Boolean queries.
 * 
 * It is a revised version of an algorithm described in the following reference:
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 * 
 * @since 0.8.0
 * @author Daniel Lemire
 */
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
                                        H.percolateDown(); // since we have
                                                           // increased the key
                                }
                        }
                }
        }

}
