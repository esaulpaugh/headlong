package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {

    @Test
    public void testBigIntAddrs() throws Throwable {
        testBigIntAddr(BigInteger.ZERO);
        testBigIntAddr(BigInteger.ONE);
        testBigIntAddr(BigInteger.TEN);
        testBigIntAddr(BigInteger.valueOf(2L));
        testBigIntAddr(Address.wrap("0x82095cafebabecafebabe00083ce15d74e191051").address);
        testBigIntAddr(Address.wrap("0x4bec173f8d9d3d90188777cafebabecafebabe99").address);
        testBigIntAddr(Address.wrap("0x4bec173f8d9d3d90188777CAFEBABEcafebabe99").address);
        testBigIntAddr(Address.wrap("0x5cafebabecafebabe7570ad8ac11f8d812ee0606").address);
        testBigIntAddr(Address.wrap("0x0000000005cafebabecafebabe7570ad8ac11f8d").address);
        testBigIntAddr(Address.wrap("0x0000000000000000000082095cafebabecafebab").address);

        TestUtils.assertThrown(IllegalArgumentException.class,
                "invalid bit length: 161",
                () -> Address.format(new BigInteger("182095cafebabecafebabe00083ce15d74e191051", 16))
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "invalid bit length: 164",
                () -> Address.format(new BigInteger("82095cafebabecafebabe00083ce15d74e1910510", 16))
        );

        final SecureRandom sr = new SecureRandom();
        sr.setSeed(new SecureRandom().generateSeed(64));
        sr.setSeed(sr.generateSeed(64));
        final AddressType type = TypeFactory.create("address");
        for (int i = 0; i < 500; i++) {
            testBigIntAddr(new BigInteger(type.bitLength, sr));
        }

        final Random r = new Random(sr.nextLong());
        for (int bitlen = 0; bitlen <= 160; bitlen++) {
            for (int i = 0; i < 10; i++) {
                testBigIntAddr(new BigInteger(bitlen, r));
            }
        }
        BigInteger temp;
        do {
            temp = new BigInteger(161, r);
        } while (temp.bitLength() < 161);
        final BigInteger tooBig = temp;
        TestUtils.assertThrown(IllegalArgumentException.class, "invalid bit length: 161", () -> Address.format(tooBig));
    }

    @Test
    public void testStringAddrs() throws Throwable {
        testStringAddr(Address.format(BigInteger.ZERO));
        testStringAddr(Address.format(BigInteger.ONE));
        testStringAddr(Address.format(BigInteger.TEN));
        testStringAddr(Address.format(BigInteger.valueOf(2L)));
        testStringAddr("0x82095cafebabecafebabe00083ce15d74e191051");
        testStringAddr("0x4bec173f8d9d3d90188777cafebabecafebabe99");
        testStringAddr("0x4bec173f8d9d3d90188777CAFEBABEcafebabe99");
        testStringAddr("0x5cafebabecafebabe7570ad8ac11f8d812ee0606");
        testStringAddr("0x0000000005cafebabecafebabe7570ad8ac11f8d");
        testStringAddr("0x0000000000000000000082095cafebabecafebab");
        testStringAddr("0xc0ec0fbb1c07aebe2a6975d50b5f6441b05023f9");
        testStringAddr("0xa62274005cafebabecafebabecaebb178db50ad6");
        testStringAddr("0xc6782c3a8155971a5d16005cafebabecafebabe8");

        TestUtils.assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 0",
                () -> Address.wrap("0x+000000000000000000082095cafebabecafebab")
        );

        TestUtils.assertThrown(IllegalArgumentException.class,
                "illegal hex val @ 0",
                () -> Address.wrap("0x-000000000000000000082095cafebabecafebab")
        );

        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected prefix 0x not found",
                () -> Address.wrap("aaaaa")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected prefix 0x not found",
                () -> Address.wrap("5cafebabecafebabe7570ad8ac11f8d812ee0606")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected address length: 42; actual: 41",
                () -> Address.wrap("0xa83aaef1b5c928162005cafebabecafebabecb0")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected address length: 42; actual: 43",
                () -> Address.wrap("0xa83aaef1b5c928162005cafebabecafebabecb0a0")
        );

        final byte[] _20 = new byte[20];
        final Random r = TestUtils.seededRandom();
        for (int i = 0; i < 1_000; i++) {
            testStringAddr(generateStringAddress(_20, r));
        }

        BigInteger _ffff = Address.wrap("0x000000000000000000000000000000000000ffff").address;
        assertEquals(BigInteger.valueOf(65535L), _ffff);

        BigInteger _8000 = Address.wrap("0x800000000000000000000000000000000000ffff").address;
        assertTrue(_8000.signum() > 0);
    }

    private static void testStringAddr(final String addrString) {
        assertTrue(Address.wrap(addrString).address.bitLength() <= 160);
    }

    private static String generateStringAddress(byte[] _20, Random r) {
        r.nextBytes(_20);
        return "0x" + Strings.encode(_20);
    }

    private static void testBigIntAddr(final BigInteger addr) {
        final String addrString = Address.format(addr);
        assertTrue(addrString.startsWith("0x"));
        assertEquals(Address.ADDRESS_STRING_LEN, addrString.length());
        assertEquals(new Address(addr), Address.wrap(addrString));
    }
}
