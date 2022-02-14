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
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import com.sun.tools.javac.util.List;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.assertThrownMessageMatch;
import static org.junit.jupiter.api.Assertions.*;

public class DecodeTest {

    private static final Function FUNCTION = new Function("gogo((fixed[],int8)[1][][5])", "(ufixed,string)");

    private static final byte[] RETURN_BYTES = Strings.decode(
            "0000000000000000000000000000000000000000000000000000000000000045"
          + "0000000000000000000000000000000000000000000000000000000000000040"
          + "0000000000000000000000000000000000000000000000000000000000000004"
          + "7730307400000000000000000000000000000000000000000000000000000000"
    );

    private static final byte[] BAD_PADDING_A = Strings.decode(
            "0000000000000000000000000000000000000000000000000000000000000045"
          + "0000000000000000000000000000000000000000000000000000000000000040"
          + "0000000000000000000000000000000000000000000000000000000000000004"
          + "7730307480000000000000000000000000000000000000000000000000000000"
    );

    private static final byte[] BAD_PADDING_B = Strings.decode(
            "0000000000000000000000000000000000000000000000000000000000000045"
          + "0000000000000000000000000000000000000000000000000000000000000040"
          + "0000000000000000000000000000000000000000000000000000000000000004"
          + "7730307400000000000000000000000200000000000000000000000000000000"
    );

    private static final byte[] BAD_PADDING_C = Strings.decode(
            "0000000000000000000000000000000000000000000000000000000000000045"
          + "0000000000000000000000000000000000000000000000000000000000000040"
          + "0000000000000000000000000000000000000000000000000000000000000004"
          + "7730307400000000000000000000000000000000000000000000000000000001"
    );

    private static final Tuple RETURN_VALS = Tuple.of(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    @Test
    public void testLenient() throws Throwable {
        final byte[] lenientBytes = Strings.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
              + "00000000000000000000000000000000000000000000000000000000000000a3"
              + "0000000000000000000000000000000000000000000000000000000000000000"
              + "0000000000000000000000000000000000000000000000000000000000000000"
              + "0000000000000000000000000000000000000000000000000000000000000000000000"
              + "0000000000000000000000000000000000000000000000000000000000000004"
              + "7730307400000000000000000000000000000000000000000000000000000000");

        assertEquals(RETURN_VALS, FUNCTION.decodeReturn(lenientBytes));
        assertEquals(RETURN_VALS, FUNCTION.decodeReturn(ByteBuffer.wrap(lenientBytes)));

        final byte[] tooSmallOffset = Strings.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
              + "000000000000000000000000000000000000000000000000000000000000003f"
              + "0000000000000000000000000000000000000000000000000000000000000004"
              + "7730307400000000000000000000000000000000000000000000000000000000");

        assertThrown(IllegalArgumentException.class, "illegal backwards jump: (0+63=63)<64", () -> FUNCTION.decodeReturn(tooSmallOffset));
    }

    @Test
    public void testUint() {
        Function foo = new Function("foo()", "(uint8)");
        int val = (int) (Math.pow(2, 8)) - 1;
        byte[] _byte = Strings.decode("00000000000000000000000000000000000000000000000000000000000000FF");

        assertEquals(val, (int) foo.decodeReturn(_byte, 0));
        int i = foo.decodeSingletonReturn(_byte);
        assertEquals(val, i);

        byte[] _int_ = Strings.decode("000000000000000000000000000000000000000000000000000000000000FFFF");
        assertEquals((int) (Math.pow(2, 16)) - 1, (int) new Function("()", "(uint16)").decodeSingletonReturn(_int_));

        byte[] _long = Strings.decode("00000000000000000000000000000000000000000000000000000000FFFFFFFF");
        assertEquals((long) (Math.pow(2, 32)) - 1, (long) new Function("()", "(uint32)").decodeSingletonReturn(_long));

        byte[] _160_ = Strings.decode("000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        assertEquals(new Address(BigInteger.valueOf(2L).pow(160).subtract(BigInteger.ONE)), new Function("()", "(address)").decodeSingletonReturn(_160_));

        Function foo2 = new Function("()", "(uint)");
        byte[] _big_ = Strings.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        BigInteger expected = BigInteger.valueOf(2L).pow(256).subtract(BigInteger.ONE);
        BigInteger bi = ((UnitType<?>) foo2.getOutputs().get(0)).maxValue();
        assertEquals(expected, bi);
        assertEquals(expected, foo2.decodeReturn(_big_, 0));
        BigInteger n = foo2.decodeSingletonReturn(_big_);
        assertEquals(expected, n);
    }

    @Test
    public void testDoSLimit() throws Throwable {
        final long _2_17 = (long) Math.pow(2, 17);
        final String hex17 = Long.toHexString(_2_17);
        final String hex17min1 = Strings.encode(Integers.toBytes(_2_17 - 1));
        System.out.println(hex17 + " " + hex17min1);

        final long _2_31 = (long) Math.pow(2.0, 31.0);
        final String hex31 = Long.toHexString(_2_31);
        final long _2_31min1 = _2_31 - 1;
        final String hex31min1 = Long.toHexString(_2_31min1);
        System.out.println(hex31 + " " + hex31min1);

        final String bigLength =
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "000000000000000000000000000000000000000000000000000000000001ffff" +
                "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                BufferUnderflowException.class,
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigLength))
        );
        final String tooBigLength =
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000020000" +
                "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 18 > 17",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigLength))
        );

        final String bigOffset =
                "000000000000000000000000000000000000000000000000000000007FFFFFFF" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "newPosition > limit: (2147483647 > 96)",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigOffset))
        );
        final String tooBigOffset =
                "0000000000000000000000000000000000000000000000000000000080000000" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 32 > 31",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigOffset))
        );

        final String valid =
                "0000000000000000000000000000000000000000000000000000000000000020" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "aa00000000000000000000000000000000000000000000000000000000000000";
        Function.parse("()", "(bytes)").decodeReturn(Strings.decode(valid));
    }

    @Test
    public void testDecode() throws Throwable {

        assertArrayEquals(RETURN_BYTES, FUNCTION.getOutputs().encode(RETURN_VALS).array());

        Tuple decoded = FUNCTION.decodeReturn(RETURN_BYTES);
        assertEquals(RETURN_VALS, decoded);

        decoded = FUNCTION.getOutputs().decode(RETURN_BYTES);
        assertEquals(RETURN_VALS, decoded);

        decoded = TupleType.parse(FUNCTION.getOutputs().toString()).decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(RETURN_VALS, decoded);

        TupleType tt = TupleType.parse("(ufixed,string)");
        decoded = tt.decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(RETURN_VALS, decoded);

        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_A));
        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_B));
        assertThrown(IllegalArgumentException.class, "malformed array: non-zero padding byte", () -> FUNCTION.decodeReturn(BAD_PADDING_C));
    }

    @Test
    public void testDynamicArrayEmptyTuples() {
        Tuple decoded = new Function("foo()", "(()[])").decodeReturn(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                                "0000000000000000000000000000000000000000000000000000000000000002"
                )
        );
        assertEquals(Tuple.singleton(new Tuple[] { Tuple.EMPTY, Tuple.EMPTY }), decoded);
    }

    @Test
    public void testBoolean() throws Throwable {
        TupleType tt = TupleType.parse("(bool)");

        String[] tooBig = new String[] {
                "0000000000000000000000000000000000000000000000000000000000000002",
                "8000000000000000000000000000000000000000000000000000000000000000",
        };
        String[] tooSmall = new String[] {
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"
        };
        String[] justRight = new String[] {
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000001"
        };

        TestUtils.assertThrown(IllegalArgumentException.class, "exceeds bit limit: 2 > 1", () -> tt.decode(Strings.decode(tooBig[0])));
        TestUtils.assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> tt.decode(Strings.decode(tooBig[1])));
        for (String hex : tooSmall) {
            TestUtils.assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> tt.decode(Strings.decode(hex)));
        }
        for (String hex : justRight) {
            tt.decode(Strings.decode(hex));
        }
    }

    @Test
    public void testCorruptBoolean() throws Throwable {
        Function f = new Function("baz(uint32,bool)");
        Tuple argsTuple = new Tuple(69L, true);
        ByteBuffer one = f.encodeCall(argsTuple);

        final byte[] array = one.array();

        System.out.println(Function.formatCall(array));

        array[array.length - 1] = 0;
        System.out.println(Function.formatCall(array));
        Tuple decoded = f.decodeCall(array);
        assertNotEquals(decoded, argsTuple);

        array[array.length - 32] = (byte) 0x80;
        System.out.println(Function.formatCall(array));
        assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;
        System.out.println(Function.formatCall(array));
        assertThrown(IllegalArgumentException.class, "signed value given for unsigned type", () -> f.decodeCall(array));
    }

    @Test
    public void testCorruptBooleanArray() throws Throwable {
        Function f = new Function("baz(bool[])");
        Tuple argsTuple = new Tuple((Object) new boolean[] { true });
        ByteBuffer one = f.encodeCall(argsTuple);

        final byte[] array = one.array();

//        System.out.println(Function.formatCall(array));

        array[array.length - 1] = 0;
        Tuple decoded = f.decodeCall(array);
        assertNotEquals(decoded, argsTuple);

        array[array.length - 1] = 2;
        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        array[array.length - 1] = -1;
        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        array[array.length - 32] = (byte) 0x80;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 68", () -> f.decodeCall(array));
    }

    @Test
    public void testStringArray() throws Throwable {
        final ArrayType<ArrayType<ByteType, String>, String[]> type = TypeFactory.create("string[]", "nam");
        final String[] array = new String[] { "Hello, world!", "world! Hello," };
        final ByteBuffer abi = ByteBuffer.wrap(
                Strings.decode(
                        "0000000000000000000000000000000000000000000000000000000000000002" +
                                "0000000000000000000000000000000000000000000000000000000000000040" +
                                "0000000000000000000000000000000000000000000000000000000000000080" +
                                "000000000000000000000000000000000000000000000000000000000000000d" +
                                "48656c6c6f2c20776f726c642100000000000000000000000000000000000000" +
                                "000000000000000000000000000000000000000000000000000000000000000d" +
                                "776f726c64212048656c6c6f2c00000000000000000000000000000000000000"
                )
        );

        assertArrayEquals(abi.array(), type.encode(array).array());

        {
            final ABIType<?>                  a = TypeFactory.create("string[]");
            final ABIType<Object>             b = TypeFactory.create("string[]");
            final ABIType<String[]>           c = TypeFactory.create("string[]");
            final ABIType<Object[]>           c_ = TypeFactory.create("string[]");
            assertThrown(
                    IllegalArgumentException.class,
                    "class mismatch: [Ljava.lang.Object; != [Ljava.lang.String; (string[] requires String[] but found Object[])",
                    () -> c_.encode(new Object[] { "" })
            );
            final ABIType<String[]>           d = TypeFactory.create("string[]");
            final ABIType<?>                  e = TypeFactory.create("string[]");
            final ABIType<? extends String[]> f = TypeFactory.create("string[]");

            final ArrayType<?, ?>        g = TypeFactory.create("string[]");
            final ArrayType<?, ?>        h = TypeFactory.create("string[]");
            final ArrayType<?, String[]> i = TypeFactory.create("string[]");
            final ArrayType<ArrayType<ByteType, String>, String[]> j = TypeFactory.create("string[]");
            final ABIType<? extends String[]> k = TypeFactory.create("string[]");

            final IntType l = TypeFactory.create("int16");
            final ArrayType<?, BigInteger[]> m = TypeFactory.create("int[]");
            final TupleType n = TypeFactory.create("(bool)");
            m.encode(new BigInteger[] {});

            TupleType.wrap(TypeFactory.create("int"), TypeFactory.create("bytes[7]"));
        }

        assertEquals("nam", type.getName());

        Object decoded0 = Function.parse("()", "(string[])").getOutputs().get(0).decode(abi, new byte[32]);
        assertArrayEquals(array, (Object[]) decoded0);

        assertArrayEquals(array, type.decode(abi.array()));
    }

    @Test
    public void testDecodeCallIndex() {
        Function f = new Function("()", "(int8,bool,string)");

        final ByteBuffer bb = f.getOutputs().encode(Tuple.of(127, true, "two"));
        final boolean b = f.decodeReturn(bb.array(), 1);
        assertTrue(b);

        final byte[] bigger = new byte[7 + bb.capacity()];
        Arrays.fill(bigger, (byte) 0xff);
        final ByteBuffer bb2 = ByteBuffer.wrap(bigger);
        bb2.position(7);
        bb2.put(bb.array());
        bb2.position(7);
        final boolean b2 = f.decodeReturn(bb2, 1);
        assertTrue(b2);

        final ByteBuffer bb4 = f.getOutputs().encode(Tuple.of(127, false, "two"));
        final boolean b3 = f.decodeReturn(bb4.array(), 1);
        assertFalse(b3);
    }

    @Test
    public void testDecodeCallIndices() throws Throwable {
        Function f = new Function("()", "(int8,bool,int8[],bool)");

        byte[] arr = f.getOutputs().encode(Tuple.of(1, true, new int[] { 3, 6 } , false)).array();
        Tuple t = f.decodeReturn(arr, 1, 3);
        assertThrown(NoSuchElementException.class, "not present because index was not specified for decoding: 0", () -> t.get(0));
        boolean one = t.get(1);
        assertTrue(one);
        assertThrown(NoSuchElementException.class, "not present because index was not specified for decoding: 2", () -> t.get(2));
        boolean three = t.get(3);
        assertFalse(three);
    }

    // TupleType.parse("(int,string,bool,int64)").encodeElements(BigInteger.valueOf(550L), "weow", true, -41L):
    private static final String TUPLE_HEX =
            "0000000000000000000000000000000000000000000000000000000000000226" +
            "0000000000000000000000000000000000000000000000000000000000000080" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd7" +
            "0000000000000000000000000000000000000000000000000000000000000004" +
            "77656f7700000000000000000000000000000000000000000000000000000000";

    @Test
    public void testTupleDecodeTypeInference() throws Throwable {
        TupleType tt = TupleType.parse("(int,string,bool,int64)");
        Object[] elements = { BigInteger.valueOf(550L), "weow", true, -41L };
        ByteBuffer bb = tt.encodeElements(elements);
        assertEquals(TUPLE_HEX, Strings.encode(bb));
        final BigInteger zero = tt.decode(bb, 0);
        final String one = tt.decode(bb, 1);
        final boolean two = tt.decode(bb, 2);
        final long three = tt.decode(bb, 3);
        Tuple t = tt.decode(bb, 2, 3);
        System.out.println("tuple: " + zero + " " + one + " " + two + " " + three + " " + t);
        assertEquals(two, t.get(2));
        long three2 = t.get(3);
        assertEquals(three, three2);

        ByteBuffer buffer = ByteBuffer.allocate(192);
        tt.encode(new Tuple(elements), buffer);
        assertThrown(BufferUnderflowException.class, buffer::get);

        ByteBuffer z = ByteBuffer.allocate(192);
        int pos = z.position();
        z.put(buffer);
        assertEquals(pos, z.position());
    }

    @Test
    public void testFunctionDecodeTypeInference() {
        Function f = Function.parse("f()", "(int,string,bool,int64)");
        ByteBuffer bb = f.getOutputs().encodeElements(BigInteger.valueOf(550L), "weow", true, -41L);
        assertEquals(TUPLE_HEX, Strings.encode(bb));
        final BigInteger zero = f.decodeReturn(bb, 0);
        final String one = f.decodeReturn(bb, 1);
        final boolean two = f.decodeReturn(bb, 2);
        final long three = f.decodeReturn(bb, 3);
        Tuple t = f.decodeReturn(bb, 0, 3);
        System.out.println("function: " + zero + " " + one + " " + two + " " + three + " " + t);
        assertEquals(zero, t.get(0));
        long three2 = t.get(3);
        assertEquals(three, three2);
    }

    @Test
    public void testBadIndices() throws Throwable {
        TupleType tt = TupleType.parse("(int,string,bool,int64)");
        ByteBuffer bb = ByteBuffer.wrap(FastHex.decode(TUPLE_HEX));

        assertThrown(IllegalArgumentException.class, "must specify at least one index", () -> tt.decode(bb, new int[0]));

        assertThrown(IllegalArgumentException.class, "negative index: -571", () -> tt.decode(bb, -571));
        assertThrown(IllegalArgumentException.class, "negative index: -1", () -> tt.decode(bb, -1));
        for (int i = 0; i < 4; i++) {
            tt.decode(bb, i);
        }
        assertThrown(IllegalArgumentException.class, "index 4 out of bounds for tuple type of length 4", () -> tt.decode(bb, 4));
        assertThrown(IllegalArgumentException.class, "index 64 out of bounds for tuple type of length 4", () -> tt.decode(bb, 64));

        Tuple t = tt.decode(bb, 1, 2);
        System.out.println(t);
        assertThrown(IllegalArgumentException.class, "index out of order: 0", () -> tt.decode(bb, 1, 2, 0));
        assertThrown(IllegalArgumentException.class, "index out of order: 1", () -> tt.decode(bb, 1, 1));
    }

    @Test
    public void testSingletonReturn() throws Throwable {
        assertThrown(IllegalArgumentException.class, "return type not a singleton: ()", () -> Function.parse("bar()", "()").decodeSingletonReturn(new byte[0]));
        assertThrown(IllegalArgumentException.class, "return type not a singleton: (decimal,uint256)", () -> Function.parse("bar()", "(decimal,uint)").decodeSingletonReturn(new byte[0]));
        Function bar = Function.parse("bar()", "(bool)");
        boolean b = bar.decodeSingletonReturn(Strings.decode("0000000000000000000000000000000000000000000000000000000000000001"));
        assertTrue(b);
    }

    @Test
    public void testDecodeEvent() {
        Event event = Event.fromJson("{\n" +
                "    \"anonymous\": false,\n" +
                "    \"inputs\": [\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"buyHash\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"sellHash\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"maker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"taker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"price\",\n" +
                "        \"type\": \"uint256\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"metadata\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"name\": \"OrdersMatched\",\n" +
                "    \"type\": \"event\"\n" +
                "  }");
        byte[][] topics = {
                FastHex.decode("c4109843e0b7d514e4c093114b863f8e7d8d9a458c372cd51bfe526b588006c9"),
                FastHex.decode("000000000000000000000000bbb677a94eda9660832e9944353dd6e814a45705"),
                FastHex.decode("000000000000000000000000bcead8896acb7a045c38287e433d896eefb40f6c"),
                FastHex.decode("0000000000000000000000000000000000000000000000000000000000000000")
        };
        byte[] data = FastHex.decode("00000000000000000000000000000000000000000000000000000000000000009b5de4f892fe73b139777ff15eb165f359a0ea9ea1c687f8e8dc5748249ca5f200000000000000000000000000000000000000000000000002386f26fc100000");
        Tuple result = event.decodeArgs(topics, data);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
                Strings.encode((byte[]) result.get(0)));
        assertEquals("9b5de4f892fe73b139777ff15eb165f359a0ea9ea1c687f8e8dc5748249ca5f2",
                Strings.encode((byte[]) result.get(1)));
        assertEquals("0xbbb677a94eda9660832e9944353dd6e814a45705", result.get(2).toString().toLowerCase());
        assertEquals("0xbcead8896acb7a045c38287e433d896eefb40f6c", result.get(3).toString().toLowerCase());
        assertEquals(new BigInteger("160000000000000000"), result.get(4));
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
                Strings.encode((byte[]) result.get(5)));
    }

    @Test
    public void testDecodeEventWithWrongSignatureHashShouldFail() throws Throwable {
        Event event = Event.fromJson("{\n" +
                "    \"anonymous\": false,\n" +
                "    \"inputs\": [\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"buyHash\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"sellHash\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"maker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"taker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"price\",\n" +
                "        \"type\": \"uint256\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"metadata\",\n" +
                "        \"type\": \"bytes32\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"name\": \"OrdersMatched\",\n" +
                "    \"type\": \"event\"\n" +
                "  }");
        byte[][] topics = {
                FastHex.decode("a4109843e0b7d514e4c093114b863f8e7d8d9a458c372cd51bfe526b588006d9"),
                FastHex.decode("000000000000000000000000bbb677a94eda9660832e9944353dd6e814a45705"),
                FastHex.decode("000000000000000000000000bcead8896acb7a045c38287e433d896eefb40f6c"),
                FastHex.decode("0000000000000000000000000000000000000000000000000000000000000000")
        };
        byte[] data = FastHex.decode("00000000000000000000000000000000000000000000000000000000000000009b5de4f892fe73b139777ff15eb165f359a0ea9ea1c687f8e8dc5748249ca5f200000000000000000000000000000000000000000000000002386f26fc100000");
        assertThrownMessageMatch(RuntimeException.class, Collections.singletonList("Decoded Event signature hash " +
                        "a4109843e0b7d514e4c093114b863f8e7d8d9a458c372cd51bfe526b588006d9 does not match the one from ABI " +
                        "c4109843e0b7d514e4c093114b863f8e7d8d9a458c372cd51bfe526b588006c9"),
                () -> event.decodeArgs(topics, data));
    }

    @Test
    public void testDecodeAnonymousEvent() {
        Event event = Event.fromJson("{\n" +
                "    \"anonymous\": true,\n" +
                "    \"inputs\": [\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"maker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": true,\n" +
                "        \"name\": \"taker\",\n" +
                "        \"type\": \"address\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"name\": \"TestEvent\",\n" +
                "    \"type\": \"event\"\n" +
                "  }");
        byte[][] topics = {
                FastHex.decode("000000000000000000000000bbb677a94eda9660832e9944353dd6e814a45705"),
                FastHex.decode("000000000000000000000000bcead8896acb7a045c38287e433d896eefb40f6c")
        };
        Tuple result = event.decodeArgs(topics, new byte[0]);
        assertEquals("0xbbb677a94eda9660832e9944353dd6e814a45705", result.get(0).toString().toLowerCase());
        assertEquals("0xbcead8896acb7a045c38287e433d896eefb40f6c", result.get(1).toString().toLowerCase());
    }

    @Test
    public void testDecodeEmptyTopicsEvent() {
        Event event = Event.fromJson("{\n" +
                "    \"anonymous\": true,\n" +
                "    \"inputs\": [\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"maker\",\n" +
                "        \"type\": \"address\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"indexed\": false,\n" +
                "        \"name\": \"taker\",\n" +
                "        \"type\": \"address\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"name\": \"TestEvent\",\n" +
                "    \"type\": \"event\"\n" +
                "  }");
        byte[] data = FastHex.decode("000000000000000000000000bbb677a94eda9660832e9944353dd6e814a45705000000000000000000000000bcead8896acb7a045c38287e433d896eefb40f6c");
        Tuple result = event.decodeArgs(new byte[0][0], data);
        assertEquals("0xbbb677a94eda9660832e9944353dd6e814a45705", result.get(0).toString().toLowerCase());
        assertEquals("0xbcead8896acb7a045c38287e433d896eefb40f6c", result.get(1).toString().toLowerCase());
    }

    @Test
    public void testDecodeIndexedDynamicType() {
        Event event = Event.fromJson("{\n" +
                "        \"anonymous\": false,\n" +
                "        \"inputs\": [\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint256[]\",\n" +
                "            \"name\": \"nums\",\n" +
                "            \"type\": \"uint256[]\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint8\",\n" +
                "            \"name\": \"random\",\n" +
                "            \"type\": \"uint8\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Stored\",\n" +
                "        \"type\": \"event\"\n" +
                "      }");
        IntType int8 = TypeFactory.create("int8");
        byte[][] topics = {
                FastHex.decode("d78fe195906f002940f4b32985f1daa40764f8481c05447b6751db32e70d744b"),
                FastHex.decode("392791df626408017a264f53fde61065d5a93a32b60171df9d8a46afdf82992d"),
                int8.encode(12).array()
        };
        Tuple result = event.decodeArgs(topics, new byte[0]);
        assertEquals("392791df626408017a264f53fde61065d5a93a32b60171df9d8a46afdf82992d", Strings.encode((byte[]) result.get(0)));
        assertEquals(12, (Integer) result.get(1));
    }

    @Test
    public void testDecodeArgsNullTopicShouldFail() {
        Event event = Event.fromJson("{\n" +
                "        \"anonymous\": false,\n" +
                "        \"inputs\": [\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint256[]\",\n" +
                "            \"name\": \"nums\",\n" +
                "            \"type\": \"uint256[]\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint8\",\n" +
                "            \"name\": \"random\",\n" +
                "            \"type\": \"uint8\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Stored\",\n" +
                "        \"type\": \"event\"\n" +
                "      }");
        assertThrows(NullPointerException.class, () -> {
            event.decodeArgs(null, new byte[0]);
        });
    }

    @Test
    public void testDecodeArgsNullDataShouldFail() {
        Event event = Event.fromJson("{\n" +
                "        \"anonymous\": false,\n" +
                "        \"inputs\": [\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint256[]\",\n" +
                "            \"name\": \"nums\",\n" +
                "            \"type\": \"uint256[]\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"indexed\": true,\n" +
                "            \"internalType\": \"uint8\",\n" +
                "            \"name\": \"random\",\n" +
                "            \"type\": \"uint8\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"name\": \"Stored\",\n" +
                "        \"type\": \"event\"\n" +
                "      }");
        assertThrows(NullPointerException.class, () -> {
            event.decodeArgs(new byte[0][0], null);
        });
    }
}
