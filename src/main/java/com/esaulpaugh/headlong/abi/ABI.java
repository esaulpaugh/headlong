package com.esaulpaugh.headlong.abi;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import sun.nio.cs.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.abi.Function.FUNCTION_ID_LEN;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

// TODO encode and decode
// TODO optimize -- maybe write all zeroes first then fill in params
public class ABI {

    private static final Charset ASCII = US_ASCII.INSTANCE;

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

    private static String canonicalize(String signature, String typeString, int argStart, final int argEnd /* StringBuilder canonicalSig, int prevNonCanonicalIndex */) {
        int splitIndex;
        String piece;

        if (typeString.endsWith("int")) {
            splitIndex = argEnd;
            piece = "256";
//            canonicalSig.append(signature, prevNonCanonicalIndex, argEnd).append("256");
        } else if(typeString.endsWith("fixed")) {
            splitIndex = argEnd;
            piece = "128x18";
//            canonicalSig.append(signature, prevNonCanonicalIndex, argEnd).append("128x18");
        } else if(typeString.contains("int[")) {
            splitIndex = signature.indexOf("int", argStart) + 3;
            piece = "256";
//            String a = signature.substring(argStart, idx);
//            String b = signature.substring(idx, argEnd);
//            canonicalSig.append(signature, prevNonCanonicalIndex, idx).append("256").append(signature, idx, argEnd);
        } else if(typeString.contains("fixed[")) {
            splitIndex = signature.indexOf("fixed", argStart) + 5;
            piece = "128x18";
//            String a = signature.substring(argStart, idx);
//            String b = signature.substring(idx, argEnd);
//            canonicalSig.append(signature, prevNonCanonicalIndex, idx).append("128x18").append(signature, idx, argEnd);
        } else {
            return null;
//            splitIndex = argEnd;
//            piece = "";
        }

        return new StringBuilder().append(signature, argStart, splitIndex).append(piece).append(signature, splitIndex, argEnd).toString();

//        return argEnd;
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
                                                     final Matcher illegalTypeCharMatcher)
            throws ParseException {

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

//                    String typeString = parseTuple(illegalTypeCharMatcher, signature, argStart);
//                    argEnd = argStart + typeString.length();

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
//                    break LOOP;
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

//    private static String parseTuple(Matcher matcher, String signature, int tupleStart) throws ParseException {
//        int idx = tupleStart;
//        int tupleDepth = 0;
//        int openTuple, closeTuple;
//        do {
//            openTuple = signature.indexOf('(', idx);
//            closeTuple = signature.indexOf(')', idx);
//
//            if(closeTuple < 0) {
//                throw new ParseException("non-terminating tuple", tupleStart);
//            }
//
//            if(openTuple == -1 || closeTuple < openTuple) {
//                tupleDepth--;
//                idx = closeTuple + 1;
//            } else {
//                tupleDepth++;
//                idx = openTuple + 1;
//            }
//        } while(tupleDepth > 0);
//
//        checkParamChars(matcher, signature, tupleStart, idx);
//        String tuple = signature.substring(tupleStart, idx);
//        System.out.println("tuple: " + tuple); // uncanonicalized
//
//        return tuple;
//
////        return idx;
//    }

    public static void checkTypes(Type[] types, Object[] values) {
        final int n = types.length;
        int i = 0;
        try {
            for ( ; i < n; i++) {
                types[i].validate(values[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid param @ " + i + ": " + e.getMessage(), e);
        }
    }

    public static void encodeParamHeads(ByteBuffer outBuffer, List<Type> types, List<Object> values, int[] headLengths) {
//        final int size = types.size();
//        for (int i = 0; i < size; i++) {
//            types[i].encodeHeads(values[i], outBuffer);
//        }

        int sum = 0;
        for (int i : headLengths) {
            sum += i;
        }

        int i = 0;
        Iterator<Type> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            Type type = ti.next();
//            sum -= headLengths[i++];
            type.encodeHead(vi.next(), outBuffer, sum, type.dynamic);
            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    public static void encodeParamTails(ByteBuffer outBuffer, List<Type> types, List<Object> values) {
//        final int size = types.size();
//        for (int i = 0; i < size; i++) {
//            types[i].encodeHeads(values[i], outBuffer);
//        }
        Iterator<Type> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            Type type = ti.next();
            type.encodeTail(vi.next(), outBuffer);
            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    public static ByteBuffer encodeFunctionCall(String signature, Object... params) throws ParseException {
        return encodeFunctionCall(new Function(signature), params);
    }

    public static ByteBuffer encodeFunctionCall(Function function, Object... params) {

//        StringBuilder canonicalSigBuilder = new StringBuilder();
//        List<Type> types = new ArrayList<>();
//        boolean wasChanged = parseFunctionSignature(signature, canonicalSigBuilder, types);
//        signature = canonicalSigBuilder.toString();

//        Function f = new Function(signature);

        System.out.println("requiredCanonicalization = " + function.requiredCanonicalization);

        final Type[] types = function.types;
        final int expectedNumParams = types.length;

        if(params.length != expectedNumParams) {
            throw new IllegalArgumentException("params.length <> types.size(): " + params.length + " != " + types.length);
        }

        checkTypes(types, params);

        // head(X(i)) = enc(len(head(X(1)) ... head(X(k)) tail(X(1)) ... tail(X(i-1)) ))

        int[] headLengths = new int[types.length];
        int dynamicHeadsByteLen = 0;
//        boolean dynamic = false;
        int paramsByteLen = 0;
        for (int i = 0; i < expectedNumParams; i++) {
            Type t = types[i];
//            dynamic |= t.dynamic;

            int byteLen = t.getDataByteLen(params[i]);
            System.out.println(params[i] + " --> " + byteLen);
            paramsByteLen += byteLen;

            if(t.dynamic) {
                headLengths[i] = 32;
                dynamicHeadsByteLen += 32;
                System.out.println(params[i] + " dynamic head " + 32);
            } else {
                headLengths[i] = byteLen;
            }

//            Integer byteLen = t.getHeadLen(params[i]);
//            if(byteLen != null) {
//                paramsByteLen += byteLen;
//            } else {
////                paramsByteLen = 1000;
////                throw new UnsupportedOperationException("dynamic types not yet supported");
//            }
        }

        System.out.println("**************** " + dynamicHeadsByteLen);

        System.out.println("**************** " + paramsByteLen);

        final int allocation = FUNCTION_ID_LEN + dynamicHeadsByteLen + paramsByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
//        Keccak keccak = new Keccak(256);
//        keccak.update(signature.getBytes(ASCII));
//        keccak.digest(outBuffer, 4);

        outBuffer.put(function.selector);

        List<Type> typeList = new LinkedList<>(Arrays.asList(types));
        List<Object> paramList = new LinkedList<>(Arrays.asList(params));

        // TODO ENCODE HEADS
        encodeParamHeads(outBuffer, typeList, paramList, headLengths);
        // TODO THEN TAILS
        encodeParamTails(outBuffer, typeList, paramList);

        return outBuffer;
    }
}
