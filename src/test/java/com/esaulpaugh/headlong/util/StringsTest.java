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

import com.esaulpaugh.headlong.abi.MonteCarloTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.util.Strings.BASE64;
import static com.esaulpaugh.headlong.util.Strings.DECIMAL;
import static com.esaulpaugh.headlong.util.Strings.DONT_PAD;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class StringsTest {

    private static final Random RAND = new Random(MonteCarloTest.getSeed(System.nanoTime()));

    private static final Supplier<byte[]> SUPPLY_RANDOM = () -> {
        byte[] x = new byte[RAND.nextInt(400)];
        RAND.nextBytes(x);
        return x;
    };

    private static void testEncoding(int n, int encoding, Supplier<byte[]> supplier) {
        for (int i = 0; i < n; i++) {
            test(supplier.get(), encoding);
        }
    }

    private static void test(byte[] x, int encoding) {
        String s = Strings.encode(x, encoding);
        byte[] y = Strings.decode(s, encoding);
        Assert.assertArrayEquals(x, y);
    }

    @Test
    public void utf8() {
        testEncoding(20_000, UTF_8, () -> {
            byte[] x = new byte[RAND.nextInt(400)];
            for (int i = 0; i < x.length; i++) {
                x[i] = (byte) RAND.nextInt(128);
            }
            return x;
        });
    }

    @Test
    public void hex() {
        testEncoding(20_000, HEX, SUPPLY_RANDOM);
    }

    @Test
    public void decimal() {
        testEncoding(20_000, DECIMAL, SUPPLY_RANDOM);
    }

    @Test
    public void base64Padded() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        java.util.Base64.Encoder mimeEncoder = java.util.Base64.getMimeEncoder();
        for(int j = 0; j < 160; j++) {
            byte[] x = new byte[j];
            for (int i = 0; i < 100; i++) {
                rand.nextBytes(x);
                String s = Strings.encode(x, BASE64);
                String s2 = mimeEncoder.encodeToString(x);
                Assert.assertEquals(base64EncodedLen(x.length, true, true), s.length());
                Assert.assertEquals(s2, s);
                Assert.assertArrayEquals(x, Strings.decode(s, BASE64));
            }
        }
    }

    @Test
    public void base64PaddedNoLineSep() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        for(int j = 0; j < 160; j++) {
            byte[] x = new byte[j];
            for (int i = 0; i < 100; i++) {
                rand.nextBytes(x);
                String s = Strings.toBase64(x, 0, x.length, false, true);
                String s2 = encoder.encodeToString(x);
                Assert.assertEquals(base64EncodedLen(x.length, false, true), s.length());
                Assert.assertEquals(s2, s);
                Assert.assertArrayEquals(x, Strings.decode(s, BASE64));
            }
        }
    }

    @Test
    public void base64Unpadded() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        for(int j = 4; j < 160; j++) {
            byte[] x = new byte[j];
            final boolean lineSep = rand.nextBoolean();
            for (int i = 0; i < 100; i++) {
                rand.nextBytes(x);
                int offset = rand.nextInt(x.length / 3);
                int len = rand.nextInt(x.length / 2);
                String s = Strings.toBase64(x, offset, len, lineSep, DONT_PAD);
                Assert.assertEquals(base64EncodedLen(len, lineSep, false), s.length());
                byte[] y = Strings.decode(s, BASE64);
                for (int k = 0; k < len; k++) {
                    if(y[k] != x[offset + k]) {
                        throw new AssertionError(y[k] + " != " + x[offset + k]);
                    }
                }
            }
        }
    }

    private static int base64EncodedLen(int numBytes, boolean lineSep, boolean padding) {
        if(padding) {
            int est = numBytes / 3 * 4 + (numBytes % 3 > 0 ? 4 : 0);
            return est + (lineSep ? (est - 1) / 76 << 1 : 0);
        }
//        return (int) StrictMath.ceil(inputLen * 4 / 3d);
        int estimated = numBytes / 3 * 4;
        estimated += lineSep ? (estimated - 1) / 76 << 1 : 0;
        int mod = numBytes % 3;
        if(mod == 0) {
            return estimated;
        }
        if(mod == 1) {
            return estimated + 2;
        }
        return estimated + 3;
    }
}
