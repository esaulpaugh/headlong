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
package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.abi.*;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

public class Deserializer {

    public static TupleType parseTupleType(String ttStr) {
        return parseTupleType(JsonUtils.parseArray(ttStr));
    }

    public static Tuple parseTupleValue(TupleType tupleType, String tupleStr) {
        return parseTupleValue(tupleType, JsonUtils.parseArray(tupleStr));
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
        ABIType<?>[] abiTypes = tupleType.elements();
        final int len = abiTypes.length;
        Object[] elements = new Object[len];
        int i = 0;
        for (Iterator<JsonElement> iter = valuesArray.iterator(); i < len; i++) {
            elements[i] = parseValue(abiTypes[i], iter.next());
        }
        return new Tuple(elements);
    }

    private static Object parseValue(ABIType<?> type, JsonElement value) {
        switch (type.typeCode()) {
            case ABIType.TYPE_CODE_BOOLEAN: return value.getAsJsonObject().get("value").getAsBoolean();
            case ABIType.TYPE_CODE_BYTE: return (byte) value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_INT: return value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_LONG: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueValue = valueObj.get("value").getAsString();
                return Long.parseLong(valueValue);
            }
            case ABIType.TYPE_CODE_BIG_INTEGER: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueType = valueObj.get("type").getAsString();
                String valueValue = valueObj.get("value").getAsString();
                if("string".equals(valueType)) {
                    BigInteger val = new BigInteger(FastHex.decode(valueValue, 2, valueValue.length() - 2));
                    BigIntegerType bigIntType = (BigIntegerType) type;
                    if(bigIntType.isUnsigned()) {
                        return new Uint(bigIntType.getBitLength()).toUnsigned(val);
                    }
                    return val;
                } else {
                    return new BigInteger(valueValue);
                }
            }
            case ABIType.TYPE_CODE_BIG_DECIMAL: {
                String valueValue = value.getAsJsonObject().get("value").getAsString();
                return new BigDecimal(new BigInteger(valueValue), ((BigDecimalType) type).getScale());
            }
            case ABIType.TYPE_CODE_ARRAY: return parseArrayValue((ArrayType<?, ?>) type, value);
            case ABIType.TYPE_CODE_TUPLE: return parseTupleValue((TupleType) type, value.getAsJsonObject().get("value").getAsJsonArray());
            default: throw new Error();
        }
    }

    private static Object parseArrayValue(ArrayType<?, ?> arrayType, JsonElement value) {
        if (arrayType.isString()) {
            return value.getAsJsonObject().get("value").getAsString();
        } else if (value.isJsonArray()) {
            JsonArray valueValue = value.getAsJsonArray();
            final int len = valueValue.size();
            final ABIType<?> elementType = arrayType.getElementType();
            int i = 0;
            if (Boolean.class == elementType.clazz()) {
                boolean[] array = new boolean[len];
                for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                    array[i] = (Boolean) parseValue(elementType, iter.next());
                }
                return array;
            } else if (Byte.class == elementType.clazz()) {
                byte[] array = new byte[len];
                for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                    array[i] = (Byte) parseValue(elementType, iter.next());
                }
                return array;
            }
            if (Integer.class == elementType.clazz()) {
                int[] array = new int[len];
                for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                    array[i] = (Integer) parseValue(elementType, iter.next());
                }
                return array;
            } else if (Long.class == elementType.clazz()) {
                long[] array = new long[len];
                for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                    array[i] = (Long) parseValue(elementType, iter.next());
                }
                return array;
            } else {
                Object[] array = (Object[]) Array.newInstance(elementType.clazz(), len);
                for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                    array[i] = parseValue(elementType, iter.next());
                }
                return array;
            }
        } else {
            JsonObject valueObj = value.getAsJsonObject();
            String valueType = valueObj.get("type").getAsString();
            String valueValue = valueObj.get("value").getAsString();
            if ("buffer".equals(valueType)) {
                return FastHex.decode(valueValue, 2, valueValue.length() - 2);
            } else {
                throw new RuntimeException("????");
            }
        }
    }
}
