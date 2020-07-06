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
package com.esaulpaugh.headlong;

import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.RecursiveAction;

public class TestUtils {

    public static Random seededRandom() {
        return new Random(TestUtils.getSeed(System.nanoTime()));
    }

    public static long getSeed(final long protoseed) {
        long c = protoseed * (System.nanoTime() << 1) * -System.nanoTime();
        c ^= c << 32;
        return c ^ (c >> 32);
    }

    public static long pickRandom(Random r) {
        long x = r.nextLong();
        switch (r.nextInt(Long.BYTES)) {
        case 0: break;
        case 1: x &= 0x00FFFFFF_FFFFFFFFL; break;
        case 2: x &= 0x0000FFFF_FFFFFFFFL; break;
        case 3: x &= 0x000000FF_FFFFFFFFL; break;
        case 4: x &= 0x00000000_FFFFFFFFL; break;
        case 5: x &= 0x00000000_00FFFFFFL; break;
        case 6: x &= 0x00000000_0000FFFFL; break;
        case 7: x &= 0x00000000_000000FFL; break;
        default: throw new Error();
        }
        return r.nextBoolean() ? -x : x;
    }

    public static void shuffle(Object[] arr, Random rand) {
        for (int i = arr.length; i > 0; ) {
            int idx = rand.nextInt(i);
            Object e = arr[idx];
            arr[idx] = arr[--i];
            arr[i] = e;
        }
    }

    public static void sort(int[] arr) {
        int j = 1;
        while(j < arr.length) {
            int i = j - 1, v = arr[j], v2;
            while(i >= 0 && v < (v2 = arr[i])) {
                arr[i-- + 1] = v2;
            }
            arr[i + 1] = v;
            j++;
        }
    }

    public static byte[] randomBytes(int n, Random r) {
        byte[] random = new byte[n];
        r.nextBytes(random);
        return random;
    }

    public static void printAndReset(StringBuilder sb) {
        System.out.println(sb.toString());
        sb.delete(0, sb.length());
    }

    public static String readFileResourceAsString(Class<?> clazz, String name) throws IOException {
        URL url = clazz.getClassLoader().getResource(name);
        if(url == null) {
            throw new IOException("url null");
        }
        return readFile(url.getFile()).toString("UTF-8");
    }

    private static ByteArrayOutputStream readFile(String filepath) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(new File(filepath))) {
            byte[] buffer = new byte[2048];
            int len;
            while ((len = fis.read(buffer)) >= 0) {
                result.write(buffer, 0, len);
            }
        }
        System.out.println("READ " + filepath);
        return result;
    }

    public static byte[] parsePrimitiveToBytes(JsonElement in) {
        try {
            return Integers.toBytes(parseLong(in));
        } catch (NumberFormatException | IllegalStateException e) {
            String inString = in.getAsString();
            if(inString.startsWith("#")) {
                return parseBigInteger(in).toByteArray();
            } else {
                return parseBytes(inString);
            }
        }
    }

    public static ArrayList<Object> parseArrayToBytesHierarchy(final JsonArray array) {
        ArrayList<Object> arrayList = new ArrayList<>();
        for (JsonElement element : array) {
            if(element.isJsonObject()) {
                arrayList.add(parseObject(element));
            } else if(element.isJsonArray()) {
                arrayList.add(parseArrayToBytesHierarchy(element.getAsJsonArray()));
            } else if(element.isJsonPrimitive()) {
                arrayList.add(parsePrimitiveToBytes(element));
            } else if(element.isJsonNull()) {
                throw new RuntimeException("null??");
            } else {
                throw new RuntimeException("?????");
            }
        }
        return arrayList;
    }

    public static long[] parseLongArray(final JsonArray array) {
        final int size = array.size();
        long[] longs = new long[size];
        for (int i = 0; i < size; i++) {
            JsonElement element = array.get(i);
            if(element.isJsonPrimitive()) {
                longs[i] = parseLong(element);
            } else if(element.isJsonNull()) {
                throw new RuntimeException("null??");
            } else {
                throw new RuntimeException("?????");
            }
        }
        return longs;
    }

    public static byte[] parseBytes(String utf8) {
        return Strings.decode(utf8, Strings.UTF_8);
    }

    public static byte[] parseBytesX(String string, int x) {
        if(string.length() == x) {
            byte[] bytesX = new byte[x];
            for (int i = 0; i < x; i++) {
                bytesX[i] = (byte) string.charAt(i);
            }
            return bytesX;
        } else {
            return Strings.decode(string);
        }
    }

    public static String parseString(JsonElement in) {
        return in.getAsString();
    }

    public static BigInteger parseBigInteger(JsonElement in) {
        String string = in.getAsString();
        return new BigInteger(string, 10);
    }

    public static BigInteger parseBigIntegerStringPoundSign(JsonElement in) {
        String string = in.getAsString();
        return new BigInteger(string.substring(1), 10);
    }

    public static long parseLong(JsonElement in) {
        return in.getAsLong();
    }

    public static Object parseObject(JsonElement in) {
        throw new UnsupportedOperationException("unsupported");
    }

    public static BigInteger parseAddress(JsonElement in) { // uint160
        String hex = "00" + in.getAsString().substring(2);
        byte[] bytes = Strings.decode(hex);
        return new BigInteger(bytes);
    }

    /** Asserts that the arguments are either both true or both false. */
    public static void assertMatching(boolean a, boolean b) {
        Assertions.assertFalse(a ^ b);
    }

    /** Asserts that exactly one of the arguments is true. */
    public static void assertNotMatching(boolean a, boolean b) {
        Assertions.assertTrue(a ^ b);
    }

    @FunctionalInterface
    public interface CustomRunnable {
        void run() throws Throwable;
    }

    public static void assertThrown(Class<? extends Throwable> clazz, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if (clazz.isInstance(t)) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }

    public static void assertThrown(Class<? extends Throwable> clazz, String substr, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if(clazz.isInstance(t)
                    && (t.getMessage() == null || t.getMessage().contains(substr))) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
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
            final long end = this.end;
            for (long lo = this.start; lo <= end; lo++) {
                int i = (int) lo;
                int len = Integers.putInt(i, four, 0);
                int r = Integers.getInt(four, 0, len, false);
                if(i != r) {
                    throw new AssertionError(i + " !=" + r);
                }
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

    public static int insertBytes(int n, byte[] b, int i, byte w, byte x, byte y, byte z) {
        if(n <= 4) {
            return insertBytes(n, b, i, (byte) 0, (byte) 0, (byte) 0, (byte) 0, w, x, y, z);
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
}
