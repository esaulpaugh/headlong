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
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    private static final String INTERNAL_TYPE = "internalType";

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

    public static List<Event<Tuple>> parseEvents(String arrayJson) {
        return parseElements(arrayJson, EVENTS);
    }

    public static List<ContractError<Tuple>> parseErrors(String arrayJson) {
        return parseElements(arrayJson, ERRORS);
    }

    public static List<ABIObject> parseElements(String arrayJson) {
        return parseElements(arrayJson, ALL);
    }

    public static Stream<ABIObject> stream(String arrayJson) {
        return stream(ABIType.FLAGS_NONE, arrayJson, ABIJSON.ALL);
    }

    /** For single-threaded use only. */
    public static <T extends ABIObject> Stream<T> stream(int flags, String arrayJson, Set<TypeEnum> types) {
        return StreamSupport.stream(new JsonSpliterator<>(flags, arrayJson, types), false);
    }

    public static <T extends ABIObject> List<T> parseElements(String arrayJson, Set<TypeEnum> types) {
        return parseElements(ABIType.FLAGS_NONE, arrayJson, types);
    }

    public static <T extends ABIObject> List<T> parseElements(int flags, String arrayJson, Set<TypeEnum> types) {
        final JsonArray arr = parseArray(arrayJson);
        final List<T> selected = new ArrayList<>(arr.size());
        final Iterator<T> iter = iterator(flags, arr, types);
        while (iter.hasNext()) {
            selected.add(iter.next());
        }
        return selected;
    }

    /** Iterators are not thread-safe. */
    public static <T extends ABIObject> Iterator<T> iterator(int flags, String arrayJson, Set<TypeEnum> types) {
        return iterator(flags, parseArray(arrayJson), types);
    }

    static <T extends ABIObject> Iterator<T> iterator(int flags, JsonArray arr, Set<TypeEnum> types) {
        final Iterator<JsonElement> jsonIter = arr.iterator();
        final MessageDigest digest = Function.newDefaultDigest();
        return new Iterator<T>() {

            T next;

            @Override
            public boolean hasNext() {
                if (this.next != null) {
                    return true;
                }
                while (jsonIter.hasNext()) {
                    final JsonObject object = (JsonObject) jsonIter.next();
                    final TypeEnum t = TypeEnum.parse(getType(object));
                    if (types.contains(t)) {
                        this.next = parseABIObject(t, object, digest, flags);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                if (hasNext()) {
                    final T curr = this.next;
                    this.next = null;
                    return curr;
                }
                throw new NoSuchElementException();
            }
        };
    }

    /** @see ABIObject#fromJsonObject(int,JsonObject) */
    static <T extends ABIObject> T parseABIObject(JsonObject object, int flags) {
        final TypeEnum t = TypeEnum.parse(getType(object));
        return parseABIObject(t, object, t.isFunction ? Function.newDefaultDigest() : null, flags);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ABIObject> T parseABIObject(TypeEnum t, JsonObject object, MessageDigest digest, int flags) {
        switch (t.ordinal()) {
        case TypeEnum.ORDINAL_FUNCTION:
        case TypeEnum.ORDINAL_RECEIVE:
        case TypeEnum.ORDINAL_FALLBACK:
        case TypeEnum.ORDINAL_CONSTRUCTOR: return (T) parseFunctionUnchecked(t, object, digest, flags);
        case TypeEnum.ORDINAL_EVENT: return (T) parseEventUnchecked(object, flags);
        case TypeEnum.ORDINAL_ERROR: return (T) parseErrorUnchecked(object, flags);
        default: throw new AssertionError();
        }
    }
// ---------------------------------------------------------------------------------------------------------------------
    static Function parseFunction(JsonObject function, MessageDigest digest, int flags) {
        final TypeEnum t = TypeEnum.parse(getType(function));
        if (!FUNCTIONS.contains(t)) {
            throw TypeEnum.unexpectedType(getType(function));
        }
        return parseFunctionUnchecked(t, function, digest, flags);
    }

    @SuppressWarnings("unchecked")
    static <X extends Tuple> Event<X> parseEvent(JsonObject event, int flags) {
        if (!EVENT.equals(getType(event))) {
            throw TypeEnum.unexpectedType(getType(event));
        }
        return (Event<X>) parseEventUnchecked(event, flags);
    }

    @SuppressWarnings("unchecked")
    static <X extends Tuple> ContractError<X> parseError(JsonObject error, int flags) {
        if (!ERROR.equals(getType(error))) {
            throw TypeEnum.unexpectedType(getType(error));
        }
        return (ContractError<X>) parseErrorUnchecked(error, flags);
    }

    private static Function parseFunctionUnchecked(TypeEnum type, JsonObject function, MessageDigest digest, int flags) {
        return new Function(
                type,
                getName(function),
                parseTupleType(function, INPUTS, flags),
                parseTupleType(function, OUTPUTS, flags),
                getString(function, STATE_MUTABILITY),
                digest
        );
    }

    private static Event<?> parseEventUnchecked(JsonObject event, int flags) {
        final JsonArray inputs = event.getAsJsonArray(INPUTS);
        if (inputs != null) {
            final boolean[] indexed = new boolean[inputs.size()];
            return new Event<>(
                    getName(event),
                    getAnonymous(event),
                    parseTupleType(inputs, indexed, flags),
                    indexed
            );
        }
        throw new IllegalArgumentException("array \"" + INPUTS + "\" null or not found");
    }

    private static ContractError<?> parseErrorUnchecked(JsonObject error, int flags) {
        return new ContractError<>(getName(error), parseTupleType(error, INPUTS, flags));
    }

    private static TupleType<Tuple> parseTupleType(final JsonArray array, final boolean[] indexed, final int flags) {
        final int size;
        if (array == null || (size = array.size()) == 0) { /* JsonArray.isEmpty requires gson v2.8.7 */
            return TupleType.empty(flags);
        }
        final ABIType<?>[] elements = new ABIType<?>[size];
        final String[] names = new String[size];
        final String[] internalTypes = new String[size];
        final StringBuilder canonicalType = TypeFactory.newTypeBuilder();
        boolean dynamic = false;
        for (int i = 0; ; ) {
            final JsonObject inputObj = array.get(i).getAsJsonObject();
            final ABIType<?> e = parseType(inputObj, flags);
            canonicalType.append(e.canonicalType);
            dynamic |= e.dynamic;
            elements[i] = e;
            names[i] = getName(inputObj);
            final String internalType = getString(inputObj, INTERNAL_TYPE);
            if (internalType != null) {
                internalTypes[i] = internalType.equals(e.canonicalType) ? e.canonicalType : internalType;
            }
            if (indexed != null) {
                indexed[i] = getIndexed(inputObj);
            }
            if (++i == size) {
                return new TupleType<>(
                        canonicalType.append(')').toString(),
                        dynamic,
                        elements,
                        names,
                        internalTypes,
                        flags
                );
            }
            canonicalType.append(',');
        }
    }

    private static TupleType<?> parseTupleType(JsonObject object, String arrayKey, int flags) {
        return parseTupleType(object.getAsJsonArray(arrayKey), null, flags);
    }

    private static ABIType<?> parseType(JsonObject object, int flags) {
        final String type = getType(object);
        if (type.startsWith(TUPLE)) {
            final TupleType<?> tt = parseTupleType(object, COMPONENTS, flags);
            return type.length() > TUPLE.length()
                    ? TypeFactory.build(tt.canonicalType + type.substring(TUPLE.length()), null, tt, flags) // tuple array
                    : tt;
        }
        return TypeFactory.create(flags, type);
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
            final Writer stringOut = new NonSyncWriter(pretty ? 512 : 256); // can also use StringWriter or CharArrayWriter, but this is faster
            final JsonWriter out = new JsonWriter(stringOut);
            if (pretty) {
                out.setIndent("  ");
            }
            out.beginObject();
            if (o.isFunction()) {
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
                final Event<?> e = o.asEvent();
                type(out, EVENT);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), e.getIndexManifest());
                out.name(ANONYMOUS).value(e.isAnonymous());
            } else {
                type(out, ERROR);
                name(out, o.getName());
                tupleType(out, INPUTS, o.getInputs(), null);
            }
            out.endObject()
                    .close();
            return stringOut.toString();
        } catch (IOException io) {
            throw new JsonIOException(io);
        }
    }

    private static void type(JsonWriter out, String type) throws IOException {
        out.name(TYPE).value(type);
    }

    private static void name(JsonWriter out, String name) throws IOException {
        if (name != null) {
            out.name(NAME).value(name);
        }
    }

    private static void internalType(JsonWriter out, String internalType) throws IOException {
        if (internalType != null) {
            out.name(INTERNAL_TYPE).value(internalType);
        }
    }

    private static void stateMutability(JsonWriter out, String stateMutability) throws IOException {
        if (stateMutability != null) {
            out.name(STATE_MUTABILITY).value(stateMutability);
        }
    }

    private static void tupleType(JsonWriter out, String name, TupleType<?> tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        int i = 0;
        for (final ABIType<?> e : tupleType) {
            out.beginObject();
            if (tupleType.elementInternalTypes != null) {
                internalType(out, tupleType.elementInternalTypes[i]);
            }
            if (tupleType.elementNames != null) {
                name(out, tupleType.elementNames[i]);
            }
            final String type = e.canonicalType;
            if (type.charAt(0) == '(') {
                type(out, TUPLE + type.substring(type.lastIndexOf(')') + 1));
                tupleType(out, COMPONENTS, ArrayType.baseType(e), null);
            } else {
                type(out, type);
            }
            if (indexedManifest != null) {
                out.name(INDEXED).value(indexedManifest[i]);
            }
            out.endObject();
            i++;
        }
        out.endArray();
    }

    private static class NonSyncWriter extends CharArrayWriter {

        NonSyncWriter(int initialLen) {
            super(initialLen);
        }

        @Override
        public void write(int c) {
            if (count >= buf.length) {
                this.buf = Arrays.copyOf(buf, buf.length << 1); // expects buffer.length to be non-zero
            }
            this.buf[count++] = (char) c;
        }

        @Override
        public void write(final String str, int off, final int len) {
            int i = count;
            final int newCount = i + len;
            if (newCount > buf.length) {
                final char[] chars = Arrays.copyOf(buf, Math.max(newCount, buf.length << 1));
                while (i < newCount) {
                    chars[i++] = str.charAt(off++);
                }
                this.buf = chars;
            } else {
                final char[] chars = buf;
                while (i < newCount) {
                    chars[i++] = str.charAt(off++);
                }
            }
            this.count = newCount;
        }

        @Override
        public String toString() {
            return new String(buf, 0, count);
        }
    }
//-------------------------------------------------------------------------------------
    private static JsonElement parseElement(String json) {
        return Streams.parse(new JsonReader(new StringReader(json)));
    }

    static JsonObject parseObject(String json) {
        return parseElement(json).getAsJsonObject();
    }

    static JsonArray parseArray(String json) {
        return parseElement(json).getAsJsonArray();
    }

    private static String getString(JsonObject object, String key) {
        final JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (((JsonPrimitive) element).isString()) {
            return element.getAsString();
        }
        throw new IllegalArgumentException(key + " is not a string");
    }

    private static boolean getAnonymous(JsonObject object) {
        final JsonElement element = object.get(ANONYMOUS);
        if (element == null) {
            return false;
        }
        if (((JsonPrimitive) element).isBoolean()) {
            return element.getAsBoolean();
        }
        throw new IllegalArgumentException(ANONYMOUS + " is not a boolean");
    }

    private static boolean getIndexed(JsonObject object) {
        final JsonElement element = object.get(INDEXED);
        if (((JsonPrimitive) element).isBoolean()) {
            return element.getAsBoolean();
        }
        throw new IllegalArgumentException(INDEXED + " is not a boolean");
    }

    static class JsonSpliterator<T extends ABIObject> implements Spliterator<T> {

        private final JsonReader jsonReader;
        private final int flags;
        private final Set<TypeEnum> types;
        private final MessageDigest digest = Function.newDefaultDigest();

        JsonSpliterator(int flags, String arrayJson, Set<TypeEnum> types) {
            JsonReader reader = new JsonReader(new StringReader(arrayJson));
            try {
                reader.beginArray();
            } catch (IOException io) {
                throw new IllegalArgumentException("not a JSON array");
            }
            this.flags = flags;
            this.jsonReader = reader;
            this.types = types;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                while (jsonReader.peek() != JsonToken.END_ARRAY) {
                    JsonObject object = readJsonObject(jsonReader);
                    TypeEnum t = TypeEnum.parse(getType(object));
                    if (types.contains(t)) {
                        action.accept(parseABIObject(t, object, digest, flags));
                        return true;
                    }
                }
                return false;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL | IMMUTABLE;
        }
    }

    private static JsonObject readJsonObject(JsonReader reader) throws IOException {
        JsonObject jsonObject = new JsonObject();
        reader.beginObject();
        while (reader.hasNext()) {
            jsonObject.add(reader.nextName(), readElement(reader));
        }
        reader.endObject();
        return jsonObject;
    }

    private static JsonArray readJsonArray(JsonReader reader) throws IOException {
        JsonArray jsonArray = new JsonArray();
        reader.beginArray();
        while (reader.hasNext()) {
            jsonArray.add(readElement(reader));
        }
        reader.endArray();
        return jsonArray;
    }

    private static JsonElement readElement(JsonReader reader) throws IOException {
        switch (reader.peek()) {
        case STRING: return new JsonPrimitive(reader.nextString());
        case NUMBER: return new JsonPrimitive(reader.nextDouble());
        case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
        case BEGIN_OBJECT: return readJsonObject(reader);
        case BEGIN_ARRAY: return readJsonArray(reader);
        default: // NULL and unknown
            reader.skipValue();
            return JsonNull.INSTANCE;
        }
    }
}
