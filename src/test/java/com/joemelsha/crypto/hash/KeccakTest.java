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
package com.joemelsha.crypto.hash;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.WrappedKeccak;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

import static com.esaulpaugh.headlong.abi.Function.SELECTOR_LEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class KeccakTest {

    private static final byte[] PART_A = Strings.decode("abcdefghijklmnopqrstuvwxyz", Strings.ASCII);
    private static final byte[] PART_B = Strings.decode("ABCDEFG", Strings.ASCII);
    private static final byte[] WHOLE;

    static {
        WHOLE = new byte[PART_A.length + PART_B.length];
        System.arraycopy(PART_A, 0, WHOLE, 0, PART_A.length);
        System.arraycopy(PART_B, 0, WHOLE, PART_A.length, PART_B.length);
    }

    @Test
    public void testGetDigestLength() {
        assertEquals(32, new Keccak(256).getDigestLength());
        assertEquals(32, new WrappedKeccak(256).getDigestLength());

        assertEquals(36, new Keccak(288).getDigestLength());
        assertEquals(36, new WrappedKeccak(288).getDigestLength());

        assertEquals(64, new Keccak(512).getDigestLength());
    }

    @Test
    public void testMultiUpdate() {
        testMultiUpdate(128);
        testMultiUpdate(224);
        testMultiUpdate(256);
        testMultiUpdate(288);
        testMultiUpdate(384);
        testMultiUpdate(512);
    }

    private static void testMultiUpdate(final int bitLen) {
        final Keccak k = new Keccak(bitLen);

        k.reset();
        k.update(new byte[1]);
        k.update(new byte[7]);

        k.reset();
        k.update(WHOLE);
        byte[] k0 = k.digest();

        k.reset();
        k.update(PART_A);
        k.update(PART_B);
        byte[] k1 = k.digest();

        assertArrayEquals(k0, k1);
    }

    @Test
    public void testPartial() throws DigestException {
        Keccak keccak = new Keccak(256);

        byte[] x = TestUtils.randomBytes(7);

        byte[] end = Arrays.copyOfRange(x, 4, 7);

        keccak.reset();
        keccak.update(x);
        keccak.digest(x, 0, 4);

        assertArrayEquals(end, Arrays.copyOfRange(x, 4, 7));

        Arrays.fill(x, (byte) 0);

        ByteBuffer bb = ByteBuffer.wrap(x);

        byte[] arr = Arrays.copyOf(bb.array(), bb.capacity());

        keccak.reset();
        keccak.digest(bb, 4);

        byte[] arr2 = bb.array();

        assertNotEquals(Strings.encode(arr), Strings.encode(arr2));
        assertArrayEquals(Arrays.copyOfRange(arr, 4, 7), Arrays.copyOfRange(arr2, 4, 7));

        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Strings.encode(keccak.digest(new byte[0])));

        keccak.reset();

        byte[] digest = new byte[32];
        bb = ByteBuffer.wrap(digest);
        Arrays.fill(digest, (byte) 0xff);

        keccak.digest(bb);

        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Strings.encode(bb));
    }

    @Test
    public void testUpdateBits() {

        byte[] prefix = Strings.decode("Hello World", Strings.UTF_8);

        Keccak keccak = new Keccak(256);
        keccak.update(prefix);

        long asymmetricBits = 0x8002030405060708L;
        int bitLen = Integers.bitLen(asymmetricBits);
        assertEquals(Long.SIZE, bitLen);
        String binary = Long.toBinaryString(asymmetricBits);
        assertEquals(Long.SIZE, binary.length());
        assertNotEquals(binary, new StringBuilder(binary).reverse().toString());

        byte[] eight = new byte[] { 8, 7, 6, 5, 4, 3, 2, (byte) 0x80 };
        long normal = Integers.getLong(eight, 0, eight.length, false);
        assertNotEquals(asymmetricBits, normal);

        keccak.updateBits(asymmetricBits, bitLen);
        byte[] specialDigest = keccak.digest(); // resets the state

        keccak.update(prefix);
        byte[] normalDigest = keccak.digest(eight);

        assertArrayEquals(normalDigest, specialDigest);

        final String expected = "01fa9b8342e622a5d6314dcf2eb0786768d1e804e61af5feb27141ead708524f";

        assertEquals(expected, Strings.encode(normalDigest));

        WrappedKeccak wk = new WrappedKeccak(256);
        wk.update(prefix, 0, prefix.length);
        wk.update(eight, 0, eight.length);
        assertEquals(expected, Strings.encode(wk.digest()));
    }

    @Test
    public void testRandom() {
        testRandom(128, 100);
        testRandom(224, 100);
        testRandom(256, 200);
        testRandom(288, 100);
        testRandom(384, 100);
        testRandom(512, 100);
    }

    private static void testRandom(int bits, final int n) {

        final MessageDigest md_a = new Keccak(bits);
        final MessageDigest md_b = new WrappedKeccak(bits);

        Random r = TestUtils.seededRandom();

        byte[] buffer = new byte[65];
        final int bound = buffer.length + 1;

        for (int i = 0; i < n; i++) {
            r.nextBytes(buffer);
            final int numUpdates = r.nextInt(20);
            for (int j = 0; j < numUpdates; j++) {
                final int end = r.nextInt(bound);
                final int start = end == 0 ? 0 : r.nextInt(end);
                final int len = end - start;
//                System.out.println("[" + start + "-" + end + ")\t\t" + Strings.encode(buffer, start, len, Strings.HEX));
                md_a.update(buffer, start, len);
                md_b.update(buffer, start, len);
            }

            byte[] a = md_a.digest();
            byte[] b = md_b.digest();

            assertArrayEquals(a, b);
        }
    }

    @Disabled("slow")
    @Test
    public void benchmark() {
        byte[] bytes = TestUtils.randomBytes(50);

        final String labelWrapped = WrappedKeccak.class.getSimpleName() + ":\t";
        final String labelKeccak = Keccak.class.getSimpleName() + ":\t\t\t";

        long start;
        final long elapsed0, elapsed1;

        WrappedKeccak wrapped = new WrappedKeccak(256);

        run(wrapped, bytes); // warmup
        start = System.nanoTime();
        run(wrapped, bytes);
        elapsed0 = stop(labelWrapped, start);

        Keccak keccak = new Keccak(256);

        run(keccak, bytes); // warmup
        start = System.nanoTime();
        run(keccak, bytes);
        elapsed1 = stop(labelKeccak, start);

        System.out.println("ratio: " + elapsed0 / (double) elapsed1 + "\n");
    }

    private static void run(MessageDigest md, byte[] bytes) {
        for (int i = 0; i < 7_000_000; i++) {
            md.update(bytes);
            try {
                md.digest(bytes, 0, SELECTOR_LEN);
            } catch (DigestException e) {
                e.printStackTrace();
            }
        }
    }

    private static long stop(String label, long start) {
        long elapsed = System.nanoTime() - start;
        System.out.println(label + (elapsed / 1_000_000.0) + "ms");
        return elapsed;
    }
}
