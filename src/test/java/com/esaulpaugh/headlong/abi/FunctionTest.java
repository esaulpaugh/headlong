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
    public void testFunctionValidation() throws Throwable {
        final String errNamed = "functions of this type must be unnamed";
        final String errNotNamed = "functions of this type must be named";
        final String errHasOutputs = "functions of this type cannot have outputs";
        final String errHasInputs = "functions of this type cannot have inputs";
        final TupleType inputs = TupleType.of("int"), outputs = TupleType.of("bool");
        final MessageDigest md = Function.newDefaultDigest();
        TestUtils.assertThrown(IllegalArgumentException.class, errHasOutputs, () -> new Function(Function.Type.CONSTRUCTOR, "foo()","(bool)", md));
        TestUtils.assertThrown(IllegalArgumentException.class, errHasOutputs, () -> new Function(Function.Type.FALLBACK, "foo()","(bool)", md));
        TestUtils.assertThrown(IllegalArgumentException.class, errNamed, () -> new Function(Function.Type.CONSTRUCTOR, "foo()","()", md));
        TestUtils.assertThrown(IllegalArgumentException.class, errNamed, () -> new Function(Function.Type.FALLBACK, "foo()","()", md));
        Function f = new Function(Function.Type.CONSTRUCTOR, "()","()", md);
        assertNull(f.getName());
        assertEquals(TupleType.EMPTY, f.getParamTypes());
        assertEquals(TupleType.EMPTY, f.getOutputTypes());
        f = new Function(Function.Type.FALLBACK, "()","()", md);
        assertEquals(Function.Type.FALLBACK, f.getType());
        assertNull(f.getName());
        assertEquals("Keccak-256", f.getHashAlgorithm());

        TestUtils.assertThrown(IllegalArgumentException.class, "functions of this type must be payable", () -> new Function(Function.Type.RECEIVE, "receive", inputs, outputs, null, md));
        TestUtils.assertThrown(IllegalArgumentException.class, errHasInputs, () -> new Function(Function.Type.RECEIVE, "receive", inputs, outputs, "payable", md));
        TestUtils.assertThrown(IllegalArgumentException.class, errHasOutputs, () -> new Function(Function.Type.RECEIVE, "receive", TupleType.EMPTY, outputs, "payable", md));
        f = new Function(Function.Type.RECEIVE, "receive", TupleType.EMPTY, TupleType.EMPTY, "payable", new WrappedKeccak(256));
        assertEquals("receive", f.getName());
        assertEquals("payable", f.getStateMutability());
        assertEquals("Keccak-256", f.getHashAlgorithm());

        TestUtils.assertThrown(IllegalArgumentException.class, errNotNamed, () -> new Function(Function.Type.FUNCTION, null, TupleType.EMPTY, TupleType.EMPTY, null, md));
        f = new Function(Function.Type.FUNCTION, "", TupleType.EMPTY, TupleType.EMPTY, null, md);
        assertEquals("", f.getName());
        assertNull(f.getStateMutability());
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
}
