package com.googlecode.javaewah;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;

public class MemoryMapTest
{
	

	@Test
	public void basicTest() throws IOException,CloneNotSupportedException {
		EWAHCompressedBitmap ewahBitmap = EWAHCompressedBitmap.bitmapOf(0, 2, 55,
				64, 1 << 30);
		EWAHCompressedBitmap newewahBitmap = ewahBitmap.clone();
		Assert.assertEquals(newewahBitmap, ewahBitmap);		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ewahBitmap.serialize(new DataOutputStream(bos));
		ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
		EWAHCompressedBitmap mapped = new EWAHCompressedBitmap(bb);
		Assert.assertEquals(mapped, ewahBitmap);
		EWAHCompressedBitmap newmapped;
		newmapped = mapped.clone();
		Assert.assertEquals(newmapped, ewahBitmap);		
	}
	
	@Test
	public void basicFileTest() throws IOException {
		File tmpfile = File.createTempFile("javaewah", "bin");
		tmpfile.deleteOnExit();
		final FileOutputStream fos = new FileOutputStream(tmpfile);
		EWAHCompressedBitmap ewahBitmap = EWAHCompressedBitmap.bitmapOf(0, 2, 55,
				64, 1 << 30);
		ewahBitmap.serialize(new DataOutputStream(fos));
		long totalcount = fos.getChannel().position();
		fos.close();
		RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
		ByteBuffer bb = memoryMappedFile.getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, totalcount);
		EWAHCompressedBitmap mapped = new EWAHCompressedBitmap(bb);
		memoryMappedFile.close();
		Assert.assertEquals(mapped, ewahBitmap);
	}
}
