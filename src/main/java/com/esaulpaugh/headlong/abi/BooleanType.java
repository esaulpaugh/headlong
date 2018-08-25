package com.esaulpaugh.headlong.abi;

class BooleanType extends Type {

    private BooleanType(String canonicalAbiType, String javaClassName) {
        super(canonicalAbiType, javaClassName, false);
    }

    static BooleanType create(String canonicalAbiType, String javaClassName) {
        return new BooleanType(canonicalAbiType, javaClassName);
    }

    @Override
    public Integer getDataByteLen(Object value) {
        return 32;
    }

//    @Override
//    public void validate(Object value) {
//        super.validate(value);
//    }
}
