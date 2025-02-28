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

import java.io.InputStream;
import java.util.Objects;

/** Represents a custom error. */
public final class ContractError<J extends Tuple> implements ABIObject {

    private final String name;
    private final TupleType<J> inputs;

    public ContractError(String name, TupleType<J> inputs) {
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

    @SuppressWarnings("unchecked")
    @Override
    public TupleType<J> getInputs() {
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
    public boolean isContractError() {
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + inputs.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ContractError) {
            ContractError<?> that = (ContractError<?>) o;
            return name.equals(that.name) && inputs.equals(that.inputs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    public static <X extends Tuple> ContractError<X> fromJson(String errorJson) {
        return fromJson(ABIType.FLAGS_NONE, errorJson);
    }

    /** @see ABIObject#fromJson(int, String) */
    public static <X extends Tuple> ContractError<X> fromJson(int flags, String errorJson) {
        return ABIJSON.parseABIObject(errorJson, ABIJSON.ERRORS, null, flags);
    }

    public static <X extends Tuple> ContractError<X> fromJson(int flags, InputStream jsonStream) {
        return ABIJSON.parseABIObject(jsonStream, ABIJSON.ERRORS, null, flags);
    }
}
