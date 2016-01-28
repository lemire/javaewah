package com.googlecode.javaewah.synth;


/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * This class will generate lists of random integers with a "clustered"
 * distribution. Reference: Anh VN, Moffat A. Index compression using 64-bit
 * words. Software: Practice and Experience 2010; 40(2):131-147.
 *
 * @author Daniel Lemire
 */
public class ClusteredDataGenerator {

    /**
     *
     */
    public ClusteredDataGenerator() {
        this.unidg = new UniformDataGenerator();
    }

    /**
     * @param seed random seed
     */
    public ClusteredDataGenerator(final int seed) {
        this.unidg = new UniformDataGenerator(seed);
    }

    /**
     * generates randomly N distinct integers from 0 to Max.
     *
     * @param N   number of integers
     * @param Max maximum integer value
     * @return a randomly generated array
     */
    public int[] generateClustered(int N, int Max) {
        int[] array = new int[N];
        fillClustered(array, 0, N, 0, Max);
        return array;
    }

    void fillClustered(int[] array, int offset, int length, int Min, int Max) {
        final int range = Max - Min;
        if ((range == length) || (length <= 10)) {
            fillUniform(array, offset, length, Min, Max);
            return;
        }
        final int cut = length
                / 2
                + ((range - length - 1 > 0) ? this.unidg.rand
                .nextInt(range - length - 1) : 0);
        final double p = this.unidg.rand.nextDouble();
        if (p < 0.25) {
            fillUniform(array, offset, length / 2, Min, Min + cut);
            fillClustered(array, offset + length / 2, length
                    - length / 2, Min + cut, Max);
        } else if (p < 0.5) {
            fillClustered(array, offset, length / 2, Min, Min + cut);
            fillUniform(array, offset + length / 2, length - length
                    / 2, Min + cut, Max);
        } else {
            fillClustered(array, offset, length / 2, Min, Min + cut);
            fillClustered(array, offset + length / 2, length
                    - length / 2, Min + cut, Max);
        }
    }

    void fillUniform(int[] array, int offset, int length, int Min, int Max) {
        int[] v = this.unidg.generateUniform(length, Max - Min);
        for (int k = 0; k < v.length; ++k)
            array[k + offset] = Min + v[k];
    }

    private final UniformDataGenerator unidg;
}
