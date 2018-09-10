package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

public class DecodeTest {

    private static BigDecimal f(long v, int s) {
        return BigDecimal.valueOf(v, s);
    }

    // (bytes32)uint8)
    public static void main(String[] args0) throws ParseException {

        Function f = new Function("baz(uint32,bool)");
        ByteBuffer buffer = f.encodeCall(69L, true);

        System.out.println(Function.format(buffer.array()));
        System.out.println(Function.hex(buffer.array()));

//        Keccak k = new Keccak(256);
//
//        // c82bdef641b71a621829729392798cefd8d7fe600efeb7a95b2919fc069a9883 -- 7, 9, 11
//        // 5ca9d4d77c79da4b8a2bb9c11708a954cb8158c98a4864176142dd72d22141cf -- "NINETY_NINE_LUFTBALLONS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11111!11!!11!1!1!!"
//        // 842b907be20a8698c5c28237510a9ba65bdb991b5d7baeef64998196999f5375 -- 7, 9, 11, 12 + "yoyo"
//
//        byte[] digest;
//
//        // fcbdffde81d1e0977a6155a25d9407b27260c3553a67082b927a1b11dd12572f
//        k.update("".getBytes(CHARSET_UTF_8));
//        digest = k.digest();
//        System.out.println(Strings.encode(digest, HEX));
//
//        k.update(new byte[] { 7, 9, 11 });
//        digest = k.digest();
//        System.out.println(Strings.encode(digest, HEX));
//
//        k.update("NINETY_NINE_LUFTBALLONS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11111!11!!11!1!1!!".getBytes(CHARSET_UTF_8));
//        digest = k.digest();
//        System.out.println(Strings.encode(digest, HEX));
//
//        k.update(new byte[] { 7, 9, 11, 12 });
//        k.update("yoyo".getBytes(CHARSET_UTF_8));
//        digest = k.digest();
//        System.out.println(Strings.encode(digest, HEX));

//        if(true)return;

//        final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(-667342700048419528L);
//        final MonteCarloTestCase testCase = new MonteCarloTestCase(params);
//        testCase.run();

//        if(true) return;


//        // (address[],int88,int192[1][1][][7][],int24[][],int144,string[][5][][8][],decimal,uint64[][][7][],uint184)
//        Function function = testCase.function();
//        System.out.println(function.getCanonicalSignature());
//        TupleType tupleType = function.paramTypes;

//        Tuple argsTuple = testCase.argsTuple;
//        // ------------------------------------------------
//        // (address[],int88,int192[1][1][][7][]) // ,int24[][],int144,string[][5][][8][],decimal,uint64[][][7][],uint184
//        // address[],int88,int192[1][1][][7][],int24[][],int144,

//        Function f2 = new Function("((fixed24x14[13][][4])[])");

        final int s = 14;
        final BigDecimal[] inner = new BigDecimal[] {  }; // f(1, s), f(2, s), f(3, s), f(4, s), f(4, s)
//        final BigDecimal[][] four = new BigDecimal[][] { inner };
//        final BigDecimal[] thirteen = new BigDecimal[] { f(1, s), f(2, s), f(3, s), f(4, s), f(5, s), f(6, s), f(7, s), f(8, s), f(9, s), f(10, s), f(11, s), f(12, s), f(13, s),  };

        Function f0 = new Function("(uint8,uint16,uint24,uint32,int8,int16,int24,int32)");
        Tuple argg = new Tuple(1, 2, 3, 4L, 5, 6, 7, 8);
        ByteBuffer b0 = f0.encodeCall(argg);
        byte[] abi0 = b0.array();
        abi0[3] = -1;
        Function.format(abi0);
        Tuple x = f0.decodeCall(abi0);
        System.out.println(x.equals(argg));

        if(true)return;

        Function f2 = new Function("((fixed24x14[])[1])");


    Tuple subtuple = new Tuple(
                (Object) new Tuple[] { new Tuple(
                        (Object) inner
//                        (Object) new BigDecimal[][][] {
//                        new BigDecimal[][] { thirteen, thirteen,  },
//                        new BigDecimal[][] { },
//                        new BigDecimal[][] { thirteen, thirteen, thirteen, thirteen },
//                        new BigDecimal[][] { thirteen, thirteen, thirteen },
//                        }
                ) }
        );

//        Tuple subtuple = argsTuple.subtuple(5, 6);
        ByteBuffer bb = f2.encodeCall(subtuple);

//        if(true)return;

////        BaseTypeInfo.remove("decimal");
//
//        // "large(bytes32[][])"
//        String signature = "(address[])"; // (bytes1[3][2])[1]
//
//        Function f0 = new Function(signature);
//
//        System.out.println(f0.getCanonicalSignature());
//        System.out.println(f0.selectorHex());
//
//        System.out.println("CANONICAL: " + f0.getCanonicalSignature());
//
////        final BigDecimal abba = new BigDecimal(BigInteger.valueOf(2).pow(128), 18);
////        final BigDecimal dabba = new BigDecimal(BigInteger.valueOf(2).pow(127), 18);
////        final BigDecimal upow = abba.subtract(BigDecimal.valueOf(1));
////        final BigDecimal pow = dabba.subtract(BigDecimal.valueOf(1));
//
//        //  != 166815876489431754686885849
//        Object[] args = new Object[] {
//
//                new BigInteger[] { new BigInteger("994640482486759406802903684908903296859034869058") }
//
////                new BigInteger("-142669133331913314037895207")
//
////                new boolean[][] { new boolean[] { true, true, false, false, true, false }, new boolean[] { true, true, false, false, true, false } },
////                new Tuple[] { new Tuple(Tuple.singleton(new BigDecimal(BigInteger.valueOf(7), 10)), BigInteger.ONE) },
////                true
//
////                "01234567890123456789012345678901".getBytes()
////                new byte[][][] { new byte[][] { "01234567890123456789012345678901".getBytes() } }
//
////                new BigDecimal[][] { new BigDecimal[] { BigDecimal.valueOf(1.0000000001) }, new BigDecimal[] { BigDecimal.valueOf(2.0000000005) },  }
////                new Tuple( new Tuple(new Tuple(new byte[][] { new byte[1] }, (byte) 9), Tuple.singleton("_".getBytes()), Tuple.singleton("yaaaaaaaaaaaaa".getBytes()) ), Tuple.singleton(new byte[45]) ),
////                new Tuple( new Tuple(new Tuple(" ".getBytes(), "_".getBytes()), Tuple.singleton("_".getBytes()), Tuple.singleton("yaaaaaaaaaaaaa".getBytes()) ), Tuple.singleton(new byte[45]) ),
////                new Tuple( new Tuple("_a".getBytes(), "yaaaaaaaaaaaaa".getBytes() ), Tuple.singleton(new byte[45]) )
////                new Tuple[] {
////                        Tuple.singleton(
////                                new byte[][][] {
////                                        new byte[][] { "_".getBytes(), "y".getBytes(), "y".getBytes() },
////                                        new byte[][] { "a".getBytes(), "B".getBytes(), "z".getBytes() },
////
//////                                new int[] { 3, 5, 9 },
//////                                new int[] { 1, 3, 5 }
////                                }
////                        )
////                }
//
//        };
//        ByteBuffer bb = f0.encodeCall(args); // , pow, upow
        byte[] abi = bb.array();
        Function.format(abi);
        Tuple t = f2.decodeCall(abi);
        System.out.println("========= " + Arrays.deepEquals(t.elements, subtuple.elements));

        if(true)return;

        // (uint8),uint8,(int24,bytes),
        Function f7 = new Function("(string[][][],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))"); // ,(string),string
//        Function f = new Function("(string[2][3][])");

//        BigInteger five = BigInteger.valueOf(5);
//        BigInteger seven = BigInteger.valueOf(7);
//        BigDecimal[] a = new BigDecimal[] { new BigDecimal(five, 18), new BigDecimal(five, 18) }; //
//        BigDecimal[] b = new BigDecimal[] { new BigDecimal(seven, 18), new BigDecimal(seven, 18) }; //
//        BigDecimal[] c = new BigDecimal[] { new BigDecimal(seven, 18), new BigDecimal(seven, 18) }; //
//        BigDecimal[][] one = new BigDecimal[][] { a, b, a }; // a, b, a
//        BigDecimal[][] two = new BigDecimal[][] { a, b, c }; // a, b, c
//        BigDecimal[][][] triple = new BigDecimal[][][] { one, two }; // one, two

        String five = "five";
        String seven = "seven";
        String[] a = new String[] { five, five }; //
        String[] b = new String[] { seven, seven }; //
        String[] c = new String[] { five, seven }; //
        String[][] one = new String[][] { a, b, a }; // a, b, a
        String[][] two = new String[][] { a, b, c }; // a, b, c
        String[][][] triple = new String[][][] { one, two }; // one, two

//        byte[] five = new byte[5];
//        byte[] seven = new byte[7];
//        byte[][] a = new byte[][] { five, five }; //
//        byte[][] b = new byte[][] { seven, seven }; //
//        byte[][] c = new byte[][] { five, seven }; //
//        byte[][][] one = new byte[][][] { a, b, a }; // a, b, a
//        byte[][][] two = new byte[][][] { a, b, c }; // a, b, c
//        byte[][][][] triple = new byte[][][][] { one, two }; // one, two

//        BigInteger five = BigInteger.valueOf(5);
//        BigInteger seven = BigInteger.valueOf(7);
//        BigInteger[] a = new BigInteger[] { five, five}; //
//        BigInteger[] b = new BigInteger[] { seven, seven }; //
//        BigInteger[] c = new BigInteger[] { five, seven }; //
//        BigInteger[][] one = new BigInteger[][] { a, b, a }; // a, b, a
//        BigInteger[][] two = new BigInteger[][] { a, b, c }; // a, b, c
//        BigInteger[][][] triple = new BigInteger[][][] { one, two }; // one, two

        // new Tuple((byte) 6), (byte) 99, new Tuple(1001, new byte[0]),

        // new Tuple(new Tuple("five"))
        Object[] argsIn = new Object[] {

                triple,

                // ((uint8)(int8)[],(int8)[],(int8)[],uint8,(int8)[],(uint8))
                BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Byte.MAX_VALUE << 2)),
                new Tuple((byte) 7),
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple((short) 9), new Tuple((short) -11) } } },
                new Tuple[] { new Tuple(13), new Tuple(-15) },
                new Tuple[] { new Tuple(17), new Tuple(-19) },
                Long.MAX_VALUE / 8_500_000,
                new Tuple[] { new Tuple((long) 0x7e), new Tuple((long) -0x7e) },
                new Tuple(BigInteger.TEN)

//                new Tuple[] { new Tuple((byte) 7) }
//                new Tuple(new Tuple((Object) triple)),
//                triple,
//                new Tuple(new BigDecimal(BigInteger.ONE, 18)), new BigDecimal(BigInteger.ONE, 18)
        }; // , new Tuple(""), ""

        abi = f7.encodeCall(argsIn).array();

        Function.format(abi);

        Tuple tupleOut = f7.decodeCall(abi);
        Object[] argsOut = tupleOut.elements;

        System.out.println("== " + Arrays.deepEquals(argsIn, argsOut));
    }
}
