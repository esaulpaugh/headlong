package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.abi.ABIException;
import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.BigDecimalType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;

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

    public static String serializeForMachine(TupleType tupleType, Tuple tuple) throws ABIException {
        tupleType.validate(tuple);
        List<Object> list = new ArrayList<>(tuple.size());
        final int len = tupleType.elements().length;
        for(int i = 0; i < len; i++) {
            list.add(serialize(tupleType.get(i), tuple.get(i)));
        }
        byte[] encoded = RLPEncoder.encodeSequentially(list);
        return Strings.encode(encoded, Strings.BASE_64_URL_SAFE);
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
        case TYPE_CODE_ARRAY: return null; // TODO
        case TYPE_CODE_TUPLE: return SuperSerial.serializeForMachine((TupleType) type, (Tuple) obj);
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
        case TYPE_CODE_ARRAY: return null; // TODO
        case TYPE_CODE_TUPLE: return SuperSerial.deserializeFromMachine((TupleType) type, item.asString(Strings.BASE_64_URL_SAFE));
        default: throw new Error();
        }
    }
}
