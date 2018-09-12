package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

class BigDecimalType extends AbstractUnitType<BigDecimal> {

    private static final long serialVersionUID = -4619900038530235710L;

    static final String CLASS_NAME = BigDecimal.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = ClassNames.getNameStub(BigDecimal[].class);

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

    @Override
    BigDecimal decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        BigDecimal dec = new BigDecimal(bi, scale);
        validateBigIntBitLen(bi);
        return dec;
    }

    // TODO eventually just rely on super.hashCode() hashing canonicalType and dynamic
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), scale);
    }

    // TODO eventually just rely on super.equals() checking canonicalType and dynamic
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BigDecimalType other = (BigDecimalType) o;
        return scale == other.scale;
    }
}
