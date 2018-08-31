package com.esaulpaugh.headlong.abi.beta.type;

class DynamicArray extends Array {

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
    }
}