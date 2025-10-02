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

import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Serializer {

    private Serializer() {}

    public static JsonPrimitive serializeTypes(TupleType<?> tupleType, Gson gson) {
        JsonArray typesArray = new JsonArray();
        for(ABIType<?> type : tupleType.elementTypes) {
            typesArray.add(new JsonPrimitive(type.canonicalType.replace("(", "tuple(")));
        }
        return new JsonPrimitive(gson.toJson(typesArray));
    }

    public static JsonPrimitive serializeValues(Tuple tuple, Gson gson) {
        JsonArray valuesArray = new JsonArray();
        for(Object val : tuple) {
            valuesArray.add(toJsonElement(val));
        }
        return new JsonPrimitive(gson.toJson(valuesArray));
    }

    private static JsonObject wrap(String type, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.addProperty("value", value);
        return obj;
    }

    private static JsonElement toJsonElement(Object val) {
        if (val instanceof Boolean) {
            return wrap("bool", val.toString());
        } else if (val instanceof Integer || val instanceof Long) {
            return wrap("number", val.toString());
        } else if (val instanceof BigInteger) {
            return wrap("string", "0x" + Strings.encode(((BigInteger) val).toByteArray()));
        } else if (val instanceof BigDecimal) {
            return wrap("number", ((BigDecimal) val).unscaledValue().toString());
        } else if (val instanceof byte[]) {
            return wrap("buffer", "0x" + Strings.encode((byte[]) val));
        } else if (val instanceof String) {
            return wrap("buffer", (String) val);
        } else if (val instanceof Address) {
            return wrap("string", val.toString());
        } else if (val.getClass().isArray()) { // boolean[], int[], long[], Object[]
            return toJsonArray(val);
        } else if(val instanceof Tuple) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", "tuple");
            jsonObject.add("value", toJsonArray(((Tuple) val).elements));
            return jsonObject;
        }
        throw new IllegalArgumentException();
    }

    private static JsonArray toJsonArray(Object array) {
        JsonArray jsonArray = new JsonArray();
        final int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            jsonArray.add(toJsonElement(Array.get(array, i)));
        }
        return jsonArray;
    }
}
