package javaewah;


/*
* Copyright 2009-2011, Daniel Lemire
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

	/**
	 * Creates an empty bitmap (not bit set to true).
	 */
    public EWAHCompressedBitmap () {
    	this.buffer = new long[defaultbuffersize];
    	this.rlw = new RunningLengthWord(this.buffer,0);
    }
    
    /**
     * Sets explicitly the buffer size (in 64-bit words). The initial memory
     * usage will be "buffersize * 64". For large poorly compressible bitmaps,
     * using large values may improve performance.
     * 
     * @param buffersize
     */
    public EWAHCompressedBitmap (final int buffersize) {
    	this.buffer = new long[buffersize];
    	this.rlw = new RunningLengthWord(this.buffer,0);
    }

    private EWAHIterator getEWAHIterator() {
        return new EWAHIterator(this.buffer,this.actualsizeinwords);
    }
    
    
    
    /**
    * Returns a new compressed bitmap contained the bitwise XOR values of the current bitmap with some other bitmap.
    * 
    * The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
    */
    public EWAHCompressedBitmap xor(final EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {// this never happens...
            container.sizeinbits = sizeInBits();
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
            final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
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
        container.sizeinbits = Math.max(sizeInBits(), a.sizeInBits());
        return container;
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise AND values of the current bitmap with some other bitmap.
     * 
     *  The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     */
    public EWAHCompressedBitmap and(final EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {// this never happens...
            container.sizeinbits = sizeInBits();
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
            final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
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
        container.sizeinbits = Math.max(sizeInBits(), a.sizeInBits());
        return container;
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise AND NOT values of the current bitmap with some other bitmap.
     * 
     *  The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     */
    public EWAHCompressedBitmap andNot(final EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords > a.actualsizeinwords ? this.actualsizeinwords : a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {// this never happens...
            container.sizeinbits = sizeInBits();
            return container;
        }
        // at this point, this is safe:
        BufferedRunningLengthWord rlwi = new BufferedRunningLengthWord(i.next());
        rlwi.setRunningBit(! rlwi.getRunningBit());
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
                if(i_is_prey)
                	container.addStreamOfDirtyWords( j.buffer(), dw_predator, preyrl - tobediscarded);
                else 
                	container.addStreamOfNegatedDirtyWords( i.buffer(), dw_predator, preyrl - tobediscarded);
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
                    if(i_is_prey)
                    	container.addStreamOfNegatedDirtyWords( i.buffer(), dw_prey, tobediscarded);
                    else 
                    	container.addStreamOfDirtyWords(j.buffer(), dw_prey, tobediscarded);
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                }
            }
            // all that is left to do now is to AND the dirty words
            final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
            if(nbre_dirty_prey > 0) {
                for(int k = 0; k<nbre_dirty_prey; ++k) {
                    if(i_is_prey)
                        container.add( (~ i.buffer()[prey.dirtywordoffset + i.dirtyWords()+k])
                                      & j.buffer()[predator.dirtywordoffset + j.dirtyWords()+k]);
                    else
                        container.add((~ i.buffer()[predator.dirtywordoffset + i.dirtyWords()+k])
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
                rlwi.setRunningBit(! rlwi.getRunningBit());
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
            discharge(rlwj, j, container);
        container.sizeinbits = Math.max(sizeInBits(), a.sizeInBits());
        return container;
    }


	/**
    * Negate (bitwise) the current bitmap. To get a negated copy, do ((EWAHCompressedBitmap) mybitmap.clone()).not();
    * 
    *     The running time is proportionnal to the compressed size (as reported by sizeInBytes()).
    *
    * LIMITATION (FIXME): in the current version, if you negate
    * a bitmap, and then iterator through the set bits, you may find
    * true bits beyond the sizeInBits range.
    *
    */
    public void not() {
    	final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        while(i.hasNext()) {
        	final RunningLengthWord rlw1 = i.next();
            rlw1.setRunningBit(! rlw1.getRunningBit());
            for(int j = 0; j<rlw1.getNumberOfLiteralWords(); ++j) {
                i.buffer()[i.dirtyWords()+j] = ~i.buffer()[i.dirtyWords()+j];
            }
        }
    }
    
    /**
     * Returns a new compressed bitmap contained the bitwise OR values of the current bitmap with some other bitmap.
     * 
     *  The running time is proportionnal to the sum of the compressed sizes (as reported by sizeInBytes()).
     */
    public EWAHCompressedBitmap or(final EWAHCompressedBitmap a) {
    	final EWAHCompressedBitmap container  = new EWAHCompressedBitmap();
        container.reserve(this.actualsizeinwords + a.actualsizeinwords);
        final EWAHIterator i = a.getEWAHIterator();
        final EWAHIterator j = getEWAHIterator();
        if(!(i.hasNext() && j.hasNext())) {//  this never happens...
            container.sizeinbits = sizeInBits();
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
                	final long tobediscarded = (predatorrl >= nbre_dirty_prey) ? nbre_dirty_prey : predatorrl;
                    container.addStreamOfEmptyWords(true, tobediscarded);
                    predator.discardFirstWords(tobediscarded);
                    prey.discardFirstWords(tobediscarded);
                }
            }
            // all that is left to do now is to OR the dirty words
            final long nbre_dirty_prey = prey.getNumberOfLiteralWords();
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
        container.sizeinbits=Math.max(sizeInBits(), a.sizeInBits());
        return container;
    }

    
    /**
     *  For internal use.
     */
    private void discharge(final BufferedRunningLengthWord initialWord, final EWAHIterator iterator,
    		final EWAHCompressedBitmap container) {
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
    private void dischargeAsEmpty(final BufferedRunningLengthWord initialWord, final EWAHIterator iterator,
    		final EWAHCompressedBitmap container) {
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
    * 
    * @returns true if the value was set (always true when i>= sizeInBits()).
    */
    public boolean set(final int i) {
    	if(i<this.sizeinbits)
    		return false;
        // must I complete a word?
        if ( (this.sizeinbits % 64) != 0) {
        	final int possiblesizeinbits = (this.sizeinbits /64)*64 + 64;
            if(possiblesizeinbits<i+1) {
                this.sizeinbits = possiblesizeinbits;
            }
        }
        addStreamOfEmptyWords(false, (i/64) - this.sizeinbits/64);
        final int bittoflip = i-(this.sizeinbits/64 * 64);
        // next, we set the bit
        if(( this.rlw.getNumberOfLiteralWords() == 0) || ((this.sizeinbits -1)/64 < i/64) ) {
        	final long newdata = 1l<<bittoflip;
            addLiteralWord(newdata);
        } else {
            this.buffer[this.actualsizeinwords-1] |= 1l<<bittoflip;
            // check if we just completed a stream of 1s
            if(this.buffer[this.actualsizeinwords-1] == ~0l)  {
                // we remove the last dirty word
                this.buffer[this.actualsizeinwords-1] = 0;
                --this.actualsizeinwords;
                this.rlw.setNumberOfLiteralWords(this.rlw.getNumberOfLiteralWords()-1);
                // next we add one clean word
                addEmptyWord(true);
            }
        }
        this.sizeinbits = i+1;
        return true;
    }


    /**
    * Adding words directly to the bitmap (for expert use).
    *
    * This is normally how you add data to the array. So you add
    * bits in streams of 8*8 bits.
    *
    * @returns the number of words added to the buffer
    */
    public int add(final long  newdata) {
        return add(newdata,wordinbits);
    }

    /**
    * Adding words directly to the bitmap (for expert use).
    * You want to add many zeroes or ones?
    * This is the method you use.
    *
    *@returns the number of words added to the buffer
    */
    public int addStreamOfEmptyWords(final boolean v, final long number) {
        if(number == 0) return 0;
        final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
        final long runlen = this.rlw.getRunningLength();
        if( ( noliteralword ) && ( runlen == 0 )) {
            this.rlw.setRunningBit(v);
        }
        int wordsadded = 0;
        if( ( noliteralword ) && (this.rlw.getRunningBit() == v)
                && (runlen < RunningLengthWord.largestrunninglengthcount) ) {
            long whatwecanadd = Math.min(number, RunningLengthWord.largestrunninglengthcount-runlen);
            this.rlw.setRunningLength(runlen+whatwecanadd);
            this.sizeinbits += whatwecanadd*wordinbits;
            if(number - whatwecanadd> 0 ) wordsadded += addStreamOfEmptyWords(v, number - whatwecanadd);
        } else {
            push_back(0);
            ++wordsadded;
            this.rlw.position = this.actualsizeinwords - 1;
            final long whatwecanadd = Math.min(number, RunningLengthWord.largestrunninglengthcount);
            this.rlw.setRunningBit(v);
            this.rlw.setRunningLength(whatwecanadd);
            this.sizeinbits += whatwecanadd*wordinbits;
            if(number - whatwecanadd> 0 ) wordsadded += addStreamOfEmptyWords(v, number - whatwecanadd);
        }
        return wordsadded;
    }
    
    /**
     * Same as addStreamOfDirtyWords, but the words are negated.
     */
    private long addStreamOfNegatedDirtyWords(final long[] data, final long start, final long number) {
        if(number == 0) return 0;
        final long NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
        final long whatwecanadd = Math.min(number, RunningLengthWord.largestliteralcount - NumberOfLiteralWords);
        this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords+whatwecanadd);
        final long leftovernumber = number -whatwecanadd;
        negative_push_back(data,(int)start,(int)whatwecanadd);
        this.sizeinbits += whatwecanadd*wordinbits;
        long wordsadded = whatwecanadd;
        if(leftovernumber>0) {
            push_back(0);
            this.rlw.position=this.actualsizeinwords - 1;
            ++wordsadded;
            wordsadded+=addStreamOfDirtyWords(data,start+whatwecanadd, leftovernumber);
        }
        return wordsadded;
	}

    /**
    * if you have several words to copy over, this might be faster.
    */
    private long addStreamOfDirtyWords(final long[] data, final long start, final long number) {
        if(number == 0) return 0;
        final long NumberOfLiteralWords = this.rlw.getNumberOfLiteralWords();
        final long whatwecanadd = Math.min(number, RunningLengthWord.largestliteralcount - NumberOfLiteralWords);
        this.rlw.setNumberOfLiteralWords(NumberOfLiteralWords+whatwecanadd);
        final long leftovernumber = number -whatwecanadd;
        push_back(data,(int)start,(int)whatwecanadd);
        this.sizeinbits += whatwecanadd*wordinbits;
        long wordsadded = whatwecanadd;
        if(leftovernumber>0) {
            push_back(0);
            this.rlw.position=this.actualsizeinwords - 1;
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
    public int add(final long  newdata, final int bitsthatmatter) {
        this.sizeinbits += bitsthatmatter;
        if(newdata == 0) {
            return addEmptyWord(false);
        } else if (newdata == ~0l) {
            return addEmptyWord(true);
        } else {
            return addLiteralWord(newdata);
        }
    }

    /**
     * Returns the size in bits of the *uncompressed* bitmap represented by this
     * compressed bitmap. Initially, the sizeInBits is zero. It is extended
     * automatically when  you set bits to true.
     */
    public int sizeInBits() {
        return this.sizeinbits;
    }

    /**
     * Change the reported size in bits of the *uncompressed* bitmap represented by
     * this compressed bitmap. It is not possible to reduce the sizeInBits, but it can be extended.
     * The new bits are set to false or true depending on the value of defaultvalue.
     * 
     * @returns true if the update was possible
     */
    public boolean setSizeInBits(final int size, final boolean defaultvalue) {
        if(size < this.sizeinbits) return false;
        // next loop could be optimized further
        if(defaultvalue)
        	while (  ((this.sizeinbits % 64) != 0) && (this.sizeinbits < size)) {
        		this.set(this.sizeinbits);
        	}
        final int leftover = size % 64; 
        if(defaultvalue == false)
        	this.addStreamOfEmptyWords(defaultvalue, (size/64) - this.sizeinbits/64 + (leftover !=0 ? 1 : 0) );
        else {
        	this.addStreamOfEmptyWords(defaultvalue, (size/64) - this.sizeinbits/64  );
        	final long newdata = (1l<<leftover) + ((1l<<leftover) - 1);
        	this.addLiteralWord(newdata);
        }
        this.sizeinbits = size;
        return true;
    }
    /**
     * Report the *compressed* size of the bitmap (equivalent to memory usage, after accounting for some overhead).
     */
    public int sizeInBytes() {
        return this.actualsizeinwords*8;
    }

    /**
     * For internal use (trading off memory for speed).
     * 
     * @returns True if the operation was a success.
     */
    private boolean reserve(final int size) {
        if(size>this.buffer.length) {
        	final long oldbuffer[] = this.buffer;
            this.buffer = new long[size];
            System.arraycopy(oldbuffer,0,this.buffer,0,oldbuffer.length);
            this.rlw.array = this.buffer;
            return true;
        }
		return false;
    }
    
    /**
     * For internal use
     */
    private void push_back(final long data) {
        if(this.actualsizeinwords==this.buffer.length) {
        	final long oldbuffer[] = this.buffer;
            this.buffer = new long[oldbuffer.length * 2];
            System.arraycopy(oldbuffer,0,this.buffer,0,oldbuffer.length);
            this.rlw.array = this.buffer;
        }
        this.buffer[this.actualsizeinwords++] = data;
    }
    
 
    
    /**
     * For internal use
     */
    private void push_back(final long[] data,final int start, final int number) {
        while(this.actualsizeinwords + number >=this.buffer.length) {
        	final long oldbuffer[] = this.buffer;
            this.buffer = new long[oldbuffer.length * 2];
            System.arraycopy(oldbuffer,0,this.buffer,0,oldbuffer.length);
            this.rlw.array = this.buffer;
        }
        System.arraycopy(data,start,this.buffer,this.actualsizeinwords,number);
        this.actualsizeinwords+=number;
    }
    /**
     * For internal use
     */
    private void negative_push_back(final long[] data,final int start, final int number) {
        while(this.actualsizeinwords + number >=this.buffer.length) {
        	final long oldbuffer[] = this.buffer;
            this.buffer = new long[oldbuffer.length * 2];
            System.arraycopy(oldbuffer,0,this.buffer,0,oldbuffer.length);
            this.rlw.array = this.buffer;
        }
        for(int k = 0; k<number;++k)
        	this.buffer[this.actualsizeinwords+k] = ~ data[start+k];
        this.actualsizeinwords+=number;
    }
    
    /**
     * For internal use
     */
    private int addEmptyWord(final boolean v) {
    	final boolean noliteralword = (this.rlw.getNumberOfLiteralWords() == 0);
    	final long runlen = this.rlw.getRunningLength();
        if( ( noliteralword ) && ( runlen == 0 )) {
            this.rlw.setRunningBit(v);
        }
        if( ( noliteralword ) && (this.rlw.getRunningBit() == v)
                && (runlen < RunningLengthWord.largestrunninglengthcount) ) {
            this.rlw.setRunningLength(runlen+1);
            return 0;
        } 
        push_back(0);
        this.rlw.position = this.actualsizeinwords - 1;
        this.rlw.setRunningBit(v);
        this.rlw.setRunningLength(1);
        return 1;
    }
    
    /**
     * For internal use
     */
    private int addLiteralWord(final long  newdata) {
    	final long numbersofar = this.rlw.getNumberOfLiteralWords();
        if(numbersofar >= RunningLengthWord.largestliteralcount) {
            push_back(0);
            this.rlw.position = this.actualsizeinwords - 1;
            this.rlw.setNumberOfLiteralWords(1);
            push_back(newdata);
            return 2;
        }
        this.rlw.setNumberOfLiteralWords(numbersofar + 1);
        push_back(newdata);
        return 1;
    }



    /**
    * reports the number of bits set to true. Running time is proportional to compressed size (as reported by sizeInBytes).
    */
    public int cardinality() {
    	int counter = 0;
    	final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord localrlw = i.next();
            if(localrlw.getRunningBit()) {
                counter += wordinbits*localrlw.getRunningLength();
            }
            for(int j = 0; j<localrlw.getNumberOfLiteralWords(); ++j) {
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
    @Override
	public String toString() {
    	String ans = " EWAHCompressedBitmap, size in bits = "
                     + this.sizeinbits + " size in words = "+this.actualsizeinwords+ "\n";
        final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord localrlw = i.next();
            if(localrlw.getRunningBit()) {
                ans+=localrlw.getRunningLength()+" 1x11\n";
            } else {
                ans+=localrlw.getRunningLength()+" 0x00\n";
            }
            ans+=localrlw.getNumberOfLiteralWords()+" dirties\n";
        }
        return ans;
    }
    
    /**
     * A more detailed string describing the bitmap (useful for debugging).
     */
    public String toDebugString() {
        String ans = " EWAHCompressedBitmap, size in bits = "
                     + this.sizeinbits + " size in words = "+this.actualsizeinwords+ "\n";
        final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        while(i.hasNext()) {
            RunningLengthWord localrlw = i.next();
            if(localrlw.getRunningBit()) {
                ans+=localrlw.getRunningLength()+" 1x11\n";
            } else {
                ans+=localrlw.getRunningLength()+" 0x00\n";
            }
            ans+=localrlw.getNumberOfLiteralWords()+" dirties\n";
            for(int j = 0; j<localrlw.getNumberOfLiteralWords(); ++j) {
                long data = i.buffer()[i.dirtyWords()+j];
                ans+="\t"+data+"\n";
            }
        }
        return ans;
    }

    public IntIterator intIterator() {
    	final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        return new IntIterator() {
            int pos = 0;
            RunningLengthWord localrlw = null;
            final static int initcapacity = 512;
            int[] localbuffer = new int[initcapacity ];
            int localbuffersize = 0;
            int bufferpos= 0;
            public boolean 	hasNext() {
                while(this.localbuffersize==0) {
                    	if(!loadNextRLE())
                    		return false;
                    	loadBuffer();
                }
                return true;
            }
            private boolean loadNextRLE() {
                while(i.hasNext()) {
                    this.localrlw = i.next();
                    return true;
                }
                return false;
            }
            
            private void add(final int val) {
            	++this.localbuffersize;
            	while(this.localbuffersize>this.localbuffer.length) {
            		int[] oldbuffer = this.localbuffer;
            		this.localbuffer = new int[this.localbuffer.length * 2];
            		System.arraycopy(oldbuffer, 0, this.localbuffer, 0, oldbuffer.length);
            	}
            	this.localbuffer[this.localbuffersize - 1] = val;
            }
            private void loadBuffer() {
            	this.bufferpos = 0;
            	this.localbuffersize=0;
                if(this.localrlw.getRunningBit()) {
                    for(int j = 0; j<this.localrlw.getRunningLength(); ++j) {
                        for(int c= 0; c<wordinbits; ++c) {
                        	add(this.pos++);
                        }
                    }
                } else {
                    this.pos+=wordinbits*this.localrlw.getRunningLength();
                }
                for(int j = 0; j<this.localrlw.getNumberOfLiteralWords(); ++j) {
                	final long data = i.buffer()[i.dirtyWords()+j];
                    for(long c= 0; c<wordinbits; ++c) {
                        if( ((1l << c) & data) != 0) {
                        	add(this.pos);
                        }
                        ++this.pos;
                    }
                }
            }
            public int next() {
            	final int answer = this.localbuffer[this.bufferpos++];
                if(this.localbuffersize == this.bufferpos) {
                	this.localbuffersize=0;
                }
                return answer ;
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
    			return new Integer(this.under.next());
    		}
    		
    		public boolean hasNext() {
    			return this.under.hasNext();
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
    public List<Integer> getPositions() {
    	final ArrayList<Integer> v = new ArrayList<Integer>();
        final EWAHIterator i = new EWAHIterator(this.buffer,this.actualsizeinwords);
        int pos = 0;
        while(i.hasNext()) {
            RunningLengthWord localrlw = i.next();
            if(localrlw.getRunningBit()) {
                for(int j = 0; j<localrlw.getRunningLength(); ++j) {
                    for(int c= 0; c<wordinbits; ++c)
                        v.add(new Integer(pos++));
                }
            } else {
                pos+=wordinbits*localrlw.getRunningLength();
            }
            for(int j = 0; j<localrlw.getNumberOfLiteralWords(); ++j) {
            	final long data = i.buffer()[i.dirtyWords()+j];
                for(long c= 0; c<wordinbits; ++c) {
                    if( ((1l << c) & data) != 0) {
                        v.add(new Integer(pos));
                    }
                    ++pos;
                }
            }
        }
        while( (v.size()>0) && (v.get(v.size()-1).intValue() >= this.sizeinbits ))
            v.remove(v.size()-1);
        return v;
    }
    
    

    @Override
    public boolean equals(Object o) {
    	if (o instanceof EWAHCompressedBitmap) {
    		EWAHCompressedBitmap other = (EWAHCompressedBitmap) o;
    		return sizeinbits == other.sizeinbits &&
    			actualsizeinwords == other.actualsizeinwords &&
    			rlw.position == other.rlw.position &&
    			Arrays.equals(buffer, other.buffer);
    	} else {
    		return false;
    	}
    }
    
    @Override
	public Object clone() throws java.lang.CloneNotSupportedException {
    	final EWAHCompressedBitmap clone = (EWAHCompressedBitmap) super.clone();
        clone.buffer = this.buffer.clone();
        clone.actualsizeinwords = this.actualsizeinwords;
        clone.sizeinbits = this.sizeinbits;
        return clone;
    }

    public void	readExternal(ObjectInput in) throws IOException {
        deserialize(in);
    }
    
    public void deserialize(DataInput in) throws IOException {
    	this.sizeinbits = in.readInt();
        this.actualsizeinwords = in.readInt();
        this.buffer = new long[in.readInt()];
        for(int k = 0; k< this.actualsizeinwords; ++k)
            this.buffer[k] = in.readLong();
        this.rlw = new RunningLengthWord(this.buffer,in.readInt());
    }

    public void	writeExternal(ObjectOutput out) throws IOException  {
        serialize(out);
    }
    
    public void serialize(DataOutput out) throws IOException {
    	out.writeInt(this.sizeinbits);
        out.writeInt(this.actualsizeinwords);
        out.writeInt(this.buffer.length);
        for(int k = 0; k< this.actualsizeinwords; ++k)
            out.writeLong(this.buffer[k]);
        out.writeInt(this.rlw.position);
    }


    static final int defaultbuffersize = 4;
    long buffer[] = null;
    int actualsizeinwords = 1;
    int sizeinbits = 0;
    RunningLengthWord rlw = null;
    public static final int wordinbits = 64;


}
