package com.migcomponents.migbase64;

import java.nio.charset.StandardCharsets;

/**
 * If you find the code useful or you find a bug, please send me a note at base64 @ miginfocom . com.
 *
 * Licence (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (base64 @ miginfocom . com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 2.2
 * @author Mikael Grev
 *         Date: 2004-aug-02
 *         Time: 11:31:11
 */
public final class Base64 /* Modified by Evan Saulpaugh */ {

    public static final int NO_FLAGS = 0;

    public static final int NO_PADDING = 1;

    public static final int NO_LINE_SEP = 2; // No "\r\n" after 76 characters

    public static final int URL_SAFE_CHARS = 4;

    private static final byte[] TABLE_STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TABLE_URL_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".getBytes(StandardCharsets.US_ASCII);

    /**
     * Encodes a raw byte array into a Base64 <code>String</code>.
     * @param buffer    buffer containing the input. If length 0, an empty array will be returned.
     * @param off       the offset into the buffer of the input bytes
     * @param len       the length of the input, in bytes
     * @param flags     indicating the desired encoding options
     * @return          a Base64-encoded <code>String</code>. Never <code>null</code>.
     */
    @SuppressWarnings("deprecation")
    public static String encodeToString(byte[] buffer, int off, int len, int flags) {
        byte[] enc = encodeToBytes(buffer, off, len, flags);
        return new String(enc, 0, 0, enc.length);
    }

    /**
     * Encodes a raw byte array into a Base64 <code>byte[]</code>.
     * @param buffer    buffer containing the input. If length 0, an empty array will be returned.
     * @param bytesOff  the offset into the buffer of the input bytes
     * @param bytesLen  the length of the input, in bytes
     * @param flags     indicating the desired encoding options
     * @return          a Base64-encoded array. Never <code>null</code>.
     */
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
        final byte[] table = (flags & URL_SAFE_CHARS) != 0 ? TABLE_URL_SAFE : TABLE_STANDARD;
        final int endEvenBytes = bytesOff + bytesEvenLen; // End of even 24-bits chunks
        byte[] out = (flags & NO_LINE_SEP) == 0
                ? encodeChunksLineSep(buffer, bytesOff, table, endEvenBytes, new byte[rawLen + (((rawLen - 1) / 76) * 2)])
                : encodeChunks(buffer, bytesOff, table, endEvenBytes, new byte[rawLen]);
        return encodeRemainder(buffer, bytesRemainder, charsLeft, table, endEvenBytes, out);
    }

    private static byte[] encodeChunks(byte[] buffer, int i, byte[] table, int end, byte[] out) {
        for (int o = 0; i < end; ) {
            final int v = (buffer[i++] & 0xff) << 16 | (buffer[i++] & 0xff) << 8 | (buffer[i++] & 0xff);
            out[o++] = table[v >>> 18]; // (v >>> 18) & 0x3f
            out[o++] = table[(v >>> 12) & 0x3f];
            out[o++] = table[(v >>> 6) & 0x3f];
            out[o++] = table[v & 0x3f];
        }
        return out;
    }

    private static byte[] encodeChunksLineSep(byte[] buffer, int i, byte[] table, int end, byte[] out) {
        final int lineSepLim = out.length - 2;
        for (int o = 0, chungus = 0; i < end; ) {
            final int v = (buffer[i++] & 0xff) << 16 | (buffer[i++] & 0xff) << 8 | (buffer[i++] & 0xff);
            out[o++] = table[v >>> 18]; // (v >>> 18) & 0x3f
            out[o++] = table[(v >>> 12) & 0x3f];
            out[o++] = table[(v >>> 6) & 0x3f];
            out[o++] = table[v & 0x3f];
            if (++chungus == 19 /* big */ && o < lineSepLim) {
                out[o++] = '\r';
                out[o++] = '\n';
                chungus = 0;
            }
        }
        return out;
    }

    private static byte[] encodeRemainder(byte[] buffer, int numBytes, int numChars, byte[] table, int endEvenBytes, byte[] out) {
        int v = 0;
        byte thirdChar = '=';
        switch (numBytes) { /* cases fall through */
        case 2: v = (buffer[endEvenBytes + 1] & 0xff) << 2; thirdChar = table[v & 0x3f];
        case 1: v |= (buffer[endEvenBytes] & 0xff) << 10;
            final int charsIdx = out.length - numChars;
            switch (numChars) { /* cases fall through */
            case 4: out[charsIdx + 3] = (byte) '=';
            case 3: out[charsIdx + 2] = thirdChar;
            case 2: out[charsIdx + 1] = table[(v >> 6) & 0x3f];
            default:out[charsIdx]     = table[v >> 12]; // (v >> 12) & 0x3f
            }
        default: return out;
        }
    }
}
