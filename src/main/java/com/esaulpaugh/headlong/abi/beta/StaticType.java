package com.esaulpaugh.headlong.abi.beta;

abstract class StaticType<V> extends StackableType<V> {

    StaticType(String canonicalType, boolean dynamic) {
        super(canonicalType, dynamic);
    }
}
