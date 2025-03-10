/*
   Copyright 2025 Evan Saulpaugh

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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.abi.ABIJSON.parseAndCloseArray;
import static com.esaulpaugh.headlong.abi.ABIJSON.reader;

/** Parses JSON arrays containing contract ABI descriptions. Object types are {@link Function}, {@link Event}, and {@link ContractError}. */
public final class ABIParser {

    final int flags;
    final Set<TypeEnum> types;
    final transient boolean needsDigest;

    public ABIParser() {
        this.flags = ABIType.FLAGS_NONE;
        this.types = ABIJSON._ALL;
        this.needsDigest = true;
    }

    public ABIParser(int flags) {
        this.flags = checkFlags(flags);
        this.types = ABIJSON._ALL;
        this.needsDigest = true;
    }

    public ABIParser(Set<TypeEnum> types) {
        this.flags = ABIType.FLAGS_NONE;
        this.types = EnumSet.copyOf(types);
        this.needsDigest = needsDigest();
    }

    /**
     * @param flags flags with which to initialize the parsed {@link ABIObject}s. {@link ABIType#FLAGS_NONE} or {@link ABIType#FLAG_LEGACY_DECODE}
     * @param types {@link Set} of {@link ABIObject} types to parse. Objects whose type is not in the set will be skipped
     */
    public ABIParser(int flags, Set<TypeEnum> types) {
        this.flags = checkFlags(flags);
        this.types = EnumSet.copyOf(types);
        this.needsDigest = needsDigest();
    }

    public <T extends ABIObject> List<T> parse(String arrayJson) {
        return parse(reader(arrayJson));
    }

    public <T extends ABIObject> List<T> parse(InputStream arrayStream) {
        return parse(reader(arrayStream));
    }

    public <T extends ABIObject> Stream<T> stream(String arrayJson) {
        return stream(reader(arrayJson));
    }

    public <T extends ABIObject> Stream<T> stream(InputStream arrayStream) {
        return stream(reader(arrayStream));
    }

    /**
     * Parses the value for the key "abi" as a contract ABI JSON array.
     *
     * @param objectJson    the JSON object containing the "abi" field
     * @return  the list of ABI objects
     * @param <T>   the element type
     */
    public <T extends ABIObject> List<T> parseABIField(String objectJson) {
        return readABIField(reader(objectJson), true);
    }

    public <T extends ABIObject> List<T> parseABIField(InputStream objectStream) {
        return readABIField(reader(objectStream), true);
    }

    public <T extends ABIObject> Stream<T> streamABIField(String objectJson) {
        return readABIField(reader(objectJson), false);
    }

    public <T extends ABIObject> Stream<T> streamABIField(InputStream objectStream) {
        return readABIField(reader(objectStream), false);
    }

    @SuppressWarnings("unchecked")
    private <X> X readABIField(JsonReader reader, boolean parse) {
        try {
            reader.beginObject();
            while (reader.peek() != JsonToken.END_OBJECT) {
                if ("abi".equals(reader.nextName())) {
                    return (X) (parse ? parse(reader) : stream(reader));
                }
                reader.skipValue();
            }
            throw new IllegalArgumentException("abi key not found");
        } catch (IOException io) {
            throw new IllegalStateException(io);
        }
    }

    private <T extends ABIObject> List<T> parse(JsonReader reader) {
        return parseAndCloseArray(reader, types, flags, needsDigest ? Function.newDefaultDigest() : null);
    }

    private <T extends ABIObject> Stream<T> stream(JsonReader reader) {
        return StreamSupport.stream(new JsonSpliterator<T>(reader), false) // sequential (non-parallel)
                .onClose(() -> {
                    try {
                        reader.endArray();
                        reader.close();
                    } catch (IOException io) {
                        throw new IllegalStateException(io);
                    }
                });
    }

    private final class JsonSpliterator<T extends ABIObject> extends Spliterators.AbstractSpliterator<T> {

        private final JsonReader reader;
        private final MessageDigest digest = needsDigest ? Function.newDefaultDigest() : null;

        JsonSpliterator(final JsonReader reader) {
            super(0, ORDERED | NONNULL);
            this.reader = reader;
            try {
                this.reader.beginArray();
            } catch (IOException io) {
                throw new IllegalStateException(io);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                while (reader.peek() != JsonToken.END_ARRAY) {
                    T e = ABIJSON.tryParseStreaming(reader, types, digest, flags);
                    if (e != null) {
                        action.accept(e);
                        return true;
                    }
                }
                return false;
            } catch (IOException io) {
                throw new IllegalStateException(io);
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null; // alternatively, throw new ConcurrentModificationException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ABIParser) {
            ABIParser other = (ABIParser) o;
            return flags == other.flags && types.equals(other.types);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return flags + types.hashCode();
    }

    @Override
    public String toString() {
        return "ABIParser{flags=" + flags + ", types=" + types + '}';
    }

    private static int checkFlags(int flags) {
        if (flags == ABIType.FLAGS_NONE || flags == ABIType.FLAG_LEGACY_DECODE) {
            return flags;
        }
        throw new IllegalArgumentException("Flags must be one of: ABIType.FLAGS_NONE, ABIType.FLAG_LEGACY_DECODE");
    }

    private boolean needsDigest() {
        return types.contains(TypeEnum.FUNCTION)
                || types.contains(TypeEnum.CONSTRUCTOR)
                || types.contains(TypeEnum.RECEIVE)
                || types.contains(TypeEnum.FALLBACK);
    }
}
