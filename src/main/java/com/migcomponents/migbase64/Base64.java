package com.migcomponents.migbase64;

/**
 * A very fast and memory efficient class to encode and decode to and from BASE64 in full accordance
 * with RFC 2045.<br><br>
 * On Windows XP sp1 with 1.4.2_04 and later ;), this encoder and decoder is about 10 times faster
 * on small arrays (10 - 1000 bytes) and 2-3 times as fast on larger arrays (10000 - 1000000 bytes)
 * compared to <code>sun.misc.Encoder()/Decoder()</code>.<br><br>
 *
 * On byte arrays the encoder is about 20% faster than Jakarta Commons Base64 Codec for encode and
 * about 50% faster for decoding large arrays. This implementation is about twice as fast on very small
 * arrays (less than 30 bytes). If source/destination is a <code>String</code> this
 * version is about three times as fast due to the fact that the Commons Codec result has to be recoded
 * to a <code>String</code> from <code>byte[]</code>, which is very expensive.<br><br>
 *
 * This encode/decode algorithm doesn't create any temporary arrays as many other codecs do, it only
 * allocates the resulting array. This produces less garbage and it is possible to handle arrays twice
 * as large as algorithms that create a temporary array. (E.g. Jakarta Commons Codec). It is unknown
 * whether Sun's <code>sun.misc.Encoder()/Decoder()</code> produce temporary arrays but since performance
 * is quite low it probably does.<br><br>
 *
 * The encoder produces the same output as the Sun one except that the Sun's encoder appends
 * a trailing line separator if the last character isn't a pad. Unclear why but it only adds to the
 * length and is probably a side effect. Both are in conformance with RFC 2045 though.<br>
 * Commons codec seem to always att a trailing line separator.<br><br>
 *
 * <b>Note!</b>
 * The encode/decode method pairs (types) come in three versions with the <b>exact</b> same algorithm and
 * thus a lot of code redundancy. This is to not create any temporary arrays for transcoding to/from different
 * format types. The methods not used can simply be commented out.<br><br>
 *
 * There is also a "fast" version of all decode methods that works the same way as the normal ones, but
 * har a few demands on the decoded input. Normally though, these fast verions should be used if the source if
 * the input is known and it hasn't bee tampered with.<br><br>
 *
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

public class Base64 /* Modified by Evan Saulpaugh */
{
    private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    // ****************************************************************************************
    // *  char[] version
    // ****************************************************************************************

    /**
     * Encodes a raw byte array into a BASE64 <code>char[]</code> representation in accordance with RFC 2045.
     * @param sArr buffer containing the input. If <code>null</code> or length 0, an empty array will be returned.
     * @param offset the offset into the buffer of the input bytes
     * @param len   the length of the input, in bytes
     * @param lineSep Optional "\r\n" after 76 characters, unless end of file.<br>
     * No line separator will be in breach of RFC 2045 which specifies max 76 per line but will be a
     * little faster.
     * @param pad    false if the output should not be padded by equals signs
     * @return A BASE64 encoded array. Never <code>null</code>.
     */
    public static char[] encodeToChars(byte[] sArr, final int offset, final int len, boolean lineSep, boolean pad) {
        // Check special case
        if (sArr == null || len == 0) {
            return EMPTY_CHAR_ARRAY;
        }

        int remainder = len % 3;
        final int strRemainder;
        if(remainder == 1) {
            strRemainder = 2;
        } else if(remainder == 2) {
            strRemainder = 3;
        } else {
            strRemainder = 0;
        }

        final int eLen = (len / 3) << 2;
        final int dLen;
        if(pad) {
            int cCnt = strRemainder == 0 ? eLen : eLen + 4; // ((sLen - 1) / 3 + 1) << 2
            dLen = cCnt + (lineSep ? (cCnt - 1) / 76 << 1 : 0);
        } else {
            dLen = eLen + strRemainder;
        }
        char[] dArr = new char[dLen];

        // Encode even 24-bits
        final int evenEnd = offset + ((len / 3) * 3);   // End of even 24-bits chunks
        final int lineSepLim = dLen - 2;
        int s = offset;
        for (int d = 0, cc = 0; s < evenEnd; ) {
            // Copy next three bytes into lower 24 bits of int, paying attension to sign.
            int i = (sArr[s++] & 0xff) << 16 | (sArr[s++] & 0xff) << 8 | (sArr[s++] & 0xff);

            // Encode the int into four chars
            dArr[d++] = CA[(i >>> 18) & 0x3f];
            dArr[d++] = CA[(i >>> 12) & 0x3f];
            dArr[d++] = CA[(i >>> 6) & 0x3f];
            dArr[d++] = CA[i & 0x3f];

            // Add optional line separator
            if (lineSep && ++cc == 19 && d < lineSepLim) {
                dArr[d++] = '\r';
                dArr[d++] = '\n';
                cc = 0;
            }
        }

        // Pad and encode last bits if source isn't even 24 bits.
        final int end = offset + len;
        final int left = end - s; // 0 - 2.
        if (left > 0) {
            boolean twoLeft = left == 2;
            int i = (sArr[evenEnd] & 0xff) << 10;
            if(twoLeft) {
                i |= (sArr[end - 1] & 0xff) << 2;
            }
            if(pad) {
                // Set last four chars
                dArr[dLen - 4] = CA[i >> 12];
                dArr[dLen - 3] = CA[(i >>> 6) & 0x3f];
                dArr[dLen - 2] = twoLeft ? CA[i & 0x3f] : '=';
                dArr[dLen - 1] = '=';
            } else {
                // Set last strRemainder chars
                int idx = dLen - strRemainder;
                dArr[idx] = CA[(i >> 12) & 0x3f];
                if(++idx < dLen) {
                    dArr[idx] = CA[(i >> 6) & 0x3f];
                }
            }
        }
        return dArr;
    }

    // ****************************************************************************************
    // * String version
    // ****************************************************************************************

    /**
     * Encodes a raw byte array into a BASE64 <code>String</code> representation in accordance with RFC 2045.
     * @param buffer buffer containing the input. If <code>null</code> or length 0 an empty string will be returned.
     * @param lineSep Optional "\r\n" after 76 characters, unless end of file.<br>
     * No line separator will be in breach of RFC 2045 which specifies max 76 per line but will be a
     * little faster.
     * @param offset the offset into the buffer of the input bytes
     * @param len   the length of the input, in bytes
     * @param pad    false if the output should not be padded by equals signs
     * @return A BASE64 encoded array. Never <code>null</code>.
     */
    public static String encodeToString(byte[] buffer, int offset, int len, boolean lineSep, boolean pad) {
        // Reuse char[] since we can't create a String incrementally anyway and StringBuffer/Builder would be slower.
        return new String(encodeToChars(buffer, offset, len, lineSep, pad));
    }
}