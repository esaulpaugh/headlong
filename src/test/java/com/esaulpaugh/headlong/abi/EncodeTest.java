package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

public class EncodeTest {

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
    public void complexFunctionTest() throws ParseException {
        Function f7 = new Function("(string[][][],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))");

        Object[] argsIn = new Object[] {
                new String[0][][],
                BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Byte.MAX_VALUE << 2)),
                new Tuple(7),
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } },
                new Tuple[] { new Tuple(13), new Tuple(-15) },
                new Tuple[] { new Tuple(17), new Tuple(-19) },
                Long.MAX_VALUE / 8_500_000,
                new Tuple[] { new Tuple((long) 0x7e), new Tuple((long) -0x7e) },
                new Tuple(BigInteger.TEN)
        };

        ByteBuffer abi = f7.encodeCallWithArgs(argsIn);

        Function.formatCall(abi.array());

        Tuple tupleOut = f7.decodeCall((ByteBuffer) abi.flip());
        Object[] argsOut = tupleOut.elements;

        System.out.println("== " + Arrays.deepEquals(argsIn, argsOut));
    }
}
