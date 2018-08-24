package com.esaulpaugh.headlong.abi;

public class BooleanType extends Type {

    private BooleanType(String canonicalAbiType, String javaClassName) {
        super(canonicalAbiType, javaClassName, false);
    }

    static BooleanType create(String canonicalAbiType, String javaClassName) {
        return new BooleanType(canonicalAbiType, javaClassName);
    }

    @Override
    public Integer getDataByteLen(Object param) {
        return 32;
    }

    @Override
    public Integer getNumElements(Object param) {
        return null;
    }

//    @Override
//    public void validate(Object param) {
//        super.validate(param);
//    }
}
