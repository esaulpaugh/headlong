package com.esaulpaugh.headlong.abi;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureParser {

    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]+");
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile("[^a-z0-9\\[\\](),]+");

    static TupleType parseFunctionSignature(final String signature) throws ParseException {

        List<StackableType<?>> typesOut = new ArrayList<>();

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        final Matcher illegalTypeCharMatcher = HAS_NON_TYPE_CHARS.matcher(signature);

        StringBuilder ctt = new StringBuilder("(");
        final int argEnd = parseTuple(signature, startParams, typesOut, illegalTypeCharMatcher, ctt);
        String canonical = completeTupleTypeString(ctt);

        final int sigEnd = signature.length();

        int terminator = signature.indexOf(')', argEnd);
        if (terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }
        if (argEnd != terminator || terminator != sigEnd - 1) {
            int errorStart = Math.max(0, argEnd);
            throw new ParseException("illegal signature termination: " + signature.substring(errorStart), errorStart);
        }

        return TupleType.create(canonical, typesOut.toArray(StackableType.EMPTY_TYPE_ARRAY));
    }

    private static int parseTuple(final String signature,
                                  final int startParams,
                                  final List<StackableType<?>> tupleTypes,
                                  final Matcher illegalTypeCharMatcher,
                                  final StringBuilder canonicalTupleType) throws ParseException {
        int argStart = startParams + 1;
        int argEnd = argStart; // this inital value is important for empty params case

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
                    StringBuilder ctt = new StringBuilder("(");
                    int result = parseTuple(signature, argStart, innerTupleTypes, illegalTypeCharMatcher, ctt);

                    argEnd = result + 1;

                    final String canonical = completeTupleTypeString(ctt);

                    StackableType<?> childType = TupleType.create(canonical, innerTupleTypes.toArray(StackableType.EMPTY_TYPE_ARRAY));

                    // check for array syntax
                    if (argEnd < sigEnd && signature.charAt(argEnd) == '[') {
                        final int nextTerminator = nextParamTerminator(signature, argEnd);
                        if (nextTerminator > argEnd) {
                            childType = TypeFactory.createForTuple(canonical + signature.substring(argEnd, nextTerminator), (TupleType) childType);
                            argEnd = nextTerminator;
                        }
                    }

                    tupleTypes.add(childType);

                    canonicalTupleType.append(childType.canonicalType).append(',');

                } catch (ParseException pe) {
                    throw (ParseException) new ParseException(pe.getMessage() + " @ " + tupleTypes.size(), pe.getErrorOffset()).initCause(pe);
                }

                if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                    break LOOP;
                }
                break;
            default: // non-tuple element
                argEnd = parseNonTuple(signature, argStart, tupleTypes, illegalTypeCharMatcher, canonicalTupleType);
                if (argEnd == -1 || argEnd >= sigEnd || signature.charAt(argEnd) == ')') {
                    return argEnd;
                }
            }
            argStart = argEnd + 1;
        }

        return argEnd;
    }

    private static int parseNonTuple(final String signature,
                                             final int argStart,
                                             final List<StackableType<?>> tupleTypes,
                                             final Matcher illegalTypeCharMatcher,
                                             final StringBuilder canonicalTupleType) throws ParseException {

        int argEnd = nextParamTerminator(signature, argStart + 1);
        if (argEnd == -1) {
            return -1;
        }
        checkTypeChars(illegalTypeCharMatcher, signature, argStart, argEnd);
        String typeString = signature.substring(argStart, argEnd);
        final String replacement = getCanonicalReplacement(signature, typeString, argStart, argEnd);
        if(replacement != null) {
            typeString = replacement;
        }

        tupleTypes.add(TypeFactory.create(typeString));
        canonicalTupleType.append(typeString).append(',');

        return argEnd;
    }

    private static String completeTupleTypeString(StringBuilder canonicalTupleType) {
        final int n = canonicalTupleType.length();
        if(n > 1) {
            return canonicalTupleType.replace(n - 1, n, ")").toString();
        }
        return canonicalTupleType.append(")").toString();
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
}
