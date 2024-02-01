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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.abi.TupleType.countBytes;
import static com.esaulpaugh.headlong.abi.TupleType.totalLen;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Represents static array types such as uint16[3][2] or bytes3 and dynamic array types such as fixed168x10[5][],
 * bytes[4], or string.
 * @param <ET>   the {@link ABIType} for the elements of the array
 * @param <E>  the Java type of the elements of the array
 * @param <A>   the Java type of the array. not necessarily equivalent to {@link E}[]
 */
public final class ArrayType<ET extends ABIType<E>, E, A> extends ABIType<A> {

    private static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    static final Class<String> STRING_CLASS = String.class;
    static final Class<String[]> STRING_ARRAY_CLASS = String[].class;

    public static final int DYNAMIC_LENGTH = -1;

    private final boolean isString;
    private final ET elementType;
    private final int length;
    private final Class<?> arrayClass;
    private final int headLength;
    final int flags;
    final boolean legacyDecode;

    ArrayType(String canonicalType, Class<A> clazz, ET elementType, int length, Class<?> arrayClass, int flags) {
        super(canonicalType, clazz, DYNAMIC_LENGTH == length || elementType.dynamic);
        this.isString = STRING_CLASS == clazz;
        this.elementType = elementType;
        this.length = length;
        this.arrayClass = arrayClass;
        this.headLength = dynamic ? OFFSET_LENGTH_BYTES : staticArrayHeadLength();
        this.flags = flags;
        this.legacyDecode = (flags & ABIType.FLAG_LEGACY_DECODE) != 0;
    }

    int staticArrayHeadLength() {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BYTE: return UNIT_LENGTH_BYTES; // all static byte arrays round up to exactly 32 bytes and not more
        case TYPE_CODE_ARRAY: return length * elementType.asArrayType().staticArrayHeadLength();
        case TYPE_CODE_TUPLE: return length * elementType.asTupleType().staticTupleHeadLength();
        default: return length * UNIT_LENGTH_BYTES;
        }
    }

    @Override
    public int getFlags() {
        return flags;
    }

    public ET getElementType() {
        return elementType;
    }

    public int getLength() {
        return length;
    }

    public boolean isString() {
        return isString;
    }

    @Override
    Class<?> arrayClass() {
        if (arrayClass != null) {
            return arrayClass;
        }
        return createArray(clazz, 0)
                .getClass();
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_ARRAY;
    }

    @Override
    int headLength() {
        return headLength;
    }

    @Override
    int dynamicByteLength(A value) {
        return totalLen(calcElementsLen(value), length == DYNAMIC_LENGTH);
    }

    @Override
    int byteLength(A value) {
        if(!dynamic) return headLength;
        return dynamicByteLength(value);
    }

    @SuppressWarnings("unchecked")
    private int calcElementsLen(A value) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return ((boolean[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_BYTE: return Integers.roundLengthUp(byteCount(value), UNIT_LENGTH_BYTES);
        case TYPE_CODE_INT: return ((int[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_LONG: return ((long[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: return ((Number[]) value).length * UNIT_LENGTH_BYTES;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return measureByteLength((E[]) value);
        case TYPE_CODE_ADDRESS: return ((Address[]) value).length * UNIT_LENGTH_BYTES;
        default: throw new AssertionError();
        }
    }

    private int staticByteLengthPacked() {
        if(length == DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("array of dynamic elements");
        }
        return length * elementType.byteLengthPacked(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    int byteLengthPacked(A value) {
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
        case TYPE_CODE_TUPLE:
        case TYPE_CODE_ADDRESS: return measureByteLengthPacked((E[]) value);
        default: throw new AssertionError();
        }
    }

    private int byteCount(Object value) {
        return decodeIfString(value).length;
    }

    private byte[] decodeIfString(Object value) {
        return !isString ? (byte[]) value : Strings.decode((String) value, Strings.UTF_8);
    }

    Object encodeIfString(byte[] bytes) {
        return !isString ? bytes : Strings.encode(bytes, Strings.UTF_8);
    }

    @Override
    public int validate(A value) {
        validateClass(value); // validateClass to disallow Object[] etc
        return totalLen(validateElements(value), length == DYNAMIC_LENGTH);
    }

    @SuppressWarnings("unchecked")
    private int validateElements(A value) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return validateBooleans((boolean[]) value);
        case TYPE_CODE_BYTE: return validateBytes(value);
        case TYPE_CODE_INT: return validateInts((int[]) value, (IntType) elementType);
        case TYPE_CODE_LONG: return validateLongs((long[]) value, (LongType) elementType);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
        case TYPE_CODE_ADDRESS: return validateObjects((E[]) value);
        default: throw new AssertionError();
        }
    }

    private int validateBooleans(boolean[] arr) {
        return checkLength(arr.length, arr) * UNIT_LENGTH_BYTES;
    }

    private int validateBytes(A arr) {
        return Integers.roundLengthUp(checkLength(byteCount(arr), arr), UNIT_LENGTH_BYTES);
    }

    private int measureArrayElements(int n, IntUnaryOperator measurer) {
        return elementType.dynamic
                ? OFFSET_LENGTH_BYTES * n + countBytes(true, n, measurer)
                : countBytes(true, n, measurer);
    }

    private int validateInts(int[] arr, IntType type) {
        return measureArrayElements(checkLength(arr.length, arr), i -> type.validatePrimitive(arr[i]));
    }

    private int validateLongs(long[] arr, LongType type) {
        return measureArrayElements(checkLength(arr.length, arr), i -> type.validatePrimitive(arr[i]));
    }

    private int validateObjects(E[] arr) {
        return measureArrayElements(checkLength(arr.length, arr), i -> elementType.validate(arr[i]));
    }

    private int measureByteLength(E[] arr) {
        return measureArrayElements(arr.length, i -> elementType.byteLength(arr[i]));
    }

    private int measureByteLengthPacked(E[] arr) { // don't count offsets
        return countBytes(true, arr.length, i -> elementType.byteLengthPacked(arr[i]));
    }

    private int checkLength(final int valueLen, Object value) {
        if(length != DYNAMIC_LENGTH && length != valueLen) {
            throw mismatchErr("array length",
                    friendlyClassName(value.getClass(), valueLen), friendlyClassName(clazz, length),
                    "length " + length, "" + valueLen);
        }
        return valueLen;
    }

    @SuppressWarnings("unchecked")
    @Override
    void encodeTail(A value, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBooleans((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: encodeBytes(decodeIfString(value), dest); return;
        case TYPE_CODE_INT: encodeInts((int[]) value, dest); return;
        case TYPE_CODE_LONG: encodeLongs((long[]) value, dest); return;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
        case TYPE_CODE_ADDRESS: encodeObjects((E[]) value, dest); return;
        default: throw new AssertionError();
        }
    }

    private void encodeObjects(E[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        if (elementType.dynamic) {
            encodeDynamic(arr, elementType, dest, OFFSET_LENGTH_BYTES * arr.length);
        } else {
            encodeStatic(arr, elementType, dest);
        }
    }

    private void encodeStatic(E[] values, ET et, ByteBuffer dest) {
        for (E value : values) {
            et.encodeTail(value, dest);
        }
    }

    private void encodeDynamic(E[] values, ET et, ByteBuffer dest, int offset) {
        if (values.length == 0) {
            return;
        }
        final int last = values.length - 1;
        for (int i = 0; true; i++) {
            insertIntUnsigned(offset, dest); // insert offset
            if (i >= last) {
                for (E value : values) {
                    et.encodeTail(value, dest);
                }
                return;
            }
            offset += et.dynamicByteLength(values[i]); // return next offset
        }
    }

    private void encodeArrayLen(int len, ByteBuffer dest) {
        if(length == DYNAMIC_LENGTH) {
            insertIntUnsigned(len, dest);
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
        dest.put(arr);
        int rem = Integers.mod(arr.length, UNIT_LENGTH_BYTES);
        insert00Padding(rem != 0 ? UNIT_LENGTH_BYTES - rem : 0, dest);
    }

    private void encodeInts(int[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        for (int e : arr) {
            insertInt(e, dest);
        }
    }

    private void encodeLongs(long[] arr, ByteBuffer dest) {
        encodeArrayLen(arr.length, dest);
        for (long e : arr) {
            insertInt(e, dest);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    void encodePackedUnchecked(A value, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBooleansPacked((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: dest.put(decodeIfString(value)); return;
        case TYPE_CODE_INT: encodeIntsPacked((int[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_LONG: encodeLongsPacked((long[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
        case TYPE_CODE_ADDRESS:
            for(E e : (E[]) value) {
                elementType.encodePackedUnchecked(e, dest);
            }
            return;
        default: throw new AssertionError();
        }
    }

    private static void encodeBooleansPacked(boolean[] arr, ByteBuffer dest) {
        for (boolean bool : arr) {
            BooleanType.encodeBooleanPacked(bool, dest);
        }
    }

    private static void encodeIntsPacked(int[] arr, int byteLen, ByteBuffer dest) {
        for (int e : arr) {
            LongType.encodeLong(e, byteLen, dest);
        }
    }

    private static void encodeLongsPacked(long[] arr, int byteLen, ByteBuffer dest) {
        for (long e : arr) {
            LongType.encodeLong(e, byteLen, dest);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    A decode(ByteBuffer bb, byte[] unitBuffer) {
        final int arrayLen = length == DYNAMIC_LENGTH ? IntType.UINT21.decode(bb, unitBuffer) : length;
        checkNoDecodePossible(bb.remaining(), arrayLen);
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (A) decodeBooleans(arrayLen, bb, unitBuffer);
        case TYPE_CODE_BYTE: return (A) encodeIfString(decodeBytes(arrayLen, bb, legacyDecode));
        case TYPE_CODE_INT: return (A) decodeInts(arrayLen, bb, (IntType) elementType, unitBuffer);
        case TYPE_CODE_LONG: return (A) decodeLongs(arrayLen, bb, (LongType) elementType, unitBuffer);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
        case TYPE_CODE_ADDRESS: return (A) decodeObjects(arrayLen, bb, unitBuffer);
        default: throw new AssertionError();
        }
    }

    /**
     * Abort early if the input is obviously too short. Best effort to fail fast before allocating memory for the array.
     */
    private void checkNoDecodePossible(final int remaining, final int arrayLen) {
        final int minByteLen = !dynamic
                                    ? headLength
                                    : elementType.dynamic
                                        ? arrayLen * OFFSET_LENGTH_BYTES
                                        : !(elementType instanceof ByteType)
                                            ? arrayLen * elementType.headLength()
                                            : legacyDecode
                                                ? arrayLen
                                                : Integers.roundLengthUp(arrayLen, UNIT_LENGTH_BYTES);
        if(remaining < minByteLen) {
            throw new IllegalArgumentException("not enough bytes remaining: " + remaining + " < " + minByteLen);
        }
    }

    private static boolean[] decodeBooleans(int len, ByteBuffer bb, byte[] unitBuffer) {
        final boolean[] booleans = new boolean[len]; // elements are false by default
        int i = 0;
        try {
            for(; i < len; i++) {
                bb.get(unitBuffer);
                int j;
                for (j = 0; j < UNIT_LENGTH_BYTES - Byte.BYTES; j++) {
                    if (unitBuffer[j] != ZERO_BYTE) {
                        throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
                    }
                }
                switch (unitBuffer[j]) {
                case 1: booleans[i] = true;
                case 0: continue;
                default: throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
                }
            }
        } catch (IllegalArgumentException cause) {
            throw TupleType.decodeException(false, i, cause);
        }
        return booleans;
    }

    private static byte[] decodeBytes(int len, ByteBuffer bb, boolean legacyDecode) {
        final byte[] data = new byte[len];
        bb.get(data);
        final int mod = Integers.mod(len, UNIT_LENGTH_BYTES);
        if (mod != 0 && !legacyDecode) {
            final int padding = UNIT_LENGTH_BYTES - mod;
            for (int i = 0; i < padding; i++) {
                if (bb.get() != ZERO_BYTE) throw new IllegalArgumentException("malformed array: non-zero padding byte");
            }
        }
        return data;
    }

    private static int[] decodeInts(int len, ByteBuffer bb, IntType intType, byte[] unitBuffer) {
        int[] ints = new int[len];
        int i = 0;
        try {
            for (; i < len; i++) {
                ints[i] = intType.decode(bb, unitBuffer);
            }
        } catch (IllegalArgumentException cause) {
            throw TupleType.decodeException(false, i, cause);
        }
        return ints;
    }

    private static long[] decodeLongs(int len, ByteBuffer bb, LongType longType, byte[] unitBuffer) {
        long[] longs = new long[len];
        int i = 0;
        try {
            for (; i < len; i++) {
                longs[i] = longType.decode(bb, unitBuffer);
            }
        } catch (IllegalArgumentException cause) {
            throw TupleType.decodeException(false, i, cause);
        }
        return longs;
    }

    @SuppressWarnings("unchecked")
    static <T> T[] createArray(Class<T> elementClass, int len) {
        return (T[]) Array.newInstance(elementClass, len); // reflection ftw
    }

    private E[] decodeObjects(int len, ByteBuffer bb, byte[] unitBuffer) {
        final E[] elements = createArray(elementType.clazz, len);
        int i = 0;
        try {
            if (!elementType.dynamic) {
                for ( ; i < elements.length; i++) {
                    elements[i] = elementType.decode(bb, unitBuffer);
                }
            } else {
                final int start = bb.position(); // save this value before offsets are decoded
                int saved = start;
                for ( ; i < elements.length; i++) {
                    bb.position(saved);
                    final int jump = start + IntType.UINT31.decode(bb, unitBuffer);
                    /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                    saved = bb.position();
                    bb.position(jump); // leniently jump to specified offset
                    elements[i] = elementType.decode(bb, unitBuffer);
                }
            }
        } catch (IllegalArgumentException cause) {
            throw TupleType.decodeException(false, i, cause);
        }
        return elements;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ABIType<?>> T baseType(ABIType<?> type) {
        return type instanceof ArrayType<?, ?, ?>
                ? baseType(type.asArrayType().getElementType())
                : (T) type;
    }
}
