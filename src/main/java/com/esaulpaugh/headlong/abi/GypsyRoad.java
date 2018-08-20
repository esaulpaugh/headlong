package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.spongycastle.util.encoders.Hex;
import sun.nio.cs.US_ASCII;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;

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

    public static List<Type> parseFunctionSignature(String signature) throws ParseException {
        ArrayList<Type> types = new ArrayList<>();
        final int endParams = signature.lastIndexOf(')');
        if(endParams < 0) {
            throw new ParseException("non-terminating function signature: " + signature, signature.length());
        }
        int paramStart = signature.indexOf('(') + 1;
        int paramEnd;
        while(paramStart < endParams) {
            paramEnd = signature.indexOf(',', paramStart);
            if(paramEnd == -1) {
                paramEnd = endParams;
            }
            types.add(new Type(signature.substring(paramStart, paramEnd)));
            paramStart = paramEnd + 1;
        }
        return types;
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
            throw new IllegalArgumentException("invalid param @ index " + i + ": " + e.getMessage(), e);
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

    public static ByteBuffer encodeFunctionCall(String signature, Object... params) throws ParseException {
        List<Type> types = parseFunctionSignature(signature);

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
                throw new UnsupportedOperationException("dynamic types not yet supported");
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
//        ByteBuffer abi3 = encodeFunctionCall("yeehaw(bytes1,bytes2[3])", // ,bytes2[]
//                new byte[] { 127 },
//                new byte[][] { new byte[] { 9, 8 }, new byte[] { 7, 6 }, new byte[] { 5, 4 } }
//        );

//        ByteBuffer abi3 = encodeFunctionCall("yeehaw(bytes,string,bytes[],bytes1[])",
//                new byte[0],
//                "",
//                new byte[0][],
//                new byte[][] {  }
//        );
//        System.out.println(Hex.toHexString(abi3.array()));

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

//        ByteBuffer abi5 = encodeFunctionCall("yeehaw(string)",
//                "ahoy123_ahoy123_ahoy123_ahoy123_!" // 4321yoe_4321yoe_4321yoe_4321yoe_
//        );

//        ByteBuffer abi6 = encodeFunctionCall("yeehaw(bytes[2])",
//                (Object) new byte[][] { new byte[32], new byte[32] }
//        );

        ByteBuffer abi7 = encodeFunctionCall("test(bytes,string,int8,int16,int24,int40,int200,bytes[0])",
                new byte[0],
                "",
                (byte) 0,
                (short) 'o',
                0,
                0L,
                BigInteger.ZERO,
                new byte[][][] {  }
        );

        System.out.println(Hex.toHexString(abi7.array()));


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
