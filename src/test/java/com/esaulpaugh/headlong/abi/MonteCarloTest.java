/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MonteCarloTest {

    private static final int N = 400_000;

    @Test
    public void fuzzTest() throws InterruptedException {

        final long masterMasterSeed = TestUtils.getSeed(System.nanoTime()); // (long) (Math.sqrt(2.0) * Math.pow(10, 15));

        final int numProcessors = Runtime.getRuntime().availableProcessors();
        final int threadsLen = numProcessors - 1;
        final Thread[] threads = new Thread[threadsLen];
        final int workPerProcessor = N / numProcessors;
        int i = 0;
        while (i < threads.length) {
            (threads[i] = newThread(masterMasterSeed + i++, workPerProcessor)).start();
        }
        newThread(masterMasterSeed + i++, workPerProcessor).run();

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println((workPerProcessor * i) + " done, MASTER_MASTER_SEED = " + masterMasterSeed);
    }

    private static Thread newThread(long seed, int n) {
        return new Thread(() -> {
            try {
                doMonteCarlo(seed, n);
            } catch (ValidationException ve) {
                throw new RuntimeException(ve);
            }
        });
    }

    private static void doMonteCarlo(long masterSeed, int n) throws ValidationException {

        final long[] seeds = generateSeeds(masterSeed, n);

        StringBuilder log = new StringBuilder();

        int i = 0;
        String temp = null;
        MonteCarloTestCase testCase;
        for(final long seed : seeds) {
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed);
            try {
                testCase = new MonteCarloTestCase(params);
                temp = testCase.function.getCanonicalSignature();
                testCase.run();
                temp = null;
//                log.append('#')
//                        .append(i)
//                        .append(" PASSED: ")
//                        .append(params.toString())
//                        .append("\t\t")
//                        .append(testCase.function.getCanonicalSignature().substring(testCase.function.getCanonicalSignature().indexOf('('))) // print function params
//                        .append('\n');
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

        private static final int THRESHOLD = 10_000;

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
//                long startTime = System.nanoTime();
                try {
                    for (int j = 0; j < n; j++) {
                        this.testCase.run();
                    }
                } catch (ValidationException ve) {
                    throw new RuntimeException(ve);
                }
//                System.out.println(n + " " + (System.nanoTime() - startTime) / 1_000_000.0);
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

    @Test
    public void testThreadSafety() throws InterruptedException, TimeoutException {

        final MonteCarloTestCase one = newComplexTestCase();
        final MonteCarloTestCase two = newComplexTestCase();

        System.out.println(one.function.getCanonicalSignature());
        System.out.println(one.params);
        System.out.println(two.function.getCanonicalSignature());
        System.out.println(two.params);
        final MonteCarloTask task = new MonteCarloTask(one, 0, 308_011);

        final int numProcessors = Runtime.getRuntime().availableProcessors();

        Thread[] threads = new Thread[numProcessors];
        final int threadsLen = threads.length;
        for (int i = 0; i < threadsLen; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 500; j++) {
                        two.run();
                    }
                } catch (ValidationException ve) {
                    throw new RuntimeException(ve);
                }
            });
        }

        ForkJoinPool pool = new ForkJoinPool();

        final int len2 = threadsLen - 1;
        for (int i = 0; i < len2; i++) {
            threads[i].start();
        }

        pool.invoke(task);

        threads[len2].run();

        pool.shutdown();
        if(!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new TimeoutException("timeout");
        }

        for (int i = 0; i < len2; i++) {
            threads[i].join();
        }
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final MonteCarloTask original = new MonteCarloTask(newComplexTestCase(), 0, 1);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        new ObjectOutputStream(baos)
                .writeObject(original);

        final MonteCarloTask deserialized = (MonteCarloTask) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))
                .readObject();

        if(!deserialized.equals(original)) {
            throw new AssertionError("deserialization failure");
        }
        System.out.println("successful deserialization");
    }

    private static MonteCarloTestCase newComplexTestCase() {
        final long time = System.nanoTime();
        long seed = TestUtils.getSeed(time);
        final long origSeed = seed;
        final long seed2 = TestUtils.getSeed(time + 1);
        System.out.println("orig =  " + seed);
        System.out.println("seed2 = " + seed2);
        System.out.println("xor = " + Long.toHexString(seed ^ seed2));

        MonteCarloTestCase testCase;
        String sig;
        do {
            seed++;
            testCase = new MonteCarloTestCase(new MonteCarloTestCase.Params(seed));
            sig = testCase.function.getCanonicalSignature();
        } while (!sig.contains("string")
                || (!sig.contains("fixed") && !sig.contains("decimal"))
                || (!sig.contains("int") && !sig.contains("address"))
                || (!sig.contains("bytes") && !sig.contains("function"))
                || sig.indexOf('[') < 0
                || sig.indexOf(')') == sig.length() - 1
        );
        System.out.println("n = " + (seed - origSeed));
        return testCase;
    }

    private static long[] generateSeeds(long masterSeed, int n) {
        Random r = new Random(masterSeed);
        long[] seeds = new long[n];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = r.nextLong();
        }
        return seeds;
    }

    @Disabled("run if you need to generate random test cases")
    @Test
    public void printNewTestCases() throws ValidationException {
        final Gson ugly = new GsonBuilder().create();
        final JsonPrimitive version = new JsonPrimitive("1.4.4+commit.3ad2258");
        JsonArray array = new JsonArray();
        int i = 0;
        for(final long seed : generateSeeds(TestUtils.getSeed(System.nanoTime()), 250)) {
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed);
            MonteCarloTestCase testCase = new MonteCarloTestCase(params);
            array.add(testCase.toJsonElement(ugly, "headlong_" + i++, version));
        }
        System.out.println(JsonUtils.toPrettyPrint(array));
    }

    private static void sleep() {
        try {
            Thread.sleep(5L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
