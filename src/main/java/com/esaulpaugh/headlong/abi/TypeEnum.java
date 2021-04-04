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