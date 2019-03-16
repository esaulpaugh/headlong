package com.esaulpaugh.headlong.abi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.util.JsonUtils.*;

/**
 * Experimental. Unoptimized.
 */
public class ContractJSONParser {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String FUNCTION = "function";
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";
    private static final String TUPLE = "tuple";
    private static final String COMPONENTS = "components";
    private static final String EVENT = "event";
    private static final String ANONYMOUS = "anonymous";
    private static final String INDEXED = "indexed";

    public static List<Function> parseFunctions(String json) throws ParseException {
        return parseObjects(json, true, false, Function.class);
    }

    public static List<Event> parseEvents(String json) throws ParseException {
        return parseObjects(json, false, true, Event.class);
    }

    public static List<ABIObject> parseObjects(String json) throws ParseException {
        return parseObjects(json, true, true, ABIObject.class);
    }

    public static <T extends ABIObject> List<T> parseObjects(final String json,
                                                             final boolean functions,
                                                             final boolean events,
                                                             final Class<T> classofT) throws ParseException {

        final Supplier<MessageDigest> defaultDigest = functions ? Function::newDefaultDigest : null;

        final List<T> list = new ArrayList<>();
        for(JsonElement e : parseArray(json)) {
            if(e.isJsonObject()) {
                JsonObject object = (JsonObject) e;
                final String type = getString(object, TYPE);
                if (functions && FUNCTION.equals(type)) {
                    list.add(classofT.cast(parseFunction(object, defaultDigest.get())));
                } else if(events && EVENT.equals(type)) {
                    list.add(classofT.cast(parseEvent(object)));
                }
            }
        }
        return list;
    }

    public static Function parseFunction(String json) throws ParseException {
        return parseFunction(parseObject(json), Function.newDefaultDigest());
    }

    private static Function parseFunction(JsonObject function, MessageDigest messageDigest) throws ParseException {

        final String type = getString(function, TYPE);
        if (!FUNCTION.equals(type)) {
            throw new IllegalArgumentException("unexpected type: " + type);
        }

        final JsonArray inputs = getArray(function, INPUTS);
        final ArrayList<ABIType<?>> inputsList = new ArrayList<>(inputs.size());
        for (JsonElement e : inputs) {
            inputsList.add(buildType(e.getAsJsonObject())); // , inputsSB
        }

        final JsonArray outputs = getArray(function, OUTPUTS, false);
        final TupleType outputTypes;
        if (outputs != null) {
            final ArrayList<ABIType<?>> outputsList = new ArrayList<>(outputs.size());
            for (JsonElement e : outputs) {
                outputsList.add(buildType(e.getAsJsonObject()));
            }
            outputTypes = TupleType.create(outputsList);
        } else {
            outputTypes = null;
        }

        return new Function(getString(function, NAME), messageDigest, TupleType.create(inputsList), outputTypes);
    }

    static Event parseEvent(String eventJson) throws ParseException {
        return parseEvent(parseObject(eventJson));
    }

    static Event parseEvent(JsonObject event) throws ParseException {
        final String type = getString(event, TYPE);
        if (!EVENT.equals(type)) {
            throw new IllegalArgumentException("unexpected type: " + type);
        }

        final JsonArray inputs = getArray(event, INPUTS);
        final int inputsLen = inputs.size();
        final ArrayList<ABIType<?>> inputsList = new ArrayList<>(inputs.size());
        final boolean[] indexed = new boolean[inputsLen];
        for (int i = 0; i < inputsLen; i++) {
            JsonObject inputObj = inputs.get(i).getAsJsonObject();
            inputsList.add(buildType(inputObj));
            indexed[i] = getBoolean(inputObj, INDEXED, false, false);
        }
        return new Event(
                getString(event, NAME),
                TupleType.create(inputsList),
                indexed,
                getBoolean(event, ANONYMOUS, false, false)
        );
    }

    private static ABIType<?> buildType(JsonObject object) throws ParseException {
        final String type = getString(object, TYPE);
        final String name = getString(object, ContractJSONParser.NAME, false);

        if(type.startsWith(TUPLE)) {
            final JsonArray components = getArray(object, COMPONENTS);
            final ArrayList<ABIType<?>> componentsList = new ArrayList<>(components.size());
            for (JsonElement c : components) {
                componentsList.add(buildType(c.getAsJsonObject()));
            }
            final TupleType base = TupleType.create(componentsList);
            final String suffix = type.substring(TUPLE.length()); // suffix e.g. "[4][]"
            return TypeFactory.createForTuple(base, suffix, name);
        }
        return TypeFactory.create(type, null, name);
    }
}