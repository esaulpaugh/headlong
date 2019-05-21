/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.abi.TupleTypeParser.EMPTY_PARAMETER;

public class EncodeTest {

    @Test
    public void emptyParamTest() throws Throwable {
        assertThrown(ParseException.class, "@ index 0, " + EMPTY_PARAMETER, () -> new Function("baz(,)"));

        assertThrown(ParseException.class, "@ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,)"));

        assertThrown(ParseException.class, "@ index 1, @ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,(int,,))"));
    }

    @Test
    public void illegalCharsTest() throws Throwable {
        assertThrown(ParseException.class, "illegal char \\u02a6 '\u02a6' @ index 2", () -> new Function("ba\u02a6z(uint32,bool)"));

        assertThrown(ParseException.class, "@ index 1, @ index 0, unrecognized type: bool\u02a6 (0000000000000000000000000000626f6f6ccaa6)", () -> new Function("baz(int32,(bool\u02a6))"));
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

        Assert.assertEquals(decoded, args);
    }

    @Test
    public void uint8ArrayTest() throws ParseException {
        Function f = new Function("baz(uint8[])");

        Tuple args = Tuple.singleton(new int[] { 0xFF, -1, 1, 2, 0 });
        ByteBuffer two = f.encodeCall(args);

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        Assert.assertEquals(decoded, args);
    }

    @Test
    public void tupleArrayTest() throws ParseException {
        Function f = new Function("((int16)[2][][1])");

        Object[] argsIn = new Object[] {
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } }
        };

        ByteBuffer buf = f.encodeCallWithArgs(argsIn);

        Assert.assertArrayEquals(FastHex.decode("f9354bbb0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000009fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff5"), buf.array());
    }

    @Test
    public void fixedLengthDynamicArrayTest() throws Throwable {

        final Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));

        Supplier<Object> bytesSupplier = () -> { byte[] v = new byte[rand.nextInt(33)]; rand.nextBytes(v); return v; };
        Supplier<Object> stringSupplier = () -> { byte[] v = new byte[rand.nextInt(33)]; rand.nextBytes(v); return new String(v, Strings.CHARSET_UTF_8); };
        Supplier<Object> booleanArraySupplier = () -> { boolean[] v = new boolean[rand.nextInt(4)]; Arrays.fill(v, rand.nextBoolean()); return v; };
        Supplier<Object> intArraySupplier = () -> { BigInteger[] v = new BigInteger[rand.nextInt(4)]; Arrays.fill(v, BigInteger.valueOf(rand.nextInt())); return v; };

        testFixedLenDynamicArray("bytes", new byte[1 + rand.nextInt(34)][], bytesSupplier);
        testFixedLenDynamicArray("string", new String[1 + rand.nextInt(34)], stringSupplier);
        testFixedLenDynamicArray("bool[]", new boolean[1 + rand.nextInt(34)][], booleanArraySupplier);
        testFixedLenDynamicArray("int[]", new BigInteger[1 + rand.nextInt(34)][], intArraySupplier);

        final String msg = "array lengths differed, expected.length=32 actual.length=0";
        assertThrown(AssertionError.class, msg, () -> testFixedLenDynamicArray("bytes", new byte[0][], null));
        assertThrown(AssertionError.class, msg, () -> testFixedLenDynamicArray("string", new String[0], null));
        assertThrown(AssertionError.class, msg, () -> testFixedLenDynamicArray("bool[]", new boolean[0][], null));
        assertThrown(AssertionError.class, msg, () -> testFixedLenDynamicArray("int[]", new BigInteger[0][], null));
    }

    private static void testFixedLenDynamicArray(String baseType, Object[] args, Supplier<Object> supplier) throws ParseException {
        final int n = args.length;
        TupleType a = TupleType.of(baseType + "[" + n + "]");

        String[] types = new String[n];
        Arrays.fill(types, baseType);
        TupleType b = TupleType.parse("(" + TupleType.of(types) + ")");

        System.out.println(a + " vs " + b);

        for (int i = 0; i < args.length; i++) {
            args[i] = supplier.get();
        }

        Tuple aArgs = new Tuple((Object) args);
        Tuple bArgs = new Tuple(new Tuple((Object[]) args));

        byte[] aEncoding = a.encode(aArgs).array();
        byte[] bEncoding = b.encode(bArgs).array();

//        System.out.println(TupleType.format(aEncoding));
//        System.out.println(TupleType.format(bEncoding));

        Assert.assertArrayEquals(aEncoding, bEncoding);
    }

    @Test
    public void complexFunctionTest() throws ParseException {
        Function f = new Function("(function[2][][],bytes24,string[0][0],address[],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))");

        byte[] func = new byte[24];
        new Random(MonteCarloTest.getSeed(System.nanoTime())).nextBytes(func);

        String oneSixty = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
//        String oneSixty = "10000000000000000000000000000000000000000";
        System.out.println(oneSixty + " " + oneSixty.length() * 4);
        BigInteger addr = new BigInteger(oneSixty, 16);
        System.out.println(addr);

        Object[] argsIn = new Object[] {
                new byte[][][][] { new byte[][][] { new byte[][] { func, func } } },
                func,
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

        Assert.assertTrue(Arrays.deepEquals(argsIn, argsOut));
    }
}
