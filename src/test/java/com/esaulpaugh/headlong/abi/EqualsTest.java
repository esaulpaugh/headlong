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
import com.esaulpaugh.headlong.util.WrappedKeccak;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EqualsTest {

    @Test
    public void testEquals() {

        final MonteCarloTestCase.Limits limits = new MonteCarloTestCase.Limits(3, 3, 3, 3);
        final Random r = new Random();
        final Keccak k = new Keccak(256);

        int maxIters = -1;
        int i = 0;
        int n = 0;
        do {

            final MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong(), limits, r, k);

            final String canonical = mctc.function.getCanonicalSignature();
            if (mctc.rawSignature().equals(canonical)) {
                i++;
                continue;
            }
            if (i > maxIters) {
                maxIters = i;
            }
            i = 0;

            final Function a = mctc.function;
            final Function b = new Function(canonical);

//            System.out.println(raw);

            boolean equals = recursiveEquals(a.getInputs(), b.getInputs());

//            System.out.println(equals);

            assertTrue(equals);

            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(a.toJson(false), b.toJson(false));
            assertEquals(a.toString(), b.toString());

            assertNotSame(a.getInputs().canonicalType, b.getInputs().canonicalType);

            assertEquals(a, b);

            n++;
        } while (n < 100);

        System.out.println("n = " + n + ", maxIters = " + maxIters);

        assertSame(TupleType.parse("(uint)").get(0).getCanonicalType(), TupleType.parse("(uint)").get(0).getCanonicalType());
        assertNotSame(Function.parse("(uint)").getInputs().getCanonicalType(), Function.parse("(uint)").getInputs().getCanonicalType());
    }

    private static boolean recursiveEquals(TupleType<?> tt, Object o) {
        if (tt == o) return true;
        if (o == null || tt.getClass() != o.getClass()) return false;
        if (!tt.equals(o)) return false;
        TupleType<?> tupleType = (TupleType<?>) o;
        return Arrays.equals(tt.elementTypes, tupleType.elementTypes);
    }

    @Test
    public void complexFunctionTest() {
        final String sig = "(function[2][][],bytes24,string[0][0],address[],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))";
        final Function f = new Function(sig);
        final String canonical = f.getCanonicalSignature();
        assertEquals(sig.replace("(uint)", "(uint256)"), canonical);

        final WrappedKeccak wk = new WrappedKeccak(256);
        final byte[] digest = wk.digest(Strings.decode(canonical, Strings.ASCII));
        assertArrayEquals(Arrays.copyOfRange(digest, 0, 4), f.selector());

        final byte[] func = TestUtils.randomBytes(24);

//                       10000000000000000000000000000000000000000
//                        FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
        final String addressHex = "ff00ee01dd02cc03cafebabe9906880777086609";
        assertEquals(TypeFactory.ADDRESS_BIT_LEN, addressHex.length() * FastHex.BITS_PER_CHAR);
        final String checksumAddress = Address.toChecksumAddress("0x" + addressHex);
        assertEquals("0xFF00eE01dd02cC03cafEBAbe9906880777086609", checksumAddress);
        assertEquals(addressHex, checksumAddress.replace("0x", "").toLowerCase(Locale.ENGLISH));
        final Address addr = Address.wrap(checksumAddress);
        assertEquals(addressHex, addr.value().toString(16));

        final Object[] argsIn = new Object[] {
                new byte[][][][] { new byte[][][] { new byte[][] { func, func } } },
                func,
                new String[0][],
                new Address[] { addr },
                BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Byte.MAX_VALUE << 2)),
                Single.of(7),
                new Tuple[][][] { new Tuple[][] { new Tuple[] { Single.of(9), Single.of(-11) } } },
                new Tuple[] { Single.of(13), Single.of(-15) },
                new Tuple[] { Single.of(17), Single.of(-19) },
                Long.MAX_VALUE / 8_500_000,
                new Tuple[] { Single.of((long) 0x7e), Single.of((long) -0x7e) },
                Single.of(BigInteger.TEN)
        };

        final ByteBuffer abi = f.encodeCallWithArgs(argsIn);
        assertEquals(0, abi.position());

        assertTrue(Function.formatCall(abi.array()).contains("18       000000000000000000000000" + addressHex));

        final Tuple tupleOut = f.decodeCall(abi);

        assertTrue(Arrays.deepEquals(argsIn, tupleOut.elements));
    }

    @Test
    public void testUint32Array() {

        long[] unsigneds = new long[] {
                0x00000004L,
                0xFFFFFFF7L,
                0x11111111L,
                0xFF00FF00L,
                0x80808080L,
                0xFF00FF00L
        };

        Function foo = Function.parse("foo(uint32[6])");

        ByteBuffer bb = foo.encodeCall(Single.of(unsigneds));

        Tuple dec = foo.decodeCall(bb);

        long[] decoded = dec.get(0);

        assertArrayEquals(unsigneds, decoded);
    }

    @Test
    public void testUint64Array() {

        BigInteger[] unsigneds = new BigInteger[] {
                new BigInteger("0000000000000004", 16),
                new BigInteger("000000FFFFFFFFF7", 16),
                new BigInteger("1111111111111111", 16),
                new BigInteger("7F00FF00FF00FF00", 16),
                new BigInteger("8080808080808080", 16),
                new BigInteger("FF00FF00FF00FF00", 16),
        };

        Function foo = Function.parse("foo(uint64[6])");

        ByteBuffer bb = foo.encodeCall(Single.of(unsigneds));
        assertEquals(0, bb.position());

        Tuple dec = foo.decodeCall(bb);

        BigInteger[] decoded = dec.get(0);

        assertArrayEquals(unsigneds, decoded);
    }

    @Test
    public void testFlagsEquals() {
        final String typeString = "(bytes[4][],bytes30[],string[])";
        final TupleType tt_a = TupleType.parse(typeString);
        final TupleType tt_b = TupleType.parse(ABIType.FLAGS_NONE, typeString);
        final TupleType tt_c = TupleType.parse(ABIType.FLAG_LEGACY_DECODE, typeString);

        assertEquals(tt_a, tt_b);
        assertNotEquals(tt_a, tt_c);

        assertEquals(TypeFactory.create(typeString), TypeFactory.create(ABIType.FLAGS_NONE, typeString));
        assertNotEquals(TypeFactory.create(typeString), TypeFactory.create(ABIType.FLAG_LEGACY_DECODE, typeString));

        assertEquals(TypeFactory.create(typeString), TypeFactory.create(ABIType.FLAGS_NONE, typeString));
        assertNotEquals(TypeFactory.create(typeString), TypeFactory.create(ABIType.FLAG_LEGACY_DECODE, typeString));

        assertEquals(
                Function.parse(typeString),
                Function.parse(ABIType.FLAGS_NONE, typeString, "()")
        );
        assertNotEquals(
                Function.parse(typeString),
                Function.parse(ABIType.FLAG_LEGACY_DECODE, typeString, "()")
        );

        assertEquals(
                new Event("lo", false, tt_a, false, false, false),
                Event.create("lo", tt_b, false, false, false)
        );
        assertNotEquals(
                new Event("lo", false, tt_a, false, false, false),
                Event.create("lo", tt_c, false, false, false)
        );
    }
}
