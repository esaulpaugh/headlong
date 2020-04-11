/*
   Copyright 2020 Evan Saulpaugh

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
package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.abi.*;
import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.RLPString;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.rlp.util.NotationParser;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;

/** Serializes and deserializes {@link Tuple}s through the use of RLP encoding. */
public final class SuperSerial {

    private static final byte[] TRUE = new byte[] { 0x01 };
    private static final byte[] FALSE = new byte[0];

    public static String serialize(TupleType tupleType, Tuple tuple, boolean machine) {
        tupleType.validate(tuple);
        Object[] objects = serializeTuple(tupleType, tuple);
        return machine ? Strings.encode(RLPEncoder.encodeSequentially(objects))
                       : Notation.forObjects(objects).toString();
    }

    public static Tuple deserialize(TupleType tupleType, String str, boolean machine) {
        Tuple in = deserializeTuple(
                tupleType,
                machine ? Strings.decode(str)
                        : RLPEncoder.encodeSequentially(NotationParser.parse(str)));
        tupleType.validate(in);
        return in;
    }

    private static Object[] serializeTuple(TupleType tupleType, Object obj) {
        Tuple tuple = (Tuple) obj;
        final int len = tupleType.size();
        Object[] out = new Object[len];
        for(int i = 0; i < len; i++) {
            out[i] = serialize(tupleType.get(i), tuple.get(i));
        }
        return out;
    }

    private static Tuple deserializeTuple(TupleType tupleType, byte[] sequence) {
        Iterator<RLPItem> sequenceIterator = RLP_STRICT.sequenceIterator(sequence);
        final int len = tupleType.size();
        Object[] elements = new Object[len];
        for(int i = 0; i < len; i++) {
            elements[i] = deserialize(tupleType.get(i), sequenceIterator.next());
        }
        return new Tuple(elements);
    }

    private static Object serialize(ABIType<?> type, Object obj) { // TODO match deserializer
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (boolean) obj ? TRUE : FALSE;
        case TYPE_CODE_BYTE: return Integers.toBytes((byte) obj); // case currently goes unused
        case TYPE_CODE_INT: return toSigned(((IntType) type).getBitLength(), BigInteger.valueOf((int) obj));
        case TYPE_CODE_LONG: return toSigned(((LongType) type).getBitLength(), BigInteger.valueOf((long) obj));
        case TYPE_CODE_BIG_INTEGER:
            final BigIntegerType bigIntegerType = (BigIntegerType) type;
            final BigInteger bigInteger = (BigInteger) obj;
            return bigIntegerType.isUnsigned() ? Integers.toBytesUnsigned(bigInteger) : toSigned(bigIntegerType.getBitLength(), bigInteger);
        case TYPE_CODE_BIG_DECIMAL:
            final BigDecimalType bigDecimalType = (BigDecimalType) type;
            final BigInteger unscaled = ((BigDecimal) obj).unscaledValue();
            return bigDecimalType.isUnsigned() ? Integers.toBytesUnsigned(unscaled) : toSigned(bigDecimalType.getBitLength(), unscaled);
        case TYPE_CODE_ARRAY: return serializeArray((ArrayType<? extends ABIType<?>, ?>) type, obj);
        case TYPE_CODE_TUPLE: return serializeTuple((TupleType) type, obj);
        default: throw new Error();
        }
    }

    private static Object deserialize(ABIType<?> type, RLPItem item) {
        final int typeCode = type.typeCode();
        if(typeCode < TYPE_CODE_ARRAY && item.isList()) {
            throw new IllegalArgumentException("RLP list items not allowed for this type: " + type + "\n" + item);
        }
        switch (typeCode) {
        case TYPE_CODE_BOOLEAN: return item.asBoolean();
        case TYPE_CODE_BYTE: return item.asByte(false); // case currently goes unused
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG:
            final UnitType<?> ut = (UnitType<?>) type;
            final boolean isInt = typeCode != TYPE_CODE_LONG;
            if(ut.isUnsigned() || (item.dataLength * Byte.SIZE) < ut.getBitLength()) {
                return isInt
                        ? (Object) item.asInt(false)
                        : (Object) item.asLong(false);
            }
            byte[] data = item.data();
            final int len = data.length;
            if(len > 0 && (data[0] & 0x80) > 0) {
                return isInt
                        ? (Object) BizarroIntegers.getInt(data, 0, len)
                        : (Object) BizarroIntegers.getLong(data, 0, len);
            }
            return isInt
                    ? (Object) Integers.getInt(data, 0, len, false)
                    : (Object) Integers.getLong(data, 0, len, false);
        case TYPE_CODE_BIG_INTEGER:
            BigIntegerType bi = (BigIntegerType) type;
            return bi.isUnsigned()
                    ? item.asBigInt(false)
                    : asSigned(bi.getBitLength(), item);
        case TYPE_CODE_BIG_DECIMAL:
            BigDecimalType t = (BigDecimalType) type;
            return t.isUnsigned()
                    ? new BigDecimal(item.asBigInt(false), t.getScale())
                    : new BigDecimal(asSigned(t.getBitLength(), item), t.getScale());
        case TYPE_CODE_ARRAY: return deserializeArray((ArrayType<? extends ABIType<?>, ?>) type, item);
        case TYPE_CODE_TUPLE: return deserializeTuple((TupleType) type, item.asBytes());
        default: throw new Error();
        }
    }

    private static byte[] toSigned(int typeBits, BigInteger val) {
        final int signum = val.signum();
        if(signum == 0) {
            return Strings.EMPTY_BYTE_ARRAY;
        }
        final byte[] bytes = val.toByteArray();
        return signum < 0
                ? signExtendNegative(bytes, typeBits / Byte.SIZE)
                : bytes[0] != 0
                    ? bytes
                    : Arrays.copyOfRange(bytes, 1, bytes.length);
    }

    private static byte[] signExtendNegative(byte[] bytes, int width) {
        byte[] full = new byte[width];
        Arrays.fill(full, (byte) 0xff);
        System.arraycopy(bytes, 0, full, full.length - bytes.length, bytes.length);
        return full;
    }

    private static BigInteger asSigned(int typeBits, RLPItem item) {
        final int dataLen = item.dataLength;
        if(dataLen != 0) {
            final int maxBytes = 32;
            if(dataLen > maxBytes) {
                throw new IllegalArgumentException("integer data cannot exceed " + maxBytes + " bytes");
            }
            return (dataLen * Byte.SIZE) < typeBits
                    ? new BigInteger(item.asString(Strings.HEX), 16)
                    : new BigInteger(item.data());
        }
        return BigInteger.ZERO;
    }

    private static Object serializeArray(ArrayType<? extends ABIType<?>, ?> arrayType, Object arr) {
        ABIType<?> elementType = arrayType.getElementType();
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return serializeBooleanArray(arr);
        case TYPE_CODE_BYTE: return serializeByteArray(arrayType, arr);
        case TYPE_CODE_INT: return serializeIntArray(arr);
        case TYPE_CODE_LONG: return serializeLongArray(arr);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return serializeObjectArray(arr, elementType);
        default: throw new Error();
        }
    }

    private static Object deserializeArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPItem item) {
        ABIType<?> elementType = arrayType.getElementType();
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return deserializeBooleanArray((RLPList) item);
        case TYPE_CODE_BYTE: return deserializeByteArray(arrayType, (RLPString) item);
        case TYPE_CODE_INT: return deserializeIntArray((RLPList) item);
        case TYPE_CODE_LONG: return deserializeLongArray((RLPList) item);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return deserializeObjectArray(elementType, (RLPList) item);
        default: throw new Error();
        }
    }

    private static byte[][] serializeBooleanArray(Object arr) {
        boolean[] booleans = (boolean[]) arr;
        final int len = booleans.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
            out[i] = booleans[i] ? TRUE : FALSE;
        }
        return out;
    }

    private static boolean[] deserializeBooleanArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        boolean[] in = new boolean[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            in[i++] = e.asBoolean();
        }
        return in;
    }

    private static byte[] serializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, Object arr) {
        return arrayType.isString() ? Strings.decode((String) arr, Strings.UTF_8) : (byte[]) arr;
    }

    private static Object deserializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPString string) {
        return arrayType.isString() ? string.asString(Strings.UTF_8) : string.asBytes();
    }

    private static byte[][] serializeIntArray(Object arr) {
        int[] ints = (int[]) arr;
        final int len = ints.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
            out[i] = Integers.toBytes(ints[i]);
        }
        return out;
    }

    private static int[] deserializeIntArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        int[] in = new int[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            in[i++] = e.asInt(false);
        }
        return in;
    }

    private static byte[][] serializeLongArray(Object arr) {
        long[] longs = (long[]) arr;
        final int len = longs.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
            out[i] = Integers.toBytes(longs[i]);
        }
        return out;
    }

    private static long[] deserializeLongArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        long[] in = new long[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            in[i++] = e.asLong();
        }
        return in;
    }

    private static Object[] serializeObjectArray(Object arr, ABIType<?> elementType) {
        Object[] objects = (Object[]) arr;
        final int len = objects.length;
        Object[] out = new Object[len];
        for (int i = 0; i < len; i++) {
            out[i] = serialize(elementType, objects[i]);
        }
        return out;
    }

    private static Object[] deserializeObjectArray(ABIType<?> elementType, RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        Object[] in = (Object[]) Array.newInstance(elementType.clazz(), elements.size()); // reflection ftw
        int i = 0;
        for (RLPItem e : elements) {
            in[i++] = deserialize(elementType, e);
        }
        return in;
    }
}
