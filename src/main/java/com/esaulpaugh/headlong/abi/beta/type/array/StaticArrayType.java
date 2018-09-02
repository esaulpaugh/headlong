package com.esaulpaugh.headlong.abi.beta.type.array;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;

/**
 * Represents any array of a fixed length whose elements are all of fixed lengths.
 */
public class StaticArrayType<T extends StackableType, E> extends ArrayType<T, E> {

    public StaticArrayType(String canonicalAbiType, String className, T elementType, int length) {
        super(canonicalAbiType, className, elementType, length);
        if(length < 0) {
            throw new NegativeArraySizeException();
        }
    }

//    @Override
//    @SuppressWarnings("unchecked")
//    public E[] decode(byte[] bytes, int index) {
//        final int arrayLen = ARRAY_LENGTH_TYPE.decode(bytes, index);
//        if(arrayLen != length) {
//            throw new IllegalArgumentException("length mismatch, actual != expected: " + arrayLen + " != " + length);
//        }
//        final int end = index + arrayLen;
//        int elementIndex = index + AbstractInt256Type.INT_LENGTH_BYTES;
//        Object[] objects = new Object[arrayLen];
//        for(int i = 0; i < end; i++, elementIndex += AbstractInt256Type.INT_LENGTH_BYTES) {
//            objects[i] = elementType.decode(bytes, elementIndex);
//        }
//        return (E[]) objects;
//    }
}
