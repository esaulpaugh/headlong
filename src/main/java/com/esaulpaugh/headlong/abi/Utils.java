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
package com.esaulpaugh.headlong.abi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Utils {

    static String regexValidate(Pattern validString, Pattern illegalChar, String input) {
        if(validString.matcher(input).matches()) {
            return input;
        }
        Matcher badChar = illegalChar.matcher(input);
        if (badChar.find()) {
            int idx = badChar.start();
            char c = input.charAt(idx);
            throw new IllegalArgumentException("illegal char 0x" + Integer.toHexString(c) + " '" + c + "' @ index " + idx);
        }
        throw new Error("regex mismatch");
    }

    static String friendlyClassName(Class<?> clazz) {
        return friendlyClassName(clazz, null);
    }

    static String friendlyClassName(Class<?> clazz, Integer arrayLength) {
        final String className = clazz.getName();
        final int split = className.lastIndexOf('[') + 1;
        final boolean hasArraySuffix = split > 0;
        final StringBuilder sb = new StringBuilder();
        final String base = hasArraySuffix ? className.substring(split) : className;
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
            while (i++ < split) {
                sb.append("[]");
            }
        }
        return sb.toString();
    }
}
