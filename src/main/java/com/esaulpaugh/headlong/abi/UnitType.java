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

import com.esaulpaugh.headlong.util.Integers;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/** Superclass for any 256-bit ("unit") Contract ABI type. Usually numbers or boolean. Not for arrays or tuples. */
public abstract class UnitType<J> extends ABIType<J> { // J generally extends Number or is Boolean

    public static final int UNIT_LENGTH_BYTES = 256 / Byte.SIZE;

    final int bitLength;
    final boolean unsigned;
    private final BigInteger min;
    private final BigInteger max;
    private final long minLong;
    private final long maxLong;
    private final int nLZ;

    UnitType(String canonicalType, Class<J> clazz, int bitLength, boolean unsigned) {
        super(canonicalType, clazz, false);
        this.bitLength = bitLength;
        this.unsigned = unsigned;
        this.nLZ = Long.SIZE - bitLength;
        this.min = minValue();
        this.max = maxValue();
        this.minLong = this.min.longValue();
        this.maxLong = this.max.longValue();
        final Class<?> c = this.getClass();
        if (
                c == BigDecimalType.class
                        || c == BooleanType.class
                        || c == IntType.class
                        || c == LongType.class
                        || c == BigIntegerType.class
                        || c == AddressType.class
        ) {
            return;
        }
        throw new AssertionError("unexpected subclass");
    }

    public final int getBitLength() {
        return bitLength;
    }

    public final boolean isUnsigned() {
        return unsigned;
    }

    public final BigInteger minValue() {
        return unsigned ? BigInteger.ZERO : BigInteger.valueOf(2L).pow(bitLength - 1).negate();
    }

    public final BigInteger maxValue() {
        return BigInteger.valueOf(2L)
                .pow(unsigned ? bitLength : bitLength - 1)
                .subtract(BigInteger.ONE);
    }

    @Override
    int headLength() {
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int byteLength(J value) {
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int byteLengthPacked(J value) {
        return bitLength / Byte.SIZE;
    }

    @Override
    public final int validate(J value) {
        validateClass(value);
        return validateInternal(value);
    }

    int validateInternal(J value) {
        return validatePrimitive(toLong(value));
    }

    @Override
    void encodeTail(J value, ByteBuffer dest) {
        insertInt(toLong(value), dest);
    }

    @Override
    void encodePackedUnchecked(J value, ByteBuffer dest) {
        LongType.encodeLong(toLong(value), byteLengthPacked(null), dest);
    }

    private static long toLong(Object value) {
        return ((Number) value).longValue();
    }

    final long decodeUnsignedLong(ByteBuffer bb) {
        final long a = bb.getLong(), b = bb.getLong(), c = bb.getLong(), d = bb.getLong();
        if ((a | b | c) == 0L && d >= 0L && d <= maxLong) {
            return d;
        }
        throw err(bb);
    }

    final long decodeSignedLong(ByteBuffer bb) {
        final long a = bb.getLong(), b = bb.getLong(), c = bb.getLong(), d = bb.getLong();
        if ((a | b | c) == 0L) {
            if (Long.numberOfLeadingZeros(d) > nLZ) {
                return d;
            }
        } else if ((a & b & c) == -1L && Long.numberOfLeadingZeros(~d) > nLZ) {
            return d;
        }
        throw err(bb);
    }

    final IllegalArgumentException err(ByteBuffer bb) {
        bb.position(bb.position() - UNIT_LENGTH_BYTES);
        decodeValid(bb, ABIType.newUnitBuffer());
        throw new AssertionError();
    }

    final int validatePrimitive(long longVal) {
        if (longVal < minLong) {
            throw negative(Integers.bitLen(~longVal));
        }
        if (longVal > maxLong) {
            throw nonNegative(Integers.bitLen(longVal));
        }
        return UNIT_LENGTH_BYTES;
    }

    final int validateBigInt(BigInteger bigIntVal) {
        if (bigIntVal.compareTo(min) < 0) {
            throw negative(bigIntVal.bitLength());
        }
        if (bigIntVal.compareTo(max) > 0) {
            throw nonNegative(bigIntVal.bitLength());
        }
        return UNIT_LENGTH_BYTES;
    }

    private IllegalArgumentException negative(int actual) {
        if (unsigned) {
            return new IllegalArgumentException("signed value given for unsigned type");
        }
        if (actual >= bitLength) {
            return new IllegalArgumentException("signed val exceeds bit limit: " + actual + " >= " + bitLength);
        }
        throw new AssertionError();
    }

    private IllegalArgumentException nonNegative(int actual) {
        if (unsigned) {
            if (actual > bitLength) {
                return new IllegalArgumentException("unsigned val exceeds bit limit: " + actual + " > " + bitLength);
            }
            throw new AssertionError();
        }
        if (actual >= bitLength) {
            return new IllegalArgumentException("signed val exceeds bit limit: " + actual + " >= " + bitLength);
        }
        throw new AssertionError();
    }

    final BigInteger decodeValid(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = unsigned ? new BigInteger(1, unitBuffer) : new BigInteger(unitBuffer);
        validateBigInt(bi);
        return bi;
    }
}
