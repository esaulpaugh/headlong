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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class JsonUtils {

    private JsonUtils() {}

    @SuppressWarnings("deprecation")
    public static JsonElement parseElement(String json) {
        return new JsonParser().parse(json); // JsonParser.parseString(json);
    }

    public static JsonObject parseObject(String json) {
        return parseElement(json).getAsJsonObject();
    }

    public static JsonArray parseArray(String json) {
        return parseElement(json).getAsJsonArray();
    }

    public static String getString(JsonObject object, String key) {
        return getString(object, key, null);
    }

    public static Boolean getBoolean(JsonObject object, String key) {
        return getBoolean(object, key, null);
    }

    public static JsonArray getArray(JsonObject object, String key) {
        final JsonElement element = object.get(key);
        if(isNull(element)) {
            return null;
        }
        return element.getAsJsonArray();
    }

    public static String getString(JsonObject object, String key, String defaultVal) {
        final JsonElement element = object.get(key);
        if(isNull(element)) {
            return defaultVal;
        }
        if(element.isJsonPrimitive() && ((JsonPrimitive) element).isString()) {
            return element.getAsString();
        }
        throw new IllegalArgumentException(key + " is not a string");
    }

    public static Boolean getBoolean(JsonObject object, String key, Boolean defaultVal) {
        final JsonElement element = object.get(key);
        if(isNull(element)) {
            return defaultVal;
        }
        if(element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean()) {
            return element.getAsBoolean();
        }
        throw new IllegalArgumentException(key + " is not a boolean");
    }

    private static boolean isNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }
}
