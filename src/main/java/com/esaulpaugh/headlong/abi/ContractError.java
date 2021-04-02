package com.esaulpaugh.headlong.abi;

public class ContractError implements ABIObject {

    @Override
    public TypeEnum getType() {
        return TypeEnum.ERROR;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public TupleType getInputs() {
        return null;
    }

    @Override
    public String getCanonicalSignature() {
        return null;
    }

    @Override
    public String toJson(boolean pretty) {
        return null;
    }
}
