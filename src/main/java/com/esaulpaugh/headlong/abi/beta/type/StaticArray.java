package com.esaulpaugh.headlong.abi.beta.type;

class StaticArray extends Array {

//    private final StackableType elementType;
//        private final int length;

    protected StaticArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length);
//        this.elementType = elementType;
//            this.length = length;
    }

    @Override
    int byteLength(Object value) {

        return getDataLen(value); // , false

//        return -3;
    }
}
