package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.abi.ABIException;
import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.BigDecimalType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.util.Strings;

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
            ABIType<?> type = tupleType.get(i);
            Object obj = tuple.get(i);
            Object temp;
            switch (type.typeCode()) {
            case TYPE_CODE_BOOLEAN:
                Boolean boo = (Boolean) obj;
                temp = boo ? TRUE : FALSE;
                break;
            case TYPE_CODE_BYTE:
                Byte b = (Byte) obj;
                temp = Integers.toBytes(b);
                break;
            case TYPE_CODE_INT:
                Integer integer = (Integer) obj;
                temp = Integers.toBytes(integer);
                break;
            case TYPE_CODE_LONG:
                Long looong = (Long) obj;
                temp = Integers.toBytes(looong);
                break;
            case TYPE_CODE_BIG_INTEGER:
                BigInteger bigInt = (BigInteger) obj;
                temp = bigInt.toByteArray();
                break;
            case TYPE_CODE_BIG_DECIMAL:
                BigDecimal bigDec = (BigDecimal) obj;
                temp = bigDec.unscaledValue().toByteArray();
                break;
            case TYPE_CODE_ARRAY:
                temp = null; // TODO
                break;
            case TYPE_CODE_TUPLE:
                Tuple t = (Tuple) obj;
                temp = SuperSerial.serializeForMachine((TupleType) type, t);
                break;
            default: throw new Error();
            }
            list.add(temp);
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
            ABIType<?> type = tupleType.get(i);
            RLPItem item = sequenceIterator.next();
            Object temp;
            switch (type.typeCode()) {
            case TYPE_CODE_BOOLEAN:
                temp = item.asBoolean();
                break;
            case TYPE_CODE_BYTE:
                temp = item.asByte();
                break;
            case TYPE_CODE_INT:
                temp = item.asInt();
                break;
            case TYPE_CODE_LONG:
                temp = item.asLong();
                break;
            case TYPE_CODE_BIG_INTEGER:
                temp = item.asBigInt();
                break;
            case TYPE_CODE_BIG_DECIMAL:
                temp = item.asBigDecimal(((BigDecimalType) type).getScale());
                break;
            case TYPE_CODE_ARRAY:
                temp = null; // TODO
                break;
            case TYPE_CODE_TUPLE:
                temp = SuperSerial.deserializeFromMachine((TupleType) type, item.asString(Strings.BASE_64_URL_SAFE));
                break;
            default:
                throw new Error();
            }
            elements.add(temp);
        }
        return new Tuple(elements.toArray());
    }
}
