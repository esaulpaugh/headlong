package com.esaulpaugh.headlong.abi.util;

import java.math.BigInteger;

/**
 * For converting integers to and from signed and unsigned representation. Use {@code new UintType(8)} for uint8 et cetera.
 */
public final class Uint {

    private static final BigInteger TWO = BigInteger.valueOf(2L);

    public final int numBits;
    public final BigInteger range;
    public BigInteger halfRange;
    public final Long rangeLong;
    public final Long maskLong;
    public final Long halfRangeLong;

    public Uint(int numBits) {
        if(numBits < 0) {
            throw new IllegalArgumentException("numBits must be non-negative");
        }
        this.numBits = numBits;
        this.range = TWO.shiftLeft(numBits - 1); // TWO.pow(numBits)
        this.halfRange = range.shiftRight(1);
        Long rangeLong, halfRangeLong, maskLong;
        try {
            rangeLong = range.longValueExact();
            halfRangeLong = rangeLong >> 1;
            maskLong = range.subtract(BigInteger.ONE).longValueExact();
        } catch (ArithmeticException ae) {
            rangeLong = halfRangeLong = maskLong = null;
        }
        this.rangeLong = rangeLong;
        this.halfRangeLong = halfRangeLong;
        this.maskLong = maskLong;
    }

    public long toSigned(long unsigned) {
        if(rangeLong == null) {
            return toSigned(BigInteger.valueOf(unsigned)).longValueExact();
        }
        if(unsigned < 0) {
            throwNegativeUnsignedVal(unsigned);
        }
        final int bitLen = com.esaulpaugh.headlong.util.Integers.bitLen(unsigned);
        if(bitLen > numBits) {
            throwTooManyBitsException(bitLen, numBits, false);
        }
        // if in upper half of range, subtract range
        return unsigned >= halfRangeLong
                ? unsigned - rangeLong
                : unsigned;
    }

    public long toUnsigned(long signed) {
        if(maskLong == null) {
            // beware of ArithmeticException
            return toUnsigned(BigInteger.valueOf(signed)).longValueExact();
        }
        final int bitLen = signed < 0 ? BizarroIntegers.bitLen(signed) : com.esaulpaugh.headlong.util.Integers.bitLen(signed);
        if(bitLen >= numBits) {
            throwTooManyBitsException(bitLen, numBits, true);
        }
        return signed & maskLong;
    }

    public BigInteger toSigned(BigInteger unsigned) {
        if(unsigned.compareTo(BigInteger.ZERO) < 0) {
            throwNegativeUnsignedVal(unsigned);
        }
        final int bitLen = unsigned.bitLength();
        if(bitLen > numBits) {
            throwTooManyBitsException(bitLen, numBits, false);
        }
        // if in upper half of range, subtract range
        return unsigned.compareTo(halfRange) >= 0
                ? unsigned.subtract(range)
                : unsigned;
    }

    public BigInteger toUnsigned(BigInteger signed) {
        final int bitLen = signed.bitLength();
        if(bitLen >= numBits) {
            throwTooManyBitsException(bitLen, numBits, true);
        }
        return signed.compareTo(BigInteger.ZERO) >= 0
                ? signed
                : signed.add(range);
    }

    private static void throwNegativeUnsignedVal(Number unsigned) {
        throw new IllegalArgumentException("unsigned value is negative: " + unsigned);
    }

    private static void throwTooManyBitsException(int bitLen, int rangeNumBits, boolean forSigned) {
        throw forSigned
                ? new IllegalArgumentException("signed has too many bits: " + bitLen + " is not < " + rangeNumBits)
                : new IllegalArgumentException("unsigned has too many bits: " + bitLen + " > " + rangeNumBits);
    }
}
