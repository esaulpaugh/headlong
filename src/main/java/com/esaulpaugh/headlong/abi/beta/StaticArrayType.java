package com.esaulpaugh.headlong.abi.beta;

/**
 * Represents any array of a fixed length whose elements are all of fixed lengths.
 */
class StaticArrayType<T extends StackableType, E> extends ArrayType<T, E> {

    StaticArrayType(String canonicalAbiType, String className, T elementType, int length) {
        super(canonicalAbiType, className, elementType, length);
        if(length < 0) {
            throw new NegativeArraySizeException();
        }
    }
}
