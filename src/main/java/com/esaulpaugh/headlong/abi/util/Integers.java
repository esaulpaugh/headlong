package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;

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
        boolean negative = (buffer[i] & 0b1000_0000) != 0;
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= (buffer[i] & 0xFF) << shiftAmount;
        case 0: break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(negative) {
            // sign extend
            switch (len) {
            case 0: val |= 0xFF;
            case 1: val |= 0xFF << 8;
            case 2: val |= 0xFF << 16;
            case 3: val |= 0xFF << 24;
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
        boolean negative = (buffer[i] & 0b1000000) != 0;
        int shiftAmount = 0;
        long val = 0L;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val = buffer[i+7] & 0xFFL; shiftAmount = Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= (buffer[i] & 0xFFL) << shiftAmount;
        case 0: break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(negative) {
            // sign extend
            switch (len) { /* cases fall through */
            case 0: val |= 0xFF;
            case 1: val |= 0xFF << 8;
            case 2: val |= 0xFF << 16;
            case 3: val |= 0xFF << 24;
            case 4: val |= 0xFFL << 32;
            case 5: val |= 0xFFL << 40;
            case 6: val |= 0xFFL << 48;
            case 7: val |= 0xFFL << 56;
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
