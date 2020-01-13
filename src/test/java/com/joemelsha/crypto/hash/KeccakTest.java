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
import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;
import org.spongycastle.crypto.digests.KeccakDigest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class KeccakTest {

    private static final byte[] PART_A = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PART_B = "ABCDEFG".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WHOLE;

    static {
        WHOLE = new byte[PART_A.length + PART_B.length];
        System.arraycopy(PART_A, 0, WHOLE, 0, PART_A.length);
        System.arraycopy(PART_B, 0, WHOLE, PART_A.length, PART_B.length);
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

        KeccakDigest k_ = new KeccakDigest(bitLen);

        k_.reset();
        k_.update(PART_A, 0, PART_A.length);
        k_.update(PART_B, 0, PART_B.length);
        byte[] output = new byte[k_.getDigestSize()];
        k_.doFinal(output, 0);
        byte[] b0 = Arrays.copyOf(output, output.length);

        k_.reset();
        k_.update(WHOLE, 0, WHOLE.length);
        k_.doFinal(output, 0);
        byte[] b1 = Arrays.copyOf(output, output.length);

        assertArrayEquals(b0, b1);
        assertArrayEquals(k0, b0);

        System.out.println(FastHex.encodeToString(b0));
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

    private static void testRandom(final int bitLen, final int n) {
        Keccak k = new Keccak(bitLen);
        KeccakDigest k_ = new KeccakDigest(bitLen);

        Random r = new Random(TestUtils.getSeed(System.nanoTime()));

        byte[] buffer = new byte[65];
        final int bound = buffer.length + 1;

        for (int i = 0; i < n; i++) {
            r.nextBytes(buffer);
            final int numUpdates = r.nextInt(20);
            for (int j = 0; j < numUpdates; j++) {
                final int end = r.nextInt(bound);
                final int start = end == 0 ? 0 : r.nextInt(end);
                final int len = end - start;
//                System.out.println("[" + start + "-" + end + ")\t\t" + FastHex.encodeToString(buffer, start, len));
                k.update(buffer, start, len);
                k_.update(buffer, start, len);
            }

            byte[] a = k.digest();

            byte[] k_Output = new byte[k_.getDigestSize()];
            k_.doFinal(k_Output, 0);

            assertArrayEquals(a, k_Output);
        }
    }

    @Test
    public void testPartial() throws DigestException {
        Keccak keccak = new Keccak(256);

        byte[] x = new byte[7];
        Random rand = new Random(TestUtils.getSeed(System.nanoTime()));
        rand.nextBytes(x);

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

        assertNotEquals(FastHex.encodeToString(arr), FastHex.encodeToString(arr2));
        assertArrayEquals(Arrays.copyOfRange(arr, 4, 7), Arrays.copyOfRange(arr2, 4, 7));

        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", FastHex.encodeToString(keccak.digest(new byte[0])));

        keccak.reset();

        byte[] digest = new byte[32];
        bb = ByteBuffer.wrap(digest);
        Arrays.fill(digest, (byte) 0xff);

        keccak.digest(bb);

        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", FastHex.encodeToString(bb.array()));
    }

    @Test
    public void testUpdateBits() throws DecodeException {

        Keccak keccak = new Keccak(256);
        keccak.update(Strings.decode("Hello World", Strings.UTF_8));

        long asymmetricBits = 0x8002030405060708L;
        int bitLen = Integers.bitLen(asymmetricBits);
        assertEquals(Long.SIZE, bitLen);
        String binary = Long.toBinaryString(asymmetricBits);
        assertEquals(Long.SIZE, binary.length());
        assertNotEquals(binary, new StringBuilder(binary).reverse().toString());

        byte[] eight = new byte[] { 8, 7, 6, 5, 4, 3, 2, (byte) 0x80 };
        long normal = Integers.getLong(eight, 0, eight.length);
        long bizarro = BizarroIntegers.getLong(eight, 0, eight.length);
        assertEquals(normal, bizarro);
        assertNotEquals(asymmetricBits, normal);

        keccak.updateBits(asymmetricBits, bitLen);
        byte[] specialDigest = keccak.digest(); // resets the state

//        keccak.reset();
        keccak.update(Strings.decode("Hello World", Strings.UTF_8));
        byte[] normalDigest = keccak.digest(eight);

        assertArrayEquals(normalDigest, specialDigest);
        assertEquals("01fa9b8342e622a5d6314dcf2eb0786768d1e804e61af5feb27141ead708524f", Strings.encode(normalDigest));
    }
}
