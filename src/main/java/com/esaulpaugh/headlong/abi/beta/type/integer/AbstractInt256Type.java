package com.esaulpaugh.headlong.abi.beta.type.integer;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.util.Pair;

public abstract class AbstractInt256Type<V> extends StackableType<V> { // instance of V should be instanceof Number or Boolean

    public static final int INT_LENGTH_BYTES = 32;

    protected final int bitLength;

    public AbstractInt256Type(String canonicalAbiType, String className, int bitLength) {
        super(canonicalAbiType, className);
        this.bitLength = bitLength;
    }

//    {
//        BigInteger bi = new BigInteger(Arrays.copyOfRange(bytes, index, index + INT_LENGTH_BYTES));
////        BigInteger bi = new BigInteger(bytes, index, INT_LENGTH_BYTES); // Java 9
//        Object obj;
//        if(bitLength <= 8) {
//            obj = bi.byteValueExact();
//        } else if(bitLength <= 16) {
//            obj = bi.shortValueExact();
//        } else if(bitLength <= 32) {
//            obj = bi.intValueExact();
//        } else if(bitLength <= 64) {
//            obj = bi.longValueExact();
//        } else {
//            obj = bi;
//        }
//
//        validate(obj);
//
//        return bi;
//    }

    @Override
    public int byteLength(Object value) {
        return INT_LENGTH_BYTES;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + bitLength + ")";
    }

//    @Override
//    protected void validate(final Object value) {
//        super.validate(value);
////        if(value instanceof Boolean) {
////            if(bitLength != 1) {
////                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
////            }
////            return;
////        }
////        if(bitLength == 1) {
////            if(!(value instanceof Boolean)) {
////                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
////            }
////            return;
////        }
//
//        _validateNumber(value, bitLength);
//
////        if(bitLength != 1) {
////
////        }
//    }

    public void validateBitLen(int bitLen) {
        if(bitLen > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLength);
        }
        System.out.println("length valid;");
    }

    public static Pair<String, AbstractInt256Type> makeInt(String abi, int bits, boolean isElement) {
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

//    private static void _validateNumber(final Object value, final int bitLimit) {
//        Number number = (Number) value;
//        final int bitLen;
//        if(number instanceof BigInteger) {
//            BigInteger bigInt = (BigInteger) number;
//            bitLen = bigInt.bitLength();
//        } else if(number instanceof BigDecimal) {
//            BigDecimal bigInt = (BigDecimal) number;
//            if(bigInt.scale() != 0) {
//                throw new IllegalArgumentException("scale must be 0");
//            }
//            bitLen = bigInt.unscaledValue().bitLength();
//        } else {
//            final long longVal = number.longValue();
//            bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
//
////            if(longVal > 0) {
////                Assert.assertEquals(Long.toBinaryString(longVal).length(), bitLen);
////            } else if(longVal == 0) {
////                Assert.assertEquals(0, bitLen);
////            } else if(longVal == -1) {
////                Assert.assertEquals(0, bitLen);
////            } else { // < -1
////                String bin = Long.toBinaryString(longVal);
////                String minBin = bin.substring(bin.indexOf('0'));
////                Assert.assertEquals(bitLen, minBin.length());
////            }
////            Assert.assertEquals(BigInteger.valueOf(longVal).bitLength(), bitLen);
//        }
//
//        if(bitLen > bitLimit) {
//            throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + bitLimit);
//        }
//        System.out.println("length valid;");
//    }
}
