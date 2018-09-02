package com.esaulpaugh.headlong.abi.beta.type.integer;

import java.math.BigInteger;
import java.util.Arrays;

public class BigIntegerType extends AbstractInt256Type<BigInteger> {

    public static final String CLASS_NAME = BigInteger.class.getName();
    public static final String CLASS_NAME_ELEMENT = BigInteger[].class.getName().replaceFirst("\\[", "");

    public BigIntegerType(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className, bitLength);
    }

    @Override
    public BigInteger decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        validateBitLen(bi.bitLength());
        return bi;
    }

    @Override
    public void validate(Object object) {
        super.validate(object);
        validateBitLen(((BigInteger) object).bitLength());
    }
}
