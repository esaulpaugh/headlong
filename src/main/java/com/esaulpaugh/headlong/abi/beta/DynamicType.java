package com.esaulpaugh.headlong.abi.beta;

abstract class DynamicType<V> extends StackableType<V> {

    DynamicType(String canonicalType, boolean dynamic) {
        super(canonicalType, dynamic);
    }

    @Override
    V decode(byte[] buffer, int index) {
//        return decodeDynamic(buffer, index, new int[1]);
        throw new UnsupportedOperationException("use decodeDynamic");
    }

    abstract V decodeDynamic(byte[] buffer, int index, int[] returnIndex);
}
