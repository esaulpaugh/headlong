package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Unsigned 0 or 1.
 */
class BooleanType extends UnitType<Boolean> {

    static final Class<?> CLASS = Boolean.class;
    static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(boolean[].class);

    static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {
        super("bool", CLASS, 1, true);
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BOOLEAN;
    }

    @Override
    int byteLengthPacked(Object value) {
        return 1;
    }

    @Override
    public Boolean parseArgument(String s) {
        Boolean bool = Boolean.parseBoolean(s);
        validate(bool);
        return bool;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Boolean decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        switch (bi.byteValue()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new IllegalArgumentException("negative value given for boolean type");
        }
    }
}
