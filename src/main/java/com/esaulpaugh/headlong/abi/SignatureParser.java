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

        StringBuilder canonicalTupleType = new StringBuilder("(");
        final int argEnd = parseTuple(signature, startParams, typesOut, illegalTypeCharMatcher, canonicalTupleType);
        String canonical = completeTupleTypeString(canonicalTupleType);

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
                                  final List<StackableType<?>> typesOut,
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
                if (typesOut.size() > 0) {
                    argEnd = argStart - 1;
                }
                break LOOP;
            case ',':
                if (signature.charAt(argStart - 1) == ')') {
                    break LOOP;
                }
                throw new ParseException("empty parameter @ " + typesOut.size(), argStart);
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

                    typesOut.add(childType);

                    canonicalTupleType.append(childType.canonicalType).append(',');

                } catch (ParseException pe) {
                    throw (ParseException) new ParseException(pe.getMessage() + " @ " + typesOut.size(), pe.getErrorOffset()).initCause(pe);
                }

                if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                    break LOOP;
                }
                break;
            default: // non-tuple element
                argEnd = parseNonTuple(signature, argStart, typesOut, illegalTypeCharMatcher, canonicalTupleType);
                if (argEnd == -1 || argEnd >= sigEnd || signature.charAt(argEnd) == ')') {
                    return argEnd;
                }
            }
            argStart = argEnd + 1;
        }

        return argEnd;
    }

    private static String completeTupleTypeString(StringBuilder canonicalTupleType) {
        final int len = canonicalTupleType.length();
        if(len == 1) {
            return "()";
        }
        return canonicalTupleType.replace(len - 1, len, ")").toString(); // replace trailing comma
    }

    private static int parseNonTuple(final String signature,
                                             final int argStart,
                                             final List<StackableType<?>> parentsElements,
                                             final Matcher illegalTypeCharMatcher,
                                             final StringBuilder canonicalTupleType) throws ParseException {

        int argEnd = nextParamTerminator(signature, argStart + 1);
        if (argEnd == -1) {
            return -1;
        }
        checkTypeChars(illegalTypeCharMatcher, signature, argStart, argEnd);

        final String typeString = canonicalizeType(signature.substring(argStart, argEnd)); // , signature, argStart, argEnd

        parentsElements.add(TypeFactory.create(typeString));
        canonicalTupleType.append(typeString).append(',');

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

    private static String canonicalizeType(String rawType) {
        final int rawTypeLen = rawType.length();
        String canonicalized = tryInsertMissingSuffix(rawType, rawTypeLen, "int", "256");
        if(canonicalized == null) {
            canonicalized = tryInsertMissingSuffix(rawType, rawTypeLen, "fixed", "128x18");
        }
        return canonicalized != null ? canonicalized : rawType;
    }

    private static String tryInsertMissingSuffix(String rawType, int rawTypeLen, String prefix, String suffix) {
        final int prefixIndex = rawType.indexOf(prefix);
        if(prefixIndex != -1) {
            final int prefixEnd = prefixIndex + prefix.length();
            if(rawTypeLen - prefixEnd == 0 || rawType.charAt(prefixEnd) == '[') { // ends w/ prefix or is ...prefix[...
                return new StringBuilder()
                        .append(rawType, 0, prefixEnd)
                        .append(suffix)
                        .append(rawType, prefixEnd, rawTypeLen)
                        .toString();
            }
        }
        return null;
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
