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
import com.esaulpaugh.headlong.abi.util.WrappedKeccak;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EqualsTest {

    @Test
    public void testEquals() {

        final Random r = TestUtils.seededRandom();
        final Keccak k = new Keccak(256);

        int maxIters = -1;
        int i = 0;
        int n = 0;
        do {

            final MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong(), 3, 3, 3, 3, r, k);

            final String canonical = mctc.function.getCanonicalSignature();
            if(mctc.rawSignature.equals(canonical)) {
                i++;
                continue;
            }
            if(i > maxIters) {
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

    private static boolean recursiveEquals(TupleType tt, Object o) {
        if (tt == o) return true;
        if (o == null || tt.getClass() != o.getClass()) return false;
        if (!tt.equals(o)) return false;
        TupleType tupleType = (TupleType) o;
        return Arrays.equals(tt.elementTypes, tupleType.elementTypes);
    }

    @Test
    public void complexFunctionTest() {
        Function f = new Function("(function[2][][],bytes24,string[0][0],address[],uint72,(uint8),(int16)[2][][1],(int24)[],(int32)[],uint40,(int48)[],(uint))");

        WrappedKeccak wk = new WrappedKeccak(256);
        byte[] digest = wk.digest(Strings.decode(f.getCanonicalSignature(), Strings.UTF_8));
        assertArrayEquals(Arrays.copyOfRange(digest, 0, 4), f.selector());

        byte[] func = TestUtils.randomBytes(24);

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
                new Tuple(7),
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } },
                new Tuple[] { new Tuple(13), new Tuple(-15) },
                new Tuple[] { new Tuple(17), new Tuple(-19) },
                Long.MAX_VALUE / 8_500_000,
                new Tuple[] { new Tuple((long) 0x7e), new Tuple((long) -0x7e) },
                new Tuple(BigInteger.TEN)
        };

        final ByteBuffer abi = f.encodeCallWithArgs(argsIn);

        assertTrue(Function.formatCall(abi.array()).contains("18       000000000000000000000000" + addressHex));

        final Tuple tupleOut = f.decodeCall((ByteBuffer) abi.flip());

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

        ByteBuffer bb = foo.encodeCall(Tuple.singleton(unsigneds));

        Tuple dec = foo.decodeCall((ByteBuffer) bb.flip());

        long[] decoded = (long[]) dec.get(0);

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

        ByteBuffer bb = foo.encodeCall(Tuple.singleton(unsigneds));

        Tuple dec = foo.decodeCall((ByteBuffer) bb.flip());

        BigInteger[] decoded = (BigInteger[]) dec.get(0);

        assertArrayEquals(unsigneds, decoded);
    }
}
