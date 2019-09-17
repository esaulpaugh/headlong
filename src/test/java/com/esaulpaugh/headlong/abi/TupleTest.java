package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

public class TupleTest {

    @Test
    public void fuzzNulls() throws Throwable {
        Random r = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        for (int i = 0; i < 1000; i++) {
            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong());
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
                BigInteger.valueOf(100L),
                BigDecimal.valueOf(120.997)
        };

        final int len = master.length;

        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));

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
                Assert.assertEquals(tuple.subtuple(i, j), new Tuple(elements));
//                for (int k = i; k < j; k++) {
//                    System.out.print((char) (k + 48));
//                }
//                System.out.println();
            }
        }
    }

    private static void shuffle(Object[] arr, Random rand) {
        for (int i = arr.length - 1; i > 0; i--) {
            int o = rand.nextInt(i + 1);
            Object x = arr[o];
            arr[o] = arr[i];
            arr[i] = x;
        }
    }
}
