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
package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.exception.DecodeException;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    public static IllegalArgumentException illegalArgumentException(ParseException pe) {
        return new IllegalArgumentException(pe);
    }

    public static IllegalArgumentException illegalArgumentException(DecodeException de) {
        return new IllegalArgumentException(de.getMessage());
    }

    public static String validateChars(Pattern pattern, String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            final char c = string.charAt(matcher.start());
            throw new ParseException(
                    "illegal char " + escapeChar(c) + " '" + c + "' @ index " + matcher.start(),
                    matcher.start()
            );
        }
        return string;
    }

    public static String escapeChar(char c) {
        String hex = Integer.toHexString(c);
        switch (hex.length()) {
        case 1: return "\\u000" + hex;
        case 2: return "\\u00" + hex;
        case 3: return "\\u0" + hex;
        case 4: return "\\u" + hex;
        default: throw new Error();
        }
    }

    public static String friendlyClassName(Class<?> clazz) {
        return friendlyClassName(clazz, null);
    }

    public static String friendlyClassName(Class<?> clazz, Integer arrayLength) {
        final String className = clazz.getName();

        final int split = className.lastIndexOf('[') + 1;
        final boolean hasArraySuffix = split > 0;
        final String base = hasArraySuffix ? className.substring(split) : className;
        final StringBuilder sb = new StringBuilder();
        switch (base) {
            case "B": sb.append("byte"); break;
            case "S": sb.append("short"); break;
            case "I": sb.append("int"); break;
            case "J": sb.append("long"); break;
            case "F": sb.append("float"); break;
            case "D": sb.append("double"); break;
            case "C": sb.append("char"); break;
            case "Z": sb.append("boolean"); break;
            default: {
                int lastDotIndex = base.lastIndexOf('.');
                if(lastDotIndex != -1) {
                    sb.append(base, lastDotIndex + 1, base.length() - (base.charAt(0) == 'L' ? 1 : 0));
                }
            }
        }
        if(hasArraySuffix) {
            int i = 0;
            if(arrayLength != null && arrayLength >= 0) {
                sb.append('[').append(arrayLength).append(']');
                i++;
            }
            for ( ; i < split; i++) {
                sb.append("[]");
            }
        }
        return sb.toString();
    }
}
