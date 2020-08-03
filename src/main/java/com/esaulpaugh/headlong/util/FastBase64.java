/*
   Copyright 2020 Evan Saulpaugh

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

public class FastBase64 {

    public static final int NO_FLAGS = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_LINE_SEP = 2; // No "\r\n" after 76 characters
    public static final int URL_SAFE_CHARS = 4;

    private static final byte[] STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] URL_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".getBytes(StandardCharsets.US_ASCII);

    private static final int LINE_LEN = 76;
    private static final int LINE_SEP_LEN = 2;
    private static final byte PADDING_BYTE = '=';

    @SuppressWarnings("deprecation")
    public static String encodeToString(byte[] buffer, int off, int len, int flags) {
        byte[] enc = encodeToBytes(buffer, off, len, flags);
        return new String(enc, 0, 0, enc.length);
    }

    public static byte[] encodeToBytes(final byte[] buffer, final int bytesOff, final int bytesLen, final int flags) {
        final int bytesChunks = bytesLen / 3;
        final int bytesEvenLen = bytesChunks * 3;
        final int bytesRemainder = bytesLen - bytesEvenLen; // bytesLen % 3; // [0,2]
        int charsLeft = 0;
        int rawLen = bytesChunks * 4;
        if ((flags & NO_PADDING) != 0) {
            switch (bytesRemainder) {
            case 2: charsLeft = 3; break;
            case 1: charsLeft = 2;
            }
            rawLen += charsLeft;
        } else if (bytesRemainder > 0) {
            rawLen += charsLeft = 4;
        }
        final boolean urlSafe = (flags & URL_SAFE_CHARS) != 0;
        final int endEvenBytes = bytesOff + bytesEvenLen; // End of even 24-bits chunks
        final byte[] out = (flags & NO_LINE_SEP) != 0
                ? encodeMain(buffer, bytesOff, urlSafe ? BigUrlSafe.TABLE : BigStandard.TABLE, endEvenBytes, rawLen)
                : encodeMainLineSep(buffer, bytesOff, urlSafe ? BigUrlSafe.TABLE : BigStandard.TABLE, endEvenBytes, rawLen + (((rawLen - 1) / LINE_LEN) * 2));
        insertRemainder(buffer, bytesRemainder, charsLeft, urlSafe ? URL_SAFE : STANDARD, endEvenBytes, out);
        return out;
    }

    private static byte[] encodeMain(byte[] buffer, int i, short[] table, int end, int len) {
        byte[] out = new byte[len];
        for (int o = 0; i < end; ) {
            int _24bits = (buffer[i++] & 0xff) << Short.SIZE | (buffer[i++] & 0xff) << Byte.SIZE | (buffer[i++] & 0xff);
            int ab = table[_24bits >>> 12];
            int cd = table[_24bits & 0xfff];
            out[o++] = (byte) (ab >>> Byte.SIZE);
            out[o++] = (byte) ab;
            out[o++] = (byte) (cd >>> Byte.SIZE);
            out[o++] = (byte) cd;
        }
        return out;
    }

    private static byte[] encodeMainLineSep(byte[] buffer, int i, short[] table, int end, int len) {
        byte[] out = new byte[len];
        int quadruples = 0;
        final int lineSepLimit = out.length - LINE_SEP_LEN;
        for (int o = 0; i < end; ) {
            int _24bits = (buffer[i++] & 0xff) << Short.SIZE | (buffer[i++] & 0xff) << Byte.SIZE | (buffer[i++] & 0xff);
            int ab = table[_24bits >>> 12];
            int cd = table[_24bits & 0xfff];
            out[o++] = (byte) (ab >>> Byte.SIZE);
            out[o++] = (byte) ab;
            out[o++] = (byte) (cd >>> Byte.SIZE);
            out[o++] = (byte) cd;
            if (++quadruples < (LINE_LEN / 4) || o >= lineSepLimit) {
                continue;
            }
            out[o++] = '\r';
            out[o++] = '\n';
            quadruples = 0;
        }
        return out;
    }

    private static void insertRemainder(byte[] buffer, int numBytes, int numChars, byte[] table, int endEvenBytes, byte[] out) {
        int bits = 0;
        byte thirdChar = PADDING_BYTE;
        switch (numBytes) { /* cases fall through */
        case 2:
            bits = (buffer[endEvenBytes + 1] & 0xff) << 2;
            thirdChar = table[bits & 0x3f];
        case 1:
            bits |= (buffer[endEvenBytes] & 0xff) << 10;
            final int charsIdx = out.length - numChars;
            switch (numChars) { /* cases fall through */
            case 4: out[charsIdx + 3] = PADDING_BYTE;
            case 3: out[charsIdx + 2] = thirdChar;
            case 2: out[charsIdx + 1] = table[(bits >> 6) & 0x3f];
            default:out[charsIdx]     = table[bits >> 12];
            }
        default:
        }
    }

    private static final class BigStandard {
        private static final short[] TABLE = initBig(STANDARD);
    }

    private static final class BigUrlSafe {
        private static final short[] TABLE = initBig(URL_SAFE);
    }

    private static short[] initBig(byte[] smallTable) {
        final short[] largeTable = new short[1 << 12];
        final int len = smallTable.length;
        for (int i = 0; i < len; i++) {
            final int offset = i * len;
            for (int j = 0; j < len; j++) {
                largeTable[offset + j] = (short) ((smallTable[i] << Byte.SIZE) | smallTable[j]);
            }
        }
        return largeTable;
    }
}
