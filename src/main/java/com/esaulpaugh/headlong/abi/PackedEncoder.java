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

    static void encodeTuple(TupleType tupleType, Tuple tuple, ByteBuffer dest) {
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
            encode(tupleType.elementTypes[i], tuple.elements[i], dest);
        }
    }

    private static void encode(ABIType<?> type, Object value, ByteBuffer dest) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBoolean((boolean) value, dest); return;
        case TYPE_CODE_BYTE:
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: encodeInt(((Number) value).longValue(), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_INTEGER: encodeInt(((BigInteger) value), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_DECIMAL: encodeInt(((BigDecimal) value).unscaledValue(), type.byteLengthPacked(null), dest); return;
        case TYPE_CODE_ARRAY: encodeArray((ArrayType<?, ?>) type, value, dest); return;
        case TYPE_CODE_TUPLE: encodeTuple((TupleType) type, (Tuple) value, dest); return;
        default: throw new Error();
        }
    }

    private static void encodeArray(ArrayType<?, ?> arrayType, Object value, ByteBuffer dest) {
        final ABIType<?> elementType = arrayType.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: encodeBooleans((boolean[]) value, dest); return;
        case TYPE_CODE_BYTE: encodeBytes(!arrayType.isString ? (byte[]) value : Strings.decode((String) value, Strings.UTF_8), dest); return;
        case TYPE_CODE_INT: encodeInts((int[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_LONG: encodeLongs((long[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_INTEGER: encodeBigIntegers((BigInteger[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_BIG_DECIMAL: encodeBigDecimals((BigDecimal[]) value, elementType.byteLengthPacked(null), dest); return;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            for(Object e : (Object[]) value) {
                encode(elementType, e, dest);
            }
            return;
        default: throw new Error();
        }
    }

    private static void encodeBooleans(boolean[] arr, ByteBuffer dest) {
        for (boolean bool : arr) {
            encodeBoolean(bool, dest);
        }
    }

    private static void encodeBytes(byte[] arr, ByteBuffer dest) {
        dest.put(arr);
    }

    private static void encodeInts(int[] arr, int byteLen, ByteBuffer dest) {
        for (int e : arr) {
            encodeInt(e, byteLen, dest);
        }
    }

    private static void encodeLongs(long[] arr, int byteLen, ByteBuffer dest) {
        for (long e : arr) {
            encodeInt(e, byteLen, dest);
        }
    }

    private static void encodeBigIntegers(BigInteger[] arr, int byteLen, ByteBuffer dest) {
        for (BigInteger e : arr) {
            encodeInt(e, byteLen, dest);
        }
    }

    private static void encodeBigDecimals(BigDecimal[] arr, int byteLen, ByteBuffer dest) {
        for (BigDecimal e : arr) {
            encodeInt(e.unscaledValue(), byteLen, dest);
        }
    }
// ---------------------------------------------------------------------------------------------------------------------
    private static void encodeBoolean(boolean value, ByteBuffer dest) {
        dest.put(value ? Encoding.ONE_BYTE : Encoding.ZERO_BYTE);
    }

    private static void encodeInt(long value, int byteLen, ByteBuffer dest) {
        if(value >= 0) {
            Integers.putN(Encoding.ZERO_BYTE, byteLen - Integers.len(value), dest);
            Integers.putLong(value, dest);
        } else {
            Integers.putN(Encoding.NEGATIVE_ONE_BYTE, byteLen - BizarroIntegers.len(value), dest);
            BizarroIntegers.putLong(value, dest);
        }
    }

    private static void encodeInt(BigInteger signed, int byteLen, ByteBuffer dest) {
        byte[] arr = signed.toByteArray();
        Integers.putN(signed.signum() >= 0 ? Encoding.ZERO_BYTE : Encoding.NEGATIVE_ONE_BYTE, byteLen - arr.length, dest);
        dest.put(arr);
    }
}
