package com.googlecode.javaewah;

import static com.googlecode.javaewah.EWAHCompressedBitmap.maxSizeInBits;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
/**
 * @since 0.8.0
 * @author Daniel Lemire
 */
public class ThresholdFuncBitmapTest {
    @Test
    public void basictest() {
        System.out.println("Testing ThresholdFuncBitmap");
        EWAHCompressedBitmap ewah1 = EWAHCompressedBitmap.bitmapOf(1,
                53, 110, 1000, 1201, 50000);
        EWAHCompressedBitmap ewah2 = EWAHCompressedBitmap.bitmapOf(1,
                100, 1000, 1100, 1200, 31416, 50001);
        EWAHCompressedBitmap ewah3 = EWAHCompressedBitmap.bitmapOf(1,
                110, 1000, 1101, 1200, 1201, 31416, 31417);

        Assert.assertTrue(EWAHCompressedBitmap.threshold(1, ewah1)
                .equals(ewah1));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(1, ewah2)
                .equals(ewah2));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(1, ewah3)
                .equals(ewah3));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah1,
                ewah1).equals(ewah1));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah2,
                ewah2).equals(ewah2));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah3,
                ewah3).equals(ewah3));

        EWAHCompressedBitmap zero = new EWAHCompressedBitmap();
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah1).equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah2).equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(2, ewah3).equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap.threshold(4, ewah1,ewah2, ewah3).equals(zero));

        EWAHCompressedBitmap ewahorth = EWAHCompressedBitmap.threshold(
                1, ewah1, ewah2, ewah3);
        EWAHCompressedBitmap ewahtrueor = EWAHCompressedBitmap.or(
                ewah1, ewah2, ewah3);
        Assert.assertTrue(ewahorth.equals(ewahtrueor));

        EWAHCompressedBitmap ewahandth = EWAHCompressedBitmap
                .threshold(3, ewah1, ewah2, ewah3);
        ewahandth.setSizeInBitsWithinLastWord(maxSizeInBits(ewah1, ewah2, ewah3));
        EWAHCompressedBitmap ewahtrueand = EWAHCompressedBitmap.and(
                ewah1, ewah2, ewah3);
        Assert.assertTrue(ewahandth.equals(ewahtrueand));

        EWAHCompressedBitmap ewahmajth = EWAHCompressedBitmap
                .threshold(2, ewah1, ewah2, ewah3);
        ewahmajth.setSizeInBitsWithinLastWord(maxSizeInBits(ewah1, ewah2, ewah3));
        EWAHCompressedBitmap ewahtruemaj = EWAHCompressedBitmap.or(
                ewah1.and(ewah2), ewah1.and(ewah3), ewah2.and(ewah3));
        Assert.assertTrue(ewahmajth.equals(ewahtruemaj));
    }

}
