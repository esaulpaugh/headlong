package com.esaulpaugh.headlong.abi.beta.type;

/**
 * Represents any array with a variable byte-length, whether because the array length is not fixed or it contains at
 * least one variable-length (dynamic) element.
 */
class DynamicArray extends Array {

    static final int DYNAMIC_LENGTH = -1;

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
    }
}
