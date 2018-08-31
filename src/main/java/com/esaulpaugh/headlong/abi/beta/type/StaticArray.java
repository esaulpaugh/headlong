package com.esaulpaugh.headlong.abi.beta.type;

/**
 * Represents any array of a fixed length whose elements are all of fixed lengths.
 */
class StaticArray extends Array {

    protected StaticArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length);
        if(length < 0) {
            throw new NegativeArraySizeException();
        }
    }
}
