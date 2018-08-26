package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.Function;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;

public class Example {

    private static final byte[] dave = "dave".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args0) throws ParseException {

//        Function sam = new Function("sam(bytes,bool,uint256[][])"); // bool[4],bool[],uint[]
//        ByteBuffer abi = sam.encodeCall(
//                dave,
//                true,
//                new BigInteger[][] {
//                        new BigInteger[] {},
//                        new BigInteger[] {
//                                BigInteger.ONE,
//                                BigInteger.valueOf(2L),
//                                BigInteger.valueOf(3L)
//                        }
//                }
//
//        );

        Function test = new Function("(bytes[9)");

        Function g = new Function("g(uint[][],string[])");
        System.out.println(g.getSelectorHex());
        ByteBuffer abi = g.encodeCall(
                new BigInteger[][] {
                        new BigInteger[] {
                                BigInteger.ONE,
                                BigInteger.valueOf(2L),
                        },
                        new BigInteger[] {
                                BigInteger.valueOf(3L)
                        }
                },
                new String[] { "one", "two", "three" } //
//                }
//                (Object) "abcd".getBytes(Charset.forName("UTF-8"))
//                (Object) new int[0]
        );

        byte[] abiBytes = abi.array();
        System.out.println(Hex.toHexString(Arrays.copyOfRange(abiBytes, 0, 4)));
        final int end = abiBytes.length;
        int i = 4;
        while(i < end) {
            System.out.println( (i / 32) + "\t" + Hex.toHexString(Arrays.copyOfRange(abiBytes, i, i + 32)));
            i += 32;
        }
        System.out.println("\n" + Hex.toHexString(abi.array()));


//        ByteBuffer abi;
//        abi = ABI.encodeFunctionCall("funct((string))", new Tuple("hello_THERE YOYOYOYOYOYO"));
//        System.out.println(Hex.toHexString(abi.array()));
//
////        byte[] function = new byte[24];
////        function[0] = 126;
////        function[23] = 127;
////        byte[] bytes32 = new byte[32];
////        bytes32[0] = 126;
////        bytes32[31] = 127;
//
//        abi = ABI.encodeFunctionCall("sam(bytes,bool,uint256[])",
//                "dave".getBytes(StandardCharsets.UTF_8),
//                true,
//                new BigInteger[]{ BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3) }
//        );
//        System.out.println(Hex.toHexString(abi.array()));
//
//        abi = ABI.encodeFunctionCall("(string,string,string[])",
//                "",
//                "y",
//                new String[0]
//        );
//        System.out.println(Hex.toHexString(abi.array()));
//
//        try {
//            abi = ABI.encodeFunctionCall("");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall(")");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(int");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(,int");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(int,)");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a()%");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a()");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(())", new Tuple());
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(()))", Tuple.EMPTY);
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(()]int)", Tuple.EMPTY, BigInteger.ZERO);
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a((");
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a(()", Tuple.EMPTY);
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        try {
//            abi = ABI.encodeFunctionCall("a((),", Tuple.EMPTY);
//        } catch (Throwable t) {
//            System.out.println("\t\t" + t.getMessage());
//        }
//        System.out.println(Hex.toHexString(abi.array()));
//
//        BigDecimal a = BigDecimal.valueOf(96, 0);
//        BigDecimal b = BigDecimal.valueOf((byte) 96.0);
//        BigDecimal c_ = BigDecimal.valueOf((short) 96.0);
//        BigDecimal d = BigDecimal.valueOf((int) 96.0);
//        BigDecimal e = BigDecimal.valueOf(96);
////            System.out.println(e.scale());
//        abi = ABI.encodeFunctionCall("a(fixed16x0,fixed16x1,fixed16x2,fixed16x3,fixed16x4)", a, b, c_, d, e);
//        System.out.println(Hex.toHexString(abi.array()));
//
//        abi = ABI.encodeFunctionCall("()");
//        System.out.println(Hex.toHexString(abi.array()));
//
//        Object[] args = new Object[] {
//                BigInteger.ONE, new BigInteger[][] {  }, BigInteger.ONE, new BigInteger[][] {  },
//                BigDecimal.valueOf(1L, 0), new BigDecimal[][] {  }, BigDecimal.valueOf(1L, 0), new BigDecimal[][] {  },
//        };
//        try {
//            abi = ABI.encodeFunctionCall(
//                    "yabba_(int,int[99][0],uint,uint[99][0],fixed,fixed[99][0],ufixed,ufixed[99][0])",
//                    args
//            );
//        } catch (Throwable t) { System.out.println("\t\t" + t.getMessage());
//        }
//        System.out.println(Hex.toHexString(abi.array()));
//
//        abi = ABI.encodeFunctionCall(
//                "yabba_(int256,int256[2][0],uint256,uint256[2][0],fixed128x18,fixed128x18[2][0],ufixed128x18,ufixed128x18[2][0])",
//                args
//        );
//        System.out.println(Hex.toHexString(abi.array()));
//
//        abi = ABI.encodeFunctionCall("dabba_(ufixed[1])", (Object) new BigDecimal[] { BigDecimal.TEN });
//        System.out.println(Hex.toHexString(abi.array()));
    }
}
