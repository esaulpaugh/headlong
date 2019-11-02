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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.util.JsonUtils.getArray;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getBoolean;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getString;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.parseArray;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.parseObject;

public final class ABIJSON {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    static final String FUNCTION = "function";
    static final String CONSTRUCTOR = "constructor";
    static final String FALLBACK = "fallback";
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
//    private static final String PAYABLE = "payable";
//    private static final String NONPAYABLE = "nonpayable";
    private static final String CONSTANT = "constant"; // deprecated

    public static ABIObject parseABIObject(String json) throws ParseException {
        return parseABIObject(parseObject(json));
    }

    public static ABIObject parseABIObject(JsonObject object) throws ParseException {
        return EVENT.equals(getString(object, TYPE)) ? parseEvent(object) : parseFunction(object);
    }

    public static List<Function> parseFunctions(String json) throws ParseException {
        return parseObjects(json, true, false, Function.class);
    }

    public static List<Event> parseEvents(String json) throws ParseException {
        return parseObjects(json, false, true, Event.class);
    }

    private static <T extends ABIObject> List<T> parseObjects(final String json,
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
                case FALLBACK:
                case CONSTRUCTOR:
                case FUNCTION:
                    if(functions) {
                        list.add(classOfT.cast(parseFunction(object, defaultDigest.get())));
                    }
                    break;
                case EVENT:
                    if (events) {
                        list.add(classOfT.cast(parseEvent(object)));
                    }
                    break;
                default: /* skip */
                }
            }
        }
        return list;
    }

    public static Function parseFunction(String json) throws ParseException {
        return parseFunction(parseObject(json), Function.newDefaultDigest());
    }

    public static Function parseFunction(JsonObject function) throws ParseException {
        return parseFunction(function, Function.newDefaultDigest());
    }

    public static Function parseFunction(JsonObject function, MessageDigest messageDigest) throws ParseException {
        final String typeString = getString(function, TYPE);
        Function.Type type = Function.Type.get(typeString);
        if(type == null) {
            throw unexpectedException(TYPE, typeString);
        }
        return new Function(
                type,
                getString(function, NAME),
                parseArrayForFunction(function, INPUTS),
                parseArrayForFunction(function, OUTPUTS),
                getString(function, STATE_MUTABILITY),
                messageDigest
        );
    }

    private static TupleType parseArrayForFunction(JsonObject function, String name) throws ParseException {
        final JsonArray array = getArray(function, name);
        if (array != null) {
            final ABIType<?>[] elementsArray = new ABIType[array.size()];
            int i = 0;
            for (JsonElement e : array) {
                elementsArray[i++] = parseType(e.getAsJsonObject());
            }
            return TupleType.wrap(elementsArray);
        }
        return TupleType.EMPTY;
    }

    static Event parseEvent(String eventJson) throws ParseException {
        return parseEvent(parseObject(eventJson));
    }

    static Event parseEvent(JsonObject event) throws ParseException {
        final String type = getString(event, TYPE);
        if (!EVENT.equals(type)) {
            throw unexpectedException(TYPE, type);
        }

        final JsonArray inputs = getArray(event, INPUTS);
        if(inputs == null) {
            throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
        }
        final int inputsLen = inputs.size();
        final ABIType<?>[] inputsArray = new ABIType[inputs.size()];
        final boolean[] indexed = new boolean[inputsLen];
        for (int i = 0; i < inputsLen; i++) {
            JsonObject inputObj = inputs.get(i).getAsJsonObject();
            inputsArray[i] = parseType(inputObj);
            indexed[i] = getBoolean(inputObj, INDEXED);
        }
        return new Event(
                getString(event, NAME),
                TupleType.wrap(inputsArray),
                indexed,
                getBoolean(event, ANONYMOUS, false)
        );
    }

    private static ABIType<?> parseType(JsonObject object) throws ParseException {
        final String type = getString(object, TYPE);
        final String name = getString(object, NAME);

        if(type.startsWith(TUPLE)) {
            final JsonArray components = getArray(object, COMPONENTS);
            final ABIType<?>[] componentsArray = new ABIType[components.size()];
            int i = 0;
            for (JsonElement c : components) {
                componentsArray[i++] = parseType(c.getAsJsonObject());
            }
            final TupleType base = TupleType.wrap(componentsArray);
            final String suffix = type.substring(TUPLE.length()); // suffix e.g. "[4][]"
            return TypeFactory.createForTuple(base, suffix, name);
        }
        return TypeFactory.create(type, null, name);
    }

    private static IllegalArgumentException unexpectedException(String key, String value) {
        return new IllegalArgumentException("unexpected " + key + ": " + (value == null ? null : "\"" + value + "\""));
    }
// -------------------------------------------
    static JsonObject buildFunctionJson(Function f) {
        JsonObject object = new JsonObject();
        Function.Type type = f.getType();
        object.add(TYPE, new JsonPrimitive(type.toString()));
        if(type != Function.Type.FALLBACK) {
            String name = f.getName();
            if(name != null) {
                object.add(NAME, new JsonPrimitive(name));
            }
            object.add(INPUTS, buildJsonArray(f.getParamTypes(), null));
        }
        if(type != Function.Type.FALLBACK && type != Function.Type.CONSTRUCTOR) {
            object.add(OUTPUTS, buildJsonArray(f.getOutputTypes(), null));
        }
        String stateMutability = f.getStateMutability();
        if(stateMutability != null) {
            object.add(STATE_MUTABILITY, new JsonPrimitive(stateMutability));
        }
        object.add(CONSTANT, new JsonPrimitive(VIEW.equals(stateMutability) || PURE.equals(stateMutability)));
        return object;
    }

    static JsonObject buildEventJson(Event event) {
        JsonObject object = new JsonObject();
        object.add(TYPE, new JsonPrimitive(EVENT));
        String name = event.getName();
        if(name != null) {
            object.add(NAME, new JsonPrimitive(name));
        }
        object.add(INPUTS, buildJsonArray(event.getParams(), event.getIndexManifest()));
        return object;
    }

    private static JsonArray buildJsonArray(TupleType tupleType, boolean[] indexedManifest) {
        JsonArray array = new JsonArray();
        ABIType<?>[] elements = tupleType.elementTypes;
        final int len = elements.length;
        boolean addIndexed = indexedManifest != null;
        for (int i = 0; i < len; i++) {
            ABIType<?> e = elements[i];
            JsonObject arrayElement = new JsonObject();
            String name = e.getName();
            arrayElement.add(NAME, name == null ? null : new JsonPrimitive(e.getName()));
            boolean tuple = e.canonicalType.startsWith("(");
            String type = e.canonicalType;
            if(tuple) {
                String substring = type.substring(type.indexOf('('), type.lastIndexOf(')') + 1);
                type = type.replace(substring, TUPLE);
            }
            arrayElement.add(TYPE, new JsonPrimitive(type));
            if(tuple) {
                ABIType<?> base = e;
                while (base instanceof ArrayType<?, ?>) {
                    base = ((ArrayType<?, ?>) base).elementType;
                }
                JsonArray components = buildJsonArray((TupleType) base, null);
                arrayElement.add(COMPONENTS, components);
            }
            array.add(arrayElement);
            if(addIndexed) {
                arrayElement.add(INDEXED, new JsonPrimitive(indexedManifest[i]));
            }
        }
        return array;
    }
}
