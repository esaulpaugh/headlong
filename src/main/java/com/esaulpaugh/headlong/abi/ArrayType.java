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
import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.SuperSerial;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

/**
 * Represents static array types such as bytes3 or uint16[3][2] and dynamic array types such as decimal[5][] or
 * string[4].
 *
 * @param <E> the {@link ABIType} for the elements of the array
 * @param <J> this {@link ArrayType}'s corresponding Java type
 */
public final class ArrayType<E extends ABIType<?>, J> extends ABIType<J> {

    static final Class<byte[]> BYTE_ARRAY_CLASS = byte[].class;
    static final String BYTE_ARRAY_ARRAY_CLASS_NAME = byte[][].class.getName();

    static final Class<String> STRING_CLASS = String.class;
    static final String STRING_ARRAY_CLASS_NAME = String[].class.getName();

    private static final IntType ARRAY_LENGTH_TYPE = new IntType("int32", Integer.SIZE, false);
    private static final int ARRAY_LENGTH_BYTE_LEN = UNIT_LENGTH_BYTES;

    static final int DYNAMIC_LENGTH = -1;

    final E elementType;
    final int length;
    private final boolean isString;

    private final String arrayClassName;

    ArrayType(String canonicalType, Class<J> clazz, boolean dynamic, E elementType, int length, String arrayClassName) {
        super(canonicalType, clazz, dynamic);
        this.elementType = elementType;
        this.length = length;
        this.arrayClassName = arrayClassName;
        this.isString = clazz == STRING_CLASS;
    }

    public E getElementType() {
        return elementType;
    }

    public int getLength() {
        return length;
    }

    public boolean isString() {
        return isString;
    }

    @Override
    String arrayClassName() {
        return arrayClassName;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_ARRAY;
    }

    /**
     * LOG_2_UNIT_LENGTH_BYTES == 5
     * x << 5 == x * 32
     *
     * @param value the value to measure
     * @return the length in bytes of this array when encoded
     */
    @Override
    int byteLength(Object value) {
        final int len;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: len = ((boolean[]) value).length * UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE: len = Integers.roundLengthUp(byteCount(value), UNIT_LENGTH_BYTES); break;
        case TYPE_CODE_INT: len = ((int[]) value).length * UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_LONG: len = ((long[]) value).length * UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: len = ((Number[]) value).length * UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: len = calcObjArrByteLen((Object[]) value); break;
        default: throw new Error();
        }
        // arrays with variable number of elements get +32 for the array length
        return length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_BYTE_LEN + len
                : len;
    }

    private int calcObjArrByteLen(Object[] elements) {
        int len = 0;
        for (Object element : elements) {
            len += elementType.byteLength(element);
        }
        return !elementType.dynamic
                ? len
                : len + (elements.length * UNIT_LENGTH_BYTES); // 32 bytes per offset
    }

    private int staticByteLengthPacked() {
        if(length != DYNAMIC_LENGTH) {
            return length * elementType.byteLengthPacked(null);
        }
        throw new IllegalArgumentException("array of dynamic elements");
    }

    @Override
    int byteLengthPacked(Object value) {
        if(value == null) {
            return staticByteLengthPacked();
        }
        final ABIType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return ((boolean[]) value).length; // * 1
        case TYPE_CODE_BYTE: return byteCount(value); // * 1
        case TYPE_CODE_INT: return ((int[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_LONG: return ((long[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: return ((Number[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return calcObjArrPackedByteLen((Object[]) value);
        default: throw new Error();
        }
    }

    private int calcObjArrPackedByteLen(Object[] elements) {
        int packedLen = 0;
        for (Object element : elements) {
            packedLen += elementType.byteLengthPacked(element);
        }
        return packedLen;
    }

    private int byteCount(Object value) {
        return ((byte[]) decodeIfString(value)).length;
    }

    Object decodeIfString(Object value) {
        return !isString ? value : Strings.decode((String) value, UTF_8);
    }

    Object encodeIfString(byte[] bytes) {
        return !isString ? bytes : Strings.encode(bytes, UTF_8);
    }

    @Override
    public int validate(final Object value) {
        validateClass(value);

        final int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: staticLen = checkLength(((boolean[]) value).length, value) * UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE: staticLen = Integers.roundLengthUp(checkLength(byteCount(value), value), UNIT_LENGTH_BYTES); break;
        case TYPE_CODE_INT: staticLen = validateIntArray((int[]) value); break;
        case TYPE_CODE_LONG: staticLen = validateLongArray((long[]) value); break;
        case TYPE_CODE_BIG_INTEGER: staticLen = validateBigIntegerArray((BigInteger[]) value); break;
        case TYPE_CODE_BIG_DECIMAL: staticLen = validateBigDecimalArray((BigDecimal[]) value); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: staticLen = validateObjectArray((Object[]) value); break;
        default: throw new Error();
        }
        // arrays with variable number of elements get +32 for the array length
        return length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_BYTE_LEN + staticLen
                : staticLen;
    }

    private int validateIntArray(final int[] arr) {
        final IntType intType = (IntType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                intType.validatePrimitive(arr[i]); // validate without boxing primitive
            }
        } catch (IllegalArgumentException iae) {
            throw abiException(iae, i);
        }
        return len * UNIT_LENGTH_BYTES;
    }

    private int validateLongArray(final long[] arr) {
        final LongType longType = (LongType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                longType.validatePrimitive(arr[i]); // validate without boxing primitive
            }
        } catch (IllegalArgumentException iae) {
            throw abiException(iae, i);
        }
        return len * UNIT_LENGTH_BYTES;
    }

    private int validateBigIntegerArray(BigInteger[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        BigIntegerType bigIntegerType = (BigIntegerType) elementType;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                bigIntegerType.validateBigInt(arr[i]);
            }
        } catch (IllegalArgumentException iae) {
            throw abiException(iae, i);
        }
        return len * UNIT_LENGTH_BYTES;
    }

    private int validateBigDecimalArray(BigDecimal[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        BigDecimalType bigDecimalType = (BigDecimalType) elementType;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                BigDecimal element = arr[i];
                bigDecimalType.validateBigInt(element.unscaledValue());
                if(element.scale() != bigDecimalType.scale) {
                    throw new IllegalArgumentException(String.format(BigDecimalType.ERR_SCALE_MISMATCH, element.scale(), bigDecimalType.scale));
                }
            }
        } catch (IllegalArgumentException iae) {
            throw abiException(iae, i);
        }
        return len * UNIT_LENGTH_BYTES;
    }

    private static IllegalArgumentException abiException(IllegalArgumentException iae, int i) {
        return new IllegalArgumentException("array index " + i + ": " + iae.getMessage());
    }

    /** For arrays of arrays or arrays of tuples only. */
    private int validateObjectArray(Object[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int byteLength = !elementType.dynamic ? 0 : len * UNIT_LENGTH_BYTES; // when dynamic, 32 bytes per offset
        for (int i = 0; i < len; i++) {
            byteLength += elementType.validate(arr[i]);
        }
        return byteLength;
    }

    private int checkLength(final int valueLength, Object value) {
        if(length == DYNAMIC_LENGTH || length == valueLength) {
            return valueLength;
        }
        throw new IllegalArgumentException(
                Utils.friendlyClassName(value.getClass(), valueLength)
                        + " not instanceof " + Utils.friendlyClassName(clazz, length) + ", " +
                        valueLength + " != " + length
        );
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        encodeArrayTail(decodeIfString(value), dest);
    }

    private void insert(Supplier<Integer> supplyLength, Runnable insert, ByteBuffer dest) {
        if(length == DYNAMIC_LENGTH) {
            Encoding.insertInt(supplyLength.get(), dest);
        }
        insert.run();
    }

    private void encodeArrayTail(Object v, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: boolean[] z = (boolean[])v; insert(() -> z.length, () -> insertBooleans(z, dest), dest); return;
        case TYPE_CODE_BYTE: byte[] b = (byte[])v; insert(() -> b.length, () -> Encoding.insertBytesPadded(b, dest), dest); return;
        case TYPE_CODE_INT: int[] i = (int[])v; insert(() -> i.length, () -> insertInts(i, dest), dest); return;
        case TYPE_CODE_LONG: long[] j = (long[])v; insert(() -> j.length, () -> insertLongs(j, dest), dest); return;
        case TYPE_CODE_BIG_INTEGER: BigInteger[] bi = (BigInteger[])v; insert(() -> bi.length, () -> Encoding.insertBigIntegers(bi, UNIT_LENGTH_BYTES, dest), dest); return;
        case TYPE_CODE_BIG_DECIMAL: BigDecimal[] bd = (BigDecimal[])v; insert(() -> bd.length, () -> Encoding.insertBigDecimals(bd, UNIT_LENGTH_BYTES, dest), dest); return;
        case TYPE_CODE_ARRAY:  // note that type for String[] has elementType.typeCode() == TYPE_CODE_ARRAY
        case TYPE_CODE_TUPLE:
            final Object[] objects = (Object[]) v;
            if(dynamic) {
                insert(() -> objects.length, () -> insertOffsets(objects, dest), dest);
            }
            for (Object object : objects) {
                elementType.encodeTail(object, dest);
            }
            return;
        default: throw new Error();
        }
    }

    private void insertOffsets(final Object[] objects, ByteBuffer dest) {
        if (elementType.dynamic) {
            int nextOffset = objects.length * Encoding.OFFSET_LENGTH_BYTES;
            for (Object object : objects) {
                nextOffset = Encoding.insertOffset(nextOffset, dest, elementType.byteLength(object));
            }
        }
    }

    private static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? BooleanType.BOOLEAN_TRUE : BooleanType.BOOLEAN_FALSE);
        }
    }

    private static void insertInts(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            Encoding.insertInt(e, dest);
        }
    }

    private static void insertLongs(long[] longs, ByteBuffer dest) {
        for (long e : longs) {
            Encoding.insertInt(e, dest);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    J decode(ByteBuffer bb, byte[] unitBuffer) {
        final int arrayLen = length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_TYPE.decode(bb, unitBuffer)
                : length;
        try {
            switch (elementType.typeCode()) {
            case TYPE_CODE_BOOLEAN: return (J) decodeBooleanArray(bb, arrayLen, unitBuffer);
            case TYPE_CODE_BYTE: return (J) decodeByteArray(bb, arrayLen);
            case TYPE_CODE_INT: return (J) decodeIntArray((IntType) elementType, bb, arrayLen, unitBuffer);
            case TYPE_CODE_LONG: return (J) decodeLongArray((LongType) elementType, bb, arrayLen, unitBuffer);
            case TYPE_CODE_BIG_INTEGER: return (J) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, unitBuffer);
            case TYPE_CODE_BIG_DECIMAL: return (J) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, unitBuffer);
            case TYPE_CODE_ARRAY:
            case TYPE_CODE_TUPLE: return (J) decodeObjectArray(arrayLen, bb, unitBuffer);
            default: throw new Error();
            }
        } catch(NegativeArraySizeException nase) {
            throw new IllegalArgumentException(nase);
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < arrayLen; i++) {
            bb.get(unitBuffer);
            for (int j = 0; j < booleanOffset; j++) {
                if(unitBuffer[j] == 0) continue;
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - j));
            }
            byte last = unitBuffer[booleanOffset];
            if(last == 1) {
                booleans[i] = true;
            } else if(last != 0) {
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
        }
        return booleans;
    }

    private Object decodeByteArray(ByteBuffer bb, int arrayLen) {
        byte[] data = new byte[arrayLen];
        byte[] padding = new byte[Integers.roundLengthUp(arrayLen, UNIT_LENGTH_BYTES) - arrayLen];
        bb.get(data);
        bb.get(padding);
        for (byte b : padding) {
            if(b != Encoding.ZERO_BYTE) throw new IllegalArgumentException("malformed array: non-zero padding byte");
        }
        return encodeIfString(data);
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = decodeBigIntElement(intType, bb, unitBuffer).intValue();
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = decodeBigIntElement(longType, bb, unitBuffer).longValue();
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = decodeBigIntElement(bigIntegerType, bb, unitBuffer);
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(decodeBigIntElement(bigDecimalType, bb, unitBuffer), scale);
        }
        return bigDecs;
    }

    private static BigInteger decodeBigIntElement(UnitType<?> type, ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer);
        BigInteger bi = new BigInteger(unitBuffer);
        type.validateBigInt(bi);
        return bi;
    }

    private Object[] decodeObjectArray(int len, ByteBuffer bb, byte[] unitBuffer) {
        Object[] dest = (Object[]) Array.newInstance(elementType.clazz, len); // reflection ftw
        if(!this.dynamic || !elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, unitBuffer);
            }
        } else {
            final int index = bb.position(); // *** save this value here if you want to support lenient mode below
            int[] offsets = new int[len];
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoding.OFFSET_TYPE.decode(bb, unitBuffer);
            }
            for (int i = 0; i < len; i++) {
                final int offset = offsets[i];
                if (offset != 0) {
                    /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                    if (bb.position() != index + offset) {
                        bb.position(index + offset); // lenient
                    }
                    dest[i] = elementType.decode(bb, unitBuffer);
                }
            }
        }
        return dest;
    }

    /**
     * Parses RLP Object {@link com.esaulpaugh.headlong.rlp.util.Notation} as a {@link J}.
     *
     * @param s the array's RLP object notation
     * @return  the parsed array
     * @see com.esaulpaugh.headlong.rlp.util.Notation
     */
    @Override
    public J parseArgument(String s) { // expects RLP object notation such as "['00', '01', '01']"
        return SuperSerial.deserializeArray(this, s, false, clazz);
    }
}
