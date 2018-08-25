package com.esaulpaugh.headlong.abi;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

// TODO encode and decode
// TODO optimize -- maybe write all zeroes first then fill in args
public class ABI {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    private static final String REGEX_NON_ASCII_CHAR = "[^\\p{ASCII}]{1,}";
    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile(REGEX_NON_ASCII_CHAR);

    private static final String REGEX_NON_TYPE_CHAR = "[^a-z0-9\\[\\](),]{1,}";
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile(REGEX_NON_TYPE_CHAR);

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

    private static ParseException newIllegalCharacterException(boolean forNonTypeChar, String signature, int start) {
        char c = signature.charAt(start);
        return new ParseException(
                "non-" + (forNonTypeChar ? "type" : "ascii") + " character at index " + start
                        + ": \'" + c + "\', " + escapeChar(c), start);
    }

    private static void checkNameChars(String signature, int startParams) throws ParseException {
        Matcher illegalChars = HAS_NON_ASCII_CHARS.matcher(signature).region(0, startParams);
        if(illegalChars.find()) {
            throw newIllegalCharacterException(false, signature, illegalChars.start());
        }
    }

    private static void checkParamChars(Matcher matcher, String signature, int argStart, int argEnd) throws ParseException {
        Matcher illegalChars = matcher.region(argStart, argEnd);
        if (illegalChars.find()) {
            throw newIllegalCharacterException(true, signature, illegalChars.start());
        }
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

    /**
     * Parses an Ethereum ABI-compatible function signature, outputting its canonical form and its parameters'
     * {@code Types}.
     *
     * @param signature     the signature
     * @param canonicalOut  the destination for the canonical form of the signature
     * @param types         the destination for the function parameters' {@code Type}s.
     * @return              true if the output signature differs from the input signature due to canonicalization
     * @throws ParseException if the input is malformed
     */
    public static boolean parseFunctionSignature(final String signature, final StringBuilder canonicalOut, final List<Type> types) throws ParseException {
        System.out.println("signature: " + escapeJava(signature));

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        final Matcher illegalTypeCharMatcher = HAS_NON_TYPE_CHARS.matcher(signature);

        Pair<Integer, Integer> results;
        try {
            results = parseTuple(signature, startParams, canonicalOut, types, illegalTypeCharMatcher);
        } catch (NonTerminationException nte) {
            throw (ParseException) new ParseException("non-terminating signature", nte.getErrorOffset()).initCause(nte);
        } catch (EmptyParameterException epe) {
            throw (ParseException) new ParseException(epe.getMessage(), epe.getErrorOffset()).initCause(epe);
        }

        final int argEnd = results.getLeft();
        final int sigEnd = signature.length();

        int terminator = signature.indexOf(')', argEnd);
        if (terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }
        if (argEnd != terminator || terminator != sigEnd - 1) {
            throw new ParseException("illegal signature termination: " + signature.substring(Math.max(0, argEnd)), argEnd);
        }

        final int prevNonCanonicalIndex = results.getRight();

        canonicalOut.append(signature, prevNonCanonicalIndex, sigEnd);

        System.out.println("canonical: " + canonicalOut.toString());

        return prevNonCanonicalIndex != 0;
    }

    private static Pair<Integer, Integer> parseTuple(final String signature,
                                                     final int startParams,
                                                     final StringBuilder canonicalOut,
                                                     final List<Type> tupleTypes,
                                                     final Matcher illegalTypeCharMatcher) throws ParseException {
        int argStart = startParams + 1;
        int argEnd = argStart; // this inital value is important for empty params case
        int prevNonCanonicalIndex = 0;

        final int sigEnd = signature.length();

        LOOP:
        while (argStart < sigEnd) {
            char c = signature.charAt(argStart);
            switch (c) {
            case ')':
                if (tupleTypes.size() > 0) {
                    argEnd = argStart - 1;
                }
                break LOOP;
            case ',':
                if (signature.charAt(argStart - 1) == ')') {
                    break LOOP;
                }
                throw new EmptyParameterException("empty parameter @ " + tupleTypes.size(), argStart);
            case '(': { // tuple element
                try {
                    ArrayList<Type> innerTupleTypes = new ArrayList<>();
                    Pair<Integer, Integer> results = parseTuple(signature, argStart, canonicalOut, innerTupleTypes, illegalTypeCharMatcher);

                    tupleTypes.add(TupleType.create(innerTupleTypes.toArray(Function.EMPTY_TYPE_ARRAY)));

                    argEnd = results.getLeft() + 1;
                    prevNonCanonicalIndex = results.getRight();

                } catch (EmptyParameterException epe) {
                    throw (EmptyParameterException) new EmptyParameterException(epe.getMessage() + " @ " + tupleTypes.size(), epe.getErrorOffset()).initCause(epe);
                }
                if (argEnd >= sigEnd || signature.charAt(argEnd) != ',') {
                    break LOOP;
                }
                argStart = argEnd + 1;
                break;
            }
            default: { // non-tuple element
                argEnd = nextParamTerminator(signature, argStart + 1);
                if (argEnd == -1) {
                    throw new NonTerminationException("non-terminating tuple", startParams);
                }
                checkParamChars(illegalTypeCharMatcher, signature, argStart, argEnd);
                String typeString = signature.substring(argStart, argEnd);
                String canonicalized = canonicalize(signature, typeString, argStart, argEnd);
                if (canonicalized != null) {
                    typeString = canonicalized;
                    canonicalOut.append(signature, prevNonCanonicalIndex, argStart).append(typeString);
                    prevNonCanonicalIndex = argEnd;
                }
                tupleTypes.add(Type.create(typeString));
                argStart = argEnd + 1;
            }
            }
        }

        return new ImmutablePair<>(argEnd, prevNonCanonicalIndex);
    }

    static int nextParamTerminator(String signature, int i) {
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

    public static ByteBuffer encodeFunctionCall(String signature, Object... arguments) throws ParseException {
        return Encoder.encodeFunctionCall(new Function(signature), arguments);
    }
}
