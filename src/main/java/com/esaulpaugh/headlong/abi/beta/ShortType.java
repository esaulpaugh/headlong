package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class ShortType extends AbstractUnitType<Short> {

    private static final String CLASS_NAME = Short.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = Utils.getNameStub(short[].class);

    private static final int SHORT_LENGTH_BITS = 16;

    ShortType(String canonicalType, boolean unsigned) {
        super(canonicalType, SHORT_LENGTH_BITS, unsigned);
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
        return TYPE_CODE_SHORT;
    }

    @Override
    Short decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi.shortValue();
    }
}
