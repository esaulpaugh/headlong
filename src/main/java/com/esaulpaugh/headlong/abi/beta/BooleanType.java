package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class BooleanType extends AbstractInt256Type<Boolean> {

    private static final String CLASS_NAME = Boolean.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = boolean[].class.getName().replaceFirst("\\[", "");

    BooleanType() {
        super("bool", 1, false);
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
    Boolean decode(ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bi = new BigInteger(elementBuffer);
        switch (bi.byteValueExact()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new ArithmeticException("expected value 0 or 1");
        }
    }
}
