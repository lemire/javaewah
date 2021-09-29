JavaEWAH
==========================================================
[![Java CI](https://github.com/lemire/javaewah/actions/workflows/basic.yml/badge.svg)](https://github.com/lemire/javaewah/actions/workflows/basic.yml)
[![][maven img]][maven]
[![][license img]][license]
[![docs-badge][]][docs]
[![Coverage Status](https://coveralls.io/repos/lemire/javaewah/badge.svg?branch=master)](https://coveralls.io/r/lemire/javaewah?branch=master)
[![Code Quality: Cpp](https://img.shields.io/lgtm/grade/java/g/lemire/javaewah.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/lemire/javaewah/context:java)


This is a word-aligned compressed variant of
the Java Bitset class. We provide both a 64-bit
and a 32-bit RLE-like compression scheme. It can
be used to implement bitmap indexes. The EWAH format
it relies upon is used in the git implementation 
that runs GitHub.

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
memory-mapped files as well as many other conveniences.

For better performance, use a 64-bit JVM over
64-bit CPUs when using the 64-bit scheme (javaewah.EWAHCompressedBitmap).
The 32-bit version (javaewah32.EWAHCompressedBitmap32) should
compress better but be comparatively slower. It is recommended however that you run your own benchmark.



Java 6 or better is required. We found the very latest OpenJDK release
offered the best performance.

Real-world usage
----------------

JavaEWAH is part of Apache Hive and its derivatives (e.g.,  Apache Spark) and Eclipse JGit. It has been used in production systems for many years. It is part of major Linux distributions. It is part of [Twitter algebird](https://github.com/twitter/algebird).


EWAH is used to accelerate the distributed version control system Git (http://githubengineering.com/counting-objects/). You can find the C port of EWAH written by the Git team at https://github.com/git/git/tree/master/ewah

When should you use a bitmap?
----------------------------------------

Sets are a fundamental abstraction in
software. They can be implemented in various
ways, as hash sets, as trees, and so forth.
In databases and search engines, sets are often an integral
part of indexes. For example, we may need to maintain a set
of all documents or rows  (represented by numerical identifier)
that satisfy some property. Besides adding or removing
elements from the set, we need fast functions
to compute the intersection, the union, the difference between sets, and so on.


To implement a set
of integers, a particularly appealing strategy is the
bitmap (also called bitset or bit vector). Using n bits,
we can represent any set made of the integers from the range
[0,n): it suffices to set the ith bit is set to one if integer i is present in the set.
Commodity processors use words of W=32 or W=64 bits. By combining many such words, we can
support large values of n. Intersections, unions and differences can then be implemented
 as bitwise AND, OR and ANDNOT operations.
More complicated set functions can also be implemented as bitwise operations.

When the bitset approach is applicable, it can be orders of
magnitude faster than other possible implementation of a set (e.g., as a hash set)
while using several times less memory.

However, a bitset, even a compressed one is not always applicable. For example, if 
you have 1000 random-looking integers, then a simple array might be the best representation.
We refer to this case as the "sparse" scenario.

When should you use compressed bitmaps?
----------------------------------------

An uncompressed BitSet can use a lot of memory. For example, if you take a BitSet
and set the bit at position 1,000,000 to true and you have just over 100kB. That's over 100kB
to store the position of one bit. This is wasteful  even if you do not care about memory:
suppose that you need to compute the intersection between this BitSet and another one
that has a bit at position 1,000,001 to true, then you need to go through all these zeroes,
whether you like it or not. That can become very wasteful.

This being said, there are definitively cases where attempting to use compressed bitmaps is wasteful.
For example, if you have a small universe size. E.g., your bitmaps represent sets of integers
from [0,n) where n is small (e.g., n=64 or n=128). If you are able to uncompressed BitSet and
it does not blow up your memory usage,  then compressed bitmaps are probably not useful
to you. In fact, if you do not need compression, then a BitSet offers remarkable speed.
One of the downsides of a compressed bitmap like those provided by JavaEWAH is slower random access:
checking whether a bit is set to true in a compressed bitmap takes longer.

The sparse scenario is another use case where compressed bitmaps should not be used.
Keep in mind that random-looking data is usually not compressible. E.g., if you have a small set of
32-bit random integers, it is not mathematically possible to use far less than 32 bits per integer,
and attempts at compression can be counterproductive.

How does EWAH compares with the alternatives?
-------------------------------------------

EWAH is part of a larger family of compressed bitmaps that are run-length-encoded
bitmaps. They identify long runs of 1s or 0s and they represent them with a marker word.
If you have a local mix of 1s and 0, you use an uncompressed word.

There are many formats in this family beside EWAH:

* Oracle's BBC is an obsolete format at this point: though it may provide good compression,
it is likely much slower than more recent alternatives due to excessive branching.
* WAH is a patented variation on BBC that provides better performance.
* Concise is a variation on the patented WAH. It some specific instances, it can compress
much better than WAH (up to 2x better), but it is generally slower.
* EWAH is both free of patent, and it is faster than all the above. On the downside, it
does not compress quite as well. It is faster because it allows some form of "skipping"
over uncompressed words. So though none of these formats are great at random access, EWAH
is better than the alternatives.

There are other alternatives however. For example, the Roaring
format (https://github.com/lemire/RoaringBitmap) is not a run-length-encoded hybrid. It provides faster random access
than even EWAH.


Data format
------------

For more details regarding the compression format, please
see Section 3 of the following paper:

Daniel Lemire, Owen Kaser, Kamel Aouiche, [Sorting improves word-aligned bitmap indexes](http://arxiv.org/abs/0901.3751). Data & Knowledge Engineering 69 (1), pages 3-28, 2010.  
 

Benchmark
---------

For a simple comparison between this library and other libraries such as
WAH, ConciseSet, BitSet and other options, please see

https://github.com/lemire/simplebitmapbenchmark

However, this is very naive. It is recommended that you run your own benchmarks.

Unit testing
------------

As of October 2011, this package relies on Maven. To
test it:

```
mvn test
```

See
http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
for details.

We support Java 8 and up, but to build the library, you need Java 9 and up
with the default Maven setup, since we rely on the `--release` flag functionality
(unavailable in Java 8) to sidestep the [Deaded-NoSuchMethodError issue](https://www.morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/).



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
	     <version>[1.1,)</version>
         </dependency>
     </dependencies>
```

Naturally, you should replace "version" by the version
you desire.

Ubuntu (Linux)
------------------

To install javaewah on Ubuntu, type:

          sudo apt-get install libjavaewah-java

Clojure
-------

Joel Boehland wrote Clojure wrappers:

https://github.com/jolby/clojure-ewah-bitmap

Frequent questions
------------------

Question: How do I build javaewah without testing or signing?

        mvn clean install -DskipTests -Dgpg.skip=true

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

http://www.javadoc.io/doc/com.googlecode.javaewah/JavaEWAH/

Mailing list and discussion group
---------------------------------

https://groups.google.com/forum/#!forum/javaewah


Further reading
---------------

- Daniel Lemire, Owen Kaser, Kamel Aouiche, [Sorting improves word-aligned bitmap indexes](http://arxiv.org/abs/0901.3751), Data & Knowledge Engineering 69 (1), 2010.
- Owen Kaser and Daniel Lemire, [Compressed bitmap indexes: beyond unions and intersections](http://arxiv.org/abs/1402.4466), Software: Practice and Experience 46 (2), 2016. 


Credit
--------


(c) 2009-2021
[Daniel Lemire](http://lemire.me/en/), Cliff Moon, [David McIntosh](https://github.com/mctofu), [Robert Becho](https://github.com/RBecho), [Colby Ranger](https://github.com/crangeratgoogle), [Veronika Zenz](https://github.com/veronikazenz), [Owen Kaser](https://github.com/owenkaser), [Gregory Ssi-Yan-Kai](https://github.com/gssiyankai), and [Rory Graves](https://github.com/rorygraves)


This code is licensed under Apache License, Version 2.0 (ASL2.0).
(GPL 2.0 derivatives are allowed.)

Acknowledgement
---------------

Special thanks to Shen Liang for optimization advice.

This work was supported by NSERC grant number 26143.

[maven img]:https://maven-badges.herokuapp.com/maven-central/com.googlecode.javaewah/JavaEWAH/badge.svg
[maven]:http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.googlecode.javaewah%22%20

[license]:LICENSE-2.0.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg


[docs-badge]:https://img.shields.io/badge/API-docs-blue.svg?style=flat-square
[docs]:http://www.javadoc.io/doc/com.googlecode.javaewah/JavaEWAH/
