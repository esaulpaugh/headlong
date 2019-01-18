package com.esaulpaugh.headlong.abi;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.text.ParseException;

public class EqualsTest {

    @Test
    public void testEquals() throws ParseException {

        // "abc((int,uint)[1],((fixed,ufixed)))"

        int n = 0;
        do {

            MonteCarloTestCase mctc = new MonteCarloTestCase(MonteCarloTest.seed(System.nanoTime()));

            String raw = mctc.rawSignature;
            String canonical = mctc.function.canonicalSignature;
            if(raw.equals(canonical)) {
                continue;
            }

            Function a = new Function(raw);
            Function b = new Function(canonical);

//            System.out.println(raw);

            boolean equals = a.paramTypes.recursiveEquals(b.paramTypes)
                    && a.equals(b);

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
        assertThrown(IllegalArgumentException.class, "negative value for unsigned type", () -> f.decodeCall(array));
    }

    private interface CustomRunnable {
        void run() throws Throwable;
    }

    private static void assertThrown(Class<? extends Throwable> clazz, String substr, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if(clazz.isAssignableFrom(t.getClass()) && t.getMessage().contains(substr)) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }

}
