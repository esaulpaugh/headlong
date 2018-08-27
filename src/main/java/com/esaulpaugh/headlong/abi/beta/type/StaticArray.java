package com.esaulpaugh.headlong.abi.beta.type;

class StaticArray extends Array {

    protected StaticArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length);
    }
}
