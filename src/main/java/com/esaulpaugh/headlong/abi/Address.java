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
import java.security.DigestException;
import java.util.Arrays;

/**
 * Provides type safety by disambiguating address arguments from {@link String} and {@link BigInteger} while imposing
 * certain format requirements such as <a href="https://eips.ethereum.org/EIPS/eip-55">EIP-55: Mixed-case checksum address encoding</a>.
 */
public final class Address {

    static final int ADDRESS_BIT_LEN = 160;
    private static final int ADDRESS_DATA_BYTES = ADDRESS_BIT_LEN / Byte.SIZE;
    private static final int ADDRESS_HEX_CHARS = ADDRESS_DATA_BYTES * FastHex.CHARS_PER_BYTE;
    private static final int PREFIX_LEN = 2;
    private static final int ADDRESS_LEN_CHARS = PREFIX_LEN + ADDRESS_HEX_CHARS;
    private static final int HEX_RADIX = 16;
    public static final int MAX_LABEL_LEN = 36;

    private final BigInteger value;

    /**
     * An informational String identifying or describing this Address.
     */
    private final String label;

    Address(BigInteger value) {
        this(value, null);
    }

    private Address(BigInteger value, String label) {
        this.value = value;
        this.label = label;
    }

    public BigInteger value() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Address) {
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
        return new Address(validateAndDecodeAddress(checksumAddress));
    }

    public static Address wrap(final String checksumAddress, final String label) {
        if (label != null && label.length() > MAX_LABEL_LEN) {
            throw new IllegalArgumentException("label length exceeds maximum: " + label.length() + " > " + MAX_LABEL_LEN);
        }
        return new Address(validateAndDecodeAddress(checksumAddress), label);
    }

    private static BigInteger validateAndDecodeAddress(final String checksumAddress) {
        validateChecksumAddress(checksumAddress);
        return new BigInteger(1, FastHex.decode(checksumAddress, PREFIX_LEN, ADDRESS_HEX_CHARS));
    }

    public Address withLabel(final String label) {
        if (this.label != null) {
            throw new IllegalArgumentException("labeling aborted because existing label not null");
        }
        return new Address(this.value, label);
    }

    public static void validateChecksumAddress(final String checksumAddress) {
        if (toChecksumAddress(checksumAddress).equals(checksumAddress)) {
            return;
        }
        throw new IllegalArgumentException("invalid checksum");
    }

    @SuppressWarnings("deprecation")
    public static String toChecksumAddress(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int len = minimalHex.length();
        final int start = ADDRESS_LEN_CHARS - len;
        if (start < PREFIX_LEN) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final byte[] addressBytes = new byte[ADDRESS_LEN_CHARS];
        Arrays.fill(addressBytes, (byte)'0');
        addressBytes[1] = 'x';
        minimalHex.getBytes(0, len, addressBytes, start);
        return doChecksum(addressBytes);
    }

    private static final byte[] LOWERCASE = new byte[1 << Byte.SIZE];
    private static final byte[] UPPERCASE = new byte[1 << Byte.SIZE];
    static {
        String lowers = "abcdef";
        String uppers = "ABCDEF";
        String nums = "0123456789";
        for (int i = 0; i < nums.length(); i++) {
            int d = nums.charAt(i);
            LOWERCASE[d] = (byte) d;
            UPPERCASE[d] = (byte) d;
            if (i < lowers.length()) {
                int lo = lowers.charAt(i);
                int hi = uppers.charAt(i);
                LOWERCASE[lo] = (byte) lo;
                LOWERCASE[hi] = (byte) lo;
                UPPERCASE[lo] = (byte) hi;
                UPPERCASE[hi] = (byte) hi;
            }
        }
    }

    /**
     * @see <a href="https://github.com/ethereum/ercs/blob/master/ERCS/erc-55.md">EIP-55</a>
     * @param address   the hexadecimal Ethereum address
     * @return  the same address with the correct EIP-55 checksum casing
     */
    public static String toChecksumAddress(final String address) {
        if (address.length() == ADDRESS_LEN_CHARS) {
            checkPrefix(address);
            final byte[] addressBytes = new byte[ADDRESS_LEN_CHARS];
            addressBytes[0] = '0';
            addressBytes[1] = 'x';
            for (int i = PREFIX_LEN; i < addressBytes.length; i++) {
                final byte val = LOWERCASE[address.charAt(i)];
                if (val == 0) throw new IllegalArgumentException("illegal hex val @ " + i);
                addressBytes[i] = val;
            }
            return doChecksum(addressBytes);
        }
        if (address.length() >= PREFIX_LEN) {
            checkPrefix(address);
        }
        throw new IllegalArgumentException("expected address length " + ADDRESS_LEN_CHARS + "; actual is " + address.length());
    }

    private static void checkPrefix(String address) {
        if (!address.startsWith("0x")) {
            throw new IllegalArgumentException("missing 0x prefix");
        }
    }

    @SuppressWarnings("deprecation")
    private static String doChecksum(final byte[] addressBytes) {
        final Keccak keccak256 = new Keccak(256);
        keccak256.update(addressBytes, PREFIX_LEN, ADDRESS_HEX_CHARS);
        final byte[] hash = keccak256.digest();
        for (int b = 0, c = PREFIX_LEN; b < ADDRESS_DATA_BYTES; b++) {
            final byte hashByte = hash[b];
            if ((hashByte >>> 4) >= 8) {
                addressBytes[c] = UPPERCASE[addressBytes[c]];
            }
            c++;
            if ((hashByte & 0xF) >= 8) {
                addressBytes[c] = UPPERCASE[addressBytes[c]];
            }
            c++;
        }
        return new String(addressBytes, 0, 0, addressBytes.length);
    }
}
