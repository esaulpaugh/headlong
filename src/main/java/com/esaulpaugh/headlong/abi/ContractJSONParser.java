package com.esaulpaugh.headlong.abi;

import com.google.gson.*;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Experimental. Unoptimized.
 */
public class ContractJSONParser {

    static final String NAME = "name";
    static final String TYPE = "type";
    static final String INPUTS = "inputs";
    static final String OUTPUTS = "outputs";
    static final String COMPONENTS = "components";
    static final String TUPLE = "tuple";
    static final String EVENT = "event";
    static final String ANONYMOUS = "anonymous";
    static final String INDEXED = "indexed";
    static final String FUNCTION = "function";

    public static List<Function> getFunctions(String json) throws ParseException {

        final MessageDigest digest = Function.newDefaultDigest();

        final List<Function> list = new ArrayList<>();
        for(JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if (FUNCTION.equals(getString(elementObj, TYPE))) {
                    list.add(parseFunction(elementObj, digest));
                }
            }
        }
        return list;
    }

    public static Function parseFunction(String json) throws ParseException {
        return parseFunction(new JsonParser().parse(json).getAsJsonObject(), Function.newDefaultDigest());
    }

    public static List<Event> getEvents(String json) throws ParseException {
        final List<Event> list = new ArrayList<>();
        for(JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if (EVENT.equals(getString(elementObj, TYPE))) {
                    list.add(parseEvent(elementObj));
                }
            }
        }
        return list;
    }

    private static Function parseFunction(JsonObject function, MessageDigest messageDigest) throws ParseException {

        String type = getString(function, TYPE);
        if (!FUNCTION.equals(type)) {
            throw new IllegalArgumentException("unexpected type: " + type);
        }

        String name = getString(function, NAME);

        JsonArray inputs = getArray(function, INPUTS);
        final int inputsLen = inputs.size();
        ABIType<?>[] inputsArray = new ABIType<?>[inputsLen];
        for (int i = 0; i < inputsLen; i++) {
            inputsArray[i] = buildType(inputs.get(i).getAsJsonObject());
        }

        String rawSignature = name + buildTupleTypeString(inputsArray);

        Function protoFunction = Function.parse(rawSignature);

        JsonArray outputs = getArray(function, OUTPUTS, false);

        TupleType outputTypes;
        if (outputs != null) {
            final int outputsLen = outputs.size();
            ABIType<?>[] outputsArray = new ABIType<?>[outputsLen];
            for (int i = 0; i < outputsLen; i++) {
                outputsArray[i] = buildType(outputs.get(i).getAsJsonObject());
            }
            outputTypes = TupleType.create(buildTupleTypeString(outputsArray), outputsArray);
        } else {
            outputTypes = null;
        }

        return new Function(
                protoFunction.canonicalSignature,
                messageDigest,
                TupleType.create(protoFunction.inputTypes.canonicalType, inputsArray),
                outputTypes
        );
    }

    static Event parseEvent(JsonObject event) throws ParseException {
        String type = getString(event, TYPE);
        if (!EVENT.equals(type)) {
            throw new IllegalArgumentException("unexpected type: " + type);
        }

        String name = getString(event, NAME);

        JsonArray inputs = getArray(event, INPUTS);
        final int inputsLen = inputs.size();
        ABIType<?>[] inputsArray = new ABIType<?>[inputsLen];
        boolean[] indexed = new boolean[inputsLen];
        for (int i = 0; i < inputsLen; i++) {
            JsonObject e = inputs.get(i).getAsJsonObject();
            inputsArray[i] = buildType(inputs.get(i).getAsJsonObject());
            indexed[i] = getBoolean(e, INDEXED, false, false);
        }

        return new Event(name, TupleType.create(buildTupleTypeString(inputsArray), inputsArray), indexed, getBoolean(event, ANONYMOUS, false, false));
    }

    static String buildTupleTypeString(ABIType<?>[] types) {
        final StringBuilder sb = new StringBuilder("(");
        for(ABIType<?> type : types) {
            sb.append(type.canonicalType).append(',');
        }
        return TupleTypeParser.completeTupleTypeString(sb);
    }

    static ABIType<?> buildTypeForTuple(JsonObject object) throws ParseException {

        final String type = getString(object, TYPE);

        final String suffix = type.substring(TUPLE.length());

        JsonArray components = getArray(object, COMPONENTS);
        final int componentsLen = components.size();

        String baseTupleTypeString = buildTupleTypeString(components, "");

        ABIType<?>[] elements = new ABIType[componentsLen];
        for (int i = 0; i < componentsLen; i++) {
            JsonObject element = components.get(i).getAsJsonObject();
            elements[i] = buildType(element);
        }

        TupleType base = TupleType.create(baseTupleTypeString, elements);

        return TypeFactory.createForTuple(base.canonicalType + suffix, base, getString(object, NAME));
    }

    static ABIType<?> buildType(JsonObject object) throws ParseException {
        if(getString(object, TYPE).startsWith(TUPLE)) {
            return buildTypeForTuple(object);
        }
        return TypeFactory.createFromJsonObject(object);
    }

    private static String buildTypeString(JsonObject object) throws ParseException {
        final String type = getString(object, TYPE);

        if(type.startsWith(TUPLE)) {
            String suffix = type.substring(TUPLE.length());
            return buildTupleTypeString(getArray(object, COMPONENTS), suffix);
        }
        return type;
    }

    private static String buildTupleTypeString(JsonArray array, String suffix) throws ParseException {
        final StringBuilder sb = new StringBuilder("(");
        for(JsonElement component : array) {
            sb.append(buildTypeString(component.getAsJsonObject()))
                    .append(',');
        }
        return TupleTypeParser.completeTupleTypeString(sb) + suffix;
    }

    static JsonElement parse(String json) {
        return new JsonParser().parse(json);
    }

    static JsonObject parseObject(String json) {
        return parse(json).getAsJsonObject();
    }

    static String getString(JsonObject object, String key) {
        return getString(object, key, true);
    }

    static boolean getBoolean(JsonObject object, String key) {
        return getBoolean(object, key, true, false);
    }

    static JsonArray getArray(JsonObject object, String key) {
        return getArray(object, key, true);
    }

    static String getString(JsonObject object, String key, boolean requireNonNull) {
        JsonElement element = object.get(key);
        if(element == null) {
            if(requireNonNull) {
                throw new IllegalArgumentException(key + " not found");
            }
            return null;
        }
        if(!element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            throw new IllegalArgumentException(key + " is not a string");
        }
        return element.getAsString();
    }

    static boolean getBoolean(JsonObject object, String key, boolean requireNonNull, boolean defaultVal) {
        JsonElement element = object.get(key);
        if(element == null) {
            if(requireNonNull) {
                throw new IllegalArgumentException(key + " not found");
            }
            return defaultVal;
        }
        if(!element.isJsonPrimitive() || !((JsonPrimitive) element).isBoolean()) {
            throw new IllegalArgumentException(key + " is not a primitive");
        }
        return element.getAsBoolean();
    }

    static JsonArray getArray(JsonObject object, String key, boolean requireNonNull) {
        JsonElement element = object.get(key);
        if (element == null) {
            if(requireNonNull) {
                throw new IllegalArgumentException(key + " not found");
            }
            return null;
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException(key + " is not an array");
        }
        return element.getAsJsonArray();
    }
}
