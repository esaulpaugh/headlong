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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.MonteCarloTest;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.Utils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegersTest {

    @Test
    public void toBytes() {
        StringBuilder sb = new StringBuilder();
        for (byte i = -5; i < 5; i++)
            sb.append(Strings.encode(Integers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fb,fc,fd,fe,ff,,01,02,03,04,", sb.toString());
        TestUtils.printAndReset(sb);
        for (short i = -5; i < 5; i++)
            sb.append(Strings.encode(Integers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fffb,fffc,fffd,fffe,ffff,,01,02,03,04,", sb.toString());
        TestUtils.printAndReset(sb);
        for (int i = -5; i < 5; i++)
            sb.append(Strings.encode(Integers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fffffffb,fffffffc,fffffffd,fffffffe,ffffffff,,01,02,03,04,", sb.toString());
        TestUtils.printAndReset(sb);
        for (long i = -5; i < 5; i++)
            sb.append(Strings.encode(Integers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fffffffffffffffb,fffffffffffffffc,fffffffffffffffd,fffffffffffffffe,ffffffffffffffff,,01,02,03,04,", sb.toString());
        TestUtils.printAndReset(sb);
    }

    @Test
    public void putGetByte() throws DecodeException {
        byte[] one = new byte[1];
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int n = Integers.putByte(b, one, 0);
            byte r = Integers.getByte(one, 0, n);
            assertEquals(b, r);
        }
    }

    @Test
    public void putGetShort() throws DecodeException {
        byte[] two = new byte[2];
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int n = Integers.putShort(s, two, 0);
            short r = Integers.getShort(two, 0, n);
            assertEquals(s, r);
        }
    }

    @Test
    public void putGetInt() {
        new ForkJoinPool().invoke(new IntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void putGetLong() throws DecodeException {
        final Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        final byte[] eight = new byte[8];
        final long _2_24 = (long) Math.pow(2.0, Short.SIZE + Byte.SIZE);
        final long _2_16 = (long) Math.pow(2.0, Short.SIZE);
        final long _2_8 = (long) Math.pow(2.0, Byte.SIZE);
        testLongs((int) _2_8, new Supplier<Long>() {
            long lo = Byte.MIN_VALUE;
            @Override
            public Long get() {
                return lo++;
            }
        }, eight);
        testLongs(1000, () -> rand.nextInt() / _2_16, eight);
        testLongs(1000, () -> rand.nextInt() / _2_8, eight);
        testLongs(1000, () -> (long) rand.nextInt(), eight);
        testLongs(1000, () -> rand.nextLong() / _2_24, eight);
        testLongs(1000, () -> rand.nextLong() / _2_16, eight);
        testLongs(1000, () -> rand.nextLong() / _2_8, eight);
        testLongs(1000, rand::nextLong, eight);
    }

    private static void testLongs(int iterations, Supplier<Long> supplier, byte[] eight) throws DecodeException {
//        System.out.print(lo >= 0 ? Integers.len(lo) : BizarroIntegers.len(lo));
//        System.out.println(n);
        for (long i = 0; i < iterations; i++) {
            long lo = supplier.get();
            int n = Integers.putLong(lo, eight, 0);
            long r = Integers.getLong(eight, 0, n);
            if(lo != r) {
                throw new AssertionError(lo + "!= " + r);
            }
        }
    }

    @Test
    public void putGetBigInt() {
        byte[] dest = new byte[17];
        Arrays.fill(dest, (byte) -1);
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));

        final int lim = Short.MAX_VALUE * 10;
        for(int i = 0; i < lim; i++) {
            BigInteger big = BigInteger.valueOf(rand.nextLong()).multiply(BigInteger.valueOf(Long.MAX_VALUE));
            int n = Integers.putBigInt(big, dest, 0);
            BigInteger r = Integers.getBigInt(dest, 0, n);
            assertEquals(big, r);
        }
    }

    @Test
    public void lenByte() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int len = Integers.len(b);
            assertEquals(b == 0 ? 0 : 1, len);
        }
    }

    @Test
    public void lenShort() {
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int len = Integers.len(s);
            assertEquals(
                    s == 0
                            ? 0
                            : s > 0 && s < 256
                            ? 1
                            : 2,
                    len
            );
        }
    }

    @Test
    public void lenInt() {
        new ForkJoinPool().invoke(new LenIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void lenLong() {
        final Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        final int lim = (int) Math.pow(2.0, 15) - 1;
        for (int i = 0; i < lim; i++) {
            long lo = rand.nextLong();
            int expectedLen = lo < 0 || lo >= 72_057_594_037_927_936L ? 8
                    : lo >= 281_474_976_710_656L ? 7
                    : lo >= 1_099_511_627_776L ? 6
                    : lo >= 4_294_967_296L ? 5
                    : lo >= 16_777_216 ? 4
                    : lo >= 65_536 ? 3
                    : lo >= 256 ? 2
                    : lo != 0 ? 1
                    : 0;
            int len = Integers.len(lo);
            if(expectedLen != len) {
                throw new AssertionError(expectedLen + " != " + len);
            }
        }
    }

    @Test
    public void insertBytes() {
        byte[] ten = new byte[10];

        final byte a = 1, b = 11, c = 111, d = 9, e = 99, f = -1, g = -100, h = 64;

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(0, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(1, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, d, 0, 0, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(2, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, c, d, 0, 0, 0, 0, 0, 0, 0 }, ten);
        Arrays.fill(ten, (byte) 0);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(3, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, b, c, d, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(4, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, a, b, c, d, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(5, ten, 1, (byte) 0, (byte) 0, (byte) 0, a, b, c, d, e);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(6, ten, 1, (byte) 0, (byte) 0, a, b, c, d, e, f);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(7, ten, 1, (byte) 0, a, b, c, d, e, f, g);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, g, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        Utils.insertBytes(8, ten, 1, a, b, c, d, e, f, g, h);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, g, h, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        byte[] src = new byte[4];
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        rand.nextBytes(src);
        Utils.insertBytes(3, ten, ten.length - 3, (byte) 0, src[1], src[2], src[3]);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, src[1], src[2], src[3] }, ten);
    }

    public static class IntTask extends RecursiveAction {

        private static final int THRESHOLD = 250_000_000;

        protected final long start, end;

        protected IntTask(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            final long n = end - start;
            if (n > THRESHOLD) {
                long midpoint = start + (n / 2);
                invokeAll(
                        new IntTask(start, midpoint),
                        new IntTask(midpoint, end)
                );
            } else {
                doWork();
            }
        }

        protected void doWork() {
            byte[] four = new byte[4];
            try {
                final long end = this.end;
                for (long lo = this.start; lo <= end; lo++) {
                    int i = (int) lo;
                    int len = Integers.putInt(i, four, 0);
                    int r = Integers.getInt(four, 0, len);
                    if(i != r) {
                        throw new AssertionError(i + " !=" + r);
                    }
                }
            } catch (DecodeException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class LenIntTask extends IntTask {

        protected LenIntTask(long start, long end) {
            super(start, end);
        }

        protected int len(int val) {
            return Integers.len(val);
        }

        @Override
        protected void doWork() {
            final long end = this.end;
            for (long lo = this.start; lo <= end; lo++) {
                int i = (int) lo;
                int expectedLen = i < 0 || i >= 16_777_216 ? 4
                        : i >= 65_536 ? 3
                        : i >= 256 ? 2
                        : i != 0 ? 1
                        : 0;
                int len = LenIntTask.this.len(i); // len(int) can be overridden by subclasses
                if(expectedLen != len) {
                    throw new AssertionError(expectedLen + " != " + len);
                }
            }
        }
    }
}
