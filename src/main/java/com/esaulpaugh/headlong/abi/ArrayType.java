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

import com.esaulpaugh.headlong.abi.util.Utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.UnitType.LOG_2_UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;

/**
 * Represents static array types such as bytes3 and uint16[3][2] and dynamic array types such as decimal[5][] or
 * string[4].
 *
 * @param <T>   the {@link ABIType} for the elements of the array
 * @param <J>   this {@link ArrayType}'s corresponding Java type
 */
public final class ArrayType<T extends ABIType<?>, J> extends ABIType<J> {

    static final Class<byte[]> BYTE_ARRAY_CLASS = byte[].class;
    static final String BYTE_ARRAY_ARRAY_CLASS_NAME = byte[][].class.getName();

    static final Class<String> STRING_CLASS = String.class;
    static final String STRING_ARRAY_CLASS_NAME = String[].class.getName();

    private static final IntType ARRAY_LENGTH_TYPE = new IntType("int32", Integer.SIZE, false);
    private static final int ARRAY_LENGTH_BYTE_LEN = UNIT_LENGTH_BYTES;

    static final int DYNAMIC_LENGTH = -1;

    final T elementType;
    final int length;
    /* transient */ final boolean isString;

    private final String arrayClassName;

    ArrayType(String canonicalType, Class<J> clazz, boolean dynamic, T elementType, int length, String arrayClassName) {
        super(canonicalType, clazz, dynamic);
        this.elementType = elementType;
        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
        this.length = length;
        this.arrayClassName = arrayClassName;
        this.isString = String.class == clazz;
        if(isString && length != DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("illegal fixed string length");
        }
    }

    public T getElementType() {
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
    int typeCode() {
        return TYPE_CODE_ARRAY;
    }

    /**
     * LOG_2_UNIT_LENGTH_BYTES == 5
     * x << 5 == x * 32
     * @param value the value to measure
     * @return  the length in bytes
     */
    @Override
    int byteLength(Object value) {
        int len;
        final ABIType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: len = ((boolean[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE: len =
                roundLengthUp(
                        (isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value).length
                );
                break;
        case TYPE_CODE_INT: len = ((int[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_LONG: len = ((long[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: len = ((Number[]) value).length << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final Object[] elements = (Object[]) value;
            final int n = elements.length;
            len = 0;
            for (int i = 0; i < n; i++) {
                len += elementType.byteLength(elements[i]);
            }
            if(elementType.dynamic) { // implies this.dynamic
                len += n << LOG_2_UNIT_LENGTH_BYTES; // 32 bytes per offset
            }
            break;
        default: throw unrecognizedType(elementType.toString());
        }
        // arrays with variable number of elements get +32 for the array length
        return length == DYNAMIC_LENGTH
                ? ARRAY_LENGTH_BYTE_LEN + len
                : len;
    }

    @Override
    int byteLengthPacked(Object value) {
        final ABIType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return ((boolean[]) value).length; // * 1
        case TYPE_CODE_BYTE: return (isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value).length; // * 1
        case TYPE_CODE_INT: return ((int[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_LONG: return ((long[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: return ((Number[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final Object[] elements = (Object[]) value;
            int staticLen = 0;
            final int len = elements.length;
            for (int i = 0; i < len; i++) {
                staticLen += elementType.byteLengthPacked(elements[i]);
            }
            return staticLen;
        default: throw unrecognizedType(elementType.toString());
        }
    }

    @Override
    public int validate(final Object value) {
        validateClass(value);

        final int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: staticLen = checkLength(((boolean[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE:
            byte[] bytes = isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value;
            staticLen = roundLengthUp(checkLength(bytes.length, value));
            break;
        case TYPE_CODE_INT: staticLen = validateIntArray((int[]) value); break;
        case TYPE_CODE_LONG: staticLen = validateLongArray((long[]) value); break;
        case TYPE_CODE_BIG_INTEGER: staticLen = validateBigIntegerArray((BigInteger[]) value); break;
        case TYPE_CODE_BIG_DECIMAL: staticLen = validateBigDecimalArray((BigDecimal[]) value); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: staticLen = validateObjectArray((Object[]) value); break;
        default: throw unrecognizedType(value.getClass().getName());
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
                // validate without boxing primitive
                intType.validatePrimitiveElement(arr[i]);
            }
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
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
                // validate without boxing primitive
                longType.validatePrimitiveElement(arr[i]);
            }
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateBigIntegerArray(BigInteger[] bigIntegers) {
        final int len = bigIntegers.length;
        checkLength(len, bigIntegers);
        BigIntegerType bigIntegerType = (BigIntegerType) elementType;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                bigIntegerType.validateBigIntBitLen(bigIntegers[i]);
            }
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateBigDecimalArray(BigDecimal[] bigDecimals) {
        final int len = bigDecimals.length;
        checkLength(len, bigDecimals);
        BigDecimalType bigDecimalType = (BigDecimalType) elementType;
        final int scale = bigDecimalType.scale;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                BigDecimal element = bigDecimals[i];
                if(element.scale() != scale) {
                    throw new IllegalArgumentException("unexpected scale: " + element.scale());
                }
                bigDecimalType.validateBigIntBitLen(element.unscaledValue());
            }
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    /**
     * For arrays of arrays or arrays of tuples only.
     */
    private int validateObjectArray(Object[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int byteLength = elementType.dynamic ? len << LOG_2_UNIT_LENGTH_BYTES : 0; // 32 bytes per offset
        int i = 0;
        try {
            for ( ; i < len; i++) {
                byteLength += elementType.validate(arr[i]);
            }
        } catch (RuntimeException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return byteLength;
    }

    private int checkLength(final int valueLength, Object value) {
        final int expected = this.length;
        if(expected != DYNAMIC_LENGTH && valueLength != expected) {
            String msg =
                    Utils.friendlyClassName(value.getClass(), valueLength) + " not instanceof " +
                            Utils.friendlyClassName(clazz, expected) + ", " +
                            valueLength + " != " + expected;
            throw new IllegalArgumentException(msg);
        }
        return valueLength;
    }

    @Override
    void encodeHead(Object value, ByteBuffer dest, int[] offset) {
        if (dynamic) { // includes String
            Encoding.insertOffset(offset, this, value, dest);
        } else {
            encodeArrayTail(value, dest);
        }
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        if(isString) {
            byte[] bytes = ((String) value).getBytes(CHARSET_UTF_8);
            Encoding.insertInt(bytes.length, dest); // insertLength
            insertBytes(bytes, dest);
        } else {
            encodeArrayTail(value, dest);
        }
    }

    private void encodeArrayTail(Object value, ByteBuffer dest) {
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            boolean[] booleans = (boolean[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(booleans.length, dest);
            }
            insertBooleans(booleans, dest);
            return;
        case TYPE_CODE_BYTE:
            byte[] bytes = (byte[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(bytes.length, dest);
            }
            insertBytes(bytes, dest);
            return;
        case TYPE_CODE_INT:
            int[] ints = (int[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(ints.length, dest);
            }
            insertInts(ints, dest);
            return;
        case TYPE_CODE_LONG:
            long[] longs = (long[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(longs.length, dest);
            }
            insertLongs(longs, dest);
            return;
        case TYPE_CODE_BIG_INTEGER:
            BigInteger[] bigInts = (BigInteger[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(bigInts.length, dest);
            }
            insertBigIntegers(bigInts, dest);
            return;
        case TYPE_CODE_BIG_DECIMAL:
            BigDecimal[] bigDecs = (BigDecimal[]) value;
            if(length == DYNAMIC_LENGTH) {
                Encoding.insertInt(bigDecs.length, dest);
            }
            insertBigDecimals(bigDecs, dest);
            return;
        case TYPE_CODE_ARRAY:  // type for String[] has elementType.typeCode() == TYPE_CODE_ARRAY
        case TYPE_CODE_TUPLE:
            final Object[] objects = (Object[]) value;
            final int len = objects.length;
            if(dynamic) {
                if(length == DYNAMIC_LENGTH) {
                    Encoding.insertInt(len, dest); // insertLength
                }
                if (elementType.dynamic) { // if elements are dynamic
                    final int[] offset = new int[] { len << LOG_2_UNIT_LENGTH_BYTES }; // mul 32 (0x20)
                    for (int i = 0; i < len; i++) {
                        Encoding.insertOffset(offset, elementType, objects[i], dest);
                    }
                }
            }
            for (int i = 0; i < len; i++) {
                elementType.encodeTail(objects[i], dest);
            }
            return;
        default:
            throw new IllegalArgumentException("unexpected array element type: " + elementType.toString());
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
        case TYPE_CODE_ARRAY:  return (J) decodeObjectArray(arrayLen, bb, elementBuffer, false);
        case TYPE_CODE_TUPLE: return (J) decodeObjectArray(arrayLen, bb, elementBuffer, true);
        default: throw unrecognizedType(elementType.toString());
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - 1; // Byte.BYTES
        for(int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer);
            for (int j = 0; j < booleanOffset; j++) {
                if(elementBuffer[j] != 0) {
                    throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - j));
                }
            }
            byte last = elementBuffer[booleanOffset];
            if(last == 1) {
                booleans[i] = true;
            } else if(last != 0) {
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
        }
        return booleans;
    }

    private Object decodeByteArray(ByteBuffer bb, int arrayLen) {
        final int mark = bb.position();
        byte[] out = new byte[arrayLen];
        bb.get(out);
        bb.position(mark + roundLengthUp(arrayLen));
        if(isString) {
            return new String(out, CHARSET_UTF_8);
        }
        return out;
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = getIntElement(intType, bb, elementBuffer);
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = getLongElement(longType, bb, elementBuffer);
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = getBigIntElement(bigIntegerType, bb, elementBuffer);
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(getBigIntElement(bigDecimalType, bb, elementBuffer), scale);
        }
        return bigDecs;
    }

    private static int getIntElement(UnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(elementBuffer);
        type.validateBigIntElement(bi);
        return bi.intValue();
    }

    private static long getLongElement(UnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(elementBuffer);
        type.validateBigIntElement(bi);
        return bi.longValue();
    }

    private static BigInteger getBigIntElement(UnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bigInt = new BigInteger(elementBuffer);
        type.validateBigIntElement(bigInt);
        return bigInt;
    }

    private Object[] decodeObjectArray(int arrayLen, ByteBuffer bb, byte[] elementBuffer, boolean tupleArray) {

//        final int index = bb.position(); // TODO must pass index to decodeObjectArrayTails if you want to support lenient mode

        final ABIType<?> elementType = this.elementType;
        Object[] dest;
        if(tupleArray) {
            dest = new Tuple[arrayLen];
        } else {
            dest = (Object[]) Array.newInstance(elementType.clazz, arrayLen); // reflection ftw
        }

        int[] offsets = new int[arrayLen];

        decodeObjectArrayHeads(elementType, bb, offsets, elementBuffer, dest);

        if(this.dynamic) {
            decodeObjectArrayTails(elementType, bb, offsets, elementBuffer, dest);
        }
        return dest;
    }

    private static void decodeObjectArrayHeads(ABIType<?> elementType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int len = offsets.length;
        if(elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoding.OFFSET_TYPE.decode(bb, elementBuffer);
            }
        } else {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private static void decodeObjectArrayTails(ABIType<?> elementType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                }
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private static RuntimeException unrecognizedType(String type) {
        return new RuntimeException("unrecognized type: " + type);
    }

    @Override
    public J parseArgument(String s) {
        throw new UnsupportedOperationException();
    }

    /**
     * Rounds a length up to the nearest multiple of 32. If {@code len} is already a multiple, method has no effect.
     * @param len   the length, a non-negative integer
     * @return  the rounded-up value
     */
    public static int roundLengthUp(int len) {
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }
}
