package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.util.BizarroIntegers;

import java.math.BigInteger;

public class Integers {

    /**
     * Retrieves an integer up to four bytes in length. Big-endian two's complement format.
     *
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation
     * @return  the integer
     */
    public static int getInt(byte[] buffer, int i, int len) {
        int shiftAmount = 0;
        int val = 0;
        byte leftmost;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= ((leftmost = buffer[i]) & 0xFF) << shiftAmount; break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(leftmost < 0) { // if negative
            // sign extend
            switch (len) {
            case 1: return val | 0xFFFFFF00;
            case 2: return val | 0xFFFF0000;
            case 3: return val | 0xFF000000;
            }
        }
        return val;
    }

    /**
     * Retrieves an integer up to eight bytes in length. Big-endian two's complement format.
     *
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation
     * @return  the integer
     */
    public static long getLong(final byte[] buffer, final int i, final int len) {
        int shiftAmount = 0;
        long val = 0L;
        byte leftmost;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val = buffer[i+7] & 0xFFL; shiftAmount = Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= ((leftmost = buffer[i]) & 0xFFL) << shiftAmount; break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(leftmost < 0) {
            // sign extend
            switch (len) { /* cases fall through */
            case 1: return val | 0xFFFFFFFFFFFFFF00L;
            case 2: return val | 0xFFFFFFFFFFFF0000L;
            case 3: return val | 0xFFFFFFFFFF000000L;
            case 4: return val | 0xFFFFFFFF00000000L;
            case 5: return val | 0xFFFFFF0000000000L;
            case 6: return val | 0xFFFF000000000000L;
            case 7: return val | 0xFF00000000000000L;
            }
        }
        return val;
    }

    public static long packUnsigned(long unsigned, UintType uintType) {
        if(uintType.rangeLong == null) {
            throw new IllegalArgumentException(uintType.numBits + "-bit range too big for type long");
        }
        if(unsigned < 0) {
            throwNegativeUnsignedVal(unsigned);
        }
        final int bitLen = com.esaulpaugh.headlong.rlp.util.Integers.bitLen(unsigned);
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
        final int bitLen = signed < 0 ? BizarroIntegers.bitLen(signed) : com.esaulpaugh.headlong.rlp.util.Integers.bitLen(signed);
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

        private static final BigInteger TWO = BigInteger.valueOf(2L);

        public final int numBits;
        public final BigInteger range;
        public final Long rangeLong;
        public final Long maskLong;

        public UintType(int numBits) {
            if(numBits < 0) {
                throw new IllegalArgumentException("numBits must be non-negative");
            }
            this.numBits = numBits;
            this.range = TWO.shiftLeft(numBits - 1); // .pow(numBits)
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
