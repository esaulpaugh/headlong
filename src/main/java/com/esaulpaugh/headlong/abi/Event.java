package com.esaulpaugh.headlong.abi;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.ContractJSONParser.parseEvent;

public class Event implements ABIObject {

    private final String name;

    private final TupleType inputs;

    private final boolean[] indexManifest;

    private final boolean anonymous;

    Event(String name, TupleType inputs, boolean[] indexed, boolean anonymous) {
        this.name = name;
        this.inputs = inputs;
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
    public String getArgsString() {
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
        final ArrayList<ABIType<?>> indexedArgs = new ArrayList<>();
        final ABIType<?>[] params = inputs.elementTypes;
        for (int i = 0; i < indexManifest.length; i++) {
            if(indexManifest[i]) {
                indexedArgs.add(params[i]);
            }
        }
        return TupleType.create(indexedArgs);
    }

    public TupleType getNonIndexedParams() {
        final ArrayList<ABIType<?>> nonIndexedArgs = new ArrayList<>();
        final ABIType<?>[] params = inputs.elementTypes;
        for (int i = 0; i < indexManifest.length; i++) {
            if(!indexManifest[i]) {
                nonIndexedArgs.add(params[i]);
            }
        }
        return TupleType.create(nonIndexedArgs);
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
        return parseEvent(eventJson);
    }

    public static Event fromJsonObject(JsonObject event) throws ParseException {
        return parseEvent(event);
    }

    @Override
    public int objectType() {
        return ABIObject.EVENT;
    }
}
