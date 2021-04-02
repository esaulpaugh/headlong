package com.esaulpaugh.headlong.abi;

import java.util.Objects;

public class ContractError implements ABIObject {

    private final String name;
    private final TupleType inputs;

    public ContractError(String name, TupleType inputs) {
        this.name = Objects.requireNonNull(name);
        this.inputs = Objects.requireNonNull(inputs);
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.ERROR;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TupleType getInputs() {
        return inputs;
    }

    @Override
    public String getCanonicalSignature() {
        return name + inputs.canonicalType;
    }

    @Override
    public String toJson(boolean pretty) {
        return ABIJSON.toJson(this, ABIJSON.ERRORS, pretty);
    }
}
