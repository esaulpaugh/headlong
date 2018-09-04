package com.esaulpaugh.headlong.abi.beta;

abstract class StaticType<V> extends StackableType<V> {

    StaticType(String canonicalType, boolean dynamic) {
        super(canonicalType, dynamic);
    }

    @Override
    V decode(byte[] buffer, int index) {
        return decodeStatic(buffer, index);
    }

    abstract V decodeStatic(byte[] buffer, int index);

}
