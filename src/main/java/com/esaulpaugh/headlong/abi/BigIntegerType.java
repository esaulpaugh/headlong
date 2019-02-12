package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class BigIntegerType extends AbstractUnitType<BigInteger> {

    static final String CLASS_NAME = BigInteger.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(BigInteger[].class);

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, bitLength, unsigned);
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
        return TYPE_CODE_BIG_INTEGER;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
    }

    @Override
    public BigInteger parseArgument(String s) {
        BigInteger bigInt = new BigInteger(s);
        validate(bigInt);
        return bigInt;
    }

    @Override
    public int validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi;
    }
}
