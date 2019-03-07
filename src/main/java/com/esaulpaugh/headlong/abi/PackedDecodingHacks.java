package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Integers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.StackableType.*;

/**
 * Experimental. Unoptimized.
 */
public class PackedDecodingHacks {

    public static Tuple decode(TupleType tupleType, byte[] buffer) {

        int numDynamic = 0;

        for (StackableType<?> type : tupleType.elementTypes) {
            if (type.dynamic) {
                numDynamic++;
            }
        }

        switch (numDynamic) {
        case 0: return decodeTupleStatic(tupleType, buffer);
        case 1: return decodeTopTuple(tupleType, buffer, buffer.length);
        default: throw new IllegalArgumentException("multiple dynamic elements");
        }
    }

    private static Tuple decodeTopTuple(TupleType tupleType, byte[] buffer, int end) {

        final StackableType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];

        int idx = end;

        Integer mark = null;

        for (int i = len - 1; i >= 0; i--) {
            final StackableType<?> type = elementTypes[i];
            if (type.dynamic) {
                mark = i;
                break;
            }
            switch (type.typeCode()) {
            case TYPE_CODE_BOOLEAN: idx--; end = idx; elements[i] = decodeBoolean(buffer, idx); break;
            case TYPE_CODE_BYTE: idx--; end = idx; elements[i] = buffer[idx]; break;
            case TYPE_CODE_INT: idx -= type.byteLengthPacked(null); end = idx; decodeInt((IntType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_LONG: idx -= type.byteLengthPacked(null); end = idx; decodeLong((LongType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_BIG_INTEGER: idx -= type.byteLengthPacked(null); end = idx; decodeBigInteger((BigIntegerType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_BIG_DECIMAL: idx -= type.byteLengthPacked(null); end = idx; decodeBigDecimal((BigDecimalType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_ARRAY: {
                final ArrayType arrayType = (ArrayType) type;
                end = idx = idx - (arrayType.elementType.byteLengthPacked(null) * arrayType.length);
                idx -= decodeArrayDynamic(arrayType, buffer, idx, end, elements, i);
                break;
            }
            case TYPE_CODE_TUPLE: throw new UnsupportedOperationException("nested tuple"); // idx -= decodeTupleDynamic(tupleType, buffer, 0, idx, elements, i); break;
            }
        }

        if (mark != null) {
            final int m = mark;
            idx = 0;
            for (int i = 0; i <= m; i++) {
                final StackableType<?> type = elementTypes[i];
                switch (type.typeCode()) {
                case TYPE_CODE_BOOLEAN: elements[i] = decodeBoolean(buffer, idx); idx++; break;
                case TYPE_CODE_BYTE: elements[i] = buffer[idx]; idx++; break;
                case TYPE_CODE_INT: idx += decodeInt((IntType) type, buffer, idx, elements, i); break;
                case TYPE_CODE_LONG: idx += decodeLong((LongType) type, buffer, idx, elements, i); break;
                case TYPE_CODE_BIG_INTEGER: idx += decodeBigInteger((BigIntegerType) type, buffer, idx, elements, i); break;
                case TYPE_CODE_BIG_DECIMAL: idx += decodeBigDecimal((BigDecimalType) type, buffer, idx, elements, i); break;
                case TYPE_CODE_ARRAY: idx += (i == m
                        ? decodeArrayDynamic((ArrayType) type, buffer, idx, end, elements, i)
                        : decodeArrayDynamic((ArrayType) type, buffer, idx, end, elements, i)); break;
                case TYPE_CODE_TUPLE: throw new UnsupportedOperationException("nested tuple"); // idx += decodeTupleDynamic((TupleType) type, buffer, idx, end, elements, i); break;
                }
            }
        }

        return new Tuple(elements);
    }

    private static Tuple decodeTupleStatic(TupleType tupleType, byte[] buffer) {

        int idx = 0;
        final int end = buffer.length;

        final StackableType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];
        for (int i = 0; i < len; i++) {
            final StackableType<?> type = elementTypes[i];
            switch (type.typeCode()) {
            case TYPE_CODE_BOOLEAN: elements[i] = decodeBoolean(buffer, idx); idx++; break;
            case TYPE_CODE_BYTE: elements[i] = buffer[idx]; idx++; break;
            case TYPE_CODE_INT: idx += decodeInt((IntType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_LONG: idx += decodeLong((LongType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_BIG_INTEGER: idx += decodeBigInteger((BigIntegerType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_BIG_DECIMAL: idx += decodeBigDecimal((BigDecimalType) type, buffer, idx, elements, i); break;
            case TYPE_CODE_ARRAY: idx += (type.dynamic
                    ? decodeArrayDynamic((ArrayType) type, buffer, idx, end, elements, i)
                    : decodeArrayDynamic((ArrayType) type, buffer, idx, end, elements, i)); break;
            case TYPE_CODE_TUPLE: throw new UnsupportedOperationException("nested tuple"); // idx += decodeTupleDynamic((TupleType) type, buffer, idx, end, elements, i); break;
            }
        }

        Tuple tuple = new Tuple(elements);
        tupleType.validate(tuple);
        return tuple;
    }

    private static Boolean decodeBoolean(byte[] buffer, int idx) {
        byte b = buffer[idx];
        if (b == 0) {
            return Boolean.FALSE;
        }
        if (b == 1) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException("invalid boolean value");
    }

    private static int decodeInt(IntType intType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = intType.byteLengthPacked(null);
        Integer val = Integers.getInt(buffer, idx, len);
        intType.validate(val);
        dest[destIdx] = val;
        return len;
    }

    private static int decodeLong(LongType longType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = longType.byteLengthPacked(null);
        Long val = Integers.getLong(buffer, idx, len);
        longType.validate(val);
        dest[destIdx] = val;
        return len;
    }

    private static int decodeBigInteger(BigIntegerType bigIntegerType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = bigIntegerType.byteLengthPacked(null);
        BigInteger val = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + len));
        bigIntegerType.validate(val);
        dest[destIdx] = val;
        return len;
    }

    private static int decodeBigDecimal(BigDecimalType bigDecimalType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = bigDecimalType.byteLengthPacked(null);
        BigInteger bigInteger = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + len));
        BigDecimal val = new BigDecimal(bigInteger, bigDecimalType.scale);
        bigDecimalType.validate(val);
        dest[destIdx] = val;
        return len;
    }

    private static int decodeArrayDynamic(ArrayType arrayType, byte[] buffer, int idx, int end, Object[] dest, int destIdx) {
        final StackableType<?> elementType = arrayType.elementType;
        final int byteLen;
        try {
            byteLen = elementType.byteLengthPacked(null);
        } catch (NullPointerException npe) {
            throw new IllegalArgumentException("nested array");
        }

        final int arrayLen;
        if (arrayType.length != -1) {
            arrayLen = arrayType.length;
        } else {
            arrayLen = (end - idx) / byteLen;
        }

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return decodeBooleanArray(arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_BYTE: return decodeByteArray(arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_INT: return decodeIntArray(arrayType.elementType, arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_LONG: return decodeLongArray(arrayType.elementType, arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_BIG_INTEGER: return decodeBigIntegerArray(arrayType.elementType, arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_BIG_DECIMAL: return decodeBigDecimalArray((BigDecimalType) arrayType.elementType, arrayLen, buffer, idx, dest, destIdx) * byteLen;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: throw new UnsupportedOperationException();
        default: throw new IllegalArgumentException("unexpected array type: " + arrayType.toString());
        }
    }

    private static int decodeBooleanArray(int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        boolean[] booleans = new boolean[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            booleans[i] = decodeBoolean(buffer, idx + i);
        }
        dest[destIdx] = booleans;
        return arrayLen;
    }

    private static int decodeByteArray(int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        byte[] bytes = new byte[arrayLen];
        System.arraycopy(buffer, idx, bytes, 0, arrayLen);
        dest[destIdx] = bytes;
        return arrayLen;
    }

    private static int decodeIntArray(StackableType<?> elementType, int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = elementType.byteLengthPacked(null);
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            Integer val = Integers.getInt(buffer, idx, len);
            elementType.validate(val);
            ints[i] = val;
            idx += len;
        }
        dest[destIdx] = ints;
        return arrayLen;
    }

    private static int decodeLongArray(StackableType<?> longType, int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = longType.byteLengthPacked(null);
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            Long val = Integers.getLong(buffer, idx, len);
            longType.validate(val);
            longs[i] = val;
            idx += len;
        }
        dest[destIdx] = longs;
        return arrayLen;
    }

    private static int decodeBigIntegerArray(StackableType<?> elementType, int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = elementType.byteLengthPacked(null);
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            BigInteger val = com.esaulpaugh.headlong.rlp.util.Integers.getBigInt(buffer, idx, len);
            elementType.validate(val);
            bigInts[i] = val;
            idx += len;
        }
        dest[destIdx] = bigInts;
        return arrayLen;
    }

    private static int decodeBigDecimalArray(BigDecimalType elementType, int arrayLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
        final int len = elementType.byteLengthPacked(null);
        BigDecimal[] bigDecimals = new BigDecimal[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            BigDecimal val = new BigDecimal(com.esaulpaugh.headlong.rlp.util.Integers.getBigInt(buffer, idx, len), elementType.scale);
            elementType.validate(val);
            bigDecimals[i] = val;
            idx += len;
        }
        dest[destIdx] = bigDecimals;
        return arrayLen;
    }
}
