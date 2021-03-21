package com.esaulpaugh.headlong.abi;

import java.util.Locale;

public enum TypeEnum {

    FUNCTION,
    RECEIVE,
    FALLBACK,
    CONSTRUCTOR,
    EVENT;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static TypeEnum parse(String typeString) {
        if(typeString != null) {
            switch (typeString) {
            case ABIJSON.FUNCTION: return TypeEnum.FUNCTION;
            case ABIJSON.RECEIVE: return TypeEnum.RECEIVE;
            case ABIJSON.FALLBACK: return TypeEnum.FALLBACK;
            case ABIJSON.CONSTRUCTOR: return TypeEnum.CONSTRUCTOR;
            case ABIJSON.EVENT: return TypeEnum.EVENT;
            default: throw unexpectedType(typeString);
            }
        }
        return TypeEnum.FUNCTION;
    }

    static IllegalArgumentException unexpectedType(String t) {
        return new IllegalArgumentException("unexpected type: " + (t == null ? null : "\"" + t + "\""));
    }
}