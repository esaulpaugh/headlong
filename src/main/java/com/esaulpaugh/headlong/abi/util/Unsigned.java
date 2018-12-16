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

    private static final class TestThread extends Thread {

        private final BigInteger start, offset, end;

        private TestThread(BigInteger start, BigInteger offset, BigInteger end) {
            this.start = start;
            this.offset = offset;
            this.end = end;
        }

        @Override
        public void run() {
        }
    }

    public static void main(String[] args) {

        if(true)return;

        long a = Long.MIN_VALUE / 4 + 1;
        long uu = unpackUnsigned(a, new UintType(62));
        long ss = packUnsigned(uu, new UintType(62));
        System.out.println(a + " " + uu + " " + ss);

//        System.out.println(Long.toHexString(new UintType(2).rangeLong));
//        //        long t = Integer.toUnsignedLong((int) rawVal);
//        System.out.println(Long.toHexString(Integer.toUnsignedLong(-10)));
//        System.out.println(Long.toHexString(-10 + 0x100));
//        System.out.println(Long.toHexString(-10 & 0xFF));

        final UintType uintType = new UintType(3);

        final BigInteger end = BigInteger.valueOf(9);
        for (BigInteger i = BigInteger.valueOf(-9); i.compareTo(end) < 0; i = i.add(BigInteger.ONE)) {
            try {
                long ii = i.longValueExact();
                long unsigned = unpackUnsigned(ii, uintType);
                long orig = packUnsigned(unsigned, uintType);
                System.out.println(i + " -> " + unsigned + " -> " + orig);
                if(orig != ii) {
                    throw new Error(orig + " != " + i);
                }
            } catch (IllegalArgumentException iae) {
                System.out.println(i + " N/A -- " + iae.getMessage());
            }
        }

        for (BigInteger i = BigInteger.valueOf(-9); i.compareTo(end) < 0; i = i.add(BigInteger.ONE)) {
            try {
                BigInteger unsigned = toUnsigned(i, uintType);
                BigInteger orig = toSigned(unsigned, uintType);
                System.out.println(i + " -> " + unsigned + " -> " + orig);
                if(orig.compareTo(i) != 0) {
                    throw new Error(orig + " != " + i);
                }
            } catch (IllegalArgumentException iae) {
                System.out.println(i + " N/A -- " + iae.getMessage());
            }
        }

        final BigInteger start = BigInteger.valueOf(2).pow(34).negate();
        final BigInteger _end = BigInteger.valueOf(2).pow(33).negate();

        Thread[] threads = new Thread[7];
        int i;
        for (i = 0; i < threads.length; i++) {
            threads[i] = new TestThread(start, BigInteger.valueOf(i + 1), _end);
            threads[i].start();
        }
        Thread main = new TestThread(start, BigInteger.valueOf(i + 1), _end);
        main.run();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
