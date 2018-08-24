package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberType extends Type {

    protected transient final Integer bitLimit;
    protected transient final Integer scale;

    protected NumberType(String canonicalAbiType, String javaClassName, int bitLimit, int scale) {
        super(canonicalAbiType, javaClassName, false);

        this.bitLimit = bitLimit;
        this.scale = scale;
    }

    static NumberType create(final String canonicalAbiType, final String javaClassName) {
        int bitLimit;
        int scale = 0;
        int idx;
        if ((idx = canonicalAbiType.lastIndexOf("int")) != -1) {
            if(idx == canonicalAbiType.length() - "int".length()) { // endswith
                bitLimit = 256;
            } else {
                bitLimit = Integer.parseInt(canonicalAbiType.substring(idx + 3));
            }
        } else if (canonicalAbiType.equals("address")) {
            bitLimit = 160;
        } else if ((idx = canonicalAbiType.indexOf("fixed")) >= 0) {
            int x = canonicalAbiType.indexOf('x', idx + "fixed".length());
            Integer m = Integer.parseInt(canonicalAbiType.substring(idx + "fixed".length(), x));
            Integer n = Integer.parseInt(canonicalAbiType.substring(x + "x".length())); // error due to tuple not parsed
            System.out.println(m + "x" + n);
            bitLimit = m;
            scale = n;
//        } else if(canonicalAbiType.equals("bool")) {
//            return new NumberType(canonicalAbiType, javaClassName, 1, 0);
        } else {
            throw new AssertionError("unknown case");
        }
        return new NumberType(canonicalAbiType, javaClassName, bitLimit, scale);
    }

    @Override
    public Integer getDataByteLen(Object param) {
        return 32;
    }

    @Override
    public Integer getNumElements(Object param) {
        return null;
    }

    @Override
    protected void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(param, expectedClassName, expectedLengthIndex);
        _validateNumber(param, bitLimit);
    }

    protected static void _validateNumber(final Object param, final int bitLimit) {
        Number number = (Number) param;
        final int bitLen;
        if(number instanceof BigInteger) {
            BigInteger bigIntParam = (BigInteger) number;
            bitLen = bigIntParam.bitLength();
        } else if(number instanceof BigDecimal) {
            BigDecimal bigIntParam = (BigDecimal) number;
            if(bigIntParam.scale() != 0) {
                throw new IllegalArgumentException("scale must be 0");
            }
            bitLen = bigIntParam.unscaledValue().bitLength();
        } else {
            final long longVal = number.longValue();
            bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);

            if(longVal > 0) {
                Assert.assertEquals(Long.toBinaryString(longVal).length(), bitLen);
            } else if(longVal == 0) {
                Assert.assertEquals(0, bitLen);
            } else if(longVal == -1) {
                Assert.assertEquals(0, bitLen);
            } else { // < -1
                String bin = Long.toBinaryString(longVal);
                String minBin = bin.substring(bin.indexOf('0'));
                Assert.assertEquals(bitLen, minBin.length());
            }
            Assert.assertEquals(BigInteger.valueOf(longVal).bitLength(), bitLen);
        }

        if(bitLen > bitLimit) {
            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLimit);
        }
        System.out.println("length valid;");
    }

//    @Override
//    public int calcDynamicByteLen(Object param) {
//        return 0;
//    }

}
