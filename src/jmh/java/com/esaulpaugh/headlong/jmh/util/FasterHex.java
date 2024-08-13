/*
   Copyright 2021 Evan Saulpaugh

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
package com.esaulpaugh.headlong.jmh.util;

import com.esaulpaugh.headlong.util.Integers;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.util.FastHex.BITS_PER_CHAR;
import static com.esaulpaugh.headlong.util.FastHex.CHARS_PER_BYTE;

/** Decode up to 1.6 gigabytes of hexadecimal data per second using a large lookup table. */
public class FasterHex {

    private FasterHex() {}

    private static final short[] DECODE_TABLE = new short[13_159]; // ('f' << 7) + 'f' + 1

    static {
        Arrays.fill(DECODE_TABLE, (byte) -1);
        insertAll('0', '9');
        insertAll('A', 'F');
        insertAll('a', 'f');
    }

    private static void insertAll(int start, int end) {
        for (int i = start; i <= end; i++) {
            for (int j = '0'; j <= '9'; j++) insert(i, j);
            for (int j = 'A'; j <= 'F'; j++) insert(i, j);
            for (int j = 'a'; j <= 'f'; j++) insert(i, j);
        }
    }

    private static void insert(int i, int j) {
        // ASCII values are at most 7 bits
        DECODE_TABLE[i << 7 | j] = (short) (decodeNibble(i) << BITS_PER_CHAR | decodeNibble(j));
    }

    private static int decodeNibble(int c) {
        switch (c) {
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': return c - '0';
        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': return  c - ('A' - 0xA);
        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': return c - ('a' - 0xa);
        default: throw new AssertionError();
        }
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
        final int chunks = dest.length / 4 * 4;
        try {
            int i = 0;
            while (i < chunks) {
                short a = DECODE_TABLE[extractor.applyAsInt(offset++) << 7 | extractor.applyAsInt(offset++)];
                short b = DECODE_TABLE[extractor.applyAsInt(offset++) << 7 | extractor.applyAsInt(offset++)];
                short c = DECODE_TABLE[extractor.applyAsInt(offset++) << 7 | extractor.applyAsInt(offset++)];
                short d = DECODE_TABLE[extractor.applyAsInt(offset++) << 7 | extractor.applyAsInt(offset++)];
                if ((a | b | c | d) < 0) {
                    throw new IllegalArgumentException("invalid hex quadruple @ " + offset);
                }
                dest[i++] = (byte) a;
                dest[i++] = (byte) b;
                dest[i++] = (byte) c;
                dest[i++] = (byte) d;
            }
            while (i < dest.length) {
                int left = DECODE_TABLE[extractor.applyAsInt(offset++) << 7 | extractor.applyAsInt(offset++)];
                if (left < 0) {
                    throw new IllegalArgumentException("invalid hex @ " + offset);
                }
                dest[i++] = (byte) left;
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            throw new IllegalArgumentException("invalid hex quadruple @ " + offset);
        }
        return dest;
    }
}
