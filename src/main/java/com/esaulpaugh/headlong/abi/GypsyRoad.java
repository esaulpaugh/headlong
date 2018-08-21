package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.spongycastle.util.encoders.Hex;
import sun.nio.cs.US_ASCII;

import java.math.BigDecimal;
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

    public static void parseFunctionSignature(final String signature, final List<Type> types, final StringBuilder canonicalSig) throws ParseException {
        System.out.println("signature: " + escapeJava(signature));

        final int startParams = signature.indexOf('(');

        if(startParams < 0) {
            throw new ParseException("params start not found", 0);
        }

        checkNameChars(signature, startParams);

        int argStart = startParams + 1;
        int argEnd = argStart; // this inital value important for empty params case
        int prevNonCanonicalIndex = 0;

        final int sigEnd = signature.length();

        LOOP:
        while(argStart < sigEnd) {
            char c = signature.charAt(argStart);
            switch (c) {
            case ')':
                if(types.size() > 0) {
                    argEnd = argStart - 1;
                }
//                argEnd = types.size() == 0 ? argStart : argStart - 1;
                break LOOP;
            case ',':
                if(signature.charAt(argStart - 1) == ')') {
//                    argEnd = argStart - 1;
                    break LOOP;
                }
                throw new ParseException("empty parameter @ " + types.size(), argStart);
//                break LOOP;
            case '(': { // tuple
                try {
                    String typeString = parseTuple(signature, argStart);
                    types.add(new Type(typeString));
                    argEnd = argStart + typeString.length();
                } catch (ParseException pe) {
                    throw new ParseException(pe.getMessage() + " @ " + types.size(), pe.getErrorOffset());
                }
                if(argEnd == sigEnd || signature.charAt(argEnd) != ',') {
                    break LOOP;
                }
                argStart = argEnd + 1;
            }
            default: { // non-tuple
                argEnd = nextParamTerminator(signature, argStart + 1);
                if(argEnd == -1) {
                    break LOOP;
                }
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
            }
//            if(signature.charAt(argStart) == ')' || signature.charAt()) { //
//                break LOOP;
//            }
//            if(signature.charAt(argStart) == '(') {
//
//            } else {
//
//            }
        }

        int terminator = signature.indexOf(')', argEnd);
        if(argEnd == -1 || terminator == -1) {
            throw new ParseException("non-terminating signature", sigEnd);
        }

        if(argEnd != terminator || terminator != sigEnd - 1) {
            throw new ParseException(
                    "illegal signature termination: " + escapeJava(signature.substring(Math.max(0, argEnd))),
                    argEnd
            );
        }

        canonicalSig.append(signature, prevNonCanonicalIndex, sigEnd);

        System.out.println("canonical: " + canonicalSig.toString());
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

    private static String parseTuple(String signature, int tupleStart) throws ParseException {
        int idx = tupleStart;
        int tupleDepth = 0;
        int openTuple, closeTuple;
        do {
            openTuple = signature.indexOf('(', idx);
            closeTuple = signature.indexOf(')', idx);

            if(closeTuple < 0) {
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

        return tuple;

//        return idx;
    }

    public static void validateParams(Object[] values, List<Type> types) {
        final int size = types.size();
        int i = 0;
        try {
            for ( ; i < size; i++) {
                types.get(i).validate(values[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid param @ " + i + ": " + e.getMessage(), e);
        }
    }

    public static void encodeParams(ByteBuffer outBuffer, Object[] values, List<Type> types) {
        for (int i = 0; i < values.length; i++) {
            types.get(i).encode(values[i], outBuffer);
        }
    }

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
                paramsByteLen += t.byteLen;
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

//        try {
//            abi = encodeFunctionCall("");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = encodeFunctionCall("a");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = encodeFunctionCall(")");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = encodeFunctionCall("a(");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = encodeFunctionCall("a(int");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = encodeFunctionCall("a(,int");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
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
            abi = encodeFunctionCall("a(())", (Object) "");
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
            abi = encodeFunctionCall("a((");
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
//        try {
            BigDecimal a = BigDecimal.valueOf(96, 0);
            BigDecimal b = BigDecimal.valueOf((byte)96.0);
            BigDecimal c_ = BigDecimal.valueOf((short) 96.0);
            BigDecimal d = BigDecimal.valueOf((int) 96.0);
            BigDecimal e = BigDecimal.valueOf(96);
            System.out.println(e.scale());

//            System.out.println("bitlen " + a.unscaledValue().bitLength());

        abi = encodeFunctionCall("a(fixed16x0,fixed16x1,fixed16x2,fixed16x3,fixed16x4)", a, b, c_, d, e);
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }

        System.out.println(Hex.toHexString(abi.array()));


        if(true) return;

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
}
