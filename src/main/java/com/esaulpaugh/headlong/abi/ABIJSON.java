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

    private static final Gson GSON;
    private static final Gson GSON_PRETTY;

    static {
        GsonBuilder builder = new GsonBuilder();
        GSON = builder.create();
        GSON_PRETTY = builder.setPrettyPrinting().create();
    }

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
        String type = getString(event, TYPE);
        if(isEvent(type)) {
            return _parseEvent(event);
        }
        throw TypeEnum.unexpectedType(type);
    }

    public static ContractError parseError(JsonObject error) {
        String type = getString(error, TYPE);
        if(isError(type)) {
            return _parseError(error);
        }
        throw TypeEnum.unexpectedType(type);
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
        final List<T> abiObjects = new ArrayList<>();
        for (JsonElement e : parseArray(arrayJson)) {
            if (e.isJsonObject()) {
                ABIObject o = parseABIObject(e.getAsJsonObject());
                boolean add;
                if(o.getType() == TypeEnum.EVENT) {
                    add = (flags & EVENTS) != 0;
                } else if(o.getType() == TypeEnum.ERROR) {
                    add = (flags & ERRORS) != 0;
                } else {
                    add = (flags & FUNCTIONS) != 0;
                }
                if(add) {
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
                parseTypes(getArray(function, INPUTS)),
                parseTypes(getArray(function, OUTPUTS)),
                getString(function, STATE_MUTABILITY),
                digest
        );
    }

    private static Event _parseEvent(JsonObject event) {
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

    private static ContractError _parseError(JsonObject error) {
        return new ContractError(getString(error, NAME), parseTypes(getArray(error, INPUTS)));
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
    static String toJson(ABIObject x, int flags, boolean pretty) {
        try {
            StringWriter stringOut = new StringWriter();
            JsonWriter out = (pretty ? GSON_PRETTY : GSON).newJsonWriter(stringOut);
            out.beginObject();
            if((flags & FUNCTIONS) != 0) {
                Function f = (Function) x;
                final TypeEnum type = f.getType();
                out.name(TYPE).value(type.toString());
                if (type != TypeEnum.FALLBACK) {
                    addIfValueNotNull(out, NAME, x.getName());
                    if (type != TypeEnum.RECEIVE) {
                        writeJsonArray(out, INPUTS, x.getInputs(), null);
                        if (type != TypeEnum.CONSTRUCTOR) {
                            writeJsonArray(out, OUTPUTS, f.getOutputs(), null);
                        }
                    }
                }
                final String stateMutability = f.getStateMutability();
                addIfValueNotNull(out, STATE_MUTABILITY, stateMutability);
                out.name(CONSTANT).value(VIEW.equals(stateMutability) || PURE.equals(stateMutability));
            } else if ((flags & EVENTS) != 0) {
                Event e = (Event) x;
                out.name(TYPE).value(EVENT);
                addIfValueNotNull(out, NAME, x.getName());
                writeJsonArray(out, INPUTS, x.getInputs(), e.getIndexManifest());
            } else {
                out.name(TYPE).value(ERROR);
                addIfValueNotNull(out, NAME, x.getName());
                writeJsonArray(out, INPUTS, x.getInputs(), null);
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
            final ABIType<?> e = tupleType.get(i);
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
