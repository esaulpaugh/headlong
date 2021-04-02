package com.esaulpaugh.headlong.abi;

public enum TypeEnum {

    EVENT(ABIJSON.EVENT),
    FUNCTION(ABIJSON.FUNCTION),
    RECEIVE(ABIJSON.RECEIVE),
    FALLBACK(ABIJSON.FALLBACK),
    CONSTRUCTOR(ABIJSON.CONSTRUCTOR),
    ERROR(ABIJSON.ERROR);

    final String name;

    TypeEnum(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TypeEnum parse(String typeString) {
        for (TypeEnum e : values()) {
            if(e.name.equals(typeString)) {
                return e;
            }
        }
        if(typeString == null) {
            return FUNCTION;
        }
        throw unexpectedType(typeString);
    }

    static IllegalArgumentException unexpectedType(String t) {
        return new IllegalArgumentException("unexpected type: " + (t == null ? null : "\"" + t + "\""));
    }
}