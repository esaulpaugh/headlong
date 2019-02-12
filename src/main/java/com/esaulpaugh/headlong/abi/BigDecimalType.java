package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

class BigDecimalType extends AbstractUnitType<BigDecimal> {

    static final String CLASS_NAME = BigDecimal.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(BigDecimal[].class);

    final int scale;

    BigDecimalType(String canonicalTypeString, int bitLength, int scale, boolean unsigned) {
        super(canonicalTypeString, bitLength, unsigned);
        this.scale = scale;
    }

    @Override
    public String className() {
        return CLASS_NAME;
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BIG_DECIMAL;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
    }

    @Override
    public BigDecimal parseArgument(String s) {
        BigDecimal bigDec = new BigDecimal(new BigInteger(s), scale);
        validate(bigDec);
        return bigDec;
    }

    @Override
    public int validate(Object object) {
        super.validate(object);
        BigDecimal dec = (BigDecimal) object;
        validateBigIntBitLen(dec.unscaledValue());
        if(dec.scale() != scale) {
            throw new IllegalArgumentException("big decimal scale mismatch: actual != expected: " + dec.scale() + " != " + scale);
        }
        return UNIT_LENGTH_BYTES;
    }

    @Override
    BigDecimal decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        BigDecimal dec = new BigDecimal(bi, scale);
        validateBigIntBitLen(bi);
        return dec;
    }
}
