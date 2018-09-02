package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigInteger;
import java.util.Arrays;

class LongType extends AbstractInt256Type<Long> {

    static final String CLASS_NAME = Long.class.getName();
    static final String CLASS_NAME_ELEMENT = long[].class.getName().replaceFirst("\\[", "");

    static final int MAX_BIT_LEN = 64;

    LongType(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className, bitLength);
    }

    @Override
    Long decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        Long l = bi.longValueExact();
        final int bitLen = l >= 0 ? RLPIntegers.bitLen(l) : BizarroIntegers.bitLen(l);
        validateBitLen(bitLen);
        return l;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        final int bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
        validateBitLen(bitLen);
    }
}
