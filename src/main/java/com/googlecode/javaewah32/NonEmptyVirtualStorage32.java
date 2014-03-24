package com.googlecode.javaewah32;

/*
 * Copyright 2009-2014, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, gssiyankai
 * Licensed under the Apache License, Version 2.0.
 */
/**
 * This is a BitmapStorage that can be used to determine quickly if the result
 * of an operation is non-trivial... that is, whether there will be at least on
 * set bit.
 * 
 * @since 0.5.0
 * @author Daniel Lemire and Veronika Zenz
 * 
 */
public class NonEmptyVirtualStorage32 implements BitmapStorage32 {
        static class NonEmptyException extends RuntimeException {
                private static final long serialVersionUID = 1L;

                /**
                 * Do not fill in the stack trace for this exception for
                 * performance reasons.
                 * 
                 * @return this instance
                 * @see java.lang.Throwable#fillInStackTrace()
                 */
                @Override
                public synchronized Throwable fillInStackTrace() {
                        return this;
                }
        }

        private static final NonEmptyException nonEmptyException = new NonEmptyException();

        /**
         * If the word to be added is non-zero, a NonEmptyException exception is
         * thrown.
         */
        @Override
        public void addWord(int newdata) {
                if (newdata != 0)
                        throw nonEmptyException;
        }

        /**
         * throws a NonEmptyException exception when number is greater than 0
         * 
         */
        @Override
        public void addStreamOfLiteralWords(int[] data, int start, int number) {
                if (number > 0) {
                        throw nonEmptyException;
                }
        }

        /**
         * If the boolean value is true and number is greater than 0, then it
         * throws a NonEmptyException exception, otherwise, nothing happens.
         * 
         */
        @Override
        public void addStreamOfEmptyWords(boolean v, int number) {
                if (v && (number > 0))
                        throw nonEmptyException;
        }

        /**
         * throws a NonEmptyException exception when number is greater than 0
         * 
         */
        @Override
        public void addStreamOfNegatedLiteralWords(int[] data, int start,
                int number) {
                if (number > 0) {
                        throw nonEmptyException;
                }
        }
        
        @Override
        public void clear() {
        }

        /**
         * Does nothing.
         * 
         * @see com.googlecode.javaewah.BitmapStorage#setSizeInBits(int)
         */
        @Override
        public void setSizeInBits(int bits) {
        }

}
