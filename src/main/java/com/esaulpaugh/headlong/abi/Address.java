package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;

import java.math.BigInteger;
import java.util.Locale;

public final class Address {

    public final BigInteger address;

    public static Address wrap(final String address) {
        final BigInteger result = _decodeAddr(address);
        if(_formatAddr(result).equals(address.toLowerCase(Locale.US))) {
            return new Address(result);
        }
        throw new AssertionError();
    }

    Address(BigInteger address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Address) {
            Address other = (Address) o;
            return address.equals(other.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return formatAddress(address);
    }

    private static final int HEX_RADIX = 16;
    private static final int ADDRESS_HEX_CHARS = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
    public static final String ADDRESS_PREFIX = "0x";
    public static final int ADDRESS_STRING_LEN = ADDRESS_PREFIX.length() + ADDRESS_HEX_CHARS;
    private static final AddressType ADDRESS_TYPE = TypeFactory.create("address");

    public static String formatAddress(final BigInteger address) {
        final String result = _formatAddr(address);
        if(_decodeAddr(result).equals(address)) {
            return result;
        }
        throw new AssertionError();
    }

    private static String _formatAddr(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int leftPad = ADDRESS_HEX_CHARS - minimalHex.length();
        if(leftPad < 0) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final StringBuilder addrBuilder = new StringBuilder(ADDRESS_PREFIX);
        for (int i = 0; i < leftPad; i++) {
            addrBuilder.append('0');
        }
        final String result = addrBuilder.append(minimalHex).toString();
        if(result.length() == ADDRESS_STRING_LEN) {
            return result;
        }
        throw new AssertionError();
    }

    private static BigInteger _decodeAddr(final String addrStr) {
        if(!addrStr.startsWith(ADDRESS_PREFIX)) {
            throw new IllegalArgumentException("expected prefix 0x not found");
        }
        if(addrStr.length() != ADDRESS_STRING_LEN) {
            throw new IllegalArgumentException("expected address length: " + ADDRESS_STRING_LEN + "; actual: " + addrStr.length());
        }
        final String hex = addrStr.substring(ADDRESS_PREFIX.length());
        FastHex.decode(hex); // check for non-hex chars
        final BigInteger address = new BigInteger(hex, HEX_RADIX);
        if(address.signum() < 0) {
            throw new AssertionError();
        }
        ADDRESS_TYPE.validate(new Address(address));
        return address;
    }
}
