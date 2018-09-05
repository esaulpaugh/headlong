package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SignatureParser {

    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]{1,}");
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile("[^a-z0-9\\[\\](),]{1,}");

    static List<StackableType> parseFunctionSignature(final String signature, final StringBuilder canonicalOut) throws ParseException {

        List<StackableType> typesOut = new ArrayList<>();

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        final Matcher illegalTypeCharMatcher = HAS_NON_TYPE_CHARS.matcher(signature);

        Pair<Integer, Integer> results = parseTuple(signature, startParams, canonicalOut, typesOut, illegalTypeCharMatcher);

        final int argEnd = results.first;
        final int sigEnd = signature.length();

        int terminator = signature.indexOf(')', argEnd);
        if (terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }
        if (argEnd != terminator || terminator != sigEnd - 1) {
            int errorStart = Math.max(0, argEnd);
            throw new ParseException("illegal signature termination: " + signature.substring(errorStart), errorStart);
        }

        final int prevNonCanonicalIndex = results.second; // if 0, signature was already canonical

        canonicalOut.append(signature, prevNonCanonicalIndex, sigEnd);

        return typesOut;
    }

    private static Pair<Integer, Integer> parseTuple(final String signature,
                                                     final int startParams,
                                                     final StringBuilder canonicalOut,
                                                     final List<StackableType> tupleTypes,
                                                     final Matcher illegalTypeCharMatcher) throws ParseException {
        int argStart = startParams + 1;
        int argEnd = argStart; // this inital value is important for empty params case
        int prevNonCanonicalIndex = 0;

        final int sigEnd = signature.length();

        LOOP:
        while (argStart < sigEnd) {
            char c = signature.charAt(argStart);
            switch (c) {
            case '[':
                break LOOP;
            case ')':
                if (tupleTypes.size() > 0) {
                    argEnd = argStart - 1;
                }
                break LOOP;
            case ',':
                if (signature.charAt(argStart - 1) == ')') {
                    break LOOP;
                }
                throw new ParseException("empty parameter @ " + tupleTypes.size(), argStart);
            case '(': { // tuple element
                try {
                    ArrayList<StackableType> innerTupleTypes = new ArrayList<>();
                    Pair<Integer, Integer> results = parseTuple(signature, argStart, canonicalOut, innerTupleTypes, illegalTypeCharMatcher);
                    argEnd = results.first + 1;
                    prevNonCanonicalIndex = results.second;
                    StackableType childType = TupleType.create(null, innerTupleTypes.toArray(StackableType.EMPTY_TYPE_ARRAY)); // don't pass non-canonical type string

                    // check for array syntax
                    if (argEnd < sigEnd && signature.charAt(argEnd) == '[') {
                        final int nextTerminator = nextParamTerminator(signature, argEnd);
                        if (nextTerminator > argEnd) {
                            childType = Typing.createForTuple(signature.substring(argStart, nextTerminator), (TupleType) childType);
                            argEnd = nextTerminator;
                        }
                    }

                    tupleTypes.add(childType);

                } catch (ParseException pe) {
                    throw (ParseException) new ParseException(pe.getMessage() + " @ " + tupleTypes.size(), pe.getErrorOffset()).initCause(pe);
                }

                if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                    break LOOP;
                }

                break;
            }
            default: { // non-tuple element
                argEnd = nextParamTerminator(signature, argStart + 1);
                if (argEnd == -1) {
                    break LOOP;
                }
                checkParamChars(illegalTypeCharMatcher, signature, argStart, argEnd);
                String typeString = signature.substring(argStart, argEnd);
                String canonicalized = canonicalize(signature, typeString, argStart, argEnd);
                if (canonicalized != null) {
                    typeString = canonicalized;
                    canonicalOut.append(signature, prevNonCanonicalIndex, argStart).append(typeString);
                    prevNonCanonicalIndex = argEnd;
                }
                tupleTypes.add(Typing.create(typeString));

                if(argEnd >= sigEnd || signature.charAt(argEnd) == ')') {
                    break LOOP;
                }
            }
            }
            argStart = argEnd + 1;
        }

        return new Pair<>(argEnd, prevNonCanonicalIndex);
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

    private static String canonicalize(String signature, String typeString, int argStart, final int argEnd) {
        final int splitIndex;
        final String piece;
        if (typeString.endsWith("int")) {
            splitIndex = argEnd;
            piece = "256";
        } else if(typeString.endsWith("fixed")) {
            splitIndex = argEnd;
            piece = "128x18";
        } else if(typeString.contains("int[")) {
            splitIndex = signature.indexOf("int", argStart) + "int".length();
            piece = "256";
        } else if(typeString.contains("fixed[")) {
            splitIndex = signature.indexOf("fixed", argStart) + "fixed".length();
            piece = "128x18";
        } else {
            return null;
        }
        return new StringBuilder().append(signature, argStart, splitIndex).append(piece).append(signature, splitIndex, argEnd).toString();
    }

    private static void checkNameChars(String signature, int startParams) throws ParseException {
        Matcher illegalChars = HAS_NON_ASCII_CHARS.matcher(signature).region(0, startParams);
        if(illegalChars.find()) {
            throw newIllegalCharacterException(false, signature, illegalChars.start());
        }
    }

    private static void checkParamChars(Matcher matcher, String signature, int argStart, int argEnd) throws ParseException {
        if (matcher.region(argStart, argEnd).find()) {
            throw newIllegalCharacterException(true, signature, matcher.start());
        }
    }

    private static ParseException newIllegalCharacterException(boolean forNonTypeChar, String signature, int start) {
        char c = signature.charAt(start);
        return new ParseException(
                "non-" + (forNonTypeChar ? "type" : "ascii") + " character at index " + start
                        + ": \'" + c + "\', " + escapeChar(c), start);
    }

    private static String escapeChar(char c) {
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
