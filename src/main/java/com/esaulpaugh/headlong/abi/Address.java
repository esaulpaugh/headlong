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
        final BigInteger value = to_big_int(checksumAddress);
        if(toChecksumAddress(value).equals(checksumAddress)) { // sanity check
            return new Address(value);
        }
        throw new AssertionError();
    }

    public static void validateChecksumAddress(final String checksumAddress) {
        validateChecksumAddress(checksumAddress, new Keccak(256));
    }

    private static void validateChecksumAddress(final String checksumAddress, final Keccak k) {
        if(raw_to_checksummed(checksumAddress, k).equals(checksumAddress)) {
            return;
        }
        throw new IllegalArgumentException("invalid checksum");
    }

    public static String toChecksumAddress(final String address) {
        final Keccak k = new Keccak(256);
        final String out = raw_to_checksummed(address, k);
        validateChecksumAddress(out, k); // sanity check
        return out;
    }

    public static String toChecksumAddress(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int leftPad = ADDRESS_HEX_CHARS - minimalHex.length();
        if(leftPad < 0) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final StringBuilder addrBuilder = new StringBuilder(HEX_PREFIX);
        for (int i = 0; i < leftPad; i++) {
            addrBuilder.append('0');
        }
        final String rawAddress = addrBuilder.append(minimalHex).toString();
        final String checksumAddress = toChecksumAddress(rawAddress);
        // sanity checks
        if(rawAddress.length() == ADDRESS_STRING_LEN
                && checksumAddress.length() == ADDRESS_STRING_LEN
                && to_big_int(checksumAddress).equals(address)) {
            return checksumAddress;
        }
        throw new AssertionError();
    }

    private static BigInteger to_big_int(final String validated) {
        return new BigInteger(validated.substring(HEX_PREFIX.length()), HEX_RADIX);
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
    private static String raw_to_checksummed(final String address, final Keccak k) {
        checkRawAddress(address);
        final String lowercaseAddr = toLowercaseAscii(address);
        k.update(lowercaseAddr.getBytes(StandardCharsets.US_ASCII), HEX_PREFIX.length(), ADDRESS_STRING_LEN - HEX_PREFIX.length());
        final byte[] digest = k.digest();
        final String hash = FastHex.encodeToString(digest);
        final byte[] ret = new byte[] {'0', 'x',
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        for (int i = HEX_PREFIX.length(); i < ret.length; i++) {
            int a = lowercaseAddr.charAt(i);
            ret[i] = (byte) (Integer.parseInt(String.valueOf(hash.charAt(i - HEX_PREFIX.length())), HEX_RADIX) >= 8
                    ? Character.toUpperCase(a)
                    : a);
        }
        final String out = new String(ret, 0, 0, ret.length);
        if(!toLowercaseAscii(out).equals(lowercaseAddr)) { // sanity check
            throw new AssertionError();
        }
        return out;
    }

    @SuppressWarnings("deprecation")
    private static String toLowercaseAscii(String str) {
        final int len = str.length();
        final byte[] ascii = new byte[len];
        for (int i = 0; i < len; i++) {
            ascii[i] = (byte) Character.toLowerCase((int) str.charAt(i));
        }
        return new String(ascii, 0, 0, ascii.length);
    }
}
