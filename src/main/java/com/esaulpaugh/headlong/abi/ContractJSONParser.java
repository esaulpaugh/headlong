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
    static final String CONSTRUCTOR = "constructor";
    static final String FALLBACK = "fallback";
    static final String STATE_MUTABILITY = "stateMutability";

    public static List<Function> parseFunctions(String json) throws ParseException {
        return parseObjects(json, true, false, Function.class);
    }

    public static List<Function> getFunctions(String json) throws ParseException {
        final MessageDigest digest = Function.newDefaultDigest();
        final List<Function> list = new ArrayList<>();
        for(JsonElement element : parseArray(json)) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                String type = getString(elementObj, TYPE);
                if(type == null) {
                    list.add(parseFunction(elementObj, digest));
                } else {
                    switch (type) {
                    case FUNCTION:
                    case CONSTRUCTOR:
                    case FALLBACK: list.add(parseFunction(elementObj, digest));
                    }
                }
            }
        }
        return list;
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
                                                             final Class<T> classOfT) throws ParseException {

        final Supplier<MessageDigest> defaultDigest = functions ? Function::newDefaultDigest : null;

        final List<T> list = new ArrayList<>();
        for(JsonElement e : parseArray(json)) {
            if(e.isJsonObject()) {
                JsonObject object = (JsonObject) e;
                String type = getString(object, TYPE);
                switch (type) {
                case FUNCTION:
                case CONSTRUCTOR:
                case FALLBACK:
                    if(functions) {
                        list.add(classOfT.cast(parseFunction(object, defaultDigest.get())));
                    }
                    break;
                case EVENT:
                    if(events) {
                        list.add(classOfT.cast(parseEvent(object)));
                    }
                }
            }
        }
        return list;
    }

    public static Function parseFunction(String json) throws ParseException {
        return parseFunction(parseObject(json), Function.newDefaultDigest());
    }

    private static Function parseFunction(JsonObject function, MessageDigest messageDigest) throws ParseException {

        final JsonArray inputs = getArray(function, INPUTS);

        final TupleType inputTypes;
        if(inputs != null) {
            final ArrayList<ABIType<?>> inputsList = new ArrayList<>(inputs.size());
            for (JsonElement e : inputs) {
                inputsList.add(buildType(e.getAsJsonObject()));
            }
            inputTypes = TupleType.create(inputsList);
        } else {
            inputTypes = TupleType.EMPTY;
        }

        final JsonArray outputs = getArray(function, OUTPUTS);
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

        return new Function(
                getString(function, NAME),
                inputTypes,
                outputTypes,
                getString(function, STATE_MUTABILITY),
                messageDigest
        );
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
            indexed[i] = getBoolean(inputObj, INDEXED);
        }
        return new Event(
                getString(event, NAME),
                TupleType.create(inputsList),
                indexed,
                getBoolean(event, ANONYMOUS, false)
        );
    }

    private static ABIType<?> buildType(JsonObject object) throws ParseException {
        final String type = getString(object, TYPE);
        final String name = getString(object, ContractJSONParser.NAME);

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