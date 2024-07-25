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
import java.util.Arrays;
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
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            byte leftChar = chars[(i & 0xF0) >>> BITS_PER_CHAR];
            byte rightChar = chars[i & 0x0F];
            ENCODE_TABLE[i] = (short) ((leftChar << Byte.SIZE) | rightChar);
        }
    }

    public static String encodeToString(byte... buffer) {
        return encodeToString(buffer, 0, buffer.length);
    }

    @SuppressWarnings("deprecation")
    public static String encodeToString(byte[] buffer, int offset, int len) {
        byte[] enc = encodeToBytes(buffer, offset, len);
        return new String(enc, 0, 0, enc.length); // on Java 9+ (compact strings on by default), faster than creating String from char[]
    }

    public static byte[] encodeToBytes(byte... buffer) {
        return encodeToBytes(buffer, 0, buffer.length);
    }

    public static byte[] encodeToBytes(byte[] buffer, int offset, int len) {
        byte[] bytes = new byte[len * CHARS_PER_BYTE];
        encodeBytes(buffer, offset, len, bytes, 0);
        return bytes;
    }

    public static void encodeBytes(byte[] buffer, int offset, int len, byte[] dest, int destOff) {
        final int end = offset + len;
        for (int j = destOff; offset < end; offset++, j += CHARS_PER_BYTE) {
            int hexPair = ENCODE_TABLE[buffer[offset] & 0xFF];
            dest[j] = (byte) (hexPair >>> Byte.SIZE); // left
            dest[j+1] = (byte) hexPair; // right
        }
    }
    
    public static byte[] decode(CharSequence hex) {
        return decode(hex, 0, hex.length());
    }

    public static byte[] decode(byte[] hexBytes) {
        return decode(hexBytes, 0, hexBytes.length);
    }

    public static byte[] decode(CharSequence hex, int offset, int len) {
        return decode(offset, len, hex::charAt);
    }

    public static byte[] decode(byte[] hexBytes, int offset, int len) {
        return decode(offset, len, o -> hexBytes[o]);
    }

    private static byte[] decode(int offset, int len, IntUnaryOperator extractor) {
        final byte[] dest = new byte[decodedLength(len)];
        for (int i = 0; i < dest.length; i++, offset += CHARS_PER_BYTE) {
            dest[i] = (byte) decodeByte(extractor, offset);
        }
        return dest;
    }

    public static int decodedLength(int encodedLen) {
        if (!Integers.isMultiple(encodedLen, CHARS_PER_BYTE)) {
            throw new IllegalArgumentException("len must be a multiple of two");
        }
        return encodedLen / CHARS_PER_BYTE;
    }

    private static final int[] DECODE_TABLE = new int[256];

    static {
        Arrays.fill(DECODE_TABLE, -(0xF << BITS_PER_CHAR) - 1);
        for (int i = '0'; i <= '9'; i++) DECODE_TABLE[i] = i - '0' + 0x0;
        for (int i = 'A'; i <= 'F'; i++) DECODE_TABLE[i] = i - 'A' + 0xA;
        for (int i = 'a'; i <= 'f'; i++) DECODE_TABLE[i] = i - 'a' + 0xA;
    }

    private static int decodeByte(IntUnaryOperator extractor, int offset) {
        try {
            int left_ = DECODE_TABLE[extractor.applyAsInt(offset)];
            int right = DECODE_TABLE[extractor.applyAsInt(++offset)];
            int b = (left_ << BITS_PER_CHAR) + right;
            if (b < 0) {
                throw new IllegalArgumentException("illegal hex val @ " + (left_ < 0 ? offset - 1 : offset));
            }
            return b;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new IllegalArgumentException("illegal hex val @ " + offset, aioobe);
        }
    }
}
