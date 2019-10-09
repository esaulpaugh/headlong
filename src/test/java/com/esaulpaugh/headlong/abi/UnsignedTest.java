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
import com.esaulpaugh.headlong.abi.util.Integers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

public class UnsignedTest {

    @Test
    public void testInvalidUnsigned() throws Throwable {
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "signed value given for unsigned type",
                () -> TupleType.parse("(uint)").validate(Tuple.singleton(BigInteger.valueOf(-1)))
        );

        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "signed value given for unsigned type",
                () -> TupleType.parse("(uint48)").validate(Tuple.singleton(-1L))
        );
    }

    @Test
    public void testToUnsigned() {
        for (int i = 2; i < 63; i++) {
            final Integers.UintType type = new Integers.UintType(i);
            final long power = (long) Math.pow(2.0, i);
            for (long j = 0; j < 2; j++)
                Assertions.assertEquals(j, type.toUnsigned(j));
            for (long j = -2; j < 0; j++)
                Assertions.assertEquals(power + j, type.toUnsigned(j));
        }
        for (int i = 2; i < 384; i++) {
            final Integers.UintType type = new Integers.UintType(i);
            for (long j = 0; j < 2; j++) {
                BigInteger bigJ = BigInteger.valueOf(j);
                Assertions.assertEquals(bigJ, type.toUnsigned(bigJ));
            }
            final BigInteger power = BigInteger.valueOf(2L).pow(i);
            for (long j = -2; j < 0; j++) {
                Assertions.assertEquals(
                        power.add(BigInteger.valueOf(j)),
                        type.toUnsigned(BigInteger.valueOf(j))
                );
            }
        }
    }

    @Test
    public void testToSigned() {
        for (int i = 2; i < 63; i++) {
            final Integers.UintType type = new Integers.UintType(i);
            final long power = (long) Math.pow(2.0, i - 1);
            for (long j = power - 2; j < power; j++) {
                Assertions.assertEquals(j, type.toSigned(j));
            }
            final long negativePower = -power;
            for (long j = power; j < power + 2; j++) {
                long expected = negativePower + (j - power);
                long actual = type.toSigned(j);
                Assertions.assertEquals(expected, actual);
            }
        }
        final BigInteger two = BigInteger.valueOf(2L);
        for (int i = 2; i < 384; i++) {
            final Integers.UintType type = new Integers.UintType(i);
            final BigInteger power = two.pow(i - 1);
            for (BigInteger j = power.subtract(two); j.compareTo(power) < 0; j = j.add(BigInteger.ONE)) {
//                System.out.println("i=" + i + ", j=" + j + ": " + expected + " == " + actual);
                Assertions.assertEquals(j, type.toSigned(j));
            }
            final BigInteger lim = power.add(BigInteger.valueOf(2L));
            final BigInteger negativePower = power.negate();
            for (BigInteger j = power; j.compareTo(lim) < 0; j = j.add(BigInteger.ONE)) {
                BigInteger expected = negativePower.add(j.subtract(power));
                BigInteger actual = type.toSigned(j);
                Assertions.assertEquals(expected, actual);
            }
        }
    }

    @Test
    public void testUintTypeSymmetry() {
        Random r = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        Integers.UintType type = new Integers.UintType(64);
        final long mask = (long) (Math.pow(2, 64) - 1.0d);
        for (int j = 0; j < 1_000; j++) {
            long x = pickRandom(r);
            x &= mask;
            Assertions.assertEquals(x, type.toSigned(type.toUnsigned(x)));
            Assertions.assertEquals(x, type.toUnsigned(type.toSigned(x)));
            BigInteger bigIntX = BigInteger.valueOf(x);
            Assertions.assertEquals(bigIntX, type.toUnsigned(type.toSigned(bigIntX)));
        }
        for (int j = 0; j < 1_000; j++) {
            BigInteger x = BigInteger.valueOf(pickRandom(r));
            Assertions.assertEquals(x, type.toSigned(type.toUnsigned(x)));
            x = x.abs();
            Assertions.assertEquals(x, type.toUnsigned(type.toSigned(x)));
        }
    }

    private static long pickRandom(Random r) {
        if(r.nextBoolean()) {
            return r.nextLong();
        }
        if(r.nextBoolean()) {
            return r.nextInt();
        }
        if(r.nextBoolean()) {
            return r.nextInt(Short.MAX_VALUE);
        }
        long x = r.nextInt();
        return r.nextBoolean() ? x : -x;
    }
}
