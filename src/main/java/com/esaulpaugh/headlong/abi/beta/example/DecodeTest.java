package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.Function;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Arrays;

public class DecodeTest {

    public static void main(String[] args0) throws ParseException {

        Function f = new Function("(string[][3][])"); // fixed[][3][]

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

        Object[] argsIn = new Object[] { triple }; // new int[][][] { new int[][] { new int[] { 1, 3, 5 } } } // new short[] { (short) 6, (short) 7 } // new byte[] { 1, 2, 3 }

        byte[] abi = f.encodeCall(argsIn).array();

        EncodeTest.printABI(abi);

        Object[] argsOut = f.decodeCall(abi);

        System.out.println("== " + equals(argsIn, argsOut));

    }

    private static boolean equals(Object[] args0, Object[] args1) {
        return Arrays.deepEquals(args0, args1);
    }

//        final int len = args0.length;
//        for (int i = 0; i < len; i++) {
//            Object arg0 = args0[i];
//            Object arg1 = args1[i];
//            if(arg0 instanceof boolean[]) {
//                if(!(arg1 instanceof boolean[])) {
//                    return false;
//                }
//                if(!Arrays.equals((boolean[]) arg0, ((boolean[]) arg1))) {
//                    return false;
//                }
//            } else if(arg0 instanceof byte[]) {
//                if(!(arg1 instanceof byte[])) {
//                    return false;
//                }
//                if(!Arrays.equals((byte[]) arg0, ((byte[]) arg1))) {
//                    return false;
//                }
//            } else if(arg0 instanceof short[]) {
//                if(!(arg1 instanceof short[])) {
//                    return false;
//                }
//                if(!Arrays.equals((short[]) arg0, ((short[]) arg1))) {
//                    return false;
//                }
//            } else if(arg0 instanceof int[]) {
//                if(!(arg1 instanceof int[])) {
//                    return false;
//                }
//                if(!Arrays.equals((int[]) arg0, ((int[]) arg1))) {
//                    return false;
//                }
//            } else if(arg0 instanceof long[]) {
//                if(!(arg1 instanceof long[])) {
//                    return false;
//                }
//                if(!Arrays.equals((long[]) arg0, ((long[]) arg1))) {
//                    return false;
//                }
//            } else {
//                if(!Objects.equals(arg0, arg1)) {
//                    return false;
//                }
//            }
//        }
//        return true;

}
