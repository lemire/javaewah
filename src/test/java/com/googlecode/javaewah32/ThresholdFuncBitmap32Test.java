package com.googlecode.javaewah32;

import static com.googlecode.javaewah32.EWAHCompressedBitmap32.maxSizeInBits;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
/**
 * @since 0.8.2
 * @author Daniel Lemire
 */
public class ThresholdFuncBitmap32Test {
    @Test
    public void basictest() {
        System.out.println("Testing ThresholdFuncBitmap");
        EWAHCompressedBitmap32 ewah1 = EWAHCompressedBitmap32.bitmapOf(1,
                53, 110, 1000, 1201, 50000);
        EWAHCompressedBitmap32 ewah2 = EWAHCompressedBitmap32.bitmapOf(1,
                100, 1000, 1100, 1200, 31416, 50001);
        EWAHCompressedBitmap32 ewah3 = EWAHCompressedBitmap32.bitmapOf(1,
                110, 1000, 1101, 1200, 1201, 31416, 31417);

        Assert.assertTrue(EWAHCompressedBitmap32.threshold(1, ewah1)
                .equals(ewah1));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(1, ewah2)
                .equals(ewah2));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(1, ewah3)
                .equals(ewah3));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah1,
                ewah1).equals(ewah1));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah2,
                ewah2).equals(ewah2));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah3,
                ewah3).equals(ewah3));

        EWAHCompressedBitmap32 zero = new EWAHCompressedBitmap32();
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah1)
                .equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah2)
                .equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(2, ewah3)
                .equals(zero));
        Assert.assertTrue(EWAHCompressedBitmap32.threshold(4, ewah1,
                ewah2, ewah3).equals(zero));

        EWAHCompressedBitmap32 ewahorth = EWAHCompressedBitmap32.threshold(
                1, ewah1, ewah2, ewah3);
        EWAHCompressedBitmap32 ewahtrueor = EWAHCompressedBitmap32.or(
                ewah1, ewah2, ewah3);
        Assert.assertTrue(ewahorth.equals(ewahtrueor));

        EWAHCompressedBitmap32 ewahandth = EWAHCompressedBitmap32
                .threshold(3, ewah1, ewah2, ewah3);
        ewahandth.setSizeInBitsWithinLastWord(maxSizeInBits(ewah1, ewah2, ewah3));
        EWAHCompressedBitmap32 ewahtrueand = EWAHCompressedBitmap32.and(
                ewah1, ewah2, ewah3);
        Assert.assertTrue(ewahandth.equals(ewahtrueand));

        EWAHCompressedBitmap32 ewahmajth = EWAHCompressedBitmap32
                .threshold(2, ewah1, ewah2, ewah3);
        ewahmajth.setSizeInBitsWithinLastWord(maxSizeInBits(ewah1, ewah2, ewah3));
        EWAHCompressedBitmap32 ewahtruemaj = EWAHCompressedBitmap32.or(
                ewah1.and(ewah2), ewah1.and(ewah3), ewah2.and(ewah3));
        Assert.assertTrue(ewahmajth.equals(ewahtruemaj));
    }

}
