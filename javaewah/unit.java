package javaewah;

/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/

import java.util.*;
import java.io.*;

public class unit {
    private static final int MEGA = 8 * 1024 * 1024;
    private static final int TEST_BS_SIZE = 8 * MEGA;

    /**
    * Inspired by Federico Fissore.
    */
    public static int[] createSortedIntArrayOfBitsToSet(int size) {
        Random random = new Random();
        // build raw int array
        int[] bits = new int[size];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = random.nextInt(TEST_BS_SIZE);
        }
        // might generate duplicates
        Arrays.sort(bits);
        // first count how many distinct values
        int counter = 0;
        int oldx = -1;
        for(int x : bits) {
            if(x!= oldx) ++counter;
            oldx=x;
        }
        // then construct new array
        int[] answer = new int[counter];
        counter = 0;
        oldx = -1;
        for(int x : bits) {
            if(x!= oldx) {
                answer[counter] = x;
                ++counter;
            }
            oldx=x;
        }
        return answer;
    }


    /**
    * Non deterministic test inspired by S.J.vanSchaik.
    */
    public static void vanSchaikTest() {
        System.out.println("running vanSchaikTest (this takes some time)");
        final int totalNumBits = 32768;
        final double odds = 0.9;
        int numBitsSet = 0;
        for (int t = 0; t < 10000; t++) {
            EWAHCompressedBitmap cBitMap = new EWAHCompressedBitmap();
            for (int i = 0; i < totalNumBits; i++) {
                if (Math.random() < odds) {
                    cBitMap.set(i);
                    numBitsSet++;
                }
            }
        }
    }

    /*
    * Test inspired by William Habermaas
    */
    public static void habermaasTest() {
        System.out.println("running habermaasTest");
        BitSet bitsetaa = new BitSet();
        EWAHCompressedBitmap aa = new EWAHCompressedBitmap();
        int[] val = { 55400, 1000000, 1000128 };
        for (int k = 0; k < val.length; ++k) {
            aa.set(val[k]);
            bitsetaa.set(val[k]);
        }
        equal(aa,bitsetaa);
        BitSet bitsetab = new BitSet();
        EWAHCompressedBitmap ab = new EWAHCompressedBitmap();
        for (int i = 4096; i < (4096 + 5); i++) {
            ab.set(i);
            bitsetab.set(i);
        }
        ab.set(99000);
        bitsetab.set(99000);
        ab.set(1000130);
        bitsetab.set(1000130);
        equal(ab,bitsetab);
        EWAHCompressedBitmap bb = aa.or(ab);
        EWAHCompressedBitmap bbAnd = aa.and(ab);
        BitSet bitsetbb = (BitSet) bitsetaa.clone();
        bitsetbb.or(bitsetab);
        BitSet bitsetbbAnd = (BitSet) bitsetaa.clone();
        bitsetbbAnd.and(bitsetab);
        equal(bbAnd,bitsetbbAnd);
        equal(bb,bitsetbb);
    }

    /**
    * Non deterministic test inspired by Federico Fissore.
    */
    public static  void shouldSetBits(int length)  {
        System.out.println("testing shouldSetBits "+length);
        int[] bitsToSet = createSortedIntArrayOfBitsToSet(length);
        EWAHCompressedBitmap ewah = new EWAHCompressedBitmap();
        System.out.println(" ... setting "+bitsToSet.length+" values");
        long now = System.currentTimeMillis();
        for (int i : bitsToSet) {
            ewah.set(i);
        }
        System.out.println(" ... verifying "+bitsToSet.length+" values");
        equal(ewah.iterator(),bitsToSet);
        System.out.println(" ... checking cardinality");
        equal(bitsToSet.length, ewah.cardinality());
    }


    public static void testRunningLengthWord() {
        System.out.println("testing RunningLengthWord");
        long x[] = new long[1];
        RunningLengthWord rlw = new RunningLengthWord(x,0);
        equal(0,rlw.getNumberOfLiteralWords());
        equal(false,rlw.getRunningBit());
        equal(0,rlw.getRunningLength());
        rlw.setRunningBit(true);
        equal(0,rlw.getNumberOfLiteralWords());
        equal(true,rlw.getRunningBit());
        equal(0,rlw.getRunningLength());
        rlw.setRunningBit(false);
        equal(0,rlw.getNumberOfLiteralWords());
        equal(false,rlw.getRunningBit());
        equal(0,rlw.getRunningLength());
        for(long rl = rlw.largestliteralcount; rl >=0; rl-=1024) {
            rlw.setNumberOfLiteralWords(rl);
            equal(rl,rlw.getNumberOfLiteralWords());
            equal(false,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
            rlw.setNumberOfLiteralWords(0);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(false,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
        }
        for(long rl = 0; rl <=rlw.largestrunninglengthcount; rl+=1024) {
            rlw.setRunningLength(rl);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(false,rlw.getRunningBit());
            equal(rl,rlw.getRunningLength());
            rlw.setRunningLength(0);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(false,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
        }
        rlw.setRunningBit(true);
        for(long rl = 0; rl <=rlw.largestrunninglengthcount; rl+=1024) {
            rlw.setRunningLength(rl);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(true,rlw.getRunningBit());
            equal(rl,rlw.getRunningLength());
            rlw.setRunningLength(0);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(true,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
        }
        for(long rl = 0; rl <=rlw.largestliteralcount; rl+=128) {
            rlw.setNumberOfLiteralWords(rl);
            equal(rl,rlw.getNumberOfLiteralWords());
            equal(true,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
            rlw.setNumberOfLiteralWords(0);
            equal(0,rlw.getNumberOfLiteralWords());
            equal(true,rlw.getRunningBit());
            equal(0,rlw.getRunningLength());
        }
    }


    static void testEWAHCompressedBitmap() {
        System.out.println("testing EWAH");
        long zero = 0;
        long specialval = 1l | (1l << 4)|(1l << 63);
        long notzero = ~zero;
        EWAHCompressedBitmap myarray1 = new EWAHCompressedBitmap();
        myarray1.add(zero);
        myarray1.add(zero);
        myarray1.add(zero);
        myarray1.add(specialval);
        myarray1.add(specialval);
        myarray1.add(notzero);
        myarray1.add(zero);
        equal(myarray1.getPositions().size(), 6+64);
        EWAHCompressedBitmap myarray2 = new EWAHCompressedBitmap();
        myarray2.add(zero);
        myarray2.add(specialval);
        myarray2.add(specialval);
        myarray2.add(notzero);
        myarray2.add(zero);
        myarray2.add(zero);
        myarray2.add(zero);
        equal(myarray2.getPositions().size(), 6+64);
        Vector<Integer> data1 = myarray1.getPositions();
        Vector<Integer> data2 = myarray2.getPositions();
        Vector<Integer> logicalor = new Vector<Integer>();
        {
            HashSet<Integer> tmp = new HashSet<Integer>();
            tmp.addAll(data1);
            tmp.addAll(data2);
            logicalor.addAll(tmp);
        }
        Collections.sort(logicalor);
        Vector<Integer> logicaland = new Vector<Integer>();
        logicaland.addAll(data1);
        logicaland.retainAll(data2);
        Collections.sort(logicaland);
        EWAHCompressedBitmap arrayand = myarray1.and(myarray2);
        isTrue(arrayand.getPositions().equals(logicaland));
        EWAHCompressedBitmap arrayor = myarray1.or(myarray2);
        isTrue(arrayor.getPositions().equals(logicalor));
        EWAHCompressedBitmap arrayandbis = myarray2.and(myarray1);
        isTrue(arrayandbis.getPositions().equals(logicaland));
        EWAHCompressedBitmap arrayorbis = myarray2.or(myarray1);
        isTrue(arrayorbis.getPositions().equals(logicalor));
        EWAHCompressedBitmap x = new EWAHCompressedBitmap();
        for(Integer i: myarray1.getPositions()) {
            x.set(i.intValue());
        }
        isTrue(x.getPositions().equals(myarray1.getPositions()));
        x = new EWAHCompressedBitmap();
        for(Integer i: myarray2.getPositions()) {
            x.set(i.intValue());
        }
        isTrue(x.getPositions().equals(myarray2.getPositions()));
        x = new EWAHCompressedBitmap();
        for(Iterator<Integer> k = myarray1.iterator(); k.hasNext(); ) {
            x.set(k.next().intValue());
        }
        isTrue(x.getPositions().equals(myarray1.getPositions()));
        x = new EWAHCompressedBitmap();
        for(Iterator<Integer> k = myarray2.iterator(); k.hasNext(); ) {
            x.set(k.next().intValue());
        }
        isTrue(x.getPositions().equals(myarray2.getPositions()));
    }

    /**  as per renaud.delbru, Feb 12, 2009
    * this might throw an error out of bound exception.
    */
    static void testLargeEWAHCompressedBitmap() {
        System.out.println("testing EWAH over a large array");
        EWAHCompressedBitmap myarray1 = new EWAHCompressedBitmap();
        int N= 11000000;
        for(int i = 0; i <N; ++i) {
            myarray1.set(i);
        }
        isTrue(myarray1.sizeInBits() == N);
    }

    static void testCardinality () {
        System.out.println("testing EWAH cardinality");
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        bitmap.set(Integer.MAX_VALUE);
        //System.out.format("Total Items %d\n", bitmap.cardinality());
        isTrue(bitmap.cardinality()==1);
    }
    static void testSetGet () {
        System.out.println("testing EWAH set/get");
        EWAHCompressedBitmap ewcb = new EWAHCompressedBitmap();
        int[] val = {5,4400,44600,55400,1000000};
        for (int k = 0; k< val.length; ++k) {
            ewcb.set(val[k]);
        }
        Vector<Integer> result = ewcb.getPositions();
        isTrue(val.length==result.size());
        for(int k = 0; k<val.length; ++k) {
            isTrue(result.get(k)==val[k]);
        }
    }

    static void testExternalization () throws IOException {
        System.out.println("testing EWAH externalization");
        EWAHCompressedBitmap ewcb = new EWAHCompressedBitmap();
        int[] val = {5,4400,44600,55400,1000000};
        for (int k = 0; k< val.length; ++k) {
            ewcb.set(val[k]);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bos);
        ewcb.writeExternal(oo);
        oo.close();
        ewcb = null;
        ewcb = new EWAHCompressedBitmap();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray() );
        ewcb.readExternal(new ObjectInputStream(bis));
        Vector<Integer> result = ewcb.getPositions();
        isTrue(val.length==result.size());
        for(int k = 0; k<val.length; ++k) {
            isTrue(result.get(k)==val[k]);
        }
    }
    static void equal(Iterator<Integer> i, int[] array) {
        int cursor = 0;
        while(i.hasNext()) {
            int x = i.next();
            int y = array[cursor++];
            if( x != y  ) throw new RuntimeException(x+" != "+y);
        }
    }

    static void equal(EWAHCompressedBitmap x, BitSet y) {
        if(x.cardinality() != y.cardinality()) throw new RuntimeException("cardinality differs ");
        for(int i : x.getPositions())
            if(!y.get(i)) throw new RuntimeException("bitset got different bits");
    }

    static void equal(int x, int y) {
        if(x!=y) throw new RuntimeException(x+" != "+y);
    }

    static void equal(long x, long y) {
        if(x!=y) throw new RuntimeException(x+" != "+y);
    }

    static void equal(boolean x, boolean y) {
        if(x!=y) throw new RuntimeException(x+" != "+y);
    }

    static void isTrue(boolean x) {
        if(!x) throw new RuntimeException();
    }



    // a non-deterministic test proposed by Marc Polizzi
    public static void PolizziTest(int maxlength) {
        System.out.println("Polizzi test with max length = "+maxlength);
        for(int k = 0; k<10000; ++k) {
            final Random rnd = new Random();
            final EWAHCompressedBitmap ewahBitmap1 = new EWAHCompressedBitmap();
            final BitSet jdkBitmap1 = new BitSet();
            final EWAHCompressedBitmap ewahBitmap2 = new EWAHCompressedBitmap();
            final BitSet jdkBitmap2 = new BitSet();
            final int len = rnd.nextInt(maxlength);
            //System.out.println("----- len: " + len);
            for (int pos = 0; pos < len; pos++) { // random *** number of bits set ***
                if (rnd.nextInt(7) == 0) { // random *** increasing *** values
                    ewahBitmap1.set(pos);
                    jdkBitmap1.set(pos);
                }
                if (rnd.nextInt(11) == 0) { // random *** increasing *** values
                    ewahBitmap2.set(pos);
                    jdkBitmap2.set(pos);
                }
            }
            assertEquals(jdkBitmap1, ewahBitmap1);
            assertEquals(jdkBitmap2, ewahBitmap2);
            // XOR
            {
                final EWAHCompressedBitmap xorEwahBitmap = ewahBitmap1.xor(ewahBitmap2);
                final BitSet xorJdkBitmap = (BitSet) jdkBitmap1.clone();
                xorJdkBitmap.xor(jdkBitmap2);
                assertEquals(xorJdkBitmap, xorEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap andEwahBitmap = ewahBitmap1.and(ewahBitmap2);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1.clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
            }
            // AND
            {
                final EWAHCompressedBitmap andEwahBitmap = ewahBitmap2.and(ewahBitmap1);
                final BitSet andJdkBitmap = (BitSet) jdkBitmap1.clone();
                andJdkBitmap.and(jdkBitmap2);
                assertEquals(andJdkBitmap, andEwahBitmap);
            }
            // OR
            {
                final EWAHCompressedBitmap orEwahBitmap = ewahBitmap1.or(ewahBitmap2);
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1.clone();
                orJdkBitmap.or(jdkBitmap2);
                assertEquals(orJdkBitmap, orEwahBitmap);
            }
            // OR
            {
                final EWAHCompressedBitmap orEwahBitmap = ewahBitmap2.or(ewahBitmap1);
                final BitSet orJdkBitmap = (BitSet) jdkBitmap1.clone();
                orJdkBitmap.or(jdkBitmap2);
                assertEquals(orJdkBitmap, orEwahBitmap);
            }
        }
    }

    // part of a test contributed by Marc Polizzi
    static void assertEquals(BitSet jdkBitmap, EWAHCompressedBitmap ewahBitmap) {
        assertEqualsIterator(jdkBitmap, ewahBitmap);
        assertEqualsPositions(jdkBitmap, ewahBitmap);
        assertCardinality(jdkBitmap, ewahBitmap);
    }


    // part of a test contributed by Marc Polizzi
    static void assertCardinality(BitSet jdkBitmap, EWAHCompressedBitmap ewahBitmap) {
        final int c1 = jdkBitmap.cardinality();
        final int c2 = ewahBitmap.cardinality();
        if (c1 != c2) {
            throw new RuntimeException("cardinality differs : " + c1 + " , " + c2);
        }
    }



    // part of a test contributed by Marc Polizzi
    static void assertEqualsIterator(BitSet jdkBitmap, EWAHCompressedBitmap ewahBitmap) {
        final Vector<Integer> positions = new Vector<Integer>();
        final Iterator<Integer> bits = ewahBitmap.iterator();
        while (bits.hasNext()) {
            final int bit = bits.next();
            if (!jdkBitmap.get(bit)) {
                throw new RuntimeException("iterator: bitset got different bits");
            }
            positions.add(bit);
        }
        for (int pos = jdkBitmap.nextSetBit(0); pos >= 0; pos = jdkBitmap.nextSetBit(pos + 1)) {
            if (!positions.contains(pos)) {
                throw new RuntimeException("iterator: bitset got different bits");
            }
        }
    }

    // part of a test contributed by Marc Polizzi
    static void assertEqualsPositions(BitSet jdkBitmap, EWAHCompressedBitmap ewahBitmap) {
        final Vector<Integer> positions = ewahBitmap.getPositions();
        for (int position : positions) {
            if (!jdkBitmap.get(position)) {
                throw new RuntimeException("positions: bitset got different bits");
            }
        }
        for (int pos = jdkBitmap.nextSetBit(0); pos >= 0; pos = jdkBitmap.nextSetBit(pos + 1)) {
            if (!positions.contains(pos)) {
                throw new RuntimeException("positions: bitset got different bits");
            }
        }
    }

    public static void testMassiveAnd() {
        System.out.println("testing massive logical and");
        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[1024];
        for(int k = 0; k<ewah.length; ++k) ewah[k] = new EWAHCompressedBitmap();
        for(int k = 0; k<30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap answer = ewah[0];
        for(int k = 1; k<ewah.length; ++k)
            answer = answer.and(ewah[k]);
        // result should be empty
        if(answer.getPositions().size() != 0)
            System.out.println(answer.toDebugString());
        isTrue(answer.getPositions().size() == 0);
    }
    public static void testMassiveXOR() {
        System.out.println("testing massive xor");
        final int N = 1024;
        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
        BitSet[] bset = new BitSet[N];
        for(int k = 0; k<ewah.length; ++k) ewah[k] = new EWAHCompressedBitmap();
        for(int k = 0; k<bset.length; ++k) bset[k] = new BitSet();
        for(int k = 0; k<30000; ++k) {
            ewah[(k + 2 * k * k) % ewah.length].set(k);
            bset[(k + 2 * k * k) % ewah.length].set(k);
        }
        EWAHCompressedBitmap answer = ewah[0];
        BitSet bitsetanswer = bset[0];
        for(int k = 1; k<ewah.length; ++k) {
            answer = answer.xor(ewah[k]);
            bitsetanswer.or(bset[k]);
            assertEqualsPositions(bitsetanswer, answer);
        }
        int k = 0;
        for(int j : answer) {
            if(k!=j)
                System.out.println(answer.toDebugString());
            equal(k,j);
            k+=1;
        }
    }


    public static void testMassiveOr() {
        System.out.println("testing massive logical or");
        final int N = 1024;
        for(int howmany = 512; howmany <=10000; howmany *=2){
	        EWAHCompressedBitmap[] ewah = new EWAHCompressedBitmap[N];
	        BitSet[] bset = new BitSet[N];
	        for(int k = 0; k<ewah.length; ++k) ewah[k] = new EWAHCompressedBitmap();
	        for(int k = 0; k<bset.length; ++k) bset[k] = new BitSet();
	        for(int k = 0; k<N; ++k)
	            assertEqualsPositions(bset[k], ewah[k]);
	        for(int k = 0; k<howmany; ++k) {
	            ewah[(k + 2 * k * k) % ewah.length].set(k);
	            bset[(k + 2 * k * k) % ewah.length].set(k);
	        }
	        for(int k = 0; k<N; ++k)
	            assertEqualsPositions(bset[k], ewah[k]);
	        EWAHCompressedBitmap answer = ewah[0];
	        BitSet bitsetanswer = bset[0];
	        for(int k = 1; k<ewah.length; ++k) {
	            EWAHCompressedBitmap tmp = answer.or(ewah[k]);
	            bitsetanswer.or(bset[k]);
	            answer = tmp;
	            assertEqualsPositions(bitsetanswer, answer);
	        }
	        assertEqualsPositions(bitsetanswer, answer);
	        int k = 0;
	        for(int j : answer) {
	            if(k!=j)
	                System.out.println(answer.toDebugString());
	            equal(k,j);
	            k+=1;
	        }
        }
    }

    public static void main(String[] args) throws IOException {
    	System.out.println("These tests can run for several minutes. Please be patient.");
        habermaasTest();
        testEWAHCompressedBitmap();
        testRunningLengthWord();
        testLargeEWAHCompressedBitmap();
        testCardinality ();
        testSetGet ();
        PolizziTest(64);
        PolizziTest(128);
        PolizziTest(256);
        PolizziTest(2048);
        testExternalization ();
        vanSchaikTest();
        for(int k = 2; k<1<<24; k*=8) shouldSetBits(k);
        testMassiveOr();
        testMassiveAnd();
        testMassiveXOR();
        System.out.println("Your code is probably ok.");
    }
}