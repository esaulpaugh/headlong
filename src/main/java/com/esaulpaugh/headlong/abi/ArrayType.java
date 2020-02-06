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

import com.esaulpaugh.headlong.util.Strings;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.UnitType.LOG_2_UNIT_LENGTH_BYTES;
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
    final boolean isString;

    private final String arrayClassName;

    ArrayType(String canonicalType, Class<J> clazz, boolean dynamic, E elementType, int length, String arrayClassName) {
        super(canonicalType, clazz, dynamic);
        this.elementType = elementType;
        this.length = length;
        this.arrayClassName = arrayClassName;
        this.isString = String.class == clazz;
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
        case TYPE_CODE_BOOLEAN: len = ((boolean[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE: len = Utils.roundLengthUp((!isString ? (byte[]) value : Strings.decode((String) value, UTF_8)).length); break;
        case TYPE_CODE_INT: len = ((int[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_LONG: len = ((long[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: len = ((Number[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
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
                : len + (elements.length << LOG_2_UNIT_LENGTH_BYTES); // 32 bytes per offset
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
        case TYPE_CODE_BYTE: return (!isString ? (byte[]) value : Strings.decode((String) value, UTF_8)).length; // * 1
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

    @Override
    public int validate(final Object value) {
        validateClass(value);

        final int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: staticLen = checkLength(((boolean[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE:
            byte[] bytes = !isString ? (byte[]) value : Strings.decode((String) value, UTF_8);
            staticLen = Utils.roundLengthUp(checkLength(bytes.length, value));
            break;
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

    private int validateIntArray(int[] arr) {
        IntType intType = (IntType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                intType.validatePrimitiveElement(arr[i]); // validate without boxing primitive
            }
        } catch (ABIException ve) {
            throw abiException(ve, i);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateLongArray(long[] arr) {
        LongType longType = (LongType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                longType.validatePrimitiveElement(arr[i]); // validate without boxing primitive
            }
        } catch (ABIException ve) {
            throw abiException(ve, i);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
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
        } catch (ABIException ve) {
            throw abiException(ve, i);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
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
                    throw new ABIException("unexpected scale: " + element.scale());
                }
            }
        } catch (ABIException ve) {
            throw abiException(ve, i);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private static ABIException abiException(ABIException ve, int i) {
        return new ABIException("index " + i + ": " + ve.getMessage());
    }

    /** For arrays of arrays or arrays of tuples only. */
    private int validateObjectArray(Object[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int byteLength = !elementType.dynamic ? 0 : len << LOG_2_UNIT_LENGTH_BYTES; // when dynamic, 32 bytes per offset
        for (int i = 0; i < len; i++) {
            byteLength += elementType.validate(arr[i]);
        }
        return byteLength;
    }

    private int checkLength(final int valueLength, Object value) {
        if(length == DYNAMIC_LENGTH || length == valueLength) {
            return valueLength;
        }
        throw new ABIException(
                Utils.friendlyClassName(value.getClass(), valueLength)
                        + " not instanceof " + Utils.friendlyClassName(clazz, length) + ", " +
                        valueLength + " != " + length
        );
    }

    @Override
    int encodeHead(Object value, ByteBuffer dest, int offset) {
        if (!dynamic) {
            encodeArrayTail(value, dest);
            return offset;
        }
        // includes String
        return Encoding.insertOffset(offset, this, value, dest);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        if(!isString) {
            encodeArrayTail(value, dest);
        } else {
            byte[] bytes = Strings.decode((String) value, UTF_8);
            Encoding.insertInt(bytes.length, dest); // insertLength
            insertBytes(bytes, dest);
        }
    }

    private void insert(Supplier<Integer> supplyLength, Runnable insert, ByteBuffer dest) {
        if(length == DYNAMIC_LENGTH) {
            Encoding.insertInt(supplyLength.get(), dest);
        }
        insert.run();
    }

    private void encodeArrayTail(Object value, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: insert(() -> ((boolean[]) value).length, () -> insertBooleans((boolean[]) value, dest), dest); return;
        case TYPE_CODE_BYTE: insert(() -> ((byte[]) value).length, () -> insertBytes((byte[]) value, dest), dest); return;
        case TYPE_CODE_INT: insert(() -> ((int[]) value).length, () -> insertInts((int[]) value, dest), dest); return;
        case TYPE_CODE_LONG: insert(() -> ((long[]) value).length, () -> insertLongs((long[]) value, dest), dest); return;
        case TYPE_CODE_BIG_INTEGER: insert(() -> ((BigInteger[]) value).length, () -> insertBigIntegers((BigInteger[]) value, dest), dest); return;
        case TYPE_CODE_BIG_DECIMAL: insert(() -> ((BigDecimal[]) value).length, () -> insertBigDecimals((BigDecimal[]) value, dest), dest); return;
        case TYPE_CODE_ARRAY:  // type for String[] has elementType.typeCode() == TYPE_CODE_ARRAY
        case TYPE_CODE_TUPLE:
            final Object[] objects = (Object[]) value;
            final int len = objects.length;
            if(dynamic) {
                if(length == DYNAMIC_LENGTH) {
                    Encoding.insertInt(len, dest); // insertLength
                }
                if (elementType.dynamic) { // if elements are dynamic
                    int offset = len << LOG_2_UNIT_LENGTH_BYTES; // mul 32 (0x20)
                    for (int i = 0; i < len; i++) {
                        offset = Encoding.insertOffset(offset, elementType, objects[i], dest);
                    }
                }
            }
            for (int i = 0; i < len; i++) {
                elementType.encodeTail(objects[i], dest);
            }
            return;
        default: throw new Error();
        }
    }

    private static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? BooleanType.BOOLEAN_TRUE : BooleanType.BOOLEAN_FALSE);
        }
    }

    private static void insertBytes(byte[] bytes, ByteBuffer dest) {
        dest.put(bytes);
        final int remainder = bytes.length & (UNIT_LENGTH_BYTES - 1);
        final int paddingLength = remainder != 0 ? UNIT_LENGTH_BYTES - remainder : 0;
        for (int i = 0; i < paddingLength; i++) {
            dest.put(Encoding.ZERO_BYTE);
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

    private static void insertBigIntegers(BigInteger[] bigInts, ByteBuffer dest) {
        for (BigInteger e : bigInts) {
            Encoding.insertInt(e, dest);
        }
    }

    private static void insertBigDecimals(BigDecimal[] bigDecs, ByteBuffer dest) {
        for (BigDecimal e : bigDecs) {
            Encoding.insertInt(e.unscaledValue(), dest);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    J decode(ByteBuffer bb, byte[] elementBuffer) {
        final int arrayLen = length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_TYPE.decode(bb, elementBuffer)
                : length;

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (J) decodeBooleanArray(bb, arrayLen, elementBuffer);
        case TYPE_CODE_BYTE: return (J) decodeByteArray(bb, arrayLen);
        case TYPE_CODE_INT: return (J) decodeIntArray((IntType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_LONG: return (J) decodeLongArray((LongType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_INTEGER: return (J) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_DECIMAL: return (J) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return (J) decodeObjectArray(arrayLen, bb, elementBuffer);
        default: throw new Error();
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer);
            for (int j = 0; j < booleanOffset; j++) {
                if(elementBuffer[j] == 0) continue;
                throw new ABIException("illegal boolean value @ " + (bb.position() - j));
            }
            byte last = elementBuffer[booleanOffset];
            if(last == 1) {
                booleans[i] = true;
            } else if(last != 0) {
                throw new ABIException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
        }
        return booleans;
    }

    private Object decodeByteArray(ByteBuffer bb, int arrayLen) {
        final int mark = bb.position();
        byte[] out = new byte[arrayLen];
        bb.get(out);
        bb.position(mark + Utils.roundLengthUp(arrayLen));
        return !isString ? out : Strings.encode(out, UTF_8);
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = decodeBigIntElement(intType, bb, elementBuffer).intValue();
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = decodeBigIntElement(longType, bb, elementBuffer).longValue();
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = decodeBigIntElement(bigIntegerType, bb, elementBuffer);
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(decodeBigIntElement(bigDecimalType, bb, elementBuffer), scale);
        }
        return bigDecs;
    }

    private static BigInteger decodeBigIntElement(UnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bi = new BigInteger(elementBuffer);
        type.validateBigIntElement(bi);
        return bi;
    }

    private Object[] decodeObjectArray(int len, ByteBuffer bb, byte[] elementBuffer) {
        Object[] dest = (Object[]) Array.newInstance(elementType.clazz, len); // reflection ftw
        if(!this.dynamic) {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        } else {
            decodeObjectArrayDynamic(len, bb, elementBuffer, dest);
        }
        return dest;
    }

    private void decodeObjectArrayDynamic(int len, ByteBuffer bb, byte[] elementBuffer, final Object[] dest) {
        if(!elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        } else {
//            final int index = bb.position(); // *** save this value here if you want to support lenient mode below
            int[] offsets = new int[len];
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoding.OFFSET_TYPE.decode(bb, elementBuffer);
            }
            for (int i = 0; i < len; i++) {
                if (offsets[i] > 0) {
                    /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                    if (bb.position() != index + offset) {
//                        System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                        bb.position(index + offset); // lenient
//                    }
                    dest[i] = elementType.decode(bb, elementBuffer);
                }
            }
        }
    }

    @Override
    public J parseArgument(String s) {
        throw new UnsupportedOperationException();
    }
}
