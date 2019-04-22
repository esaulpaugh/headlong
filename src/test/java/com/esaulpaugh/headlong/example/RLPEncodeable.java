package com.esaulpaugh.headlong.example;

public interface RLPEncodeable {

    Object[] toObjectArray();

    byte[] toRLP();

    void toRLP(byte[] dest, int destIndex);

}
