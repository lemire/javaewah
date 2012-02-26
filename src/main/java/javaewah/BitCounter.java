package javaewah;

public class BitCounter implements BitmapStorage {

  private int oneBits;
  
  @Override
  public int add(final long newdata) {
    oneBits += Long.bitCount(newdata);
    return 0;
  }

  @Override
  public int addStreamOfEmptyWords(boolean v, long number) {
    if (v) {
      oneBits += number * EWAHCompressedBitmap.wordinbits;
    }
    return 0;
  }

  @Override
  public long addStreamOfDirtyWords(long[] data, long start, long number) {
    for(int i=(int)start;i<start+number;i++) {
      add(data[i]);      
    }
    return 0;
  }

  @Override
  public void setSizeInBits(int bits) {
    // no action
  }

  public int getCount() {
    return oneBits;
  }
  
}
