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
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {

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
    public void testVectorChecksums() {
        for(String address : VECTORS) {
            Address.validateChecksumAddress(address);
        }
    }

    @Test
    public void testCorruptedVectors() throws Throwable {
        for(String address : VECTORS) {
            final String corrupted = address.replace('F', 'f').replace('b', 'B');
            assertThrown(IllegalArgumentException.class, "invalid checksum", () -> Address.validateChecksumAddress(corrupted));
        }
    }

    @Test
    public void testGeneratedChecksums() {
        final Random r = TestUtils.seededRandom();
        String[] valid = new String[] {
                Address.wrapDecoded(BigInteger.valueOf(TestUtils.pickRandom(r, 8, true))).toString(),
                Address.wrapDecoded(BigInteger.valueOf(TestUtils.pickRandom(r, 8, true))).toString(),
                MonteCarloTestCase.generateAddress(r).toString(),
                MonteCarloTestCase.generateAddress(r).toString()
        };

        for(String address : valid) {
            Address.validateChecksumAddress(address);
        }
    }

    @Test
    public void testBigIntAddrs() throws Throwable {
        testBigIntAddr(BigInteger.ZERO);
        testBigIntAddr(BigInteger.ONE);
        testBigIntAddr(BigInteger.TEN);
        testBigIntAddr(BigInteger.valueOf(2L));
        testBigIntAddr(Address.wrap("0x82095CAfeBaBECaFebaBe00083Ce15d74e191051").value());
        testBigIntAddr(Address.wrap("0x4bEc173F8D9D3D90188777cAfeBabeCafebAbE99").value());
        testBigIntAddr(Address.wrap("0x5cafEBaBEcafEBabE7570ad8AC11f8d812ee0606").value());
        testBigIntAddr(Address.wrap("0x0000000005CaFEbabeCafEbABE7570ad8ac11F8d").value());
        testBigIntAddr(Address.wrap("0x0000000000000000000082095CafEBABEcAFebAB").value());

        assertThrown(IllegalArgumentException.class,
                "invalid bit length: 161",
                () -> Address.toChecksumAddress(new BigInteger("182095cafebabecafebabe00083ce15d74e191051", 16))
        );
        assertThrown(IllegalArgumentException.class,
                "invalid bit length: 164",
                () -> Address.toChecksumAddress(new BigInteger("82095cafebabecafebabe00083ce15d74e1910510", 16))
        );

        final SecureRandom sr = new SecureRandom();
        sr.setSeed(new SecureRandom().generateSeed(64));
        sr.setSeed(sr.generateSeed(64));
        for (int i = 0; i < 10; i++) {
            testBigIntAddr(new BigInteger(TypeFactory.ADDRESS_BIT_LEN, sr));
        }

        final Random r = new Random(sr.nextLong());
        for (int bitlen = 0; bitlen <= 160; bitlen++) {
            testBigIntAddr(new BigInteger(bitlen, r));
            testBigIntAddr(new BigInteger(bitlen, r));
        }
        BigInteger temp;
        do {
            temp = new BigInteger(161, r);
        } while (temp.bitLength() < 161);
        final BigInteger tooBig = temp;
        assertThrown(IllegalArgumentException.class, "invalid bit length: 161", () -> Address.toChecksumAddress(tooBig));
    }

    @Test
    public void testStringAddrs() throws Throwable {
        testStringAddr(Address.toChecksumAddress(BigInteger.ZERO));
        testStringAddr(Address.toChecksumAddress(BigInteger.ONE));
        testStringAddr(Address.toChecksumAddress(BigInteger.TEN));
        testStringAddr(Address.toChecksumAddress(BigInteger.valueOf(2L)));

        final Random r = TestUtils.seededRandom();
        testStringAddr(MonteCarloTestCase.generateAddressString(r));
        testStringAddr(MonteCarloTestCase.generateAddressString(r));

        BigInteger _FFff = Address.wrap("0x000000000000000000000000000000000000FFff").value();
        assertEquals(BigInteger.valueOf(65535L), _FFff);

        BigInteger _8000 = Address.wrap("0x8000000000000000000000000000000000000000").value();
        assertTrue(_8000.signum() > 0);

        final BigIntegerType uint160Type = TypeFactory.create("uint160");

        final BigInteger _0000 = Address.wrap("0x0000000000000000000000000000000000000000").value();
        assertEquals(uint160Type.minValue(), _0000);

        final BigInteger _FFfF = Address.wrap("0xFFfFfFffFFfffFFfFFfFFFFFffFFFffffFfFFFfF").value();
        assertEquals(uint160Type.maxValue(), _FFfF);

        final BigInteger _ffFf = Address.wrap("0xffFfffFFFffffFFFFfFFFfffFFFfffFfFFfFff0f").value();
        assertEquals(uint160Type.maxValue().subtract(BigInteger.valueOf(240L)), _ffFf);
    }

    private static void testStringAddr(final String addrString) {
        assertTrue(Address.wrap(addrString).value().bitLength() <= 160);
    }

    private static void testBigIntAddr(final BigInteger addr) {
        final String addrString = Address.toChecksumAddress(addr);
        assertTrue(addrString.startsWith("0x"));
        assertEquals(42, addrString.length());
        final String checksumAddress = Address.toChecksumAddress(addr);
        final Address a = Address.wrap(checksumAddress);
        assertEquals(checksumAddress, a.toString());
        assertEquals(addrString, checksumAddress);
        final Address b = Address.wrap(addrString);
        assertEquals(addrString, b.toString());
        assertEquals(a, b);
        assertEquals(a.value(), b.value());
        assertEquals(addr, a.value());
        assertEquals(addr, computeValue(checksumAddress));
        assertEquals(addr, computeValue2(checksumAddress));
    }

    private static BigInteger computeValue(String checksumAddress) {
        int hexBytes = (checksumAddress.length() - 2) / FastHex.CHARS_PER_BYTE;
        byte[] bytes = new byte[1 + hexBytes];
        System.arraycopy(FastHex.decode(checksumAddress, 2, checksumAddress.length() - 2), 0, bytes, 1, hexBytes);
        return new BigInteger(bytes);
    }

    private static BigInteger computeValue2(String checksumAddress) {
        return new BigInteger(checksumAddress.substring(2), 16);
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
                "expected prefix 0x not found",
                () -> Address.wrap("aaaaa")
        );
        assertThrown(IllegalArgumentException.class,
                "expected prefix 0x not found",
                () -> Address.wrap("5cafebabecafebabe7570ad8ac11f8d812ee0606")
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
    public void testConstants() {
        assertEquals("0x", Address.HEX_PREFIX);
        assertEquals(42, Address.ADDRESS_STRING_LEN);
    }
}
