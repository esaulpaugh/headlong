package com.esaulpaugh.headlong.abi;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

public class MonteCarloTest {

    private Long masterSeed = null; // (long) (Math.sqrt(2.0) * Math.pow(10, 15));

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

        if(masterSeed == null) {
            masterSeed = seed(System.nanoTime()) * sr.nextLong();
        }

        final long[] seeds = generateSeeds(masterSeed);

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

        System.out.println(log.toString());
        System.out.println("MASTER_SEED = " + masterSeed);
    }

    private static class MonteCarloTask extends RecursiveAction {

        private static final long serialVersionUID = -4228469691073264266L;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MonteCarloTask that = (MonteCarloTask) o;
            return start == that.start &&
                    end == that.end &&
                    Objects.equals(testCase, that.testCase);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testCase, start, end);
        }
    }

    private static final int TIMEOUT_SECONDS = 60;

    @Test
    public void threadedTest() throws ParseException, InterruptedException, IOException, ClassNotFoundException {

//        System.out.println(params);

//        ForkJoinPool pool = ForkJoinPool.commonPool();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        final long time = System.nanoTime();
        long seed = seed(time);
        final long origSeed = seed;
        final long seed2 = seed(time + 1);
        System.out.println("orig =  " + seed);
        System.out.println("seed2 = " + seed2);
        System.out.println("xor = " + Long.toHexString(seed ^ seed2));

        MonteCarloTestCase temp;
        String sig;
        do {
            seed++;
            temp = new MonteCarloTestCase(new MonteCarloTestCase.Params(seed));
            sig = temp.function.canonicalSignature;
        } while (!sig.contains("string")
                || (!sig.contains("fixed") && !sig.contains("decimal"))
                || (!sig.contains("int") && !sig.contains("address"))
                || (!sig.contains("bytes") && !sig.contains("function"))
                || sig.indexOf('[') < 0
                || sig.indexOf(')') == sig.length() - 1
        );
        System.out.println("n = " + (seed - origSeed));

        final MonteCarloTestCase testCase = temp;

        System.out.println(testCase.function.getCanonicalSignature());
        System.out.println(testCase.params);
        final MonteCarloTask task = new MonteCarloTask(testCase, 0, 20_000);

        oos.writeObject(task);

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.invoke(task);
//        pool.awaitQuiescence(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ForkJoinTask.helpQuiesce();
        pool.shutdownNow();

        Thread[] threads = new Thread[8];
        final int len = threads.length;
        for (int i = 0; i < len; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 500; j++) {
                        testCase.run();
                    }
                }
            };
        }

        final int len2 = len - 1;
        for (int i = 0; i < len2; i++) {
            threads[i].start();
        }
        threads[len2].run();

        for (int i = 0; i < len2; i++) {
            threads[i].join();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        final MonteCarloTask deserialized = (MonteCarloTask) ois.readObject();

        boolean equal = deserialized.equals(task);
        int d_hash = deserialized.hashCode();
        int t_hash = task.hashCode();

        System.out.println("equal = " + equal);
        System.out.println("hashCodes: " + d_hash + " " + t_hash);

        System.out.println(deserialized.testCase.hashCode() + " == " + task.testCase.hashCode());
        System.out.println(deserialized.testCase.argsTuple.hashCode() + " == " + task.testCase.argsTuple.hashCode());


//        System.out.println(deserialized.testCase.params.hashCode() + " == " + task.testCase.params.hashCode());
//        System.out.println(deserialized.testCase.function.hashCode() + " == " + task.testCase.function.hashCode());
//        System.out.println(deserialized.testCase.function.paramTypes.hashCode() + " == " + task.testCase.function.paramTypes.hashCode());


        if(!equal || d_hash != t_hash) {
            throw new AssertionError("deserialization failure");
        }

        System.out.println("successful deserialization");
    }

    private static void sleep() {
        try {
            Thread.sleep(5L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static long seed(final long nanoTime) {
        final long a = System.nanoTime() << 1;
        final long b = -System.nanoTime() >> 1;
        final long c = nanoTime * a * b;
        final long d = c << 32;
        final long e = c >> 32;
        final long f = c ^ d;
        final long g = c ^ e;
        return f + g;
    }
}
