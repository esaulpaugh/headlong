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

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.abi.ABIJSON.reader;

/** Parses JSON arrays containing contract ABI descriptions. Object types are {@link Function}, {@link Event}, and {@link ContractError}. */
public final class ABIParser {

    private final int flags;
    private final Set<TypeEnum> types;

    public ABIParser() {
        this(ABIType.FLAGS_NONE, ABIJSON.ALL);
    }

    public ABIParser(int flags) {
        this(flags, ABIJSON.ALL);
    }

    public ABIParser(Set<TypeEnum> types) {
        this(ABIType.FLAGS_NONE, types);
    }

    /**
     * @param flags flags with which to initialize the parsed {@link ABIObject}s. {@link ABIType#FLAGS_NONE} or {@link ABIType#FLAG_LEGACY_DECODE}
     * @param types {@link Set} of {@link ABIObject} types to parse. Objects whose type is not in the set will be skipped
     */
    public ABIParser(int flags, Set<TypeEnum> types) {
        if (flags != ABIType.FLAGS_NONE
                && flags != ABIType.FLAG_LEGACY_DECODE) {
            throw new IllegalArgumentException("Argument flags must be one of: { ABIType.FLAGS_NONE, ABIType.FLAG_LEGACY_DECODE }");
        }
        this.flags = flags;
        this.types = EnumSet.copyOf(types);
    }

    public <T extends ABIObject> List<T> parse(String arrayJson) {
        return ABIJSON.parseArray(reader(arrayJson), types, flags);
    }

    public <T extends ABIObject> List<T> parse(InputStream arrayStream) {
        return ABIJSON.parseArray(reader(arrayStream), types, flags);
    }

    public <T extends ABIObject> Stream<T> stream(String arrayJson) {
        return stream(reader(arrayJson), types, flags);
    }

    public <T extends ABIObject> Stream<T> stream(InputStream arrayStream) {
        return stream(reader(arrayStream), types, flags);
    }

    private static <T extends ABIObject> Stream<T> stream(JsonReader reader, Set<TypeEnum> types, int flags) {
        final ABIJSON.JsonSpliterator<T> spliterator = new ABIJSON.JsonSpliterator<>(reader, types, flags); // For single-threaded use only
        return StreamSupport.stream(spliterator, false)
                .onClose(spliterator::close);
    }
}
