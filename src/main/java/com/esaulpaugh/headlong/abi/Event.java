package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class Event implements ABIObject {

    private final String name;

    private final TupleType inputs;

    private final boolean[] indexManifest;

    private final boolean anonymous;

    public Event(String name, String paramsString, boolean[] indexed) throws ParseException {
        this(name, paramsString, indexed, false);
    }

    public Event(String name, String paramsString, boolean[] indexed, boolean anonymous) throws ParseException {
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

    public String signature() {
        return name + inputs.canonicalType;
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

    public TupleType getIndexedParams() {
        return inputs.subTupleType(indexManifest);
    }

    public TupleType getNonIndexedParams() {
        return inputs.subTupleType(indexManifest, true);
    }

    public byte[] topics0() {
        return anonymous ? null : Function.newDefaultDigest().digest(Strings.decode(signature(), UTF_8));
    }

    public byte[] topics0(MessageDigest md) {
        return anonymous ? null : md.digest(Strings.decode(signature(), UTF_8));
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

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + inputs.hashCode();
        result = 31 * result + Arrays.hashCode(indexManifest);
        result = 31 * result + (anonymous ? 1 : 0);
        return result;
    }

    @Override
    public int objectType() {
        return ABIObject.EVENT;
    }

    public static Event fromJson(String eventJson) throws ParseException {
        return ContractJSONParser.parseEvent(eventJson);
    }

    public static Event fromJsonObject(JsonObject event) throws ParseException {
        return ContractJSONParser.parseEvent(event);
    }
}
