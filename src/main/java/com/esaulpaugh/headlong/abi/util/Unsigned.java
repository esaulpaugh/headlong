package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.Integers;

import java.math.BigInteger;

/**
 * <p>
 *     Basic utility methods for converting between signed and unsigned representation.
 *     For greater functionality, consider Guava.
 * </p>
 */
public class Unsigned {

    public static long packUnsigned(long unsigned, UintType uintType) {
        if(uintType.rangeLong == null) {
            throw new IllegalArgumentException(uintType.numBits + "-bit range too big for type long");
        }
        if(unsigned < 0) {
            throwNegativeUnsignedVal(unsigned);
        }
        final int bitLen = Integers.bitLen(unsigned);
        if(bitLen > uintType.numBits) {
            throwTooManyBitsException(bitLen, uintType.numBits, false);
        }
        // if in upper half of range, subtract range
        return unsigned >= uintType.rangeLong >> 1 // div 2
                ? unsigned - uintType.rangeLong
                : unsigned;
    }

    public static long unpackUnsigned(long signed, UintType uintType) {
        if(uintType.maskLong == null) {
            throw new IllegalArgumentException(uintType.numBits + "-bit range too big for type long");
        }
        final int bitLen = signed < 0 ? BizarroIntegers.bitLen(signed) : Integers.bitLen(signed);
        if(bitLen >= uintType.numBits) {
            throwTooManyBitsException(bitLen, uintType.numBits, true);
        }
        return signed & uintType.maskLong;
    }

    public static BigInteger toSigned(BigInteger unsigned, UintType uintType) {
        if(unsigned.compareTo(BigInteger.ZERO) < 0) {
            throwNegativeUnsignedVal(unsigned);
        }
        final int bitLen = unsigned.bitLength();
        if(bitLen > uintType.numBits) {
            throwTooManyBitsException(bitLen, uintType.numBits, false);
        }
        // if in upper half of range, subtract range
        return unsigned.compareTo(uintType.range.shiftRight(1)) >= 0
                ? unsigned.subtract(uintType.range)
                : unsigned;
    }

    public static BigInteger toUnsigned(BigInteger signed, UintType uintType) {
        final int bitLen = signed.bitLength();
        if(bitLen >= uintType.numBits) {
            throwTooManyBitsException(bitLen, uintType.numBits, true);
        }
        return signed.compareTo(BigInteger.ZERO) >= 0
                ? signed
                : signed.add(uintType.range);
    }

    private static void throwNegativeUnsignedVal(Number unsigned) {
        throw new IllegalArgumentException("unsigned value is negative: " + unsigned);
    }

    private static void throwTooManyBitsException(int bitLen, int rangeNumBits, boolean forSigned) {
        throw forSigned
                ? new IllegalArgumentException("signed has too many bits: " + bitLen + " is not < " + rangeNumBits)
                : new IllegalArgumentException("unsigned has too many bits: " + bitLen + " > " + rangeNumBits);
    }

    /**
     * Use {@code new UintType(8)} for uint8 et cetera.
     */
    public static class UintType {
        public final int numBits;
        public final BigInteger range;
        public final Long rangeLong;
        public final Long maskLong;

        public UintType(int numBits) {
            this.numBits = numBits;
            this.range = BigInteger.valueOf(2).pow(numBits);
            Long rangeLong, maskLong;
            try {
                rangeLong = range.longValueExact();
                maskLong = range.subtract(BigInteger.ONE).longValueExact();
            } catch (ArithmeticException ae) {
                rangeLong = maskLong = null;
            }
            this.rangeLong = rangeLong;
            this.maskLong = maskLong;
        }
    }
}
