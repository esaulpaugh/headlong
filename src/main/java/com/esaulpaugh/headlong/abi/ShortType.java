//package com.esaulpaugh.headlong.abi;
//
//import com.esaulpaugh.headlong.abi.util.ABIUtils;
//
//import java.math.BigInteger;
//import java.nio.ByteBuffer;
//
//class ShortType extends AbstractUnitType<Short> {
//
//    private static final long serialVersionUID = -8064633105605031334L;
//
//    private static final String CLASS_NAME = Short.class.getName();
//    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(short[].class);
//
//    ShortType(String canonicalType, boolean unsigned) {
//        super(canonicalType, Short.SIZE, unsigned);
//    }
//
//    @Override
//    String className() {
//        return CLASS_NAME;
//    }
//
//    @Override
//    String arrayClassNameStub() {
//        return ARRAY_CLASS_NAME_STUB;
//    }
//
//    @Override
//    int typeCode() {
//        return TYPE_CODE_SHORT;
//    }
//
//    @Override
//    Short decode(ByteBuffer bb, byte[] unitBuffer) {
//        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
//        BigInteger bi = new BigInteger(unitBuffer);
//        validateBigIntBitLen(bi);
//        return bi.shortValue();
//    }
//}
