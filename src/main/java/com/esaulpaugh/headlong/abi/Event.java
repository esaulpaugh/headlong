package com.esaulpaugh.headlong.abi;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.ContractJSONParser.parseEvent;

public class Event {

    private final String name;

    private final TupleType inputs;

    private final boolean[] indexed;

    private final boolean anonymous;

    Event(String name, TupleType inputs, boolean[] indexed, boolean anonymous) {
        this.name = name;
        this.inputs = inputs;
        this.indexed = Arrays.copyOf(indexed, indexed.length);
        this.anonymous = anonymous;
    }

    public String getName() {
        return name;
    }

    public TupleType getInputs() {
        return inputs;
    }

    public boolean[] getIndexed() {
        return Arrays.copyOf(indexed, indexed.length);
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    // ---------------------

    public static Event fromJson(String eventJson) throws ParseException {
        return parseEvent(eventJson);
    }

    public static Event fromJsonObject(JsonObject event) throws ParseException {
        return parseEvent(event);
    }

    @Override
    public String toString() {
        System.out.println(getName());
        StringBuilder sb = new StringBuilder();
        inputs.recursiveToString(sb);
        return sb.toString();
    }

}
