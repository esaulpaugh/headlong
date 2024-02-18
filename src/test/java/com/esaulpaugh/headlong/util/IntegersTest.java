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
package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import static com.esaulpaugh.headlong.TestUtils.insertBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void putGetByte() {
        byte[] one = new byte[1];
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int n = Integers.putByte(b, one, 0);
            byte r = Integers.getByte(one, 0, n, false);
            assertEquals(b, r);
        }
    }

    @Test
    public void putGetShort() {
        byte[] two = new byte[2];
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int n = Integers.putShort(s, two, 0);
            short r = Integers.getShort(two, 0, n, false);
            assertEquals(s, r);
        }
    }

    @Test
    public void putGetInt() throws InterruptedException, TimeoutException {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            pool.invoke(new TestUtils.IntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
        } finally {
            TestUtils.requireNoTimeout(TestUtils.shutdownAwait(pool, 10L));
        }
    }

    @Test
    public void putGetLong() {
        Random rand = TestUtils.seededRandom();
        byte[] eight = new byte[8];
        for (long i = 0; i < 20_000; i++) {
            long lo = TestUtils.pickLong(rand);
            int n = Integers.putLong(lo, eight, 0);
            long r = Integers.getLong(eight, 0, n, false);
            if(lo != r) {
                throw new AssertionError(lo + "!= " + r);
            }
        }
    }

    @Test
    public void putGetBigInt() {
        byte[] dest = new byte[17];
        Arrays.fill(dest, (byte) -1);
        Random rand = TestUtils.seededRandom();
        for(int i = 0; i < 30_000; i++) {
            BigInteger big = TestUtils.wildBigInteger(rand, true, 136);
            int n = Integers.putBigInt(big, dest, 0);
            BigInteger r = Integers.getBigInt(dest, 0, n, false);
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
    public void lenInt() throws InterruptedException, TimeoutException {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            pool.invoke(new TestUtils.LenIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
        } finally {
            TestUtils.requireNoTimeout(TestUtils.shutdownAwait(pool, 10L));
        }
    }

    @Test
    public void lenLong() {
        Random rand = TestUtils.seededRandom();
        for (int i = 0; i < 30_000; i++) {
            long lo = TestUtils.pickLong(rand);
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

    @Disabled("tests TestUtils methods")
    @Test
    public void testInsertBytes() {
        byte[] ten = new byte[10];

        final byte a = 1, b = 11, c = 111, d = 9, e = 99, f = -1, g = -100, h = 64;

        Arrays.fill(ten, (byte) 0);
        insertBytes(0, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(1, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, d, 0, 0, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(2, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, c, d, 0, 0, 0, 0, 0, 0, 0 }, ten);
        Arrays.fill(ten, (byte) 0);

        Arrays.fill(ten, (byte) 0);
        insertBytes(3, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, b, c, d, 0, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(4, ten, 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, a, b, c, d);
        assertArrayEquals(new byte[] { 0, a, b, c, d, 0, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(5, ten, 1, (byte) 0, (byte) 0, (byte) 0, a, b, c, d, e);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, 0, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(6, ten, 1, (byte) 0, (byte) 0, a, b, c, d, e, f);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, 0, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(7, ten, 1, (byte) 0, a, b, c, d, e, f, g);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, g, 0, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        insertBytes(8, ten, 1, a, b, c, d, e, f, g, h);
        assertArrayEquals(new byte[] { 0, a, b, c, d, e, f, g, h, 0 }, ten);

        Arrays.fill(ten, (byte) 0);
        byte[] src = TestUtils.randomBytes(4);
        insertBytes(3, ten, ten.length - 3, (byte) 0, src[1], src[2], src[3]);
        assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, src[1], src[2], src[3] }, ten);
    }

    @Test
    public void testBigInteger() throws Throwable {
        long x = Long.MAX_VALUE;
        byte[] xBytes = Integers.toBytes(x);

        assertEquals("7fffffffffffffff", Strings.encode(xBytes));

        BigInteger positive = BigInteger.valueOf(x).add(BigInteger.ONE);

        assertEquals(positive, new BigInteger("8000000000000000", 16));

        byte[] bBytes = positive.toByteArray();

        TestUtils.assertThrown(IllegalArgumentException.class, () -> Integers.getBigInt(bBytes, 0, bBytes.length, false));

        assertEquals(positive, Integers.getBigInt(bBytes, 0, bBytes.length, true));
        assertEquals(positive, new BigInteger(Strings.encode(Integers.toBytesUnsigned(positive)), 16));

        String[] errs = new String[3 * 3 * 3];
        int e = 0;
        for (byte i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    try {
                        Integers.getBigInt(new byte[]{i}, j, k, false);
                    } catch (Throwable thr) {
                        errs[e] = thr.getClass().getSimpleName() + "|" + thr.getMessage();
                    }
                    e++;
                }
            }
        }

        final String aioobe = "ArrayIndexOutOfBoundsException|";
        final String illegal = "IllegalArgumentException|deserialized integers with leading zeroes are invalid; index: 0, len: ";
        final String negative = "NegativeArraySizeException|";

        int i = 0;
        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]);
        assertTrue(errs[i++].contains(aioobe));
        assertTrue(errs[i++].contains(negative));
        assertNull(errs[i++]);
        assertNull(errs[i++]);

        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]);
        assertTrue(errs[i++].contains(aioobe));
        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]); // 10++
        assertTrue(errs[i++].contains(aioobe));

        assertEquals(illegal + "-1", errs[i++]);
        assertNull(errs[i++]);
        assertEquals(illegal + "1", errs[i++]);
        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]);

        assertTrue(errs[i++].contains(aioobe)); // 17++
        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]);
        assertTrue(errs[i++].contains(aioobe));
        assertTrue(errs[i++].contains(negative));
        assertNull(errs[i++]);
        assertNull(errs[i++]);
        assertTrue(errs[i++].contains(aioobe));
        assertNull(errs[i++]);
        assertTrue(errs[i++].contains(aioobe));

        System.out.println(i + " / " + errs.length + " passed");
    }

    @Test
    public void testReturnValues() {
        testReturnValues(Integers::len, Integers::putLong);
    }

    public static void testReturnValues(ToIntFunction<Long> getLen, ToIntBiFunction<Long, ByteBuffer> put) {
        Random r = TestUtils.seededRandom();
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES + r.nextInt(35));
        final int offsetBound = 1 + bb.capacity() - Long.BYTES;
        for (int i = 0; i < 50; i++) {
            int offset = r.nextInt(offsetBound);
            bb.position(offset);
            final long val = TestUtils.pickLong(r);
            int len0 = getLen.applyAsInt(val);
            int len1 = put.applyAsInt(val, bb);
            int len2 = bb.position() - offset;
            assertEquals(len0, len1);
            assertEquals(len1, len2);
        }
    }
}
