package com.googlecode.javaewah.benchmark;


/*
 * Copyright 2009-2013, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc. and Veronika Zenz
 * Licensed under APL 2.0.
 */


import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;


/**
 * This class will generate "clustered" lists of random integers. That is, the
 * integers tend not to be randomly distributed.
 * 
 * @author Daniel Lemire
 */
public class UniformDataGenerator {
        /**
         * construct generator of random arrays.
         */
        public UniformDataGenerator(){
        }
    /**
    * generates randomly N distinct integers from 0 to Max.
    */
    int[] generateUniform(int N, int Max) {
        if (N > Max)
            throw new RuntimeException("not possible");
        int[] ans = new int[N];
        if (N == Max) {
            for (int k = 0; k < N; ++k)
                ans[k]=k;
            return ans;
        }
        HashSet<Integer> s = new HashSet<Integer>();
        while (s.size() < N)
            s.add(new Integer(this.rand.nextInt(Max)));
        Iterator<Integer> i = s.iterator();
        for (int k = 0; k < N; ++k)
            ans[k]=i.next().intValue();
        Arrays.sort(ans);
        return ans;
    }

    Random rand = new Random();

    
}
