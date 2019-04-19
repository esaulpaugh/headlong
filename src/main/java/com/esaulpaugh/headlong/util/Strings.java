package com.esaulpaugh.headlong.util;

import com.migcomponents.migbase64.Base64;

import java.nio.charset.Charset;

import static com.esaulpaugh.headlong.util.Utils.EMPTY_BYTE_ARRAY;

/**
 * Utility for encoding and decoding hexadecimal, Base64, and UTF-8-encoded {@code String}s.
 */
public class Strings {

    public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    public static final Charset CHARSET_ASCII = Charset.forName("US-ASCII");

    public static final int UTF_8 = 0; // 256
    public static final int BASE64 = 1; // 64
    public static final int HEX = 2; // 16

    public static final boolean PAD = true;
    public static final boolean DONT_PAD = false;

    public static String encode(byte[] bytes) {
        return FastHex.encodeToString(bytes, 0, bytes.length);
    }

    public static byte[] decode(String encoded) {
        return FastHex.decode(encoded, 0, encoded.length());
    }

    public static String encode(byte[] bytes, int encoding) {
        return encode(bytes, 0, bytes.length, encoding);
    }

    public static String encode(byte[] bytes, int from, int len, int encoding) {
        switch (encoding) {
        case UTF_8: return new String(bytes, from, len, CHARSET_UTF_8);
        case BASE64: return toBase64(bytes, from, len, PAD);
        case HEX:
        default: return FastHex.encodeToString(bytes, from, len);
        }
    }

    public static byte[] decode(String string, int encoding) {
        switch (encoding) {
        case UTF_8: return fromUtf8(string);
        case BASE64: throw new UnsupportedOperationException(); // decoding no longer supported. try spongycastle
        case HEX:
        default: return fromHex(string);
        }
    }

    private static byte[] fromUtf8(String utf8) {
        if(utf8.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        return utf8.getBytes(CHARSET_UTF_8);
    }

    public static String toBase64(byte[] bytes, int from, int len, boolean pad) {
//        final int options = withPadding ? Base64.NO_OPTIONS : Base64.NO_PADDING;
        return Base64.encodeToString(bytes, from, len, false, pad);
    }

    private static byte[] fromHex(String hex) {
        if(hex.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        return FastHex.decode(hex, 0 ,hex.length());
    }
}
