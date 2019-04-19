package com.esaulpaugh.headlong.util;

import java.nio.charset.Charset;

public class Utils {

    public static final Charset CHARSET_ASCII = Charset.forName("US-ASCII");

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static String escapeChar(char c) {
        String hex = Integer.toHexString((int) c);
        switch (hex.length()) {
        case 1: return "\\u000" + hex;
        case 2: return "\\u00" + hex;
        case 3: return "\\u0" + hex;
        case 4: return "\\u" + hex;
        default: return "\\u0000";
        }
    }
}
