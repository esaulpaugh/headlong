package com.esaulpaugh.headlong.abi.beta.type.integer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

public class BigDecimalType extends AbstractInt256Type<BigDecimal> {

    public static final String CLASS_NAME = BigDecimal.class.getName();
    public static final String CLASS_NAME_ELEMENT = BigDecimal[].class.getName().replaceFirst("\\[", "");

    public final int scale;

    public BigDecimalType(String canonicalAbiType, String className, int bitLength, int scale) {
        super(canonicalAbiType, className, bitLength);
        this.scale = scale;
    }

    @Override
    public BigDecimal decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        BigDecimal dec = new BigDecimal(bi, scale);
        validateBitLen(bi.bitLength());
        return dec;
    }

    @Override
    public void validate(Object object) {
        super.validate(object);
        BigDecimal dec = (BigDecimal) object;
        validateBitLen(dec.unscaledValue().bitLength());
        if(dec.scale() != scale) {
            throw new IllegalArgumentException("big decimal scale mismatch: actual != expected: " + dec.scale() + " != " + scale);
        }
    }
}
