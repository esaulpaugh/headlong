/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.util;

import java.nio.charset.StandardCharsets;
import java.util.function.IntUnaryOperator;

/** Hexadecimal codec optimized for small inputs. */
public final class FastHex {

    private FastHex() {}

    public static final int CHARS_PER_BYTE = 2;

    public static final int BITS_PER_CHAR = Byte.SIZE / CHARS_PER_BYTE;

    // Byte values index directly into the encoding table (size 256) whose elements consist of two ASCII values encoded
    // together as a short
    private static final short[] ENCODE_TABLE = new short[1 << Byte.SIZE];

    static {
        final byte[] chars = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        final int leftNibbleMask = 0xF0;
        final int rightNibbleMask = 0x0F;
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            byte leftChar = chars[(i & leftNibbleMask) >>> BITS_PER_CHAR];
            byte rightChar = chars[i & rightNibbleMask];
            ENCODE_TABLE[i] = (short) ((leftChar << Byte.SIZE) | rightChar);
        }
    }

    public static String encodeToString(byte... buffer) {
        return encodeToString(buffer, 0, buffer.length);
    }

    @SuppressWarnings("deprecation")
    public static String encodeToString(byte[] buffer, int offset, int len) {
        byte[] enc = encodeToBytes(buffer, offset, len);
        return new String(enc, 0, 0, enc.length); // faster on Java 9+ (compact strings on by default)
    }

    public static byte[] encodeToBytes(byte... buffer) {
        return encodeToBytes(buffer, 0, buffer.length);
    }

    public static byte[] encodeToBytes(byte[] buffer, int offset, int len) {
        final int end = offset + len;
        byte[] bytes = new byte[len * CHARS_PER_BYTE];
        for (int j = 0; offset < end; offset++, j += CHARS_PER_BYTE) {
            int hexPair = ENCODE_TABLE[buffer[offset] & 0xFF];
            bytes[j] = (byte) (hexPair >>> Byte.SIZE); // left
            bytes[j+1] = (byte) hexPair; // right
        }
        return bytes;
    }

    public static byte[] decode(String hex) {
        return decode(hex, 0, hex.length());
    }

    public static byte[] decode(String hex, int offset, int len) {
        return decode(offset, len, hex::charAt);
    }

    public static byte[] decode(byte[] hexBytes, int offset, int len) {
        return decode(offset, len, o -> hexBytes[o]);
    }

    private static byte[] decode(int offset, int len, IntUnaryOperator extractor) {
        if (!Integers.isMultiple(len, CHARS_PER_BYTE)) {
            throw new IllegalArgumentException("len must be a multiple of two");
        }
        byte[] dest = new byte[len / CHARS_PER_BYTE];
        for (int i = 0; i < dest.length; i++, offset += CHARS_PER_BYTE) {
            dest[i] = (byte) decodeByte(extractor, offset);
        }
        return dest;
    }

    private static int decodeByte(IntUnaryOperator extractor, int offset) {
        return decodeNibble(extractor.applyAsInt(offset), offset) << BITS_PER_CHAR | decodeNibble(extractor.applyAsInt(++offset), offset);
    }

    public static void decode(final String src, int offset, final int len, final byte[] dest, int destOffset) {
        if (!Integers.isMultiple(len, CHARS_PER_BYTE)) {
            throw new IllegalArgumentException("len must be a multiple of two");
        }
        final int srcEnd = offset + len;
        if(srcEnd > src.length()) {
            throw new IllegalArgumentException("last src index exceeds src length: " + srcEnd + " > " + src.length());
        }
        final int destEnd = destOffset + len / 2;
        if(destEnd > dest.length) {
            throw new IllegalArgumentException("last dest index exceeds dest length: " + destEnd + " > " + dest.length);
        }
        while (destOffset < destEnd) {
            dest[destOffset++] = (byte) (decodeNibble(src.charAt(offset), offset) << BITS_PER_CHAR | decodeNibble(src.charAt(++offset), offset++));
        }
    }

    private static int decodeNibble(int c, int offset) {
        switch (c) {
        case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9': return c - '0';
        case 'A':case 'B':case 'C':case 'D':case 'E':case 'F': return  c - ('A' - 0xA);
        case 'a':case 'b':case 'c':case 'd':case 'e':case 'f': return c - ('a' - 0xa);
        default: throw new IllegalArgumentException("illegal hex val @ " + offset);
        }
    }
}
