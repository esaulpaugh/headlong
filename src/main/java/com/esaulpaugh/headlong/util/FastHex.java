package com.esaulpaugh.headlong.util;

import java.util.Arrays;

/**
 * Uses a larger encoding table to speed up encoding.
 */
public final class FastHex {

    private static final int NIBBLE_BITS = Byte.SIZE / 2;

    private static final int NO_MAPPING = -1;

    // Byte values index directly into the encoding table (size 256) whose elements contain two char values each,
    // encoded together as an int.
    private static final int[] ENCODE_TABLE = new int[1 << Byte.SIZE];

    // Char values index directly into the decoding table (size 256).
    private static final int[] DECODE_TABLE = new int[1 << Byte.SIZE];

    static {
        final int[] ints = new int[] {
                '0', '1', '2', '3',
                '4', '5', '6', '7',
                '8', '9', 'a', 'b',
                'c', 'd', 'e', 'f' };
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            ENCODE_TABLE[i] = (ints[(i & 0xF0) >>> NIBBLE_BITS] << Byte.SIZE) | ints[i & 0x0F];
        }

        Arrays.fill(DECODE_TABLE, NO_MAPPING);

        DECODE_TABLE['0'] = 0x00;
        DECODE_TABLE['1'] = 0x01;
        DECODE_TABLE['2'] = 0x02;
        DECODE_TABLE['3'] = 0x03;
        DECODE_TABLE['4'] = 0x04;
        DECODE_TABLE['5'] = 0x05;
        DECODE_TABLE['6'] = 0x06;
        DECODE_TABLE['7'] = 0x07;
        DECODE_TABLE['8'] = 0x08;
        DECODE_TABLE['9'] = 0x09;
        DECODE_TABLE['A'] = DECODE_TABLE['a'] = 0x0a;
        DECODE_TABLE['B'] = DECODE_TABLE['b'] = 0x0b;
        DECODE_TABLE['C'] = DECODE_TABLE['c'] = 0x0c;
        DECODE_TABLE['D'] = DECODE_TABLE['d'] = 0x0d;
        DECODE_TABLE['E'] = DECODE_TABLE['e'] = 0x0e;
        DECODE_TABLE['F'] = DECODE_TABLE['f'] = 0x0f;
    }

    public static byte[] encodeToBytes(byte[] buffer, final int offset, final int length) {
        final int end = offset + length;
        byte[] bytes = new byte[length << 1];
        for (int i = offset, j = 0; i < end; i++, j+=2) {
            int hexPair = ENCODE_TABLE[buffer[i] & 0xFF];
            bytes[j] = (byte) (hexPair >>> Byte.SIZE); // left
            bytes[j+1] = (byte) (hexPair & 0xFF); // right
        }
        return bytes;
    }

    public static String encodeToString(byte[] buffer, final int offset, final int length) {
        final int end = offset + length;
        char[] chars = new char[length << 1];
        for (int i = offset, j = 0; i < end; i++, j+=2) {
            int hexPair = ENCODE_TABLE[buffer[i] & 0xFF];
            chars[j] = (char) (hexPair >>> Byte.SIZE); // left char
            chars[j+1] = (char) (hexPair & 0xFF); // right char
        }
        return new String(chars);
    }

    public static byte[] encodeToBytes(byte[] buffer) {
        return encodeToBytes(buffer, 0, buffer.length);
    }

    public static String encodeToString(byte[] buffer) {
        return encodeToString(buffer, 0, buffer.length);
    }

    public static byte[] decode(byte[] hexBytes, final int offset, final int length) {
        if ((length & 0x01) != 0) {
            throw new IllegalArgumentException("length must be a multiple of two");
        }
        final int bytesLen = length >> 1;
        byte[] bytes = new byte[bytesLen];
        for (int i = 0, j = offset; i < bytesLen; i++, j+=2) {
            int left = DECODE_TABLE[hexBytes[j]];
            if (left == NO_MAPPING) {
                throw new IllegalArgumentException("illegal val @ " + j);
            }
            int right = DECODE_TABLE[hexBytes[j+1]];
            if (right == NO_MAPPING) {
                throw new IllegalArgumentException("illegal val @ " + (j + 1));
            }
            bytes[i] = (byte) ((left << NIBBLE_BITS) | right);
        }
        return bytes;
    }

    public static byte[] decode(String hex) {
        return decode(hex.getBytes(Strings.CHARSET_ASCII), 0, hex.length());
    }

    public static byte[] decode(String hex, final int offset, final int length) {
        return decode(hex.getBytes(Strings.CHARSET_ASCII), offset, length);
    }
}

