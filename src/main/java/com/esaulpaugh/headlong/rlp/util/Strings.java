package com.esaulpaugh.headlong.rlp.util;

import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.Charset;

import static org.apache.commons.lang3.ArrayUtils.EMPTY_BYTE_ARRAY;

public class Strings {

    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    public static final int HEX = 16;
    public static final int BASE64 = 64;
    public static final int UTF_8 = 256;

    public static final boolean WITH_PADDING = true;
    public static final boolean NO_PADDING = false;

    public static String encode(byte[] bytes, int encoding) {
        return encode(bytes, 0, bytes.length, encoding);
    }

    public static String encode(byte[] bytes, int from, int len, int encoding) {
        switch (encoding) {
        case UTF_8: return toUtf8(bytes, from, len);
        case BASE64: return toBase64(bytes, from, len, NO_PADDING);
        case HEX:
        default: return toHex(bytes, from, len);
        }
    }

    public static byte[] decode(String string, int encoding) {
        if(string.isEmpty())
            return EMPTY_BYTE_ARRAY;
        switch (encoding) {
        case UTF_8: return fromUtf8(string);
        case BASE64: return fromBase64(string, NO_PADDING);
        case HEX:
        default: return fromHex(string);
        }
    }

    private static String toUtf8(byte[] bytes, int from, int len) {
        return new String(bytes, from, len, CHARSET_UTF_8);
    }

    private static byte[] fromUtf8(String utf8) {
        return utf8.getBytes(CHARSET_UTF_8);
    }

    private static String toHex(byte[] bytes, int from, int len) {
        return Hex.toHexString(bytes, from, len);
    }

    private static byte[] fromHex(String hex) {
        return Hex.decode(hex);
    }

    private static String toBase64(byte[] bytes, int from, int len, boolean withPadding) {
        if(withPadding) {
            return Base64.toBase64String(bytes, from, len);
        }
        return unpad(Base64.encode(bytes, from, len));
    }

    private static byte[] fromBase64(String base64, boolean hasPadding) {
        if(hasPadding) {
            return Base64.decode(base64);
        }
        return Base64.decode(pad(base64));
    }

    public static int calcHexDecodedLen(int encodedLen) {
        return encodedLen >> 1; // div by two
    }

    public static int calcBase64DecodedLen(String base64) {
        return (int) Math.floor(getUnpaddedLen(base64) * 0.75f); // ⌊⌋
    }

    public static int calcDecodedLen(String string, int encoding) {
        switch (encoding) {
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

    private static String unpad(byte[] padded) {
        final int len = padded.length;
        if(len < 4) {
            return "";
        }
        int minus1 = len - 1;
        if(padded[minus1] != '=') {
            return org.spongycastle.util.Strings.fromByteArray(padded);
        }
        int minus2 = len - 2;
        final int newLen = padded[minus2] == '=' ? minus2 : minus1;
        char[] chars = new char[newLen];
        for (int i = 0; i < newLen; i++) {
            chars[i] = (char) (padded[i] & 0xFF);
        }
        return String.valueOf(chars);
    }

    private static byte[] pad(String unpadded) {
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
            unpadded.getBytes(0, unpaddedLen, bytes, 0);
            return bytes;
        }
        case 3: {
            final int paddedLen = unpaddedLen + 1;
            byte[] bytes = new byte[paddedLen];
            bytes[paddedLen - 1] = '=';
            unpadded.getBytes(0, unpaddedLen, bytes, 0);
            return bytes;
        }
        }
        throw new IllegalArgumentException("illegal input length: " + unpaddedLen);
    }
}
