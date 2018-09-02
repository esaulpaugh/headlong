package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Pair;
import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigInteger;

abstract class AbstractInt256Type<V> extends StackableType<V> { // instance of V should be instanceof Number or Boolean

    static final int INT_LENGTH_BYTES = 32;

    private final int bitLength;

    protected final boolean signed;

    AbstractInt256Type(String canonicalAbiType, String className, int bitLength, boolean signed) {
        super(canonicalAbiType, className);
        this.bitLength = signed ? bitLength - 1 : bitLength;
        this.signed = signed;
    }

    @Override
    int byteLength(Object value) {
        return INT_LENGTH_BYTES;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bitLength + ")";
    }

    void validateLongBitLen(long longVal) {
        final int bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
        if(bitLen > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLength);
        }
        System.out.println("bit len valid: " + bitLen);
    }

    void validateBigIntBitLen(final BigInteger bigIntVal) {
        if(bigIntVal.bitLength() > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + bigIntVal.bitLength() + " > " + bitLength);
        }
        System.out.println("bigint bit len valid: " + bigIntVal.bitLength());
    }

    static Pair<String, AbstractInt256Type> makeInt(String abi, int bitLength, boolean isElement, boolean signed) {
        String className;
        AbstractInt256Type integer;
        if (bitLength > LongType.MAX_BIT_LEN) {
            className = isElement ? BigIntegerType.CLASS_NAME_ELEMENT : BigIntegerType.CLASS_NAME;
            integer = new BigIntegerType(abi, BigIntegerType.CLASS_NAME, bitLength, signed);
        } else if (bitLength > IntType.MAX_BIT_LEN) {
            className = isElement ? LongType.CLASS_NAME_ELEMENT : LongType.CLASS_NAME;
            integer = new LongType(abi, LongType.CLASS_NAME, bitLength, signed);
        } else if (bitLength > 16) {
            className = isElement ? IntType.CLASS_NAME_ELEMENT : IntType.CLASS_NAME;
            integer = new IntType(abi, IntType.CLASS_NAME, bitLength, signed);
        } else if (bitLength > 8) {
            className = isElement ? ShortType.CLASS_NAME_ELEMENT : ShortType.CLASS_NAME;
            integer = new ShortType(abi, ShortType.CLASS_NAME, signed);
        } else {
            className = isElement ? ByteType.CLASS_NAME_ELEMENT : ByteType.CLASS_NAME;
            integer = new ByteType(abi, ByteType.CLASS_NAME, signed);
        }
        return new Pair<>(className, integer);
    }
}
