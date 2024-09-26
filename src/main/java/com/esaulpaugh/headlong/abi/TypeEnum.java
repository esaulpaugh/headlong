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

/** Enum representing the variants of {@link ABIObject}. */
public enum TypeEnum {

    FUNCTION(ABIJSON.FUNCTION, true),
    RECEIVE(ABIJSON.RECEIVE, true),
    FALLBACK(ABIJSON.FALLBACK, true),
    CONSTRUCTOR(ABIJSON.CONSTRUCTOR, true),
    EVENT(ABIJSON.EVENT, false),
    ERROR(ABIJSON.ERROR, false);

    static final int ORDINAL_FUNCTION = 0;
    static final int ORDINAL_RECEIVE = 1;
    static final int ORDINAL_FALLBACK = 2;
    static final int ORDINAL_CONSTRUCTOR = 3;
    static final int ORDINAL_EVENT = 4;
    static final int ORDINAL_ERROR = 5;

    private final String name;
    public final boolean isFunction;

    TypeEnum(String name, boolean isFunction) {
        this.name = name;
        this.isFunction = isFunction;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TypeEnum parse(final String typeString) {
        if (ABIJSON.FUNCTION.equals(typeString) || typeString == null) {
            return FUNCTION;
        }
        if (ABIJSON.EVENT.equals(typeString)) {
            return EVENT;
        }
        if (ABIJSON.ERROR.equals(typeString)) return ERROR;
        if (ABIJSON.RECEIVE.equals(typeString)) return RECEIVE;
        if (ABIJSON.FALLBACK.equals(typeString)) return FALLBACK;
        if (ABIJSON.CONSTRUCTOR.equals(typeString)) return CONSTRUCTOR;
        throw unexpectedType(typeString);
    }

    static IllegalArgumentException unexpectedType(String t) {
        return new IllegalArgumentException("unexpected type: " + (t == null ? null : '"' + t + '"'));
    }
}