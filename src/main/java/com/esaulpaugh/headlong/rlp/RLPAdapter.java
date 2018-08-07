package com.esaulpaugh.headlong.rlp;

public interface RLPAdapter<T> {

    T fromRLP(byte[] rlp) throws DecodeException;

    byte[] toRLP(T t);

}
