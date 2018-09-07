package com.esaulpaugh.headlong.abi.beta;

import org.junit.Test;

import java.security.SecureRandom;
import java.text.ParseException;

public class MonteCarloTest {

    private static final int N = 90000;

    @Test
    public void monteCarlo() throws ParseException {

        SecureRandom sr = new SecureRandom();

        final long[] seeds = new long[N];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = System.nanoTime() * (System.nanoTime() << 1) * (System.nanoTime() >> 1) * sr.nextLong();
        }

//        StringBuilder log = new StringBuilder();

        int i = 0;
        for(final long seed : seeds) {
//            System.out.println("new seed " + seed);
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed); // TODO -1442214311993141792
            try {
                final MonteCarloTestCase testCase = new MonteCarloTestCase(params);

                System.out.print(i++ + ", " + testCase.canonicalSignature + " :: ");

                boolean result = testCase.run();

                System.out.println(result);

            } catch (Throwable t) {
                System.err.println("SEED = " + params.seed);
                throw t;
            }
        }

//        System.out.println(log.toString());
    }
}
