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
import com.esaulpaugh.headlong.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EqualsTest {

    @Test
    public void testEquals() {

        Random r = TestUtils.seededRandom();

        int n = 0;
        do {

            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong());

            String canonical = mctc.function.getCanonicalSignature();
            if(mctc.rawSignature.equals(canonical)) {
                continue;
            }

            Function a = mctc.function;
            Function b = new Function(canonical);

//            System.out.println(raw);

            boolean equals = recursiveEquals(a.getParamTypes(), b.getParamTypes());

//            System.out.println(equals);

            assertTrue(equals);

            assertNotSame(a.getParamTypes().canonicalType, b.getParamTypes().canonicalType);

            assertEquals(a, b);

            n++;
        } while (n < 100);

        assertSame(TupleType.parse("(uint)").elementTypes[0].canonicalType, TupleType.parse("(uint)").elementTypes[0].canonicalType);
        assertNotSame(Function.parse("(uint)").getParamTypes().canonicalType, Function.parse("(uint)").getParamTypes().canonicalType);

        assertEquals(
                Function.parse("(bool)", new WrappedKeccak(256)),
                Function.parse("(bool)", new Keccak(256))
        );
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
}
