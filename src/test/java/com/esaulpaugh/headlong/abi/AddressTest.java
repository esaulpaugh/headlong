/*
   Copyright 2021 Evan Saulpaugh

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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.abi.Address.MAX_LABEL_LEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^0x[\\dA-Fa-f]{40}$");

    private static final String[] VECTORS = new String[] {
            "0x52908400098527886E0F7030069857D2E4169EE7",
            "0x8617E340B3D01FA5F11F306F4090FD50E238070D",
            "0xde709f2102306220921060314715629080e2fb77",
            "0x27b1fdb04752bbc536007a920d24acb045561c26",
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb" };

    @Test
    public void testVectorChecksums() throws Throwable {
        for(final String vector : VECTORS) {
            testAddress(vector, vector.substring(0, 12));
            final String corrupted = vector.replace('F', 'f').replace('b', 'B');
            assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.validateChecksumAddress(corrupted));
        }
    }

    @Test
    public void testGeneratedChecksums() {
        final Random r = TestUtils.seededRandom();
        testAddress(new Address(BigInteger.valueOf(TestUtils.pickRandom(r, 1 + r.nextInt(Long.BYTES), true))).toString(), null);
        testAddress(MonteCarloTestCase.generateAddress(r).toString(), null);
        testAddress(MonteCarloTestCase.generateAddress(r).toString(), "a label");
    }

    private static void testAddress(final String in, final String label) {
        assertTrue(ADDRESS_PATTERN.matcher(in).matches());
        Address.validateChecksumAddress(in);
        final String checksummedStr = Address.toChecksumAddress(in);
        assertEquals(in, checksummedStr);
        final Address checksummed = Address.wrap(checksummedStr, label);
        assertEquals(in, checksummed.toString());
        final BigInteger checksummedVal = checksummed.value();
        assertEquals(checksummedVal, Address.wrap(in).value());
        assertTrue(checksummedVal.bitLength() <= 160);
    }

    private static String generateAddressString(Random r) {
        byte[] _20 = new byte[TypeFactory.ADDRESS_BIT_LEN / Byte.SIZE];
        r.nextBytes(_20);
        return Address.toChecksumAddress("0x" + Strings.encode(_20));
    }

    @Test
    public void testStringAddrs() {
        final Random r = TestUtils.seededRandom();

        testAddress(Address.toChecksumAddress(BigInteger.ZERO), "LABEL");
        testAddress(Address.toChecksumAddress(BigInteger.ONE), "0x0x0x0x0x0x\0\0\0\\\"\u0009xzzzzzzzzzz");
        testAddress(Address.toChecksumAddress(BigInteger.TEN), null);
        testAddress(Address.toChecksumAddress(BigInteger.valueOf(2L)), null);
        for (int i = 0; i < 10; i++) {
            testAddress(generateAddressString(r), TestUtils.generateASCIIString(r.nextInt(MAX_LABEL_LEN + 1), r));
        }

        BigInteger _FFff = Address.wrap("0x000000000000000000000000000000000000FFff").value();
        assertEquals(BigInteger.valueOf(65535L), _FFff);

        BigInteger _10000 = Address.wrap("0x0000000000000000000000000000000000010000").value();
        assertEquals(BigInteger.valueOf(65536L), _10000);

        BigInteger _8000 = Address.wrap("0x8000000000000000000000000000000000000000").value();
        assertTrue(_8000.signum() > 0);

        final BigIntegerType uint160Type = TypeFactory.create("uint160");
        final BigInteger min = uint160Type.minValue();
        final BigInteger max = uint160Type.maxValue();

        final BigInteger _0000 = Address.wrap("0x0000000000000000000000000000000000000000").value();
        assertEquals(min, _0000);

        final BigInteger _FFfF = Address.wrap("0xFFfFfFffFFfffFFfFFfFFFFFffFFFffffFfFFFfF").value();
        assertEquals(max, _FFfF);

        final BigInteger _ffFf = Address.wrap("0xffFfffFFFffffFFFFfFFFfffFFFfffFfFFfFff0f").value();
        assertEquals(max.subtract(BigInteger.valueOf(240L)), _ffFf);
    }

    @Test
    public void testBigIntAddrs() throws Throwable {
        final BigInteger[] values = new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.TEN,
                BigInteger.valueOf(256L),
                Address.wrap("0x82095CAfeBaBECaFebaBe00083Ce15d74e191051").value(),
                Address.wrap("0x4bEc173F8D9D3D90188777cAfeBabeCafebAbE99").value(),
                Address.wrap("0x5cafEBaBEcafEBabE7570ad8AC11f8d812ee0606").value(),
                Address.wrap("0x0000000005CaFEbabeCafEbABE7570ad8ac11F8d").value(),
                Address.wrap("0x0000000000000000000082095CafEBABEcAFebAB").value()
        };
        for (BigInteger val : values) {
            testBigIntAddressVal(val);
        }

        final SecureRandom sr = new SecureRandom();
        sr.setSeed(new SecureRandom().generateSeed(64));
        sr.setSeed(sr.generateSeed(64));
        testBigIntAddressVal(new BigInteger(TypeFactory.ADDRESS_BIT_LEN, sr));
        testBigIntAddressVal(new BigInteger(TypeFactory.ADDRESS_BIT_LEN, sr));

        BigInteger temp;
        do {
            temp = new BigInteger(161, sr);
        } while (temp.bitLength() < 161);
        final BigInteger tooBig = temp;
        assertThrown(IllegalArgumentException.class, "invalid bit length: 161", () -> Address.toChecksumAddress(tooBig));
        assertThrown(IllegalArgumentException.class,
                "invalid bit length: 161",
                () -> Address.toChecksumAddress(new BigInteger("182095cafebabecafebabe00083ce15d74e191051", 16))
        );
        assertThrown(IllegalArgumentException.class,
                "invalid bit length: 164",
                () -> Address.toChecksumAddress(new BigInteger("82095cafebabecafebabe00083ce15d74e1910510", 16))
        );

        final Random r = new Random(sr.nextLong());
        for (int bitlen = 0; bitlen <= 160; bitlen++) {
            testBigIntAddressVal(new BigInteger(bitlen, r));
            testBigIntAddressVal(new BigInteger(bitlen, r));
        }
    }

    private static void testBigIntAddressVal(final BigInteger addrVal) {
        final String checksumAddress = Address.toChecksumAddress(addrVal);
        assertTrue(checksumAddress.startsWith("0x"));
        assertEquals(42, checksumAddress.length());
        final Address address = Address.wrap(checksumAddress);
        assertEquals(checksumAddress, address.toString());
        assertEquals(addrVal, address.value());
        assertEquals(addrVal, computeValue1(checksumAddress));
        assertEquals(addrVal, computeValue2(checksumAddress));
        assertEquals(addrVal, computeValue4(checksumAddress));
        assertEquals(addrVal, computeValue5(checksumAddress));
        assertEquals(addrVal, computeValue6(checksumAddress));
    }

    private static BigInteger computeValue1(final String checksumAddress) {
        final int numDataBytes = (checksumAddress.length() - 2) / FastHex.CHARS_PER_BYTE;
        final byte[] bytes = new byte[1 + numDataBytes];
        System.arraycopy(FastHex.decode(checksumAddress, 2, checksumAddress.length() - 2), 0, bytes, 1, numDataBytes);
        return new BigInteger(bytes);
    }

    private static BigInteger computeValue2(final String checksumAddress) {
        return new BigInteger(checksumAddress.substring(2), 16);
    }

    private static BigInteger computeValue4(final String checksumAddress) {
        BigInteger sum = BigInteger.ZERO;
        int begin = 2, end = 4;
        int shiftAmt = 42 / FastHex.CHARS_PER_BYTE - 2;
        while (begin < 42) {
            sum = sum.add(new BigInteger(zeroPad(Integer.parseInt(checksumAddress.substring(begin, end), 16), shiftAmt--)));
            begin += 2;
            end += 2;
        }
        return sum;
    }

    private static byte[] zeroPad(final int c, final int n) {
        byte[] bytes = new byte[2 + n];
        bytes[1] = (byte) c;
        return bytes;
    }

    private static BigInteger computeValue5(final String checksumAddress) {
        byte[] x = new byte[21];
        FastHex.decode(checksumAddress, 2, 40, x, 1);
        return new BigInteger(x);
    }

    private static BigInteger computeValue6(final String checksumAddress) {
        return parseAndShift(checksumAddress, 27, 42)
                .add(parseAndShift(checksumAddress, 12, 27))
                .add(parseAndShift(checksumAddress, 2, 12));
    }

    private static BigInteger parseAndShift(String addr, int start, int end) {
        long v = Long.parseLong(addr.substring(start, end), 16);
        return BigInteger.valueOf(v).shiftLeft((42 - end) * 4);
    }

    @Test
    public void testStringAddrExceptions() throws Throwable {
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x82095cafebabecafebabe00083ce15d74e191051"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x4bec173f8d9d3d90188777cafebabecafebabe99"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x4bec173f8d9d3d90188777CAFEBABEcafebabe99"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x5cafebabecafebabe7570ad8ac11f8d812ee0606"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x0000000005cafebabecafebabe7570ad8ac11f8d"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x0000000000000000000082095cafebabecafebab"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0xc0ec0fbb1c07aebe2a6975d50b5f6441b05023f9"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0xa62274005cafebabecafebabecaebb178db50ad6"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0xc6782c3a8155971a5d16005cafebabecafebabe8"));
        assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.wrap("0x000000000000000000000000000000000000ffff"));

        assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 2",
                () -> Address.wrap("0x+000000000000000000082095cafebabecafebab")
        );
        assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 2",
                () -> Address.wrap("0x-000000000000000000082095cafebabecafebab")
        );
        assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 41",
                () -> Address.wrap("0x0000000000000000000082095cafebabecafeba+")
        );
        assertThrown(IllegalArgumentException.class,
                "missing 0x prefix",
                () -> Address.wrap("0yaaaaa")
        );
        assertThrown(IllegalArgumentException.class,
                "missing 0x prefix",
                () -> Address.wrap("8x5cafebabecafebabe7570ad8ac11f8d812ee0606")
        );
        assertThrown(IllegalArgumentException.class,
                "expected address length 42; actual is 41",
                () -> Address.wrap("0xa83aaef1b5c928162005cafebabecafebabecb0")
        );
        assertThrown(IllegalArgumentException.class,
                "expected address length 42; actual is 43",
                () -> Address.wrap("0xa83aaef1b5c928162005cafebabecafebabecb0a0")
        );
    }

    @Test
    public void testJunk() throws Throwable {
        assertThrown(IllegalArgumentException.class, "expected address length 42; actual is 0", () -> Address.toChecksumAddress(""));
        assertThrown(IllegalArgumentException.class, "expected address length 42; actual is 1", () -> Address.toChecksumAddress("\0"));
        assertThrown(IllegalArgumentException.class, "expected address length 42; actual is 2", () -> Address.toChecksumAddress("0x"));
        assertThrown(IllegalArgumentException.class,
                "missing 0x prefix",
                () -> Address.toChecksumAddress("\0x\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0")
        );
        assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 37",
                () -> Address.toChecksumAddress("0x00000000000000000000000000000000000\u00090000")
        );
        BigInteger big = null;
        assertThrown(NullPointerException.class, () -> Address.toChecksumAddress(big));
        String s = null;
        assertThrown(NullPointerException.class, () -> Address.toChecksumAddress(s));
    }

    @Test
    public void testLabel() throws Throwable {
        Address someAddr = Address.wrap("0x5cafEBaBEcafEBabE7570ad8AC11f8d812ee0606", "Cafe Babe's hot wallet");
        assertEquals("Cafe Babe's hot wallet", someAddr.getLabel());
        assertThrown(IllegalArgumentException.class,
                "labeling aborted because existing label not null",
                () -> someAddr.withLabel("Cafe Joe's wallet"));
        Address joesAddr = Address.wrap(someAddr.toString(), "Cafe Joe's wallet");
        assertEquals("Cafe Joe's wallet", joesAddr.getLabel());

        Address anon = Address.wrap("0x0000000000000000000082095CafEBABEcAFebAB").withLabel("unknown");
        assertEquals("unknown", anon.getLabel());

        Address anon2 = Address.wrap("0xFFfFfFffFFfffFFfFFfFFFFFffFFFffffFfFFFfF", "unknown2");
        assertEquals("unknown2", anon2.getLabel());

        assertThrown(IllegalArgumentException.class,
                "label length exceeds maximum: 176 > 36",
                () -> Address.wrap(
                        "0xFFfFfFffFFfffFFfFFfFFFFFffFFFffffFfFFFfF",
                        "Four score and seven years ago our fathers brought forth on this continent, a new nation, " +
                        "conceived in Liberty, and dedicated to the proposition that all men are created equal.")
        );
        final int addrDataChars = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
        assertEquals(40, addrDataChars);
        assertTrue(MAX_LABEL_LEN < addrDataChars);
    }
}
