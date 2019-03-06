package com.esaulpaugh.headlong.abi;

import com.google.gson.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Experimental. Unoptimized.
 */
public class ContractJSONParser {

    public static List<Function> getFunctions(String json) throws ParseException {
        final List<Function> list = new ArrayList<>();
        for(JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if ("function".equals(getType(elementObj))) {
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
                if ("event".equals(getType(elementObj))) {
                    list.add(elementObj);
                }
            }
        }
        return list;
    }

    private static String buildFunctionSignature(JsonObject function) {
        final StringBuilder sb = new StringBuilder("(");
        for(JsonElement element : function.getAsJsonArray("inputs")) {
            sb.append(buildTypeString(element.getAsJsonObject()))
                    .append(',');
        }
        return function.get("name").getAsString() + TupleTypeParser.completeTupleTypeString(sb);
    }

    private static String buildTypeString(JsonObject object) {
        final String type = getType(object);
        return type.startsWith("tuple")
                ? buildTupleTypeString(type, object)
                : type;
    }

    private static String buildTupleTypeString(String type, JsonObject object) {
        final StringBuilder sb = new StringBuilder("(");
        for(JsonElement component : object.getAsJsonArray("components")) {
            sb.append(buildTypeString(component.getAsJsonObject()))
                    .append(',');
        }
        return TupleTypeParser.completeTupleTypeString(sb) + type.substring("tuple".length()); // suffix, e.g. [4][];
    }

    private static String getType(JsonObject object) {
        return object.get("type").getAsString();
    }
}
