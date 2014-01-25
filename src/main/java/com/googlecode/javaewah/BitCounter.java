package com.googlecode.javaewah;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz and Owen Kaser
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * BitCounter is a fake bitset data structure. Instead of storing the actual
 * data, it only records the number of set bits.
 * 
 * @since 0.4.0
 * @author David McIntosh
 */

public final class BitCounter implements BitmapStorage {

        /**
         * Virtually add words directly to the bitmap
         * 
         * @param newdata
         *                the word
         */
        @Override
        public void add(final long newdata) {
                this.oneBits += Long.bitCount(newdata);
                return;
        }

        /**
         * virtually add several literal words.
         * 
         * @param data
         *                the literal words
         * @param start
         *                the starting point in the array
         * @param number
         *                the number of literal words to add
         */
        @Override
        public void addStreamOfLiteralWords(long[] data, int start, int number) {
                for (int i = start; i < start + number; i++) {
                        add(data[i]);
                }
                return;
        }

        /**
         * virtually add many zeroes or ones.
         * 
         * @param v
         *                zeros or ones
         * @param number
         *                how many to words add
         */
        @Override
        public void addStreamOfEmptyWords(boolean v, long number) {
                if (v) {
                        this.oneBits += number
                                * EWAHCompressedBitmap.wordinbits;
                }
                return;
        }

        /**
         * virtually add several negated literal words.
         * 
         * @param data
         *                the literal words
         * @param start
         *                the starting point in the array
         * @param number
         *                the number of literal words to add
         */
        // @Override : causes problems with Java 1.5
        @Override
        public void addStreamOfNegatedLiteralWords(long[] data, int start,
                int number) {
                for (int i = start; i < start + number; i++) {
                        add(~data[i]);
                }
                return;
        }

        /**
         * As you act on this class, it records the number of set (true) bits.
         * 
         * @return number of set bits
         */
        public int getCount() {
                return this.oneBits;
        }

        /**
         * should directly set the sizeinbits field, but is effectively ignored
         * in this class.
         * 
         * @param bits
         *                number of bits
         */
        // @Override : causes problems with Java 1.5
        @Override
        public void setSizeInBits(int bits) {
                // no action
        }

        private int oneBits;

}
