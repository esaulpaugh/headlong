package com.esaulpaugh.headlong.abi;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Random;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;

public class EqualsTest {

    @Test
    public void testEquals() throws ParseException {

        Random r = new Random(MonteCarloTest.seed(System.nanoTime()));

        int n = 0;
        do {

            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong());

            String raw = mctc.rawSignature;
            String canonical = mctc.function.canonicalSignature;
            if(raw.equals(canonical)) {
                continue;
            }

            Function a = mctc.function;
            Function b = new Function(canonical);

//            System.out.println(raw);

            boolean equals = a.paramTypes.recursiveEquals(b.paramTypes);

//            System.out.println(equals);

            Assert.assertTrue(equals);

            n++;
        } while (n < 10_000);
    }

    @Test
    public void testBooleanNotEquals() throws Throwable {
        Function f = new Function("baz(uint32,bool)");
        Tuple argsTuple = new Tuple(69L, true);
        ByteBuffer one = f.encodeCall(argsTuple);

        final byte[] array = one.array();

        System.out.println(Function.formatCall(array));

        Tuple decoded;

        array[array.length - 1] = 0;
        System.out.println(Function.formatCall(array));
        decoded = f.decodeCall(array);
        Assert.assertNotEquals(decoded, argsTuple);

        array[array.length - 32] = (byte) 0x80;
        System.out.println(Function.formatCall(array));
        assertThrown(IllegalArgumentException.class, "exceeds bit limit", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;
        System.out.println(Function.formatCall(array));
        assertThrown(IllegalArgumentException.class, "negative value for boolean type", () -> f.decodeCall(array));
    }
}
