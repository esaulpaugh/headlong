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
import com.joemelsha.crypto.hash.Keccak;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TupleTest {

    private static final Object[] OBJECTS = new Object[] {
            new byte[0],
            new int[0],
            new short[0],
            new long[0],
            new boolean[0],
            new Throwable() {},
            new BigInteger[0],
            new BigDecimal[0],
            true,
            5,
            9L,
            "aha",
            '\0',
            Tuple.EMPTY,
            0.1f,
            1.9d
    };

    @Disabled("meta test")
    @Test
    public void metaTest1() {
        Random r = ThreadLocalRandom.current();

        int bits = 24;

        final int pow = (int) Math.pow(2.0, bits);
        final int powMinus1 = pow - 1;
        System.out.println(Long.toHexString(powMinus1));

        System.out.println(pow);

        boolean[] bools = new boolean[pow];

        BigIntegerType type = new BigIntegerType("int" + bits, bits, false);
//        BooleanType type = new BooleanType();

        for (int i = 0; i < 1_579_919_999; i++) {
            bools[MonteCarloTestCase.generateBigInteger(r, type).intValue() & powMinus1] = true;
//            bools[(int) MonteCarloTestCase.generateLong(r, type) & powMinus1] = true;
        }

        int count = 0;
        for (int i = 0; i < pow; i++) {
            if(!bools[i]) {
                count++;
                System.err.println(i);
            }
        }

        System.out.println("missed " + count);
        System.out.println(pow - count + " / " + pow + " " + (1 - ((double) count / pow)));
    }

    @Test
    public void testTuple() {
        final Tuple emptyA = new Tuple();
        final Tuple emptyB = new Tuple((Object[]) new Object[] {});

        assertEquals(Tuple.EMPTY, emptyA);
        assertEquals(Tuple.EMPTY, emptyB);

        assertTrue(Tuple.EMPTY.isEmpty());
        assertTrue(emptyA.isEmpty());
        assertTrue(emptyB.isEmpty());

        assertFalse(new Tuple(0).isEmpty());
        assertFalse(new Tuple(false).isEmpty());
        assertFalse(new Tuple((Object) null).isEmpty());
    }

    @Test
    public void testTypeSafety() throws Throwable {

        final Random r = TestUtils.seededRandom();
        final Keccak k = new Keccak(256);

        for (int i = 0; i < 1000; i++) {

            r.setSeed(i);

            MonteCarloTestCase testCase = new MonteCarloTestCase(i, 3, 3, 3, 3, r, k);

            Object[] elements = testCase.argsTuple.elements;

            final int idx = 0;
            if(elements.length > idx) {
                Object e = elements[idx];
                Object replacement = OBJECTS[r.nextInt(OBJECTS.length)];
                if(e.getClass() != replacement.getClass()) {
                    elements[idx] = replacement;
                } else {
                    elements[idx] = new Object();
                }
                try {
                    TestUtils.assertThrown(IllegalArgumentException.class, " but found ", () -> testCase.function.encodeCall(Tuple.of(elements)));
                } catch (AssertionError ae) {
                    System.err.println(i);
                    ae.printStackTrace();
                    throw ae;
                }
            }
        }
    }

    @Test
    public void fuzzNulls() throws Throwable {
        final Random r = TestUtils.seededRandom();
        final Keccak k = new Keccak(256);
        for (int i = 0; i < 1000; i++) {
            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong(), 3, 3, 3, 3, r, k);
            Tuple args = mctc.argsTuple;
            if(args.elements.length > 0) {
                int idx = r.nextInt(args.elements.length);
                replace(args.elements, idx);
                TestUtils.assertThrown(IllegalArgumentException.class, "null", () -> mctc.function.encodeCall(args));
            }
        }
    }

    private static void replace(Object[] parent, int index) {
        Object element = parent[index];
        if(element instanceof Object[]) {
            Object[] eArr = (Object[]) element;
            if(eArr.length > 0) {
                Object inner = parent[index];
                if(inner instanceof Object[]) {
                    Object[] innerArr = (Object[]) inner;
                    if(innerArr.length > 0) {
                        innerArr[innerArr.length - 1] = null;
                        return;
                    }
                }
                eArr[0] = null;
                return;
            }
        }
        parent[index] = null;
    }

    @Test
    public void testSubtuple() throws Throwable {

        Object[] master = new Object[] {
                null,
                true,
                (short) 77,
                new Object[] {},
                new byte[0],
                9f,
                1.11d,
                'X',
                0b0,
                1,
                10L,
                BigInteger.valueOf(3405691582L),
                BigDecimal.valueOf(120.997)
        };

        final int len = master.length;

        Random rand = TestUtils.seededRandom();

        TestUtils.shuffle(master, rand);

        Tuple tuple = new Tuple(master);

        int start = 1 + rand.nextInt(len);
        int end = rand.nextInt(start);
        TestUtils.assertThrown(IllegalArgumentException.class, start + " > " + end, () -> tuple.subtuple(start, end));

        for (int i = 0; i <= len; i++) {
            for (int j = len; j >= i; j--) {
                final int n = j - i;
                Object[] elements = new Object[n];
                System.arraycopy(master, i, elements, 0, n);
                assertEquals(tuple.subtuple(i, j), new Tuple(elements));
//                for (int k = i; k < j; k++) {
//                    System.out.print((char) (k + 48));
//                }
//                System.out.println();
            }
        }
    }

    @Test
    public void testSubtubleType() throws Throwable {
        TupleType tt = TupleType.parse("(bytes3,uint16[],string)");

        assertEquals(TupleType.EMPTY, tt.subTupleType(false, false, false));
        assertEquals(TupleType.of("string"), tt.subTupleType(false, false, true));
        assertEquals(TupleType.of("uint16[]"), tt.subTupleType(false, true, false));
        assertEquals(TupleType.of("uint16[]", "string"), tt.subTupleType(false, true, true));
        assertEquals(TupleType.of("bytes3"), tt.subTupleType(true, false, false));
        assertEquals(TupleType.of("bytes3", "string"), tt.subTupleType(true, false, true));
        assertEquals(TupleType.of("bytes3", "uint16[]"), tt.subTupleType(true, true, false));
        assertEquals(TupleType.of("bytes3", "uint16[]", "string"), tt.subTupleType(true, true, true));

        TestUtils.assertThrown(IllegalArgumentException.class, "manifest.length != size()", () -> tt.subTupleType(true, true));
        TestUtils.assertThrown(IllegalArgumentException.class, "manifest.length != size()", () -> tt.subTupleType(false, false, false, false));

        assertEquals(TupleType.of("bytes3", "uint16[]", "string"), tt.subTupleTypeNegative(false, false, false));
        assertEquals(TupleType.of("bytes3", "uint16[]"), tt.subTupleTypeNegative(false, false, true));
        assertEquals(TupleType.of("bytes3", "string"), tt.subTupleTypeNegative(false, true, false));
        assertEquals(TupleType.of("bytes3"), tt.subTupleTypeNegative(false, true, true));
        assertEquals(TupleType.of("uint16[]", "string"), tt.subTupleTypeNegative(true, false, false));
        assertEquals(TupleType.of("uint16[]"), tt.subTupleTypeNegative(true, false, true));
        assertEquals(TupleType.of("string"), tt.subTupleTypeNegative(true, true, false));
        assertEquals(TupleType.EMPTY, tt.subTupleTypeNegative(true, true, true));

        TestUtils.assertThrown(IllegalArgumentException.class, "manifest.length != size()", () -> tt.subTupleType(new boolean[0]));
        TestUtils.assertThrown(IllegalArgumentException.class, "manifest.length != size()", () -> tt.subTupleType(new boolean[5]));
    }
}
