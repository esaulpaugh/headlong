package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.joemelsha.crypto.hash.Keccak;

import java.math.BigInteger;
import java.util.Locale;

public final class Address {

    public final BigInteger address;

    public static Address wrap(final String address) {
        final BigInteger result = toBigInt(address);
        if(format(result).equals(address)) {
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
        return format(address);
    }

    private static final int HEX_RADIX = 16;
    private static final int ADDRESS_HEX_CHARS = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
    public static final String HEX_PREFIX = "0x";
    public static final int ADDRESS_STRING_LEN = HEX_PREFIX.length() + ADDRESS_HEX_CHARS;
    private static final AddressType ADDRESS_TYPE = TypeFactory.create("address");

    public static String format(final BigInteger address) {
        final String result = toString(address);
        if(toBigInt(result).equals(address)) {
            return result;
        }
        throw new AssertionError();
    }

    private static String toString(final BigInteger address) {
        final String minimalHex = address.toString(HEX_RADIX);
        final int leftPad = ADDRESS_HEX_CHARS - minimalHex.length();
        if(leftPad < 0) {
            throw new IllegalArgumentException("invalid bit length: " + address.bitLength());
        }
        final StringBuilder addrBuilder = new StringBuilder(HEX_PREFIX);
        for (int i = 0; i < leftPad; i++) {
            addrBuilder.append('0');
        }
        final String result = addrBuilder.append(minimalHex).toString();
        if(result.length() == ADDRESS_STRING_LEN) {
            return toChecksumAddress(result);
        }
        throw new AssertionError();
    }

    private static BigInteger toBigInt(final String addrStr) {
        if(!addrStr.startsWith(HEX_PREFIX)) {
            throw new IllegalArgumentException("expected prefix 0x not found");
        }
        if(addrStr.length() != ADDRESS_STRING_LEN) {
            throw new IllegalArgumentException("expected address length: " + ADDRESS_STRING_LEN + "; actual: " + addrStr.length());
        }
        final String hex = addrStr.substring(HEX_PREFIX.length());
        FastHex.decode(hex); // check for non-hex chars
        requireValidChecksum(addrStr);
        final BigInteger address = new BigInteger(hex, HEX_RADIX);
        if(address.signum() < 0) {
            throw new AssertionError();
        }
        ADDRESS_TYPE.validate(new Address(address));
        AddressType.ADDRESS_INNER.validate(address);
        return address;
    }

    public static void requireValidChecksum(final String address) {
        if(toChecksumAddress(address).equals(address)) {
            return;
        }
        throw new IllegalArgumentException("invalid checksum");
    }

    public static String toChecksumAddress(String address) {
        address = address.toLowerCase(Locale.ENGLISH).replace(HEX_PREFIX, "");
        final String hash = Strings.encode(new Keccak(256).digest(Strings.decode(address, Strings.ASCII)), Strings.HEX);
        final StringBuilder ret = new StringBuilder(HEX_PREFIX);

        for (int i = 0; i < address.length(); i++) {
            if(Integer.parseInt(String.valueOf(hash.charAt(i)), HEX_RADIX) >= 8) {
                ret.append(Character.toUpperCase(address.charAt(i)));
            } else {
                ret.append(address.charAt(i));
            }
        }

        return ret.toString();
    }
}
