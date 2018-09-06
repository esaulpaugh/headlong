package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.Function;
import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

// TODO monte carlo smoke test
public class DecodeTest {

    // (bytes32)uint8)
    public static void main(String[] args0) throws ParseException {

//        BaseTypeInfo.remove("decimal");

        // "large(bytes32[][])"
        String signature = "(bool[6][2],((decimal),uint)[1],bool)"; // (bytes1[3][2])[1]

        Function f0 = new Function(signature);

        System.out.println(f0.getCanonicalSignature());
        System.out.println(f0.selectorHex());

        System.out.println("CANONICAL: " + f0.getCanonicalSignature());

//        final BigDecimal abba = new BigDecimal(BigInteger.valueOf(2).pow(128), 18);
//        final BigDecimal dabba = new BigDecimal(BigInteger.valueOf(2).pow(127), 18);
//        final BigDecimal upow = abba.subtract(BigDecimal.valueOf(1));
//        final BigDecimal pow = dabba.subtract(BigDecimal.valueOf(1));

        Object[] args = new Object[] {
                new boolean[][] { new boolean[] { true, true, false, false, true, false }, new boolean[] { true, true, false, false, true, false } },
                new Tuple[] { new Tuple(Tuple.singleton(new BigDecimal(BigInteger.valueOf(7), 10)), BigInteger.ONE) },
                true

//                "01234567890123456789012345678901".getBytes()
//                new byte[][][] { new byte[][] { "01234567890123456789012345678901".getBytes() } }

//                new BigDecimal[][] { new BigDecimal[] { BigDecimal.valueOf(1.0000000001) }, new BigDecimal[] { BigDecimal.valueOf(2.0000000005) },  }
//                new Tuple( new Tuple(new Tuple(new byte[][] { new byte[1] }, (byte) 9), Tuple.singleton("_".getBytes()), Tuple.singleton("yaaaaaaaaaaaaa".getBytes()) ), Tuple.singleton(new byte[45]) ),
//                new Tuple( new Tuple(new Tuple(" ".getBytes(), "_".getBytes()), Tuple.singleton("_".getBytes()), Tuple.singleton("yaaaaaaaaaaaaa".getBytes()) ), Tuple.singleton(new byte[45]) ),
//                new Tuple( new Tuple("_a".getBytes(), "yaaaaaaaaaaaaa".getBytes() ), Tuple.singleton(new byte[45]) )
//                new Tuple[] {
//                        Tuple.singleton(
//                                new byte[][][] {
//                                        new byte[][] { "_".getBytes(), "y".getBytes(), "y".getBytes() },
//                                        new byte[][] { "a".getBytes(), "B".getBytes(), "z".getBytes() },
//
////                                new int[] { 3, 5, 9 },
////                                new int[] { 1, 3, 5 }
//                                }
//                        )
//                }

        };
        ByteBuffer bb = f0.encodeCall(args); // , pow, upow
        byte[] abi = bb.array();
        EncodeTest.printABI(abi);
        Tuple t = f0.decodeCall(abi);
        System.out.println("========= " + Arrays.deepEquals(t.elements, args));

//        if(true)return;

        // (uint8),uint8,(int24,bytes),
        Function f = new Function("(string[][][],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))"); // ,(string),string
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

        abi = f.encodeCall(argsIn).array();

        EncodeTest.printABI(abi);

        Tuple tupleOut = f.decodeCall(abi);
        Object[] argsOut = tupleOut.elements;

        System.out.println("== " + Arrays.deepEquals(argsIn, argsOut));
    }
}
