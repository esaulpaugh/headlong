package com.esaulpaugh.headlong.abi.beta.type;

class Byte extends StackableType {

    protected static final StackableType BYTE_OBJECT = new Byte("uint8", StackableType.CLASS_NAME_BYTE);
    protected static final StackableType BYTE_PRIMITIVE = new Byte("uint8", "B");

    Byte(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, 1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    int byteLength(Object value) {
        return 1;
    }

    @Override
    protected void validate(Object value) {

    }
}
