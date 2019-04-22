package com.esaulpaugh.headlong.example;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;

public interface RLPAdapter<T> {

    // default interface methods not supported on Android except Android N+
//    default T decode(byte[] rlp) throws DecodeException {
//        return decode(rlp, 0);
//    }

    T decode(byte[] rlp, int index) throws DecodeException;

    byte[] encode(T t);

}
