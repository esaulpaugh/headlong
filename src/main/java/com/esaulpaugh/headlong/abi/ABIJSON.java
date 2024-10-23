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

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.CharArrayWriter;
import java.io.Closeable;
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
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
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
        return stream(arrayJson, ABIJSON.ALL);
    }

    public static <T extends ABIObject> List<T> parseElements(String arrayJson, Set<TypeEnum> types) {
        return parseElements(ABIType.FLAGS_NONE, arrayJson, types);
    }

    public static <T extends ABIObject> List<T> parseElements(int flags, String arrayJson, Set<TypeEnum> types) {
        try (final JsonReader reader = read(arrayJson)) {
            return parseArray(reader, types, flags);
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
    }

    public static <T extends ABIObject> Stream<T> stream(String arrayJson, Set<TypeEnum> types) {
        return stream(ABIType.FLAGS_NONE, arrayJson, types);
    }

    /** For single-threaded use only. */
    public static <T extends ABIObject> Stream<T> stream(int flags, String arrayJson, Set<TypeEnum> types) {
        final JsonSpliterator<T> spliterator = new JsonSpliterator<>(arrayJson, types, flags);
        return StreamSupport.stream(spliterator, false)
                    .onClose(spliterator::close);
    }

    /** Iterators are not thread-safe. */
    public static <T extends ABIObject> Iterator<T> iterator(int flags, String arrayJson, Set<TypeEnum> types) {
        return ABIJSON.<T>stream(flags, arrayJson, types).iterator();
    }
//----------------------------------------------------------------------------------------------------------------------
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
                String stateMutability = f.getStateMutability();
                if (stateMutability != null) {
                    out.name(STATE_MUTABILITY).value(stateMutability);
                }
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

    private static void tupleType(JsonWriter out, String name, TupleType<?> tupleType, boolean[] indexedManifest) throws IOException {
        out.name(name).beginArray();
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
            final ABIType<?> e = tupleType.elementTypes[i];
            out.beginObject();
            if (tupleType.elementInternalTypes != null) {
                String internalType = tupleType.elementInternalTypes[i];
                if (internalType != null) {
                    out.name(INTERNAL_TYPE).value(internalType);
                }
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
            int i = this.count;
            char[] buf = this.buf;
            final int newCount = i + len;
            if (newCount > buf.length) {
                this.buf = buf = Arrays.copyOf(buf, Math.max(newCount, buf.length << 1));
            }
            while (i < newCount) {
                buf[i++] = str.charAt(off++);
            }
            this.count = newCount;
        }

        @Override
        public String toString() {
            return new String(buf, 0, count);
        }
    }
//----------------------------------------------------------------------------------------------------------------------
    static class JsonSpliterator<T extends ABIObject> extends Spliterators.AbstractSpliterator<T> implements Closeable {

        private final JsonReader jsonReader;
        private final Set<TypeEnum> types;
        private final MessageDigest digest;
        private final int flags;
        boolean closed = false;

        JsonSpliterator(String arrayJson, Set<TypeEnum> types, int flags) {
            super(Long.SIZE, ORDERED | NONNULL | IMMUTABLE);
            try {
                JsonReader reader = read(arrayJson);
                reader.beginArray();
                this.jsonReader = reader;
                this.types = types;
                this.digest = Function.newDefaultDigest();
                this.flags = flags;
            } catch (IOException io) {
                throw new IllegalStateException(io);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                if (!closed) {
                    while (jsonReader.peek() != JsonToken.END_ARRAY) {
                        T e = tryParseStreaming(jsonReader, types, digest, flags);
                        if (e != null) {
                            action.accept(e);
                            return true;
                        }
                    }
                    doClose();
                }
                return false;
            } catch (IOException io) {
                throw new IllegalStateException(io);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public void close() {
            if (!closed) {
                try {
                    doClose();
                } catch (IOException io) {
                    throw new IllegalStateException(io);
                }
            }
        }

        private void doClose() throws IOException {
            try {
                jsonReader.close();
            } finally {
                closed = true;
            }
        }
    }

    static <T extends ABIObject> T parseABIObject(String json, Set<TypeEnum> types, MessageDigest digest, int flags) {
        try {
            T obj = tryParseStreaming(read(json), types, digest, flags);
            if (obj != null) {
                return obj;
            }
            throw new IllegalArgumentException("unexpected type");
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
    }

    static <T extends ABIObject> T tryParseStreaming(JsonReader reader, Set<TypeEnum> types, MessageDigest digest, int flags) throws IOException {
        reader.beginObject();
        JsonObject jsonObject = null;
        TypeEnum t = TypeEnum.FUNCTION;
        while (reader.peek() != JsonToken.END_OBJECT) {
            String name = reader.nextName();
            if (TYPE.equals(name)) {
                t = TypeEnum.parse(reader.nextString());
                if (!types.contains(t)) {
                    while (reader.peek() != JsonToken.END_OBJECT) {
                        reader.skipValue();
                    }
                    reader.endObject();
                    return null;
                }
                if (jsonObject == null) {
                    return finishParse(t, reader, digest, flags);
                }
            } else {
                if (jsonObject == null) {
                    jsonObject = new JsonObject();
                }
                jsonObject.add(name, TypeAdapters.JSON_ELEMENT.read(reader));
            }
        }
        reader.endObject();
        JsonReader r = read(jsonObject.toString());
        r.beginObject();
        return finishParse(t, r, digest, flags);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ABIObject> T finishParse(TypeEnum t, JsonReader reader, MessageDigest digest, int flags) throws IOException {
        switch (t.ordinal()) {
        case TypeEnum.ORDINAL_FUNCTION:
        case TypeEnum.ORDINAL_RECEIVE:
        case TypeEnum.ORDINAL_FALLBACK:
        case TypeEnum.ORDINAL_CONSTRUCTOR: return (T) parseFunction(t, reader, digest, flags);
        case TypeEnum.ORDINAL_EVENT: return (T) parseEvent(reader, flags);
        case TypeEnum.ORDINAL_ERROR: return (T) parseError(reader, flags);
        default: throw new AssertionError();
        }
    }

    private static Function parseFunction(TypeEnum t, JsonReader reader, MessageDigest digest, int flags) throws IOException {
        String name = null;
        TupleType<?> inputs = TupleType.EMPTY;
        TupleType<?> outputs = TupleType.EMPTY;
        String stateMutability = null;
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
            case NAME: name = reader.nextString(); continue;
            case INPUTS: inputs = parseTupleType(reader, false, flags); continue;
            case OUTPUTS: outputs = parseTupleType(reader, false, flags); continue;
            case STATE_MUTABILITY: stateMutability = reader.nextString(); continue;
            default: reader.skipValue();
            }
        }
        reader.endObject();
        return new Function(t, name, inputs, outputs, stateMutability, digest);
    }

    private static Event<?> parseEvent(JsonReader reader, int flags) throws IOException {
        String name = null;
        boolean anonymous = false;
        TupleType<?> tt = null;
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
            case NAME: name = reader.nextString(); continue;
            case ANONYMOUS: anonymous = reader.nextBoolean(); continue;
            case INPUTS: tt = parseTupleType(reader, true, flags); continue;
            default: reader.skipValue();
            }
        }
        reader.endObject();
        return new Event<>(
                name,
                anonymous,
                tt,
                tt.indexed
        );
    }

    private static ContractError<?> parseError(JsonReader reader, int flags) throws IOException {
        String name = null;
        TupleType<?> tt = null;
        while (reader.peek() != JsonToken.END_OBJECT) {
            switch (reader.nextName()) {
            case NAME: name = reader.nextString(); continue;
            case INPUTS: tt = parseTupleType(reader, false, flags); continue;
            default: reader.skipValue();
            }
        }
        reader.endObject();
        return new ContractError<>(name, tt);
    }

    public static TupleType<?> parseTupleType(JsonReader reader, final boolean eventParams, final int flags) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return TupleType.empty(flags);
        }

        reader.beginArray();
        if (reader.peek() == JsonToken.END_ARRAY) {
            reader.endArray();
            return TupleType.empty(flags);
        }

        ABIType<?>[] elements = new ABIType<?>[8];
        String[] names = new String[8];
        String[] internalTypes = new String[8];
        boolean[] indexed = eventParams ? new boolean[8] : null;
        StringBuilder canonicalType = TupleType.newTypeBuilder();
        boolean dynamic = false;

        for (int i = 0; true; canonicalType.append(',')) {
            String name = null;
            String internalType = null;
            boolean isIndexed = false;
            String type = null;
            ABIType<?> e = null;

            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                switch (reader.nextName()) {
                case TYPE: type = reader.nextString(); continue;
                case COMPONENTS: e = parseTupleType(reader, false, flags); continue;
                case NAME: name = reader.nextString(); continue;
                case INTERNAL_TYPE: internalType = reader.nextString(); continue;
                case INDEXED: isIndexed = reader.nextBoolean(); continue;
                default: reader.skipValue();
                }
            }
            reader.endObject();

            if (e == null || !type.startsWith(TUPLE)) {
                e = TypeFactory.create(flags, type);
            } else {
                e = type.length() == TUPLE.length()
                        ? e
                        : TypeFactory.build(e.canonicalType + type.substring(TUPLE.length()), null, e.asTupleType(), flags); // tuple array
            }

            canonicalType.append(e.canonicalType);
            dynamic |= e.dynamic;

            elements[i] = e;
            names[i] = name;
            if (internalType != null) {
                internalTypes[i] = internalType.equals(e.canonicalType) ? e.canonicalType : internalType;
            }
            if (eventParams) {
                indexed[i] = isIndexed;
            }

            i++;
            if (reader.peek() == JsonToken.END_ARRAY) {
                reader.endArray();
                return new TupleType<>(
                        canonicalType.append(')').toString(),
                        dynamic,
                        Arrays.copyOf(elements, i),
                        Arrays.copyOf(names, i),
                        Arrays.copyOf(internalTypes, i),
                        eventParams ? Arrays.copyOf(indexed, i) : null,
                        flags
                );
            }

            if (i == elements.length) {
                final int newLen = i << 1;
                elements = Arrays.copyOf(elements, newLen);
                names = Arrays.copyOf(names, newLen);
                internalTypes = Arrays.copyOf(internalTypes, newLen);
                indexed = eventParams ? Arrays.copyOf(indexed, newLen) : null;
            }
        }
    }

    private static JsonReader read(String json) {
        return new JsonReader(new StringReader(json));
    }

    public static <T extends ABIObject> List<T> parseABIField(int flags, String objectJson, Set<TypeEnum> types) {
        try (final JsonReader reader = read(objectJson)) {
            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                if ("abi".equals(reader.nextName())) {
                    return parseArray(reader, types, flags);
                }
                reader.skipValue();
            }
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
        throw new IllegalStateException("abi key not found");
    }

    private static <T extends ABIObject> List<T> parseArray(final JsonReader reader, Set<TypeEnum> types, int flags) throws IOException {
        final List<T> list = new ArrayList<>();
        reader.beginArray();
        final MessageDigest digest = Function.newDefaultDigest();
        while (reader.peek() != JsonToken.END_ARRAY) {
            T e = tryParseStreaming(reader, types, digest, flags);
            if (e != null) {
                list.add(e);
            }
        }
        reader.endArray();
        return list;
    }
}
