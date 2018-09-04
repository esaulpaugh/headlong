package com.esaulpaugh.headlong.abi.beta;

/**
 * Represents any array with a variable byte-length, whether because the array length is not fixed or it contains
 * variable-length (dynamic) elements.
 *
 */
class DynamicArrayType<T extends StackableType, E> extends ArrayType<T, E> {

    static final int DYNAMIC_LENGTH = -1;

    DynamicArrayType(String canonicalType, String className, String arrayClassNameStub, T elementType, int length) {
        super(canonicalType, className, arrayClassNameStub, elementType, length, true);
        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
    }
}
