package com.googlecode.javaewah32;

/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc. and Veronika Zenz
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * BitCounter is a fake bitset data structure. Instead of storing the actual
 * data, it only records the number of set bits.
 * 
 * @since 0.5.0
 * @author Daniel Lemire and David McIntosh
 */

public final class BitCounter32 implements BitmapStorage32 {

        /**
         * Virtually add words directly to the bitmap
         * 
         * @param newdata
         *                the word
         */
        // @Override : causes problems with Java 1.5
        @Override
        public void add(final int newdata) {
                this.oneBits += Integer.bitCount(newdata);
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
        // @Override : causes problems with Java 1.5
        @Override
        public void addStreamOfLiteralWords(int[] data, int start, int number) {
                for (int i = start; i < start + number; i++) {
                        add(data[i]);
                }
        }

        /**
         * virtually add many zeroes or ones.
         * 
         * @param v
         *                zeros or ones
         * @param number
         *                how many to words add
         */
        // @Override : causes problems with Java 1.5
        @Override
        public void addStreamOfEmptyWords(boolean v, int number) {
                if (v) {
                        this.oneBits += number
                                * EWAHCompressedBitmap32.wordinbits;
                }
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
        public void addStreamOfNegatedLiteralWords(int[] data, int start,
                int number) {
                for (int i = start; i < start + number; i++) {
                        add(~data[i]);
                }
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
