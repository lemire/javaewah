JavaEWAH
==========================================================

(c) 2009-2013
Daniel Lemire (http://lemire.me/en/), 
Cliff Moon (https://github.com/cliffmoon), 
David McIntosh (https://github.com/mctofu),
Robert Becho (https://github.com/RBecho),
Colby Ranger (https://github.com/crangeratgoogle)
Veronika Zenz (https://github.com/veronikazenz)
and Owen Kaser (https://github.com/owenkaser)


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


For better performance, use a 64-bit JVM over
64-bit CPUs when using the 64-bit scheme (javaewah.EWAHCompressedBitmap).

The 32-bit version (javaewah32.EWAHCompressedBitmap32) should
compress better but be comparatively slower.

Java 6 or better is required.

For more details regarding the compression format, please
see Section 3 of the following paper:

Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves word-aligned bitmap indexes. Data & Knowledge Engineering 69 (1), pages 3-28, 2010.  
 http://arxiv.org/abs/0901.3751
 
 (The PDF file is freely available on the arXiv site.)

Benchmark
---------

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

See example.java.

Maven central repository
------------------------

You can download JavaEWAH from the Maven central repository:
http://repo1.maven.org/maven2/com/googlecode/javaewah/JavaEWAH/

You can also specify the dependency in the Maven "pom.xml" file:

    <dependencies>
         <dependency>
	     <groupId>com.googlecode.javaewah</groupId>
	     <artifactId>JavaEWAH</artifactId>
	     <version>0.7.9</version>
         </dependency>
     </dependencies>

Naturally, you should replace "version" by the version
you desire.

Travis (Continuous integration)
-------------------------------

You can check whether the latest version builds on your favorite version
of Java using Travis: https://travis-ci.org/lemire/javaewah/builds/11059867 

Clojure 
-------

Joel Boehland wrote Clojure wrappers:

https://github.com/jolby/clojure-ewah-bitmap

Frequent questions
------------------

Question: How do I check the value of a bit?

Answer: If you need to routinely check the value of a given bit quickly, then 
EWAH might not be the right format. However, if you must do it, you can proceed as
follows:

          /**
           * Suppose you have the following bitmap:
           */
          EWAHCompressedBitmap b = EWAHCompressedBitmap.bitmapOf(0,2,64,1<<30);
          /**
           * We want to know if bit 64 is set:
           */
          boolean is64set = (b.and(EWAHCompressedBitmap.bitmapOf(64)).cardinality() == 1);                

