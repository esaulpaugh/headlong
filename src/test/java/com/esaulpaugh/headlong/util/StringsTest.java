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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
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
    public void testEncodeDirectBuffer() {
        testEncodeBuffer(ByteBuffer.allocateDirect(RAND.nextInt(257)));
    }

    @Test
    public void testEncodeReadOnlyBuffer() {
        testEncodeBuffer(ByteBuffer.allocate(RAND.nextInt(257)).asReadOnlyBuffer());
    }

    private void testEncodeBuffer(ByteBuffer bb) {
        final int len = bb.capacity();
        final int pos = RAND.nextInt(len + 1);
        bb.position(pos);
        final String hex = Strings.encode(bb);
        assertEquals(pos, bb.position());
        assertEquals(len * 2, hex.length());
        for (int i = 0; i < len; i += 2) {
            if (hex.charAt(i) != '0' || hex.charAt(i + 1) != '0') {
                throw new AssertionError("" + i);
            }
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
    public void base64NoOptionsBytesWithOffset() {
        Random rand = TestUtils.seededRandom();
        java.util.Base64.Encoder mimeEncoder = java.util.Base64.getMimeEncoder();
        java.util.Base64.Decoder mimeDecoder = java.util.Base64.getMimeDecoder();
        for(int j = 0; j < 250; j++) {
            byte[] in = new byte[j];
            rand.nextBytes(in);
            final int destOffset = rand.nextInt(180);
            final int padding = rand.nextInt(180);
            byte[] dest = new byte[destOffset + FastBase64.encodedSize(in.length, FastBase64.NO_FLAGS) + padding];
            FastBase64.encodeToBytes(in, 0, in.length, dest, destOffset, FastBase64.NO_FLAGS);
            @SuppressWarnings("deprecation")
            String s = new String(dest, 0, destOffset, dest.length - padding - destOffset);
            String s2 = mimeEncoder.encodeToString(in);
            assertEquals(base64EncodedLen(j, true, true), s.length());
            assertEquals(s2, s);
            assertArrayEquals(in, mimeDecoder.decode(s));
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
        if (lineSep) {
            estimated += (numBytes - 1) / 57 * 2;
        }
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
    public void testCharSequences() {
        final Random rand = TestUtils.seededRandom();
        final int size = 2_000;
        for (int k = 0; k < 5; k++) {
            final int start = rand.nextInt(size + 1);
            final int end = start + rand.nextInt(1 + size - start);
            final byte[] bytes = new byte[end - start];
            final ByteBuffer buf = ByteBuffer.wrap(bytes);
            final int longs = bytes.length / Long.BYTES;
            for (int i = 0; i < longs; i++) {
                buf.putLong(TestUtils.wildLong(rand));
            }
            for (int i = longs * Long.BYTES; i < bytes.length; i++) {
                buf.put((byte) rand.nextInt());
            }
            final String s = Strings.encode(bytes);
            final CharBuffer c0 = CharBuffer.wrap(s.toCharArray());
            final CharBuffer c1 = CharBuffer.wrap(s);
            assertArrayEquals(bytes, FastHex.decode(s));
            assertArrayEquals(bytes, FastHex.decode(c0));
            assertArrayEquals(bytes, FastHex.decode(c0.asReadOnlyBuffer()));
            assertArrayEquals(bytes, FastHex.decode(c1));
            assertArrayEquals(bytes, FastHex.decode(c1.asReadOnlyBuffer()));
            assertArrayEquals(bytes, FastHex.decode(new StringBuffer(s)));
            assertArrayEquals(bytes, FastHex.decode(new StringBuilder(s)));
        }
    }

    @Test
    public void testHexExceptions() throws Throwable {
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode("0"));
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 0", () -> FastHex.decode("(0"));
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", () -> FastHex.decode("0'"));
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", () -> FastHex.decode("F\0"));
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", () -> FastHex.decode("f\0"));

        final char[] chars = "\0\0".toCharArray();
        final TestUtils.CustomRunnable r = () -> FastHex.decode(new String(chars));

        chars[1] = 'F';
        for (int i = 0; i <= 0xFF; i++) {
            final char v = (char) i;
            chars[0] = v;
            if (validHex(v)) {
                r.run();
            } else {
                assertThrown(IllegalArgumentException.class, "illegal hex val @ 0", r);
            }
        }

        chars[0] = 'f';
        for (int i = 0; i <= 0xFF; i++) {
            final char v = (char) i;
            chars[1] = v;
            if (validHex(v)) {
                r.run();
            } else {
                assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", r);
            }
        }

        chars[0] = 256;
        chars[1] = '0';
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 0", r);
        chars[0] = '0';
        chars[1] = 256;
        assertThrown(IllegalArgumentException.class, "illegal hex val @ 1", r);
    }

    private static boolean validHex(int c) {
        switch (c) {
        case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
        case 'A':case 'B':case 'C':case 'D':case 'E':case 'F':
        case 'a':case 'b':case 'c':case 'd':case 'e':case 'f': return true;
        default: return false;
        }
    }

    @Test
    public void testSingleByteHex() {
        assertEquals("02", FastHex.encodeToString((byte) 0b0000_0010));
    }

    @Test
    public void testFastHexDecode() throws Throwable {
        final byte[] hexBytes = new byte[] { '9', 'a', 'f', '0', '1', 'E', '9', '0', '0', '0' };
        {
            byte[] data = FastHex.decode(hexBytes);
            assertArrayEquals(new byte[] { (byte) 0x9a, (byte) 0xf0, 0x1e, (byte) 0x90, 0x00 }, data);

            data = FastHex.decode(hexBytes, 1, hexBytes.length - 2);
            assertArrayEquals(new byte[] { (byte) 0xaf, 0x01, (byte) 0xe9, 0x00 }, data);

//            final int nextIdx = FastHex.decode(hexBytes, 2, hexBytes.length - 4, data, 1);
//            assertArrayEquals(new byte[] { (byte) 0xaf, (byte) 0xf0 }, data);
//            assertEquals(2, nextIdx);
        }

        final String hex = new String(hexBytes, StandardCharsets.US_ASCII);
        byte[] data = FastHex.decode(hex);
        assertArrayEquals(new byte[] { (byte) 0x9a, (byte) 0xf0, 0x1e, (byte) 0x90, 0x00 }, data);

        data = FastHex.decode(hex, 1, hex.length() - 2);
        assertArrayEquals(new byte[] { (byte) 0xaf, 0x01, (byte) 0xe9, 0x00 }, data);

//        final int nextIdx = FastHex.decode(hex, 2, hex.length() - 4, data, 1);
//        assertArrayEquals(new byte[] { (byte) 0xaf, (byte) 0xf0 }, data);
//        assertEquals(2, nextIdx);

        assertEquals(0, FastHex.decodedLength(0));
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decodedLength(1));
        assertEquals(1, FastHex.decodedLength(2));
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decodedLength(3));
        assertEquals(2, FastHex.decodedLength(4));
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decodedLength(5));

        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode("0"));
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode("00ff", 0, 9));
//        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode("00ff11", 2, 1, new byte[10], 0));

        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode(new byte[1]));
        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode(new byte[4], 0, 9));
//        assertThrown(IllegalArgumentException.class, "len must be a multiple of two", () -> FastHex.decode(new byte[6], 2, 1, new byte[10], 0));
    }

    @Disabled("slow")
    @Test
    public void testHexDecode() {
        int count = 0;
        for (int k = 0; k < 256; k++) {
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    try {
                        String s = "0" + (char) k + (char) i + (char) j;
                        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
                        assertArrayEquals(FastHex.decode(bytes, 0, bytes.length), FastHex.decode(s));
                    } catch (IllegalArgumentException iae) {
                        if (iae.getMessage().startsWith("i")) {
                            count++;
                        }
                    }
                }
            }
        }
        assertEquals((int) Math.pow("0123456789ABCDEFabcdef".length(), 3), 256 * 256 * 256 - count);
        assertEquals("0123456789ABCDEFabcdef".length(), (int) Math.cbrt(256 * 256 * 256 - count));
    }
}
