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

public final class FastBase64 {

    private FastBase64() {}

    public static final int NO_FLAGS = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_LINE_SEP = 2; // No "\r\n" after 76 characters
    public static final int URL_SAFE_CHARS = 4;

    private static final int LINE_LEN = 76;
    private static final int LINE_SEP_LEN = 2;
    private static final byte PADDING_BYTE = '=';

    private static final short[] URL_SAFE = table("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_");

    private static final class Standard { // inner class to delay loading of table until called for
        private Standard() {}
        static final short[] TABLE = table("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
    }

    static short[] table(String alphabet) {
        final byte[] bytes = Strings.decode(alphabet, Strings.ASCII);
        final short[] table = new short[1 << 12];
        for (int i = 0, offset = 0; i < bytes.length; i++, offset += bytes.length) {
            final int leftBits = bytes[i] << Byte.SIZE;
            for (int j = 0; j < bytes.length; j++) {
                table[offset + j] = (short) (leftBits | (bytes[j] & 0xFF));
            }
        }
        return table;
    }

    @SuppressWarnings("deprecation")
    public static String encodeToString(byte[] buffer, int offset, int len, int flags) {
        byte[] enc = encodeToBytes(buffer, offset, len, flags);
        return new String(enc, 0, 0, enc.length);
    }

    public static byte[] encodeToBytes(byte[] buffer, int offset, int len, int flags) {
        final int chunks = len / 3;
        final int evenBytes = chunks * 3;
        final int bytesLeft = len - evenBytes; // bytesLen % 3; // [0,2]
        int chars = chunks * 4;
        int charsLeft = 0;
        if(bytesLeft > 0) {
            chars += charsLeft = (flags & NO_PADDING) != 0 ? bytesLeft + 1 : 4;
        }
        final int endEvenBytes = offset + evenBytes; // End of even 24-bits chunks
        final short[] table = (flags & URL_SAFE_CHARS) != 0 ? URL_SAFE : Standard.TABLE;
        final byte[] out = (flags & NO_LINE_SEP) != 0
                ? encodeMain(buffer, offset, table, endEvenBytes, chars)
                : encodeMainLineSep(buffer, offset, table, endEvenBytes, chars + (((chars - 1) / LINE_LEN) * 2));
        insertRemainder(buffer, bytesLeft, charsLeft, table, endEvenBytes, out);
        return out;
    }

    private static byte[] encodeMain(byte[] buffer, int i, short[] table, int end, int len) {
        final byte[] out = new byte[len];
        for (int o = 0; i < end; ) {
            final int _24bits = (buffer[i++] & 0xff) << Short.SIZE | (buffer[i++] & 0xff) << Byte.SIZE | (buffer[i++] & 0xff);
            final int ab = table[_24bits >>> 12];
            final int cd = table[_24bits & 0xfff];
            out[o++] = (byte) (ab >>> Byte.SIZE);
            out[o++] = (byte) ab;
            out[o++] = (byte) (cd >>> Byte.SIZE);
            out[o++] = (byte) cd;
        }
        return out;
    }

    private static byte[] encodeMainLineSep(byte[] buffer, int i, short[] table, int end, int len) {
        final byte[] out = new byte[len];
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

    private static void insertRemainder(byte[] buffer, int numBytes, int numChars, short[] table, int endEvenBytes, byte[] out) {
        int bits = 0;
        short thirdChar = PADDING_BYTE;
        switch (numBytes) { /* cases fall through */
        case 2:
            bits = (buffer[endEvenBytes + 1] & 0xff) << 2;
            thirdChar = table[bits & 0x3f];
        case 1:
            bits |= (buffer[endEvenBytes] & 0xff) << 10;
            final int charsIdx = out.length - numChars;
            switch (numChars) { /* cases fall through */
            case 4: out[charsIdx + 3] = PADDING_BYTE;
            case 3: out[charsIdx + 2] = (byte) thirdChar;
            case 2: out[charsIdx + 1] = (byte) table[(bits >> 6) & 0x3f];
            default:out[charsIdx]     = (byte) table[bits >> 12];
            }
        default:
        }
    }
}
