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
import com.esaulpaugh.headlong.util.Uint;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ADDRESS;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.ABIType.newUnitBuffer;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Experimental. Unoptimized.
 */
final class PackedDecoder {

    private PackedDecoder() {}

    static int checkDynamics(ABIType<?> type) {
        int count = 0;
        if (type.dynamic) {
            if (type instanceof ArrayType) {
                final ArrayType<?, ?, ?> at = type.asArrayType();
                count = checkDynamics(at.getElementType());
                if (DYNAMIC_LENGTH == at.getLength()) {
                    count++;
                }
            } else {
                for (ABIType<?> e : type.asTupleType().elementTypes) {
                    count += checkDynamics(e);
                }
            }
            if (count > 1) {
                throw new IllegalArgumentException("multiple dynamic elements: " + count);
            }
        }
        return count;
    }

    private static Tuple decodeTuple(TupleType<?> tupleType, ByteBuffer bb, int end) {
        final Object[] elements = new Object[tupleType.size()];
        int start = bb.position();
        int firstDynamicIndex = -1;
        for (int i = elements.length - 1; i >= 0; i--) {
            final ABIType<?> type = tupleType.get(i);
            if (type.dynamic) {
                firstDynamicIndex = i;
                break;
            }
            if (type instanceof ArrayType) {
                final ArrayType<ABIType<Object>, ?, ?> arrayType = type.asArrayType();
                final int elementByteLength = arrayType.getElementType() instanceof UnitType
                        ? UNIT_LENGTH_BYTES
                        : arrayType.getElementType().byteLengthPacked(null);
                end -= elementByteLength * arrayType.getLength();
                bb.position(end);
                elements[i] = decodeArray(arrayType, bb, -1);
            } else if (type instanceof TupleType) {
                end -= type.byteLengthPacked(null);
                bb.position(end);
                elements[i] = decodeTupleStatic(type.asTupleType(), bb);
            } else {
                end -= type.byteLengthPacked(null);
                bb.position(end);
                elements[i] = decode(type, bb, -1);
            }
        }

        for (int i = 0; i <= firstDynamicIndex; i++) {
            ABIType<Object> t = tupleType.get(i);
            bb.position(start);
            Object e = decode(t, bb, end);
            elements[i] = e;
            start += t.byteLengthPacked(e);
        }

        return Tuple.create(elements);
    }

    static Object decode(ABIType<?> type, ByteBuffer bb, int end) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return BooleanType.decodeBoolean(bb.get());
        case TYPE_CODE_BYTE: return bb.get();
        case TYPE_CODE_INT: return (int) decodeLong((IntType) type, bb, type.byteLengthPacked(null));
        case TYPE_CODE_LONG: return decodeLong((LongType) type, bb, type.byteLengthPacked(null));
        case TYPE_CODE_BIG_INTEGER: return decodeBigInteger((BigIntegerType) type, type.byteLengthPacked(null), bb);
        case TYPE_CODE_BIG_DECIMAL: return decodeBigDecimal((BigDecimalType) type, type.byteLengthPacked(null), bb);
        case TYPE_CODE_ARRAY: return decodeArray(type.asArrayType(), bb, end);
        case TYPE_CODE_TUPLE: return type.dynamic
                                        ? decodeTuple(type.asTupleType(), bb, end)
                                        : decodeTupleStatic(type.asTupleType(), bb);
        case TYPE_CODE_ADDRESS: return new Address(getBigInt(bb, type.byteLengthPacked(null)));
        default: throw new AssertionError();
        }
    }

    private static Tuple decodeTupleStatic(TupleType<?> tupleType, ByteBuffer bb) {
        final Object[] elements = new Object[tupleType.size()];
        for (int i = 0; i < elements.length; i++) {
            int prev = bb.position();
            ABIType<Object> t = tupleType.get(i);
            elements[i] = decode(t, bb, -1);
            bb.position(prev + t.byteLengthPacked(null));
        }
        return Tuple.create(elements);
    }

    private static BigInteger decodeBigInteger(BigIntegerType type, int elementLen, ByteBuffer bb) {
        if (type.unsigned) {
            return getBigInt(bb, elementLen);
        } else {
//            dest[destIdx] = new BigInteger(buffer, idx, elementLen); // Java 9+
            return getSignedBigInt(bb, elementLen);
        }
    }

    private static BigDecimal decodeBigDecimal(BigDecimalType type, int elementLen, ByteBuffer bb) {
//            unscaled = new BigInteger(buffer, idx, elementLen); // Java 9+
        return new BigDecimal(type.unsigned ? getBigInt(bb, elementLen) : getSignedBigInt(bb, elementLen), type.scale);
    }

    private static BigInteger getBigInt(ByteBuffer bb, int elementLen) {
        return Integers.getBigInt(bb.array(), bb.position(), elementLen, true);
    }

    private static BigInteger getSignedBigInt(ByteBuffer bb, int elementLen) {
        byte[] temp = new byte[elementLen];
        bb.get(temp);
        return new BigInteger(temp);
    }

    private static Object decodeArray(ArrayType<ABIType<Object>, ?, ?> arrayType, ByteBuffer bb, int end) {
        final ABIType<Object> elementType = arrayType.getElementType();
        final int elementByteLen = elementType instanceof UnitType ? UNIT_LENGTH_BYTES : elementType.byteLengthPacked(null);
        final int typeLen = arrayType.getLength();
        final int arrayLen;
        if (DYNAMIC_LENGTH == typeLen) {
            if (elementByteLen == 0) {
                throw new IllegalArgumentException("can't decode dynamic number of zero-length elements");
            }
            arrayLen = (end - bb.position()) / elementByteLen;
        } else {
            arrayLen = typeLen;
        }
        final Object array;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: array = decodeBooleanArray(arrayLen, bb); break;
        case TYPE_CODE_BYTE: array = decodeByteArray(arrayType, arrayLen, bb); break;
        case TYPE_CODE_INT: array = decodeIntArray(arrayLen, bb); break;
        case TYPE_CODE_LONG: array = decodeLongArray(arrayLen, bb); break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ADDRESS: array = decodeElements(elementType, arrayLen, bb); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: array = decodeObjectArray(elementType, arrayLen, bb); break;
        default: throw new AssertionError();
        }
        return array;
    }

    private static boolean[] decodeBooleanArray(int arrayLen, ByteBuffer bb) {
        boolean[] booleans = new boolean[arrayLen];
        byte[] unitBuffer = newUnitBuffer();
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = BooleanType.INSTANCE.decode(bb, unitBuffer);
        }
        return booleans;
    }

    private static Object decodeByteArray(ArrayType<?, ?, ?> arrayType, int arrayLen, ByteBuffer bb) {
        byte[] bytes = new byte[arrayLen];
        bb.get(bytes);
        return arrayType.encodeIfString(bytes);
    }

    private static int[] decodeIntArray(int arrayLen, ByteBuffer bb) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < ints.length; i++) {
            long value = decodeSignedLong(bb, UNIT_LENGTH_BYTES);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new ArithmeticException("overflow");
            }
            ints[i] = (int) value;
        }
        return ints;
    }

    private static long[] decodeLongArray(int arrayLen, ByteBuffer bb) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = decodeSignedLong(bb, UNIT_LENGTH_BYTES);
        }
        return longs;
    }

    private static Object[] decodeElements(ABIType<Object> elementType, int arrayLen, ByteBuffer bb) {
        final Object[] elements = ArrayType.createArray(elementType.clazz, arrayLen);
        byte[] unitBuffer = newUnitBuffer();
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elementType.decode(bb, unitBuffer);
        }
        return elements;
    }

    private static Object[] decodeObjectArray(ABIType<Object> elementType, int arrayLen, ByteBuffer bb) {
        final Object[] objects = ArrayType.createArray(elementType.clazz, arrayLen);
        for (int i = 0; i < objects.length; i++) {
            objects[i] = decode(elementType, bb, -1);
        }
        return objects;
    }

    private static long decodeLong(UnitType<? extends Number> type, ByteBuffer bb, int len) {
        final long val = decodeSignedLong(bb, len);
        if (type.unsigned) {
            return new Uint(type.bitLength).toUnsignedLong(val);
        }
        return val;
    }

    private static long decodeSignedLong(ByteBuffer bb, int len) {
        // new BigInteger(buffer, i, len); // Java 9+
        return getSignedBigInt(bb, len).longValueExact();
    }
}
