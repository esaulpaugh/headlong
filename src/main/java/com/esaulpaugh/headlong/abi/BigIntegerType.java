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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/** Represents an integer type such as uint64 or int256. */
public final class BigIntegerType extends UnitType<BigInteger> {

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, BigInteger.class, bitLength, unsigned);
    }

    @Override
    Class<?> arrayClass() {
        return BigInteger[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BIG_INTEGER;
    }

    @Override
    public int validate(BigInteger value) {
        return validateBigInt(value);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        Encoding.insertInt((BigInteger) value, UNIT_LENGTH_BYTES, dest);
    }

    @Override
    void encodePackedUnchecked(BigInteger value, ByteBuffer dest) {
        Encoding.insertInt(value, byteLengthPacked(null), dest);
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) {
        return decodeValid(bb, unitBuffer);
    }

    @Override
    public BigInteger parseArgument(String s) {
        BigInteger bigInt = new BigInteger(s);
        validate(bigInt);
        return bigInt;
    }

    private static final int HEX_RADIX = 16;
    private static final int ADDRESS_HEX_CHARS = TypeFactory.ADDRESS_BIT_LEN / FastHex.BITS_PER_CHAR;
    public static final String ADDRESS_PREFIX = "0x";
    public static final int ADDRESS_STRING_LEN = ADDRESS_PREFIX.length() + ADDRESS_HEX_CHARS;
    private static final BigIntegerType ADDRESS_TYPE = TypeFactory.create("address");

    public static String formatAddress(final BigInteger address) {
        final String result = _formatAddr(address);
        if(_decodeAddr(result).equals(address)) {
            return result;
        }
        throw new AssertionError();
    }

    public static BigInteger decodeAddress(final String address) {
        final BigInteger result = _decodeAddr(address);
        if(_formatAddr(result).equals(address)) {
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
        ADDRESS_TYPE.validate(address);
        return address;
    }
}
