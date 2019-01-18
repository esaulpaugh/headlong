package com.esaulpaugh.headlong.abi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;
import static com.esaulpaugh.headlong.abi.StackableType.*;

class CallEncoder {

    static final int OFFSET_LENGTH_BYTES = UNIT_LENGTH_BYTES;
    static final IntType OFFSET_TYPE = new IntType("int32", Integer.SIZE, false);

    static final byte NEGATIVE_ONE_BYTE = (byte) 0xFF;
    private static final byte ZERO_BYTE = (byte) 0;

    private static final byte[] BOOLEAN_FALSE = new byte[UNIT_LENGTH_BYTES];
    private static final byte[] BOOLEAN_TRUE = new byte[UNIT_LENGTH_BYTES];

    private static final byte[] NON_NEGATIVE_INT_PADDING = new byte[24];
    private static final byte[] NEGATIVE_INT_PADDING = new byte[24];

    static {
        BOOLEAN_TRUE[BOOLEAN_TRUE.length-1] = 1;
        Arrays.fill(NEGATIVE_INT_PADDING, NEGATIVE_ONE_BYTE);
    }

    static int calcEncodingLength(Function f, Tuple argsTuple, boolean validate) {
        final int bodyLen = validate ? f.paramTypes.validate(argsTuple) : f.paramTypes.byteLength(argsTuple);
        return Function.SELECTOR_LEN + bodyLen;
    }

    static ByteBuffer encodeCall(Function function, Tuple argsTuple) {
        return encodeCall(function, argsTuple, calcEncodingLength(function, argsTuple, true));
    }

    static ByteBuffer encodeCall(Function function, Tuple argsTuple, int allocation) {
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
        encodeCall(function, argsTuple, outBuffer);
        return outBuffer;
    }

    static void encodeCall(Function function, Tuple argsTuple, ByteBuffer dest) {
        dest.put(function.selector);
        insertTuple(function.paramTypes, argsTuple, dest);
    }

    static void insertTuple(TupleType tupleType, Tuple tuple, ByteBuffer outBuffer) {
        final StackableType<?>[] types = tupleType.elementTypes;
        final Object[] values = tuple.elements;
        final int[] offset = new int[] { headLengthSum(types, values) };

        final int len = types.length;
        int i;
        for (i = 0; i < len; i++) {
            encodeHead(types[i], values[i], outBuffer, offset);
        }
        if(tupleType.dynamic) {
            for (i = 0; i < len; i++) {
                StackableType<?> type = types[i];
                if (type.dynamic) {
                    encodeTail(type, values[i], outBuffer);
                }
            }
        }
    }

    private static int headLengthSum(StackableType<?>[] elementTypes, Object[] elements) {
        int headLengths = 0;
        final int n = elementTypes.length;
        for (int i = 0; i < n; i++) {
            StackableType<?> t = elementTypes[i];
            if(t.dynamic) {
                headLengths += OFFSET_LENGTH_BYTES;
            } else {
                headLengths += t.byteLength(elements[i]);
            }
        }
        return headLengths;
    }

    private static void encodeHead(StackableType<?> type, Object value, ByteBuffer dest, int[] offset) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBool((boolean) value, dest); return;
        case TYPE_CODE_BYTE:
        case TYPE_CODE_SHORT:
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: insertInt(((Number) value).longValue(), dest); return;
        case TYPE_CODE_BIG_INTEGER: insertInt(((BigInteger) value), dest); return;
        case TYPE_CODE_BIG_DECIMAL: insertInt(((BigDecimal) value).unscaledValue(), dest); return;
        case TYPE_CODE_ARRAY:
            if (type.dynamic) { // includes String
                insertOffset(offset, type, value, dest);
            } else {
                encodeArrayStatic((ArrayType<?, ?>) type, value, dest);
            }
            return;
        case TYPE_CODE_TUPLE:
            if (type.dynamic) {
                insertOffset(offset, type, value, dest);
            } else {
                insertTuple((TupleType) type, (Tuple) value, dest);
            }
            return;
        default:
            throw new IllegalArgumentException("unexpected array type: " + type.toString());
        }
    }

    private static void insertOffset(final int[] offset, StackableType<?> paramType, Object object, ByteBuffer dest) {
        final int val = offset[0];
        insertInt(val, dest);
        offset[0] = val + paramType.byteLength(object);
    }

    private static void encodeTail(StackableType<?> type, Object value, ByteBuffer dest) {
//        only dynamics expected
        switch (type.typeCode()) {
        case TYPE_CODE_ARRAY:
            final ArrayType<?, ?> arrayType = (ArrayType<?, ?>) type;
            if(arrayType.isString) {
                byte[] bytes = ((String) value).getBytes(CHARSET_UTF_8);
                insertInt(bytes.length, dest); // insertLength
                insertBytes(bytes, dest);
            } else {
                encodeArrayTail(arrayType, value, dest);
            }
            return;
        case TYPE_CODE_TUPLE:
            insertTuple((TupleType) type, (Tuple) value, dest);
            return;
        default: throw new IllegalArgumentException("unrecognized type: " + type.toString());
        }
    }

    // ----------------------------------------------

    private static void encodeArrayStatic(ArrayType<?, ?> arrayType, Object value, ByteBuffer dest) {
        switch (arrayType.elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBooleans((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: insertBytes((byte[]) value, dest); return;
        case TYPE_CODE_SHORT: insertShorts((short[]) value, dest); return;
        case TYPE_CODE_INT: insertInts((int[]) value, dest); return;
        case TYPE_CODE_LONG: insertLongs((long[]) value, dest); return;
        case TYPE_CODE_BIG_INTEGER: insertBigIntegers((BigInteger[]) value, dest); return;
        case TYPE_CODE_BIG_DECIMAL: insertBigDecimals((BigDecimal[]) value, dest); return;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final StackableType<?> elementType = arrayType.elementType;
            for(Object e : (Object[]) value) {
                encodeHead(elementType, e, dest, null);
            }
            return;
        default: throw new IllegalArgumentException("unexpected array type: " + arrayType.toString());
        }
    }

    private static void encodeArrayTail(ArrayType<?, ?> arrayType, Object value, ByteBuffer dest) {
        final StackableType<?> elementType = arrayType.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            boolean[] booleans = (boolean[]) value;
            if(arrayType.dynamic) {
                insertInt(booleans.length, dest);
            }
            insertBooleans(booleans, dest);
            return;
        case TYPE_CODE_BYTE:
            byte[] bytes = (byte[]) value;
            if(arrayType.dynamic) {
                insertInt(bytes.length, dest);
            }
            insertBytes(bytes, dest);
            return;
        case TYPE_CODE_SHORT:
            short[] shorts = (short[]) value;
            if(arrayType.dynamic) {
                insertInt(shorts.length, dest);
            }
            insertShorts(shorts, dest);
            return;
        case TYPE_CODE_INT:
            int[] ints = (int[]) value;
            if(arrayType.dynamic) {
                insertInt(ints.length, dest);
            }
            insertInts(ints, dest);
            return;
        case TYPE_CODE_LONG:
            long[] longs = (long[]) value;
            if(arrayType.dynamic) {
                insertInt(longs.length, dest);
            }
            insertLongs(longs, dest);
            return;
        case TYPE_CODE_BIG_INTEGER:
            BigInteger[] bigInts = (BigInteger[]) value;
            if(arrayType.dynamic) {
                insertInt(bigInts.length, dest);
            }
            insertBigIntegers(bigInts, dest);
            return;
        case TYPE_CODE_BIG_DECIMAL:
            BigDecimal[] bigDecs = (BigDecimal[]) value;
            if(arrayType.dynamic) {
                insertInt(bigDecs.length, dest);
            }
            insertBigDecimals(bigDecs, dest);
            return;
        case TYPE_CODE_ARRAY:  // type for String[] has TYPE_CODE_ARRAY
        case TYPE_CODE_TUPLE:
            final Object[] objects = (Object[]) value;
            final int len = objects.length;
            if(arrayType.dynamic) {
                insertInt(len, dest); // insertLength
                if (elementType.dynamic) { // if elements are dynamic
                    final int[] offset = new int[] { len << 5 }; // mul 32 (0x20)
                    for (int i = 0; i < len; i++) {
                        insertOffset(offset, elementType, objects[i], dest);
                    }
                }
            }
            for (int i = 0; i < len; i++) {
                encodeTail(elementType, objects[i], dest);
            }
            return;
        default:
            throw new IllegalArgumentException("unexpected array element type: " + elementType.toString());
        }
    }

    // -------------------------------------------------------------------------------------------------

    private static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        }
    }

    private static int paddingLength(int len) {
        int mod = len & 31;
        return mod == 0
                ? 0
                : 32 - mod;
    }

    private static void insertBytes(byte[] bytes, ByteBuffer dest) {
        dest.put(bytes);
        final int paddingLength = paddingLength(bytes.length);
        for (int i = 0; i < paddingLength; i++) {
            dest.put(ZERO_BYTE);
        }
    }

    private static void insertShorts(short[] shorts, ByteBuffer dest) {
        for (short e : shorts) {
            insertInt(e, dest);
        }
    }

    private static void insertInts(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

    private static void insertLongs(long[] longs, ByteBuffer dest) {
        for (long e : longs) {
            insertInt(e, dest);
        }
    }

    private static void insertBigIntegers(BigInteger[] bigInts, ByteBuffer dest) {
        for (BigInteger e : bigInts) {
            insertInt(e, dest);
        }
    }

    private static void insertBigDecimals(BigDecimal[] bigDecs, ByteBuffer dest) {
        for (BigDecimal e : bigDecs) {
            insertInt(e.unscaledValue(), dest);
        }
    }

    // ------------------------------------------------------------------------------

    private static void insertInt(long val, ByteBuffer dest) {
        dest.put(val < 0 ? NEGATIVE_INT_PADDING : NON_NEGATIVE_INT_PADDING);
        dest.putLong(val);
    }

    private static void insertInt(BigInteger bigGuy, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        final byte paddingByte = bigGuy.signum() == -1 ? NEGATIVE_ONE_BYTE : ZERO_BYTE;
        final int lim = 32 - arr.length;
        for (int i = 0; i < lim; i++) {
            dest.put(paddingByte);
        }
        dest.put(arr);
    }

    private static void insertBool(boolean bool, ByteBuffer dest) {
        dest.put(bool ? BOOLEAN_TRUE : BOOLEAN_FALSE);
    }
}
