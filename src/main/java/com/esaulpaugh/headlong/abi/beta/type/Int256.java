package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.math.BigDecimal;
import java.math.BigInteger;

class Int256 extends StackableType {

    protected final int bitLength;

    Int256(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className);
        this.bitLength = bitLength;
    }

    @Override
    int byteLength(Object value) {
        return 32;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + bitLength;
    }

    @Override
    protected void validate(final Object value) {
        super.validate(value);
//        if(value instanceof Boolean) {
//            if(bitLength != 1) {
//                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
//            }
//            return;
//        }
//        if(bitLength == 1) {
//            if(!(value instanceof Boolean)) {
//                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
//            }
//            return;
//        }
        if(bitLength != 1) {
            _validateNumber(value, bitLength);
        }
    }

    private static void _validateNumber(final Object value, final int bitLimit) {
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
