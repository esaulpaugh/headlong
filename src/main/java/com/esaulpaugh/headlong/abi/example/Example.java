package com.esaulpaugh.headlong.abi.example;

import com.esaulpaugh.headlong.abi.ABI;
import com.esaulpaugh.headlong.abi.Tuple;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;

public class Example {

    public static void main(String[] args0) throws ParseException {

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

        ByteBuffer abi;

//        try {
//            abi = ABI.encodeFunctionCall("(())", new Tuple());
//            abi = ABI.encodeFunctionCall("((),())", new Tuple(), new Tuple());
//            abi = ABI.encodeFunctionCall("((int))", new Tuple(BigInteger.TEN));
//            abi = ABI.encodeFunctionCall("((int),(uint))", new Tuple(BigInteger.TEN), new Tuple(BigInteger.TEN));
//            System.out.println(Hex.toHexString(abi.array()));
//        } catch (Throwable t) {
////            t.printStackTrace();
//            System.out.println("\t\t" + t.getMessage());
//        }

//        abi = ABI.encodeFunctionCall("(bool,bool,bool[])",
//                true,
//                true,
//                new boolean[] { true, false, true }
//        );
//        abi = ABI.encodeFunctionCall("(uint200,uint200[],uint208,uint208[],uint216,uint216[],uint224,uint224[],uint232,uint232[],uint240,uint240[],uint248,uint248[],uint256,uint256[])",
//                BigInteger.valueOf(5L),
//                new BigInteger[] { new BigInteger("172345678901234567890123456789012345678901234567890123456789") },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { new BigInteger("172345678901234567890123456789012345678901234567890123456789000") },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { BigInteger.valueOf(65L) },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { BigInteger.valueOf(65L) },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { BigInteger.valueOf(65L) },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { BigInteger.valueOf(65L) },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { BigInteger.valueOf(65L) },
//                BigInteger.valueOf(5L),
//                new BigInteger[] { new BigInteger("172345678901234567890123456789012345678901234567890123456789000000000000000000") }
//        );
        byte[] function = new byte[24];
        function[0] = 126;
        function[23] = 127;
        byte[] bytes32 = new byte[32];
        bytes32[0] = 126;
        bytes32[31] = 127;

        // TODO ENCODE ARRAY LENGTH FOR DYNAMICS
        abi = ABI.encodeFunctionCall("(function,bytes32,bytes[])",
                function,
                bytes32,
                new byte[][] { new byte[] { 5, 6, 7 } }
        );

        System.out.println(Hex.toHexString(abi.array()));

        if(true) return;

        abi = ABI.encodeFunctionCall("(string,string,string[])",
                "",
                "y",
                new String[0]
        );


//        if(true) return;

        try {
            abi = ABI.encodeFunctionCall("");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
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
        try {
            abi = ABI.encodeFunctionCall("a(int");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a(,int");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
//        try {
//            abi = encodeFunctionCall("a(,int)");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
        try {
            abi = ABI.encodeFunctionCall("a(int,)");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a()%");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a()");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a(())", new Tuple());
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a(()))", Tuple.EMPTY);
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a(()]int)", Tuple.EMPTY, BigInteger.ZERO);
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a((");
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a(()", Tuple.EMPTY);
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
        try {
            abi = ABI.encodeFunctionCall("a((),", Tuple.EMPTY);
        } catch (Throwable t) {
            System.out.println("\t\t" + t.getMessage());
        }
////        try {
//            BigDecimal a = BigDecimal.valueOf(96, 0);
//            BigDecimal b = BigDecimal.valueOf((byte)96.0);
//            BigDecimal c_ = BigDecimal.valueOf((short) 96.0);
//            BigDecimal d = BigDecimal.valueOf((int) 96.0);
//            BigDecimal e = BigDecimal.valueOf(96);
//            System.out.println(e.scale());
//
////            System.out.println("bitlen " + a.unscaledValue().bitLength());
//
//        abi = encodeFunctionCall("a(fixed16x0,fixed16x1,fixed16x2,fixed16x3,fixed16x4)", a, b, c_, d, e);
////        } catch (Throwable t) {
////            System.out.println("\t\t" + t.getMessage());
////        }

        abi = ABI.encodeFunctionCall("()");

        Object[] params_ = new Object[] {
                BigInteger.ONE, new BigInteger[][] {  }, BigInteger.ONE, new BigInteger[][] {  },
                BigDecimal.valueOf(1L, 0), new BigDecimal[][] {  }, BigDecimal.valueOf(1L, 0), new BigDecimal[][] {  },
        };

//        Tuple tuple = new Tuple(params_);

        try {
            abi = ABI.encodeFunctionCall("yabba_(int,int[99][0],uint,uint[99][0],fixed,fixed[99][0],ufixed,ufixed[99][0])",
                    params_
            );
        } catch (Throwable t) { System.out.println("\t\t" + t.getMessage());
        }

        System.out.println(Hex.toHexString(abi.array()));

        abi = ABI.encodeFunctionCall("yabba_(int256,int256[2][0],uint256,uint256[2][0],fixed128x18,fixed128x18[2][0],ufixed128x18,ufixed128x18[2][0])",
                params_
        );

        System.out.println(Hex.toHexString(abi.array()));

//        abi = encodeFunctionCall("yabba_(int[1])",
//                (Object) new BigInteger[] { BigInteger.ZERO }
//        );
//        abi = encodeFunctionCall("yabba_(int[1])",
//                (Object) new BigInteger[] { BigInteger.ZERO }
//        );


//        try {
        abi = ABI.encodeFunctionCall("dabba_(ufixed[1])", (Object) new BigDecimal[] { BigDecimal.TEN });
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }

        System.out.println(Hex.toHexString(abi.array()));


        if(true) return;

//        Class c0 = int[].class;
//        System.out.println(c0.getName());
//
//        Class c = Class.forName(c0.getName());
//        System.out.println(c.getName());
//
//        System.out.println(Class.forName("[[I") == int[][].class);
//
//
//        final int len = 4 + (9 * 32);
//
//        Keccak keccak = new Keccak(256);
//
//        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[len]);
//        keccak.update("baz(uint32,bool)".getBytes(ASCII));
////        keccak.digest(out, 0, 4);
//        keccak.digest(outBuffer, 4);
//
//
//        Encoder.insertInt(69L, outBuffer);
//        Encoder.insertBool(true, outBuffer);
//
//        System.out.println(Strings.encode(outBuffer.array(), HEX));
//
//        outBuffer.position(0);
//
//        keccak.update("bar(bytes3[2])".getBytes(ASCII));
//        keccak.digest(outBuffer, 4);
//
//        Encoder.insertBytesArray(new byte[][] { "abc".getBytes(ASCII), "def".getBytes(ASCII) }, outBuffer);
//
//        System.out.println(Strings.encode(outBuffer.array(), HEX));
//
//        outBuffer.position(0);
//
//        keccak.update("sam(bytes,bool,uint256[])".getBytes(ASCII));
//        keccak.digest(outBuffer, 4);
//
//        Encoder.insertInt(0x60, outBuffer);
//        Encoder.insertBool(true, outBuffer);
//        Encoder.insertInt(0xa0, outBuffer);
//        Encoder.insertInt(0x04, outBuffer);
//        Encoder.insertBytes("dave".getBytes(ASCII), outBuffer);
//        Encoder.insertInt(0x03, outBuffer);
//        Encoder.insertInts(new int[] { 1, 2, 3 }, outBuffer);
//
//        System.out.println(Strings.encode(outBuffer.array(), HEX));
    }

}
