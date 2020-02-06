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

import com.esaulpaugh.headlong.util.JsonUtils;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

/** Represents an event in Ethereum. */
public final class Event implements ABIObject {

    private final String name;

    private final TupleType inputs;

    private final boolean[] indexManifest;

    private final boolean anonymous;

    public Event(String name, String paramsString, boolean[] indexed) {
        this(name, paramsString, indexed, false);
    }

    public Event(String name, String paramsString, boolean[] indexed, boolean anonymous) {
        this(name, TupleType.parse(paramsString), indexed, anonymous);
    }

    public Event(String name, TupleType params, boolean[] indexed, boolean anonymous) {
        this.name = Objects.requireNonNull(name);
        this.inputs = Objects.requireNonNull(params);
        if(indexed.length != inputs.elementTypes.length) {
            throw new IllegalArgumentException("indexed.length doesn't match number of inputs");
        }
        this.indexManifest = Arrays.copyOf(indexed, indexed.length);
        this.anonymous = anonymous;
    }

    public String getName() {
        return name;
    }

    public TupleType getParams() {
        return inputs;
    }

    public boolean[] getIndexManifest() {
        return Arrays.copyOf(indexManifest, indexManifest.length);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String signature() {
        return name + inputs.canonicalType;
    }

    public TupleType getIndexedParams() {
        return inputs.subTupleType(indexManifest);
    }

    public TupleType getNonIndexedParams() {
        return inputs.subTupleType(indexManifest, true);
    }

    public byte[] topics0() {
        return topics0(Function.newDefaultDigest());
    }

    public byte[] topics0(MessageDigest md) {
        return anonymous ? null : md.digest(Strings.decode(signature(), UTF_8));
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + inputs.hashCode();
        result = 31 * result + Arrays.hashCode(indexManifest);
        result = 31 * result + (anonymous ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (anonymous != event.anonymous) return false;
        if (!name.equals(event.name)) return false;
        if (!inputs.equals(event.inputs)) return false;
        return Arrays.equals(indexManifest, event.indexManifest);
    }

    public static Event fromJson(String eventJson) {
        return fromJsonObject(JsonUtils.parseObject(eventJson));
    }

    public static Event fromJsonObject(JsonObject event) {
        return ABIJSON.parseEvent(event);
    }

    @Override
    public String toJson(boolean pretty) {
        JsonObject object = ABIJSON.buildEventJson(this);
        return pretty ? JsonUtils.toPrettyPrint(object) : object.toString();
    }

    @Override
    public String toString() {
        return toJson(true);
    }
}
