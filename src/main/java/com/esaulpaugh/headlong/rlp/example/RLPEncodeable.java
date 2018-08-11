package com.esaulpaugh.headlong.rlp.example;

public interface RLPEncodeable {

    Object[] toObjectArray();

    byte[] toRLP();

    void toRLP(byte[] dest, int destIndex);

}
