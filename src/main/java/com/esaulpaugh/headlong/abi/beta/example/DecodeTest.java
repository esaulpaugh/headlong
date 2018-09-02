package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.Function;
import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.text.ParseException;
import java.util.Arrays;

public class DecodeTest {

    public static void main(String[] args0) throws ParseException {

        // (uint8),uint8,(int24,bytes),
        Function f = new Function("((string),string)"); // ,(string),string
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
        Object[] argsIn = new Object[] { new Tuple(""), "" }; // , new Tuple(""), ""

        byte[] abi = f.encodeCall(argsIn).array();

        EncodeTest.printABI(abi);

        Tuple tupleOut = f.decodeCall(abi);
        Object[] argsOut = tupleOut.elements;

        System.out.println("== " + Arrays.deepEquals(argsIn, argsOut));
    }
}
