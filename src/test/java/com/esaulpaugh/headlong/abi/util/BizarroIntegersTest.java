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
package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.MonteCarloTest;
import com.esaulpaugh.headlong.rlp.util.IntegersTest;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BizarroIntegersTest {

    @Test
    public void toBytes() {
        StringBuilder sb = new StringBuilder();
        for (byte i = -5; i < 5; i++)
            sb.append(Strings.encode(BizarroIntegers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fb,fc,fd,fe,,00,01,02,03,04,", sb.toString());
        TestUtils.printAndReset(sb);
        for (short i = -5; i < 5; i++)
            sb.append(Strings.encode(BizarroIntegers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fb,fc,fd,fe,,0000,0001,0002,0003,0004,", sb.toString());
        TestUtils.printAndReset(sb);
        for (int i = -5; i < 5; i++)
            sb.append(Strings.encode(BizarroIntegers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fb,fc,fd,fe,,00000000,00000001,00000002,00000003,00000004,", sb.toString());
        TestUtils.printAndReset(sb);
        for (long i = -5; i < 5; i++)
            sb.append(Strings.encode(BizarroIntegers.toBytes(i), Strings.HEX)).append(',');
        assertEquals("fb,fc,fd,fe,,0000000000000000,0000000000000001,0000000000000002,0000000000000003,0000000000000004,", sb.toString());
        TestUtils.printAndReset(sb);
    }

    @Test
    public void putGetByte() {
        byte[] one = new byte[1];
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int n = BizarroIntegers.putByte(b, one, 0);
            byte r = BizarroIntegers.getByte(one, 0, n);
            assertEquals(b, r);
        }
    }

    @Test
    public void putGetShort() {
        byte[] two = new byte[2];
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int n = BizarroIntegers.putShort(s, two, 0);
            short r = BizarroIntegers.getShort(two, 0, n);
            assertEquals(s, r);
        }
    }

    @Test
    public void putGetInt() {
        new ForkJoinPool().invoke(new BizzaroIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void putGetLong() {
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

    private static void testLongs(int iterations, Supplier<Long> supplier, byte[] eight) {
//        System.out.println(n);
//        System.out.print(lo >= 0 ? Integers.len(lo) : BizarroIntegers.len(lo));
        for (long i = 0; i < iterations; i++) {
            long lo = supplier.get();
            int n = BizarroIntegers.putLong(lo, eight, 0);
            long r = BizarroIntegers.getLong(eight, 0, n);
            if(lo != r) {
                throw new AssertionError(lo + "!= " + r);
            }
        }
    }

    @Test
    public void lenByte() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int len = BizarroIntegers.len(b);
            assertEquals(b == -1 ? 0 : 1, len);
        }
    }

    @Test
    public void lenShort() {
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int expectedLen = s >= 0 || s < -256 ? 2
                    : s != -1 ? 1
                    : 0;
            int len = BizarroIntegers.len(s);
            if(expectedLen != len) {
                throw new AssertionError(expectedLen + " != " + len);
            }
        }
    }

    @Test
    public void lenInt() {
        new ForkJoinPool().invoke(new BizzaroLenIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void lenLong() {
        final Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        final int lim = (int) Math.pow(2.0, 15) - 1;
        for (int i = 0; i < lim; i++) {
            long lo = rand.nextLong();
            int expectedLen = lo >= 0 || lo < -72_057_594_037_927_936L ? 8
                    : lo < -281_474_976_710_656L ? 7
                    : lo < -1_099_511_627_776L ? 6
                    : lo < -4_294_967_296L ? 5
                    : lo < -16_777_216 ? 4
                    : lo < -65_536 ? 3
                    : lo < -256 ? 2
                    : lo != -1 ? 1
                    : 0;
            int len = BizarroIntegers.len(lo);
            if(expectedLen != len) {
                throw new AssertionError(expectedLen + " != " + len);
            }
        }
    }

    private static final class BizzaroIntTask extends IntegersTest.IntTask {

        public BizzaroIntTask(int start, int end) {
            super(start, end);
        }

        @Override
        protected void doWork() {
            byte[] four = new byte[4];
            final long end = this.end;
            for (long lo = this.start; lo <= end; lo++) {
                int i = (int) lo;
                int len = BizarroIntegers.putInt(i, four, 0);
                int r = BizarroIntegers.getInt(four, 0, len);
                assertEquals(i, r);
            }
        }
    }

    private static final class BizzaroLenIntTask extends IntegersTest.LenIntTask {

        public BizzaroLenIntTask(int start, int end) {
            super(start, end);
        }

        @Override
        protected int len(int val) {
            return BizarroIntegers.len(val);
        }
    }
}
