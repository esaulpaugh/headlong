package com.esaulpaugh.headlong.abi.beta.type;

class DynamicArray extends Array {

    static final int DYNAMIC_LENGTH = -1;

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
    }
}