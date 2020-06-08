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

import com.esaulpaugh.headlong.abi.util.Uint;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;

/**
 * Experimental. Unoptimized.
 */
public final class PackedDecoder {

    public static Tuple decode(TupleType types, byte[] buffer) {
        return decode(types, buffer, 0, buffer.length);
    }

    public static Tuple decode(TupleType tupleType, byte[] buffer, int from, int to) {
        if (countDynamicsTupleType(tupleType) <= 1) {
            final Tuple[] elements = new Tuple[1];
            decodeTuple(tupleType, buffer, from, to, elements, 0); // can also call decodeTupleStatic if numDynamic == 0
            final Tuple tuple = elements[0];
            tupleType.validate(tuple);
            return tuple;
        }
        throw new IllegalArgumentException("multiple dynamic elements");
    }

    private static int countDynamicsTupleType(TupleType tupleType) {
        int numDynamic = 0;
        for (ABIType<?> e : tupleType) {
            numDynamic += !e.dynamic
                    ? 0
                    : TYPE_CODE_TUPLE == e.typeCode()
                        ? countDynamicsTupleType((TupleType) e)
                        : countDynamicsArrayType(e); // assume ArrayType bc currently only TupleType and ArrayType can be dynamic
        }
        return numDynamic;
    }

    private static int countDynamicsArrayType(ABIType<?> type) {
        int numDynamic = 0;
        do {
            final ArrayType<? extends ABIType<?>, ?> arrayType = (ArrayType<? extends ABIType<?>, ?>) type;
            if(DYNAMIC_LENGTH == arrayType.length) {
                numDynamic++;
            }
            type = arrayType.elementType;
        } while (TYPE_CODE_ARRAY == type.typeCode()); // loop until type is the base type
        if(TYPE_CODE_TUPLE == type.typeCode()) {
            numDynamic += countDynamicsTupleType((TupleType) type);
        }
        return numDynamic;
    }

    private static int decodeTuple(TupleType tupleType, byte[] buffer, int start, int end, Object[] parentElements, int pei) {
        final ABIType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];

        int mark = -1;

        for (int i = len - 1; i >= 0; i--) {
            final ABIType<?> type = elementTypes[i];
            if (type.dynamic) {
                mark = i;
                break;
            }
            // static types only
            final int typeCode = type.typeCode();
            if(TYPE_CODE_ARRAY == typeCode) {
                final ArrayType<? extends ABIType<?>, ?> arrayType = (ArrayType<? extends ABIType<?>, ?>) type;
                end -= arrayType.elementType.byteLengthPacked(null) * arrayType.length;
                decodeArray(arrayType, buffer, end, end, elements, i);
            } else if(TYPE_CODE_TUPLE == typeCode) {
                TupleType inner = (TupleType) type;
                int innerLen = inner.byteLengthPacked(null);
                end -= decodeTupleStatic(inner, buffer, end - innerLen, end, elements, i);
            } else {
                end -= decode(elementTypes[i], buffer, end - type.byteLengthPacked(null), end, elements, i);
            }
        }

        if (mark > -1) {
            for (int i = 0; i <= mark; i++) {
                start += decode(elementTypes[i], buffer, start, end, elements, i);
            }
        }
        return tupleType.byteLengthPacked(parentElements[pei] = new Tuple(elements));
    }

    private static int decode(ABIType<?> type, byte[] buffer, int idx, int end, Object[] elements, int i) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: elements[i] = BooleanType.decodeBoolean(buffer[idx]); return type.byteLengthPacked(null);
        case TYPE_CODE_BYTE: elements[i] = buffer[idx]; return type.byteLengthPacked(null);
        case TYPE_CODE_INT: return decodeInt(type.byteLengthPacked(null), (IntType) type, buffer, idx, elements, i);
        case TYPE_CODE_LONG: return decodeLong(type.byteLengthPacked(null), (LongType) type, buffer, idx, elements, i);
        case TYPE_CODE_BIG_INTEGER: return decodeBigInteger(type.byteLengthPacked(null), buffer, idx, elements, i);
        case TYPE_CODE_BIG_DECIMAL: return decodeBigDecimal(type.byteLengthPacked(null), ((BigDecimalType) type).scale, buffer, idx, elements, i);
        case TYPE_CODE_ARRAY: return decodeArray((ArrayType<? extends ABIType<?>, ?>) type, buffer, idx, end, elements, i);
        case TYPE_CODE_TUPLE:
            return type.dynamic
                    ? decodeTuple((TupleType) type, buffer, idx, end, elements, i)
                    : decodeTupleStatic((TupleType) type, buffer, idx, end, elements, i);
        default: throw new Error();
        }
    }

    private static int decodeTupleStatic(TupleType tupleType, byte[] buffer, int idx, int end, Object[] parentElements, int pei) {
        final ABIType<?>[] elementTypes = tupleType.elementTypes;
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];
        for (int i = 0; i < len; i++) {
            idx += decode(elementTypes[i], buffer, idx, end, elements, i);
        }
        return tupleType.byteLengthPacked(parentElements[pei] = new Tuple(elements));
    }

    private static int decodeInt(int elementLen, IntType intType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        int signed = getPackedInt(buffer, idx, elementLen);
        dest[destIdx] = intType.unsigned ? (int) new Uint(intType.getBitLength()).toUnsignedLong(signed) : signed;
        return elementLen;
    }

    private static int decodeLong(int elementLen, LongType longType, byte[] buffer, int idx, Object[] dest, int destIdx) {
        long signed = getPackedLong(buffer, idx, elementLen);
        dest[destIdx] = longType.unsigned ? new Uint(longType.getBitLength()).toUnsignedLong(signed) : signed;
        return elementLen;
    }

    private static int decodeBigInteger(int elementLen, byte[] buffer, int idx, Object[] dest, int destIdx) {
//        BigInteger val = new BigInteger(buffer, idx, elementLen); // Java 9+
        BigInteger val = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen));
        dest[destIdx] = val;
        return elementLen;
    }

    private static int decodeBigDecimal(int elementLen, int scale, byte[] buffer, int idx, Object[] dest, int destIdx) {
//        BigInteger unscaled = new BigInteger(buffer, idx, elementLen); // Java 9+
        BigInteger unscaled = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen));
        dest[destIdx] = new BigDecimal(unscaled, scale);
        return elementLen;
    }

    private static int decodeArray(ArrayType<? extends ABIType<?>, ?> arrayType, byte[] buffer, int idx, int end, Object[] dest, int destIdx) {
        final ABIType<?> elementType = arrayType.elementType;
        final int elementByteLen = elementType.byteLengthPacked(null);
        final int arrayLen;
        if(arrayType.length == DYNAMIC_LENGTH) {
            if (elementByteLen == 0) {
                throw new IllegalArgumentException("can't decode dynamic number of zero-length elements");
            }
            arrayLen = (end - idx) / elementByteLen;
        } else {
            arrayLen = arrayType.length;
        }
        final Object array;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: array = decodeBooleanArray(arrayLen, buffer, idx); break;
        case TYPE_CODE_BYTE: array = arrayType.encodeIfString(decodeByteArray(arrayLen, buffer, idx)); break;
        case TYPE_CODE_INT: array = decodeIntArray((IntType) elementType, elementByteLen, arrayLen, buffer, idx); break;
        case TYPE_CODE_LONG: array = decodeLongArray((LongType) elementType, elementByteLen, arrayLen, buffer, idx); break;
        case TYPE_CODE_BIG_INTEGER: array = decodeBigIntegerArray(elementByteLen, arrayLen, buffer, idx); break;
        case TYPE_CODE_BIG_DECIMAL: array = decodeBigDecimalArray(elementByteLen, ((BigDecimalType) elementType).scale, arrayLen, buffer, idx); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: array = decodeObjectArray(arrayLen, elementType, buffer, idx, end); break;
        default: throw new Error();
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

    private static int[] decodeIntArray(IntType intType, int elementLen, int arrayLen, byte[] buffer, int idx) {
        int[] ints = new int[arrayLen];
        Uint uint = new Uint(intType.bitLength);
        for (int i = 0; i < arrayLen; i++) {
            int signed = getPackedInt(buffer, idx, elementLen);
            ints[i] = intType.unsigned ? (int) uint.toUnsignedLong(signed) : signed;
            idx += elementLen;
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, int elementLen, int arrayLen, byte[] buffer, int idx) {
        long[] longs = new long[arrayLen];
        Uint uint = new Uint(longType.bitLength);
        for (int i = 0; i < arrayLen; i++) {
            long signed = getPackedLong(buffer, idx, elementLen);
            longs[i] = longType.unsigned ? uint.toUnsignedLong(signed) : signed;
            idx += elementLen;
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(int elementLen, int arrayLen, byte[] buffer, int idx) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen));
            idx += elementLen;
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(int elementLen, int scale, int arrayLen, byte[] buffer, int idx) {
        BigDecimal[] bigDecimals = new BigDecimal[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigDecimals[i] = new BigDecimal(new BigInteger(Arrays.copyOfRange(buffer, idx, idx + elementLen)), scale);
            idx += elementLen;
        }
        return bigDecimals;
    }

    private static Object[] decodeObjectArray(int arrayLen, ABIType<?> elementType, byte[] buffer, int idx, int end) {
        Object[] objects = (Object[]) Array.newInstance(elementType.clazz, arrayLen); // reflection ftw
        for (int i = 0; i < arrayLen; i++) {
            int len = decode(elementType, buffer, idx, end, objects, i);
            idx += len;
            end -= len;
        }
        return objects;
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
        if(leftmost < 0) { // if negative
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
