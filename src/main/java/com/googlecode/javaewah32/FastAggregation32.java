package com.googlecode.javaewah32;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Fast algorithms to aggregate many bitmaps. These algorithms are just given as
 * reference. They may not be faster than the corresponding methods in the
 * EWAHCompressedBitmap class.
 * 
 * @author Daniel Lemire
 * 
 */
public class FastAggregation32 {

        /**
         * Compute the and aggregate using a temporary uncompressed bitmap.
         * 
         * @param bitmaps
         *                the source bitmaps
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words (per input bitmap)
         * @return the or aggregate.
         */
        public static EWAHCompressedBitmap32 bufferedand(final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {
                EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
                bufferedandWithContainer(answer, bufsize, bitmaps);
                return answer;
        }

        /**
         * Compute the and aggregate using a temporary uncompressed bitmap.
         * 
         * @param container
         *                where the aggregate is written
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words (per input bitmap)
         * @param bitmaps
         *                the source bitmaps
         */
        public static void bufferedandWithContainer(
                final BitmapStorage32 container, final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {

                java.util.LinkedList<IteratingBufferedRunningLengthWord32> al = new java.util.LinkedList<IteratingBufferedRunningLengthWord32>();
                for (EWAHCompressedBitmap32 bitmap : bitmaps) {
                        al.add(new IteratingBufferedRunningLengthWord32(bitmap));
                }
                int[] hardbitmap = new int[bufsize * bitmaps.length];

                for (IteratingRLW32 i : al)
                        if (i.size() == 0) {
                                al.clear();
                                break;
                        }

                while (!al.isEmpty()) {
                        Arrays.fill(hardbitmap, ~0);
                        int effective = Integer.MAX_VALUE;
                        for (IteratingRLW32 i : al) {
                                int eff = IteratorAggregation32.inplaceand(
                                        hardbitmap, i);
                                if (eff < effective)
                                        effective = eff;
                        }
                        for (int k = 0; k < effective; ++k)
                                container.addWord(hardbitmap[k]);
                        for (IteratingRLW32 i : al)
                                if (i.size() == 0) {
                                        al.clear();
                                        break;
                                }
                }
        }

        /**
         * Compute the or aggregate using a temporary uncompressed bitmap.
         * 
         * @param bitmaps
         *                the source bitmaps
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words
         * @return the or aggregate.
         */
        public static EWAHCompressedBitmap32 bufferedor(final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {
                EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
                bufferedorWithContainer(answer, bufsize, bitmaps);
                return answer;
        }

        /**
         * Compute the or aggregate using a temporary uncompressed bitmap.
         * 
         * @param container
         *                where the aggregate is written
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words
         * @param bitmaps
         *                the source bitmaps
         */
        public static void bufferedorWithContainer(
                final BitmapStorage32 container, final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {
                int range = 0;
                EWAHCompressedBitmap32[] sbitmaps = bitmaps.clone();
                Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap32>() {
                        @Override
                        public int compare(EWAHCompressedBitmap32 a,
                                EWAHCompressedBitmap32 b) {
                                return b.sizeinbits - a.sizeinbits;
                        }
                });

                java.util.ArrayList<IteratingBufferedRunningLengthWord32> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord32>();
                for (EWAHCompressedBitmap32 bitmap : sbitmaps) {
                        if (bitmap.sizeinbits > range)
                                range = bitmap.sizeinbits;
                        al.add(new IteratingBufferedRunningLengthWord32(bitmap));
                }
                int[] hardbitmap = new int[bufsize];
                int maxr = al.size();
                while (maxr > 0) {
                        int effective = 0;
                        for (int k = 0; k < maxr; ++k) {
                                if (al.get(k).size() > 0) {
                                        int eff = IteratorAggregation32
                                                .inplaceor(hardbitmap,
                                                        al.get(k));
                                        if (eff > effective)
                                                effective = eff;
                                } else
                                        maxr = k;
                        }
                        for (int k = 0; k < effective; ++k)
                                container.addWord(hardbitmap[k]);
                        Arrays.fill(hardbitmap, 0);

                }
                container.setSizeInBits(range);
        }

        /**
         * Compute the xor aggregate using a temporary uncompressed bitmap.
         * 
         * @param bitmaps
         *                the source bitmaps
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words
         * @return the xor aggregate.
         */
        public static EWAHCompressedBitmap32 bufferedxor(final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {
                EWAHCompressedBitmap32 answer = new EWAHCompressedBitmap32();
                bufferedxorWithContainer(answer, bufsize, bitmaps);
                return answer;
        }

        /**
         * Compute the xor aggregate using a temporary uncompressed bitmap.
         * 
         * @param container
         *                where the aggregate is written
         * @param bufsize
         *                buffer size used during the computation in 64-bit
         *                words
         * @param bitmaps
         *                the source bitmaps
         */
        public static void bufferedxorWithContainer(
                final BitmapStorage32 container, final int bufsize,
                final EWAHCompressedBitmap32... bitmaps) {
                int range = 0;
                EWAHCompressedBitmap32[] sbitmaps = bitmaps.clone();
                Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap32>() {
                        @Override
                        public int compare(EWAHCompressedBitmap32 a,
                                EWAHCompressedBitmap32 b) {
                                return b.sizeinbits - a.sizeinbits;
                        }
                });

                java.util.ArrayList<IteratingBufferedRunningLengthWord32> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord32>();
                for (EWAHCompressedBitmap32 bitmap : sbitmaps) {
                        if (bitmap.sizeinbits > range)
                                range = bitmap.sizeinbits;
                        al.add(new IteratingBufferedRunningLengthWord32(bitmap));
                }
                int[] hardbitmap = new int[bufsize];
                int maxr = al.size();
                while (maxr > 0) {
                        int effective = 0;
                        for (int k = 0; k < maxr; ++k) {
                                if (al.get(k).size() > 0) {
                                        int eff = IteratorAggregation32
                                                .inplacexor(hardbitmap,
                                                        al.get(k));
                                        if (eff > effective)
                                                effective = eff;
                                } else
                                        maxr = k;
                        }
                        for (int k = 0; k < effective; ++k)
                                container.addWord(hardbitmap[k]);
                        Arrays.fill(hardbitmap, 0);
                }
                container.setSizeInBits(range);
        }

        /**
         * Uses a priority queue to compute the or aggregate.
         * 
         * @param container
         *                where we write the result
         * @param bitmaps
         *                to be aggregated
         */
        public static void orToContainer(final BitmapStorage32 container,
                final EWAHCompressedBitmap32... bitmaps) {
                if (bitmaps.length < 2)
                        throw new IllegalArgumentException(
                                "We need at least two bitmaps");
                PriorityQueue<EWAHCompressedBitmap32> pq = new PriorityQueue<EWAHCompressedBitmap32>(
                        bitmaps.length,
                        new Comparator<EWAHCompressedBitmap32>() {
                                @Override
                                public int compare(EWAHCompressedBitmap32 a,
                                        EWAHCompressedBitmap32 b) {
                                        return a.sizeInBytes()
                                                - b.sizeInBytes();
                                }
                        });
                for (EWAHCompressedBitmap32 x : bitmaps) {
                        pq.add(x);
                }
                while (pq.size() > 2) {
                        EWAHCompressedBitmap32 x1 = pq.poll();
                        EWAHCompressedBitmap32 x2 = pq.poll();
                        pq.add(x1.or(x2));
                }
                pq.poll().orToContainer(pq.poll(), container);
        }

        /**
         * Uses a priority queue to compute the xor aggregate.
         * 
         * @param container
         *                where we write the result
         * @param bitmaps
         *                to be aggregated
         */
        public static void xorToContainer(final BitmapStorage32 container,
                final EWAHCompressedBitmap32... bitmaps) {
                if (bitmaps.length < 2)
                        throw new IllegalArgumentException(
                                "We need at least two bitmaps");
                PriorityQueue<EWAHCompressedBitmap32> pq = new PriorityQueue<EWAHCompressedBitmap32>(
                        bitmaps.length,
                        new Comparator<EWAHCompressedBitmap32>() {
                                @Override
                                public int compare(EWAHCompressedBitmap32 a,
                                        EWAHCompressedBitmap32 b) {
                                        return a.sizeInBytes()
                                                - b.sizeInBytes();
                                }
                        });
                for (EWAHCompressedBitmap32 x : bitmaps) {
                        pq.add(x);
                }
                while (pq.size() > 2) {
                        EWAHCompressedBitmap32 x1 = pq.poll();
                        EWAHCompressedBitmap32 x2 = pq.poll();
                        pq.add(x1.xor(x2));
                }
                pq.poll().xorToContainer(pq.poll(), container);
        }

        /**
         * For internal use. Computes the bitwise or of the provided bitmaps and
         * stores the result in the container. (This used to be the default.)
         * 
         * @deprecated use EWAHCompressedBitmap32.or instead
         * @since 0.4.0
         * @param container
         *                where store the result
         * @param bitmaps
         *                to be aggregated
         */
        @Deprecated
        public static void legacy_orWithContainer(
                final BitmapStorage32 container,
                final EWAHCompressedBitmap32... bitmaps) {
                if (bitmaps.length == 2) {
                        // should be more efficient
                        bitmaps[0].orToContainer(bitmaps[1], container);
                        return;
                }

                // Sort the bitmaps in descending order by sizeinbits. We will
                // exhaust the
                // sorted bitmaps from right to left.
                final EWAHCompressedBitmap32[] sortedBitmaps = bitmaps.clone();
                Arrays.sort(sortedBitmaps,
                        new Comparator<EWAHCompressedBitmap32>() {
                                @Override
                                public int compare(EWAHCompressedBitmap32 a,
                                        EWAHCompressedBitmap32 b) {
                                        return a.sizeinbits < b.sizeinbits ? 1
                                                : a.sizeinbits == b.sizeinbits ? 0
                                                        : -1;
                                }
                        });

                final IteratingBufferedRunningLengthWord32[] rlws = new IteratingBufferedRunningLengthWord32[bitmaps.length];
                int maxAvailablePos = 0;
                for (EWAHCompressedBitmap32 bitmap : sortedBitmaps) {
                        EWAHIterator32 iterator = bitmap.getEWAHIterator();
                        if (iterator.hasNext()) {
                                rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord32(
                                        iterator);
                        }
                }

                if (maxAvailablePos == 0) { // this never happens...
                        container.setSizeInBits(0);
                        return;
                }

                int maxSize = sortedBitmaps[0].sizeinbits;

                while (true) {
                        int maxOneRl = 0;
                        int minZeroRl = Integer.MAX_VALUE;
                        int minSize = Integer.MAX_VALUE;
                        int numEmptyRl = 0;
                        for (int i = 0; i < maxAvailablePos; i++) {
                                IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                int size = rlw.size();
                                if (size == 0) {
                                        maxAvailablePos = i;
                                        break;
                                }
                                minSize = Math.min(minSize, size);

                                if (rlw.getRunningBit()) {
                                        int rl = rlw.getRunningLength();
                                        maxOneRl = Math.max(maxOneRl, rl);
                                        minZeroRl = 0;
                                        if (rl == 0 && size > 0) {
                                                numEmptyRl++;
                                        }
                                } else {
                                        int rl = rlw.getRunningLength();
                                        minZeroRl = Math.min(minZeroRl, rl);
                                        if (rl == 0 && size > 0) {
                                                numEmptyRl++;
                                        }
                                }
                        }

                        if (maxAvailablePos == 0) {
                                break;
                        } else if (maxAvailablePos == 1) {
                                // only one bitmap is left so just write the
                                // rest of it out
                                rlws[0].discharge(container);
                                break;
                        }

                        if (maxOneRl > 0) {
                                container.addStreamOfEmptyWords(true, maxOneRl);
                                for (int i = 0; i < maxAvailablePos; i++) {
                                        IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                        rlw.discardFirstWords(maxOneRl);
                                }
                        } else if (minZeroRl > 0) {
                                container.addStreamOfEmptyWords(false,
                                        minZeroRl);
                                for (int i = 0; i < maxAvailablePos; i++) {
                                        IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                        rlw.discardFirstWords(minZeroRl);
                                }
                        } else {
                                int index = 0;

                                if (numEmptyRl == 1) {
                                        // if one rlw has literal words to
                                        // process and the rest have a run of
                                        // 0's we can write them out here
                                        IteratingBufferedRunningLengthWord32 emptyRl = null;
                                        int minNonEmptyRl = Integer.MAX_VALUE;
                                        for (int i = 0; i < maxAvailablePos; i++) {
                                                IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                                int rl = rlw.getRunningLength();
                                                if (rl == 0) {
                                                        assert emptyRl == null;
                                                        emptyRl = rlw;
                                                } else {
                                                        minNonEmptyRl = Math
                                                                .min(minNonEmptyRl,
                                                                        rl);
                                                }
                                        }
                                        int wordsToWrite = minNonEmptyRl > minSize ? minSize
                                                : minNonEmptyRl;
                                        if (emptyRl != null)
                                                emptyRl.writeLiteralWords(
                                                        wordsToWrite, container);
                                        index += wordsToWrite;
                                }

                                while (index < minSize) {
                                        int word = 0;
                                        for (int i = 0; i < maxAvailablePos; i++) {
                                                IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                                if (rlw.getRunningLength() <= index) {
                                                        word |= rlw
                                                                .getLiteralWordAt(index
                                                                        - rlw.getRunningLength());
                                                }
                                        }
                                        container.addWord(word);
                                        index++;
                                }
                                for (int i = 0; i < maxAvailablePos; i++) {
                                        IteratingBufferedRunningLengthWord32 rlw = rlws[i];
                                        rlw.discardFirstWords(minSize);
                                }
                        }
                }
                container.setSizeInBits(maxSize);
        }

}
