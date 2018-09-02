package com.esaulpaugh.headlong.abi.beta;

/**
 * Represents any array with a variable byte-length, whether because the array length is not fixed or it contains at
 * least one variable-length (dynamic) element.
 */
class DynamicArrayType<T extends StackableType, E> extends ArrayType<T, E> {

    static final int DYNAMIC_LENGTH = -1;

    DynamicArrayType(String canonicalAbiType, String className, T elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
    }
}
