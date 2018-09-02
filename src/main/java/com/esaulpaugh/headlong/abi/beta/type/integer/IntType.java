package com.esaulpaugh.headlong.abi.beta.type.integer;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigInteger;
import java.util.Arrays;

public class IntType extends AbstractInt256Type<Integer> {

    public static final IntType OFFSET_TYPE = new IntType("uint32", IntType.CLASS_NAME, IntType.MAX_BIT_LEN);

    public static final String CLASS_NAME = Integer.class.getName();
    public static final String CLASS_NAME_ELEMENT = int[].class.getName().replaceFirst("\\[", "");

    public static final int MAX_BIT_LEN = 32;

    public IntType(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className, bitLength);
    }

    @Override
    public Integer decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        Integer i = bi.intValueExact();
        final int bitLen = i >= 0 ? RLPIntegers.bitLen(i) : BizarroIntegers.bitLen(i);
        validateBitLen(bitLen);
        return i;
    }

    @Override
    public void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        final int bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
        validateBitLen(bitLen);
    }
}
