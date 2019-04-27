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

import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.ABIType.*;

final class PackedEncoder {

    static void insertTuple(TupleType tupleType, Tuple tuple, ByteBuffer dest) {

        final ABIType<?>[] types = tupleType.elementTypes;
        final Object[] values = tuple.elements;

        final int len = types.length;
        int i;
        for (i = 0; i < len; i++) {
            encode(types[i], values[i], dest);
        }
    }

    @SuppressWarnings("unchecked")
    private static void encode(ABIType<?> type, Object value, ByteBuffer dest) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBool((boolean) value, dest); break;
        case TYPE_CODE_BYTE:
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: insertInt(((Number) value).longValue(), type.byteLengthPacked(value), dest); break;
        case TYPE_CODE_BIG_INTEGER: insertInt(((BigInteger) value), type.byteLengthPacked(value), dest); break;
        case TYPE_CODE_BIG_DECIMAL: insertInt(((BigDecimal) value).unscaledValue(), type.byteLengthPacked(value), dest); break;
        case TYPE_CODE_ARRAY:
            encodeArray((ArrayType<ABIType<?>, ?>) type, value, dest); break;
        case TYPE_CODE_TUPLE:
            insertTuple((TupleType) type, (Tuple) value, dest); break;
        default:
            throw new IllegalArgumentException("unexpected array type: " + type.toString());
        }
    }

    private static void encodeArray(ArrayType<ABIType<?>,?> arrayType, Object value, ByteBuffer dest) {
        final ABIType<?> elementType = arrayType.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBooleans((boolean[]) value, dest); break;
        case TYPE_CODE_BYTE:
            byte[] arr = arrayType.isString ? ((String) value).getBytes(Strings.CHARSET_UTF_8) : (byte[]) value;
            insertBytes(arr, dest); break;
        case TYPE_CODE_INT: insertInts((int[]) value, elementType.byteLengthPacked(value), dest); break;
        case TYPE_CODE_LONG: insertLongs((long[]) value, elementType.byteLengthPacked(value), dest); break;
        case TYPE_CODE_BIG_INTEGER: insertBigIntegers((BigInteger[]) value, elementType.byteLengthPacked(value), dest); break;
        case TYPE_CODE_BIG_DECIMAL: insertBigDecimals((BigDecimal[]) value, elementType.byteLengthPacked(value), dest); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            for(Object e : (Object[]) value) {
                encode(elementType, e, dest);
            }
            break;
        default: throw new IllegalArgumentException("unexpected array type: " + arrayType.toString());
        }
    }

    // ------------------------

    private static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        final int idx = dest.position();
        final int len = bools.length;
        for (int i = idx; i < len; i++) {
            dest.put(bools[i] ? (byte) 1 : (byte) 0);
        }
    }

    private static void insertBytes(byte[] bytes, ByteBuffer dest) {
        dest.put(bytes);
    }

    private static void insertInts(int[] ints, int byteLen, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertLongs(long[] longs, int byteLen, ByteBuffer dest) {
        for (long e : longs) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertBigIntegers(BigInteger[] bigInts, int byteLen, ByteBuffer dest) {
        for (BigInteger e : bigInts) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertBigDecimals(BigDecimal[] bigDecs, int byteLen, ByteBuffer dest) {
        for (BigDecimal e : bigDecs) {
            insertInt(e.unscaledValue(), byteLen, dest);
        }
    }

    // ---------------------------

    private static void insertBool(boolean value, ByteBuffer dest) {
        dest.put(value ? (byte) 1 : (byte) 0);
    }

    private static void insertInt(long value, int byteLen, ByteBuffer dest) {
        if(value >= 0) {
            dest.position(dest.position() + (byteLen - Integers.len(value)));
            Integers.putLong(value, dest);
        } else {
            final int paddingBytes = byteLen - BizarroIntegers.len(value);
            for (int i = 0; i < paddingBytes; i++) {
                dest.put(CallEncoder.NEGATIVE_ONE_BYTE);
            }
            BizarroIntegers.putLong(value, dest);
        }
    }

    private static void insertInt(BigInteger bigGuy, int byteLen, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        final int paddingBytes = byteLen - arr.length;
        if(bigGuy.signum() == -1) {
            for (int i = 0; i < paddingBytes; i++) {
                dest.put(CallEncoder.NEGATIVE_ONE_BYTE);
            }
        } else {
            for (int i = 0; i < paddingBytes; i++) {
                dest.put((byte) 0);
            }
        }
        dest.put(arr);
    }
}
