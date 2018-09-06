package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigInteger;

abstract class AbstractUnitType<V> extends StackableType<V> { // instance of V should be instanceof Number or Boolean

    static final int UNIT_LENGTH_BYTES = 32;

    private final int bitLength;

    AbstractUnitType(String canonicalType, int bitLength, boolean signed) {
        super(canonicalType, false);
        this.bitLength = signed ? bitLength - 1 : bitLength;
//        this.signed = signed;
    }

    @Override
    int byteLength(Object value) {
        return UNIT_LENGTH_BYTES;
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
    }

    void validateBigIntBitLen(final BigInteger bigIntVal) {
        if(bigIntVal.bitLength() > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + bigIntVal.bitLength() + " > " + bitLength);
        }
    }
}
