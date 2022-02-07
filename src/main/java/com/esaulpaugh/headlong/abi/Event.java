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

import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Objects;

/** Represents an event in Ethereum. */
public final class Event implements ABIObject {

    private final String name;
    private final boolean anonymous;
    private final TupleType inputs;
    private final boolean[] indexManifest;

    public static Event create(String name, TupleType inputs, boolean... indexed) {
        return new Event(name, false, inputs, indexed);
    }

    public static Event createAnonymous(String name, TupleType inputs, boolean... indexed) {
        return new Event(name, true, inputs, indexed);
    }

    public Event(String name, boolean anonymous, TupleType inputs, boolean... indexed) {
        this.name = Objects.requireNonNull(name);
        this.inputs = Objects.requireNonNull(inputs);
        if(indexed.length != inputs.size()) {
            throw new IllegalArgumentException("indexed.length doesn't match number of inputs");
        }
        this.indexManifest = Arrays.copyOf(indexed, indexed.length);
        this.anonymous = anonymous;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.EVENT;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TupleType getInputs() {
        return inputs;
    }

    public boolean[] getIndexManifest() {
        return Arrays.copyOf(indexManifest, indexManifest.length);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public String getCanonicalSignature() {
        return name + inputs.canonicalType;
    }

    public TupleType getIndexedParams() {
        return inputs.select(indexManifest);
    }

    public TupleType getNonIndexedParams() {
        return inputs.exclude(indexManifest);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * name.hashCode() + inputs.hashCode()) + Arrays.hashCode(indexManifest)) + Boolean.hashCode(anonymous);
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof Event)) return false;
        Event other = (Event) o;
        return other.anonymous == this.anonymous
                && other.name.equals(this.name)
                && other.inputs.equals(this.inputs)
                && Arrays.equals(other.indexManifest, this.indexManifest);
    }

    public static Event fromJson(String eventJson) {
        return fromJsonObject(JsonUtils.parseObject(eventJson));
    }

    public static Event fromJsonObject(JsonObject event) {
        return ABIJSON.parseEvent(event);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public boolean isEvent() {
        return true;
    }
}
