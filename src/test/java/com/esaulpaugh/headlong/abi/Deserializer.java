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

import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Uint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

public class Deserializer {

    private Deserializer() {}

    public static TupleType parseTupleType(String ttStr) {
        return parseTupleType(Streams.parse(new JsonReader(new StringReader(ttStr))).getAsJsonArray());
    }

    public static Tuple parseTupleValue(TupleType tupleType, String tupleStr) {
        return parseTupleValue(tupleType, Streams.parse(new JsonReader(new StringReader(tupleStr))).getAsJsonArray());
    }

    public static TupleType parseTupleType(JsonArray typesArray) {
        final int len = typesArray.size();
        String[] typeStrings = new String[len];
        for (int i = 0; i < len; i++) {
            typeStrings[i] = typesArray.get(i).getAsString().replace("tuple", "");
        }
        return TupleType.of(typeStrings);
    }

    public static Tuple parseTupleValue(TupleType tupleType, JsonArray valuesArray) {
        final int len = tupleType.size();
        Object[] elements = new Object[len];
        int i = 0;
        for (Iterator<JsonElement> iter = valuesArray.iterator(); i < len; i++) {
            elements[i] = parseValue(tupleType.get(i), iter.next());
        }
        return Tuple.ofAll(elements);
    }

    private static Object parseValue(final ABIType<?> type, final JsonElement value) {
        final int typeCode = type.typeCode();
        if(typeCode == ABIType.TYPE_CODE_ARRAY) {
            return parseArrayValue((ArrayType<?, ?, ?>) type, value);
        }
        final JsonObject valueObj = value.getAsJsonObject();
        final JsonElement valVal = valueObj.get("value");
        switch (typeCode) {
        case ABIType.TYPE_CODE_BOOLEAN: return valVal.getAsBoolean();
        case ABIType.TYPE_CODE_BYTE: return (byte) valVal.getAsInt();
        case ABIType.TYPE_CODE_INT: return valVal.getAsInt();
        case ABIType.TYPE_CODE_LONG: return Long.parseLong(valVal.getAsString());
        case ABIType.TYPE_CODE_BIG_INTEGER: {
            String valueType = valueObj.get("type").getAsString();
            String valStr = valVal.getAsString();
            if ("string".equals(valueType)) {
                BigIntegerType bigIntType = (BigIntegerType) type;
                BigInteger val = new BigInteger(FastHex.decode(valStr, 2, valStr.length() - 2));
                if (bigIntType.isUnsigned()) {
                    return new Uint(bigIntType.getBitLength()).toUnsigned(val);
                }
                return val;
            } else {
                return new BigInteger(valStr);
            }
        }
        case ABIType.TYPE_CODE_BIG_DECIMAL: return new BigDecimal(
                new BigInteger(valVal.getAsString()), ((BigDecimalType) type).getScale()
        );
        case ABIType.TYPE_CODE_TUPLE: return parseTupleValue((TupleType) type, valVal.getAsJsonArray());
        case ABIType.TYPE_CODE_ADDRESS: return Address.wrap(valVal.getAsString());
        default: throw new Error();
        }
    }

    private static Object parseArrayValue(final ArrayType<?, ?, ?> arrayType, final JsonElement value) {
        if (value.isJsonArray()) {
            JsonArray valArr = value.getAsJsonArray();
            final int len = valArr.size();
            final ABIType<?> elementType = arrayType.getElementType();
            int i = 0;
            final Object arrayObj;
            final Class<?> clazz = elementType.clazz();
            final Iterator<JsonElement> iter = valArr.iterator();
            if (Boolean.class == clazz) {
                boolean[] array = (boolean[]) (arrayObj = new boolean[len]);
                for (; i < len; i++) {
                    array[i] = (boolean) parseValue(elementType, iter.next());
                }
            } else if (Byte.class == clazz) {
                byte[] array = (byte[]) (arrayObj = new byte[len]);
                for (; i < len; i++) {
                    array[i] = (byte) parseValue(elementType, iter.next());
                }
            } else if (Integer.class == clazz) {
                int[] array = (int[]) (arrayObj = new int[len]);
                for (; i < len; i++) {
                    array[i] = (int) parseValue(elementType, iter.next());
                }
            } else if (Long.class == clazz) {
                long[] array = (long[]) (arrayObj = new long[len]);
                for (; i < len; i++) {
                    array[i] = (long) parseValue(elementType, iter.next());
                }
            } else {
                Object[] array = (Object[]) (arrayObj = ArrayType.createArray(clazz, len));
                for (; i < len; i++) {
                    array[i] = parseValue(elementType, iter.next());
                }
            }
            return arrayObj;
        }
        final JsonObject valueObj = value.getAsJsonObject();
        if (arrayType.isString()) {
            return valueObj.get("value").getAsString();
        }
        String valueType = valueObj.get("type").getAsString();
        if ("buffer".equals(valueType)) {
            String valStr = valueObj.get("value").getAsString();
            return FastHex.decode(valStr, 2, valStr.length() - 2);
        } else {
            throw new RuntimeException("????");
        }
    }
}
