package javaewah;


/*
* Copyright 2009-2010, Daniel Lemire
* Licensed under the GPL version 3 and APL 2.0, among other licenses.
*/
/**
* This implements the patent-free(*) EWAH scheme.
* Roughly speaking, it is a 64-bit variant of the
* BBC compression scheme used by Oracle for its bitmap
* indexes.
*
* The objective of this compression type is to provide
* some compression, while reducing as much as possible
* the CPU cycle usage.
* 
*
* This implementation being 64-bit, it assumes a 64-bit CPU
* together with a 64-bit Java Virtual Machine. This same code
* on a 32-bit machine may not be as fast.
*
* For more details, see the following paper:
*
* Daniel Lemire, Owen Kaser, Kamel Aouiche, Sorting improves
* word-aligned bitmap indexes. Data & Knowledge
* Engineering 69 (1), pages 3-28, 2010.
* http://arxiv.org/abs/0901.3751
*
* It was first described by Wu et al. and named WBC:
*
* K. Wu, E. J. Otoo, A. Shoshani, H. Nordberg, Notes on design and
* implementation of compressed bit vectors, Tech. Rep. LBNL/PUB-3161,
* Lawrence Berkeley National Laboratory, available from http://crd.lbl.
*  gov/~kewu/ps/PUB-3161.html (2001).
*
* *- The author (D. Lemire) does not know of any patent
*    infringed by the following implementation. However, similar
*    schemes, like WAH are covered by patents.
*/

import java.util.*;
import java.io.*;

public class EWAHCompressedBitmap implements Cloneable, Externalizable, Iterable<Integer> {

    public EWAHCompressedBitmap () {}

    EWAHIterator getEWAHIterator() {
        return new EWAHIterator(buffer,actualsizeinwords);
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise AND-Not values of the current bitmap with some other bitmap.
     * 
     * The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     * 
     */
    public EWAHCompressedBitmap andNot(EWAHCompressedBitmap a) {
        try {
        	final EWAHCompressedBitmap nota = (EWAHCompressedBitmap) a.clone();
            nota.not();
            return and(nota);
        } catch(java.lang.CloneNotSupportedException cnse) {
            throw new RuntimeException();
        }
    }
    
    /**
    * Returns a new compressed bitmap contained the bitwise XOR values of the current bitmap with some other bitmap.
    * 
    * The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
    */
    public EWAHCompressedBitmap xor(EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {// this never happens...
            container.setSizeInBits(sizeInBits());
            return container;
        }
        // at this point, this is safe:
        BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
        BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
        while (true) {
        	final boolean i_is_prey = rlwi.size()<rlwj.size();
        	final BufferedRunningLengthWord prey = i_is_prey ? rlwi: rlwj;
        	final BufferedRunningLengthWord predator = i_is_prey ? rlwj: rlwi;
            if(prey.getRunningBit() == false) {
                final long predatorrl = predator.getRunningLength();
                final long preyrl = prey.getRunningLength();
                final long  tobediscarded = (predatorrl >= preyrl) ?  preyrl : predatorrl;
                container.addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
                final long dw_predator = predator.dirtywordoffset + (i_is_prey ? j.dirtyWords(): i.dirtyWords());
                container.addStreamOfDirtyWords(i_is_prey ? j.buffer(): i.buffer(), dw_predator, preyrl - tobediscarded);
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
            } else {
                // we have a stream of 1x11
            	final long predatorrl  = predator.getRunningLength();
            	final long preyrl  = prey.getRunningLength();
            	final long tobediscarded = (predatorrl >= preyrl) ?  preyrl : predatorrl;
                container.addStreamOfEmptyWords(! predator.getRunningBit(), tobediscarded);
                final int  dw_predator  = predator.dirtywordoffset + (i_is_prey ? j.dirtyWords(): i.dirtyWords());
                final long[] buf = i_is_prey ? j.buffer(): i.buffer();
                for(int k = 0; k<preyrl - tobediscarded; ++k)
                    container.add(~ buf[k+dw_predator]);
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
            }
            final long predatorrl = predator.getRunningLength();
            if(predatorrl>0) {
                if(predator.getRunningBit() == false) {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                	final long dw_prey = prey.dirtywordoffset + (i_is_prey ? i.dirtyWords(): j.dirtyWords());
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                    container.addStreamOfDirtyWords(i_is_prey ? i.buffer(): j.buffer(),dw_prey, tobediscarded);
                } else {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                	final int dw_prey = prey.dirtywordoffset + (i_is_prey ? i.dirtyWords(): j.dirtyWords());
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                    final long[] buf = i_is_prey ? i.buffer():j.buffer();
                    for(int k = 0; k<tobediscarded; ++k)
                        container.add(~ buf[k+dw_prey]);
                }
            }
            // all that is left to do now is to AND the dirty words
            long nbre_dirty_prey = prey.getNumberOfLiteralWords();
            if(nbre_dirty_prey > 0) {
                for(int k = 0; k<nbre_dirty_prey; ++k) {
                    if(i_is_prey)
                        container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords()+k]
                                      ^ j.buffer()[predator.dirtywordoffset + j.dirtyWords()+k]);
                    else
                        container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()+k]
                                      ^ j.buffer()[prey.dirtywordoffset + j.dirtyWords()+k]);
                }
                predator.discardFirstWords(nbre_dirty_prey);
            }
            if( i_is_prey ) {
                if(!i.hasNext()) {
                    rlwi = null;
                    break;
                }
                rlwi = new BufferedRunningLengthWord(i.next());
            } else {
                if(!j.hasNext()) {
                    rlwj = null;
                    break;
                }
                rlwj = new BufferedRunningLengthWord( j.next());
            }
        }
        if (rlwi != null)
            discharge(rlwi, i, container);
        if (rlwj != null)
            discharge(rlwj, j, container);
        container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
        return container;
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise AND values of the current bitmap with some other bitmap.
     * 
     *  The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     */
    public EWAHCompressedBitmap and(EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {// this never happens...
            container.setSizeInBits(sizeInBits());
            return container;
        }
        // at this point, this is safe:
        BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
        BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord(j.next());
        while (true) {
            boolean i_is_prey = rlwi.size()<rlwj.size();
            final BufferedRunningLengthWord prey = i_is_prey ? rlwi: rlwj;
            final BufferedRunningLengthWord predator = i_is_prey ? rlwj: rlwi;
            if(prey.getRunningBit() == false) {
            	final long preyrl = prey.getRunningLength();
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
                container.addStreamOfEmptyWords(false, preyrl);
            } else {
                // we have a stream of 1x11
            	final long predatorrl  = predator.getRunningLength();
            	final long preyrl  = prey.getRunningLength();
            	final long tobediscarded = (predatorrl >= preyrl) ?  preyrl : predatorrl;
                container.addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
                final int  dw_predator  = predator.dirtywordoffset + (i_is_prey ? j.dirtyWords(): i.dirtyWords());
                container.addStreamOfDirtyWords(i_is_prey ? j.buffer(): i.buffer(), dw_predator, preyrl - tobediscarded);
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
            }
            final long predatorrl = predator.getRunningLength();
            if(predatorrl>0) {
                if(predator.getRunningBit() == false) {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                    container.addStreamOfEmptyWords(false, tobediscarded);
                } else {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final int dw_prey = prey.dirtywordoffset +  (i_is_prey ? i.dirtyWords(): j.dirtyWords());
                    final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                    container.addStreamOfDirtyWords(i_is_prey ? i.buffer(): j.buffer(), dw_prey, tobediscarded);
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                }
            }
            // all that is left to do now is to AND the dirty words
            long nbre_dirty_prey = prey.getNumberOfLiteralWords();
            if(nbre_dirty_prey > 0) {
                for(int k = 0; k<nbre_dirty_prey; ++k) {
                    if(i_is_prey)
                        container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords()+k]
                                      & j.buffer()[predator.dirtywordoffset + j.dirtyWords()+k]);
                    else
                        container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()+k]
                                      & j.buffer()[prey.dirtywordoffset + j.dirtyWords()+k]);
                }
                predator.discardFirstWords(nbre_dirty_prey);
            }
            if( i_is_prey ) {
                if(!i.hasNext()) {
                    rlwi = null;
                    break;
                }
                rlwi = new BufferedRunningLengthWord(i.next());
            } else {
                if(!j.hasNext()) {
                    rlwj = null;
                    break;
                }
                rlwj = new BufferedRunningLengthWord( j.next());
            }
        }
        if (rlwi != null)
            dischargeAsEmpty(rlwi, i, container);
        if (rlwj != null)
            dischargeAsEmpty(rlwj, j, container);
        container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
        return container;
    }

    /**
    * Negate (bitwise) the current bitmap. To obtained a negated copy, do ((EWAHCompressedBitmap) mybitmap.clone()).not();
    * 
    *     The running time is proportionnal to the compressed size (as reported by sizeInBytes()).
    */
    public void not() {
    	final EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        while(i.hasNext()) {
        	final RunningLengthWord rlw = i.next();
            rlw.setRunningBit(! rlw.getRunningBit());
            for(int j = 0; j<rlw.getNumberOfLiteralWords(); ++j) {
                i.buffer()[i.dirtyWords()+j] = ~i.buffer()[i.dirtyWords()+j];
            }
        }
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise OR values of the current bitmap with some other bitmap.
     * 
     *  The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     */
    public EWAHCompressedBitmap or(EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {//  this never happens...
            container.setSizeInBits(sizeInBits());
            return container;
        }
        // at this point, this is  safe:
        BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
        BufferedRunningLengthWord rlwj = new BufferedRunningLengthWord( j.next());
        //RunningLength;
        while (true) {
        	final boolean i_is_prey = rlwi.size()<rlwj.size();
        	final BufferedRunningLengthWord prey = i_is_prey ? rlwi: rlwj;
        	final BufferedRunningLengthWord predator = i_is_prey ? rlwj: rlwi;
            if(prey.getRunningBit() == false) {
                final long predatorrl = predator.getRunningLength();
                final long preyrl = prey.getRunningLength();
                final long  tobediscarded = (predatorrl >= preyrl) ?  preyrl : predatorrl;
                container.addStreamOfEmptyWords(predator.getRunningBit(), tobediscarded);
                final long dw_predator = predator.dirtywordoffset + (i_is_prey ? j.dirtyWords(): i.dirtyWords());
                container.addStreamOfDirtyWords(i_is_prey ? j.buffer(): i.buffer(), dw_predator, preyrl - tobediscarded);
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
            } else {
                // we have a stream of 1x11
            	final long preyrl  = prey.getRunningLength();
                container.addStreamOfEmptyWords(true, preyrl);
                predator.discardFirstWords(preyrl);
                prey.discardFirstWords(preyrl);
            }
            long predatorrl = predator.getRunningLength();
            if(predatorrl>0) {
                if(predator.getRunningBit() == false) {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                	final long dw_prey = prey.dirtywordoffset + (i_is_prey ? i.dirtyWords(): j.dirtyWords());
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                    container.addStreamOfDirtyWords(i_is_prey ? i.buffer(): j.buffer(),dw_prey, tobediscarded);
                } else {
                	final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
                	final int dw_prey = prey.dirtywordoffset + (i_is_prey ? i.dirtyWords(): j.dirtyWords());
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                    container.addStreamOfEmptyWords(true, tobediscarded);
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                }
            }
            // all that is left to do now is to OR the dirty words
            long nbre_dirty_prey = prey.getNumberOfLiteralWords();
            if(nbre_dirty_prey > 0) {
                for(int k = 0; k< nbre_dirty_prey; ++k) {
                    if(i_is_prey)
                        container.add(i.buffer()[prey.dirtywordoffset + i.dirtyWords()+k]
                                      | j.buffer()[predator.dirtywordoffset + j.dirtyWords()+k]);
                    else
                        container.add(i.buffer()[predator.dirtywordoffset + i.dirtyWords()+k]
                                      | j.buffer()[prey.dirtywordoffset + j.dirtyWords()+k]);
                }
                predator.discardFirstWords(nbre_dirty_prey);
            }
            if( i_is_prey ) {
                if(!i.hasNext()) {
                    rlwi = null;
                    break;
                }
                rlwi = new BufferedRunningLengthWord(i.next());
            } else {
                if(!j.hasNext()) {
                    rlwj = null;
                    break;
                }
                rlwj = new BufferedRunningLengthWord( j.next());
            }
        }
        if (rlwi != null)
            discharge(rlwi, i, container);
        if (rlwj != null)
            discharge(rlwj, j, container);
        container.setSizeInBits(Math.max(sizeInBits(), a.sizeInBits()));
        return container;
    }

    
    /**
     *  For internal use.
     */
    private void discharge(BufferedRunningLengthWord initialWord, EWAHIterator iterator,
                           EWAHCompressedBitmap container) {
        BufferedRunningLengthWord runningLengthWord = initialWord;
        for (;;) {
            final long runningLength = runningLengthWord.getRunningLength();
            container.addStreamOfEmptyWords(runningLengthWord.getRunningBit(), runningLength);
            container.addStreamOfDirtyWords(iterator.buffer(), iterator.dirtyWords() + runningLengthWord.dirtywordoffset,
                                            runningLengthWord.getNumberOfLiteralWords());
            if (!iterator.hasNext())
                break;
            runningLengthWord = new BufferedRunningLengthWord(iterator.next());
        }
    }
    /**
     * For internal use.
     */
    private void dischargeAsEmpty(BufferedRunningLengthWord initialWord, EWAHIterator iterator,
                                  EWAHCompressedBitmap container) {
        BufferedRunningLengthWord runningLengthWord = initialWord;
        for (;;) {
            final long runningLength = runningLengthWord.getRunningLength();
            container.addStreamOfEmptyWords(false, runningLength + runningLengthWord.getNumberOfLiteralWords());
            if (!iterator.hasNext())
                break;
            runningLengthWord = new BufferedRunningLengthWord(iterator.next());
        }
    }

    /**
    * set the bit at position i to true, the bits must
    * be set in increasing order. For example, set(15) and then set(7) will fail. You must
    * do set(7) and then set(15).
    */
    public void set(int i) {
        assert i>= sizeinbits;
        // must I complete a word?
        if ( (sizeinbits % 64) != 0) {
        	final int possiblesizeinbits = (sizeinbits /64)*64 + 64;
            if(possiblesizeinbits<i+1) {
                sizeinbits = possiblesizeinbits;
            }
        }
        addStreamOfEmptyWords(false, (i/64) - sizeinbits/64);
        int bittoflip = i-(sizeinbits/64 * 64);
        // next, we set the bit
        if(( rlw.getNumberOfLiteralWords() == 0) || ((sizeinbits -1)/64 < i/64) ) {
        	final long newdata = 1l<<bittoflip;
            addLiteralWord(newdata);
        } else {
            buffer[actualsizeinwords-1] |= 1l<<bittoflip;
            // check if we just completed a stream of 1s
            if(buffer[actualsizeinwords-1] == ~0l)  {
                // we remove the last dirty word
                buffer[actualsizeinwords-1] = 0;
                --actualsizeinwords;
                rlw.setNumberOfLiteralWords(rlw.getNumberOfLiteralWords()-1);
                // next we add one clean word
                addEmptyWord(true);
            }
        }
        sizeinbits = i+1;
    }


    /**
    * Adding words directly to the bitmap (for expert use).
    *
    * This is normally how you add data to the array. So you add
    * bits in streams of 8*8 bits.
    *
    * @returns the number of words added to the buffer
    */
    public int add(long  newdata) {
        return add(newdata,wordinbits);
    }

    /**
    * Adding words directly to the bitmap (for expert use).
    * You want to add many zeroes or ones?
    * This is the method you use.
    *
    *@returns the number of words added to the buffer
    */
    public int addStreamOfEmptyWords(boolean v, long number) {
        if(number == 0) return 0;
        final boolean noliteralword = (rlw.getNumberOfLiteralWords() == 0);
        final long runlen = rlw.getRunningLength();
        if( ( noliteralword ) && ( runlen == 0 )) {
            rlw.setRunningBit(v);
        }
        int wordsadded = 0;
        if( ( noliteralword ) && (rlw.getRunningBit() == v)
                && (runlen < rlw.largestrunninglengthcount) ) {
            long whatwecanadd = Math.min(number, rlw.largestrunninglengthcount-runlen);
            rlw.setRunningLength(runlen+whatwecanadd);
            sizeinbits += whatwecanadd*wordinbits;
            if(number - whatwecanadd> 0 ) wordsadded += addStreamOfEmptyWords(v, number - whatwecanadd);
        } else {
            push_back(0);
            ++wordsadded;
            rlw.position = actualsizeinwords - 1;
            final long whatwecanadd = Math.min(number, rlw.largestrunninglengthcount);
            rlw.setRunningBit(v);
            rlw.setRunningLength(whatwecanadd);
            sizeinbits += whatwecanadd*wordinbits;
            if(number - whatwecanadd> 0 ) wordsadded += addStreamOfEmptyWords(v, number - whatwecanadd);
        }
        return wordsadded;
    }

    /**
    * if you have several words to copy over, this might be faster.
    */
    private long addStreamOfDirtyWords(long[] data, long start, long number) {
        if(number == 0) return 0;
        final long NumberOfLiteralWords = rlw.getNumberOfLiteralWords();
        final long whatwecanadd = Math.min(number, rlw.largestliteralcount - NumberOfLiteralWords);
        rlw.setNumberOfLiteralWords(NumberOfLiteralWords+whatwecanadd);
        final long leftovernumber = number -whatwecanadd;
        final long oldsize = actualsizeinwords;
        push_back(data,(int)start,(int)whatwecanadd);
        sizeinbits += whatwecanadd*wordinbits;
        long wordsadded = whatwecanadd;
        if(leftovernumber>0) {
            push_back(0);
            rlw.position=actualsizeinwords - 1;
            ++wordsadded;
            wordsadded+=addStreamOfDirtyWords(data,start+whatwecanadd, leftovernumber);
        }
        return wordsadded;
    }

    /**
    * Adding words directly to the bitmap (for expert use).
    * 
    *
    * @returns the number of words added to the buffer
    */
    public int add(long  newdata, int bitsthatmatter) {
        sizeinbits += bitsthatmatter;
        if(newdata == 0) {
            return addEmptyWord(false);
        } else if (newdata == ~0l) {
            return addEmptyWord(true);
        } else {
            return addLiteralWord(newdata);
        }
    }

    /**
     * the size in bits of the *uncompressed* bitmap.
     */
    public int sizeInBits() {
        return sizeinbits;
    }

    /**
     * For expert use : change the reported size in bits of the *uncompressed* bitmap.
     */
    private void setSizeInBits(int size) {
        sizeinbits = size;
    }
    /**
     * Report the *compressed* size of the bitmap (equivalent to memory usage, after accounting for some overhead).
     */
    public int sizeInBytes() {
        return actualsizeinwords*8;
    }

    /**
     * For internal use (trading off memory for speed).
     */
    private void reserve(int size) {
        if(size>buffer.length) {
        	final long oldbuffer[] = buffer;
            buffer = new long[size];
            System.arraycopy(oldbuffer,0,buffer,0,oldbuffer.length);
            rlw.array = buffer;
        }
    }
    
    /**
     * For internal use
     */
    private void push_back(long data) {
        if(actualsizeinwords==buffer.length) {
        	final long oldbuffer[] = buffer;
            buffer = new long[oldbuffer.length * 2];
            System.arraycopy(oldbuffer,0,buffer,0,oldbuffer.length);
            rlw.array = buffer;
        }
        buffer[actualsizeinwords++] = data;
    }
    
    /**
     * For internal use
     */
    private void push_back(long[] data,int start, int number) {
        while(actualsizeinwords + number >=buffer.length) {
        	final long oldbuffer[] = buffer;
            buffer = new long[oldbuffer.length * 2];
            System.arraycopy(oldbuffer,0,buffer,0,oldbuffer.length);
            rlw.array = buffer;
        }
        System.arraycopy(data,start,buffer,actualsizeinwords,number);
        actualsizeinwords+=number;
    }
    
    /**
     * For internal use
     */
    private int addEmptyWord(boolean v) {
    	final boolean noliteralword = (rlw.getNumberOfLiteralWords() == 0);
    	final long runlen = rlw.getRunningLength();
        if( ( noliteralword ) && ( runlen == 0 )) {
            rlw.setRunningBit(v);
        }
        if( ( noliteralword ) && (rlw.getRunningBit() == v)
                && (runlen < rlw.largestrunninglengthcount) ) {
            rlw.setRunningLength(runlen+1);
            return 0;
        } else {
            push_back(0);
            rlw.position = actualsizeinwords - 1;
            rlw.setRunningBit(v);
            rlw.setRunningLength(1);
            return 1;
        }
    }
    
    /**
     * For internal use
     */
    private int addLiteralWord(long  newdata) {
    	final long numbersofar = rlw.getNumberOfLiteralWords();
        if(numbersofar >= rlw.largestliteralcount) {
            push_back(0);
            rlw.position = actualsizeinwords - 1;
            rlw.setNumberOfLiteralWords(1);
            push_back(newdata);
            return 2;
        }
        rlw.setNumberOfLiteralWords(numbersofar + 1);
        push_back(newdata);
        return 1;
    }



    /**
    * reports the number of bits set to true. Running time is proportional to compressed size (as reported by sizeInBytes).
    */
    public int cardinality() {
        int counter = 0;
        EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord rlw = i.next();
            if(rlw.getRunningBit()) {
                counter += wordinbits*rlw.getRunningLength();
            } else {
            }
            for(int j = 0; j<rlw.getNumberOfLiteralWords(); ++j) {
                long data = i.buffer()[i.dirtyWords()+j];
                for(int c= 0; c<wordinbits; ++c)
                    if((data & (1l<<c)) != 0) ++counter;
            }
        }
        return counter;
    }
    
    /**
     * A string describing the bitmap.
     */
    public String toString() {
        String ans = " EWAHCompressedBitmap, size in bits = "
                     + sizeinbits + " size in words = "+actualsizeinwords+ "\n";
        final EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord rlw = i.next();
            if(rlw.getRunningBit()) {
                ans+=rlw.getRunningLength()+" 1x11\n";
            } else {
                ans+=rlw.getRunningLength()+" 0x00\n";
            }
            ans+=rlw.getNumberOfLiteralWords()+" dirties\n";
        }
        return ans;
    }
    
    /**
     * A more detailed string describing the bitmap (useful for debugging).
     */
    public String toDebugString() {
        String ans = " EWAHCompressedBitmap, size in bits = "
                     + sizeinbits + " size in words = "+actualsizeinwords+ "\n";
        EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord rlw = i.next();
            if(rlw.getRunningBit()) {
                ans+=rlw.getRunningLength()+" 1x11\n";
            } else {
                ans+=rlw.getRunningLength()+" 0x00\n";
            }
            ans+=rlw.getNumberOfLiteralWords()+" dirties\n";
            for(int j = 0; j<rlw.getNumberOfLiteralWords(); ++j) {
                long data = i.buffer()[i.dirtyWords()+j];
                ans+="\t"+data+"\n";
            }
        }
        return ans;
    }

    public IntIterator intIterator() {
    	final EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        return new IntIterator() {
            int pos = 0;
            RunningLengthWord rlw = null;
            Vector<Integer> buffer = new Vector<Integer>();
            int bufferpos= 0;
            public boolean 	hasNext() {
                if(rlw == null)
                    if(!loadNextRLE())
                        return false;
                    else {
                        loadBuffer();
                        return buffer.size() > 0;
                    }
                else
                    return true;
            }
            private boolean loadNextRLE() {
                while(i.hasNext()) {
                    rlw = i.next();
                    if(rlw.getRunningBit() && (rlw.getRunningLength()>0) )
                        return true;
                    if(rlw.getNumberOfLiteralWords()>0)
                        return true;
                }
                return false;
            }
            private void loadBuffer() {
                buffer = new Vector<Integer>();
                bufferpos = 0;
                if(rlw.getRunningBit()) {
                    for(int j = 0; j<rlw.getRunningLength(); ++j) {
                        for(int c= 0; c<wordinbits; ++c)
                            buffer.add(new Integer(pos++));
                    }
                } else {
                    pos+=wordinbits*rlw.getRunningLength();
                }
                for(int j = 0; j<rlw.getNumberOfLiteralWords(); ++j) {
                	final long data = i.buffer()[i.dirtyWords()+j];
                    for(long c= 0; c<wordinbits; ++c) {
                        if( ((1l << c) & data) != 0) {
                            buffer.add(new Integer(pos));
                        }
                        ++pos;
                    }
                }
            }
            public int next() {
                if(buffer.size() == bufferpos+1)
                    rlw = null;
                return buffer.get(bufferpos++);
            }
            public void remove() {
                throw new RuntimeException("not implemented");
            }
        };
    }

    /**
    * iterate over the positions of the true values.
    */
    public Iterator<Integer> iterator() {
    	return new Iterator<Integer>() {
    		final private IntIterator under = intIterator();
    		
    		public Integer next() {
    			return under.next();
    		}
    		
    		public boolean hasNext() {
    			return under.hasNext();
    		}

			public void remove() {
				throw new UnsupportedOperationException("bitsets do not support remove");
			}
    	};
    }

    /**
    * get the locations of the true values as one vector.
    * (may use more memory than iterator())
    */
    public Vector<Integer> getPositions() {
    	final Vector<Integer> v = new Vector<Integer>();
        final EWAHIterator i = new EWAHIterator(buffer,actualsizeinwords);
        int pos = 0;
        while(i.hasNext()) {
            RunningLengthWord rlw = i.next();
            if(rlw.getRunningBit()) {
                for(int j = 0; j<rlw.getRunningLength(); ++j) {
                    for(int c= 0; c<wordinbits; ++c)
                        v.add(new Integer(pos++));
                }
            } else {
                pos+=wordinbits*rlw.getRunningLength();
            }
            for(int j = 0; j<rlw.getNumberOfLiteralWords(); ++j) {
            	final long data = i.buffer()[i.dirtyWords()+j];
                for(long c= 0; c<wordinbits; ++c) {
                    if( ((1l << c) & data) != 0) {
                        v.add(new Integer(pos));
                    }
                    ++pos;
                }
            }
        }
        while( (v.size()>0) && (v.lastElement().intValue() >= sizeinbits ))
            v.removeElementAt(v.size()-1);
        return v;
    }


    public Object clone() throws java.lang.CloneNotSupportedException {
    	final EWAHCompressedBitmap clone = (EWAHCompressedBitmap) super.clone();
        clone.buffer = this.buffer.clone();
        clone.actualsizeinwords = this.actualsizeinwords;
        clone.sizeinbits = this.sizeinbits;
        return clone;
    }

    public void	readExternal(ObjectInput in) throws IOException {
        sizeinbits = in.readInt();
        actualsizeinwords = in.readInt();
        buffer = new long[in.readInt()];
        for(int k = 0; k< actualsizeinwords; ++k)
            buffer[k] = in.readLong();
        rlw = new RunningLengthWord(buffer,actualsizeinwords-1);
    }

    public void	writeExternal(ObjectOutput out) throws IOException  {
        out.writeInt(sizeinbits);
        out.writeInt(actualsizeinwords);
        out.writeInt(buffer.length);
        for(int k = 0; k< actualsizeinwords; ++k)
            out.writeLong(buffer[k]);
    }


    static final int defaultbuffersize = 512;
    long buffer[] = new long[defaultbuffersize];
    int actualsizeinwords = 1;
    int sizeinbits = 0;
    RunningLengthWord rlw = new RunningLengthWord(buffer,0);
    public static final int wordinbits = 8*8;


}