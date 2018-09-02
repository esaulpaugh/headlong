package com.esaulpaugh.headlong.abi.beta.type.integer;

import java.math.BigInteger;
import java.util.Arrays;

public class ShortType extends AbstractInt256Type<Short> {

    public static final String CLASS_NAME = Short.class.getName();
    public static final String CLASS_NAME_ELEMENT = short[].class.getName().replaceFirst("\\[", "");

    public static final int SHORT_LENGTH_BITS = 16;

    public ShortType(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, SHORT_LENGTH_BITS);
    }

    @Override
    public Short decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        return bi.shortValueExact();
//        validate(s);
//        return s;
    }

//    @Override
//    public void validate(Object object) {
//        super.validate(object);
//        if(bitLength != SHORT_LENGTH_BITS) {
//            throw new AssertionError();
//        }
//    }
}
