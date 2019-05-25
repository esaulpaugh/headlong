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

import com.esaulpaugh.headlong.abi.MonteCarloTest;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.crypto.digests.KeccakDigest;

import java.util.Arrays;
import java.util.Random;

public class KeccakTest {

    private static final byte[] PART_A = "abcdefghijklmnopqrstuvwxyz".getBytes(Strings.CHARSET_ASCII);
    private static final byte[] PART_B = "ABCDEFG".getBytes(Strings.CHARSET_ASCII);
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

        Assert.assertArrayEquals(k0, k1);

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

        Assert.assertArrayEquals(b0, b1);
        Assert.assertArrayEquals(k0, b0);

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

        Random r = new Random(MonteCarloTest.getSeed(System.nanoTime()));

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

            Assert.assertArrayEquals(a, k_Output);
        }
    }
}
