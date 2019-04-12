package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.abi.MonteCarloTest;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public class BizarroIntegersTest {

    @Test
    public void putGetByte() throws DecodeException {
        byte[] one = new byte[1];
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int n = BizarroIntegers.putByte(b, one, 0);
            byte r = BizarroIntegers.getByte(one, 0, n);
            Assert.assertEquals(b, r);
        }
    }

    @Test
    public void putGetShort() throws DecodeException {
        byte[] two = new byte[2];
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int n = BizarroIntegers.putShort(s, two, 0);
            short r = BizarroIntegers.getShort(two, 0, n);
            Assert.assertEquals(s, r);
        }
    }

    @Test
    public void putGetInt() {
        new ForkJoinPool().invoke(new BizzaroIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void putGetLong() throws DecodeException {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        byte[] eight = new byte[8];
        final long lim = Long.MAX_VALUE - (long) Math.pow(2.0, 24);
        for (long i = Long.MAX_VALUE; i >= lim; i--) {
            long lo = rand.nextBoolean() ? rand.nextLong() : rand.nextInt();
            int n = BizarroIntegers.putLong(lo, eight, 0);
            long r = BizarroIntegers.getLong(eight, 0, n);
            Assert.assertEquals(lo, r);
        }
    }

    @Test
    public void lenByte() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte b = (byte) i;
            int len = BizarroIntegers.len(b);
            Assert.assertEquals(b == -1 ? 0 : 1, len);
        }
    }

    @Test
    public void lenShort() {
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short s = (short) i;
            int len = BizarroIntegers.len(s);
            Assert.assertEquals(
                    s == -1
                            ? 0
                            : s < -1 && s >= -256
                            ? 1
                            : 2,
                    len
            );
        }
    }

    @Test
    public void lenInt() {
        new ForkJoinPool().invoke(new BizzaroLenIntTask(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void lenLong() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));

        for (int i = 0; i < Short.MAX_VALUE; i++) {
            long lo = rand.nextLong();
            int len = BizarroIntegers.len(lo);
            Assert.assertEquals(
                    lo == -1
                            ? 0
                            : lo < -1 && lo >= -256
                            ? 1
                            : lo < -256 && lo >= -65536
                            ? 2
                            : lo < -65536 && lo >= -16777216
                            ? 3
                            : lo < -16777216 && lo >= -4_294_967_296L
                            ? 4
                            : lo < -4_294_967_296L && lo >= -1_099_511_627_776L
                            ? 5
                            : lo < -1_099_511_627_776L && lo >= -281_474_976_710_656L
                            ? 6
                            : lo < -281_474_976_710_656L && lo >= -72_057_594_037_927_936L
                            ? 7
                            : 8,
                    len
            );
        }
    }

    private static final class BizzaroIntTask extends IntegersTest.IntTask {

        public BizzaroIntTask(int start, int end) {
            super(start, end);
        }

        @Override
        protected void doWork() {
            byte[] four = new byte[4];
            try {
                final long end = this.end;
                for (long lo = this.start; lo <= end; lo++) {
                    int i = (int) lo;
                    int len = BizarroIntegers.putInt(i, four, 0);
                    int r = BizarroIntegers.getInt(four, 0, len);
                    Assert.assertEquals(i, r);
                }
            } catch (DecodeException e) {
                throw new RuntimeException(e);
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
