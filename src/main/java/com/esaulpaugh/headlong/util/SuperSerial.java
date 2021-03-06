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

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.BigDecimalType;
import com.esaulpaugh.headlong.abi.IntType;
import com.esaulpaugh.headlong.abi.LongType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.abi.UnitType;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
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
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;

/** Serializes and deserializes {@link Tuple}s through the use of RLP encoding. */
public final class SuperSerial {

    private SuperSerial() {}

    private static final byte[] TRUE = new byte[] { 0x1 };
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

    public static <T> T deserializeArray(ArrayType<? extends ABIType<?>, ?> arrayType, String str, boolean machine, Class<T> classOfT) {
        byte[] rlp = machine ? Strings.decode(str) : RLPEncoder.encodeSequentially(NotationParser.parse(str));
        Object array = deserializeArray(arrayType, RLP_STRICT.wrap(rlp));
        arrayType.validate(array);
        return classOfT.cast(array);
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
        if(sequenceIterator.hasNext()) {
            throw new IllegalArgumentException("trailing unconsumed items");
        }
        return new Tuple(elements);
    }

    private static Object serialize(ABIType<?> type, Object obj) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return serializeBoolean((boolean) obj);
        case TYPE_CODE_BYTE: return Integers.toBytes((byte) obj); // case currently goes unused
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: return serializeBigInteger((UnitType<?>) type, BigInteger.valueOf(((Number) obj).longValue()));
        case TYPE_CODE_BIG_INTEGER: return serializeBigInteger((UnitType<?>) type, (BigInteger) obj);
        case TYPE_CODE_BIG_DECIMAL: return serializeBigInteger((UnitType<?>) type, ((BigDecimal) obj).unscaledValue());
        case TYPE_CODE_ARRAY: return serializeArray((ArrayType<? extends ABIType<?>, ?>) type, obj);
        case TYPE_CODE_TUPLE: return serializeTuple((TupleType) type, obj);
        default: throw new Error();
        }
    }

    private static byte[] serializeBoolean(boolean val) {
        return val ? TRUE : FALSE;
    }

    private static Object deserialize(ABIType<?> type, RLPItem item) {
        if(type.typeCode() < TYPE_CODE_ARRAY && item.isList()) {
            throw new IllegalArgumentException("RLPList not allowed for this type: " + type);
        }
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return item.asBoolean();
        case TYPE_CODE_BYTE: return item.asByte(); // case currently goes unused
        case TYPE_CODE_INT: return deserializeBigInteger((UnitType<?>) type, item).intValueExact();
        case TYPE_CODE_LONG: return deserializeBigInteger((UnitType<?>) type, item).longValueExact();
        case TYPE_CODE_BIG_INTEGER: return deserializeBigInteger((UnitType<?>) type, item);
        case TYPE_CODE_BIG_DECIMAL:
            BigDecimalType bdt = (BigDecimalType) type;
            return new BigDecimal(deserializeBigInteger(bdt, item), bdt.getScale());
        case TYPE_CODE_ARRAY: return deserializeArray((ArrayType<? extends ABIType<?>, ?>) type, item);
        case TYPE_CODE_TUPLE: return deserializeTuple((TupleType) type, item.asBytes());
        default: throw new Error();
        }
    }

    private static byte[] serializeBigInteger(UnitType<?> ut, BigInteger val) {
        if(ut.isUnsigned()) {
            return Integers.toBytesUnsigned(val);
        }
        if(val.signum() != 0) {
            final byte[] bytes = val.toByteArray();
            return val.signum() < 0
                    ? signExtendNegative(bytes, ut.getBitLength() / Byte.SIZE)
                    : bytes[0] != 0
                        ? bytes
                        : Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Strings.EMPTY_BYTE_ARRAY;
    }

    private static byte[] signExtendNegative(final byte[] negative, final int newWidth) {
        final byte[] extended = new byte[newWidth];
        Arrays.fill(extended, (byte) 0xff);
        System.arraycopy(negative, 0, extended, newWidth - negative.length, negative.length);
        return extended;
    }

    private static BigInteger deserializeBigInteger(UnitType<?> ut, RLPItem item) {
        if(ut.isUnsigned()) {
            return item.asBigInt();
        }
        if(item.dataLength != 0) {
            if (item.dataLength * Byte.SIZE < ut.getBitLength()) {
                byte[] padded = new byte[item.dataLength + 1];
                item.exportData(padded, 1);
                return new BigInteger(padded);
            }
            if(item.dataLength > UNIT_LENGTH_BYTES) {
                throw new IllegalArgumentException("integer data cannot exceed " + UNIT_LENGTH_BYTES + " bytes");
            }
            return item.asBigIntSigned();
        }
        return BigInteger.ZERO;
    }

    private static Object serializeArray(ArrayType<? extends ABIType<?>, ?> type, Object arr) {
        switch (type.getElementType().typeCode()) {
        case TYPE_CODE_BOOLEAN: return serializeBooleanArray((boolean[]) arr);
        case TYPE_CODE_BYTE: return serializeByteArray(arr, type.isString());
        case TYPE_CODE_INT: return serializeIntArray((UnitType<?>) type.getElementType(), (int[]) arr);
        case TYPE_CODE_LONG: return serializeLongArray((UnitType<?>) type.getElementType(), (long[]) arr);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return serializeObjectArray((Object[]) arr, type.getElementType());
        default: throw new Error();
        }
    }

    private static Object deserializeArray(ArrayType<? extends ABIType<?>,?> type, RLPItem item) {
        switch (type.getElementType().typeCode()) {
        case TYPE_CODE_BOOLEAN: return deserializeBooleanArray((RLPList) item);
        case TYPE_CODE_BYTE: return deserializeByteArray(item, type.isString());
        case TYPE_CODE_INT: return deserializeIntArray((RLPList) item, (IntType) type.getElementType());
        case TYPE_CODE_LONG: return deserializeLongArray((RLPList) item, (LongType) type.getElementType());
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return deserializeObjectArray(type.getElementType(), (RLPList) item);
        default: throw new Error();
        }
    }

    private static byte[][] serializeBooleanArray(boolean[] booleans) {
        byte[][] out = new byte[booleans.length][];
        for (int i = 0; i < booleans.length; i++) {
            out[i] = serializeBoolean(booleans[i]);
        }
        return out;
    }

    private static boolean[] deserializeBooleanArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        boolean[] in = new boolean[len];
        for (int i = 0; i < len; i++) {
            in[i] = elements.get(i).asBoolean();
        }
        return in;
    }

    private static byte[] serializeByteArray(Object arr, boolean isString) {
        return isString ? Strings.decode((String) arr, Strings.UTF_8) : (byte[]) arr;
    }

    private static Object deserializeByteArray(RLPItem item, boolean isString) {
        return isString ? item.asString(Strings.UTF_8) : item.asBytes();
    }

    private static byte[][] serializeIntArray(UnitType<?> ut, int[] values) {
        byte[][] out = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            out[i] = serializeBigInteger(ut, BigInteger.valueOf(values[i]));
        }
        return out;
    }

    private static int[] deserializeIntArray(RLPList list, IntType type) {
        final List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        final int[] in = new int[len];
        for (int i = 0; i < len; i++) {
            in[i] = deserializeBigInteger(type, elements.get(i)).intValueExact();
        }
        return in;
    }

    private static byte[][] serializeLongArray(UnitType<?> ut, long[] values) {
        byte[][] out = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            out[i] = serializeBigInteger(ut, BigInteger.valueOf(values[i]));
        }
        return out;
    }

    private static long[] deserializeLongArray(RLPList list, LongType type) {
        final List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        final long[] in = new long[len];
        for (int i = 0; i < len; i++) {
            in[i] = deserializeBigInteger(type, elements.get(i)).longValueExact();
        }
        return in;
    }

    private static Object[] serializeObjectArray(Object[] objects, ABIType<?> elementType) {
        Object[] out = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            out[i] = serialize(elementType, objects[i]);
        }
        return out;
    }

    private static Object[] deserializeObjectArray(ABIType<?> elementType, RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        Object[] in = (Object[]) Array.newInstance(elementType.clazz(), len); // reflection ftw
        for (int i = 0; i < len; i++) {
            in[i] = deserialize(elementType, elements.get(i));
        }
        return in;
    }
}
