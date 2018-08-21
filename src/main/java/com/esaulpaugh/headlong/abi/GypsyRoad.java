package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.spongycastle.util.encoders.Hex;
import sun.nio.cs.US_ASCII;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

// TODO encode and decode
// TODO optimize -- maybe write all zeroes first then fill in params
public class GypsyRoad {

    private static final int _24 = 24;
    private static final int _32 = 32;

//    private static final Type TYPE_FLOAT = float.class;
//    private static final Type TYPE_INT_ARRAY = int[].class;
//    private static final Type TYPE_BYTE_ARRAY = byte[].class;
//
    private static final byte[] PADDING_192_BITS = new byte[_24];

    private static final Charset ASCII = US_ASCII.INSTANCE;

//    private static final Pattern PRINTABLE = Pattern.compile("[\\p{Print}].*");
//    private static final Pattern NON_PRINTABLE = Pattern.compile("[^\\p{Print}].*");
//
//    private static final Pattern CTRL = Pattern.compile("[\\p{Cntrl}].*");
//    private static final Pattern NON_CTRL = Pattern.compile("[^\\p{Cntrl}].*");

    private static final String REGEX_NON_ASCII_CHAR = "[^\\p{ASCII}]{1,}";
    private static final Pattern NON_ASCII_CHAR = Pattern.compile(REGEX_NON_ASCII_CHAR);
    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile(REGEX_NON_ASCII_CHAR);


    private static final String REGEX_NON_TYPE_CHAR = "[^a-z0-9\\[\\](),]{1,}";
    private static final Pattern NON_TYPE_CHAR = Pattern.compile(REGEX_NON_TYPE_CHAR);
    private static final Pattern HAS_NON_TYPE_CHARS = Pattern.compile(REGEX_NON_TYPE_CHAR);

//    public static void parseFunctionSignature(String signature, List<Type> types, StringBuilder canonicalSig) {
//
//        System.out.println("signature: " + signature);
//
//        String[] tokens = signature.trim().split(REGEX_NON_TYPE); // "[^\\p{Print}]|\\s|\\(|\\)|," // \p{Cntrl} \p{Print}  \p{C}\p{Z} \u001C|\u001D|\u001E|\u001F \u001C|\u001D|\u001E|\u001F
//
//        for (String s : tokens) {
//            System.out.println(StringEscapeUtils.escapeJava(s));
//        }
//
////        String[] tokens = signature.trim().split("(?!\\p{Print})|\\s|,|\\(|\\)|"); // \p{Cntrl} \p{Print}  \p{C}\p{Z} \u001C|\u001D|\u001E|\u001F \u001C|\u001D|\u001E|\u001F
//
//        canonicalSig.append(tokens[0]).append('('); // tokens[1].equals("(")
//
////        int i = tokens.length - 1;
////        String token;
////        do {
////            token = tokens[i--];
////        } while (!token.equals(")") && i >= 2);
//
//        final int firstTypeIndex = 2;
//        final int lastTypeIndex = tokens.length - 2; // ignore trailing ")"
//        for (int i = firstTypeIndex; i <= lastTypeIndex; i++) {
//            String typeString = tokens[i];
//            if(!typeString.isEmpty()) {
//                if (typeString.endsWith("int")) { // canonicalize
//                    typeString = typeString + "256";
//                }
//                canonicalSig.append(typeString).append(",");
//                types.add(new Type(typeString));
//            }
//        }
//        if(lastTypeIndex > firstTypeIndex) {
//            final int builderLen = canonicalSig.length();
//            canonicalSig.replace(builderLen - 1, builderLen, ")");
//        } else {
//            canonicalSig.append(')');
//        }
//
//        System.out.println("canonical: " + canonicalSig.toString());
//    }

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

    private static void throwIllegalCharException(boolean forNonTypeChar, String signature, int start) throws ParseException {
        char c = signature.charAt(start);
        throw new ParseException(
                "non-" + (forNonTypeChar ? "type" : "ascii") + " character at index " + start
                        + ": \'" + c + "\', " + escapeChar(c), start);
    }

    private static void checkNameChars(String signature, int startParams) throws ParseException {
        Matcher illegalChars = HAS_NON_ASCII_CHARS.matcher(signature).region(0, startParams);
        if(illegalChars.find()) {
            throwIllegalCharException(false, signature, illegalChars.start());
        }
    }

    private static void checkParamChars(String signature, int argStart, int argEnd) throws ParseException {
        Matcher illegalChars = HAS_NON_TYPE_CHARS.matcher(signature).region(argStart, argEnd);
        if (illegalChars.find()) {
            throwIllegalCharException(true, signature, illegalChars.start());
        }
    }

//    private static void checkStartAndEndIndices(String signature, int startParams, int endParams) throws ParseException {
//        if(startParams < 0) {
//            throw new ParseException("params start not found", 0);
//        }
//        if(endParams != signature.length() - 1) {
//            throw new ParseException("illegal signature termination: " + StringEscapeUtils.escapeJava(signature.substring(Math.max(0, endParams))), endParams + 1);
//        }
//    }

    public static void parseFunctionSignature(final String signature, final List<Type> types, final StringBuilder canonicalSig) throws ParseException {
        System.out.println("signature: " + escapeJava(signature));

        final int startParams = signature.indexOf('(');
//        final int endParams = signature.lastIndexOf(')');

//        final int endParams = signature.length() - 1;

//        checkStartAndEndIndices(signature, startParams, endParams);

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);
//        checkParamsChars(signature, startParams, end - 1); // endParams

//        Matcher illegalChars = HAS_NON_TYPE_CHARS.matcher(signature);

        int argStart = startParams + 1;
        int argEnd = -1; // Integer.MIN_VALUE; // = Integer.MIN_VALUE; // do not default to -1
        int prevNonCanonicalIndex = 0;


        final int sigEnd = signature.length(); //  - 1;

//        int nextEnd;

//        final int allParamsEnd = signature.lastIndexOf(')');
        while(argStart < sigEnd) { // argStart < endParams
            if(signature.charAt(argStart) == '(') {
                try {
                    argEnd = parseTuple(signature, argStart/*, endParams*/);
                } catch (ParseException pe) {
                    throw new ParseException(pe.getMessage() + " @ " + types.size(), pe.getErrorOffset()); //  + " (param " + types.size() + ")"
                }
//                if(signature.indexOf(')', argEnd) == -1) {
//                    argEnd = -1;
//                    break;
//                }
                if(argEnd == sigEnd || signature.charAt(argEnd) != ',') {
                    break;
                }
                argStart = argEnd + 1;
            } else {
//                System.out.println(signature.charAt(argStart));
//                int nextEnd = nextParamTerminator(signature, argStart);
                argEnd = nextParamTerminator(signature, argStart);

                if(argEnd == -1) {
//                    argEnd = -1;
                    break;
                }
                if(argEnd == argStart) {

                    if(types.size() == 0 && signature.charAt(argStart) == ')') {
//                        argEnd = argStart;
                        break;
                    }

//                    if(signature.charAt(argStart) == ',') {
                        throw new ParseException("empty parameter @ " + types.size(), argStart); // prevNonCanonicalIndex = paramEnd + 1;
//                    }

                }

//                argEnd = nextEnd;

//                argEnd = signature.indexOf(',', argStart);
//                if(argEnd == -1) {
//                    argEnd = signature.indexOf(')', argStart);
//                    if(argEnd == -1) {
//                        break;
////                        throw new ParseException("non-terminating signature", signature.length());
//                    }
//                }
//                if(argEnd == argStart) {
//                    throw new ParseException("empty parameter @ " + types.size(), argStart); // prevNonCanonicalIndex = paramEnd + 1;
//                }
                checkParamChars(signature, argStart, argEnd);
                String typeString = signature.substring(argStart, argEnd);
                if (typeString.endsWith("int")) { // canonicalize
                    typeString = typeString + "256";
                    canonicalSig.append(signature, prevNonCanonicalIndex, argEnd).append("256");
                    prevNonCanonicalIndex = argEnd;
                }
                types.add(new Type(typeString));
                argStart = argEnd + 1;
            }
//            System.out.println("argEnd = " + argEnd);
        }

//        System.out.println("final argEnd = " + argEnd + ", final argStart = " + argStart);

        int terminator = signature.indexOf(')', argEnd); // lastIndexOf(')');
        // argEnd == -1 ||
        if(terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }

//        if(argEnd == Integer.MIN_VALUE) {
//            argEnd = argStart;
//        }

//        if() {
//            throw new ParseException("non-terminating signature", signature.length());
//        }
//
        if(argEnd != terminator || terminator != sigEnd - 1) {
            throw new ParseException(
                    "illegal signature termination: " + escapeJava(signature.substring(Math.max(0, argEnd))),
                    argEnd
            );
        }

//        if(paramEnd == endParams - 1) {
//            throw new ParseException("empty parameter @ " + types.size(), paramEnd - 1);
//        }

//        final int endParams = end - 1;
//        if(signature.charAt(endParams) != ')') {
//            throw new ParseException("illegal signature termination: " + StringEscapeUtils.escapeJava(signature.substring(Math.max(0, endParams))), endParams + 1);
//        }

        canonicalSig.append(signature, prevNonCanonicalIndex, sigEnd);

        System.out.println("canonical: " + canonicalSig.toString());
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

    private static int parseTuple(String signature, int tupleStart/*, int endParams*/) throws ParseException {
        int idx = tupleStart;
        int tupleDepth = 0;
        int openTuple, closeTuple;
        do {
            openTuple = signature.indexOf('(', idx);
            closeTuple = signature.indexOf(')', idx);

            if(closeTuple < 0) { //  || closeTuple >= endParams
                throw new ParseException("non-terminating tuple", tupleStart);
            }

            if(openTuple == -1 || closeTuple < openTuple) {
                tupleDepth--;
                idx = closeTuple + 1;
            } else {
                tupleDepth++;
                idx = openTuple + 1;
            }
        } while(tupleDepth > 0);

        checkParamChars(signature, tupleStart, idx);
        String tuple = signature.substring(tupleStart, idx);
        System.out.println("tuple: " + tuple); // uncanonicalized

        return idx;
    }

    public static void validateParams(Object[] values, List<Type> types) {
        final int size = types.size();
        int i = 0;
        try {
            for ( ; i < size; i++) {
                types.get(i).validate(values[i]);
            }
            // encode
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid param @ " + i + ": " + e.getMessage(), e);
        }
    }

    public static void encodeParams(ByteBuffer outBuffer, Object[] values, List<Type> types) {
        for (int i = 0; i < values.length; i++) {

            types.get(i).encode(values[i], outBuffer);

//            Object value = values[i];
//            if(value instanceof Object[]) {
//                Object[] arr = (Object[]) value;
//                for (int j = 0; j < arr.length; j++) {
//                    types.get(i).encode(values[i], outBuffer);
//                }
//            } else {

//            }
        }
    }

//    private static String canonicalize() {
//
//    }

    public static ByteBuffer encodeFunctionCall(String signature, Object... params) throws ParseException {

        StringBuilder canonicalSigBuilder = new StringBuilder();
        List<Type> types = new ArrayList<>();
        parseFunctionSignature(signature, types, canonicalSigBuilder);
        signature = canonicalSigBuilder.toString();

        if(params.length != types.size()) {
            throw new IllegalArgumentException("params.length <> types.size(): " + params.length + " != " + types.size());
        }

        validateParams(params, types);

        int paramsByteLen = 0;
        final int size = types.size();
        for (int i = 0; i < size; i++) {
            Type t = types.get(i);
            if(t.byteLen != null) {
//                if(t.byteLen != 32) {
//                int roundedUp = t.byteLen + (32 - (t.byteLen % 32));
//                System.out.println(roundedUp);
                    paramsByteLen += t.byteLen;
//                } else {
//                paramsByteLen += t.byteLen;
            } else {
//                paramsByteLen += t.calcDynamicByteLen(params[i]);

                paramsByteLen = 1000;

//                throw new UnsupportedOperationException("dynamic types not yet supported");
            }

        }

        System.out.println("allocating " + (4 + paramsByteLen));
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[4 + paramsByteLen]); // ByteOrder.BIG_ENDIAN by default
        Keccak keccak = new Keccak(256);
        keccak.update(signature.getBytes(ASCII));
        keccak.digest(outBuffer, 4);

        encodeParams(outBuffer, params, types);

        return outBuffer;
    }

    public static void main(String[] args0) throws ClassNotFoundException, ParseException {

        //        int countP = 0, countC = 0, countN = 0;
//        int j = Short.MIN_VALUE;
//        for ( ; j <= Short.MAX_VALUE; j++) {
//            String s = new String(new char[] { (char) j });
//            boolean p = PRINTABLE.matcher(s).matches();
//            boolean c = CTRL.matcher(s).matches();
//            boolean np = NON_PRINTABLE.matcher(s).matches();
//            boolean nc = NON_CTRL.matcher(s).matches();
////            System.out.println("printable:" + p);
////            if(p != nc || np != c) {
////                countN++;
////                System.out.println(i + ": " + Hex.toHexString(new byte[] { (byte) ((char) j) }) + ", printable:" + p + " != " + nc + " || " + np + " != " + c + " == " + (char) j);
////            }
//            if(p) {
//                countP++;
//            }
//            if(c) {
//                countC++;
//            }
//            countN++;
//        }
//
//        System.out.println("countP: " + countP + ", countC = " + countC + ", countN = " + countN + ", " + j);

//        Class.forName(int.class.getName());
//        Class.forName(float.class.getName());
//        Class.forName(boolean.class.getName());

//        System.out.println(byte.class.getName());
//        System.out.println(byte[].class.getName());
//        System.out.println(byte[][].class.getName());
//        System.out.println(short.class.getName());
//        System.out.println(short[].class.getName());
//        System.out.println(short[][].class.getName());

        String s;
        Class clazz;

//        s = boolean.class.getName();
//        System.out.println(s);
//        clazz = Class.forName(s);
//        System.out.println(clazz.getName() + " == " + s);
//
//        s = BigInteger.class.getName();
//        System.out.println(s);
//        clazz = Class.forName(s);
//        System.out.println(clazz.getName() + " == " + s);
//
//        s = BigInteger[][][].class.getName();
//        System.out.println(s);
//        clazz = Class.forName(s);
//        System.out.println(clazz.getName() + " == " + s);

//        callFunction("zzz(uint32[][3][])", (Object) new int[][][] { new int[][] { new int[0], new int[0], new int[0] } });

        // TODO VALIDATE NO ARITHMETIC OVERFLOW e.g. Integer.MAX_VALUE --> int24
        // TODO CALC DYNAMIC LENGTH
        // TODO TEST SIGNED/UNSIGNED
        // TODO TUPLES, FIXEDMxN
//        ByteBuffer abi0 = encodeFunctionCall("yee(bytes[],bytes,bytes7,string,uint8[],uint16,uint24[][1],uint24[2][],uint32,uint64,int128,int256[1][2][3])",
//                new byte[][] { new byte[9] },
//                new byte[7],
//                new byte[7],
//                "",
//                new byte[2],
//                (short) 0,
//                new int[][] { new int[1] },
//                new int[][] { new int[2], new int[2] },
//                0,
//                0L,
//                BigInteger.TEN,
//                new BigInteger[][][] {
//                        new BigInteger[][] {
//                                new BigInteger[] { BigInteger.ONE },
//                                new BigInteger[] { BigInteger.ONE }
//                        },
//                        new BigInteger[][] {
//                                new BigInteger[] { BigInteger.ONE },
//                                new BigInteger[] { BigInteger.ONE }
//                        },
//                        new BigInteger[][] {
//                                new BigInteger[] { BigInteger.ONE },
//                                new BigInteger[] { BigInteger.ONE }
//                        }
//                }
//                );

//        ByteBuffer abi1 = encodeFunctionCall("yeehaw(bool[1],int24[3],int56[2],bool,int8,int16,int24,int32,int56,int64)",
//                new boolean[] { true },
//                new int[] { 1, -10, Integer.MAX_VALUE / 128 },
//                new long[] { Integer.MAX_VALUE, Long.MAX_VALUE >>> 7 },
//                true,
//                (byte) 4,
//                (short) 5,
//                Integer.MAX_VALUE >>> 7,
//                Integer.MAX_VALUE,
//                (long) Integer.MAX_VALUE << 7,
//                Long.MAX_VALUE
//        );
//        System.out.println(Hex.toHexString(abi1.array()));

//        for (int i = 30; i >= -456; i--) {
//            BizarroIntegers.bitLen(i);
//        }
//
//        ByteBuffer abi2 = encodeFunctionCall("yeehaw(int8,int8,int16,int16,int24,int24,int32,int32,int40,int40,int64,int64)",
//                (byte) -1,
//                (byte) 0,
//                (short) -1,
//                (short) 0,
//                -1,
//                0,
////                -(Short.MAX_VALUE * 2 + 2 * 256) - 16711170,
////                -((Short.MAX_VALUE * 2 + 2) * 256),
////                -4,
////                -16711169,
////                Integer.MIN_VALUE,
//                //                -(int) (Math.pow(2, 24)),
////                16777216,
//                -1,
//                0,
//                -1L,
//                0L,
//                -1L,
//                0L
//        );
//

//        ByteBuffer abi00 = encodeFunctionCall("yeehaw(bytes1,bytes2[3])", // ,bytes2[]
//                new byte[] { 127 },
//                new byte[][] { new byte[] { 9, 8 }, new byte[] { 7, 6 }, new byte[] { 5, 4 } }
//        );
//
//        ByteBuffer abi0 = encodeFunctionCall("yeehaw(bytes,string,bytes[],bytes1[])",
//                new byte[0],
//                "",
//                new byte[0][],
//                new byte[][] {  }
//        );
////        System.out.println(Hex.toHexString(abi3.array()));
//
//        ByteBuffer abi = encodeFunctionCall("yeehaw(int8[3][5][2])",
//                (Object) new byte[][][] {
//                        new byte[][] { new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA }, new byte[3], new byte[3], new byte[3], new byte[3] },
//                        new byte[][] { new byte[3], new byte[3], new byte[3], new byte[3], new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF } }
//                }
//                );
//
//        ByteBuffer abi2 = encodeFunctionCall("yeehaw(int16[3][5][2])",
//                (Object) new short[][][] {
//                        new short[][] { new short[3], new short[3], new short[3], new short[3], new short[3] },
//                        new short[][] { new short[3], new short[3], new short[3], new short[3], new short[] { -1, -2, -3 } }
//                }
//        );
//
//        ByteBuffer abi3 = encodeFunctionCall("yeehaw(bytes2[5][2])",
//                (Object) new byte[][][] {
//                        new byte[][] { new byte[2], new byte[2], new byte[2], new byte[2], new byte[2] },
//                        new byte[][] { new byte[2], new byte[2], new byte[2], new byte[2], new byte[] { -9, -8 } }
//                }
//        );
//
//        ByteBuffer abi4 = encodeFunctionCall("yeehaw(fixed16x2,ufixed16x2,int16,uint16,int40,uint40,int256,uint256)",
//                BigDecimal.valueOf(250.54),
//                BigDecimal.valueOf(250.54),
//                (short) 25_054,
//                (short) 25_054,
//                25_054L,
//                25_054L,
//                BigInteger.valueOf(25_054),
//                BigInteger.valueOf(25_054)
//        );
//
//        ByteBuffer abi5 = encodeFunctionCall("yeehaw(string)",
//                "ahoy123_ahoy123_ahoy123_ahoy123_!" // 4321yoe_4321yoe_4321yoe_4321yoe_
//        );
//
//        ByteBuffer abi6 = encodeFunctionCall("yeehaw(bytes[2])",
//                (Object) new byte[][] { new byte[32], new byte[32] }
//        );
//
//        ByteBuffer abi7 = encodeFunctionCall("test(bytes,string,int8,int16,int24,int40,int200,bytes[0])",
//                new byte[0],
//                "",
//                (byte) 0,
//                (short) 'o',
//                0,
//                0L,
//                BigInteger.ZERO,
//                new byte[][] {  }
//        );

//        ByteBuffer abi_ = encodeFunctionCall("()");
//
//        ByteBuffer abi__ = encodeFunctionCall("____(int)", BigInteger.TEN);
//
//        ByteBuffer abi___ = encodeFunctionCall(")()");
//
//
//        ByteBuffer abi65 = encodeFunctionCall(" -----_;\u007F-a ;(int)", BigInteger.TEN);
//
//        ByteBuffer abi8 = encodeFunctionCall(" noncanon(int)",
////                        '\u0009' +
////                        '\r' +
////                        '\u000B' +
////                        '\u000C' +
////                        '\n' +
////                        '\u001C' +
////                        '\u001D' +
////                        '\u001E' +
////                        '\u001F' +
////                        " \t ,\f,",
//                BigInteger.ZERO
////                BigInteger.ZERO,
////                BigInteger.ZERO,
////                BigInteger.ONE,
////                BigInteger.ONE,
////                BigInteger.ONE
//        );

        ByteBuffer abi = null;

        try {
            abi = encodeFunctionCall("");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall(")");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(int");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(,int");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(,int)");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(int,)");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a()%");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a()");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(())");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(()))");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(()]int)");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a(()");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = encodeFunctionCall("a((),");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }

//        abi = encodeFunctionCall("a((()))");

        System.out.println(Hex.toHexString(abi.array()));


        if(true) return;

//        Type t = new Type("uint[][12][]");
//        System.out.println(t);

//        if(true) return;

//        System.out.println(int[][].class);

        Class c0 = int[].class;
        System.out.println(c0.getName());

        Class c = Class.forName(c0.getName());
        System.out.println(c.getName());

        System.out.println(Class.forName("[[I") == int[][].class);


        final int len = 4 + (9 * 32);

        Keccak keccak = new Keccak(256);

        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[len]);
        keccak.update("baz(uint32,bool)".getBytes(ASCII));
//        keccak.digest(out, 0, 4);
        keccak.digest(outBuffer, 4);


        Encoder.insertInt(69L, outBuffer);
        Encoder.insertBool(true, outBuffer);

        System.out.println(Strings.encode(outBuffer.array(), HEX));

        outBuffer.position(0);

        keccak.update("bar(bytes3[2])".getBytes(ASCII));
        keccak.digest(outBuffer, 4);

        Encoder.insertBytesArray(new byte[][] { "abc".getBytes(ASCII), "def".getBytes(ASCII) }, outBuffer);

        System.out.println(Strings.encode(outBuffer.array(), HEX));

        outBuffer.position(0);

        keccak.update("sam(bytes,bool,uint256[])".getBytes(ASCII));
        keccak.digest(outBuffer, 4);

        Encoder.insertInt(0x60, outBuffer);
        Encoder.insertBool(true, outBuffer);
        Encoder.insertInt(0xa0, outBuffer);
        Encoder.insertInt(0x04, outBuffer);
        Encoder.insertBytes("dave".getBytes(ASCII), outBuffer);
        Encoder.insertInt(0x03, outBuffer);
        Encoder.insertInts(new int[] { 1, 2, 3 }, outBuffer);

        System.out.println(Strings.encode(outBuffer.array(), HEX));

    }

//    private static void insertBytesArray(byte[][] src, ByteBuffer dest) {
//        for(byte[] e : src) {
//            insertBytes(e, dest);
//        }
//    }
//
//    private static void insertBytes(byte[] src,/* int offset, int len,*/ ByteBuffer dest) {
//        dest.put(src);
//        final int n = _32 - src.length;
//        for (int i = 0; i < n; i++) {
//            dest.put((byte) 0);
//        }
//    }
//
//    private static void insertBool(boolean bool, ByteBuffer dest) {
//        insertInt(bool ? 1L : 0L, dest);
//    }
//
//    public static void insertInts(int[] ints, ByteBuffer dest) {
//        for (int e : ints) {
//            insertInt(e, dest);
//        }
//    }
//
//    private static void insertInt(long val, ByteBuffer dest) {
////        final int pos = dest.position();
//        dest.put(PADDING_192_BITS);
//        dest.putLong(val);
////        putLongBigEndian(val, dest, pos + NUM_PADDING_BYTES);
////        return pos + INT_PARAM_LENGTH_BYTES;
//    }
//
//    public static void putLongBigEndian(long val, ByteBuffer dest) {
//
//    }
//
//    public static void putLongBigEndian(long val, byte[] o, int i) {
//        o[i] = (byte) (val >>> 56);
//        o[i+1] = (byte) (val >>> 48);
//        o[i+2] = (byte) (val >>> 40);
//        o[i+3] = (byte) (val >>> 32);
//        o[i+4] = (byte) (val >>> 24);
//        o[i+5] = (byte) (val >>> 16);
//        o[i+6] = (byte) (val >>> 8);
//        o[i+7] = (byte) val;
//    }

}
