package com.googlecode.javaewah32.symmetric;

import java.util.Comparator;
import com.googlecode.javaewah.datastructure.PriorityQ;
import com.googlecode.javaewah32.BitmapStorage32;
import com.googlecode.javaewah32.EWAHCompressedBitmap32;
import com.googlecode.javaewah32.IteratingBufferedRunningLengthWord32;

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
 * @since 0.8.2
 * @author Daniel Lemire
 */
public class RunningBitmapMerge32 implements BitmapSymmetricAlgorithm32 {

        @Override
        public void symmetric(UpdateableBitmapFunction32 f, BitmapStorage32 out,
                EWAHCompressedBitmap32... set) {
                out.clear();
                final PriorityQ<EWAHPointer32> H = new PriorityQ<EWAHPointer32>(
                        set.length, new Comparator<EWAHPointer32>() {
                                @Override
                                public int compare(EWAHPointer32 arg0,
                                        EWAHPointer32 arg1) {
                                        return arg0.compareTo(arg1);
                                }
                        });
                f.resize(set.length);

                for (int k = 0; k < set.length; ++k) {
                        final EWAHPointer32 x = new EWAHPointer32(0,
                                new IteratingBufferedRunningLengthWord32(set[k]),
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
                                final EWAHPointer32 p = H.peek();
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
