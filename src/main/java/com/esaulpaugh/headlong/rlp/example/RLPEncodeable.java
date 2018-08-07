package com.esaulpaugh.headlong.rlp.example;

public interface RLPEncodeable<T> {

    Object[] toObjectArray();

    byte[] toRLP();

    void toRLP(byte[] dest, int destIndex);

}
