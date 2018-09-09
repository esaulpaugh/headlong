package com.esaulpaugh.headlong.rlp.util;

import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.esaulpaugh.headlong.rlp.util.RLPIntegers.EMPTY_BYTE_ARRAY;

/**
 * Utility for encoding and decoding hexadecimal, base64, and utf-8 encoded {@code String}s.
 */
public class Strings {

    public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    public static final int UTF_8 = 0; // 256
    public static final int BASE64 = 1; // 64
    public static final int HEX = 2; // 16

    public static final boolean WITH_PADDING = true;
    public static final boolean NO_PADDING = false;

    public static String encode(byte[] bytes, int encoding) {
        return encode(bytes, 0, bytes.length, encoding);
    }

    public static String encode(byte[] bytes, int from, int len, int encoding) {
        switch (encoding) {
        case UTF_8: return new String(bytes, from, len, StandardCharsets.UTF_8);
        case BASE64: return toBase64(bytes, from, len, WITH_PADDING);
        case HEX:
        default: return Hex.toHexString(bytes, from, len);
        }
    }

    public static byte[] decode(String string, int encoding) {
        switch (encoding) {
        case UTF_8: return fromUtf8(string);
        case BASE64: return fromBase64(string, WITH_PADDING);
        case HEX:
        default: return fromHex(string);
        }
    }

    public static String toBase64(byte[] bytes, int from, int len, boolean withPadding) {
        if(withPadding) {
            return Base64.toBase64String(bytes, from, len);
        }
        return unpadBase64(Base64.encode(bytes, from, len));
    }

    private static byte[] fromUtf8(String utf8) {
        if(utf8.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        return utf8.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] fromHex(String hex) {
        if(hex.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        return Hex.decode(hex);
    }

    public static byte[] fromBase64(String base64, boolean hasPadding) {
        if(base64.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        if(hasPadding) {
            return Base64.decode(base64);
        }
        return Base64.decode(padBase64(base64));
    }

    public static int calcHexDecodedLen(int encodedLen) {
        return encodedLen >> 1; // div by two
    }

    public static int calcBase64DecodedLen(String base64) {
        return (int) Math.floor(getUnpaddedLen(base64) * 0.75f); // ⌊⌋
    }

    public static int calcDecodedLen(String string, int encoding) {
        switch (encoding) {
        case UTF_8: return string.getBytes(CHARSET_UTF_8).length;
        case BASE64: return calcBase64DecodedLen(string);
        case HEX:
        default: return calcHexDecodedLen(string.length());
        }
    }

    private static int getUnpaddedLen(String base64) {
        final int len = base64.length();
        if(len == 0) return 0;
        if (base64.charAt(len - 1) != '=')
            return len;
        int temp;
        return base64.charAt((temp = len - 2)) == '='
                ? temp
                : len - 1;
    }

    private static String unpadBase64(byte[] padded) { // ascii bytes
        final int len = padded.length;
        if(len < 4) {
            return "";
        }
        int last = len - 1;
        if(padded[last] != '=') {
            return new String(padded, CHARSET_UTF_8);
        }
        int secondToLast = len - 2;
        final int newLen = padded[secondToLast] == '=' ? secondToLast : last;

        return new String(padded, 0, newLen, CHARSET_UTF_8);
    }

    private static byte[] padBase64(String unpadded) {
        final int unpaddedLen = unpadded.length();
        final int remainder = unpadded.length() % 4;
        switch (remainder) {
        case 0: return unpadded.getBytes(CHARSET_UTF_8);
        case 1: break;
        case 2: {
            final int paddedLen = unpaddedLen + 2;
            byte[] bytes = new byte[paddedLen];
            bytes[paddedLen - 2] = '=';
            bytes[paddedLen - 1] = '=';
            unpadded.getBytes(0, unpaddedLen, bytes, 0); // there is no replacement for this method
            return bytes;
        }
        case 3: {
            final int paddedLen = unpaddedLen + 1;
            byte[] bytes = new byte[paddedLen];
            bytes[paddedLen - 1] = '=';
            unpadded.getBytes(0, unpaddedLen, bytes, 0); // there is no replacement for this method
            return bytes;
        }
        }
        throw new IllegalArgumentException("illegal input length: " + unpaddedLen);
    }
}
