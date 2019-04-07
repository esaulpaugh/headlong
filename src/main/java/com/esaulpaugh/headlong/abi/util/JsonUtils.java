package com.esaulpaugh.headlong.abi.util;

import com.google.gson.*;

public class JsonUtils {

    public static JsonElement parse(String json) {
        return new JsonParser().parse(json);
    }

    public static JsonObject parseObject(String json) {
        return parse(json).getAsJsonObject();
    }

    public static JsonArray parseArray(String json) {
        return parse(json).getAsJsonArray();
    }

    public static String getString(JsonObject object, String key) {
        return getString(object, key, null);
    }

    public static boolean getBoolean(JsonObject object, String key) {
        return getBoolean(object, key, null);
    }

    public static JsonArray getArray(JsonObject object, String key) {
        return getArray(object, key, null);
    }

    public static String getString(JsonObject object, String key, String defaultVal) {
        JsonElement element = object.get(key);
        if(element == null || element.isJsonNull()) {
            return defaultVal;
        }
        if(!element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            throw new IllegalArgumentException(key + " is not a string");
        }
        return element.getAsString();
    }

    public static boolean getBoolean(JsonObject object, String key, Boolean defaultVal) {
        JsonElement element = object.get(key);
        if(element == null || element.isJsonNull()) {
            return defaultVal;
        }
        if(!element.isJsonPrimitive() || !((JsonPrimitive) element).isBoolean()) {
            throw new IllegalArgumentException(key + " is not a primitive");
        }
        return element.getAsBoolean();
    }

    public static JsonArray getArray(JsonObject object, String key, JsonArray defaultVal) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultVal;
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException(key + " is not an array");
        }
        return element.getAsJsonArray();
    }
}
