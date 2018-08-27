package com.esaulpaugh.headlong.abi.beta.type;

import static com.esaulpaugh.headlong.abi.beta.type.Array.roundUp;

class Byte extends Int256 {

    protected static final StackableType BYTE_OBJECT = new Byte("uint8", StackableType.CLASS_NAME_BYTE);
    protected static final StackableType BYTE_PRIMITIVE = new Byte("uint8", "B");

    Byte(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, 8);
    }

    private Byte(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className, bitLength);
    }

    static Byte booleanType(String canonicalAbiType, String className) {
        return new Byte(canonicalAbiType, className, 1);
    }

    @Override
    int byteLength(Object value) {
        return roundUp(1);
    }

    @Override
    protected void validate(Object value) {
        super.validate(value);
        if(bitLength == 1) {
            if(!(value instanceof Boolean)) {
                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
            }
        }
    }
}
