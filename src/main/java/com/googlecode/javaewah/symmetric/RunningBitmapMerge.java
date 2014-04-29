package com.googlecode.javaewah.symmetric;

import com.googlecode.javaewah.BitmapStorage;
import com.googlecode.javaewah.EWAHCompressedBitmap;
import com.googlecode.javaewah.IteratingBufferedRunningLengthWord;
import com.googlecode.javaewah.datastructure.PriorityQ;

import java.util.Comparator;

/**
 * This is an implementation of the RunningBitmapMerge algorithm running on top
 * of JavaEWAH. It is well suited to computing symmetric Boolean queries.
 * 
 * It is a revised version of an algorithm described in the following reference:
 * <ul><li>
 * Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned
 * bitmap indexes. Data &amp; Knowledge Engineering 69 (1), pages 3-28, 2010.
 * </li></ul>
 *
 * @author Daniel Lemire
 * @since 0.8.0
 */
public class RunningBitmapMerge implements BitmapSymmetricAlgorithm {

    @Override
    public void symmetric(UpdateableBitmapFunction f, BitmapStorage out,
                          EWAHCompressedBitmap... set) {
        out.clear();
        final PriorityQ<EWAHPointer> h = new PriorityQ<EWAHPointer>(
                set.length, new Comparator<EWAHPointer>() {
            @Override
            public int compare(EWAHPointer arg0,
                               EWAHPointer arg1) {
                return arg0.compareTo(arg1);
            }
        }
        );
        f.resize(set.length);

        for (int k = 0; k < set.length; ++k) {
            final EWAHPointer x = new EWAHPointer(0, new IteratingBufferedRunningLengthWord(set[k]), k);
            if (x.hasNoData())
                continue;
            f.rw[k] = x;
            x.callbackUpdate(f);
            h.toss(x);
        }
        h.buildHeap(); // just in case we use an insane number of inputs

        int lasta = 0;
        if (h.isEmpty())
            return;
        mainloop:
        while (true) { // goes until no more active inputs
            final int a = h.peek().endOfRun();
            // I suppose we have a run of length a - lasta here.
            f.dispatch(out, lasta, a);
            lasta = a;

            while (h.peek().endOfRun() == a) {
                final EWAHPointer p = h.peek();
                p.parseNextRun();
                p.callbackUpdate(f);
                if (p.hasNoData()) {
                    h.poll(); // we just remove it
                    if (h.isEmpty())
                        break mainloop;
                } else {
                    h.percolateDown(); // since we have
                    // increased the key
                }
            }
        }
    }

}
