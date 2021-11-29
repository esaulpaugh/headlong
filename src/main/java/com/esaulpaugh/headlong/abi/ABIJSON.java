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
import com.google.gson.stream.JsonWriter;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.esaulpaugh.headlong.abi.util.JsonUtils.getArray;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getBoolean;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getString;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.parseArray;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.parseObject;

/** For parsing JSON representations of {@link ABIObject}s according to the ABI specification. */
public final class ABIJSON {

    private ABIJSON() {}

    public static final Set<TypeEnum> FUNCTIONS = Collections.unmodifiableSet(EnumSet.of(TypeEnum.FUNCTION, TypeEnum.RECEIVE, TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR));
    public static final Set<TypeEnum> EVENTS    = Collections.unmodifiableSet(EnumSet.of(TypeEnum.EVENT));
    public static final Set<TypeEnum> ERRORS    = Collections.unmodifiableSet(EnumSet.of(TypeEnum.ERROR));
    public static final Set<TypeEnum> ALL       = Collections.unmodifiableSet(EnumSet.allOf(TypeEnum.class));

    private static final String NAME = "name";
    private static final String TYPE = "type";
    static final String FUNCTION = "function";
    static final String RECEIVE = "receive";
    static final String FALLBACK = "fallback";
    static final String CONSTRUCTOR = "constructor";
    static final String EVENT = "event";
    static final String ERROR = "error";
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";
    private static final String TUPLE = "tuple";
    private static final String COMPONENTS = "components";
    private static final String ANONYMOUS = "anonymous";
    private static final String INDEXED = "indexed";
    private static final String STATE_MUTABILITY = "stateMutability";
    private static final String PURE = "pure";
    private static final String VIEW = "view";
    static final String PAYABLE = "payable";
//    private static final String NONPAYABLE = "nonpayable";// to mark as nonpayable, do not specify any stateMutability
    private static final String CONSTANT = "constant"; // deprecated

    public static Function parseFunction(String objectJson) {
        return parseFunction(parseObject(objectJson));
    }

    public static Function parseFunction(JsonObject function) {
        return parseFunction(function, Function.newDefaultDigest());
    }

    public static Function parseFunction(JsonObject function, MessageDigest messageDigest) {
        return _parseFunction(TypeEnum.parse(getType(function)), function, messageDigest);
    }

    public static Event parseEvent(String objectJson) {
        return parseEvent(parseObject(objectJson));
    }

    public static Event parseEvent(JsonObject event) {
        if(EVENT.equals(getType(event))) {
            return _parseEvent(event);
        }
        throw TypeEnum.unexpectedType(getType(event));
    }

    public static ContractError parseError(JsonObject error) {
        if(ERROR.equals(getType(error))) {
            return _parseError(error);
        }
        throw TypeEnum.unexpectedType(getType(error));
    }

    public static ABIObject parseABIObject(JsonObject object) {
        return parseABIObject(TypeEnum.parse(getType(object)), object, Function.newDefaultDigest());
    }

    /**
     * Selects all objects with type {@link TypeEnum#FUNCTION}.
     *
     * @see #parseFunctions(String) 
     * @param arrayJson the JSON array string
     * @return  the parsed {@link Function}s
     */
    public static List<Function> parseNormalFunctions(String arrayJson) {
        return parseElements(arrayJson, EnumSet.of(TypeEnum.FUNCTION));
    }

    /**
     * Selects all objects with type {@link TypeEnum#FUNCTION}, {@link TypeEnum#RECEIVE}, {@link TypeEnum#FALLBACK}, or
     * {@link TypeEnum#CONSTRUCTOR}.
     *
     * @param arrayJson the JSON array string
     * @return  the parsed {@link Function}s
     */
    public static List<Function> parseFunctions(String arrayJson) {
        return parseElements(arrayJson, FUNCTIONS);
    }

    public static List<Event> parseEvents(String arrayJson) {
        return parseElements(arrayJson, EVENTS);
    }

    public static List<ContractError> parseErrors(String arrayJson) {
        return parseElements(arrayJson, ERRORS);
    }

    public static List<ABIObject> parseElements(String arrayJson) {
        return parseElements(arrayJson, ALL);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ABIObject> List<T> parseElements(String arrayJson, Set<TypeEnum> types) {
        final List<T> selected = new ArrayList<>();
        final MessageDigest digest = Function.newDefaultDigest();
        for (JsonElement e : parseArray(arrayJson)) {
            if (e.isJsonObject()) {
                JsonObject object = e.getAsJsonObject();
                TypeEnum t = TypeEnum.parse(getType(object));
                if(types.contains(t)) {
                    selected.add((T) parseABIObject(t, object, digest));
                }
            }
        }
        return selected;
    }

    private static ABIObject parseABIObject(TypeEnum t, JsonObject object, MessageDigest digest) {
        switch (t.ordinal()) {
        case TypeEnum.ORDINAL_FUNCTION:
        case TypeEnum.ORDINAL_RECEIVE:
        case TypeEnum.ORDINAL_FALLBACK:
        case TypeEnum.ORDINAL_CONSTRUCTOR: return _parseFunction(t, object, digest);
        case TypeEnum.ORDINAL_EVENT: return _parseEvent(object);
        case TypeEnum.ORDINAL_ERROR: return _parseError(object);
        default: throw new AssertionError();
        }
    }
// ---------------------------------------------------------------------------------------------------------------------
    private static Function _parseFunction(TypeEnum type, JsonObject function, MessageDigest digest) {
        return new Function(
                type,
                getName(function),
                parseTupleType(function, INPUTS),
                parseTupleType(function, OUTPUTS),
                getString(function, STATE_MUTABILITY),
                digest
        );
    }

    private static Event _parseEvent(JsonObject event) {
        final JsonArray inputs = getArray(event, INPUTS);
        if (inputs != null) {
            final int inputsLen = inputs.size();
            final ABIType<?>[] types = new ABIType<?>[inputsLen];
            final boolean[] indexed = new boolean[inputsLen];
            for (int i = 0; i < inputsLen; i++) {
                JsonObject inputObj = inputs.get(i).getAsJsonObject();
                types[i] = parseType(inputObj);
                indexed[i] = getBoolean(inputObj, INDEXED);
            }
            return new Event(
                    getName(event),
                    getBoolean(event, ANONYMOUS, false),
                    TupleType.wrap(types),
                    indexed
            );
        }
        throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
    }

    private static ContractError _parseError(JsonObject error) {
        return new ContractError(getName(error), parseTupleType(error, INPUTS));
    }

    private static TupleType parseTupleType(JsonObject parent, String arrayName) {
        JsonArray array = getArray(parent, arrayName);
        final int size;
        if (array == null || (size = array.size()) <= 0) { /* JsonArray.isEmpty requires gson v2.8.7 */
            return TupleType.EMPTY;
        }
        final ABIType<?>[] elements = new ABIType[size];
        for(int i = 0; i < size; i++) {
            elements[i] = parseType(array.get(i).getAsJsonObject());
        }
        return TupleType.wrap(elements);
    }

    private static ABIType<?> parseType(JsonObject object) {
        final String type = getType(object);
        final String name = getName(object);
        if(type.startsWith(TUPLE)) {
            TupleType baseType = parseTupleType(object, COMPONENTS);
            return TypeFactory.build(
                    baseType.canonicalType + type.substring(TUPLE.length()), // + suffix e.g. "[4][]"
                    baseType,
                    name);
        }
        return TypeFactory.build(type, null, name);
    }

    private static String getType(JsonObject obj) {
        return getString(obj, TYPE);
    }

    private static String getName(JsonObject obj) {
        return getString(obj, NAME);
    }
// ---------------------------------------------------------------------------------------------------------------------
    static String toJson(ABIObject o, boolean pretty) {
        try {
            CharArrayWriter stringOut = new CharArrayWriter(256);
            JsonWriter out = new JsonWriter(stringOut);
            if (pretty) {
                out.setIndent("  ");
            }
            out.beginObject();
            if(o.isFunction()) {
                final Function f = o.asFunction();
                final TypeEnum t = f.getType();
                type(out, t.toString());
                if (t != TypeEnum.FALLBACK) {
                    name(out, o.getName());
                    if (t != TypeEnum.RECEIVE) {
                        tupleType(out, INPUTS, o.getInputs(), null);
                        if (t != TypeEnum.CONSTRUCTOR) {
                            tupleType(out, OUTPUTS, f.getOutputs(), null);
                        }
                    }
                }
                stateMutability(out, f.getStateMutability());
            } else if (o.isEvent()) {
                Event e = o.asEvent();
                type(out, EVENT);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), e.getIndexManifest());
            } else {
                type(out, ERROR);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), null);
            }
            out.endObject();
            return stringOut.toString();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    private static void type(JsonWriter out, String type) throws IOException {
        out.name(TYPE).value(type);
    }

    private static void name(JsonWriter out, String name) throws IOException {
        if(name != null) {
            out.name(NAME).value(name);
        }
    }

    private static void stateMutability(JsonWriter out, String stateMutability) throws IOException {
        if(stateMutability != null) {
            out.name(STATE_MUTABILITY).value(stateMutability);
        }
        out.name(CONSTANT).value(VIEW.equals(stateMutability) || PURE.equals(stateMutability));
    }

    private static void tupleType(JsonWriter out, String name, TupleType tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        int i = 0;
        for (ABIType<?> e : tupleType.elementTypes) {
            final String type = e.canonicalType;
            final boolean tupleBase = type.charAt(0) == '(';
            out.beginObject();
            name(out, e.getName());
            type(out, tupleBase ? type.replace(type.substring(0, type.lastIndexOf(')') + 1), TUPLE) : type);
            if(tupleBase) {
                tupleType(out, COMPONENTS, (TupleType) ArrayType.baseType(e), null);
            }
            if(indexedManifest != null) {
                out.name(INDEXED).value(indexedManifest[i]);
            }
            out.endObject();
            i++;
        }
        out.endArray();
    }
}
