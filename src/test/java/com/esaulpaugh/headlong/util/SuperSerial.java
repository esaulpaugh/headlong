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

    public static byte[] serializeForMachine(TupleType tupleType, Tuple tuple) throws ABIException {
        tupleType.validate(tuple);
        List<Object> list = new ArrayList<>(tuple.size());
        final int len = tupleType.elements().length;
        for(int i = 0; i < len; i++) {
            list.add(serialize(tupleType.get(i), tuple.get(i)));
        }
        return RLPEncoder.encodeSequentially(list);
    }

    public static Tuple deserializeFromMachine(TupleType tupleType, String str) throws DecodeException {
        byte[] bytes = Strings.decode(str, Strings.BASE_64_URL_SAFE);
        Iterator<RLPItem> sequenceIterator = RLP_STRICT.sequenceIterator(bytes);
        List<Object> elements = new ArrayList<>();
        final int len = tupleType.elements().length;
        for(int i = 0; i < len; i++) {
            elements.add(deserialize(tupleType.get(i), sequenceIterator.next()));
        }
        return new Tuple(elements.toArray());
    }

    private static Object serialize(ABIType<?> type, Object obj) throws ABIException {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (Boolean) obj ? TRUE : FALSE;
        case TYPE_CODE_BYTE: return Integers.toBytes((byte) obj);
        case TYPE_CODE_INT: return Integers.toBytes((int) obj);
        case TYPE_CODE_LONG: return Integers.toBytes((long) obj);
        case TYPE_CODE_BIG_INTEGER: return ((BigInteger) obj).toByteArray();
        case TYPE_CODE_BIG_DECIMAL: return ((BigDecimal) obj).unscaledValue().toByteArray();
        case TYPE_CODE_ARRAY: return serializeArray((ArrayType<? extends ABIType<?>, ?>) type, obj);
        case TYPE_CODE_TUPLE: return serializeForMachine((TupleType) type, (Tuple) obj);
        default: throw new Error();
        }
    }

    private static Object serializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, Object obj) {
        return arrayType.isString() ? Strings.decode((String) obj, Strings.UTF_8) : obj;
    }

    private static Object serializeBooleanArray(Object obj) {
        boolean[] booleans = (boolean[]) obj;
        List<byte[]> list = new ArrayList<>(booleans.length);
        for (boolean aBoolean : booleans) {
            list.add(Integers.toBytes(aBoolean ? 1 : 0));
        }
        return list;
    }

    private static Object serializeIntArray(Object obj) {
        int[] ints = (int[]) obj;
        List<byte[]> list = new ArrayList<>(ints.length);
        for (int anInt : ints) {
            list.add(Integers.toBytes(anInt));
        }
        return list;
    }

    private static Object serializeLongArray(Object obj) {
        long[] longs = (long[]) obj;
        List<byte[]> list = new ArrayList<>(longs.length);
        for (long aLong : longs) {
            list.add(Integers.toBytes(aLong));
        }
        return list;
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
        case TYPE_CODE_TUPLE: return deserializeFromMachine((TupleType) type, item.asString(Strings.BASE_64_URL_SAFE));
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
        case TYPE_CODE_TUPLE:
            final Object[] array = (Object[]) obj;
            final int len = array.length;
            List<Object> objects = new ArrayList<>(len);
            for (Object e : array) {
                objects.add(serialize(elementType, e));
            }
            return objects;
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

    private static Object deserializeByteArray(ArrayType<? extends ABIType<?>,?> arrayType, RLPString string) {
        return arrayType.isString() ? string.asString(Strings.UTF_8) : string.asBytes();
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

    private static int[] deserializeIntArray(RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        int[] ints = new int[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            ints[i++] = e.asInt();
        }
        return ints;
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

    private static Object[] deserializeObjectArray(ABIType<?> elementType, RLPList list) throws DecodeException {
        List<RLPItem> elements = list.elements(RLP_STRICT);
        Object[] objects = new Object[elements.size()];
        int i = 0;
        for (RLPItem e : elements) {
            objects[i++] = deserialize(elementType, e);
        }
        return objects;
    }
}
