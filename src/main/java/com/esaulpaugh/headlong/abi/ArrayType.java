package com.esaulpaugh.headlong.abi;

import java.util.Stack;

public class ArrayType extends Type {

    protected transient final String abiBaseType;
    protected transient final Stack<Integer> fixedLengthStack;
    protected transient final int arrayDepth;
    protected transient final Integer baseTypeByteLen;

    protected ArrayType(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int arrayDepth, Integer baseTypeByteLen, boolean dynamic) {
        super(canonicalAbiType, javaClassName, dynamic);
        this.abiBaseType = abiBaseType;
        this.fixedLengthStack = fixedLengthStack;
        this.arrayDepth = arrayDepth;
        this.baseTypeByteLen = baseTypeByteLen;
    }

    static ArrayType create(String canonicalAbiType, String abiBaseType, String javaClassName, String javaBaseType, Stack<Integer> fixedLengthStack, int depth) {
        Integer baseTypeByteLen;
        Integer byteLen;

        boolean dynamic = fixedLengthStack.contains(null);
        if(dynamic) {
            switch (javaBaseType) {
            case "B":
            case "[B":
            case "java.lang.String":
                baseTypeByteLen = 1; break;
            default: baseTypeByteLen = null;
            }
            byteLen = null;
        } else { // static
            baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1);

            int rounded = roundUp(baseTypeByteLen);

            if(baseTypeByteLen == 1 && !canonicalAbiType.startsWith("bytes1")) { // typeString.startsWith("int8") || typeString.startsWith("uint8")
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

        return new ArrayType(canonicalAbiType, abiBaseType, javaClassName, fixedLengthStack, depth, baseTypeByteLen, dynamic);
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
            throw new IllegalArgumentException("array length mismatch: " + actual + " != " + expected);
        }
        System.out.println("fixed length valid;");
    }

    @Override
    public Integer getByteLen() {
        return null;
    }

    @Override
    public int calcDynamicByteLen(Object param) {
        Stack<Integer> dynamicLengthStack = new Stack<>();
        buildLengthStack(param, dynamicLengthStack);

        int dynamicLen = 1;
        for (int i = arrayDepth - 1; i >= 0; i--) {
            int len;
            Integer fixedLen = fixedLengthStack.get(i);
            if(fixedLen != null) {
                len = fixedLen;
            } else {
                len = dynamicLengthStack.get(i);
            }
            dynamicLen *= len;
        }

        return roundUp(32 + 32 + dynamicLen);
    }

    private void buildLengthStack(Object value, Stack<Integer> dynamicLengthStack) {
        if(value instanceof String) {
            dynamicLengthStack.push(roundUp(((String) value).length()));
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                Object[] arr = (Object[]) value;
                for (Object obj : arr) {
                    buildLengthStack(obj, dynamicLengthStack);
                }
            } else if (value instanceof byte[]) {
                dynamicLengthStack.push(roundUp(((byte[]) value).length));
            } else if (value instanceof int[]) {
                dynamicLengthStack.push(((int[]) value).length << 5); // mul 32
            } else if (value instanceof long[]) {
                dynamicLengthStack.push(((long[]) value).length << 5);
            } else if (value instanceof short[]) {
                dynamicLengthStack.push(((short[]) value).length << 5);
            } else if(value instanceof boolean[]) {
                dynamicLengthStack.push(((boolean[]) value).length << 5);
            }
        } else if (value instanceof Number) {
            dynamicLengthStack.push(32);
        } else if(value instanceof Boolean) {
            dynamicLengthStack.push(32);
        } else if(value instanceof Tuple) {
            throw new AssertionError("override expected");
//            dynamicLengthStack.push(((Tuple) value).byteLen);
        } else {
            // shouldn't happen if type checks/validation already occurred
            throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
        }
    }
}
