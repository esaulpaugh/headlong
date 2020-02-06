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

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.abi.TypeFactory.EMPTY_PARAMETER;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncodeTest {

    private static final Class<IllegalArgumentException> PARSE_ERR = IllegalArgumentException.class;

    private static final char[] ALPHABET = "(),abcdefgilmnorstuxy0123456789[]".toCharArray(); // ")uint8,[]"
    private static final int ALPHABET_LEN = ALPHABET.length;

    @Disabled("takes minutes to run")
    @Test
    public void fuzzSignatures() throws InterruptedException {
        final Random r = TestUtils.seededRandom();
        final Runnable runnable = () -> {
            for (int len = 6; len <= 9; len++) {
                System.out.println(len + "(" + Thread.currentThread().getId() + ")");
                final char[] temp = new char[len];
                temp[0] = '(';
                temp[len - 1] = ')';
                final int lim = temp.length - 1;
                final int num = 4_000_000 + (int) Math.pow(3.7, len);
                for (int j = 0; j < num; j++) {
                    for (int i = 1; i < lim; i++) {
                        temp[i] = ALPHABET[r.nextInt(ALPHABET_LEN)];
                    }
                    String sig = new String(temp);
                    try {
                        TupleType tt = TupleType.parse(sig);
                        System.out.println("\t\t\t" + len + ' ' + sig);
                    } catch (IllegalArgumentException parseException) {
                        /* do nothing */
                    } catch (Throwable t) {
                        System.err.println(sig);
                        t.printStackTrace();
                        throw new RuntimeException(t);
                    }
                }
            }
        };

        final Thread[] threads = new Thread[7];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].start();
        }
        runnable.run();
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    public void testFormat() throws Throwable {
        testFormat(true, Function::formatCall);
    }

    @Test
    public void testTupleFormat() throws Throwable {
        testFormat(false, TupleType::format);
    }

    private static void testFormat(boolean func, java.util.function.Function<byte[], String> format) throws Throwable {
        final int expectedMod = func ? 4 : 0;
        byte[] master = new byte[260];
        Arrays.fill(master, (byte) 0xff);
        for (int i = 0; i < 260; i++) {
            byte[] x = Arrays.copyOfRange(master, 0, i);
            int mod = i % UNIT_LENGTH_BYTES;
            if(mod == expectedMod) {
                String formatted = format.apply(x);
                assertEquals(i * 2, formatted.codePoints().filter(ch -> ch == 'f').count());
                int div = (i - expectedMod) / UNIT_LENGTH_BYTES;
                if(div > 0) {
                    String substr = (div - 1) + "\tffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
                    TestUtils.assertMatching(func, formatted.contains(substr));
                }
            } else {
                TestUtils.assertThrown(IllegalArgumentException.class, "expected length mod 32 == 0, found: ", () -> format.apply(x));
            }
        }
    }

    @Test
    public void nonTerminatingTupleTest() throws Throwable {
        assertThrown(PARSE_ERR, "unrecognized type: aaaaaa", () -> TupleType.parse("aaaaaa"));

        assertThrown(PARSE_ERR, "unrecognized type: (", () -> Function.parse("("));

        assertThrown(PARSE_ERR, "unrecognized type: ([", () -> Function.parse("(["));

        assertThrown(PARSE_ERR, "unrecognized type: (int", () -> Function.parse("(int"));

        assertThrown(PARSE_ERR, "unrecognized type: (bool[],", () -> Function.parse("(bool[],"));

        assertThrown(PARSE_ERR, "unrecognized type: (()", () -> Function.parse("(()"));

        assertThrown(PARSE_ERR, "unrecognized type: (())...", () -> Function.parse("(())..."));

        assertThrown(PARSE_ERR, "unrecognized type: ((((()))", () -> Function.parse("((((()))"));

        assertThrown(PARSE_ERR, "illegal signature termination", () -> Function.parse("f()[]"));
    }

    @Test
    public void emptyParamTest() throws Throwable {
        assertThrown(PARSE_ERR, EMPTY_PARAMETER, () -> Function.parse("(,"));

        assertThrown(PARSE_ERR, "@ index 0, " + EMPTY_PARAMETER, () -> new Function("baz(,)"));

        assertThrown(PARSE_ERR, "@ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,)"));

        assertThrown(PARSE_ERR, "@ index 1, @ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,(int,,))"));
    }

    @Test
    public void illegalCharsTest() throws Throwable {
        assertThrown(PARSE_ERR, "illegal char", () -> Function.parse("œ()"));

        assertThrown(PARSE_ERR, "illegal char 0x2a6 '\u02a6' @ index 2", () -> new Function("ba\u02a6z(uint32,bool)"));

        assertThrown(PARSE_ERR, "@ index 1, @ index 0, unrecognized type: bool\u02a6", () -> new Function("baz(int32,(bool\u02a6))"));
    }

    @Test
    public void simpleFunctionTest() {
        Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature automatically
        Tuple args = new Tuple(69L, true);

        // Two equivalent styles:
        ByteBuffer one = f.encodeCall(args);
        ByteBuffer two = f.encodeCallWithArgs(69L, true);

        System.out.println(Function.formatCall(one.array())); // a multi-line hex representation

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        assertEquals(decoded, args);
    }

    @Test
    public void leadingZeroArrayLenTest() throws Throwable {
        assertThrown(PARSE_ERR, "leading zero in array length", () -> Function.parse("zzz(()[04])"));
    }

    @Test
    public void uint8ArrayTest() {
        Function f = new Function("baz(uint8[])");

        Tuple args = Tuple.singleton(new int[] { 0xFF, -1, 1, 2, 0 });
        ByteBuffer two = f.encodeCall(args);

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        assertEquals(decoded, args);
    }

    @Test
    public void tupleArrayTest() {
        Function f = new Function("((int16)[2][][1])");

        Object[] argsIn = new Object[] {
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } }
        };

        ByteBuffer buf = f.encodeCallWithArgs(argsIn);

        assertArrayEquals(FastHex.decode("f9354bbb0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000009fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff5"), buf.array());
    }

    @Test
    public void fixedLengthDynamicArrayTest() throws Throwable {

        final Random rand = TestUtils.seededRandom();

        Supplier<Object> bytesSupplier = () -> { byte[] v = new byte[rand.nextInt(33)]; rand.nextBytes(v); return v; };
        Supplier<Object> stringSupplier = () -> { byte[] v = new byte[rand.nextInt(33)]; rand.nextBytes(v); return new String(v, StandardCharsets.UTF_8); };
        Supplier<Object> booleanArraySupplier = () -> { boolean[] v = new boolean[rand.nextInt(4)]; Arrays.fill(v, rand.nextBoolean()); return v; };
        Supplier<Object> intArraySupplier = () -> { BigInteger[] v = new BigInteger[rand.nextInt(4)]; Arrays.fill(v, BigInteger.valueOf(rand.nextInt())); return v; };

        testFixedLenDynamicArray("bytes", new byte[1 + rand.nextInt(34)][], bytesSupplier);
        testFixedLenDynamicArray("string", new String[1 + rand.nextInt(34)], stringSupplier);
        testFixedLenDynamicArray("bool[]", new boolean[1 + rand.nextInt(34)][], booleanArraySupplier);
        testFixedLenDynamicArray("int[]", new BigInteger[1 + rand.nextInt(34)][], intArraySupplier);

        final String msg = "array lengths differ, expected: <32> but was: <0>";
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("bytes", new byte[0][], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("string", new String[0], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("bool[]", new boolean[0][], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("int[]", new BigInteger[0][], null));
    }

    private static void testFixedLenDynamicArray(String baseType, Object[] args, Supplier<Object> supplier) {
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

        assertArrayEquals(aEncoding, bEncoding);
    }

    @Test
    public void complexFunctionTest() {
        Function f = new Function("(function[2][][],bytes24,string[0][0],address[],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))");

        byte[] func = new byte[24];
        TestUtils.seededRandom().nextBytes(func);

//                       10000000000000000000000000000000000000000
//                        FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
        String uint160 = "ff00ee01dd02cc03cafebabe9906880777086609";
        BigInteger addr = new BigInteger(uint160, 16);
        assertEquals(160, uint160.length() * 4);
        assertEquals(uint160, addr.toString(16));
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

        assertTrue(Function.formatCall(abi.array()).contains("18\t000000000000000000000000" + uint160));

        Tuple tupleOut = f.decodeCall((ByteBuffer) abi.flip());

        assertTrue(Arrays.deepEquals(argsIn, tupleOut.elements));
    }

    @Test
    public void paddingTest() {
        Function f = new Function("(bool,uint8,int64,address,ufixed,bytes2,(string),bytes,function)");

        StringBuilder sb = new StringBuilder();
        for(ABIType<?> type : f.getParamTypes()) {
            sb.append(type.getClass().getSimpleName()).append(',');
        }
        Assertions.assertEquals("BooleanType,IntType,LongType,BigIntegerType,BigDecimalType,ArrayType,TupleType,ArrayType,ArrayType,", sb.toString());

        Tuple args = new Tuple(
                true,
                1,
                1L,
                BigInteger.valueOf(8L),
                new BigDecimal(BigInteger.TEN, 18),
                new byte[] { 1, 0 },
                Tuple.singleton("\u0002"),
                new byte[] { 0x04 },
                new byte[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 }
        );

        final int len = f.measureCallLength(args) + 7 + 8;

        byte[] ffff = new byte[len];
        Arrays.fill(ffff, (byte) 0xff);

        ByteBuffer full = (ByteBuffer) ByteBuffer.wrap(ffff).position(7);
        ByteBuffer empty = (ByteBuffer) ByteBuffer.allocate(len).position(7);

        f.encodeCall(args, full)
                .encodeCall(args, empty);

        byte[] fullBytes = full.array();
        byte[] emptyBytes = empty.array();
        byte[] xor = new byte[len];
        for(int i = 0; i < len; i++) {
            xor[i] = (byte) (fullBytes[i] ^ emptyBytes[i]);
        }

        System.out.println(Function.hexOf(full));
        System.out.println("^");
        System.out.println(Function.hexOf(empty));
        System.out.println("=");
        System.out.println(Function.hexOf(xor));

        assertArrayEquals(
                FastHex.decode("ffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffff"),
                xor
        );
    }
}
