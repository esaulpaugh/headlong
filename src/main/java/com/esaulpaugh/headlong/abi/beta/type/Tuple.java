package com.esaulpaugh.headlong.abi.beta.type;

class Tuple extends StackableType {

    protected final StackableType[] memberTypes;

    Tuple(String canonicalAbiType, boolean dynamic, StackableType... memberTypes) {
        super(canonicalAbiType, com.esaulpaugh.headlong.abi.beta.util.Tuple.class.getName(), dynamic); // Tuple.class.getName()
        this.memberTypes = memberTypes;
    }

    static Tuple create(String canonicalAbiType, StackableType... members) {

        for (StackableType type : members) {
            if(type.dynamic) {
                return new Tuple(canonicalAbiType, true, members);
            }
        }

        return new Tuple(canonicalAbiType, false, members);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + memberTypes.length;
    }

    @Override
    int byteLength(Object value) {
        com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;

        if(tuple.elements.length != memberTypes.length) {
            throw new IllegalArgumentException("tuple length mismatch");
        }

        int len = 0;
        for (int i = 0; i < memberTypes.length; i++) {
            len += memberTypes[i].byteLength(tuple.elements[i]);
        }

//        return len; // tuple has no dynamic head
        return len + 32;
//        return dynamic ? (memberTypes.length << 5) + len : len;
    }

    @Override
    protected void validate(final Object value) {
        super.validate(value);

        final com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;
        final Object[] elements = tuple.elements;

        final int expected = this.memberTypes.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("length valid;");

        checkTypes(this.memberTypes, elements);
    }

    public static void checkTypes(StackableType[] paramTypes, Object[] values) {
        final int n = paramTypes.length;
        int i = 0;
        try {
            for ( ; i < n; i++) {
                paramTypes[i].validate(values[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid arg @ " + i + ": " + e.getMessage(), e);
        }
    }
}
