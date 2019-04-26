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

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final Charset CHARSET_ASCII = Charset.forName("US-ASCII");

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static String validateChars(Pattern pattern, String name) throws ParseException {
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            final char c = name.charAt(matcher.start());
            throw new ParseException(
                    "illegal char " + escapeChar(c) + " \'" + c + "\' @ index " + matcher.start(),
                    matcher.start()
            );
        }
        return name;
    }

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
