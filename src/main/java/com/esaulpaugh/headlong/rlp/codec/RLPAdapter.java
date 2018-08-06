package com.esaulpaugh.headlong.rlp.codec;

import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;

public interface RLPAdapter<T> {

    T fromRLP(byte[] rlp) throws DecodeException;

    byte[] toRLP(T t);

}
