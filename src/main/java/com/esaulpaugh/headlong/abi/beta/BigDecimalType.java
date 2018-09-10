package com.esaulpaugh.headlong.abi.beta;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

class BigDecimalType extends AbstractUnitType<BigDecimal> {

    static final String CLASS_NAME = BigDecimal.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = ArrayType.getNameStub(BigDecimal[].class);

    final int scale;

    BigDecimalType(String canonicalTypeString, int bitLength, int scale, boolean unsigned) {
        super(canonicalTypeString, bitLength, unsigned);
        this.scale = scale;
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    BigDecimal decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        BigDecimal dec = new BigDecimal(bi, scale);
        validateBigIntBitLen(bi);
        return dec;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BIG_DECIMAL;
    }

    @Override
    int validate(Object object) {
        super.validate(object);
        BigDecimal dec = (BigDecimal) object;
        validateBigIntBitLen(dec.unscaledValue());
        if(dec.scale() != scale) {
            throw new IllegalArgumentException("big decimal scale mismatch: actual != expected: " + dec.scale() + " != " + scale);
        }
        return UNIT_LENGTH_BYTES;
    }
}
