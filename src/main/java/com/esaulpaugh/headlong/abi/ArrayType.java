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
import java.nio.ByteBuffer;
import java.util.function.IntConsumer;

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;
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

    private static final ClassLoader CLASS_LOADER = ArrayType.class.getClassLoader();

    static final Class<String> STRING_CLASS = String.class;
    static final Class<String[]> STRING_ARRAY_CLASS = String[].class;

    private static final int ARRAY_LENGTH_BYTE_LEN = UNIT_LENGTH_BYTES;

    static final int DYNAMIC_LENGTH = -1;

    private final boolean isString;
    final E elementType;
    private final int length;
    private final Class<?> arrayClass;

    ArrayType(String canonicalType, Class<J> clazz, E elementType, int length) {
        this(canonicalType, clazz, elementType, length, null);
    }

    ArrayType(String canonicalType, Class<J> clazz, E elementType, int length, Class<?> arrayClass) {
        super(canonicalType, clazz, elementType.dynamic || length == DYNAMIC_LENGTH);
        this.isString = clazz == STRING_CLASS;
        this.elementType = elementType;
        this.length = length;
        this.arrayClass = arrayClass;
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
    Class<?> arrayClass() throws ClassNotFoundException {
        return arrayClass != null ? arrayClass : Class.forName('[' + clazz.getName(), false, CLASS_LOADER);
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
        final int elementsLen = calcElementsLen(value);
        // arrays with variable number of elements get +32 for the array length
        return length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_BYTE_LEN + elementsLen
                : elementsLen;
    }

    private int calcElementsLen(Object value) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return ((boolean[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_BYTE: return Integers.roundLengthUp(byteCount(value), UNIT_LENGTH_BYTES);
        case TYPE_CODE_INT: return ((int[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_LONG: return ((long[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: return ((Number[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return measureByteLen((Object[]) value);
        default: throw new Error();
        }
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
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return ((boolean[]) value).length; // * 1
        case TYPE_CODE_BYTE: return byteCount(value); // * 1
        case TYPE_CODE_INT: return ((int[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_LONG: return ((long[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: return ((Number[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return measurePackedByteLen((Object[]) value);
        default: throw new Error();
        }
    }

    private int byteCount(Object value) {
        return decodeIfString(value).length;
    }

    byte[] decodeIfString(Object value) {
        return !isString ? (byte[]) value : Strings.decode((String) value, UTF_8);
    }

    Object encodeIfString(byte[] bytes) {
        return !isString ? bytes : Strings.encode(bytes, UTF_8);
    }

    @Override
    public int validate(final Object val) {
        validateClass(val);
        final int elementsLen = validateElements(val);
        // arrays with variable number of elements get +32 for the array length
        return length != DYNAMIC_LENGTH ? elementsLen : ARRAY_LENGTH_BYTE_LEN + elementsLen;
    }

    private int validateElements(Object val) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return validateBooleans((boolean[]) val);
        case TYPE_CODE_BYTE: return validateBytes(val);
        case TYPE_CODE_INT: return validateInts((int[]) val, (IntType) elementType);
        case TYPE_CODE_LONG: return validateLongs((long[]) val, (LongType) elementType);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return validateObjects((Object[]) val);
        default: throw new Error();
        }
    }

    private int validateBooleans(boolean[] arr) {
        return checkLength(arr.length, arr) * UNIT_LENGTH_BYTES;
    }

    private int validateBytes(Object arr) {
        return Integers.roundLengthUp(checkLength(byteCount(arr), arr), UNIT_LENGTH_BYTES);
    }

    private int validateInts(int[] arr, IntType type) {
        return validateUnits(arr.length, arr, (i) -> type.validatePrimitive(arr[i]));
    }

    private int validateLongs(long[] arr, LongType type) {
        return validateUnits(arr.length, arr, (i) -> type.validatePrimitive(arr[i]));
    }

    private int validateUnits(int len, Object arr, IntConsumer elementValidator) {
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementValidator.accept(i);
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("array index " + i + ": " + iae.getMessage());
        }
        return len * UNIT_LENGTH_BYTES;
    }

    private int validateObjects(Object[] elements) {
        checkLength(elements.length, elements);
        return measureObjects(elements, (int i) -> elementType.validate(elements[i]));
    }

    private int measureByteLen(Object[] elements) {
        return measureObjects(elements, (int i) -> elementType.byteLength(elements[i]));
    }

    private int measurePackedByteLen(Object[] elements) {
        return measureObjects(elements, (int i) -> elementType.byteLengthPacked(elements[i]));
    }

    /** For arrays of arrays and arrays of tuples only. */
    private int measureObjects(Object[] elements, Inspector visitor) {
        int byteLength = measureAll(elements, visitor);
        return !elementType.dynamic ? byteLength : (elements.length * OFFSET_LENGTH_BYTES) + byteLength;
    }

    @FunctionalInterface
    private interface Inspector {
        int inspect(int i);
    }

    private static int measureAll(Object[] elements, Inspector inspector) {
        int sum = 0;
        for (int i = 0; i < elements.length; i++) {
            sum += inspector.inspect(i);
        }
        return sum;
    }

    private int checkLength(final int valueLen, Object value) {
        if(length != valueLen && length != DYNAMIC_LENGTH) {
            throw new IllegalArgumentException(
                    Utils.friendlyClassName(value.getClass(), valueLen)
                            + " not instanceof " + Utils.friendlyClassName(clazz, length) + ", " +
                            valueLen + " != " + length
            );
        }
        return valueLen;
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        encodeArrayTail(value, dest);
    }

    private void insert(int len, ByteBuffer dest, Runnable insert) {
        if(length == DYNAMIC_LENGTH) {
            Encoding.insertInt(len, dest);
        }
        insert.run();
    }

    private void encodeArrayTail(Object v, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBooleans((boolean[]) v, dest); return;
        case TYPE_CODE_BYTE: encodeBytes(decodeIfString(v), dest); return;
        case TYPE_CODE_INT: encodeInts((int[]) v, dest); return;
        case TYPE_CODE_LONG: encodeLongs((long[]) v, dest); return;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: encodeObjects((Object[]) v, dest); return;
        default: throw new Error();
        }
    }

    private void encodeBooleans(boolean[] arr, ByteBuffer dest) {
        insert(arr.length, dest, () -> {
            for (boolean e : arr) {
                dest.put(e ? BooleanType.BOOLEAN_TRUE : BooleanType.BOOLEAN_FALSE);
            }
        });
    }

    private void encodeBytes(byte[] arr, ByteBuffer dest) {
        insert(arr.length, dest, () -> Encoding.insertBytesPadded(arr, dest));
    }

    private void encodeInts(int[] arr, ByteBuffer dest) {
        insert(arr.length, dest, () -> {
            for (int e : arr) {
                Encoding.insertInt(e, dest);
            }
        });
    }

    private void encodeLongs(long[] arr, ByteBuffer dest) {
        insert(arr.length, dest, () -> {
            for (long e : arr) {
                Encoding.insertInt(e, dest);
            }
        });
    }

    private void encodeObjects(Object[] arr, ByteBuffer dest) {
        if(dynamic) {
            insert(arr.length, dest, () -> insertOffsets(arr, dest));
        }
        for (Object object : arr) {
            elementType.encodeTail(object, dest);
        }
    }

    private void insertOffsets(Object[] objects, ByteBuffer dest) {
        if (elementType.dynamic) {
            int nextOffset = objects.length * OFFSET_LENGTH_BYTES;
            for (Object object : objects) {
                nextOffset = Encoding.insertOffset(nextOffset, dest, elementType.byteLength(object));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    J decode(ByteBuffer bb, byte[] unitBuffer) {
        final int arrayLen = length == DYNAMIC_LENGTH ? Encoding.UINT17.decode(bb, unitBuffer) : length;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (J) decodeBooleans(bb, arrayLen, unitBuffer);
        case TYPE_CODE_BYTE: return (J) decodeBytes(bb, arrayLen);
        case TYPE_CODE_INT: return (J) decodeInts((IntType) elementType, bb, arrayLen, unitBuffer);
        case TYPE_CODE_LONG: return (J) decodeLongs((LongType) elementType, bb, arrayLen, unitBuffer);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return (J) decodeObjects(arrayLen, bb, unitBuffer);
        default: throw new Error();
        }
    }

    private static boolean[] decodeBooleans(ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
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

    private Object decodeBytes(ByteBuffer bb, int arrayLen) {
        byte[] data = new byte[arrayLen];
        byte[] padding = new byte[Integers.roundLengthUp(arrayLen, UNIT_LENGTH_BYTES) - arrayLen];
        bb.get(data);
        bb.get(padding);
        for (byte b : padding) {
            if(b != Encoding.ZERO_BYTE) throw new IllegalArgumentException("malformed array: non-zero padding byte");
        }
        return encodeIfString(data);
    }

    private static int[] decodeInts(IntType intType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = intType.decode(bb, unitBuffer);
        }
        return ints;
    }

    private static long[] decodeLongs(LongType longType, ByteBuffer bb, int arrayLen, byte[] unitBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = longType.decode(bb, unitBuffer);
        }
        return longs;
    }

    private Object[] decodeObjects(int len, ByteBuffer bb, byte[] unitBuffer) {
        final Object[] elements = (Object[]) Array.newInstance(elementType.clazz, len); // reflection ftw
        if(!this.dynamic || !elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                elements[i] = elementType.decode(bb, unitBuffer);
            }
        } else {
            final int tailStart = bb.position(); // save this value for later
            int[] offsets = new int[len];
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoding.UINT31.decode(bb, unitBuffer);
            }
            decodeTails(bb, offsets, tailStart, (i) -> elements[i] = elementType.decode(bb, unitBuffer));
        }
        return elements;
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
