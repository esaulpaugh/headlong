package com.esaulpaugh.headlong.abi;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SignatureParser {

    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]+");
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile("[^a-z0-9\\[\\](),]+");

    static List<StackableType<?>> parseFunctionSignature(final String signature, final StringBuilder canonicalOut) throws ParseException {

        List<StackableType<?>> typesOut = new ArrayList<>();

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        final Matcher illegalTypeCharMatcher = HAS_NON_TYPE_CHARS.matcher(signature);

        ParseResult result = parseTuple(signature, startParams, canonicalOut, typesOut, illegalTypeCharMatcher);

        final int argEnd = result.argumentEnd;
        final int sigEnd = signature.length();

        int terminator = signature.indexOf(')', argEnd);
        if (terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }
        if (argEnd != terminator || terminator != sigEnd - 1) {
            int errorStart = Math.max(0, argEnd);
            throw new ParseException("illegal signature termination: " + signature.substring(errorStart), errorStart);
        }

        canonicalOut.append(signature, result.previousNonCanonicalIndex, sigEnd);// if prevNonCanonicalIndex == 0, signature was already canonical

        return typesOut;
    }

    private static ParseResult parseTuple(final String signature,
                                                     final int startParams,
                                                     final StringBuilder canonicalOut,
                                                     final List<StackableType<?>> tupleTypes,
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
            case '(': // tuple element
                try {
                    ArrayList<StackableType<?>> innerTupleTypes = new ArrayList<>();
                    ParseResult result = parseTuple(signature, argStart, canonicalOut, innerTupleTypes, illegalTypeCharMatcher);
                    argEnd = result.argumentEnd + 1;
                    prevNonCanonicalIndex = result.previousNonCanonicalIndex;

                    String nonCanonical = signature.substring(argStart, argEnd);
//                    if(canonicalTypeString.charAt(0) != '(' || canonicalTypeString.charAt(canonicalTypeString.length() - 1) != ')') {
//                        throw new Error();
//                    }
                    // TODO passing non-canonical but create expects canonical
                    StackableType<?> childType = TupleType.create(nonCanonical, innerTupleTypes.toArray(StackableType.EMPTY_TYPE_ARRAY));

                    // check for array syntax
                    if (argEnd < sigEnd && signature.charAt(argEnd) == '[') {
                        final int nextTerminator = nextParamTerminator(signature, argEnd);
                        if (nextTerminator > argEnd) {
                            childType = TypeFactory.createForTuple(signature.substring(argStart, nextTerminator), (TupleType) childType);
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
            default: // non-tuple element
                ParseResult result = parseNonTuple(signature, argStart, canonicalOut, prevNonCanonicalIndex, tupleTypes, illegalTypeCharMatcher);
                argEnd = result.argumentEnd;
                if(argEnd == -1 || argEnd >= sigEnd || signature.charAt(argEnd) == ')') {
                    return result;
                }
                prevNonCanonicalIndex = result.previousNonCanonicalIndex;
            }
            argStart = argEnd + 1;
        }

        return new ParseResult(argEnd, prevNonCanonicalIndex);
    }

    private static ParseResult parseNonTuple(final String signature,
                                                        final int argStart,
                                                        final StringBuilder canonicalOut,
                                                        int prevNonCanonicalIndex,
                                                        final List<StackableType<?>> tupleTypes,
                                                        final Matcher illegalTypeCharMatcher) throws ParseException {
        int argEnd = nextParamTerminator(signature, argStart + 1);
        if (argEnd == -1) {
            return new ParseResult(-1, prevNonCanonicalIndex);
        }
        checkTypeChars(illegalTypeCharMatcher, signature, argStart, argEnd);
        final String typeString = signature.substring(argStart, argEnd);
        final String replacement = getCanonicalReplacement(signature, typeString, argStart, argEnd);
        if (replacement == null) {
            tupleTypes.add(TypeFactory.create(typeString));
        } else {
            tupleTypes.add(TypeFactory.create(replacement));
            canonicalOut.append(signature, prevNonCanonicalIndex, argStart).append(replacement);
            prevNonCanonicalIndex = argEnd;
        }

        return new ParseResult(argEnd, prevNonCanonicalIndex);
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

    private static String getCanonicalReplacement(String signature, String typeString, int argStart, final int argEnd) {
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

    private static void checkTypeChars(Matcher matcher, String signature, int argStart, int argEnd) throws ParseException {
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

    private static final class ParseResult {
        final int argumentEnd;
        final int previousNonCanonicalIndex;

        ParseResult(int argumentEnd, int previousNonCanonicalIndex) {
            this.argumentEnd = argumentEnd;
            this.previousNonCanonicalIndex = previousNonCanonicalIndex;
        }
    }
}
