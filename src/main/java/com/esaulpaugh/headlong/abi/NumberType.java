package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigDecimal;
import java.math.BigInteger;

class NumberType extends Type {

    private static final int ADDRESS_BIT_LEN = 160;

    protected transient final Integer bitLimit;

    private NumberType(String canonicalAbiType, String javaClassName, int bitLimit) {
        super(canonicalAbiType, javaClassName, false);

        this.bitLimit = bitLimit;
    }

    static NumberType create(final String canonicalAbiType, final String javaClassName) {
        int bitLimit;
        int idx;
        if ((idx = canonicalAbiType.lastIndexOf("int")) != -1) {
            if(idx == canonicalAbiType.length() - "int".length()) { // i.e. endswith
                bitLimit = 256;
            } else {
                bitLimit = Integer.parseInt(canonicalAbiType.substring(idx + "int".length()));
            }
        } else if (canonicalAbiType.equals("address")) {
            bitLimit = ADDRESS_BIT_LEN;
        } else if ((idx = canonicalAbiType.indexOf("fixed")) >= 0) {
            int x = canonicalAbiType.indexOf('x', idx + "fixed".length());
            Integer m = Integer.parseInt(canonicalAbiType.substring(idx + "fixed".length(), x));
            int n = Integer.parseInt(canonicalAbiType.substring(x + "x".length()));
            System.out.println(m + "x" + n);
            bitLimit = m;
        } else {
            throw new AssertionError("unknown case");
        }
        return new NumberType(canonicalAbiType, javaClassName, bitLimit);
    }

    @Override
    public Integer getDataByteLen(Object value) {
        return 32;
    }

    @Override
    protected void validate(final Object value, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(value, expectedClassName, expectedLengthIndex);
        _validateNumber(value, bitLimit);
    }

    protected static void _validateNumber(final Object value, final int bitLimit) {
        Number number = (Number) value;
        final int bitLen;
        if(number instanceof BigInteger) {
            BigInteger bigInt = (BigInteger) number;
            bitLen = bigInt.bitLength();
        } else if(number instanceof BigDecimal) {
            BigDecimal bigInt = (BigDecimal) number;
            if(bigInt.scale() != 0) {
                throw new IllegalArgumentException("scale must be 0");
            }
            bitLen = bigInt.unscaledValue().bitLength();
        } else {
            final long longVal = number.longValue();
            bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);

//            if(longVal > 0) {
//                Assert.assertEquals(Long.toBinaryString(longVal).length(), bitLen);
//            } else if(longVal == 0) {
//                Assert.assertEquals(0, bitLen);
//            } else if(longVal == -1) {
//                Assert.assertEquals(0, bitLen);
//            } else { // < -1
//                String bin = Long.toBinaryString(longVal);
//                String minBin = bin.substring(bin.indexOf('0'));
//                Assert.assertEquals(bitLen, minBin.length());
//            }
//            Assert.assertEquals(BigInteger.valueOf(longVal).bitLength(), bitLen);
        }

        if(bitLen > bitLimit) {
            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLimit);
        }
        System.out.println("length valid;");
    }
}
