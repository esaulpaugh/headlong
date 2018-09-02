package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

abstract class AbstractInt256Type<V> extends StackableType<V> { // instance of V should be instanceof Number or Boolean

    static final int INT_LENGTH_BYTES = 32;

    private final int bitLength;

    AbstractInt256Type(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className);
        this.bitLength = bitLength;
    }

    @Override
    int byteLength(Object value) {
        return INT_LENGTH_BYTES;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bitLength + ")";
    }

    void validateBitLen(int bitLen) {
        if(bitLen > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLength);
        }
//        System.out.println("bit len valid;");
    }

    static Pair<String, AbstractInt256Type> makeInt(String abi, int bits, boolean isElement) {
        String className;
        AbstractInt256Type integer;
        if (bits > LongType.MAX_BIT_LEN) {
            className = isElement ? BigIntegerType.CLASS_NAME_ELEMENT : BigIntegerType.CLASS_NAME;
            integer = new BigIntegerType(abi, BigIntegerType.CLASS_NAME, bits);
        } else if (bits > IntType.MAX_BIT_LEN) {
            className = isElement ? LongType.CLASS_NAME_ELEMENT : LongType.CLASS_NAME;
            integer = new LongType(abi, LongType.CLASS_NAME, bits);
        } else if (bits > 16) {
            className = isElement ? IntType.CLASS_NAME_ELEMENT : IntType.CLASS_NAME;
            integer = new IntType(abi, IntType.CLASS_NAME, bits);
        } else if (bits > 8) {
            className = isElement ? ShortType.CLASS_NAME_ELEMENT : ShortType.CLASS_NAME;
            integer = new ShortType(abi, ShortType.CLASS_NAME);
        } else {
            className = isElement ? ByteType.CLASS_NAME_ELEMENT : ByteType.CLASS_NAME;
            integer = new ByteType(abi, ByteType.CLASS_NAME);
        }
        return new Pair<>(className, integer);
    }
}
