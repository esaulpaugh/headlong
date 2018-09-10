package com.esaulpaugh.headlong.abi.beta.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Unsigned {

    public static final BigInteger INT8_RANGE = BigInteger.valueOf(2).pow(8);
    public static final BigInteger INT16_RANGE = BigInteger.valueOf(2).pow(16);
    public static final BigInteger INT24_RANGE = BigInteger.valueOf(2).pow(24);
    public static final BigInteger INT32_RANGE = BigInteger.valueOf(2).pow(32);
    public static final BigInteger INT40_RANGE = BigInteger.valueOf(2).pow(40);
    public static final BigInteger INT48_RANGE = BigInteger.valueOf(2).pow(48);
    public static final BigInteger INT56_RANGE = BigInteger.valueOf(2).pow(56);
    public static final BigInteger INT64_RANGE = BigInteger.valueOf(2).pow(64);
    public static final BigInteger INT72_RANGE = BigInteger.valueOf(2).pow(72);
    public static final BigInteger INT80_RANGE = BigInteger.valueOf(2).pow(80);
    public static final BigInteger INT88_RANGE = BigInteger.valueOf(2).pow(88);
    public static final BigInteger INT96_RANGE = BigInteger.valueOf(2).pow(96);

    private static final int INT8_RANGE_INT = INT8_RANGE.intValue();
    private static final int INT16_RANGE_INT = INT16_RANGE.intValue();
    private static final int INT24_RANGE_INT = INT24_RANGE.intValue();
    private static final long INT32_RANGE_LONG = INT32_RANGE.longValue();
    private static final long INT40_RANGE_LONG = INT40_RANGE.longValue();
    private static final long INT48_RANGE_LONG = INT48_RANGE.longValue();
    private static final long INT56_RANGE_LONG = INT56_RANGE.longValue();

    public static BigInteger convert(BigInteger val, BigInteger range) {
        return val.compareTo(BigInteger.ZERO) >= 0 ? val : val.add(range);
    }

    public static int uint8(int val) {
        return val >= 0 ? val : val + INT8_RANGE_INT;
    }

    public static int uint16(int val) {
        return val >= 0 ? val : val + INT16_RANGE_INT;
    }

    public static int uint24(int val) {
        return val >= 0 ? val : val + INT24_RANGE_INT;
    }

    public static long uint32(int val) {
        return val >= 0 ? (long) val : ((long) val) + INT32_RANGE_LONG;
    }

    public static BigInteger uint64(long val) {
        return val >= 0
                ? BigInteger.valueOf(val)
                : BigInteger.valueOf(val).add(INT64_RANGE);
    }

    private static void test8() {
        for (int i = Byte.MAX_VALUE - 2; i < Byte.MAX_VALUE; i++) {
            System.out.println(i + " --> " + uint8(i));
        }
        System.out.println(Byte.MAX_VALUE + " --> " + uint8(Byte.MAX_VALUE));
        for (int i = Byte.MIN_VALUE; i < Byte.MIN_VALUE + 3; i++) {
            System.out.println(i + " --> " + uint8(i));
        }

        System.out.println();
        System.out.println(-2 + " --> " + uint8(-2));
        System.out.println(-1 + " --> " + uint8(-1));
        System.out.println("2^8 = " + new BigDecimal(BigInteger.valueOf(2L).pow(8), 0));
        System.out.println();
    }

    private static void test16() {
        for (int i = Short.MAX_VALUE - 2; i < Short.MAX_VALUE; i++) {
            System.out.println(i + " --> " + uint16(i));
        }
        System.out.println(Short.MAX_VALUE + " --> " + uint16(Short.MAX_VALUE));
        for (int i = Short.MIN_VALUE; i < Short.MIN_VALUE + 3; i++) {
            System.out.println(i + " --> " + uint16(i));
        }

        System.out.println();
        System.out.println(-2 + " --> " + uint16(-2));
        System.out.println(-1 + " --> " + uint16(-1));
        System.out.println("2^16 = " + new BigDecimal(BigInteger.valueOf(2L).pow(16), 0));
        System.out.println();
    }

    private static void test32() {
        for (int i = Integer.MAX_VALUE - 2; i < Integer.MAX_VALUE; i++) {
            System.out.println(i + " --> " + uint32(i));
        }
        System.out.println(Integer.MAX_VALUE + " --> " + uint32(Integer.MAX_VALUE));
        for (int i = Integer.MIN_VALUE; i < Integer.MIN_VALUE + 3; i++) {
            System.out.println(i + " --> " + uint32(i));
        }

        System.out.println();
        System.out.println(-2 + " --> " + uint32(-2));
        System.out.println(-1 + " --> " + uint32(-1));
        System.out.println("2^32 = " + new BigDecimal(BigInteger.valueOf(2L).pow(32), 0));
        System.out.println();
    }

    private static void test64() {
        for (long i = Long.MAX_VALUE - 2; i < Long.MAX_VALUE; i++) {
            System.out.println(i + " --> " + uint64(i));
        }
        System.out.println(Long.MAX_VALUE + " --> " + uint64(Long.MAX_VALUE));
        for (long i = Long.MIN_VALUE; i < Long.MIN_VALUE + 3; i++) {
            System.out.println(i + " --> " + uint64(i));
        }

        System.out.println();
        System.out.println(-2 + " --> " + uint64(-2));
        System.out.println(-1 + " --> " + uint64(-1));
        System.out.println("2^64 = " + new BigDecimal(BigInteger.valueOf(2L).pow(64), 0));
        System.out.println();
    }

    public static void main(String[] args) {

        System.out.println(INT8_RANGE + " " + Math.pow(2, 8));
        System.out.println(INT16_RANGE + " " + BigInteger.valueOf(2).pow(16));
        System.out.println(INT24_RANGE + " " + BigInteger.valueOf(2).pow(24));
        System.out.println(INT32_RANGE + " " + BigInteger.valueOf(2).pow(32));
        System.out.println(INT64_RANGE + " " + BigInteger.valueOf(2).pow(64));

        test16();

//        test8();
//        test32();

//        System.out.println(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
//        System.out.println(toUnsigned(0xFFFFFFFF_FFFFFFFFL)); // 0x80000000_00000000L
//        System.out.println(toUnsigned(0x80000000_00000000L)); // 0xFFFFFFFF_FFFFFFFFL

    }

}
