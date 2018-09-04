package com.esaulpaugh.headlong.abi.beta;

/**
 * Represents any array of a fixed length whose elements are all of fixed lengths.
 */
class StaticArrayType<T extends StackableType, A> extends ArrayType<T, A> {

    StaticArrayType(String canonicalType, String className, String arrayClassNameStub, T elementType, int length) {
        super(canonicalType, className, arrayClassNameStub, elementType, length, false);
        if(length < 0) {
            throw new NegativeArraySizeException();
        }
    }
}
