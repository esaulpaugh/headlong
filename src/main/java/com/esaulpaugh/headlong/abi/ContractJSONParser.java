package com.esaulpaugh.headlong.abi;

import com.google.gson.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Experimental. Unoptimized.
 */
public class ContractJSONParser {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String INPUTS = "inputs";
    private static final String COMPONENTS = "components";
    private static final String TUPLE = "tuple";
    private static final String FUNCTION = "function";

    public static List<Function> getFunctions(String json) throws ParseException {
        final List<Function> list = new ArrayList<>();
        for(JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if (FUNCTION.equals(getString(elementObj, TYPE))) {
                    list.add(parseFunction(elementObj));
                }
            }
        }
        return list;
    }

    public static Function parseFunction(String json) throws ParseException {
        return parseFunction(new JsonParser().parse(json).getAsJsonObject());
    }

    private static Function parseFunction(JsonObject object) throws ParseException {
        return new Function(buildFunctionSignature(object));
    }

    public static List<JsonObject> getEvents(String json) {
        final List<JsonObject> list = new ArrayList<>();
        for(JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if ("event".equals(getString(elementObj, TYPE))) {
                    list.add(elementObj);
                }
            }
        }
        return list;
    }

    private static String buildFunctionSignature(JsonObject function) {
        String type = getString(function, TYPE);
        if(!FUNCTION.equals(type)) {
            throw new IllegalArgumentException("unexpected type: " + type);
        }
        final StringBuilder sb = new StringBuilder("(");
        for(JsonElement element : getArray(function, INPUTS)) {
            sb.append(buildTypeString(element.getAsJsonObject()))
                    .append(',');
        }
        return getString(function, NAME) + TupleTypeParser.completeTupleTypeString(sb);
    }

    private static String buildTypeString(JsonObject object) {
        final String type = getString(object, TYPE);
        return type.startsWith(TUPLE)
                ? buildTupleTypeString(type, object)
                : type;
    }

    private static String buildTupleTypeString(String type, JsonObject object) {
        final StringBuilder sb = new StringBuilder("(");
        for(JsonElement component : object.getAsJsonArray(COMPONENTS)) {
            sb.append(buildTypeString(component.getAsJsonObject()))
                    .append(',');
        }
        return TupleTypeParser.completeTupleTypeString(sb) + type.substring(TUPLE.length()); // suffix, e.g. [4][];
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if(element == null) {
            throw new IllegalArgumentException(key + " not found");
        }
        if(!element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            throw new IllegalArgumentException(key + " is not a string");
        }
        return element.getAsString();
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if(element == null) {
            throw new IllegalArgumentException(key + " not found");
        }
        if(!element.isJsonArray()) {
            throw new IllegalArgumentException(key + " is not an array");
        }
        return element.getAsJsonArray();
    }
}
