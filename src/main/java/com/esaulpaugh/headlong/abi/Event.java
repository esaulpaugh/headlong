package com.esaulpaugh.headlong.abi;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.Arrays;

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
        this.name = name;
        this.inputs = params;
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

    public TupleType getIndexedParams() {
        return inputs.subTupleType(indexManifest);
    }

    public TupleType getNonIndexedParams() {
        return inputs.subTupleType(indexManifest, true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        inputs.toString(sb);
        return sb.toString();
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
