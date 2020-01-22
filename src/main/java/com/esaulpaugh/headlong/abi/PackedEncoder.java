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
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;

final class PackedEncoder {

    static void insertTuple(TupleType tupleType, Tuple tuple, ByteBuffer dest) {
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
            encode(tupleType.elementTypes[i], tuple.elements[i], dest);
        }
    }

    @SuppressWarnings("unchecked")
    private static void encode(ABIType<?> type, Object value, ByteBuffer dest) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBoolean((boolean) value, dest); return;
        case TYPE_CODE_BYTE:
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: insertInt(((Number) value).longValue(), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_INTEGER: insertInt(((BigInteger) value), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_DECIMAL: insertInt(((BigDecimal) value).unscaledValue(), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_ARRAY: encodeArray((ArrayType<ABIType<?>, ?>) type, value, dest); return;
        case TYPE_CODE_TUPLE: insertTuple((TupleType) type, (Tuple) value, dest); return;
        default: throw new Error();
        }
    }

    private static void encodeArray(ArrayType<ABIType<?>,?> arrayType, Object value, ByteBuffer dest) {
        final ABIType<?> elementType = arrayType.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: insertBooleans((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: insertBytes(!arrayType.isString ? (byte[]) value : Strings.decode((String) value, Strings.UTF_8), dest); return;
        case TYPE_CODE_INT: insertInts((int[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_LONG: insertLongs((long[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_INTEGER: insertBigIntegers((BigInteger[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_DECIMAL: insertBigDecimals((BigDecimal[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            for(Object e : (Object[]) value) {
                encode(elementType, e, dest);
            }
            return;
        default: throw new Error();
        }
    }
    // ------------------------
    private static void insertBooleans(boolean[] arr, ByteBuffer dest) {
        for (boolean bool : arr) {
            insertBoolean(bool, dest);
        }
    }

    private static void insertBytes(byte[] arr, ByteBuffer dest) {
        dest.put(arr);
    }

    private static void insertInts(int[] arr, int byteLen, ByteBuffer dest) {
        for (int e : arr) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertLongs(long[] arr, int byteLen, ByteBuffer dest) {
        for (long e : arr) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertBigIntegers(BigInteger[] arr, int byteLen, ByteBuffer dest) {
        for (BigInteger e : arr) {
            insertInt(e, byteLen, dest);
        }
    }

    private static void insertBigDecimals(BigDecimal[] arr, int byteLen, ByteBuffer dest) {
        for (BigDecimal e : arr) {
            insertInt(e.unscaledValue(), byteLen, dest);
        }
    }
    // ---------------------------
    private static void insertBoolean(boolean value, ByteBuffer dest) {
        dest.put(value ? Encoding.ONE_BYTE : Encoding.ZERO_BYTE);
    }

    private static void insertInt(long value, int byteLen, ByteBuffer dest) {
        if(value >= 0) {
            pad(dest, Encoding.ZERO_BYTE, byteLen - Integers.len(value));
            Integers.putLong(value, dest);
        } else {
            pad(dest, Encoding.NEGATIVE_ONE_BYTE, byteLen - BizarroIntegers.len(value));
            BizarroIntegers.putLong(value, dest);
        }
    }

    private static void insertInt(BigInteger bigGuy, int byteLen, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        pad(dest, bigGuy.signum() >= 0 ? Encoding.ZERO_BYTE : Encoding.NEGATIVE_ONE_BYTE, byteLen - arr.length);
        dest.put(arr);
    }

    private static void pad(ByteBuffer dest, byte val, int n) {
        for (int i = 0; i < n; i++) {
            dest.put(val);
        }
    }
}
