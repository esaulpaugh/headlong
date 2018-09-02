package com.esaulpaugh.headlong.abi.beta.type.array;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;

/**
 * Represents any array with a variable byte-length, whether because the array length is not fixed or it contains at
 * least one variable-length (dynamic) element.
 */
public class DynamicArrayType<T extends StackableType, E> extends ArrayType<T, E> {

    public static final int DYNAMIC_LENGTH = -1;

    public DynamicArrayType(String canonicalAbiType, String className, T elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
    }
}
