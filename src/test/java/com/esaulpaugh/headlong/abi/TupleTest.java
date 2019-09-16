package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

public class TupleTest {

    @Test
    public void testSubtuple() throws Throwable {

        Object[] master = new Object[] {
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
