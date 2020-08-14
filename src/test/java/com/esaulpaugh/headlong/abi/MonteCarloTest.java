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
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.joemelsha.crypto.hash.Keccak;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.WriteAbortedException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MonteCarloTest {

    @Test
    public void repro() {
        Random instance = new Random();
        MessageDigest md = Function.newDefaultDigest();
        MonteCarloTestCase testCase = new MonteCarloTestCase(7706440032491971509L,3,3,3,3, instance, md);
        testCase.runAll(new Random());
    }

    private static final int N = 400_000;

    @Test
    public void gambleGamble() throws InterruptedException, AssertionError {

        final long masterSeed = TestUtils.getSeed(System.nanoTime()); // (long) (Math.sqrt(2.0) * Math.pow(10, 15));

        System.out.println("MASTER SEED: " + masterSeed + "L");

        final int numProcessors = Runtime.getRuntime().availableProcessors();
        final GambleGambleRunnable[] runnables = new GambleGambleRunnable[numProcessors];
        final int workPerProcessor = N / numProcessors;
        final ExecutorService pool = Executors.newFixedThreadPool(numProcessors);
        int i = 0;
        while (i < runnables.length) {
            pool.submit(runnables[i] = new GambleGambleRunnable(masterSeed + (i++), workPerProcessor));
        }
        pool.shutdown();
        pool.awaitTermination(300L, TimeUnit.SECONDS);
        for (GambleGambleRunnable runnable : runnables) {
            if(runnable.thrown != null) {
                throw new AssertionError(runnable.thrown);
            }
        }

        System.out.println((workPerProcessor * i) + " done");
    }

    private static void doMonteCarlo(long threadSeed, int n) {

        final StringBuilder log = new StringBuilder();

        final Random r = new Random(threadSeed);
        final Keccak k = new Keccak(256);

        final String desc = "thread-" + Thread.currentThread().getId() + " seed: " + threadSeed + "L";

        final Random instance = new Random();

        int i = 0;
        MonteCarloTestCase testCase = null;
        for(; i < n; i++) {
            try {
                testCase = new MonteCarloTestCase(r.nextLong(), 3, 3, 3, 3, r, k);
//                if(testCase.function.getCanonicalSignature().contains("int[")) throw new Error("canonicalization failed!");
                testCase.runAll(instance);
//                if(System.nanoTime() % 50_000_000 == 0) throw new Error("simulated random error");
//                log.append('#')
//                        .append(i)
//                        .append(" PASSED: ")
//                        .append("\t\t")
//                        .append(testCase.function.getCanonicalSignature().substring(testCase.function.getCanonicalSignature().indexOf('('))) // print function params
//                        .append('\n');
                i++;
            } catch (Throwable t) {
                System.out.println(log.toString());
                sleep();
                System.err.println("#" + i + " failed for " + testCase);
                System.err.println(desc);
                throw t;
            }
        }

        if(log.length() > 0) System.out.println(log.toString());
        System.out.println(desc);
    }

    private static class GambleGambleRunnable implements Runnable {

        private GambleGambleRunnable(long seed, int n) {
            this.seed = seed;
            this.n = n;
        }

        private final long seed;
        private final int n;
        private Throwable thrown = null;

        @Override
        public void run() {
            try {
                doMonteCarlo(seed, n);
            } catch (Throwable t) {
                thrown = t;
            }
        }
    }

    private static class MonteCarloTask extends RecursiveAction {

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
                for (int j = 0; j < n; j++) {
                    this.testCase.runStandard();
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

        final Random r = new Random();
        final Keccak k = new Keccak(256);
        final MonteCarloTestCase one = newComplexTestCase(r, k);
        final MonteCarloTestCase two = newComplexTestCase(r, k);

        System.out.println(one);
        System.out.println(two);
        final MonteCarloTask task = new MonteCarloTask(one, 0, 308_011);

        final int parallelism = Runtime.getRuntime().availableProcessors();
        final ExecutorService threadPool = Executors.newFixedThreadPool(parallelism);
        for (int i = 0; i < parallelism; i++) {
            threadPool.submit(() -> {
                for (int j = 0; j < 500; j++)
                    two.runStandard();
            });
        }

        ForkJoinPool fjPool = new ForkJoinPool();

        fjPool.invoke(task);

        for (int j = 0; j < 500; j++)
            two.runStandard();

        fjPool.shutdown();
        if(!fjPool.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new TimeoutException("not very Timely!!");
        }

        threadPool.shutdown();
        threadPool.awaitTermination(20L, TimeUnit.SECONDS);
    }

    @Test
    public void testNotSerializable() throws Throwable {
        final Random r = new Random();
        final Keccak k = new Keccak(256);
        final MonteCarloTask original = new MonteCarloTask(newComplexTestCase(r, k), 0, 1);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        TestUtils.assertThrown(NotSerializableException.class, "com.esaulpaugh.headlong.abi.MonteCarloTestCase", () -> new ObjectOutputStream(baos)
                .writeObject(original));

        TestUtils.assertThrown(WriteAbortedException.class,
                "writing aborted; java.io.NotSerializableException: com.esaulpaugh.headlong.abi.MonteCarloTestCase",
                () -> new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))
                .readObject()
        );
    }

    private static MonteCarloTestCase newComplexTestCase(Random r, Keccak k) {
        long seed = TestUtils.getSeed(System.nanoTime());
        final long origSeed = seed;

        MonteCarloTestCase testCase;
        String sig;
        do {
            seed++;
            testCase = new MonteCarloTestCase(seed, 4, 4, 2, 2, r, k);
            sig = testCase.function.getCanonicalSignature();
        } while (
                sig.endsWith("()")
                        || sig.indexOf('[') < 0
                        || (!sig.contains("int") && !sig.contains("address"))
                        || (!sig.contains("fixed") && !sig.contains("decimal"))
                        || (!sig.contains("bytes1") && !sig.contains("bytes2") && !sig.contains("bytes3") && !sig.contains("function"))
                        || (!sig.contains("string") && !sig.contains("bytes,") && !sig.contains("bytes)"))
        );
        System.out.println("n = " + (seed - origSeed));
        return testCase;
    }

    @Disabled("run if you need to generate random test cases")
    @Test
    public void printNewTestCases() {
        final Random r = new Random();
        final Keccak k = new Keccak(256);
        final Gson ugly = new GsonBuilder().create();
        final JsonPrimitive version = new JsonPrimitive("1.4.4+commit.3ad2258");
        final JsonArray array = new JsonArray();
        for(int i = 0; i < 250; i++) {
            MonteCarloTestCase testCase = new MonteCarloTestCase(r.nextLong(), 3, 3, 3, 3, r, k);
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

    @Test
    public void testSignatureCollision() {
        String a = "O()";
        String b = "QChn()";

        Assertions.assertNotEquals(a, b);

        Function fa = Function.parse(a);
        Function fb = Function.parse(b);

//        System.out.println(fa.selectorHex() + " == " + fb.selectorHex());

        Assertions.assertEquals("a0ea32de", fa.selectorHex());

        Assertions.assertEquals(fa.selectorHex(), fb.selectorHex());
        Assertions.assertNotEquals(fa, fb);
    }

    private static String generateASCIIString(final int len, Random r) {
        char[] chars = new char[len];
        for(int i = 0; i < len; i++) {
            char c;
            do {
                c = (char) (r.nextInt(160)); // 95) + 32
            } while (Character.isISOControl(c));
            if(c == '(') c = '_';
            chars[i] = c;
        }
        return new String(chars);
    }

    @Disabled("search for colliding signatures")
    @Test
    public void findSelectorCollisions() {

        final int n = 5_000_000;

        final Random r = TestUtils.seededRandom();

        final char[] lowercase = "abcdefghijklmnopqrstuvwxyz".toCharArray();
//        final char[] allCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

        final String paramsTupleStr = "()";

        for (char first : lowercase) {
            final HashMap<String, String> signatureMap = new HashMap<>(n / 20, 0.75f);
            final SortedSet<String> sorted = new TreeSet<>();
            for (int i = 0; i < n; i++) {
                final String str = generateName(first, lowercase, r) + paramsTupleStr;
                final Function foo = Function.parse(str);
                final String selectorHex = foo.selectorHex();
                final String signature = foo.getCanonicalSignature();
                final String name = signature.substring(0, signature.indexOf('('));
                final String prevSig = signatureMap.put(selectorHex, signature);
                if (prevSig != null
                        && prevSig.charAt(0) == signature.charAt(0)
                        && prevSig.charAt(1) == signature.charAt(1)
                        && !prevSig.equals(signature)) {
                    String prevName = prevSig.substring(0, prevSig.indexOf('('));
                    final String result = selectorHex + " " + (prevName.compareTo(name) < 0
                            ? prevName + "\t" + name
                            : name + "\t" + prevName);
                    sorted.add(result);
//                    System.err.println(result);
                    signatureMap.remove(selectorHex);
                }
                i++;
            }
//            System.out.println(signatureMap.size() + " " + sorted.size());
            MessageDigest keccak = Function.newDefaultDigest();
            for (String s : sorted) {
                String a = s.substring(s.lastIndexOf('\t') + 1) + paramsTupleStr;
                String b = s.substring(s.indexOf(' ') + 1, s.indexOf('\t')) + paramsTupleStr;
                String hashA = Strings.encode(keccak.digest(Strings.decode(a, Strings.UTF_8)));
                String hashB = Strings.encode(keccak.digest(Strings.decode(b, Strings.UTF_8)));
                System.out.println(a + " " + hashA + '\n' + b + " " + hashB);
            }
        }
    }

    private static String generateName(char first, char[] chars, Random r) {
        StringBuilder sb = new StringBuilder("" + first);
        appendNext(sb, chars, r);
        appendNext(sb, chars, r);
        appendNext(sb, chars, r);
        appendNext(sb, chars, r);
        return sb.toString();
    }

    private static void appendNext(StringBuilder sb, char[] chars, Random r) {
        sb.append(next(chars, r));
    }

    private static char next(char[] chars, Random r) {
        return chars[r.nextInt(chars.length)];
    }
}
