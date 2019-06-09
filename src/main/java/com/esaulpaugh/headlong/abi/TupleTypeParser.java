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

    static final String EMPTY_PARAMETER = "empty parameter";
    private static final String NON_TERMINATING_TUPLE = "non-terminating tuple";

    static TupleType parseTupleType(final String rawTupleTypeString) throws ParseException {

        if(rawTupleTypeString.charAt(0) != '(') {
            throw new ParseException("params start not found", 0);
        }

        final ArrayList<ABIType<?>> typesOut = new ArrayList<>();
        final int argEnd = parseTupleType(rawTupleTypeString, typesOut);

        final int terminator = rawTupleTypeString.indexOf(')', argEnd);
        final int end = rawTupleTypeString.length();
        if (terminator == -1) {
            throw new ParseException(NON_TERMINATING_TUPLE, end);
        }
        if (argEnd != terminator || terminator != end - 1) {
            int errStart = Math.max(0, argEnd);
            throw new ParseException("illegal tuple termination: " + rawTupleTypeString.substring(errStart), errStart);
        }

        return TupleType.wrap(typesOut.toArray(ABIType.EMPTY_TYPE_ARRAY));
    }

    private static int parseTupleType(final String signature, final List<ABIType<?>> typesOut) throws ParseException {
        final int sigEnd = signature.length();

        int argStart = 1;
        int argEnd = argStart; // this inital value is important for empty params case

        try {
            while (argStart < sigEnd) {
                char c = signature.charAt(argStart);
                switch (c) {
                case '[':
                    return argEnd;
                case ')':
                    if(signature.charAt(argStart - 1) == ',') {
                        throw new ParseException(EMPTY_PARAMETER, argStart);
                    }
                    if (typesOut.size() > 0) {
                        argEnd = argStart - 1;
                    }
                    return argEnd;
                case ',':
                    if (signature.charAt(argStart - 1) == ')') {
                        return argEnd;
                    }
                    throw new ParseException(EMPTY_PARAMETER, argStart);
                case '(': // tuple or tuple array
                    argEnd = nextParamTerminator(signature, findTupleEnd(signature, argStart, sigEnd));
                    typesOut.add(TypeFactory.create(signature.substring(argStart, argEnd)));
                    if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                        return argEnd;
                    }
                    break;
                default: // non-tuple element
                    argEnd = nextParamTerminator(signature, argStart + 1);
                    if(argEnd == -1) {
                        return -1;
                    } else {
                        typesOut.add(TypeFactory.create(signature.substring(argStart, argEnd)));
                        if (argEnd >= sigEnd || signature.charAt(argEnd) == ')') {
                            return argEnd;
                        }
                    }
                }
                argStart = argEnd + 1;
            }
        } catch (ParseException pe) {
            throw (ParseException) new ParseException(
                    "@ index " + typesOut.size() + ", " + pe.getMessage(),
                    pe.getErrorOffset()
            ).initCause(pe);
        }
        return argEnd;
    }

    private static int findTupleEnd(String signature, final int tupleStart, final int sigEnd) throws ParseException {
        int depth = 1;
        int i = tupleStart + 1;
        do {
            if(i >= sigEnd) {
                throw new ParseException(NON_TERMINATING_TUPLE, sigEnd);
            }
            char x = signature.charAt(i++);
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

    private static int nextParamTerminator(String signature, int i) {
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
