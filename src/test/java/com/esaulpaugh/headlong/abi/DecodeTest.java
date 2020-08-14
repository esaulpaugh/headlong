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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    private static final Tuple RETURN_ARGS = Tuple.of(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    private static final Tuple EXPECTED = new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

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

        assertEquals(RETURN_ARGS, FUNCTION.decodeReturn(lenientBytes));

        final byte[] tooSmallOffset = Strings.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
                        + "000000000000000000000000000000000000000000000000000000000000001f"
                        + "0000000000000000000000000000000000000000000000000000000000000004"
                        + "7730307400000000000000000000000000000000000000000000000000000000");

        assertThrown(IllegalArgumentException.class, "offset less than 0x20", () -> FUNCTION.decodeReturn(tooSmallOffset));
    }

    @Test
    public void testUint() {
        byte[] _byte = Strings.decode("00000000000000000000000000000000000000000000000000000000000000FF");
        assertEquals((int) (Math.pow(2, 8)) - 1, new Function("()", "(uint8)").decodeReturn(_byte).get(0));

        byte[] _int_ = Strings.decode("000000000000000000000000000000000000000000000000000000000000FFFF");
        assertEquals((int) (Math.pow(2, 16)) - 1, new Function("()", "(uint16)").decodeReturn(_int_).get(0));

        byte[] _long = Strings.decode("00000000000000000000000000000000000000000000000000000000FFFFFFFF");
        assertEquals((long) (Math.pow(2, 32)) - 1, new Function("()", "(uint32)").decodeReturn(_long).get(0));

        byte[] _160_ = Strings.decode("000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        assertEquals(BigInteger.valueOf(2L).pow(160).subtract(BigInteger.ONE), new Function("()", "(address)").decodeReturn(_160_).get(0));

        byte[] _big_ = Strings.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        assertEquals(BigInteger.valueOf(2L).pow(256).subtract(BigInteger.ONE), new Function("()", "(uint)").decodeReturn(_big_).get(0));
    }

    @Test
    public void testDoSLimit() throws Throwable {
        final String _2_17 = Strings.encode(Integers.toBytes((long) Math.pow(2, 17) - 1));
        System.out.println(_2_17);
        final String bigOffset =
                        "000000000000000000000000000000000000000000000000000000000001FFFF" +
                        "0000000000000000000000000000000000000000000000000000000000000001" +
                        "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "newPosition > limit: (131071 > 96)",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigOffset))
        );
        final String bigLength =
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "000000000000000000000000000000000000000000000000000000000001FFFF" +
                        "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "java.nio.BufferUnderflowException",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(bigLength))
        );

        final String tooBigOffset =
                        "000000000000000000000000000000000000000000000000000000000002FFFF" +
                        "0000000000000000000000000000000000000000000000000000000000000001" +
                        "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 18 > 17",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigOffset))
        );
        final String tooBigLength =
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "000000000000000000000000000000000000000000000000000000000002FFFF" +
                        "aa00000000000000000000000000000000000000000000000000000000000000";
        assertThrown(
                IllegalArgumentException.class,
                "exceeds bit limit: 18 > 17",
                () -> Function.parse("()", "(bytes)").decodeReturn(Strings.decode(tooBigLength))
        );

        final String valid =
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000001" +
                        "aa00000000000000000000000000000000000000000000000000000000000000";
        Function.parse("()", "(bytes)").decodeReturn(Strings.decode(valid));
    }

    @Test
    public void testDecode() throws Throwable {

        assertArrayEquals(FUNCTION.getOutputTypes().encode(RETURN_ARGS).array(), RETURN_BYTES);

        Tuple decoded = FUNCTION.decodeReturn(RETURN_BYTES);
        assertEquals(EXPECTED, decoded);

        decoded = FUNCTION.getOutputTypes().decode(RETURN_BYTES);
        assertEquals(EXPECTED, decoded);

        decoded = TupleType.parse(FUNCTION.getOutputTypes().toString()).decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(EXPECTED, decoded);

        decoded = TupleType.parseElements("ufixed,string").decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(EXPECTED, decoded);

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
        assertEquals(Tuple.of((Object) new Tuple[] { Tuple.EMPTY, Tuple.EMPTY }), decoded);
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

        for (String hex : tooBig) {
            TestUtils.assertThrown(IllegalArgumentException.class, "exceeds bit limit", () -> tt.decode(Strings.decode(hex)));
        }
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
        assertThrown(IllegalArgumentException.class, "exceeds bit limit", () -> f.decodeCall(array));

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

        array[array.length - 32] = (byte) 0x80;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 100", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;

        assertThrown(IllegalArgumentException.class, "illegal boolean value @ 100", () -> f.decodeCall(array));
    }
}
