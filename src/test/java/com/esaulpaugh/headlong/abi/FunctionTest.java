/*
   Copyright 2020 Evan Saulpaugh

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
import com.esaulpaugh.headlong.abi.util.WrappedKeccak;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FunctionTest {

    @Test
    public void testFixedLeadingZeroes() throws Throwable {
        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed8x011\"", () -> Function.parse("foo(fixed8x011)"));
        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed8x0011\"", () -> Function.parse("foo(fixed8x0011)"));

        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed08x1\"", () -> Function.parse("foo(fixed08x1)"));
        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed008x1\"", () -> Function.parse("foo(fixed008x1)"));

        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed08x079\"", () -> Function.parse("foo(fixed08x079)"));
        TestUtils.assertThrown(IllegalArgumentException.class, "@ index 0, unrecognized type: \"fixed008x0079\"", () -> Function.parse("foo(fixed008x0079)"));
    }

    @Test
    public void testFormatLength() {
        int len = 4 + 32;
        byte[] buffer = new byte[len + 103];
        for (int i = 0; i < 33; i++) {
            assertEquals(
                    "ID       00000000\n" +
                    "0        0000000000000000000000000000000000000000000000000000000000000000",
                    Function.formatCall(buffer, i, len)
            );
        }
    }

    @Test
    public void testFunctionValidation() throws Throwable {
        final Class<? extends Throwable> err = IllegalArgumentException.class;
        final TupleType inputs = TupleType.of("int");
        final TupleType outputs = TupleType.of("bool");
        final MessageDigest md = Function.newDefaultDigest();
        TestUtils.assertThrown(err, "type is \"constructor\"; functions of this type must define no outputs", () -> new Function(TypeEnum.CONSTRUCTOR, "foo", TupleType.EMPTY, TupleType.parse("(bool)"), null, md));
        TestUtils.assertThrown(err, "type is \"fallback\"; functions of this type must define no outputs", () -> new Function(TypeEnum.FALLBACK, "foo", TupleType.EMPTY, TupleType.parse("(bool)"), null, md));
        TestUtils.assertThrown(err, "type is \"constructor\"; functions of this type must not define name", () -> new Function(TypeEnum.CONSTRUCTOR, "foo", TupleType.EMPTY, TupleType.EMPTY, null, md));
        TestUtils.assertThrown(err, "type is \"fallback\"; functions of this type must not define name", () -> new Function(TypeEnum.FALLBACK, "foo", TupleType.EMPTY, TupleType.EMPTY, null, md));
        Function f = new Function(TypeEnum.CONSTRUCTOR, null, TupleType.EMPTY, TupleType.EMPTY, null, md);
        assertNull(f.getName());
        assertEquals(TupleType.EMPTY, f.getInputs());
        assertEquals(TupleType.EMPTY, f.getOutputs());
        f = new Function(TypeEnum.FALLBACK, null, TupleType.EMPTY, TupleType.EMPTY, null, md);
        assertEquals(TypeEnum.FALLBACK, f.getType());
        assertNull(f.getName());
        assertEquals("Keccak-256", f.getHashAlgorithm());

        TestUtils.assertThrown(err, "type is \"receive\"; functions of this type must define stateMutability as \"payable\"", () -> new Function(TypeEnum.RECEIVE, "receive", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "type is \"receive\"; functions of this type must define no inputs", () -> new Function(TypeEnum.RECEIVE, "receive", inputs, outputs, "payable", md));
        TestUtils.assertThrown(err, "type is \"receive\"; functions of this type must define no outputs", () -> new Function(TypeEnum.RECEIVE, "receive", TupleType.EMPTY, outputs, "payable", md));
        f = new Function(TypeEnum.RECEIVE, "receive", TupleType.EMPTY, TupleType.EMPTY, "payable", new WrappedKeccak(256));
        assertEquals("receive", f.getName());
        assertEquals("payable", f.getStateMutability());
        assertEquals("Keccak-256", f.getHashAlgorithm());

        TestUtils.assertThrown(err, "type is \"function\"; functions of this type must define name", () -> new Function(TypeEnum.FUNCTION, null, TupleType.EMPTY, TupleType.EMPTY, null, md));
        f = new Function(TypeEnum.FUNCTION, "", TupleType.EMPTY, TupleType.EMPTY, null, md);
        assertEquals("", f.getName());
        assertNull(f.getStateMutability());

        TestUtils.assertThrown(err, "illegal char 0x28 '(' @ index 0", () -> new Function(TypeEnum.FUNCTION, "(", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "illegal char 0x28 '(' @ index 1", () -> new Function(TypeEnum.FUNCTION, "a(", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "illegal char 0x28 '(' @ index 0", () -> new Function(TypeEnum.FUNCTION, "(b", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "illegal char 0x28 '(' @ index 1", () -> new Function(TypeEnum.FUNCTION, "c(d", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "illegal char 0x256 '\u0256' @ index 0", () -> new Function(TypeEnum.FUNCTION, "\u0256", inputs, outputs, null, md));
        new Function(TypeEnum.FUNCTION, "z", inputs, outputs, null, md);
        new Function(TypeEnum.FUNCTION, "", inputs, outputs, null, md);

        TestUtils.assertThrown(err, "unexpected type: \"event\"", () -> new Function(TypeEnum.EVENT, "foo", inputs, outputs, null, md));
        TestUtils.assertThrown(err, "unexpected type: \"error\"", () -> new Function(TypeEnum.ERROR, "foo", inputs, outputs, null, md));
    }

    @Test
    public void testNonCanonicalEquals() {

        testNonCanonicalEquals("foo(int256)",           "foo(int)");
        testNonCanonicalEquals("foo(int256[])",         "foo(int[])");
        testNonCanonicalEquals("foo(int256[31])",       "foo(int[31])");
        testNonCanonicalEquals("foo(int256[][])",       "foo(int[][])");
        testNonCanonicalEquals("foo(int256[][7])",      "foo(int[][7])");
        testNonCanonicalEquals("foo(int256[5][])",      "foo(int[5][])");
        testNonCanonicalEquals("foo(int256[100][100])", "foo(int[100][100])");

        testNonCanonicalEquals("foo(uint256)",          "foo(uint)");
        testNonCanonicalEquals("foo(uint256[])",        "foo(uint[])");
        testNonCanonicalEquals("foo(uint256[31])",      "foo(uint[31])");
        testNonCanonicalEquals("foo(uint256[][])",      "foo(uint[][])");
        testNonCanonicalEquals("foo(uint256[][7])",     "foo(uint[][7])");
        testNonCanonicalEquals("foo(uint256[5][])",     "foo(uint[5][])");
        testNonCanonicalEquals("foo(uint256[100][100])","foo(uint[100][100])");
    }

    private static void testNonCanonicalEquals(String canonical, String nonCanonical) {
        assertNotEquals(canonical, nonCanonical);
        Function canon = Function.parse(canonical);
        Function nonCanon = Function.parse(nonCanonical);
        assertEquals(canon, nonCanon);
        assertEquals(canon.getCanonicalSignature(), nonCanon.getCanonicalSignature());
    }

    @Test
    public void testFormatTupleType() {
        String f = Function.formatCall(new byte[] { 1, 1, 1, 1, 0x45, 0x13, 0x79, 0x03,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
                34, 33, 32, 31,
        }, 4, 36);
        System.out.println(f);
        assertEquals("ID       45137903\n0        2221201f2221201f2221201f2221201f2221201f2221201f2221201f2221201f", f);
    }
}
