package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Random;

public class EncodeTest {

    @Test
    public void emptyParamTest() throws Throwable {
        TestUtils.assertThrown(ParseException.class, "empty parameter @ 0", () -> new Function("baz(,)"));

        TestUtils.assertThrown(ParseException.class, "empty parameter @ 1", () -> new Function("baz(bool,)"));

        TestUtils.assertThrown(ParseException.class, "empty parameter @ 1 of element 1", () -> new Function("baz(bool,(int,,))"));
    }

    @Test
    public void illegalCharsTest() throws Throwable {
        TestUtils.assertThrown(ParseException.class, "non-ascii char, '\u02a6' \\u02a6, @ index 2", () -> new Function("ba\u02a6z(uint32,bool)"));

        TestUtils.assertThrown(ParseException.class, "non-type char, '\u02a6' \\u02a6, @ index 4 of element 0 of element 1", () -> new Function("baz(int32,(bool\u02a6))"));
    }

    @Test
    public void simpleFunctionTest() throws ParseException {
        Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature automatically
        Tuple args = new Tuple(69L, true);

        // Two equivalent styles:
        ByteBuffer one = f.encodeCall(args);
        ByteBuffer two = f.encodeCallWithArgs(69L, true);

        System.out.println(Function.formatCall(one.array())); // a multi-line hex representation

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        System.out.println(decoded.equals(args));
    }

    @Test
    public void tupleArrayTest() throws ParseException {
        Function f = new Function("((int16)[2][][1])");

        Object[] argsIn = new Object[] {
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } }
        };

        f.encodeCallWithArgs(argsIn);
    }

    @Test
    public void complexFunctionTest() throws ParseException {
        Function f = new Function("(function[2][][],string[0][0],address[],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))");

        byte[] func = new byte[24];
        new Random(MonteCarloTest.seed(System.nanoTime())).nextBytes(func);

        String oneSixty = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
//        String oneSixty = "10000000000000000000000000000000000000000";
        System.out.println(oneSixty + " " + oneSixty.length() * 4);
        BigInteger addr = new BigInteger(oneSixty, 16);
        System.out.println(addr);

        Object[] argsIn = new Object[] {
                new byte[][][][] { new byte[][][] { new byte[][] { func, func } } },
                new String[0][],
                new BigInteger[] { addr },
                BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Byte.MAX_VALUE << 2)),
                new Tuple(7),
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } },
                new Tuple[] { new Tuple(13), new Tuple(-15) },
                new Tuple[] { new Tuple(17), new Tuple(-19) },
                Long.MAX_VALUE / 8_500_000,
                new Tuple[] { new Tuple((long) 0x7e), new Tuple((long) -0x7e) },
                new Tuple(BigInteger.TEN)
        };

        ByteBuffer abi = f.encodeCallWithArgs(argsIn);

        Function.formatCall(abi.array());

        Tuple tupleOut = f.decodeCall((ByteBuffer) abi.flip());
        Object[] argsOut = tupleOut.elements;

        System.out.println("== " + Arrays.deepEquals(argsIn, argsOut));
    }
}
