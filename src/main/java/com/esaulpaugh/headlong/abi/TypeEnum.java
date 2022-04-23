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

public enum TypeEnum {

    FUNCTION(ABIJSON.FUNCTION),
    RECEIVE(ABIJSON.RECEIVE),
    FALLBACK(ABIJSON.FALLBACK),
    CONSTRUCTOR(ABIJSON.CONSTRUCTOR),
    EVENT(ABIJSON.EVENT),
    ERROR(ABIJSON.ERROR);

    static final int ORDINAL_FUNCTION = 0;
    static final int ORDINAL_RECEIVE = 1;
    static final int ORDINAL_FALLBACK = 2;
    static final int ORDINAL_CONSTRUCTOR = 3;
    static final int ORDINAL_EVENT = 4;
    static final int ORDINAL_ERROR = 5;

    final String name;

    TypeEnum(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TypeEnum parse(String typeString) {
        if(typeString == null) {
            return FUNCTION;
        }
        switch (typeString) {
        case ABIJSON.FUNCTION: return FUNCTION;
        case ABIJSON.RECEIVE: return RECEIVE;
        case ABIJSON.FALLBACK: return FALLBACK;
        case ABIJSON.CONSTRUCTOR: return CONSTRUCTOR;
        case ABIJSON.EVENT: return EVENT;
        case ABIJSON.ERROR: return ERROR;
        default: throw unexpectedType(typeString);
        }
    }

    static IllegalArgumentException unexpectedType(String t) {
        return new IllegalArgumentException("unexpected type: " + (t == null ? null : "\"" + t + '"'));
    }
}