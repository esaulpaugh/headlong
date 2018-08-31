package com.esaulpaugh.headlong.abi.beta.type;

import static com.esaulpaugh.headlong.abi.beta.util.ClassNames.toFriendly;

// TODO support model classes à la Student.java
// TODO support vyper e.g. "decimal"
abstract class StackableType {

    public static final StackableType[] EMPTY_TYPE_ARRAY = new StackableType[0];

    protected final String canonicalAbiType;
    protected final String className;

    protected final boolean dynamic;

    protected StackableType(String canonicalAbiType, String className) {
        this(canonicalAbiType, className, false);
    }

    protected StackableType(String canonicalAbiType, String className, boolean dynamic) {
        this.canonicalAbiType = canonicalAbiType;
        this.className = className;
        this.dynamic = dynamic;
    }

    abstract int byteLength(Object value);

    protected void validate(Object value) {
        validate(this, value);
    }

    private static void validate(final StackableType type, final Object value) { //  { // , final String expectedClassName

        String expectedClassName = type.className; // TODO

        // will throw NPE if argument null
        if(!expectedClassName.equals(value.getClass().getName())) {
            boolean isAssignable;
            try {
                isAssignable = Class.forName(expectedClassName).isAssignableFrom(value.getClass());
            } catch (ClassNotFoundException cnfe) {
                isAssignable = false;
            }
            if(!isAssignable) {
                throw new IllegalArgumentException("class mismatch: "
                        + value.getClass().getName()
                        + " not assignable to "
                        + expectedClassName
                        + " (" + toFriendly(value.getClass().getName()) + " not instanceof " + toFriendly(expectedClassName) + "/" + type.canonicalAbiType + ")");
            }
        }
        System.out.print("matches class " + expectedClassName + " ");
    }
}
