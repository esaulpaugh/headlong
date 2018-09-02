package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class BigIntegerType extends AbstractInt256Type<BigInteger> {

    static final String CLASS_NAME = BigInteger.class.getName();
    static final String CLASS_NAME_ELEMENT = BigInteger[].class.getName().replaceFirst("\\[", "");

    BigIntegerType(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className, bitLength);
    }

    @Override
    BigInteger decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        validateBitLen(bi.bitLength());
        return bi;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        validateBitLen(((BigInteger) object).bitLength());
    }
}
