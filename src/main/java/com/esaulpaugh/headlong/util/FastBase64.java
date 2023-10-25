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

/** A base64 encoder which accepts offset and length arguments.*/
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

    private static volatile short[] standardTable = null;

    private static short[] getStandardTable() {
        if (standardTable == null) {
            synchronized (FastBase64.class) {
                if (standardTable == null) {
                    standardTable = table("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
                }
            }
        }
        return standardTable;
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
        final byte[] out = new byte[encodedSize(len, flags)];
        encodeToBytes(buffer, offset, len, out, 0, flags);
        return new String(out, 0, 0, out.length);
    }

    public static int encodedSize(int inputLen, int flags) {
        final int chunks = inputLen / 3;
        return size(chunks, inputLen - chunks * 3, (flags & NO_PADDING) != 0, (flags & NO_LINE_SEP) != 0);
    }

    private static int size(int chunks, int remainder, boolean noPadding, boolean noLineSep) {
        final int chars = (chunks * 4) + (remainder != 0 ? (noPadding ? remainder + 1 : 4) : 0);
        return noLineSep ? chars : chars + (((chars - 1) / LINE_LEN) * 2);
    }

    public static byte[] encodeToBytes(byte[] buffer, int offset, int len, int flags) {
        byte[] out = new byte[encodedSize(len, flags)];
        encodeToBytes(buffer, offset, len, out, 0, flags);
        return out;
    }

    public static void encodeToBytes(byte[] buffer, int offset, int len, byte[] dest, int destOff, int flags) {
        final int chunks = (len / 3);
        final int evenBytes = chunks * 3;
        final int remainder = len - evenBytes; // bytesLen % 3; // [0,2]
        int charsLeft = 0;
        if (remainder > 0) {
            charsLeft = (flags & NO_PADDING) != 0 ? remainder + 1 : 4;
        }
        final int endEvenBytes = offset + evenBytes; // End of even 24-bits chunks
        final int endEvenChars = destOff + (chunks * 4);
        final short[] table = (flags & URL_SAFE_CHARS) != 0 ? URL_SAFE : getStandardTable();
        if ((flags & NO_LINE_SEP) != 0) {
            encodeMain(buffer, offset, table, endEvenBytes, dest, destOff);
            insertRemainder(buffer, endEvenBytes, remainder, endEvenChars, charsLeft, table, dest);
        } else {
            final int endChars = destOff + size(chunks, remainder, (flags & NO_PADDING) != 0, false);
            final int o = encodeMainLineSep(buffer, offset, table, endEvenBytes, dest, destOff, endChars);
            insertRemainder(buffer, endEvenBytes, remainder, o, charsLeft, table, dest);
        }
    }

    private static void encodeMain(byte[] buffer, int i, short[] table, int end, byte[] dest, int o) {
        while (i < end) {
            final int _24bits = (buffer[i++] & 0xff) << Short.SIZE | (buffer[i++] & 0xff) << Byte.SIZE | (buffer[i++] & 0xff);
            final int ab = table[_24bits >>> 12];
            final int cd = table[_24bits & 0xfff];
            dest[o++] = (byte) (ab >>> Byte.SIZE);
            dest[o++] = (byte) ab;
            dest[o++] = (byte) (cd >>> Byte.SIZE);
            dest[o++] = (byte) cd;
        }
    }

    private static int encodeMainLineSep(byte[] buffer, int i, short[] table, int end, byte[] dest, int o, final int endChars) {
        final int lineSepLimit = endChars - LINE_SEP_LEN;
        for (int quadruples = 0; i < end; ) {
            int _24bits = (buffer[i++] & 0xff) << Short.SIZE | (buffer[i++] & 0xff) << Byte.SIZE | (buffer[i++] & 0xff);
            int ab = table[_24bits >>> 12];
            int cd = table[_24bits & 0xfff];
            dest[o++] = (byte) (ab >>> Byte.SIZE);
            dest[o++] = (byte) ab;
            dest[o++] = (byte) (cd >>> Byte.SIZE);
            dest[o++] = (byte) cd;
            if (++quadruples < (LINE_LEN / 4) || o >= lineSepLimit) {
                continue;
            }
            dest[o++] = '\r';
            dest[o++] = '\n';
            quadruples = 0;
        }
        return o;
    }

    private static void insertRemainder(byte[] buffer, int offset, int remainder, int o, int charsLeft, short[] table, byte[] dest) {
        int bits = 0;
        short thirdChar = PADDING_BYTE;
        switch (remainder) { /* cases fall through */
        case 2:
            bits = (buffer[offset + 1] & 0xff) << 2;
            thirdChar = table[bits & 0x3f];
        case 1:
            bits |= (buffer[offset] & 0xff) << 10;
            switch (charsLeft) { /* cases fall through */
            case 4: dest[o + 3] = PADDING_BYTE;
            case 3: dest[o + 2] = (byte) thirdChar;
            case 2: dest[o + 1] = (byte) table[(bits >> 6) & 0x3f];
            default:dest[o]     = (byte) table[bits >> 12];
            }
        default:
        }
    }
}
