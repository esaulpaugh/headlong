package com.esaulpaugh.headlong.abi.beta.type.integer;

import java.math.BigInteger;
import java.util.Arrays;

public class BooleanType extends AbstractInt256Type<Boolean> {

    public static final String CLASS_NAME = Boolean.class.getName();
    public static final String CLASS_NAME_ELEMENT = boolean[].class.getName().replaceFirst("\\[", "");

    public static final byte[] BOOLEAN_FALSE;
    public static final byte[] BOOLEAN_TRUE;

    static {
        BOOLEAN_FALSE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
        BOOLEAN_TRUE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
        BOOLEAN_TRUE[BOOLEAN_TRUE.length - 1] = 1;
    }

    public BooleanType(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, 1);
    }

    @Override
    public Boolean decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        switch (bi.byteValueExact()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new ArithmeticException("expected value 0 or 1");
        }
    }

//    @Override
//    public void validate(Object object) {
//        super.validate(object);
//        if(bitLength != 1) {
//            throw new AssertionError();
//        }
//    }
}
