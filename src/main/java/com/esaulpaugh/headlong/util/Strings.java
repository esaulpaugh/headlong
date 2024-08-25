/*
   Copyright 2019 Evan Saulpaugh

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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Utility for encoding and decoding hexadecimal, Base64, ASCII, and UTF-8 {@link String}s. */
public final class Strings {

    private Strings() {}

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static final int HEX = 0; // 16
    public static final int UTF_8 = 1; // 256
    public static final int BASE_64_URL_SAFE = 2; // 64
    public static final int ASCII = 3; // 128

    public static String encode(byte b) {
        return encode(new byte[] { b });
    }

    public static String encode(ByteBuffer buf) {
        if (buf.hasArray()) {
            return encode(buf.array());
        } else {
            final int pos = buf.position();
            byte[] bytes = new byte[buf.position(0).limit()];
            buf.get(bytes).position(pos);
            return encode(bytes);
        }
    }

    public static String encode(byte[] bytes) {
        return encode(bytes, HEX);
    }

    public static String encode(byte[] bytes, int encoding) {
        return encode(bytes, 0, bytes.length, encoding);
    }

    public static String encode(byte[] buffer, int from, int len, int encoding) {
        switch (encoding) {
        case HEX: return FastHex.encodeToString(buffer, from, len);
        case UTF_8: return new String(buffer, from, len, StandardCharsets.UTF_8);
        case BASE_64_URL_SAFE: return FastBase64.encodeToString(buffer, from, len, FastBase64.URL_SAFE_CHARS | FastBase64.NO_LINE_SEP | FastBase64.NO_PADDING);
        case ASCII: return new String(buffer, from, len, StandardCharsets.US_ASCII);
        default: throw new UnsupportedOperationException();
        }
    }

    public static byte[] decode(String hex) {
        return decode(hex, HEX);
    }

    public static byte[] decode(String str, int encoding) {
        if (str.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }
        switch (encoding) {
        case HEX: return FastHex.decode(str, 0, str.length());
        case UTF_8: return str.getBytes(StandardCharsets.UTF_8);
        case BASE_64_URL_SAFE: return Base64.getUrlDecoder().decode(str);
        case ASCII: return str.getBytes(StandardCharsets.US_ASCII);
        default: throw new UnsupportedOperationException();
        }
    }
}
