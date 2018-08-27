package com.esaulpaugh.headlong.abi.beta.type;

import java.nio.charset.StandardCharsets;

abstract class Array extends StackableType {

    protected final StackableType elementType;
    protected final int length;

    protected Array(String canonicalAbiType, String className, StackableType elementType, int length) {
        this(canonicalAbiType, className, elementType, length, false);
    }

    protected Array(String canonicalAbiType, String className, StackableType elementType, int length, boolean dynamic) { // , Stack<StackableType> typeStack
        super(canonicalAbiType, className, dynamic);
        this.elementType = elementType;
        this.length = length;
    }

    @Override
    int byteLength(Object value) {
        return getDataLen(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + elementType + ", " + length;
    }

    @Override
    protected void validate(final Object value) { // , final String expectedClassName // int stackIndex
        super.validate(value);

        if(value.getClass().isArray()) {
//            StackableType elementType = ((Array) this.typeStack.peek()).elementType;
            if(value instanceof Object[]) { // includes BigInteger[]
                Object[] arr = (Object[]) value;
                checkLength(arr.length);
                for (Object element : arr) {
                    elementType.validate(element);
//                    validate(, element); //
                }
            } else if (value instanceof byte[]) {
                validateByteArray((byte[]) value);
            } else if (value instanceof int[]) {
                validateIntArray((int[]) value);
            } else if (value instanceof long[]) {
                validateLongArray((long[]) value);
            } else if (value instanceof short[]) {
                validateShortArray((short[]) value);
            } else if (value instanceof boolean[]) {
                validateBooleanArray((boolean[]) value);
            }
        } else if(value instanceof String) {
            validateByteArray(((String) value).getBytes(StandardCharsets.UTF_8));
        } else if(value instanceof Number) {
            elementType.validate(value);
//            _validateNumber(value, elementType);
        } else if(value instanceof Boolean) {
            elementType.validate(value);
//            Type.validate(value, CLASS_NAME_BOOLEAN, elementType.canonicalAbiType); // TODO
        } else {
            throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }
    }

    private void validateBooleanArray(boolean[] arr) {
        checkLength(arr.length);
    }

    private void validateByteArray(byte[] arr) {
        checkLength(arr.length);
    }

    private void validateShortArray(short[] arr) {
        checkLength(arr.length);
    }

    private void validateIntArray(int[] arr) {
        final int len = arr.length;
        checkLength(len);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateLongArray(long[] arr) {
        final int len = arr.length;
        checkLength(len);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void checkLength(int actual) {
        int expected = this.length;
        if(expected == -1) {
            System.out.println("dynamic length");
            return;
        }
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("fixed length valid;");
    }

    // -----------------------------------------------------------------------------------------------------------------

    protected int _overhead(Object value) { // , boolean dynamic
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) { // always needs dynamic head?
                return dynamic ? 64 : 0;
            }
            if (value instanceof int[]) {
                return dynamic ? 64 : 0;
            }
            if (value instanceof long[]) {
                return dynamic ? 64 : 0;
            }
            if (value instanceof short[]) {
                return dynamic ? 64 : 0;
            }
            if (value instanceof boolean[]) {
                return dynamic ? 64 : 0;
            }
            if (value instanceof Number[]) {
                return dynamic ? 64 : 0;
            }
        }
        if (value instanceof String) { // always needs dynamic head
            return 64;
        }
        if (value instanceof Number) {
            return 0;
        }
        if (value instanceof Tuple) {
            throw new RuntimeException("arrays of tuples not yet supported"); // TODO **************************************
        }
        if (value instanceof Object[]) {
//            int len = 0;
//            for (Object element : (Object[]) value) {
//                len += this.elementType.byteLength(element);
//            }
            return dynamic ? 64 : 0;
//            throw new AssertionError("Object array not expected here");
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    protected int getDataLen(Object value) {
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) { // always needs dynamic head?
                int staticLen = roundUp(((byte[]) value).length);
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof int[]) {
                int staticLen = ((int[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof long[]) {
                int staticLen = ((long[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof short[]) {
                int staticLen = ((short[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof boolean[]) {
                int staticLen = ((boolean[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof Number[]) {
                int staticLen = ((Number[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
        }
        if (value instanceof String) { // always needs dynamic head
            return 64 + roundUp(((String) value).length());
        }
        if (value instanceof Number) {
            return 32;
        }
        if (value instanceof Tuple) {
            throw new RuntimeException("arrays of tuples not yet supported"); // TODO **************************************
        }
        if (value instanceof Object[]) {
            int len = 0;
            for (Object element : (Object[]) value) {
                len += this.elementType.byteLength(element);
            }
            return dynamic ? 64 + len : len;
//            throw new AssertionError("Object array not expected here");
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    protected static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 - mod);
    }

}
