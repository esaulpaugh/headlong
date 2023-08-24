/*
   Copyright 2021 Evan Saulpaugh

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

import java.util.Objects;

public final class ContractError implements ABIObject {

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
        if (o instanceof ContractError) {
            ContractError that = (ContractError) o;
            return name.equals(that.name) && inputs.equals(that.inputs);
        }
        return false;
    }

    public static ContractError fromJson(String errorJson) {
        return fromJsonObject(JsonUtils.parseObject(errorJson));
    }

    public static ContractError fromJson(int flags, String errorJson) {
        return fromJsonObject(flags, JsonUtils.parseObject(errorJson));
    }

    public static ContractError fromJsonObject(JsonObject error) {
        return ABIJSON.parseError(error, ABIType.FLAGS_NONE);
    }

    public static ContractError fromJsonObject(int flags, JsonObject error) {
        return ABIJSON.parseError(error, flags);
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
