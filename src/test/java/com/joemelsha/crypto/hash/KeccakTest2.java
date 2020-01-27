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
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.util.Arrays;
import java.util.Random;

import static org.bouncycastle.jcajce.provider.digest.Keccak.Digest256;
import static org.bouncycastle.jcajce.provider.digest.Keccak.DigestKeccak;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class KeccakTest2 {

    private static final byte[] PART_A = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PART_B = "ABCDEFG".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WHOLE;

    static {
        WHOLE = new byte[PART_A.length + PART_B.length];
        System.arraycopy(PART_A, 0, WHOLE, 0, PART_A.length);
        System.arraycopy(PART_B, 0, WHOLE, PART_A.length, PART_B.length);
    }

    @Test
    public void testMultiUpdate() throws DigestException {
        testMultiUpdate(128);
        testMultiUpdate(224);
        testMultiUpdate(256);
        testMultiUpdate(288);
        testMultiUpdate(384);
        testMultiUpdate(512);
    }

    private static void testMultiUpdate(final int bitLen) throws DigestException {
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

        DigestKeccak k_ = new DigestKeccak(bitLen);

        k_.reset();
        k_.update(PART_A, 0, PART_A.length);
        k_.update(PART_B, 0, PART_B.length);
        byte[] output = new byte[k_.getDigestLength()];
        k_.digest(output, 0, output.length);
        byte[] b0 = Arrays.copyOf(output, output.length);

        k_.reset();
        k_.update(WHOLE, 0, WHOLE.length);
        k_.digest(output, 0, output.length);
        byte[] b1 = Arrays.copyOf(output, output.length);

        assertArrayEquals(b0, b1);
        assertArrayEquals(k0, b0);

        System.out.println(Strings.encode(b0));
    }

    @Test
    public void testRandom() throws DigestException {
        testRandom(128, 100);
        testRandom(224, 100);
        testRandom(256, 200);
        testRandom(288, 100);
        testRandom(384, 100);
        testRandom(512, 100);
    }

    private static void testRandom(final int bitLen, final int n) throws DigestException {
        Keccak k = new Keccak(bitLen);
        DigestKeccak k_ = new DigestKeccak(bitLen);

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
//                System.out.println("[" + start + "-" + end + ")\t\t" + Strings.encode(buffer, start, len));
                k.update(buffer, start, len);
                k_.update(buffer, start, len);
            }

            byte[] a = k.digest();

            byte[] k_Output = new byte[k_.getDigestLength()];
            k_.digest(k_Output, 0, k_Output.length);

            assertArrayEquals(a, k_Output);
        }
    }

    @Test
    public void testUpdateBits() throws DecodeException, DigestException {

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
        long normal = Integers.getLong(eight, 0, eight.length);
        assertNotEquals(asymmetricBits, normal);

        keccak.updateBits(asymmetricBits, bitLen);
        byte[] specialDigest = keccak.digest(); // resets the state

        keccak.update(prefix);
        byte[] normalDigest = keccak.digest(eight);

        assertArrayEquals(normalDigest, specialDigest);
        assertEquals("01fa9b8342e622a5d6314dcf2eb0786768d1e804e61af5feb27141ead708524f", Strings.encode(normalDigest));

        Digest256 k_ = new Digest256();
        k_.update(prefix, 0 , prefix.length);

        k_.update(eight, 0, eight.length);

        byte[] out = new byte[k_.getDigestLength()];
        k_.digest(out, 0, out.length);
        assertEquals("01fa9b8342e622a5d6314dcf2eb0786768d1e804e61af5feb27141ead708524f", Strings.encode(out));
    }
}
