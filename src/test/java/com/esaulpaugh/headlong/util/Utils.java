package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.util.exception.DecodeException;

import java.util.concurrent.RecursiveAction;

public class Utils {

    public static int insertBytes(int n, byte[] b, int i, byte w, byte x, byte y, byte z) {
        if(n <= 4) {
            return Utils.insertBytes(n, b, i, (byte) 0, (byte) 0, (byte) 0, (byte) 0, w, x, y, z);
        }
        throw new IllegalArgumentException("n must be <= 4");
    }

    /**
     * Inserts bytes into an array in the order they are given.
     * @param n     the number of bytes to insert
     * @param b     the buffer into which the bytes will be inserted
     * @param i     the index at which to insert
     * @param s     the lead byte if eight bytes are to be inserted
     * @param t     the lead byte if seven bytes are to be inserted
     * @param u     the lead byte if six bytes are to be inserted
     * @param v     the lead byte if five bytes are to be inserted
     * @param w     the lead byte if four bytes are to be inserted
     * @param x     the lead byte if three bytes are to be inserted
     * @param y     the lead byte if two bytes are to be inserted
     * @param z     the last byte
     * @return n    the number of bytes inserted
     */
    public static int insertBytes(int n, byte[] b, int i, byte s, byte t, byte u, byte v, byte w, byte x, byte y, byte z) {
        switch (n) { /* cases fall through */
        case 8: b[i++] = s;
        case 7: b[i++] = t;
        case 6: b[i++] = u;
        case 5: b[i++] = v;
        case 4: b[i++] = w;
        case 3: b[i++] = x;
        case 2: b[i++] = y;
        case 1: b[i] = z;
        case 0: return n;
        default: throw new IllegalArgumentException("n is out of range: " + n);
        }
    }

    public static class IntTask extends RecursiveAction {

        private static final int THRESHOLD = 250_000_000;

        protected final long start, end;

        public IntTask(long start, long end) {
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

        public LenIntTask(long start, long end) {
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
