package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.JsonUtils;
import com.google.gson.JsonObject;

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

    public Function function() {
        return Function.parse(getCanonicalSignature());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, inputs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractError that = (ContractError) o;
        return name.equals(that.name) && inputs.equals(that.inputs);
    }

    public static ContractError fromJson(String errorJson) {
        return fromJsonObject(JsonUtils.parseObject(errorJson));
    }

    public static ContractError fromJsonObject(JsonObject error) {
        return ABIJSON.parseError(error);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public boolean isContractError() {
        return true;
    }
}
