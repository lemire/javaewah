JavaEWAH
==========================================================
[![Build Status](https://travis-ci.org/lemire/javaewah.png)](https://travis-ci.org/lemire/javaewah)
[![][maven img]][maven]
[![][license img]][license]
[![docs-badge][]][docs]


(c) 2009-2015
Daniel Lemire (http://lemire.me/en/), 
Cliff Moon, 
David McIntosh (https://github.com/mctofu),
Robert Becho (https://github.com/RBecho),
Colby Ranger (https://github.com/crangeratgoogle),
Veronika Zenz (https://github.com/veronikazenz),
Owen Kaser (https://github.com/owenkaser),
Gregory Ssi-Yan-Kai (https://github.com/gssiyankai),
and Rory Graves (https://github.com/rorygraves)


This code is licensed under Apache License, Version 2.0 (ASL2.0).
(GPL 2.0 derivatives are allowed.)

This is a word-aligned compressed variant of
the Java Bitset class. We provide both a 64-bit 
and a 32-bit RLE-like compression scheme. It can
be used to implement bitmap indexes.

The goal of word-aligned compression is not to 
achieve the best compression, but rather to 
improve query processing time. Hence, we try
to save CPU cycles, maybe at the expense of
storage. However, the EWAH scheme we implemented
is always more efficient storage-wise than an
uncompressed bitmap (as implemented in the java
BitSet class by Sun).

JavaEWAH offers competitive speed. In an exhaustive 
comparison, Guzun et al. (ICDE 2014) found that "EWAH
offers the best query time for all distributions." 

JavaEWAH also supports memory-mapped files: we can
serialize the bitmaps to disk and then map them to
memory using the java.nio classes. This may avoid
wasteful serialization/deserialization routines.

The library also provides a drop-in replacement for
the standard BitSet class. Like the other bitmap classes
in JavaEWAH, this uncompressed BitSet class supports
memory-mapped files as well as many other conviences.

For better performance, use a 64-bit JVM over
64-bit CPUs when using the 64-bit scheme (javaewah.EWAHCompressedBitmap).
The 32-bit version (javaewah32.EWAHCompressedBitmap32) should
compress better but be comparatively slower.



Java 6 or better is required. We found the very latest OpenJDK release
offered the best performance.

Real-world usage
----------------

JavaEWAH is part of Apache Hive, Apache Spark and Eclipse JGit. It has been used in production systems for many years.

Data format
------------

For more details regarding the compression format, please
see Section 3 of the following paper:

Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned bitmap indexes. Data & Knowledge Engineering 69 (1), pages 3-28, 2010.  
 http://arxiv.org/abs/0901.3751
 
 (The PDF file is freely available on the arXiv site.)

Benchmark
---------


To run a simple benchmark, use the following command:

         $ mvn -Dtest=PerformanceTest test

For a simple comparison between this library and other libraries such as
WAH, ConciseSet, BitSet and other options, please see

https://github.com/lemire/simplebitmapbenchmark
 
Unit testing
------------

As of October 2011, this packages relies on Maven. To
test it:

mvn test

See 
http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
for details.


Usage
-----

```java
        EWAHCompressedBitmap ewahBitmap1 = EWAHCompressedBitmap.bitmapOf(0, 2, 55, 64, 1 << 30);
        EWAHCompressedBitmap ewahBitmap2 = EWAHCompressedBitmap.bitmapOf(1, 3, 64,
                1 << 30);
        System.out.println("bitmap 1: " + ewahBitmap1);
        System.out.println("bitmap 2: " + ewahBitmap2);
        // or
        EWAHCompressedBitmap orbitmap = ewahBitmap1.or(ewahBitmap2);
        System.out.println("bitmap 1 OR bitmap 2: " + orbitmap);
        System.out.println("memory usage: " + orbitmap.sizeInBytes() + " bytes");
        // and
        EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
        System.out.println("bitmap 1 AND bitmap 2: " + andbitmap);
        System.out.println("memory usage: " + andbitmap.sizeInBytes() + " bytes");
        // xor
        EWAHCompressedBitmap xorbitmap = ewahBitmap1.xor(ewahBitmap2);
        System.out.println("bitmap 1 XOR bitmap 2:" + xorbitmap);
        System.out.println("memory usage: " + xorbitmap.sizeInBytes() + " bytes");
        // fast aggregation over many bitmaps
        EWAHCompressedBitmap ewahBitmap3 = EWAHCompressedBitmap.bitmapOf(5, 55,
                1 << 30);
        EWAHCompressedBitmap ewahBitmap4 = EWAHCompressedBitmap.bitmapOf(4, 66,
                1 << 30);
        System.out.println("bitmap 3: " + ewahBitmap3);
        System.out.println("bitmap 4: " + ewahBitmap4);
        andbitmap = EWAHCompressedBitmap.and(ewahBitmap1, ewahBitmap2, ewahBitmap3,
                ewahBitmap4);
        System.out.println("b1 AND b2 AND b3 AND b4: " + andbitmap);
        // serialization
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Note: you could use a file output steam instead of ByteArrayOutputStream
        ewahBitmap1.serialize(new DataOutputStream(bos));
        EWAHCompressedBitmap ewahBitmap1new = new EWAHCompressedBitmap();
        byte[] bout = bos.toByteArray();
        ewahBitmap1new.deserialize(new DataInputStream(new ByteArrayInputStream(bout)));
        System.out.println("bitmap 1 (recovered) : " + ewahBitmap1new);
        if (!ewahBitmap1.equals(ewahBitmap1new)) throw new RuntimeException("Will not happen");
        //
        // we can use a ByteBuffer as backend for a bitmap
        // which allows memory-mapped bitmaps
        //
        ByteBuffer bb = ByteBuffer.wrap(bout);
        EWAHCompressedBitmap rmap = new EWAHCompressedBitmap(bb);
        System.out.println("bitmap 1 (mapped) : " + rmap);

        if (!rmap.equals(ewahBitmap1)) throw new RuntimeException("Will not happen");
        //
        // support for threshold function (new as of version 0.8.0):
        // mark as true a bit that occurs at least T times in the source
        // bitmaps
        //
        EWAHCompressedBitmap threshold2 = EWAHCompressedBitmap.threshold(2,
                ewahBitmap1, ewahBitmap2, ewahBitmap3, ewahBitmap4);
        System.out.println("threshold 2 : " + threshold2);
```
See example.java.

You can use our drop-in replacement for the BitSet class in a memory-mapped file
context as follows: 

```java
		final FileOutputStream fos = new FileOutputStream(tmpfile);
		BitSet Bitmap = BitSet.bitmapOf(0, 2, 55, 64, 512);
		Bitmap.serialize(new DataOutputStream(fos));
		RandomAccessFile memoryMappedFile = new RandomAccessFile(tmpfile, "r");
		ByteBuffer bb = memoryMappedFile.getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, totalcount);
		ImmutableBitSet mapped = new ImmutableBitSet(bb.asLongBuffer());
```


There are more examples in the "examples" folder (e.g.,
for memory-file mapping).


Maven central repository
------------------------

You can download JavaEWAH from the Maven central repository:
http://repo1.maven.org/maven2/com/googlecode/javaewah/JavaEWAH/

You can also specify the dependency in the Maven "pom.xml" file:

```xml
    <dependencies>
         <dependency>
	     <groupId>com.googlecode.javaewah</groupId>
	     <artifactId>JavaEWAH</artifactId>
	     <version>1.0.5</version>
         </dependency>
     </dependencies>
```

Naturally, you should replace "version" by the version
you desire.

Travis (Continuous integration)
-------------------------------

You can check whether the latest version builds on your favorite version
of Java using Travis: https://travis-ci.org/lemire/javaewah/builds/

Clojure 
-------

Joel Boehland wrote Clojure wrappers:

https://github.com/jolby/clojure-ewah-bitmap

Frequent questions
------------------

Question: Will JavaEWAH support long values?

Answer: It might, but it does not at the moment. 

Question: How do I check the value of a bit?

Answer: If you need to routinely check the value of a given bit quickly, then 
EWAH might not be the right format. However, if you must do it, you can proceed as
follows:
```java
          /**
           * Suppose you have the following bitmap:
           */
          EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf(0,2,64,1<<30);
          /**
           * We want to know if bit 64 is set:
           */
          boolean is64set = b.get(64);
```
API Documentation
-----------------

http://lemire.me/docs/javaewah/




Further reading
---------------

Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned bitmap indexes, Data & Knowledge Engineering 69 (1), 2010. 
http://arxiv.org/abs/0901.3751

Owen Kaser and Daniel Lemire, Compressed bitmap indexes: beyond unions and intersections, Software: Practice and Experience, 2014.
http://arxiv.org/abs/1402.4466


Help needed
------------

JavaEWAH has been used in production for many years. However, we still need help writing more tests.

[![Coverage Status](https://coveralls.io/repos/lemire/javaewah/badge.svg?branch=master)](https://coveralls.io/r/lemire/javaewah?branch=master)

Acknowledgement
---------------

Special thanks to Shen Liang for optimization advice.

This work was supported by NSERC grant number 26143.

[maven img]:https://maven-badges.herokuapp.com/maven-central/com.googlecode.javaewah/JavaEWAH/badge.svg
[maven]:http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.googlecode.javaewah%22%20

[license]:LICENSE-2.0.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg


[docs-badge]:https://img.shields.io/badge/API-docs-blue.svg?style=flat-square
[docs]:http://lemire.me/docs/javaewah/
