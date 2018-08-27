package com.esaulpaugh.headlong.abi.beta.type;

class DynamicArray extends Array {

//    private int dynamicLen;

    // TODO test DynamicArray (e.g. [4] w/ dynamic element) enforces specified len
//    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType) {
//        this(canonicalAbiType, className, elementType, -1);
//    }

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
    }

//    public void setDynamicLen(int dynamicLen) {
//        this.dynamicLen = dynamicLen;
//    }
}