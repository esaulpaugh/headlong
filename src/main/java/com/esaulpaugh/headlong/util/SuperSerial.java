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

import com.esaulpaugh.headlong.abi.ABIException;
import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.BigDecimalType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.RLPString;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.rlp.util.NotationParser;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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

public class SuperSerial {

    private static final byte[] TRUE = new byte[] { 0x01 };
    private static final byte[] FALSE = new byte[] { 0x00 };

    public static String serialize(TupleType tupleType, Tuple tuple, boolean machine) throws ABIException, DecodeException {
        tupleType.validate(tuple);
        List<Object> list = serializeTuple(tupleType, tuple);
        return machine
                ? Strings.encode(RLPEncoder.encodeSequentially(list))
                : Notation.forObjects(list).toString();
    }

    public static Tuple deserialize(TupleType tupleType, String str, boolean machine) throws DecodeException, ABIException {
        Tuple tuple = deserializeTuple(
                tupleType,
                machine ? Strings.decode(str) : RLPEncoder.encodeSequentially(NotationParser.parse(str))
        );
        tupleType.validate(tuple);
        return tuple;
    }

    private static List<Object> serializeTuple(TupleType tupleType, Tuple tuple) throws ABIException {
        List<Object> list = new ArrayList<>(tuple.size());
        final int len = tupleType.elements().length;
        for(int i = 0; i < len; i++) {
            list.add(serialize(tupleType.get(i), tuple.get(i)));
        }
        return list;
    }

    private static Tuple deserializeTuple(TupleType tupleType, byte[] sequence) throws DecodeException {
        Iterator<RLPItem> sequenceIterator = RLP_STRICT.sequenceIterator(sequence);
        List<Object> elements = new ArrayList<>();
        final int len = tupleType.elements().length;
        for(int i = 0; i < len; i++) {
            elements.add(deserialize(tupleType.get(i), sequenceIterator.next()));
        }
        return new Tuple(elements.toArray());
    }

    private static Object serialize(ABIType<?> type, Object obj) throws ABIException {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (boolean) obj ? TRUE : FALSE;
        case TYPE_CODE_BYTE: return Integers.toBytes((byte) obj);
        case TYPE_CODE_INT: return Integers.toBytes((int) obj);
        case TYPE_CODE_LONG: return Integers.toBytes((long) obj);
        case TYPE_CODE_BIG_INTEGER: return ((BigInteger) obj).toByteArray();
        case TYPE_CODE_BIG_DECIMAL: return ((BigDecimal) obj).unscaledValue().toByteArray();
        case TYPE_CODE_ARRAY: return serializeArray((ArrayType<? extends ABIType<?>, ?>) type, obj);
        case TYPE_CODE_TUPLE: return serializeTuple((TupleType) type, (Tuple) obj);
        default: throw new Error();
        }
    }

    private static Object deserialize(ABIType<?> type, RLPItem item) throws DecodeException {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return item.asBoolean();
        case TYPE_CODE_BYTE: return item.asByte();
        case TYPE_CODE_INT: return item.asInt();
        case TYPE_CODE_LONG: return item.asLong();
        case TYPE_CODE_BIG_INTEGER: return item.asBigInt();
        case TYPE_CODE_BIG_DECIMAL: return item.asBigDecimal(((BigDecimalType) type).getScale());
        case TYPE_CODE_ARRAY: return deserializeArray((ArrayType<? extends ABIType<?>, ?>) type, item);
        case TYPE_CODE_TUPLE: return deserializeTuple((TupleType) type, item.asBytes());
        default: throw new Error();
        }
    }

    private static Object serializeArray(ArrayType<? extends ABIType<?>, ?> arrayType, Object obj) throws ABIException {
        ABIType<?> elementType = arrayType.getElementType();
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return serializeBooleanArray(obj);
        case TYPE_CODE_BYTE: return serializeByteArray(arrayType, obj);
        case TYPE_CODE_INT: return serializeIntArray(obj);
        case TYPE_CODE_LONG: return serializeLongArray(obj);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: return serializeObjectArray((Object[]) obj, elementType);
        default: throw new Error();
        }
    }

    private static Object deserializeArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPItem item) throws DecodeException {
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

    private static Object serializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, Object obj) {
        return arrayType.isString() ? Strings.decode((String) obj, Strings.UTF_8) : obj;
    }

    private static Object deserializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPString string) {
        return arrayType.isString() ? string.asString(Strings.UTF_8) : string.asBytes();
    }

    private static List<byte[]> serializeBooleanArray(Object obj) {
        boolean[] booleans = (boolean[]) obj;
        List<byte[]> list = new ArrayList<>(booleans.length);
        for (boolean e : booleans) {
            list.add(Integers.toBytes(e ? 1 : 0));
        }
        return list;
    }

    private static boolean[] deserializeBooleanArray(RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        boolean[] booleans = new boolean[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            booleans[i++] = e.asBoolean();
        }
        return booleans;
    }

    private static List<byte[]> serializeIntArray(Object obj) {
        int[] ints = (int[]) obj;
        List<byte[]> list = new ArrayList<>(ints.length);
        for (int e : ints) {
            list.add(Integers.toBytes(e));
        }
        return list;
    }

    private static int[] deserializeIntArray(RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        int[] ints = new int[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            ints[i++] = e.asInt();
        }
        return ints;
    }

    private static List<byte[]> serializeLongArray(Object obj) {
        long[] longs = (long[]) obj;
        List<byte[]> list = new ArrayList<>(longs.length);
        for (long e : longs) {
            list.add(Integers.toBytes(e));
        }
        return list;
    }

    private static long[] deserializeLongArray(RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        long[] longs = new long[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            longs[i++] = e.asLong();
        }
        return longs;
    }

    private static List<Object> serializeObjectArray(Object[] obj, ABIType<?> elementType) throws ABIException {
        final int len = obj.length;
        List<Object> objects = new ArrayList<>(len);
        for (Object e : obj) {
            objects.add(serialize(elementType, e));
        }
        return objects;
    }

    private static Object[] deserializeObjectArray(ABIType<?> elementType, RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        Object[] objects = (Object[]) Array.newInstance(elementType.clazz(), elements.size()); // reflection ftw
        int i = 0;
        for (RLPItem e : elements) {
            objects[i++] = deserialize(elementType, e);
        }
        return objects;
    }
}
