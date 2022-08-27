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
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.util.JsonUtils.getArray;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getBoolean;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.getString;
import static com.esaulpaugh.headlong.abi.util.JsonUtils.parseArray;

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
    static final String PAYABLE = "payable"; // to mark as nonpayable, do not specify any stateMutability

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

    public static <T extends ABIObject> List<T> parseElements(String arrayJson, Set<TypeEnum> types) {
        final List<T> selected = new ArrayList<>();
        final MessageDigest digest = Function.newDefaultDigest();
        for (final JsonElement e : parseArray(arrayJson)) {
            if (e.isJsonObject()) {
                final JsonObject object = e.getAsJsonObject();
                final TypeEnum t = TypeEnum.parse(getType(object));
                if(types.contains(t)) {
                    selected.add(parseABIObject(t, object, () -> digest));
                }
            }
        }
        return selected;
    }

    /** @see ABIObject#fromJsonObject(JsonObject) */
    static <T extends ABIObject> T parseABIObject(JsonObject object) {
        return parseABIObject(TypeEnum.parse(getType(object)), object, Function::newDefaultDigest);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ABIObject> T parseABIObject(TypeEnum t, JsonObject object, Supplier<MessageDigest> digest) {
        switch (t.ordinal()) {
        case TypeEnum.ORDINAL_FUNCTION:
        case TypeEnum.ORDINAL_RECEIVE:
        case TypeEnum.ORDINAL_FALLBACK:
        case TypeEnum.ORDINAL_CONSTRUCTOR: return (T) parseFunctionUnchecked(t, object, digest.get());
        case TypeEnum.ORDINAL_EVENT: return (T) parseEventUnchecked(object);
        case TypeEnum.ORDINAL_ERROR: return (T) parseErrorUnchecked(object);
        default: throw new AssertionError();
        }
    }
// ---------------------------------------------------------------------------------------------------------------------
    static Function parseFunction(JsonObject function, MessageDigest digest) {
        final TypeEnum t = TypeEnum.parse(getType(function));
        if (FUNCTIONS.contains(t)) {
            return parseFunctionUnchecked(t, function, digest);
        }
        throw TypeEnum.unexpectedType(getType(function));
    }

    static Event parseEvent(JsonObject event) {
        if(!EVENT.equals(getType(event))) {
            throw TypeEnum.unexpectedType(getType(event));
        }
        return parseEventUnchecked(event);
    }

    static ContractError parseError(JsonObject error) {
        if(!ERROR.equals(getType(error))) {
            throw TypeEnum.unexpectedType(getType(error));
        }
        return parseErrorUnchecked(error);
    }

    private static Function parseFunctionUnchecked(TypeEnum type, JsonObject function, MessageDigest digest) {
        return new Function(
                type,
                getName(function),
                parseTupleType(function, INPUTS),
                parseTupleType(function, OUTPUTS),
                getString(function, STATE_MUTABILITY),
                digest
        );
    }

    private static Event parseEventUnchecked(JsonObject event) {
        final JsonArray inputs = getArray(event, INPUTS);
        if (inputs != null) {
            final boolean[] indexed = new boolean[inputs.size()];
            return new Event(
                    getName(event),
                    getBoolean(event, ANONYMOUS, false),
                    parseTupleType(inputs, indexed),
                    indexed
            );
        }
        throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
    }

    private static TupleType parseTupleType(final JsonArray array, final boolean[] indexed) {
        int size;
        if (array == null || (size = array.size()) <= 0) { /* JsonArray.isEmpty requires gson v2.8.7 */
            return TupleType.EMPTY;
        }
        final ABIType<?>[] elements = new ABIType<?>[size];
        final String[] names = new String[size];
        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (int i = 0; i < elements.length; i++) {
            final JsonObject inputObj = array.get(i).getAsJsonObject();
            final ABIType<?> e = parseType(inputObj);
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
            elements[i] = e;
            names[i] = getName(inputObj);
            if(indexed != null) {
                indexed[i] = getBoolean(inputObj, INDEXED);
            }
        }
        return new TupleType(TupleType.completeTupleTypeString(canonicalBuilder), dynamic, names, elements);
    }

    private static ContractError parseErrorUnchecked(JsonObject error) {
        return new ContractError(getName(error), parseTupleType(error, INPUTS));
    }

    private static TupleType parseTupleType(JsonObject object, String arrayKey) {
        return parseTupleType(getArray(object, arrayKey), null);
    }

    private static ABIType<?> parseType(JsonObject object) {
        final String type = getType(object);
        if(type.startsWith(TUPLE)) {
            if(type.length() == TUPLE.length()) {
                return parseTupleType(object, COMPONENTS);
            }
            TupleType baseType = parseTupleType(object, COMPONENTS);
            return TypeFactory.build(baseType.canonicalType + type.substring(TUPLE.length()), null, baseType); // return ArrayType
        }
        return TypeFactory.create(type);
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
            final Writer stringOut = new NonSyncWriter(); // can also use StringWriter or CharArrayWriter, but this is faster
            final JsonWriter out = new JsonWriter(stringOut);
            if (pretty) {
                out.setIndent("  ");
            }
            out.beginObject();
            if(o.isFunction()) {
                final Function f = o.asFunction();
                final TypeEnum t = o.getType();
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
                type(out, EVENT);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), o.asEvent().getIndexManifest());
            } else {
                type(out, ERROR);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), null);
            }
            out.endObject()
                    .close();
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
    }

    private static void tupleType(JsonWriter out, String name, TupleType tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        int i = 0;
        for (final ABIType<?> e : tupleType.elementTypes) {
            out.beginObject();
            if(tupleType.elementNames != null) {
                name(out, tupleType.elementNames[i]);
            }
            final String type = e.canonicalType;
            if(type.charAt(0) == '(') {
                type(out, TUPLE + type.substring(type.lastIndexOf(')') + 1));
                tupleType(out, COMPONENTS, ArrayType.baseType(e), null);
            } else {
                type(out, type);
            }
            if(indexedManifest != null) {
                out.name(INDEXED).value(indexedManifest[i]);
            }
            out.endObject();
            i++;
        }
        out.endArray();
    }

    private static class NonSyncWriter extends CharArrayWriter {

        NonSyncWriter() {
            super(256); // must be > 0
        }

        @Override
        public void write(int c) {
            if (count >= buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1); // expects buf.length to be non-zero
            }
            buf[count++] = (char) c;
        }

        @Override
        public void write(String str, int off, int len) {
            int newCount = count + len;
            if (newCount > buf.length) {
                buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newCount));
            }
            str.getChars(off, off + len, buf, count);
            count = newCount;
        }

        @Override
        public NonSyncWriter append(CharSequence csq) {
            String str = csq.toString();
            write(str, 0, str.length());
            return this;
        }

        @Override
        public String toString() {
            return new String(buf, 0, count);
        }
    }
}
