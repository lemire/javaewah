package com.googlecode.javaewah;

interface Buffer {
    
    int sizeInWords();
    
    long getWord(int position);
    
    long getLastWord();
    
    long[] getWords();
    
    void clear();
    
    void trim();
    
    void setWord(int position, long word);
    
    void setLastWord(long word);

    void push_back(long data);

    void push_back(long[] data, int start, int number);

    void negative_push_back(long[] data, int start, int number);

    void removeLastWord();
    
    void negateWord(int position);
    
    void andWord(int position, long mask);
    
    void orWord(int position, long mask);
    
    void andLastWord(long mask);
    
    void orLastWord(long mask);    
    
    void expand(int position, int length);
    
    void collapse(int position, int length);
    
    Buffer clone();

}
