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
package com.esaulpaugh.headlong.util;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.util.FastHex.BITS_PER_CHAR;
import static com.esaulpaugh.headlong.util.FastHex.CHARS_PER_BYTE;
import static com.esaulpaugh.headlong.util.FastHex.decodeNibble;

/** Decode up to 1.6 gigabytes of hexadecimal data per second using a large lookup table. */
public class FasterHex {

    private static final short[] DECODE_TABLE = new short[13_159]; // ('f' << 7) + 'f' + 1

    static {
        Arrays.fill(DECODE_TABLE, (byte) -1);
        insert('0', '9');
        insert('A', 'F');
        insert('a', 'f');
    }

    private static void insert(int start, int end) {
        final int o = -1;
        for (int i = start; i <= end; i++) {
            for (int j = '0'; j <= '9'; j++) DECODE_TABLE[i << 7 | j] = (short) (decodeNibble(i, o) << BITS_PER_CHAR | decodeNibble(j, o)); // ASCII values are at most 7 bits
            for (int j = 'A'; j <= 'F'; j++) DECODE_TABLE[i << 7 | j] = (short) (decodeNibble(i, o) << BITS_PER_CHAR | decodeNibble(j, o));
            for (int j = 'a'; j <= 'f'; j++) DECODE_TABLE[i << 7 | j] = (short) (decodeNibble(i, o) << BITS_PER_CHAR | decodeNibble(j, o));
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
        if (Integers.mod(len, CHARS_PER_BYTE) != 0) {
            throw new IllegalArgumentException("len must be a multiple of two");
        }
        byte[] dest = new byte[len / CHARS_PER_BYTE];
        for (int i = 0; i < dest.length; i++, offset += CHARS_PER_BYTE) {
            dest[i] = (byte) decodeByte(extractor, offset);
        }
        return dest;
    }

    private static short decodeByte(IntUnaryOperator extractor, int offset) {
        try {
            short val = DECODE_TABLE[extractor.applyAsInt(offset) << 7 | extractor.applyAsInt(offset+1)];
            if(val >= 0) {
                return val;
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            /* fall through */
        }
        throw new IllegalArgumentException("invalid hex pair @ " + offset);
    }
}
