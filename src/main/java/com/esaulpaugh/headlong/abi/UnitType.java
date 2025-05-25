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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_ARRAY_CLASS;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS;

/** Superclass for any 256-bit ("unit") Contract ABI type. Usually numbers or boolean. Not for arrays or tuples. */
public abstract class UnitType<J> extends ABIType<J> { // J generally extends Number or is Boolean or Address

    // 69 non-BigDecimalType entries in BASE_TYPE_MAP
    // - 3 which are only aliases to instances already counted (int, uint, decimal)
    // + 0 unique instances in LEGACY_BASE_TYPE_MAP
    // + 3 instances not in the maps (uint21, uint31, and ADDRESS_INNER)
    // =
    private static final long INSTANCE_LIMIT = 69L;
    private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);

    static final int UNIT_LENGTH_BITS = 256;
    public static final int UNIT_LENGTH_BYTES = UNIT_LENGTH_BITS / Byte.SIZE;

    private final long minLong;
    private final long maxLong;
    private final BigInteger min;
    private final BigInteger max;
    final int bitLength;
    final boolean unsigned;

    UnitType(String canonicalType, Class<J> clazz, int bitLength, boolean unsigned) {
        super(canonicalType, clazz, false);
        if (!(this instanceof BigDecimalType)) {
            if (INSTANCE_COUNT.incrementAndGet() > INSTANCE_LIMIT) {
                INSTANCE_COUNT.decrementAndGet();
                throw new IllegalStateException("instance not permitted");
            }
        } else if (bitLength > UNIT_LENGTH_BITS) {
            bitLength = UNIT_LENGTH_BITS;
        } else if (bitLength < Byte.SIZE || Integers.mod(bitLength, Byte.SIZE) != 0) {
            throw new IllegalStateException("bit length not permitted");
        }
        this.bitLength = bitLength;
        this.unsigned = unsigned;
        this.min = minValue();
        this.max = maxValue();
        this.minLong = this.min.longValue();
        this.maxLong = this.max.longValue();
    }

    public final int getBitLength() {
        return bitLength;
    }

    public final boolean isUnsigned() {
        return unsigned;
    }

    public final BigInteger minValue() {
        return unsigned ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(bitLength - 1).negate();
    }

    public final BigInteger maxValue() {
        return BigInteger.ONE
                .shiftLeft(unsigned ? bitLength : bitLength - 1)
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
        if ((a | b | c | (d & (-1L << bitLength))) == 0L) {
            return d;
        }
        throw err(bb);
    }

    final long decodeSignedLong(ByteBuffer bb) {
        final long a = bb.getLong(), b = bb.getLong(), c = bb.getLong(), d = bb.getLong();
        if ((a | b | c) == 0L) {
            if (Long.numberOfLeadingZeros(d) - (Long.SIZE - bitLength) > 0) {
                return d;
            }
        } else if ((a & b & c) == -1L && Long.numberOfLeadingZeros(~d) - (Long.SIZE - bitLength) > 0) {
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
        } else if (actual >= bitLength) {
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
//======================================================================================================================
    private static final Map<CharSequenceView, ABIType<?>> BASE_TYPE_MAP = new HashMap<>(256);
    private static final Map<CharSequenceView, ABIType<?>> LEGACY_BASE_TYPE_MAP = new HashMap<>(256);

    /* called from TypeFactory */
    static ABIType<?> get(String rawType) {
        return get(new CharSequenceView(rawType));
    }

    /* called from TypeFactory */
    static ABIType<?> getLegacy(String rawType) {
        return getLegacy(new CharSequenceView(rawType));
    }

    /* called from TypeFactory */
    static ABIType<?> get(CharSequenceView view) {
        return BASE_TYPE_MAP.get(view);
    }

    /* called from TypeFactory */
    static ABIType<?> getLegacy(CharSequenceView slice) {
        return LEGACY_BASE_TYPE_MAP.get(slice);
    }

    private static final AtomicBoolean MAP_INITIALIZED = new AtomicBoolean(false);

    static void initInstances() {
        if (MAP_INITIALIZED.compareAndSet(false, true)) {
//            assert BASE_TYPE_MAP.isEmpty();
//            assert LEGACY_BASE_TYPE_MAP.isEmpty();
//            assert INSTANCE_COUNT.get() >= 0L; // the number of static instances in whichever subclass is initialized first
//            assert INSTANCE_COUNT.get() <= 2L;

            // optimized insertion order
            map("bool", BooleanType.INSTANCE);
            map("string", new ArrayType<ByteType, Byte, String>("string", STRING_CLASS, ByteType.INSTANCE, DYNAMIC_LENGTH, STRING_ARRAY_CLASS, ABIType.FLAGS_NONE));

            for (int n = 1; n <= 32; n++) {
                mapByteArray("bytes" + n, n);
            }
            mapByteArray("function", 24);
            mapByteArray("bytes", DYNAMIC_LENGTH);

            for (int n = 8; n <= 24; n += 8) mapInt("uint" + n, n, true); // will trigger IntType initialization, which will init UINT21 and UINT31
            for (int n = 32; n <= 56; n += 8) mapLong("uint" + n, n, true);
            for (int n = 64; n <= 256; n += 8) mapBigInteger("uint" + n, n, true);

            map("uint", get("uint256"));

            mapBigInteger("int256", 256, false);
            map("int", get("int256"));

            for (int n = 8; n <= 32; n += 8) mapInt("int" + n, n, false);
            for (int n = 40; n <= 64; n += 8) mapLong("int" + n, n, false);
            for (int n = 72; n < 256; n += 8) mapBigInteger("int" + n, n, false);

            map("address", AddressType.INSTANCE);

            map("fixed128x18", new BigDecimalType("fixed128x18", 128, 18, false));
            map("ufixed128x18", new BigDecimalType("ufixed128x18", 128, 18, true));

            map("decimal", get("int168"));
            map("fixed", get("fixed128x18"));
            map("ufixed", get("ufixed128x18"));

            for (Map.Entry<CharSequenceView, ABIType<?>> e : BASE_TYPE_MAP.entrySet()) {
                ABIType<?> value = e.getValue();
                if (value instanceof ArrayType) {
                    final ArrayType<?, ?, ?> at = value.asArrayType();
                    value = new ArrayType<>(at.canonicalType, at.clazz, ByteType.INSTANCE, at.getLength(), at.arrayClass(), ABIType.FLAG_LEGACY_DECODE);
                }
                LEGACY_BASE_TYPE_MAP.put(e.getKey(), value);
            }

//            assert BASE_TYPE_MAP.size() == 108;
//            assert LEGACY_BASE_TYPE_MAP.size() == 108;
//            assert INSTANCE_COUNT.get() == INSTANCE_LIMIT;
        }
    }

    private static void map(String key, ABIType<?> value) {
        BASE_TYPE_MAP.put(new CharSequenceView(key), value);
    }

    private static void mapInt(String type, int bitLen, boolean unsigned) {
        map(type, new IntType(type, bitLen, unsigned));
    }

    private static void mapLong(String type, int bitLen, boolean unsigned) {
        map(type, new LongType(type, bitLen, unsigned));
    }

    private static void mapBigInteger(String type, int bitLen, boolean unsigned) {
        map(type, new BigIntegerType(type, bitLen, unsigned));
    }

    private static void mapByteArray(String type, int arrayLen) {
        map(type, new ArrayType<ByteType, Byte, byte[]>(type, byte[].class, ByteType.INSTANCE, arrayLen, byte[][].class, ABIType.FLAGS_NONE));
    }
}
