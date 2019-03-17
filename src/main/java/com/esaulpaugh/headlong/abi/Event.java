package com.esaulpaugh.headlong.abi;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.Arrays;

public class Event implements ABIObject {

    private final String name;

    private final TupleType inputs;

    private final boolean[] indexManifest;

    private final boolean anonymous;

    Event(String name, String paramsString, boolean[] indexed) throws ParseException {
        this(name, paramsString, indexed, false);
    }

    Event(String name, String paramsString, boolean[] indexed, boolean anonymous) throws ParseException {
        this(name, TupleType.parse(paramsString), indexed, anonymous);
    }

    Event(String name, TupleType params, boolean[] indexed, boolean anonymous) {
        this.name = name;
        this.inputs = params;
        this.indexManifest = Arrays.copyOf(indexed, indexed.length);
        this.anonymous = anonymous;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the canonical type string for the tuple of inputs.
     * @return
     */
    public String getParamsString() {
        return inputs.canonicalType;
    }

    public TupleType getInputs() {
        return inputs;
    }

    public boolean[] getIndexManifest() {
        return Arrays.copyOf(indexManifest, indexManifest.length);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public TupleType getIndexedParams() {
        return inputs.subtuple(indexManifest);
    }

    public TupleType getNonIndexedParams() {
        return inputs.subtuple(indexManifest, true);
    }

    @Override
    public String toString() {
        System.out.println(getName());
        StringBuilder sb = new StringBuilder();
        inputs.recursiveToString(sb);
        return sb.toString();
    }

    // ---------------------

    public static Event fromJson(String eventJson) throws ParseException {
        return ContractJSONParser.parseEvent(eventJson);
    }

    public static Event fromJsonObject(JsonObject event) throws ParseException {
        return ContractJSONParser.parseEvent(event);
    }

    @Override
    public int objectType() {
        return ABIObject.EVENT;
    }
}
