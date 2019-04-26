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
import com.esaulpaugh.headlong.util.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.crypto.digests.KeccakDigest;

import java.util.Arrays;
import java.util.Random;

public class KeccakTest {

    @Test
    public void testMultiUpdate() {

        final byte[] a = "abcdefghijklmnopqrstuvwxyz".getBytes(Utils.CHARSET_ASCII);
        final byte[] b = "ABCDEFG".getBytes(Utils.CHARSET_ASCII);
        final byte[] input = new byte[a.length + b.length];
        System.arraycopy(a, 0, input, 0, a.length);
        System.arraycopy(b, 0, input, a.length, b.length);

        final Keccak k = new Keccak(256);

        k.reset();
        k.update(new byte[1]);
        k.update(new byte[7]);

        k.reset();
        k.update(input);
        byte[] k0 = k.digest();

        k.reset();
        k.update(a);
        k.update(b);
        byte[] k1 = k.digest();

        Assert.assertArrayEquals(k0, k1);

        KeccakDigest k_ = new KeccakDigest(256);

        k_.reset();
        k_.update(a, 0, a.length);
        k_.update(b, 0, b.length);
        byte[] output = new byte[32];
        k_.doFinal(output, 0);
        byte[] b0 = Arrays.copyOf(output, output.length);

        k_.reset();
        k_.update(input, 0, input.length);
        k_.doFinal(output, 0);
        byte[] b1 = Arrays.copyOf(output, output.length);

        Assert.assertArrayEquals(b0, b1);
        Assert.assertArrayEquals(k0, b0);

        System.out.println(FastHex.encodeToString(b0));
    }

    @Test
    public void testRandom() {

        Keccak k = new Keccak(256);
        KeccakDigest k_ = new KeccakDigest(256);

        Random r = new Random(MonteCarloTest.getSeed(System.nanoTime()));

        byte[] buffer = new byte[65];
        final int bound = buffer.length + 1;

        r.nextBytes(buffer);

        final int n = 1_000;
        for (int i = 0; i < n; i++) {
            final int numUpdates = r.nextInt(20);
            for (int j = 0; j < numUpdates; j++) {
                final int len = r.nextInt(bound);

//                System.out.println(len + "\u0009\u0009" + FastHex.encodeToString(buffer, 0, len));
                k.update(buffer, 0 , len);
                k_.update(buffer, 0, len);
            }

            byte[] a = k.digest();
//            ByteBuffer zzz = ByteBuffer.allocate(32);
//            k.digest(zzz);

            byte[] k_Output = new byte[k_.getDigestSize()];
            k_.doFinal(k_Output, 0);

            Assert.assertArrayEquals(a, k_Output);

//            System.out.println(FastHex.encodeToString(k_Output));
        }
    }
}
