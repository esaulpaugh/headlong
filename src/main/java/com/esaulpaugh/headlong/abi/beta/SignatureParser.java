package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.type.TupleType;
import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SignatureParser {

    private static final String REGEX_NON_ASCII_CHAR = "[^\\p{ASCII}]{1,}";
    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile(REGEX_NON_ASCII_CHAR);

    private static final String REGEX_NON_TYPE_CHAR = "[^a-z0-9\\[\\](),]{1,}";
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile(REGEX_NON_TYPE_CHAR);

    static List<StackableType> parseFunctionSignature(final String signature, final StringBuilder canonicalOut) throws ParseException {
//        System.out.println("signature: " + escapeJava(signature));

//        if(canonicalOut.length() > 0) {
//            throw new IllegalArgumentException("canonicalOut must be empty");
//        }
//        if(!typesOut.isEmpty()) {
//            throw new IllegalArgumentException("typesOut must be empty");
//        }

        List<StackableType> typesOut = new ArrayList<>();

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        final Matcher illegalTypeCharMatcher = HAS_NON_TYPE_CHARS.matcher(signature);

        Pair<Integer, Integer> results;
        try {
            results = parseTuple(signature, startParams, canonicalOut, typesOut, illegalTypeCharMatcher);
        } catch (NonTerminationException nte) {
            throw (ParseException) new ParseException("non-terminating signature", nte.getErrorOffset()).initCause(nte);
        } catch (EmptyParameterException epe) {
            throw (ParseException) new ParseException(epe.getMessage(), epe.getErrorOffset()).initCause(epe);
        }

        final int argEnd = results.first;
        final int sigEnd = signature.length();

        int terminator = signature.indexOf(')', argEnd);
        if (terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }
        if (argEnd != terminator || terminator != sigEnd - 1) {
            throw new ParseException("illegal signature termination: " + signature.substring(Math.max(0, argEnd)), argEnd);
        }

        final int prevNonCanonicalIndex = results.second;

        canonicalOut.append(signature, prevNonCanonicalIndex, sigEnd);

        System.out.println("canonical: " + canonicalOut.toString());

        return typesOut;

//        return prevNonCanonicalIndex != 0;
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
//                Tuple.create(null, tupleTypes.get(tupleTypes.size() - 1));
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
                    ArrayList<StackableType> innerTupleTypes = new ArrayList<>();
                    Pair<Integer, Integer> results = parseTuple(signature, argStart, canonicalOut, innerTupleTypes, illegalTypeCharMatcher);

                    // TODO test DynamicArrayType (e.g. [4] w/ dynamic element) enforces specified len
//                    new DynamicArrayType(canonicalAbiType, className, typeStack.peek());
//                    new StaticArrayType(canonicalAbiType, className, typeStack.peek(), length);


                    // TODO NON-CANONICAL, DON'T SUBSTRING
                    // signature.substring(argEnd, argEnd)
                    StackableType[] members = innerTupleTypes.toArray(StackableType.EMPTY_TYPE_ARRAY);

                    TupleType tupleType = TupleType.create(null, members);
                    StackableType typleArray = null;

                    int k = results.first + 1;
                    int nextTerminator = -1;
                    if(k < sigEnd && signature.charAt(k) == '[') {
                        nextTerminator = nextParamTerminator(signature, k);
                        if(nextTerminator > k) {
                            typleArray = Typing.createForTuple(signature.substring(argStart, nextTerminator), tupleType);
                        }
                    }

                    if(typleArray != null) {
                        tupleTypes.add(typleArray);
                        argEnd = nextTerminator;
                    } else {
                        tupleTypes.add(tupleType);
                        argEnd = results.first + 1;
                    }

                    prevNonCanonicalIndex = results.second;

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
                tupleTypes.add(Typing.create(typeString));
                argStart = argEnd + 1;
            }
            }
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
        Matcher illegalChars = matcher.region(argStart, argEnd);
        if (illegalChars.find()) {
            throw newIllegalCharacterException(true, signature, illegalChars.start());
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
