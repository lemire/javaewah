package com.googlecode.javaewah32;

interface Buffer {
    
    int sizeInWords();
    
    int getWord(int position);
    
    int getLastWord();
    
    int[] getWords();
    
    void clear();
    
    void trim();
    
    void setWord(int position, int word);
    
    void setLastWord(int word);

    void push_back(int data);

    void push_back(int[] data, int start, int number);

    void negative_push_back(int[] data, int start, int number);

    void removeLastWord();
    
    void negateWord(int position);
    
    void andWord(int position, int mask);
    
    void orWord(int position, int mask);
    
    void andLastWord(int mask);
    
    void orLastWord(int mask);
    
    void expand(int position, int length);
    
    void collapse(int position, int length);
    
    Buffer clone();

}
