package com.googlecode.javaewah;

import junit.framework.Assert;

import org.junit.Test;
import com.googlecode.javaewah.symmetric.RunningBitmapMerge;
import com.googlecode.javaewah.symmetric.ThresholdFuncBitmap;

@SuppressWarnings("javadoc")

public class ThresholdFuncBitmapTest {
        @Test
        public void basictest() {
                EWAHCompressedBitmap ewah1 = EWAHCompressedBitmap.bitmapOf(1,53,110,1000, 1201,50000);
                EWAHCompressedBitmap ewah2 = EWAHCompressedBitmap.bitmapOf(1,100,1000,1100,1200,31416,50001);
                EWAHCompressedBitmap ewah3 =  EWAHCompressedBitmap.bitmapOf(1,110,1000,1101,1200, 1201,31416, 31417);
                EWAHCompressedBitmap ewahorth = new EWAHCompressedBitmap();
                (new RunningBitmapMerge()).symmetric(new ThresholdFuncBitmap(1), ewahorth, ewah1, ewah2, ewah3); 
                
                EWAHCompressedBitmap ewahtrueor = EWAHCompressedBitmap.or(ewah1,ewah2,ewah3);
                Assert.assertTrue(ewahorth.equals(ewahtrueor));
                        
                EWAHCompressedBitmap ewahandth = new EWAHCompressedBitmap();
                (new RunningBitmapMerge()).symmetric(new ThresholdFuncBitmap(3), ewahandth, ewah1, ewah2, ewah3); 
                EWAHCompressedBitmap ewahtrueand = EWAHCompressedBitmap.and(ewah1,ewah2,ewah3);
                Assert.assertTrue(ewahandth.equals(ewahtrueand));
                
                EWAHCompressedBitmap ewahmajth = new EWAHCompressedBitmap();
                (new RunningBitmapMerge()).symmetric(new ThresholdFuncBitmap(2), ewahmajth, ewah1, ewah2, ewah3); 
                EWAHCompressedBitmap ewahtruemaj = EWAHCompressedBitmap.or(ewah1.and(ewah2),ewah1.and(ewah3),ewah2.and(ewah3));
                Assert.assertTrue(ewahmajth.equals(ewahtruemaj));
        }

}
