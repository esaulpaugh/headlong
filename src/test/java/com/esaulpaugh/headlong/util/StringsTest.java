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

import com.esaulpaugh.headlong.TestUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsTest {

    private static final Random RAND = TestUtils.seededRandom();

    private static final Supplier<byte[]> SUPPLY_RANDOM = () -> TestUtils.randomBytes(RAND.nextInt(115), RAND);

    private static void testEncoding(int n, int encoding, Supplier<byte[]> supplier) {
        for (int i = 0; i < n; i++) {
            byte[] x = supplier.get();
            assertArrayEquals(x, Strings.decode(Strings.encode(x, encoding), encoding));
        }
    }

    @Test
    public void utf8() {
        testEncoding(20_000, UTF_8, () -> {
            byte[] x = new byte[RAND.nextInt(115)];
            for (int i = 0; i < x.length; i++) {
                x[i] = (byte) RAND.nextInt(128);
            }
            return x;
        });
    }

    @Test
    public void hex() {
        Random r = TestUtils.seededRandom();
        byte[] bytes = TestUtils.randomBytes(r.nextInt(100), r);
        assertEquals(Hex.toHexString(bytes), FastHex.encodeToString(bytes));

        assertEquals(
                Hex.toHexString(new byte[] { 0, -1, 9, 51, 127, -128 }),
                FastHex.encodeToString((byte)0, (byte)-1, (byte)9, (byte)51, (byte)127, (byte)-128)
        );

        testEncoding(20_000, HEX, SUPPLY_RANDOM);
    }

    @Test
    public void base64NoOptions() {
        Random rand = TestUtils.seededRandom();
        java.util.Base64.Encoder mimeEncoder = java.util.Base64.getMimeEncoder();
        java.util.Base64.Decoder mimeDecoder = java.util.Base64.getMimeDecoder();
        for(int j = 0; j < 250; j++) {
            byte[] x = new byte[j];
            rand.nextBytes(x);
            String s = FastBase64.encodeToString(x, 0, j, FastBase64.NO_FLAGS);
            String s2 = mimeEncoder.encodeToString(x);
            assertEquals(base64EncodedLen(j, true, true), s.length());
            assertEquals(s2, s);
            assertArrayEquals(x, mimeDecoder.decode(s));
        }
    }

    @Test
    public void base64PaddedNoLineSep() {
        Random rand = TestUtils.seededRandom();
        java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder();
        for (int j = 0; j < 250; j++) {
            byte[] x = new byte[j];
            rand.nextBytes(x);
            String s = FastBase64.encodeToString(x, 0, j, FastBase64.URL_SAFE_CHARS | FastBase64.NO_LINE_SEP);
            String sControl = encoder.encodeToString(x);
            assertEquals(base64EncodedLen(j, false, true), s.length());
            assertEquals(sControl, s);
            assertArrayEquals(x, Strings.decode(s, BASE_64_URL_SAFE));
        }
    }

    @Test
    public void base64Default() {
        final Random rand = TestUtils.seededRandom();
        for(int j = 3; j < 250; j++) {
            byte[] x = TestUtils.randomBytes(j, rand);
            int offset = rand.nextInt(j / 3);
            int len = rand.nextInt(j / 2);
            String s = Strings.encode(x, offset, len, BASE_64_URL_SAFE);
            assertEquals(base64EncodedLen(len, false, false), s.length());
            byte[] y = Strings.decode(s, BASE_64_URL_SAFE);
            for (int k = 0; k < len; k++) {
                if(y[k] != x[offset + k]) {
                    throw new AssertionError(y[k] + " != " + x[offset + k]);
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

    @Test
    public void testHexExceptions() throws Throwable {
        TestUtils.assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode("0"));

        TestUtils.assertThrown(IllegalArgumentException.class, "illegal hex val @ 0", () -> FastHex.decode("(0"));

        TestUtils.assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", () -> FastHex.decode("0'"));
    }

    @Test
    public void testSingleByteHex() {
        assertEquals("02", FastHex.encodeToString((byte) 0b0000_0010));
    }

    @Disabled("slowish")
    @Test
    public void testHexDecode() {
        int count = 0;
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                try {
                    FastHex.decode("" + (char) i + (char) j);
                } catch (IllegalArgumentException iae) {
                    if(iae.getMessage().contains("illegal hex val @ ")) {
                        count++;
                    }
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    if(aioobe.getMessage().contains("out of bounds for length 256")) {
                        count++;
                    }
                }
            }
        }
        assertEquals("0123456789ABCDEFabcdef".length(), (int) Math.sqrt(256 * 256 - count));
    }
}
