package com.esaulpaugh.headlong.abi;

import com.google.gson.*;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.abi.util.JsonUtils.*;

/**
 * Experimental. Unoptimized.
 */
public class ContractJSONParser {

    static final String NAME = "name";
    static final String TYPE = "type";
    static final String FUNCTION = "function";
    static final String INPUTS = "inputs";
    static final String OUTPUTS = "outputs";
    static final String TUPLE = "tuple";
    static final String COMPONENTS = "components";
    static final String EVENT = "event";
    static final String ANONYMOUS = "anonymous";
    static final String INDEXED = "indexed";

    public static List<Function> getFunctions(String json) throws ParseException {
        final MessageDigest digest = Function.newDefaultDigest();
        final List<Function> list = new ArrayList<>();
        for(JsonElement element : parseArray(json)) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if (FUNCTION.equals(getString(elementObj, TYPE))) {
                    list.add(parseFunction(elementObj, digest));
                }
            }
        }
        return list;
    }

    public static List<Event> getEvents(String json) throws ParseException {
        final List<Event> list = new ArrayList<>();
        for(JsonElement element : parseArray(json)) {
            if(element.isJsonObject()) {
                final JsonObject elementObj = (JsonObject) element;
                if (EVENT.equals(getString(elementObj, TYPE))) {
                    list.add(parseEvent(elementObj));
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
        final int inputsLen = inputs.size();
        final ABIType<?>[] inputsArray = new ABIType<?>[inputsLen];
        final StringBuilder inputsSB = new StringBuilder("(");
        for (int i = 0; i < inputsLen; i++) {
            inputsArray[i] = buildType(inputs.get(i).getAsJsonObject(), inputsSB);
        }

        final JsonArray outputs = getArray(function, OUTPUTS, false);
        final TupleType outputTypes;
        if (outputs != null) {
            final int outputsLen = outputs.size();
            final ABIType<?>[] outputsArray = new ABIType<?>[outputsLen];
            final StringBuilder outputsSB = new StringBuilder("(");
            for (int i = 0; i < outputsLen; i++) {
                outputsArray[i] = buildType(outputs.get(i).getAsJsonObject(), outputsSB);
            }
            final String outputsTupleTypeString = TupleTypeParser.completeTupleTypeString(outputsSB);
            outputTypes = TupleType.create(outputsTupleTypeString, outputsArray);
        } else {
            outputTypes = null;
        }

        final Function protoFunction = Function.parse(getString(function, NAME) + TupleTypeParser.completeTupleTypeString(inputsSB));

        return new Function(
                protoFunction.canonicalSignature,
                messageDigest,
                TupleType.create(protoFunction.inputTypes.canonicalType, inputsArray),
                outputTypes
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
        final ABIType<?>[] inputsArray = new ABIType<?>[inputsLen];
        final boolean[] indexed = new boolean[inputsLen];
        final StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < inputsLen; i++) {
            JsonObject input = inputs.get(i).getAsJsonObject();
            inputsArray[i] = buildType(inputs.get(i).getAsJsonObject(), sb);
            indexed[i] = getBoolean(input, INDEXED, false, false);
        }
        return new Event(
                getString(event, NAME),
                TupleType.create(TupleTypeParser.completeTupleTypeString(sb), inputsArray),
                indexed,
                getBoolean(event, ANONYMOUS, false, false)
        );
    }

    private static ABIType<?> buildType(JsonObject object, StringBuilder parentSb) throws ParseException {
        final String type = getString(object, TYPE);

        if(type.startsWith(TUPLE)) {
            final JsonArray components = getArray(object, COMPONENTS);
            final int componentsLen = components.size();
            final StringBuilder sb = new StringBuilder("(");
            final ABIType<?>[] elements = new ABIType[componentsLen];
            for (int i = 0; i < componentsLen; i++) {
                elements[i] = buildType(components.get(i).getAsJsonObject(), sb);
            }
            final TupleType base = TupleType.create(TupleTypeParser.completeTupleTypeString(sb), elements);

            final String canonical = base.canonicalType + type.substring(TUPLE.length()); // suffix e.g. "[4][]"

            parentSb.append(canonical).append(',');

            return TypeFactory.createForTuple(canonical, base, getString(object, NAME));
        }

        final ABIType<?> abiType = TypeFactory.createFromJsonObject(object);
        parentSb.append(abiType.canonicalType).append(',');
        return abiType;
    }
}