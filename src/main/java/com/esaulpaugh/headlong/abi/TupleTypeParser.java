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

    static TupleType parseTupleType(final String rawTupleTypeString) throws ParseException {

        if(rawTupleTypeString.charAt(0) != '(') {
            throw new ParseException("params start not found", 0);
        }

        final ArrayList<ABIType<?>> typesOut = new ArrayList<>();
        final int argEnd = parseTupleType(rawTupleTypeString, 0, typesOut);

        final int terminator = rawTupleTypeString.indexOf(')', argEnd);
        final int end = rawTupleTypeString.length();
        if (terminator == -1) {
            throw new ParseException("non-terminating tuple", end);
        }
        if (argEnd != terminator || terminator != end - 1) {
            int errStart = Math.max(0, argEnd);
            throw new ParseException("illegal tuple termination: " + rawTupleTypeString.substring(errStart), errStart);
        }

        return TupleType.create(typesOut);
    }

    private static int parseTupleType(final String signature,
                                      final int startParams,
                                      final List<ABIType<?>> typesOut) throws ParseException {
        final int sigEnd = signature.length();

        int argStart = startParams + 1;
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
                case '(': // tuple element
                    ArrayList<ABIType<?>> innerList = new ArrayList<>();

                    argEnd = parseTupleType(signature, argStart, innerList) + 1; // +1 to skip over trailing ')'

                    TupleType tupleType = TupleType.create(innerList);

                    // check for suffix i.e. array syntax
                    if (argEnd < sigEnd && signature.charAt(argEnd) == '[') { // TODO allow parsing of non-tuple types by end-user
                        final int nextTerminator = nextParamTerminator(signature, argEnd);
                        if (nextTerminator > argEnd) {
                            String suffix = signature.substring(argEnd, nextTerminator); // e.g. "[4][]"
                            typesOut.add(TypeFactory.createForTuple(tupleType, suffix, null));
                            argEnd = nextTerminator;
                        }
                    } else {
                        typesOut.add(tupleType);
                    }
                    if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                        return argEnd;
                    }
                    break /* switch */;
                default: // non-tuple element
                    argEnd = nextParamTerminator(signature, argStart + 1);
                    if(argEnd == -1) {
                        return -1;
                    } else {
                        typesOut.add(TypeFactory.create(signature.substring(argStart, argEnd), null));
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