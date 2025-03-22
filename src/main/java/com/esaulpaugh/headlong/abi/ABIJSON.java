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

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** For parsing JSON representations of {@link ABIObject}s according to the ABI specification. */
public final class ABIJSON {

    private ABIJSON() {}

    static final EnumSet<TypeEnum> _ALL = EnumSet.allOf(TypeEnum.class);

    public static final Set<TypeEnum> ALL               = Collections.unmodifiableSet(_ALL);
    public static final Set<TypeEnum> FUNCTIONS         = Collections.unmodifiableSet(EnumSet.of(TypeEnum.FUNCTION, TypeEnum.RECEIVE, TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR));
    public static final Set<TypeEnum> NORMAL_FUNCTIONS  = Collections.unmodifiableSet(EnumSet.of(TypeEnum.FUNCTION));
    public static final Set<TypeEnum> EVENTS            = Collections.unmodifiableSet(EnumSet.of(TypeEnum.EVENT));
    public static final Set<TypeEnum> ERRORS            = Collections.unmodifiableSet(EnumSet.of(TypeEnum.ERROR));

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
        return parseElements(ABIType.FLAGS_NONE, arrayJson, NORMAL_FUNCTIONS, Function.newDefaultDigest());
    }

    /**
     * Selects all objects with type {@link TypeEnum#FUNCTION}, {@link TypeEnum#RECEIVE}, {@link TypeEnum#FALLBACK}, or
     * {@link TypeEnum#CONSTRUCTOR}.
     *
     * @param arrayJson the JSON array string
     * @return  the parsed {@link Function}s
     */
    public static List<Function> parseFunctions(String arrayJson) {
        return parseElements(ABIType.FLAGS_NONE, arrayJson, FUNCTIONS, Function.newDefaultDigest());
    }

    public static List<Event<Tuple>> parseEvents(String arrayJson) {
        return parseElements(ABIType.FLAGS_NONE, arrayJson, EVENTS, null);
    }

    public static List<ContractError<Tuple>> parseErrors(String arrayJson) {
        return parseElements(ABIType.FLAGS_NONE, arrayJson, ERRORS, null);
    }

    /** Allows a {@link MessageDigest} to be reused for multiple calls. See also {@link ABIParser}. */
    public static <T extends ABIObject> List<T> parseElements(int flags, String arrayJson, Set<TypeEnum> types, MessageDigest digest) {
        return parseArrayAndCloseReader(reader(arrayJson), types, flags, digest);
    }

    /**
     * Returns a minified version of the argument which is optimized for parsing. Accepts JSON array or JSON object.
     *
     * @param json  Contract ABI JSON array or Function/Event/ContractError JSON object
     * @return  optimized JSON
     */
    public static String optimize(String json) {
        try (JsonReader reader = reader(json)) {
            final JsonToken token = reader.peek();
            if (token == JsonToken.BEGIN_OBJECT) {
                return toJson(ABIObject.fromJson(json), false, true);
            } else if (token == JsonToken.BEGIN_ARRAY) {
                return optimize(parseArrayAndCloseReader(reader, ABIJSON.ALL, ABIType.FLAGS_NONE, Function.newDefaultDigest()));
            }
            throw new IllegalArgumentException("unexpected token: " + token);
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
    }

    /**
     * Returns a minified JSON array containing the given elements' encodings which is optimized for parsing.
     *
     * @param elements  the objects to be put into the array
     * @return  the JSON array
     * @param <T>   the common supertype of the elements
     */
    public static <T extends ABIObject> String optimize(List<T> elements) {
        return encode(true, elements);
    }

    /**
     * Returns a JSON array string containing the given elements' encodings.
     *
     * @param elements  the objects to be put into the array
     * @return  the JSON array
     * @param <T>   common supertype of the elements
     */
    public static <T extends ABIObject> String encode(List<T> elements) {
        return encode(false, elements);
    }
//----------------------------------------------------------------------------------------------------------------------
    private static <T extends ABIObject> String encode(boolean minify, List<T> elements) {
        final Writer stringOut = new NonSyncWriter(2048);
        try (JsonWriter out = new JsonWriter(stringOut)) {
            out.setIndent(minify ? "" : "  ");
            out.beginArray();
            for (ABIObject e : elements) {
                writeObject(e, out, minify);
            }
            out.endArray();
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
        return stringOut.toString();
    }

    static <T extends ABIObject> List<T> parseArrayAndCloseReader(final JsonReader reader, Set<TypeEnum> types, int flags, MessageDigest digest) {
        final List<T> list = new ArrayList<>();
        try (JsonReader ignored = reader) {
            reader.beginArray();
            while (reader.peek() != JsonToken.END_ARRAY) {
                T e = tryParseStreaming(reader, types, digest, flags);
                if (e != null) {
                    list.add(e);
                }
            }
            reader.endArray();
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
        return list;
    }

    static String toJson(ABIObject o, boolean pretty, boolean minify) {
        final Writer stringOut = new NonSyncWriter(pretty ? 512 : 256); // can also use StringWriter or CharArrayWriter, but this is faster
        try (JsonWriter out = new JsonWriter(stringOut)) {
            if (pretty) {
                out.setIndent("  ");
            }
            writeObject(o, out, minify);
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
        return stringOut.toString();
    }

    private static void writeObject(ABIObject o, JsonWriter out, boolean minify) throws IOException {
        out.beginObject();
        if (o.isFunction()) {
            final Function f = o.asFunction();
            final TypeEnum t = o.getType();
            type(out, t.toString()); // "type" key should be first, so streaming API can skip non-matching items quickly
            if (t != TypeEnum.FALLBACK) {
                name(out, o.getName());
                if (t != TypeEnum.RECEIVE) {
                    tupleType(out, INPUTS, o.getInputs(), null, minify);
                    if (t != TypeEnum.CONSTRUCTOR) {
                        tupleType(out, OUTPUTS, f.getOutputs(), null, minify);
                    }
                }
            }
            final String stateMutability = f.getStateMutability();
            if (stateMutability != null) {
                out.name(STATE_MUTABILITY).value(stateMutability);
            }
        } else if (o.isEvent()) {
            final Event<?> e = o.asEvent();
            type(out, EVENT);
            name(out, o.getName());
            tupleType(out, INPUTS, o.getInputs(), e.getIndexManifest(), minify);
            if (!minify || e.isAnonymous()) {
                out.name(ANONYMOUS).value(e.isAnonymous());
            }
        } else {
            type(out, ERROR);
            name(out, o.getName());
            tupleType(out, INPUTS, o.getInputs(), null, minify);
        }
        out.endObject();
    }

    private static void type(JsonWriter out, String type) throws IOException {
        out.name(TYPE).value(type);
    }

    private static void name(JsonWriter out, String name) throws IOException {
        if (name != null) {
            out.name(NAME).value(name);
        }
    }

    private static void tupleType(JsonWriter out, String name, TupleType<?> tupleType, boolean[] indexedManifest, boolean minify) throws IOException {
        if (minify && tupleType.isEmpty()) {
            return;
        }
        out.name(name).beginArray();
        for (int i = 0; i < tupleType.elementTypes.length; i++) {
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
            final ABIType<?> e = tupleType.elementTypes[i];
            final String type = e.canonicalType;
            if (type.charAt(0) == '(') {
                type(out, TUPLE + type.substring(type.lastIndexOf(')') + 1));
                tupleType(out, COMPONENTS, ArrayType.baseType(e), null, minify);
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
//----------------------------------------------------------------------------------------------------------------------
    static <T extends ABIObject> T parseABIObject(String json, Set<TypeEnum> types, MessageDigest digest, int flags) {
        return parseABIObjectAndCloseReader(reader(json), types, digest, flags);
    }

    /** Reads an {@link ABIObject} from JSON and closes the {@link InputStream}. Assumes UTF-8 encoding. */
    static <T extends ABIObject> T parseABIObject(InputStream is, Set<TypeEnum> types, MessageDigest digest, int flags) {
        return parseABIObjectAndCloseReader(reader(is), types, digest, flags);
    }

    private static <T extends ABIObject> T parseABIObjectAndCloseReader(JsonReader reader, Set<TypeEnum> types, MessageDigest digest, int flags) {
        try (JsonReader ignored = reader) {
            T obj = tryParseStreaming(reader, types, digest, flags);
            if (obj != null) {
                return obj;
            }
            throw new IllegalArgumentException("unexpected ABI object type");
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends ABIObject> T tryParseStreaming(JsonReader reader, Set<TypeEnum> types, MessageDigest digest, int flags) throws IOException {
        reader.beginObject();
        TypeEnum t = null;
        String name = null;
        TupleType<?> inputs = null;
        TupleType<?> outputs = null;
        String stateMutability = null;
        Boolean anonymous = null;
        do {
            switch (reader.nextName()) {
            case TYPE:
                requireNull(t, TYPE);
                t = TypeEnum.parse(reader.nextString());
                if (!types.contains(t)) {
                    // skip this JSON object. for best performance, "type" should be declared first
                    while (reader.peek() != JsonToken.END_OBJECT) {
                        reader.skipValue();
                    }
                    reader.endObject();
                    return null;
                }
                continue;
            case NAME: requireNull(name, NAME); name = reader.nextString(); continue;
            case INPUTS: requireNull(inputs, INPUTS); inputs = parseTupleType(reader, flags); continue;
            case OUTPUTS: requireNull(outputs, OUTPUTS); outputs = parseTupleType(reader, flags); continue;
            case STATE_MUTABILITY: requireNull(stateMutability, STATE_MUTABILITY); stateMutability = reader.nextString(); continue;
            case ANONYMOUS: requireNull(anonymous, ANONYMOUS); anonymous = reader.nextBoolean(); continue;
            default: reader.skipValue();
            }
        } while (reader.peek() != JsonToken.END_OBJECT);
        reader.endObject();
        if (t == null) {
            t = TypeEnum.FUNCTION;
            if (!types.contains(t)) {
                return null; // skip
            }
        }
        if (inputs == null) {
            inputs = TupleType.empty(flags);
        }
        switch (t.ordinal()) {
        case TypeEnum.ORDINAL_FUNCTION:
        case TypeEnum.ORDINAL_RECEIVE:
        case TypeEnum.ORDINAL_FALLBACK:
        case TypeEnum.ORDINAL_CONSTRUCTOR: return (T) new Function(t, name, inputs, outputs != null ? outputs : TupleType.empty(flags), stateMutability, digest);
        case TypeEnum.ORDINAL_EVENT: return (T) new Event<>(name, anonymous == Boolean.TRUE, inputs, inputs.indexed);
        case TypeEnum.ORDINAL_ERROR: return (T) new ContractError<>(name, inputs);
        default: throw new AssertionError();
        }
    }

    private static void requireNull(Object val, String key) {
        if (val != null) {
            throw new IllegalStateException("duplicate field: " + key);
        }
    }

    private static TupleType<?> parseTupleType(final JsonReader reader, final int flags) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return TupleType.empty(flags);
        }

        reader.beginArray();
        if (reader.peek() == JsonToken.END_ARRAY) {
            reader.endArray();
            return TupleType.empty(flags);
        }

        final StringBuilder canonicalType = TupleType.newTypeBuilder();
        ABIType<?>[] elements = new ABIType<?>[8];
        String[] names = new String[8];
        String[] internalTypes = new String[8];
        boolean[] indexedManifest = new boolean[8];
        boolean dynamic = false;

        for (int i = 0; true; canonicalType.append(',')) {
            String type = null;
            TupleType<?> components = null;
            String internalType = null;
            Boolean indexed = null;

            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                switch (reader.nextName()) {
                case TYPE: requireNull(type, TYPE); type = reader.nextString(); continue;
                case COMPONENTS: requireNull(components, COMPONENTS); components = parseTupleType(reader, flags); continue;
                case NAME: requireNull(names[i], NAME); names[i] = reader.nextString(); continue;
                case INTERNAL_TYPE: requireNull(internalType, INTERNAL_TYPE); internalType = reader.nextString(); continue;
                case INDEXED: requireNull(indexed, INDEXED); indexed = reader.nextBoolean(); continue;
                default: reader.skipValue();
                }
            }
            reader.endObject();

            final ABIType<?> e = resolveElement(type, components, flags, i);
            canonicalType.append(e.canonicalType);
            dynamic |= e.dynamic;
            elements[i] = e;

            if (internalType != null) {
                internalTypes[i] = internalType.equals(e.canonicalType) ? e.canonicalType : internalType;
            }
            if (indexed == Boolean.TRUE) {
                indexedManifest[i] = true;
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
                        Arrays.copyOf(indexedManifest, i),
                        flags
                );
            }

            if (i == elements.length) {
                final int newLen = i << 1;
                elements = Arrays.copyOf(elements, newLen);
                names = Arrays.copyOf(names, newLen);
                internalTypes = Arrays.copyOf(internalTypes, newLen);
                indexedManifest = Arrays.copyOf(indexedManifest, newLen);
            }
        }
    }

    private static ABIType<?> resolveElement(String type, TupleType<?> components, int flags, int i) {
        if (type == null || type.charAt(0) == '(') {
            throw new IllegalArgumentException("bad type at tuple index " + i);
        }
        if (components != null) {
            if (type.equals(TUPLE)) {
                return components;
            }
            if (type.startsWith(TUPLE)) {
                return TypeFactory.build(components.canonicalType + type.substring(TUPLE.length()), null, components, flags); // tuple array
            }
            throw new IllegalArgumentException("unexpected field " + COMPONENTS + " at tuple index " + i);
        }
        if (type.startsWith(TUPLE)) {
            throw new IllegalArgumentException("components missing at tuple index " + i);
        }
        return TypeFactory.create(flags, type);
    }

    static JsonReader reader(InputStream input) {
        return strict(
                new BufferedReader(
                    new InputStreamReader(
                            input,
                            StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                    )
                )
        );
    }

    static JsonReader reader(String json) {
        return strict(new StringReader(json));
    }

    private static final int STRICTNESS;

    static {
        final JsonReader reader = new JsonReader(new StringReader(""));
        int level = 0;
        try {
            reader.setStrictness(Strictness.STRICT); // since gson 2.11.0
            level = 1;
            reader.setNestingLimit(50); // since gson 2.12.0 (allow setStrictness to succeed before trying)
            level = 2;
        } catch (LinkageError ignored) { // e.g. runtime gson doesn't have one of the above methods
        }
        STRICTNESS = level;
    }

    @SuppressWarnings("deprecation")
    private static JsonReader strict(Reader r) {
        final JsonReader reader = new JsonReader(r);
        switch (STRICTNESS) {
        case 2: reader.setNestingLimit(50); /* fall through */
        case 1: reader.setStrictness(Strictness.STRICT); return reader;
        default: reader.setLenient(false); return reader;
        }
    }

    private static final class NonSyncWriter extends CharArrayWriter {

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
}
