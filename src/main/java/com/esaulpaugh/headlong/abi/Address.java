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
import java.nio.charset.StandardCharsets;

public final class Address {

    private static final int HEX_RADIX = 16;
    private static final int ADDRESS_HEX_CHARS = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
    public static final String HEX_PREFIX = "0x";
    public static final int ADDRESS_STRING_LEN = HEX_PREFIX.length() + ADDRESS_HEX_CHARS;

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
        return new Address(new BigInteger(checksumAddress.substring(HEX_PREFIX.length()), HEX_RADIX));
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

    private static void checkRawAddress(final String address) {
        if(!address.startsWith(HEX_PREFIX)) {
            throw new IllegalArgumentException("expected prefix 0x not found");
        }
        if(address.length() != ADDRESS_STRING_LEN) {
            throw new IllegalArgumentException("expected address length " + ADDRESS_STRING_LEN + "; actual is " + address.length());
        }
        FastHex.decode(address, HEX_PREFIX.length(), address.length() - HEX_PREFIX.length()); // check for non-hex chars
    }

    /**
     * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md#implementation">EIP-55</a>
     * @param address   the hexadecimal Ethereum address
     * @return  the same address with the correct EIP-55 checksum casing
     */
    @SuppressWarnings("deprecation")
    public static String toChecksumAddress(final String address) {
        checkRawAddress(address);
        final byte[] ascii = new byte[Address.ADDRESS_STRING_LEN];
        for (int i = 0; i < ascii.length; i++) {
            ascii[i] = (byte) Character.toLowerCase((int) address.charAt(i));
        }
        final Keccak keccak256 = new Keccak(256);
        keccak256.update(ascii, HEX_PREFIX.length(), ADDRESS_STRING_LEN - HEX_PREFIX.length());
        final ByteBuffer digest = ByteBuffer.wrap(new byte[33], 1, 32);
        keccak256.digest(digest);
        final String hash = FastHex.encodeToString(digest.array());
        final byte[] ret = new byte[Address.ADDRESS_STRING_LEN];
        ret[0] = '0';
        ret[1] = 'x';
        for (int i = 2; i < ret.length; i++) {
            int a = ascii[i];
            ret[i] = (byte) (isHigh(hash.charAt(i)) ? Character.toUpperCase(a) : a);
        }
        return new String(ret, 0, 0, ret.length);
    }

    private static boolean isHigh(int c) {
        switch (c) {
        case '8':case '9':case 'a':case 'b':case 'c':case 'd':case 'e':case 'f': return true;
        default: return false;
        }
    }
}
