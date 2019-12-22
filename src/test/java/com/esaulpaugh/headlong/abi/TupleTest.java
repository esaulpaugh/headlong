package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TupleTest {

    private static final Object[] OBJECTS = new Object[] {
            new byte[0],
            new int[0],
            new short[0],
            new long[0],
            new boolean[0],
            new Throwable(),
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

    @Test
    public void testTypeSafety() throws Throwable {

        Random rand = new Random(TestUtils.getSeed(System.nanoTime()));

        for (int i = 0; i < 1000; i++) {

            rand.setSeed(i);

            MonteCarloTestCase testCase = new MonteCarloTestCase(i);

            Object[] elements = testCase.argsTuple.elements;

            final int idx = 0;
            if(elements.length > idx) {
                Object e = elements[idx];
                Object replacement = OBJECTS[rand.nextInt(OBJECTS.length)];
                if(e.getClass() != replacement.getClass()) {
                    elements[idx] = replacement;
                } else {
                    elements[idx] = new Object();
                }
                try {
                    TestUtils.assertThrown(ABIException.class, "not assignable to", () -> testCase.function.encodeCall(Tuple.of(elements)));
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
        Random r = new Random(TestUtils.getSeed(System.nanoTime()));
        for (int i = 0; i < 1000; i++) {
            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong());
            Tuple args = mctc.argsTuple;
            if(args.elements.length > 0) {
                int idx = r.nextInt(args.elements.length);
                replace(args.elements, idx);
                TestUtils.assertThrown(ABIException.class, "null", () -> mctc.function.encodeCall(args));
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
                BigInteger.valueOf(100L),
                BigDecimal.valueOf(120.997)
        };

        final int len = master.length;

        Random rand = new Random(TestUtils.getSeed(System.nanoTime()));

        shuffle(master, rand);

        Tuple tuple = new Tuple(master);

        int start = 1 + rand.nextInt(len);
        int end = rand.nextInt(start);
        TestUtils.assertThrown(NegativeArraySizeException.class, String.valueOf(end - start), () -> tuple.subtuple(start, end));

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

    private static void shuffle(Object[] arr, Random rand) {
        for (int i = arr.length; i > 0; ) {
            int o = rand.nextInt(i);
            Object x = arr[o];
            arr[o] = arr[--i];
            arr[i] = x;
        }
    }
}
