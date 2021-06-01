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

    public static final int FUNCTIONS = 1;
    public static final int EVENTS = 2;
    public static final int ERRORS = 4;

    public static final int ALL = FUNCTIONS | EVENTS | ERRORS;

    private static final String NAME = "name";
    private static final String TYPE = "type";
    static final String EVENT = "event";
    static final String FUNCTION = "function";
    static final String RECEIVE = "receive";
    static final String FALLBACK = "fallback";
    static final String CONSTRUCTOR = "constructor";
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
        return _parseFunction(getString(function, TYPE), function, messageDigest);
    }

    public static Event parseEvent(String objectJson) {
        return parseEvent(parseObject(objectJson));
    }

    public static Event parseEvent(JsonObject event) {
        if(isEvent(getString(event, TYPE))) {
            return _parseEvent(event);
        }
        throw TypeEnum.unexpectedType(getString(event, TYPE));
    }

    public static ContractError parseError(JsonObject error) {
        if(isError(getString(error, TYPE))) {
            return _parseError(error);
        }
        throw TypeEnum.unexpectedType(getString(error, TYPE));
    }

    public static ABIObject parseABIObject(JsonObject object) {
        String type = getString(object, TYPE);
        return isEvent(type)
                ? _parseEvent(object)
                : isError(type)
                    ? parseError(object)
                    : _parseFunction(type, object, Function.newDefaultDigest());
    }

    public static List<Function> parseFunctions(String arrayJson) {
        return parseElements(arrayJson, FUNCTIONS, Function.class);
    }

    public static List<Event> parseEvents(String arrayJson) {
        return parseElements(arrayJson, EVENTS, Event.class);
    }

    public static List<ContractError> parseErrors(String arrayJson) {
        return parseElements(arrayJson, ERRORS, ContractError.class);
    }

    public static List<ABIObject> parseElements(String arrayJson) {
        return parseElements(arrayJson, ALL, ABIObject.class);
    }

    private static <T extends ABIObject> List<T> parseElements(String arrayJson, int flags, Class<T> classOfT) {
        final boolean functions = (flags & FUNCTIONS) != 0, events = (flags & EVENTS) != 0, errors = (flags & ERRORS) != 0;
        final List<T> abiObjects = new ArrayList<>();
        for (JsonElement e : parseArray(arrayJson)) {
            if (e.isJsonObject()) {
                ABIObject o = parseABIObject(e.getAsJsonObject());
                TypeEnum t = o.getType();
                if(t == TypeEnum.EVENT ? events : t == TypeEnum.ERROR ? errors : functions) {
                    abiObjects.add(classOfT.cast(o));
                }
            }
        }
        return abiObjects;
    }
// ---------------------------------------------------------------------------------------------------------------------
    private static boolean isEvent(String typeString) {
        return EVENT.equals(typeString);
    }

    private static boolean isError(String typeString) {
        return ERROR.equals(typeString);
    }

    private static Function _parseFunction(String type, JsonObject function, MessageDigest digest) {
        return new Function(
                TypeEnum.parse(type),
                getString(function, NAME),
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
            final List<ABIType<?>> typeList = new ArrayList<>(inputsLen);
            final boolean[] indexed = new boolean[inputsLen];
            for (int i = 0; i < inputsLen; i++) {
                JsonObject inputObj = inputs.get(i).getAsJsonObject();
                typeList.add(parseType(inputObj));
                indexed[i] = getBoolean(inputObj, INDEXED);
            }
            return new Event(
                    getString(event, NAME),
                    getBoolean(event, ANONYMOUS, false),
                    TupleType.wrap(typeList),
                    indexed
            );
        }
        throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
    }

    private static ContractError _parseError(JsonObject error) {
        return new ContractError(getString(error, NAME), parseTupleType(error, INPUTS));
    }

    private static TupleType parseTupleType(JsonObject parent, String arrayName) {
        JsonArray array = getArray(parent, arrayName);
        if (array != null) {
            final List<ABIType<?>> typeList = new ArrayList<>(array.size());
            for(JsonElement e : array) {
                typeList.add(parseType(e.getAsJsonObject()));
            }
            return TupleType.wrap(typeList);
        }
        return TupleType.EMPTY;
    }

    private static ABIType<?> parseType(JsonObject object) {
        final String typeStr = getString(object, TYPE);
        if(typeStr.startsWith(TUPLE)) {
            TupleType baseType = parseTupleType(object, COMPONENTS);
            return TypeFactory.build(
                    baseType.canonicalType + typeStr.substring(TUPLE.length()), // + suffix e.g. "[4][]"
                    baseType,
                    getString(object, NAME));
        }
        return TypeFactory.create(typeStr, Object.class, getString(object, NAME));
    }
// ---------------------------------------------------------------------------------------------------------------------
    static String toJson(ABIObject x, int flags, boolean pretty) {
        try {
            StringWriter stringOut = new StringWriter();
            JsonWriter out = new JsonWriter(stringOut);
            if (pretty) {
                out.setIndent("  ");
            }
            out.beginObject();
            if((flags & FUNCTIONS) != 0) {
                Function f = (Function) x;
                final TypeEnum type = f.getType();
                type(out, type.toString());
                if (type != TypeEnum.FALLBACK) {
                    name(out, x.getName());
                    if (type != TypeEnum.RECEIVE) {
                        tupleType(out, INPUTS, x.getInputs());
                        if (type != TypeEnum.CONSTRUCTOR) {
                            tupleType(out, OUTPUTS, f.getOutputs());
                        }
                    }
                }
                stateMutability(out, f.getStateMutability());
            } else if ((flags & EVENTS) != 0) {
                Event e = (Event) x;
                type(out, EVENT);
                name(out, x.getName());
                tupleType(out, INPUTS, x.getInputs(), e.getIndexManifest());
            } else {
                type(out, ERROR);
                name(out, x.getName());
                tupleType(out, INPUTS, x.getInputs());
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

    private static void tupleType(JsonWriter out, String name, TupleType tupleType) throws IOException {
        tupleType(out, name, tupleType, null);
    }

    private static void tupleType(JsonWriter out, String name, TupleType tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        int i = 0;
        for (ABIType<?> e : tupleType) {
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
