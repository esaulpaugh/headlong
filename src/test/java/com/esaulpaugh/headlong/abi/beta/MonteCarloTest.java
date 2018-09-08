package com.esaulpaugh.headlong.abi.beta;

import org.junit.Test;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Random;

//
public class MonteCarloTest {

    private static final Long MASTER_SEED = null; // (long) (Math.sqrt(2.0) * Math.pow(10, 15));

    private static final int N = 10_000;

    private static long[] generateSeeds(long masterSeed) {
        Random r = new Random(masterSeed);
        long[] seeds = new long[N];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = r.nextLong();
        }
        return seeds;
    }

    @Test
    public void monteCarlo() throws ParseException {

        SecureRandom sr = new SecureRandom();

        final long masterSeed = MASTER_SEED != null
                ? MASTER_SEED
                : System.nanoTime() * (System.nanoTime() << 1) * (System.nanoTime() >> 1) * sr.nextLong();

        final long[]seeds = generateSeeds(masterSeed);

        StringBuilder log = new StringBuilder();

        int i = 0;
        String temp = null;
        MonteCarloTestCase testCase;
        boolean result;
        for(final long seed : seeds) {
            // "(8527343108833427504,4,9,4,4)"
            // "(5215733063408107969,2,3,4,4)"
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed); // "(-6307556721730084796,2,3,4,4)"
            try {
                testCase = new MonteCarloTestCase(params);
                temp = testCase.canonicalSignature;
                result = testCase.run();
                temp = null;
                log.append('#').append(i).append(result ? " PASSED: " : " FAILED: ").append(params.toString()).append("\t\t").append(testCase.canonicalSignature).append('\n');
                i++;
            } catch (Throwable t) {
                System.out.println(log.toString());
                sleep(5);
                System.err.println("#" + i + " failed for " + params.toString() + "\t\t" + temp);
                System.err.println("MASTER_SEED = " + masterSeed);
                throw t;
            }
        }

        System.out.println(log.toString());
        System.out.println("MASTER_SEED = " + masterSeed);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
