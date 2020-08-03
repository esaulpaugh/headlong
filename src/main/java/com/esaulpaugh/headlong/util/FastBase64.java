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
        final boolean noLineSep = (flags & NO_LINE_SEP) != 0;
        final int endEvenBytes = bytesOff + bytesEvenLen; // End of even 24-bits chunks
        final byte[] out = noLineSep ? new byte[rawLen] : new byte[rawLen + (((rawLen - 1) / LINE_LEN) * 2)];
        final byte[] smallTable = urlSafe ? URL_SAFE : STANDARD;
//        if(bytesLen < 250_000) {
            insertQuadruples(buffer, bytesOff, smallTable, endEvenBytes, out, noLineSep);
//        } else {
//            LargeTableEncoder.insertQuadruples(buffer, bytesOff, endEvenBytes, out, urlSafe, noLineSep);
//        }
        insertRemainder(buffer, bytesRemainder, charsLeft, smallTable, endEvenBytes, out);
        return out;
    }

    private static void insertQuadruples(byte[] buffer, int bytesOff, byte[] smallTable, int endEvenBytes, byte[] out, boolean noLineSep) {
        if(noLineSep) {
            insertQuadruplesNoLineSep(buffer, bytesOff, smallTable, endEvenBytes, out);
        } else {
            insertQuadruples(buffer, bytesOff, smallTable, endEvenBytes, out);
        }
    }

    private static void insertQuadruplesNoLineSep(byte[] buffer, int i, byte[] table, int end, byte[] out) {
        int o = 0;
        while (i < end) {
            int _24bits = (buffer[i++] & 0xff) << Short.SIZE
                    | (buffer[i++] & 0xff) << Byte.SIZE
                    | (buffer[i++] & 0xff);
            out[o++] = table[_24bits >>> 18]; // (v >>> 18) & 0x3f
            out[o++] = table[(_24bits >>> 12) & 0x3f];
            out[o++] = table[(_24bits >>> 6) & 0x3f];
            out[o++] = table[_24bits & 0x3f];
        }
    }

    private static void insertQuadruples(byte[] buffer, int i, byte[] table, int end, byte[] out) {
        final int lineSepLimit = out.length - LINE_SEP_LEN;
        int quadruples = 0;
        int o = 0;
        while (i < end) {
            int _24bits = (buffer[i++] & 0xff) << Short.SIZE
                    | (buffer[i++] & 0xff) << Byte.SIZE
                    | (buffer[i++] & 0xff);
            out[o++] = table[_24bits >>> 18];
            out[o++] = table[(_24bits >>> 12) & 0x3f];
            out[o++] = table[(_24bits >>> 6) & 0x3f];
            out[o++] = table[_24bits & 0x3f];
            if (++quadruples < LINE_LEN / 4 || o >= lineSepLimit) {
                continue;
            }
            out[o++] = '\r';
            out[o++] = '\n';
            quadruples = 0;
        }
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

//    private static final class LargeTableEncoder{
//
//        private static final short[] LARGE_STANDARD = new short[1 << 12];
//        private static final short[] LARGE_URL_SAFE = new short[1 << 12];
//
//        static {
//            final int len = STANDARD.length;
//            for (int i = 0; i < len; i++) {
//                final int offset = i * len;
//                for (int j = 0; j < len; j++) {
//                    LARGE_STANDARD[offset + j] = (short) ((STANDARD[i] << Byte.SIZE) | STANDARD[j]);
//                    LARGE_URL_SAFE[offset + j] = (short) ((URL_SAFE[i] << Byte.SIZE) | URL_SAFE[j]);
//                }
//            }
//        }
//
//        private static void insertQuadruples(byte[] buffer, int bytesOff, int endEvenBytes, byte[] out, boolean urlSafe, boolean noLineSep) {
//            short[] table = urlSafe ? LargeTableEncoder.LARGE_URL_SAFE : LargeTableEncoder.LARGE_STANDARD;
//            if(noLineSep) {
//                LargeTableEncoder.insertQuadruplesNoLineSep(buffer, bytesOff, table, endEvenBytes, out);
//            } else {
//                LargeTableEncoder.insertQuadruples(buffer, bytesOff, table, endEvenBytes, out);
//            }
//        }
//
//        private static void insertQuadruplesNoLineSep(byte[] buffer, int i, short[] table, int end, byte[] out) {
//            int o = 0;
//            while (i < end) {
//                int _24bits = (buffer[i++] & 0xff) << Short.SIZE
//                        | (buffer[i++] & 0xff) << Byte.SIZE
//                        | (buffer[i++] & 0xff);
//                int ab = table[_24bits >>> 12];
//                int cd = table[_24bits & 0xfff];
//                out[o++] = (byte) (ab >>> 8);
//                out[o++] = (byte) ab;
//                out[o++] = (byte) (cd >>> 8);
//                out[o++] = (byte) cd;
//            }
//        }
//
//        private static void insertQuadruples(byte[] buffer, int i, short[] table, int end, byte[] out) {
//            final int lineSepLimit = out.length - LINE_SEP_LEN;
//            int o = 0, quadruples = 0;
//            while (i < end) {
//                int _24bits = (buffer[i++] & 0xff) << Short.SIZE
//                        | (buffer[i++] & 0xff) << Byte.SIZE
//                        | (buffer[i++] & 0xff);
//                int ab = table[_24bits >>> 12];
//                int cd = table[_24bits & 0xfff];
//                out[o++] = (byte) (ab >>> Byte.SIZE);
//                out[o++] = (byte) ab;
//                out[o++] = (byte) (cd >>> Byte.SIZE);
//                out[o++] = (byte) cd;
//                if(++quadruples < LINE_LEN / 4 || o >= lineSepLimit) {
//                    continue;
//                }
//                out[o++] = '\r';
//                out[o++] = '\n';
//                quadruples = 0;
//            }
//        }
//    }
}
