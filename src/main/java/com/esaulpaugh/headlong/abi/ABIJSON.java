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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.util.JsonUtils.getArray;
import static com.esaulpaugh.headlong.util.JsonUtils.getBoolean;
import static com.esaulpaugh.headlong.util.JsonUtils.getString;
import static com.esaulpaugh.headlong.util.JsonUtils.parseArray;
import static com.esaulpaugh.headlong.util.JsonUtils.parseObject;

/** For parsing JSON representations of {@link ABIObject}s according to the ABI specification. */
public final class ABIJSON {

    private ABIJSON() {}

    private static final String NAME = "name";
    private static final String TYPE = "type";
    static final String FUNCTION = "function";
    static final String RECEIVE = "receive";
    static final String FALLBACK = "fallback";
    static final String CONSTRUCTOR = "constructor";
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";
    private static final String TUPLE = "tuple";
    private static final String COMPONENTS = "components";
    private static final String EVENT = "event";
    private static final String ANONYMOUS = "anonymous";
    private static final String INDEXED = "indexed";
    private static final String STATE_MUTABILITY = "stateMutability";
    private static final String PURE = "pure";
    private static final String VIEW = "view";
    static final String PAYABLE = "payable";
//    private static final String NONPAYABLE = "nonpayable";// to mark as nonpayable, do not specify any stateMutability
    private static final String CONSTANT = "constant"; // deprecated

    private static final Gson GSON;
    private static final Gson GSON_PRETTY;

    static {
        GsonBuilder builder = new GsonBuilder();
        GSON = builder.create();
        GSON_PRETTY = builder.setPrettyPrinting().create();
    }

    public static Function parseFunction(String objectJson) {
        return (Function) parseABIObject(objectJson);
    }

    public static Event parseEvent(String objectJson) {
        return (Event) parseABIObject(objectJson);
    }

    public static ABIObject parseABIObject(String objectJson) {
        return parseABIObject(parseObject(objectJson));
    }

    public static ABIObject parseABIObject(JsonObject object) {
        return EVENT.equals(getString(object, TYPE))
                ? parseEvent(object)
                : parseFunction(object, Function.newDefaultDigest());
    }

    public static List<Function> parseFunctions(String arrayJson) {
        return parseFunctions(arrayJson, Function.newDefaultDigest());
    }

    public static List<Function> parseFunctions(String arrayJson, MessageDigest digest) {
        return parseObjects(arrayJson, true, false, digest, Function.class);
    }

    public static List<Event> parseEvents(String arrayJson) {
        return parseObjects(arrayJson, false, true, null, Event.class);
    }

    public static <T extends ABIObject> List<T> parseObjects(final String arrayJson,
                                                             final boolean functions,
                                                             final boolean events,
                                                             final MessageDigest digest,
                                                             final Class<T> classOfT) { // e.g. ABIObject.class
        final List<T> abiObjects = new ArrayList<>();
        for (JsonElement e : parseArray(arrayJson)) {
            if (e.isJsonObject()) {
                JsonObject jsonObj = (JsonObject) e;
                switch (getString(jsonObj, TYPE, FUNCTION)) {
                case FUNCTION:
                case RECEIVE:
                case FALLBACK:
                case CONSTRUCTOR:
                    if (functions) {
                        abiObjects.add(classOfT.cast(parseFunction(jsonObj, digest)));
                    }
                    break;
                case EVENT:
                    if (events) {
                        abiObjects.add(classOfT.cast(parseEvent(jsonObj)));
                    }
                    break;
                default: /* skip */
                }
            }
        }
        return abiObjects;
    }

    public static Function parseFunction(JsonObject function, MessageDigest messageDigest) {
        return new Function(
                parseFunctionType(getString(function, TYPE)),
                getString(function, NAME),
                parseTypes(getArray(function, INPUTS)),
                parseTypes(getArray(function, OUTPUTS)),
                getString(function, STATE_MUTABILITY),
                messageDigest
        );
    }
// ---------------------------------------------------------------------------------------------------------------------
    private static Function.Type parseFunctionType(String type) {
        if(type != null) {
            switch (type) {
            case FUNCTION: return Function.Type.FUNCTION;
            case RECEIVE: return Function.Type.RECEIVE;
            case FALLBACK: return Function.Type.FALLBACK;
            case CONSTRUCTOR: return Function.Type.CONSTRUCTOR;
            default: throw new IllegalArgumentException("unexpected type: \"" + type + "\"");
            }
        }
        return Function.Type.FUNCTION;
    }

    private static TupleType parseTypes(JsonArray array) {
        if (array != null) {
            final ABIType<?>[] elementsArray = new ABIType[array.size()];
            for (int i = 0; i < elementsArray.length; i++) {
                elementsArray[i] = parseType(array.get(i).getAsJsonObject());
            }
            return TupleType.wrap(elementsArray);
        }
        return TupleType.EMPTY;
    }

    static Event parseEvent(JsonObject event) {
        final String type = getString(event, TYPE);
        if (EVENT.equals(type)) {
            final JsonArray inputs = getArray(event, INPUTS);
            if (inputs != null) {
                final int inputsLen = inputs.size();
                final ABIType<?>[] inputsArray = new ABIType[inputsLen];
                final boolean[] indexed = new boolean[inputsLen];
                for (int i = 0; i < inputsLen; i++) {
                    JsonObject inputObj = inputs.get(i).getAsJsonObject();
                    inputsArray[i] = parseType(inputObj);
                    indexed[i] = getBoolean(inputObj, INDEXED);
                }
                return new Event(
                        getString(event, NAME),
                        getBoolean(event, ANONYMOUS, false),
                        TupleType.wrap(inputsArray),
                        indexed
                );
            }
            throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
        }
        throw new IllegalArgumentException("unexpected type: " + (type == null ? null : "\"" + type + "\""));
    }

    private static ABIType<?> parseType(JsonObject object) {
        final String typeStr = getString(object, TYPE);
        if(typeStr.startsWith(TUPLE)) {
            TupleType baseType = parseTypes(getArray(object, COMPONENTS));
            return TypeFactory.build(
                    baseType.canonicalType + typeStr.substring(TUPLE.length()), // + suffix e.g. "[4][]"
                    baseType,
                    getString(object, NAME));
        }
        return TypeFactory.create(typeStr, Object.class, getString(object, NAME));
    }
// ---------------------------------------------------------------------------------------------------------------------
    public static String toJson(ABIObject x, boolean function, boolean pretty) {
        try {
            StringWriter stringOut = new StringWriter();
            JsonWriter out = (pretty ? GSON_PRETTY : GSON).newJsonWriter(stringOut);
            out.beginObject();
            if(function) {
                Function f = (Function) x;
                final Function.Type type = f.getType();
                out.name(TYPE).value(type.toString());
                if (type != Function.Type.FALLBACK) {
                    addIfValueNotNull(out, NAME, x.getName());
                    if (type != Function.Type.RECEIVE) {
                        writeJsonArray(out, INPUTS, x.getInputs(), null);
                        if (type != Function.Type.CONSTRUCTOR) {
                            writeJsonArray(out, OUTPUTS, f.getOutputs(), null);
                        }
                    }
                }
                final String stateMutability = f.getStateMutability();
                addIfValueNotNull(out, STATE_MUTABILITY, stateMutability);
                out.name(CONSTANT).value(VIEW.equals(stateMutability) || PURE.equals(stateMutability));
            } else {
                out.name(TYPE).value(EVENT);
                addIfValueNotNull(out, NAME, x.getName());
                writeJsonArray(out, INPUTS, x.getInputs(), ((Event) x).getIndexManifest());
            }
            out.endObject();
            return stringOut.toString();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    private static void writeJsonArray(JsonWriter out, String name, TupleType tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
            final ABIType<?> e = tupleType.elementTypes[i];
            out.beginObject();
            addIfValueNotNull(out, NAME, e.getName());
            out.name(TYPE);
            final String type = e.canonicalType;
            if(type.startsWith("(")) { // tuple
                out.value(type.replace(type.substring(0, type.lastIndexOf(')') + 1), TUPLE));
                ABIType<?> base = e;
                while (ABIType.TYPE_CODE_ARRAY == base.typeCode()) {
                    base = ((ArrayType<? extends ABIType<?>, ?>) base).getElementType();
                }
                writeJsonArray(out, COMPONENTS, (TupleType) base, null);
            } else {
                out.value(type);
            }
            if(indexedManifest != null) {
                out.name(INDEXED).value(indexedManifest[i]);
            }
            out.endObject();
        }
        out.endArray();
    }

    private static void addIfValueNotNull(JsonWriter out, String key, String value) throws IOException {
        if(value != null) {
            out.name(key).value(value);
        }
    }
}
