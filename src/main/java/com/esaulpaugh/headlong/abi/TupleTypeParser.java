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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

final class TupleTypeParser {

    static final String ILLEGAL_TUPLE_START = "illegal tuple start";
    static final String EMPTY_PARAMETER = "empty parameter";
    static final String ILLEGAL_TUPLE_TERMINATION = "illegal tuple termination";

    static TupleType parseTupleType(final String rawTypeString) throws ParseException {
        if(rawTypeString.charAt(0) != '(') {
            throw new ParseException(ILLEGAL_TUPLE_START, 0);
        }
        final int end = rawTypeString.length();
        final ArrayList<ABIType<?>> elements = new ArrayList<>();
        final int argEnd = parseTupleType(rawTypeString, end, elements);
        if(argEnd < 0 || argEnd != end - 1 || rawTypeString.charAt(argEnd) != ')') {
            throw new ParseException(ILLEGAL_TUPLE_TERMINATION, Math.max(0, argEnd));
        }
        return TupleType.wrap(elements.toArray(ABIType.EMPTY_TYPE_ARRAY));
    }

    private static int parseTupleType(final String rawTypeString, final int end, final List<ABIType<?>> elements) throws ParseException {
        int argEnd = 1; // this inital value is important for empty params case: "()"
        try {
            int argStart = 1; // after opening '('
            while (argStart < end) {
                int fromIndex;
                char c = rawTypeString.charAt(argStart);
                switch (c) {
                case '[':
                    return argEnd;
                case ')':
                    if(rawTypeString.charAt(argStart - 1) == ',') {
                        throw new ParseException(EMPTY_PARAMETER, argStart);
                    }
                    if (elements.size() > 0) {
                        argEnd = argStart - 1;
                    }
                    return argEnd;
                case ',':
                    if (rawTypeString.charAt(argStart - 1) == ')') {
                        return argEnd;
                    }
                    throw new ParseException(EMPTY_PARAMETER, argStart);
                case '(': // tuple or tuple array
                    fromIndex = findSubtupleEnd(rawTypeString, end, argStart);
                    break;
                default: // non-tuple element
                    fromIndex = argStart + 1;
                }
                argEnd = nextTerminator(rawTypeString, fromIndex);
                if(argEnd == -1) {
                    return -1;
                }
                elements.add(TypeFactory.create(rawTypeString.substring(argStart, argEnd)));
                argStart = argEnd + 1; // jump over terminator
            }
        } catch (ParseException pe) {
            throw (ParseException) new ParseException(
                    "@ index " + elements.size() + ", " + pe.getMessage(),
                    pe.getErrorOffset()
            ).initCause(pe);
        }
        return argEnd;
    }

    private static int findSubtupleEnd(String parentTypeString, final int end, final int subtupleStart) throws ParseException {
        int depth = 1;
        int i = subtupleStart + 1;
        do {
            if(i >= end) {
                throw new ParseException(ILLEGAL_TUPLE_TERMINATION, end);
            }
            char x = parentTypeString.charAt(i++);
            if(x > ')') {
                continue;
            }
            if(x == ')') {
                depth--;
            } else if(x == '(') {
                depth++;
            }
        } while(depth > 0);
        return i;
    }

    private static int nextTerminator(String signature, int i) {
        int comma = signature.indexOf(',', i);
        int close = signature.indexOf(')', i);
        if(comma == -1) {
            return close;
        }
        if(close == -1) {
            return comma;
        }
        return Math.min(comma, close);
    }
}
