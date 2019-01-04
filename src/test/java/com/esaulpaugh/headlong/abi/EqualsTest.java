package com.esaulpaugh.headlong.abi;

import org.junit.Assert;
import org.junit.Test;

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

}
