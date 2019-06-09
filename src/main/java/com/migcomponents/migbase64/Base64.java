package com.migcomponents.migbase64;

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

    private static final char[] TABLE_STANDARD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final char[] TABLE_URL_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

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
        final boolean noPad = (flags & NO_PADDING) != 0;
        final boolean noLineSep = (flags & NO_LINE_SEP) != 0;
        final char[] table = (flags & URL_SAFE_CHARS) != 0 ? TABLE_URL_SAFE : TABLE_STANDARD;

        final int bytesChunks = bytesLen / 3;
        final int bytesEvenLen = bytesChunks * 3;
        final int bytesRemainder = bytesLen - bytesEvenLen; // bytesLen % 3; // [0,2]
        final int charsRemainder = bytesRemainder == 1 ? 2 : bytesRemainder == 2 ? 3 : 0;

        final int rawLen = (bytesChunks << 2) + (noPad ? charsRemainder : (charsRemainder != 0) ? 4 : 0);
        final int outLen = noLineSep ? rawLen : rawLen + (((rawLen - 1) / 76) << 1);
        final byte[] out = new byte[outLen];

        final int lineSepLim = outLen - 2;
        final int endEvenBytes = bytesOff + bytesEvenLen; // End of even 24-bits chunks
        for (int i = bytesOff, o = 0, chungus = 0; i < endEvenBytes; ) {
            final int v = (buffer[i++] & 0xff) << 16 | (buffer[i++] & 0xff) << 8 | (buffer[i++] & 0xff);
            out[o++] = (byte) table[v >>> 18]; // (v >>> 18) & 0x3f
            out[o++] = (byte) table[(v >>> 12) & 0x3f];
            out[o++] = (byte) table[(v >>> 6) & 0x3f];
            out[o++] = (byte) table[v & 0x3f];
            if (!noLineSep && ++chungus == 19 /* big */ && o < lineSepLim) {
                out[o++] = '\r';
                out[o++] = '\n';
                chungus = 0;
            }
        }
        // Encode remaining bytes (if any)
        if(bytesRemainder > 0) {
            boolean twoBytesLeft = false;
            int v = 0;
            switch (bytesRemainder) { /* cases fall through */
            case 2: v |= (buffer[endEvenBytes + 1] & 0xff) << 2; twoBytesLeft = true;
            case 1: v |= (buffer[endEvenBytes] & 0xff) << 10;
            }
            final int charsLeft = noPad ? charsRemainder : charsRemainder + (twoBytesLeft ? 1 : 2); // plus equals signs
            final int charsIdx = outLen - charsLeft;
            switch (charsLeft) { /* cases fall through */
            case 4:     out[charsIdx + 3]   = (byte) '=';
            case 3:     out[charsIdx + 2]   = (byte) (twoBytesLeft ? table[v & 0x3f] : '=');
            case 2:     out[charsIdx + 1]   = (byte) table[(v >> 6) & 0x3f];
            default:    out[charsIdx]       = (byte) table[v >> 12]; // (v >> 12) & 0x3f
            }
        }
        return out;
    }
}
