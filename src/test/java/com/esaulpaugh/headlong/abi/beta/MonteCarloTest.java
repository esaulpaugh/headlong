package com.esaulpaugh.headlong.abi.beta;

import org.junit.Test;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Random;
import java.util.concurrent.RecursiveAction;

public class MonteCarloTest {

    private static final Long MASTER_SEED = (long) (Math.sqrt(2.0) * Math.pow(10, 15));

    private static final int N = 100_000;

    private static long[] generateSeeds(long masterSeed) { // (-2465594717398185362,4,4,4,4)		((int256),ufixed72x2,uint160)
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

            // (8952920882133644975,256,2,1,1)

            // (7042232989689500075,2000,2,1,1)
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed); // "(-6307556721730084796,2,3,4,4)"
            try {
                testCase = new MonteCarloTestCase(params);
                temp = testCase.function.canonicalSignature;
                result = testCase.run();
                temp = null;
                log.append('#').append(i).append(result ? " PASSED: " : " FAILED: ").append(params.toString()).append("\t\t").append(testCase.function.canonicalSignature).append('\n');
                i++;
            } catch (Throwable t) {
                System.out.println(log.toString());
                sleep();
                System.err.println("#" + i + " failed for " + params.toString() + "\t\t" + temp);
                System.err.println("MASTER_SEED = " + masterSeed);
                throw t;
            }
        }

//        System.out.println(log.toString());
        System.out.println("MASTER_SEED = " + masterSeed);
    }

    private static class MonteCarloTask extends RecursiveAction {

        private static final int THRESHOLD = 50_000;

        final MonteCarloTestCase testCase;
        private final int start;
        private final int end;

        MonteCarloTask(final MonteCarloTestCase testCase, int start, int end) {
            this.testCase = testCase;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
//            System.out.println("compute(" + start + ", " + end + ")");
            final int start = this.start;
            final int end = this.end;
            final int n = end - start;
            if(n < THRESHOLD) {
//                System.out.println("n = " + n);

                final MonteCarloTestCase tc = this.testCase;

//                long[] seeds = new long[n];
//                for (int i = 0; i < n; i++) {
//                    seeds[i] = System.nanoTime() * (System.nanoTime() << 1) * (System.nanoTime() >> 1);
//                }

//                try {
                    for (int j = 0; j < n; j++) {
                        tc.run();
//                        tc.runNewRandomArgs(); // new MonteCarloTestCase(new MonteCarloTestCase.Params(seeds[j]))
                    }
//                } catch (ParseException pe) {
//                    throw new RuntimeException(pe);
//                }

            } else {
                final int midpoint = start + (n / 2);
                invokeAll(
                        new MonteCarloTask(testCase, start, midpoint),
                        new MonteCarloTask(testCase, midpoint, end)
                );
            }
        }
    }

    private static final int TIMEOUT_SECONDS = 60;

//    @Test
//    public void threadedTest() throws ParseException {
//
////        System.out.println(params);
//
////        ForkJoinPool pool = ForkJoinPool.commonPool();
//
//        final long seed = System.nanoTime() * System.nanoTime() * System.nanoTime();
//        final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params("(5262708696611543287,4,4,4,4)"); //
//        final MonteCarloTestCase testCase = new MonteCarloTestCase(params);
//        System.out.println(testCase.function.getCanonicalSignature());
//        System.out.println(testCase.params);
//        ForkJoinTask<Void> task = new MonteCarloTask(testCase, 0, 8_000_000);
//
//        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
//        pool.invoke(task);
//        pool.awaitQuiescence(TIMEOUT_SECONDS, TimeUnit.SECONDS);
//        pool.shutdownNow();
//
////        Thread[] threads = new Thread[8];
////        final int len = threads.length;
////        for (int i = 0; i < len; i++) {
////            threads[i] = new Thread(() -> {
////                for (int j = 0; j < 5_000_000; j++) {
////                    testCase.run();
////                }
////            });
////        }
////
////        final int len2 = len - 1;
////        for (int i = 0; i < len2; i++) {
////            threads[i].start();
////        }
////        threads[len2].run();
////
////        for (int i = 0; i < len2; i++) {
////            threads[i].join();
////        }
//    }

    private static void sleep() {
        try {
            Thread.sleep(5L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
