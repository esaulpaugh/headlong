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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.ABIType.*;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;

/**
 * Experimental. Unoptimized.
 */
public final class PackedDecoder {

    public static Tuple decode(TupleType tupleType, byte[] buffer) throws ValidationException {
        int numDynamic = 0;
        for (ABIType<?> type : tupleType) {
            if (type.dynamic) {
                numDynamic++;
            }
        }
        if (numDynamic == 0) {
            return decodeTupleStatic(tupleType, buffer);
        }
        if (numDynamic == 1) {
            return decodeTopTuple(tupleType, buffer, buffer.length);
        }
        throw new IllegalArgumentException("multiple dynamic elements");
    }

    private static Tuple decodeTopTuple(TupleType tupleType, byte[] buffer, int end) throws ValidationException {

        final ABIType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];

        int idx = end;

        Integer mark = null;

        for (int i = len - 1; i >= 0; i--) {
            final ABIType<?> type = elementTypes[i];
            if (type.dynamic) {
                mark = i;
                break;
            }

            if(type.typeCode() == TYPE_CODE_ARRAY) {
                final ArrayType<? extends ABIType<?>, ?> arrayType = (ArrayType<?, ?>) type;
                end = idx -= (arrayType.elementType.byteLengthPacked(null) * arrayType.length);
                idx -= decodeArrayDynamic(arrayType, buffer, idx, end, elements, i);
            } else if(type.typeCode() == TYPE_CODE_TUPLE) {
                throw new UnsupportedOperationException("nested tuple");
            } else {
                end = idx -= decode(elementTypes[i], buffer, idx - type.byteLengthPacked(null), end, elements, i);
            }
        }

        if (mark != null) {
            final int m = mark;
            idx = 0;
            for (int i = 0; i <= m; i++) {
                idx += decode(elementTypes[i], buffer, idx, end, elements, i);
            }
        }

        return new Tuple(elements);
    }

    private static int decode(ABIType<?> type, byte[] buffer, int idx, int end, Object[] elements, int i) throws ValidationException {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: elements[i] = BooleanType.decodeBoolean(buffer[idx]); return type.byteLengthPacked(null);
        case TYPE_CODE_BYTE: elements[i] = buffer[idx]; return type.byteLengthPacked(null);
        case TYPE_CODE_INT: return decodeInt(type.byteLengthPacked(null), (IntType) type, buffer, idx, elements, i);
        case TYPE_CODE_LONG: return decodeLong(type.byteLengthPacked(null), (LongType) type, buffer, idx, elements, i);
        case TYPE_CODE_BIG_INTEGER: return decodeBigInteger(type.byteLengthPacked(null), (BigIntegerType) type, buffer, idx, elements, i);
        case TYPE_CODE_BIG_DECIMAL: return decodeBigDecimal(type.byteLengthPacked(null), (BigDecimalType) type, buffer, idx, elements, i);
        case TYPE_CODE_ARRAY: return decodeArrayDynamic((ArrayType<?, ?>) type, buffer, idx, end, elements, i);
        }
        throw new UnsupportedOperationException("nested tuple?"); // idx += decodeTupleDynamic((TupleType) type, buffer, idx, end, elements, i); break;
    }

    private static Tuple decodeTupleStatic(TupleType tupleType, byte[] buffer) throws ValidationException {
        final ABIType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];
        int idx = 0;
        final int end = buffer.length;
        for (int i = 0; i < len; i++) {
            idx += decode(elementTypes[i], buffer, idx, end, elements, i);
        }

        Tuple tuple = new Tuple(elements);
        tupleType.validate(tuple);
        return tuple;
    }

    private static int decodeInt(int elementLen, IntType intType, byte[] buffer, int idx, Object[] dest, int destIdx) throws ValidationException {
        Integer val = getPackedInt(buffer, idx, elementLen);
        intType.validate(val);
        dest[destIdx] = val;
        return elementLen;
    }

    private static int decodeLong(int elementLen, LongType longType, byte[] buffer, int idx, Object[] dest, int destIdx) throws ValidationException {
        Long val = getPackedLong(buffer, idx, elementLen);
        longType.validate(val);
        dest[destIdx] = val;
        return elementLen;
    }

    private static int decodeBigInteger(int elementLen, BigIntegerType bigIntegerType, byte[] buffer, int idx, Object[] dest, int destIdx) throws ValidationException {
        BigInteger val = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen));
        bigIntegerType.validate(val);
        dest[destIdx] = val;
        return elementLen;
    }

    private static int decodeBigDecimal(int elementLen, BigDecimalType bigDecimalType, byte[] buffer, int idx, Object[] dest, int destIdx) throws ValidationException {
        BigInteger bigInteger = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen));
        BigDecimal val = new BigDecimal(bigInteger, bigDecimalType.scale);
        bigDecimalType.validate(val);
        dest[destIdx] = val;
        return elementLen;
    }

    private static int decodeArrayDynamic(ArrayType<? extends ABIType<?>, ?> arrayType, byte[] buffer, int idx, int end, Object[] dest, int destIdx) throws ValidationException {
        final ABIType<?> elementType = arrayType.elementType;
        final int elementByteLen;
        try {
            elementByteLen = elementType.byteLengthPacked(null);
        } catch (NullPointerException npe) {
            throw new IllegalArgumentException("nested array");
        }

        final int arrayLen = arrayType.length == DYNAMIC_LENGTH ? (end - idx) / elementByteLen : arrayType.length;
        final Object array;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: array = decodeBooleanArray(arrayLen, buffer, idx); break;
        case TYPE_CODE_BYTE: array = decodeByteArray(arrayLen, buffer, idx); break;
        case TYPE_CODE_INT: array = decodeIntArray(elementByteLen, elementType, arrayLen, buffer, idx); break;
        case TYPE_CODE_LONG: array = decodeLongArray(elementByteLen, elementType, arrayLen, buffer, idx); break;
        case TYPE_CODE_BIG_INTEGER: array = decodeBigIntegerArray(elementByteLen, elementType, arrayLen, buffer, idx); break;
        case TYPE_CODE_BIG_DECIMAL: array = decodeBigDecimalArray(elementByteLen, (BigDecimalType) elementType, arrayLen, buffer, idx); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: throw new UnsupportedOperationException();
        default: throw new IllegalArgumentException("unexpected array type: " + arrayType.toString());
        }
        dest[destIdx] = array;
        return arrayLen * elementByteLen;
    }

    private static boolean[] decodeBooleanArray(int arrayLen, byte[] buffer, int idx) {
        boolean[] booleans = new boolean[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            booleans[i] = BooleanType.decodeBoolean(buffer[idx + i]);
        }
        return booleans;
    }

    private static byte[] decodeByteArray(int arrayLen, byte[] buffer, int idx) {
        byte[] bytes = new byte[arrayLen];
        System.arraycopy(buffer, idx, bytes, 0, arrayLen);
        return bytes;
    }

    private static int[] decodeIntArray(int elementLen, ABIType<?> elementType, int arrayLen, byte[] buffer, int idx) throws ValidationException {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            Integer val = getPackedInt(buffer, idx, elementLen);
            elementType.validate(val);
            ints[i] = val;
            idx += elementLen;
        }
        return ints;
    }

    private static long[] decodeLongArray(int elementLen, ABIType<?> longType, int arrayLen, byte[] buffer, int idx) throws ValidationException {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            Long val = getPackedLong(buffer, idx, elementLen);
            longType.validate(val);
            longs[i] = val;
            idx += elementLen;
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(int elementLen, ABIType<?> elementType, int arrayLen, byte[] buffer, int idx) throws ValidationException {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            BigInteger val = com.esaulpaugh.headlong.util.Integers.getBigInt(buffer, idx, elementLen);
            elementType.validate(val);
            bigInts[i] = val;
            idx += elementLen;
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(int elementLen, BigDecimalType elementType, int arrayLen, byte[] buffer, int idx) throws ValidationException {
        BigDecimal[] bigDecimals = new BigDecimal[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            BigDecimal val = new BigDecimal(com.esaulpaugh.headlong.util.Integers.getBigInt(buffer, idx, elementLen), elementType.scale);
            elementType.validate(val);
            bigDecimals[i] = val;
            idx += elementLen;
        }
        return bigDecimals;
    }

    static int getPackedInt(byte[] buffer, int i, int len) {
        int shiftAmount = 0;
        int val = 0;
        byte leftmost;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= ((leftmost = buffer[i]) & 0xFF) << shiftAmount; break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(leftmost < 0) { // if negative
            // sign extend
            switch (len) {
            case 1: return val | 0xFFFFFF00;
            case 2: return val | 0xFFFF0000;
            case 3: return val | 0xFF000000;
            }
        }
        return val;
    }

    static long getPackedLong(final byte[] buffer, final int i, final int len) {
        int shiftAmount = 0;
        long val = 0L;
        byte leftmost;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val = buffer[i+7] & 0xFFL; shiftAmount = Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= ((leftmost = buffer[i]) & 0xFFL) << shiftAmount; break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(leftmost < 0) {
            // sign extend
            switch (len) { /* cases fall through */
            case 1: return val | 0xFFFFFFFFFFFFFF00L;
            case 2: return val | 0xFFFFFFFFFFFF0000L;
            case 3: return val | 0xFFFFFFFFFF000000L;
            case 4: return val | 0xFFFFFFFF00000000L;
            case 5: return val | 0xFFFFFF0000000000L;
            case 6: return val | 0xFFFF000000000000L;
            case 7: return val | 0xFF00000000000000L;
            }
        }
        return val;
    }
}
