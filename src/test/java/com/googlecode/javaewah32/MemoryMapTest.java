package com.googlecode.javaewah32;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;


public class MemoryMapTest
{

	@Test
	public void basicTest() throws IOException, CloneNotSupportedException {
		EWAHCompressedBitmap32 ewahBitmap = EWAHCompressedBitmap32.bitmapOf(0, 2, 55,
				64, 1 << 30);
		EWAHCompressedBitmap32 newewahBitmap = ewahBitmap.clone();
		Assert.assertEquals(newewahBitmap, ewahBitmap);		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ewahBitmap.serialize(new DataOutputStream(bos));
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		EWAHCompressedBitmap32 mapped = new EWAHCompressedBitmap32(bb);
		Assert.assertEquals(mapped, ewahBitmap);
		EWAHCompressedBitmap32 newmapped;
		newmapped = mapped.clone();
		Assert.assertEquals(newmapped, ewahBitmap);		
	}
	
	@Test
	public void basicFileTest() throws IOException {
		File tmpfile = File.createTempFile("roaring", "bin");
		tmpfile.deleteOnExit();
		final FileOutputStream fos = new FileOutputStream(tmpfile);
		EWAHCompressedBitmap32 ewahBitmap = EWAHCompressedBitmap32.bitmapOf(0, 2, 55,
				64, 1 << 30);
		ewahBitmap.serialize(new DataOutputStream(fos));
		long totalcount = fos.getChannel().position();
		fos.close();
		RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
		ByteBuffer bb = memoryMappedFile.getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, totalcount);
		EWAHCompressedBitmap32 mapped = new EWAHCompressedBitmap32(bb);
		memoryMappedFile.close();
		Assert.assertEquals(mapped, ewahBitmap);
	}
}
