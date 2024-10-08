/*
   Copyright 2019 Evan Saulpaugh

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

import com.google.gson.JsonObject;

/** Supertype of json-encodeable types {@link Function}, {@link Event}, and {@link ContractError}. */
public interface ABIObject {

    TypeEnum getType();

    String getName();

    <T extends Tuple> TupleType<T> getInputs();

    String getCanonicalSignature();

    default String toJson(boolean pretty) {
        return ABIJSON.toJson(this, pretty);
    }

    default boolean isFunction() {
        return false;
    }

    default boolean isEvent() {
        return false;
    }

    default boolean isContractError() {
        return false;
    }

    default Function asFunction() {
        return (Function) this;
    }

    @SuppressWarnings("unchecked")
    default <X extends Tuple> Event<X> asEvent() {
        return (Event<X>) this;
    }

    @SuppressWarnings("unchecked")
    default <X extends Tuple> ContractError<X> asContractError() {
        return (ContractError<X>) this;
    }

    static <T extends ABIObject> T fromJson(String json) {
        return fromJson(ABIType.FLAGS_NONE, json);
    }

    /**
     * Constructs an {@link ABIObject} from a {@link JsonObject}.
     *
     * @param flags     {@link ABIType#FLAGS_NONE} recommended. See also {@link ABIType#FLAG_LEGACY_DECODE}
     * @param json    the JSON object string to decode
     * @return  the {@link ABIObject} represented by {@code object}
     * @param <T>   {@link Function}, {@link Event}, {@link ContractError}, or supertype {@link ABIObject}
     */
    static <T extends ABIObject> T fromJson(int flags, String json) {
        return ABIJSON.parseABIObject(json, ABIJSON.ALL, Function.newDefaultDigest(), flags);
    }
}
