package com.esaulpaugh.headlong.rlp.codec;

public interface RLPAdapter<T> {

    T fromRLP(byte[] rlp);

    byte[] toRLP(T t);

}
