package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Fast algorithms to aggregate many bitmaps. These algorithms are just given as
 * reference. They may not be faster than the corresponding methods in the
 * EWAHCompressedBitmap class.
 *
 * @author Daniel Lemire
 */
public final class FastAggregation {

    /** Private constructor to prevent instantiation */
    private FastAggregation() {}

    /**
     * Compute the and aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words (per input bitmap)
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap bufferedand(final int bufSize,
                                                   final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
        bufferedandWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the and aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words (per input bitmap)
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedandWithContainer(
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {

        java.util.LinkedList<IteratingBufferedRunningLengthWord> al = new java.util.LinkedList<IteratingBufferedRunningLengthWord>();
        for (EWAHCompressedBitmap bitmap : bitmaps) {
            al.add(new IteratingBufferedRunningLengthWord(bitmap));
        }

        long[] hardbitmap = new long[bufSize * bitmaps.length];

        for (IteratingRLW i : al)
            if (i.size() == 0) {
                al.clear();
                break;
            }

        while (!al.isEmpty()) {
            Arrays.fill(hardbitmap, ~0l);
            long effective = Integer.MAX_VALUE;
            for (IteratingRLW i : al) {
                int eff = IteratorAggregation.inplaceand(
                        hardbitmap, i);
                if (eff < effective)
                    effective = eff;
            }
            for (int k = 0; k < effective; ++k)
                container.addWord(hardbitmap[k]);
            for (IteratingRLW i : al)
                if (i.size() == 0) {
                    al.clear();
                    break;
                }
        }
    }

    /**
     * Compute the or aggregate using a temporary uncompressed bitmap.
     *
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words
     * @return the or aggregate.
     */
    public static EWAHCompressedBitmap bufferedor(final int bufSize,
                                                  final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
        bufferedorWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the or aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedorWithContainer(
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {
        int range = 0;
        EWAHCompressedBitmap[] sbitmaps = bitmaps.clone();
        Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap>() {
            @Override
            public int compare(EWAHCompressedBitmap a,
                               EWAHCompressedBitmap b) {
                return b.sizeInBits - a.sizeInBits;
            }
        });

        java.util.ArrayList<IteratingBufferedRunningLengthWord> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord>();
        for (EWAHCompressedBitmap bitmap : sbitmaps) {
            if (bitmap.sizeInBits > range)
                range = bitmap.sizeInBits;
            al.add(new IteratingBufferedRunningLengthWord(bitmap));
        }
        long[] hardbitmap = new long[bufSize];
        int maxr = al.size();
        while (maxr > 0) {
            long effective = 0;
            for (int k = 0; k < maxr; ++k) {
                if (al.get(k).size() > 0) {
                    int eff = IteratorAggregation
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
     * @param bitmaps the source bitmaps
     * @param bufSize buffer size used during the computation in 64-bit
     *                words
     * @return the xor aggregate.
     */
    public static EWAHCompressedBitmap bufferedxor(final int bufSize,
                                                   final EWAHCompressedBitmap... bitmaps) {
        EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
        bufferedxorWithContainer(answer, bufSize, bitmaps);
        return answer;
    }

    /**
     * Compute the xor aggregate using a temporary uncompressed bitmap.
     *
     * @param container where the aggregate is written
     * @param bufSize   buffer size used during the computation in 64-bit
     *                  words
     * @param bitmaps   the source bitmaps
     */
    public static void bufferedxorWithContainer(
            final BitmapStorage container, final int bufSize,
            final EWAHCompressedBitmap... bitmaps) {
        int range = 0;
        EWAHCompressedBitmap[] sbitmaps = bitmaps.clone();
        Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap>() {
            @Override
            public int compare(EWAHCompressedBitmap a,
                               EWAHCompressedBitmap b) {
                return b.sizeInBits - a.sizeInBits;
            }
        });

        java.util.ArrayList<IteratingBufferedRunningLengthWord> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord>();
        for (EWAHCompressedBitmap bitmap : sbitmaps) {
            if (bitmap.sizeInBits > range)
                range = bitmap.sizeInBits;
            al.add(new IteratingBufferedRunningLengthWord(bitmap));
        }
        long[] hardbitmap = new long[bufSize];
        int maxr = al.size();
        while (maxr > 0) {
            long effective = 0;
            for (int k = 0; k < maxr; ++k) {
                if (al.get(k).size() > 0) {
                    int eff = IteratorAggregation.inplacexor(hardbitmap, al.get(k));
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
     * @param <T>     a class extending LogicalElement (like a compressed
     *                bitmap)
     * @param bitmaps bitmaps to be aggregated
     * @return the or aggregate
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends LogicalElement> T or(T... bitmaps) {
        PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
                new Comparator<T>() {
                    @Override
                    public int compare(T a, T b) {
                        return a.sizeInBytes()
                                - b.sizeInBytes();
                    }
                }
        );
        Collections.addAll(pq, bitmaps);
        while (pq.size() > 1) {
            T x1 = pq.poll();
            T x2 = pq.poll();
            pq.add((T) x1.or(x2));
        }
        return pq.poll();
    }

    /**
     * Uses a priority queue to compute the or aggregate.
     * 
     * The content of the container is overwritten.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void orToContainer(final BitmapStorage container,
                                     final EWAHCompressedBitmap... bitmaps) {
        if (bitmaps.length < 2)
            throw new IllegalArgumentException(
                    "We need at least two bitmaps");
        PriorityQueue<EWAHCompressedBitmap> pq = new PriorityQueue<EWAHCompressedBitmap>(
                bitmaps.length, new Comparator<EWAHCompressedBitmap>() {
            @Override
            public int compare(EWAHCompressedBitmap a,
                               EWAHCompressedBitmap b) {
                return a.sizeInBytes()
                        - b.sizeInBytes();
            }
        }
        );
        Collections.addAll(pq, bitmaps);
        while (pq.size() > 2) {
            EWAHCompressedBitmap x1 = pq.poll();
            EWAHCompressedBitmap x2 = pq.poll();
            pq.add(x1.or(x2));
        }
        pq.poll().orToContainer(pq.poll(), container);
    }

    /**
     * Uses a priority queue to compute the xor aggregate.
     *
     * @param <T>     a class extending LogicalElement (like a compressed
     *                bitmap)
     * @param bitmaps bitmaps to be aggregated
     * @return the xor aggregate
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends LogicalElement> T xor(T... bitmaps) {
        PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
                new Comparator<T>() {

                    @Override
                    public int compare(T a, T b) {
                        return a.sizeInBytes()
                                - b.sizeInBytes();
                    }
                }
        );
        Collections.addAll(pq, bitmaps);
        while (pq.size() > 1) {
            T x1 = pq.poll();
            T x2 = pq.poll();
            pq.add((T) x1.xor(x2));
        }
        return pq.poll();
    }

    /**
     * Uses a priority queue to compute the xor aggregate.
     * 
     * The content of the container is overwritten.
     *
     * @param container where we write the result
     * @param bitmaps   to be aggregated
     */
    public static void xorToContainer(final BitmapStorage container,
                                      final EWAHCompressedBitmap... bitmaps) {
        if (bitmaps.length < 2)
            throw new IllegalArgumentException(
                    "We need at least two bitmaps");
        PriorityQueue<EWAHCompressedBitmap> pq = new PriorityQueue<EWAHCompressedBitmap>(
                bitmaps.length, new Comparator<EWAHCompressedBitmap>() {
            @Override
            public int compare(EWAHCompressedBitmap a,
                               EWAHCompressedBitmap b) {
                return a.sizeInBytes()
                        - b.sizeInBytes();
            }
        }
        );
        Collections.addAll(pq, bitmaps);
        while (pq.size() > 2) {
            EWAHCompressedBitmap x1 = pq.poll();
            EWAHCompressedBitmap x2 = pq.poll();
            pq.add(x1.xor(x2));
        }
        pq.poll().xorToContainer(pq.poll(), container);
    }

    /**
     * For internal use. Computes the bitwise or of the provided bitmaps and
     * stores the result in the container. (This used to be the default.)
     *
     * @param container where store the result
     * @param bitmaps   to be aggregated
     * @since 0.4.0
     * @deprecated use EWAHCompressedBitmap.or instead
     */
    @Deprecated
    public static void legacy_orWithContainer(
            final BitmapStorage container,
            final EWAHCompressedBitmap... bitmaps) {
        if (bitmaps.length == 2) {
            // should be more efficient
            bitmaps[0].orToContainer(bitmaps[1], container);
            return;
        }

        // Sort the bitmaps in descending order by sizeInBits. We will
        // exhaust the
        // sorted bitmaps from right to left.
        final EWAHCompressedBitmap[] sortedBitmaps = bitmaps.clone();
        Arrays.sort(sortedBitmaps,
                new Comparator<EWAHCompressedBitmap>() {
                    @Override
                    public int compare(EWAHCompressedBitmap a,
                                       EWAHCompressedBitmap b) {
                        return a.sizeInBits < b.sizeInBits ? 1 : a.sizeInBits == b.sizeInBits ? 0 : -1;
                    }
                }
        );

        final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
        int maxAvailablePos = 0;
        for (EWAHCompressedBitmap bitmap : sortedBitmaps) {
            EWAHIterator iterator = bitmap.getEWAHIterator();
            if (iterator.hasNext()) {
                rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord(iterator);
            }
        }

        if (maxAvailablePos == 0) { // this never happens...
            container.setSizeInBits(0);
            return;
        }

        int maxSize = sortedBitmaps[0].sizeInBits;

        while (true) {
            long maxOneRl = 0;
            long minZeroRl = Long.MAX_VALUE;
            long minSize = Long.MAX_VALUE;
            int numEmptyRl = 0;
            for (int i = 0; i < maxAvailablePos; i++) {
                IteratingBufferedRunningLengthWord rlw = rlws[i];
                long size = rlw.size();
                if (size == 0) {
                    maxAvailablePos = i;
                    break;
                }
                minSize = Math.min(minSize, size);

                if (rlw.getRunningBit()) {
                    long rl = rlw.getRunningLength();
                    maxOneRl = Math.max(maxOneRl, rl);
                    minZeroRl = 0;
                    if (rl == 0 && size > 0) {
                        numEmptyRl++;
                    }
                } else {
                    long rl = rlw.getRunningLength();
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
                    IteratingBufferedRunningLengthWord rlw = rlws[i];
                    rlw.discardFirstWords(maxOneRl);
                }
            } else if (minZeroRl > 0) {
                container.addStreamOfEmptyWords(false,
                        minZeroRl);
                for (int i = 0; i < maxAvailablePos; i++) {
                    IteratingBufferedRunningLengthWord rlw = rlws[i];
                    rlw.discardFirstWords(minZeroRl);
                }
            } else {
                int index = 0;

                if (numEmptyRl == 1) {
                    // if one rlw has literal words to
                    // process and the rest have a run of
                    // 0's we can write them out here
                    IteratingBufferedRunningLengthWord emptyRl = null;
                    long minNonEmptyRl = Long.MAX_VALUE;
                    for (int i = 0; i < maxAvailablePos; i++) {
                        IteratingBufferedRunningLengthWord rlw = rlws[i];
                        long rl = rlw
                                .getRunningLength();
                        if (rl == 0) {
                            assert emptyRl == null;
                            emptyRl = rlw;
                        } else {
                            minNonEmptyRl = Math
                                    .min(minNonEmptyRl,
                                            rl);
                        }
                    }
                    long wordsToWrite = minNonEmptyRl > minSize ? minSize
                            : minNonEmptyRl;
                    if (emptyRl != null)
                        emptyRl.writeLiteralWords(
                                (int) wordsToWrite,
                                container);
                    index += wordsToWrite;
                }

                while (index < minSize) {
                    long word = 0;
                    for (int i = 0; i < maxAvailablePos; i++) {
                        IteratingBufferedRunningLengthWord rlw = rlws[i];
                        if (rlw.getRunningLength() <= index) {
                            word |= rlw
                                    .getLiteralWordAt(index
                                            - (int) rlw
                                            .getRunningLength());
                        }
                    }
                    container.addWord(word);
                    index++;
                }
                for (int i = 0; i < maxAvailablePos; i++) {
                    IteratingBufferedRunningLengthWord rlw = rlws[i];
                    rlw.discardFirstWords(minSize);
                }
            }
        }
        container.setSizeInBits(maxSize);
    }

}
