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

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.TupleType.countBytes;
import static com.esaulpaugh.headlong.abi.TupleType.totalLen;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

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

    public static final int DYNAMIC_LENGTH = -1;

    private final boolean isString;
    private final E elementType;
    private final int length;
    private final Class<?> arrayClass;

    { // instance initializer
        this.isString = clazz == STRING_CLASS;
    }

    ArrayType(String canonicalType, Class<J> clazz, E elementType, int length) {
        this(canonicalType, clazz, elementType, length, null);
    }

    ArrayType(String canonicalType, Class<J> clazz, E elementType, int length, Class<?> arrayClass) {
        super(canonicalType, clazz, length == DYNAMIC_LENGTH || elementType.dynamic);
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
     * @param value the value to measure
     * @return the length in bytes of this array when encoded
     */
    @Override
    int byteLength(Object value) {
        return totalLen(calcElementsLen(value), length == DYNAMIC_LENGTH);
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
        case TYPE_CODE_TUPLE: return measureByteLength((Object[]) value);
        default: throw new Error();
        }
    }

    private int staticByteLengthPacked() {
        if(length == DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("array of dynamic elements");
        }
        return length * elementType.byteLengthPacked(null);
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
        case TYPE_CODE_TUPLE: return measureByteLengthPacked((Object[]) value);
        default: throw new Error();
        }
    }

    private int byteCount(Object value) {
        return decodeIfString(value).length;
    }

    byte[] decodeIfString(Object value) {
        return !isString ? (byte[]) value : Strings.decode((String) value, Strings.UTF_8);
    }

    Object encodeIfString(byte[] bytes) {
        return !isString ? bytes : Strings.encode(bytes, Strings.UTF_8);
    }

    @Override
    public int validate(J value) {
        return totalLen(validateElements(validateClass(value)), length == DYNAMIC_LENGTH); // validateClass to disallow Object[] etc
    }

    private int validateElements(J value) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return validateBooleans((boolean[]) value);
        case TYPE_CODE_BYTE: return validateBytes(value);
        case TYPE_CODE_INT: return validateInts((int[]) value, (IntType) elementType);
        case TYPE_CODE_LONG: return validateLongs((long[]) value, (LongType) elementType);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return validateObjects((Object[]) value);
        default: throw new Error();
        }
    }

    private int validateBooleans(boolean[] arr) {
        return checkLength(arr.length, arr) * UNIT_LENGTH_BYTES;
    }

    private int validateBytes(J arr) {
        return Integers.roundLengthUp(checkLength(byteCount(arr), arr), UNIT_LENGTH_BYTES);
    }

    private int offsetsLen(int len) {
        return elementType.dynamic ? OFFSET_LENGTH_BYTES * len: 0;
    }

    private int validateInts(int[] arr, IntType type) {
        return countBytes(true, checkLength(arr.length, arr), offsetsLen(arr.length), (i) -> type.validatePrimitive(arr[i]));
    }

    private int validateLongs(long[] arr, LongType type) {
        return countBytes(true, checkLength(arr.length, arr), offsetsLen(arr.length), (i) -> type.validatePrimitive(arr[i]));
    }

    private int validateObjects(Object[] arr) {
        return countBytes(true, checkLength(arr.length, arr), offsetsLen(arr.length), (i) -> elementType._validate(arr[i]));
    }

    private int measureByteLength(Object[] arr) {
        return countBytes(true, arr.length, offsetsLen(arr.length), (i) -> elementType.byteLength(arr[i]));
    }

    private int measureByteLengthPacked(Object[] arr) { // don't count offsets
        return countBytes(true, arr.length, 0, (i) -> elementType.byteLengthPacked(arr[i]));
    }

    private int checkLength(final int valueLen, Object value) {
        if(length != DYNAMIC_LENGTH && length != valueLen) {
            throw mismatchErr("array length",
                    friendlyClassName(value.getClass(), valueLen), friendlyClassName(clazz, length),
                    "length " + length, "" + valueLen);
        }
        return valueLen;
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBooleans((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: encodeBytes(decodeIfString(value), dest); return;
        case TYPE_CODE_INT: encodeInts((int[]) value, dest); return;
        case TYPE_CODE_LONG: encodeLongs((long[]) value, dest); return;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            Object[] arr = (Object[]) value;
            encodeArrayLen(arr.length, dest);
            TupleType.encodeObjects(dynamic, arr, (i) -> elementType, dest);
            return;
        default: throw new Error();
        }
    }

    private void encodeArrayLen(int len, ByteBuffer dest) {
        if(length == DYNAMIC_LENGTH) {
            Encoding.insertInt(len, dest);
        }
    }

    private void encodeBooleans(boolean[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        for (boolean e : arr) {
            BooleanType.encodeBoolean(e, dest);
        }
    }

    private void encodeBytes(byte[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        Encoding.insertBytesPadded(arr, dest);
    }

    private void encodeInts(int[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        for (int e : arr) {
            Encoding.insertInt(e, dest);
        }
    }

    private void encodeLongs(long[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        for (long e : arr) {
            Encoding.insertInt(e, dest);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    J decode(ByteBuffer bb, byte[] unitBuffer) {
        final int arrayLen = length == DYNAMIC_LENGTH ? Encoding.UINT17.decode(bb, unitBuffer) : length;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (J) decodeBooleans(arrayLen, bb, unitBuffer);
        case TYPE_CODE_BYTE: return (J) decodeBytes(arrayLen, bb);
        case TYPE_CODE_INT: return (J) decodeInts(arrayLen, bb, (IntType) elementType, unitBuffer);
        case TYPE_CODE_LONG: return (J) decodeLongs(arrayLen, bb, (LongType) elementType, unitBuffer);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return (J) decodeObjects(arrayLen, bb, unitBuffer);
        default: throw new Error();
        }
    }

    private static Object decodeBooleans(int len, ByteBuffer bb, byte[] unitBuffer) {
        final boolean[] booleans = new boolean[len]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < len; i++) {
            bb.get(unitBuffer);
            for (int j = 0; j < booleanOffset; j++) {
                if (unitBuffer[j] == Encoding.ZERO_BYTE) {
                    continue;
                }
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
            byte last = unitBuffer[booleanOffset];
            if (last == Encoding.ONE_BYTE) {
                booleans[i] = true;
            } else if (last != Encoding.ZERO_BYTE) {
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
        }
        return booleans;
    }

    private Object decodeBytes(int len, ByteBuffer bb) {
        byte[] data = new byte[len];
        bb.get(data);
        int mod = Integers.mod(len, UNIT_LENGTH_BYTES);
        if(mod != 0) {
            byte[] padding = new byte[UNIT_LENGTH_BYTES - mod];
            bb.get(padding);
            for (byte b : padding) {
                if(b != Encoding.ZERO_BYTE) throw new IllegalArgumentException("malformed array: non-zero padding byte");
            }
        }
        return encodeIfString(data);
    }

    private static Object decodeInts(int len, ByteBuffer bb, IntType intType, byte[] unitBuffer) {
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            ints[i] = intType.decode(bb, unitBuffer);
        }
        return ints;
    }

    private static Object decodeLongs(int len, ByteBuffer bb, LongType longType, byte[] unitBuffer) {
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            longs[i] = longType.decode(bb, unitBuffer);
        }
        return longs;
    }

    private Object decodeObjects(int len, ByteBuffer bb, byte[] unitBuffer) {
        Object[] elements = (Object[]) Array.newInstance(elementType.clazz, len); // reflection ftw
        TupleType.decodeObjects(bb, unitBuffer, (i) -> elementType, elements);
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

    public static ABIType<?> baseType(ABIType<?> type) {
        while (type.typeCode() == ABIType.TYPE_CODE_ARRAY) {
            type = ((ArrayType<? extends ABIType<?>, ?>) type).getElementType();
        }
        return type;
    }
}
