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
import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
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

    /**
     * Bit mask to select the sign bit of a {@code byte}.
     */
    private static final int SIGN_BIT_MASK = 0x80;

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
        final int typeCode = type.typeCode();
        switch (typeCode) {
        case TYPE_CODE_BOOLEAN: return serializeBoolean((boolean) obj);
        case TYPE_CODE_BYTE: return Integers.toBytes((byte) obj); // case currently goes unused
        case TYPE_CODE_INT: return toSigned(((IntType) type).getBitLength(), BigInteger.valueOf((int) obj));
        case TYPE_CODE_LONG: return toSigned(((LongType) type).getBitLength(), BigInteger.valueOf((long) obj));
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
            return serializeBigInteger(
                    (UnitType<?>) type,
                    typeCode == TYPE_CODE_BIG_INTEGER
                            ? (BigInteger) obj
                            : ((BigDecimal) obj).unscaledValue()
            );
        case TYPE_CODE_ARRAY: return serializeArray((ArrayType<? extends ABIType<?>, ?>) type, obj);
        case TYPE_CODE_TUPLE: return serializeTuple((TupleType) type, obj);
        default: throw new Error();
        }
    }

    private static byte[] serializeBoolean(boolean val) {
        return val ? TRUE : FALSE;
    }

    private static Object serializeBigInteger(UnitType<?> ut, BigInteger bigInt) {
        return ut.isUnsigned() ? Integers.toBytesUnsigned(bigInt) : toSigned(ut.getBitLength(), bigInt);
    }

    private static Object deserialize(ABIType<?> type, RLPItem item) {
        final int typeCode = type.typeCode();
        if(typeCode < TYPE_CODE_ARRAY && item.isList()) {
            throw new IllegalArgumentException("RLPList not allowed for this type: " + type + "\n" + item);
        }
        switch (typeCode) {
        case TYPE_CODE_BOOLEAN: return item.asBoolean();
        case TYPE_CODE_BYTE: return item.asByte(false); // case currently goes unused
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: return deserializePrimitive((UnitType<?>) type, item, typeCode == TYPE_CODE_INT);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
            final UnitType<?> ut = (UnitType<?>) type;
            final BigInteger bigInt = deserializeBigInteger(ut, item);
            return typeCode == TYPE_CODE_BIG_INTEGER ? bigInt : new BigDecimal(bigInt, ((BigDecimalType) ut).getScale());
        case TYPE_CODE_ARRAY: return deserializeArray((ArrayType<? extends ABIType<?>, ?>) type, item);
        case TYPE_CODE_TUPLE: return deserializeTuple((TupleType) type, item.asBytes());
        default: throw new Error();
        }
    }

    private static Number deserializePrimitive(UnitType<?> ut, RLPItem item, boolean isInt) {
        if(ut.isUnsigned() || (item.dataLength * Byte.SIZE) < ut.getBitLength()) {
            return isInt
                    ? item.asInt(false)
                    : (Number) item.asLong(false);
        }
        final byte[] data = item.data();
        final int len = data.length;
        if(len > 0 && (data[0] & SIGN_BIT_MASK) != 0) {
            return isInt
                    ? BizarroIntegers.getInt(data, 0, len)
                    : (Number) BizarroIntegers.getLong(data, 0, len);
        }
        return isInt
                ? Integers.getInt(data, 0, len, false)
                : (Number) Integers.getLong(data, 0, len, false);
    }

    private static BigInteger deserializeBigInteger(UnitType<?> ut, RLPItem item) {
        return ut.isUnsigned()
                ? item.asBigInt(false)
                : asSigned(ut.getBitLength(), item);
    }

    private static byte[] toSigned(int typeBits, BigInteger val) {
        final int signum = val.signum();
        if(signum != 0) {
            final byte[] bytes = val.toByteArray();
            return signum < 0
                    ? signExtendNegative(bytes, typeBits / Byte.SIZE)
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

    private static BigInteger asSigned(int typeBits, RLPItem item) {
        final int dataLen = item.dataLength;
        if(dataLen != 0) {
            if (dataLen * Byte.SIZE < typeBits) {
                byte[] padded = new byte[dataLen + 1];
                item.exportData(padded, 1);
                return new BigInteger(padded);
            }
            if(dataLen > UNIT_LENGTH_BYTES) {
                throw new IllegalArgumentException("integer data cannot exceed " + UNIT_LENGTH_BYTES + " bytes");
            }
            return item.asBigIntSigned();
        }
        return BigInteger.ZERO;
    }

    private static Object serializeArray(ArrayType<? extends ABIType<?>, ?> arrayType, Object arr) {
        final ABIType<?> elementType = arrayType.getElementType();
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return serializeBooleanArray((boolean[]) arr);
        case TYPE_CODE_BYTE: return serializeByteArray(arr, arrayType.isString());
        case TYPE_CODE_INT: return serializeIntArray((int[]) arr);
        case TYPE_CODE_LONG: return serializeLongArray((long[]) arr);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return serializeObjectArray(arr, elementType);
        default: throw new Error();
        }
    }

    private static Object deserializeArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPItem item) {
        final ABIType<?> elementType = arrayType.getElementType();
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return deserializeBooleanArray((RLPList) item);
        case TYPE_CODE_BYTE: return deserializeByteArray(item, arrayType.isString());
        case TYPE_CODE_INT: return deserializeIntArray((RLPList) item);
        case TYPE_CODE_LONG: return deserializeLongArray((RLPList) item);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return deserializeObjectArray(elementType, (RLPList) item);
        default: throw new Error();
        }
    }

    private static byte[][] serializeBooleanArray(boolean[] booleans) {
        final int len = booleans.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
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

    private static byte[][] serializeIntArray(int[] ints) {
        final int len = ints.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
            out[i] = Integers.toBytes(ints[i]);
        }
        return out;
    }

    private static int[] deserializeIntArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        int[] in = new int[len];
        for (int i = 0; i < len; i++) {
            in[i] = elements.get(i).asInt(false);
        }
        return in;
    }

    private static byte[][] serializeLongArray(long[] longs) {
        final int len = longs.length;
        byte[][] out = new byte[len][];
        for (int i = 0; i < len; i++) {
            out[i] = Integers.toBytes(longs[i]);
        }
        return out;
    }

    private static long[] deserializeLongArray(RLPList list) {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        final int len = elements.size();
        long[] in = new long[len];
        for (int i = 0; i < len; i++) {
            in[i] = elements.get(i).asLong();
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
        final int len = elements.size();
        Object[] in = (Object[]) Array.newInstance(elementType.clazz(), len); // reflection ftw
        for (int i = 0; i < len; i++) {
            in[i] = deserialize(elementType, elements.get(i));
        }
        return in;
    }
}
