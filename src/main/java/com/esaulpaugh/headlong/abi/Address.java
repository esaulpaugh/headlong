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

import com.esaulpaugh.headlong.util.FastHex;
import com.joemelsha.crypto.hash.Keccak;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public final class Address {

    private static final String HEX_PREFIX = "0x";
    private static final int PREFIX_LEN = 2;
    private static final int ADDRESS_HEX_CHARS = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
    private static final int HEX_RADIX = 16;

    private final BigInteger value;

    Address(BigInteger value) {
        this.value = value;
    }

    public BigInteger value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Address) {
            Address other = (Address) o;
            return value.equals(other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toChecksumAddress(value);
    }

    public static Address wrap(final String checksumAddress) {
        validateChecksumAddress(checksumAddress);
        return new Address(new BigInteger(checksumAddress.substring(PREFIX_LEN), HEX_RADIX));
    }

    public static void validateChecksumAddress(final String checksumAddress) {
        if(toChecksumAddress(checksumAddress).equals(checksumAddress)) {
            return;
        }
        throw new IllegalArgumentException("invalid checksum");
    }

    public static String toChecksumAddress(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int leftPad = ADDRESS_HEX_CHARS - minimalHex.length();
        if(leftPad < 0) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final StringBuilder rawAddress = new StringBuilder(HEX_PREFIX);
        for (int i = 0; i < leftPad; i++) {
            rawAddress.append('0');
        }
        return toChecksumAddress(rawAddress.append(minimalHex).toString());
    }

    /**
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md#implementation">EIP-55</a>
     * @param address   the hexadecimal Ethereum address
     * @return  the same address with the correct EIP-55 checksum casing
     */
    @SuppressWarnings("deprecation")
    public static String toChecksumAddress(final String address) {
        final int addressLen = PREFIX_LEN + ADDRESS_HEX_CHARS;
        if(address.charAt(0) != '0' || address.charAt(1) != 'x') {
            throw new IllegalArgumentException("missing 0x prefix");
        }
        if(address.length() != addressLen) {
            throw new IllegalArgumentException("expected address length " + addressLen + "; actual is " + address.length());
        }
        final byte[] ret = new byte[addressLen];
        ret[0] = '0';
        ret[1] = 'x';
        final byte[] hash = lowercaseAndHash(address, ret);
        for (int i = PREFIX_LEN; i < ret.length; i++) {
            final int c = ret[i];
            requireIsHex(c, i);
            ret[i] = (byte) (isHigh(hash[i]) ? Character.toUpperCase(c) : c);
        }
        return new String(ret, 0, 0, ret.length);
    }

    private static byte[] lowercaseAndHash(final String address, byte[] out) {
        for (int i = PREFIX_LEN; i < out.length; i++) {
            out[i] = (byte) Character.toLowerCase(address.charAt(i));
        }
        final Keccak keccak256 = new Keccak(256);
        keccak256.update(out, PREFIX_LEN, ADDRESS_HEX_CHARS);
        final int offset = PREFIX_LEN / FastHex.CHARS_PER_BYTE; // offset by one byte so the indices of the hex-encoded hash and the address ascii line up
        final ByteBuffer digest = ByteBuffer.wrap(new byte[offset + 256 / Byte.SIZE], offset, 32);
        keccak256.digest(digest);
        final byte[] digestBytes = digest.array();
        return FastHex.encodeToBytes(digestBytes, 0, digestBytes.length);
    }

    private static void requireIsHex(int c, int idx) {
        switch (c) {
        case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
        case 'A':case 'B':case 'C':case 'D':case 'E':case 'F':
        case 'a':case 'b':case 'c':case 'd':case 'e':case 'f': return;
        default:
        }
        throw new IllegalArgumentException("illegal hex val @ " + idx);
    }

    private static boolean isHigh(int c) {
        switch (c) {
        case '8':case '9':case 'a':case 'b':case 'c':case 'd':case 'e':case 'f': return true;
        default: return false;
        }
    }
}
