package com.esaulpaugh.headlong.abi;

import java.nio.charset.StandardCharsets;
import java.util.Stack;

class ArrayType extends Type {

    private transient final Type baseType;
    private transient final Stack<Integer> fixedLengthStack;
    private transient final int arrayDepth;

    private ArrayType(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int arrayDepth, boolean dynamic) {
        super(canonicalAbiType, javaClassName, dynamic);
        this.baseType = Type.create(abiBaseType);
        this.fixedLengthStack = fixedLengthStack;
        this.arrayDepth = arrayDepth;
    }

    static ArrayType create(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int depth) {
        Integer baseTypeByteLen;
        int byteLen;

        boolean dynamic = fixedLengthStack.contains(null);
        if(!dynamic) {
            baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1); // e.g. uint8[2] --> 1, uint16[2] --> 32?

            int rounded = roundUp(baseTypeByteLen);

            if(baseTypeByteLen == 1 && !canonicalAbiType.equals("bytes1")) { // int8, uint8
                depth--;
            }

            int product = rounded;
            StringBuilder sb = new StringBuilder("(" + baseTypeByteLen + " --> " + product + ")");
            for (int i = depth - 1; i >= 0; i--) {
                product *= fixedLengthStack.get(i);
                sb.append(" * ").append(fixedLengthStack.get(i));
            }
            byteLen = product;
            System.out.println(canonicalAbiType + " : static len: " + sb.toString() + " = " + byteLen);
        }

        if(abiBaseType.equals("string")
                || abiBaseType.equals("function")
                || (abiBaseType.startsWith("bytes"))) {
            abiBaseType = "uint8";
        }

        return new ArrayType(canonicalAbiType, abiBaseType, javaClassName, fixedLengthStack, depth, dynamic);
    }

    @Override
    protected void validate(final Object value, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(value, expectedClassName, expectedLengthIndex);

        if(value.getClass().isArray()) {
            if(value instanceof Object[]) {
                validateArray((Object[]) value, expectedClassName, expectedLengthIndex);
            } else if (value instanceof byte[]) {
                validateByteArray((byte[]) value, expectedLengthIndex);
            } else if (value instanceof int[]) {
                validateIntArray((int[]) value, expectedLengthIndex);
            } else if (value instanceof long[]) {
                validateLongArray((long[]) value, expectedLengthIndex);
            } else if (value instanceof short[]) {
                validateShortArray((short[]) value, expectedLengthIndex);
            } else if (value instanceof boolean[]) {
                validateBooleanArray((boolean[]) value, expectedLengthIndex);
            }
        } else if(value instanceof String) {
            validateByteArray(((String) value).getBytes(StandardCharsets.UTF_8), expectedLengthIndex);
        } else if(value instanceof Number) {
            NumberType._validateNumber(value, ((NumberType) baseType).bitLimit);
        } else if(value instanceof Boolean) {
            super.validate(value, CLASS_NAME_BOOLEAN, expectedLengthIndex);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }
    }

    private void validateArray(final Object[] arr, final String expectedClassName, final int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));

        final String nextExpectedClassName;
        if(expectedClassName.charAt(1) == 'L') {
            nextExpectedClassName = expectedClassName.substring(2, expectedClassName.length() - 1);
        } else {
            nextExpectedClassName = expectedClassName.substring(1);
        }

        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], nextExpectedClassName, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateBooleanArray(boolean[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        final int nextExpectedLengthIndex = expectedLengthIndex + 1;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_BOOLEAN, nextExpectedLengthIndex);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateByteArray(byte[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
    }

    private void validateShortArray(short[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
//        int i = 0;
//        try {
//            for ( ; i < len; i++) {
//                validate(arr[i], CLASS_NAME_SHORT, expectedLengthIndex + 1);
//            }
//        } catch (IllegalArgumentException | NullPointerException re) {
//            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
//        }
    }

    private void validateIntArray(int[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_INT, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateLongArray(long[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_LONG, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private static void checkLength(int actual, Integer expected) {
        if(expected == null) {
            System.out.println("dynamic length");
            return;
        }
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("fixed length valid;");
    }

    @Override
    public Integer getDataByteLen(Object value) {
        Stack<Integer> dynamicByteLenStack = new Stack<>();
        buildByteLenStack(value, dynamicByteLenStack);

        int n = 1;
        for (int i = arrayDepth - 1; i >= 0; i--) {
            int len;
            Integer fixedLen = fixedLengthStack.get(i);
            if(fixedLen != null) {
                len = fixedLen;
            } else {
                len = dynamicByteLenStack.get(i);
            }
            n *= len;
        }

        return dynamic
                ? 32 + 32 + roundUp(n)
                : roundUp(n);
    }

//    protected static int _getNumElements(Object value) {
//        if (value instanceof String) {
//            return ((String) value).length();
//        }
//        if (value instanceof Number[]) {
//            return ((Number[]) value).length;
//        }
//        if (value instanceof byte[]) {
//            return ((byte[]) value).length;
//        }
//        if (value instanceof int[]) {
//            return ((int[]) value).length;
//        }
//        if (value instanceof long[]) {
//            return ((long[]) value).length;
//        }
//        if (value instanceof short[]) {
//            return ((short[]) value).length;
//        }
//        if (value instanceof boolean[]) {
//            return ((boolean[]) value).length;
//        }
//        if (value instanceof Tuple[]) {
//            return ((Tuple[]) value).length;
//        }
//        if (value instanceof Object[]) {
//            return ((Object[]) value).length;
//        }
//        // shouldn't happen if type checks/validation already occurred
//        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
//    }

    static void buildByteLenStack(Object value, Stack<Integer> dynamicLengthStack) {
        if(value instanceof String) {
            int len = ((String) value).length();
            System.out.println("len = " + len);
            dynamicLengthStack.push(roundUp(((String) value).length()));
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                dynamicLengthStack.push(roundUp(((Object[]) value).length << 5)); // mul 32
                Object[] arr = (Object[]) value;
                for (Object obj : arr) {
                    buildByteLenStack(obj, dynamicLengthStack);
                }
            } else if (value instanceof byte[]) {
                dynamicLengthStack.push(roundUp(((byte[]) value).length));
            } else if (value instanceof int[]) {
                dynamicLengthStack.push(((int[]) value).length << 5); // mul 32
            } else if (value instanceof long[]) {
                dynamicLengthStack.push(((long[]) value).length << 5); // mul 32
            } else if (value instanceof short[]) {
                dynamicLengthStack.push(((short[]) value).length << 5); // mul 32
            } else if(value instanceof boolean[]) {
                dynamicLengthStack.push(((boolean[]) value).length << 5); // mul 32
            }
        } else if (value instanceof Number) {
            dynamicLengthStack.push(32);
        } else if(value instanceof Boolean) {
            dynamicLengthStack.push(32);
        } else if(value instanceof Tuple) {
            buildByteLenStack(((Tuple) value).elements, dynamicLengthStack);
        } else {
            // shouldn't happen if type checks/validation already occurred
            throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
        }
    }
}
