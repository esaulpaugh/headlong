package com.esaulpaugh.headlong.abi;

import sun.nio.cs.UTF_8;

import java.util.Stack;

public class ArrayType extends Type {

    protected transient final Type baseType;
    protected transient final Stack<Integer> fixedLengthStack;
    protected transient final int arrayDepth;
    protected transient final Integer baseTypeByteLen;

    protected ArrayType(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int arrayDepth, Integer baseTypeByteLen, boolean dynamic) {
        super(canonicalAbiType, javaClassName, dynamic);
        this.baseType = Type.create(abiBaseType);
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
            baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1); // e.g. uint8[2] --> 1, uint16[2] --> 32?

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

        if(abiBaseType.equals("string")
                || abiBaseType.equals("function")
                || (abiBaseType.startsWith("bytes"))) { //  && abiBaseType.length() > "bytes".length()
            abiBaseType = "uint8";
        }

        return new ArrayType(canonicalAbiType, abiBaseType, javaClassName, fixedLengthStack, depth, baseTypeByteLen, dynamic);
    }

    @Override
    protected void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(param, expectedClassName, expectedLengthIndex);

        if(param.getClass().isArray()) {
            if(param instanceof Object[]) {
                validateArray((Object[]) param, expectedClassName, expectedLengthIndex);
            } else if (param instanceof byte[]) {
                validateByteArray((byte[]) param, expectedLengthIndex);
            } else if (param instanceof int[]) {
                validateIntArray((int[]) param, expectedLengthIndex);
            } else if (param instanceof long[]) {
                validateLongArray((long[]) param, expectedLengthIndex);
            } else if (param instanceof short[]) {
                validateShortArray((short[]) param, expectedLengthIndex);
            } else if (param instanceof boolean[]) {
                validateBooleanArray((boolean[]) param, expectedLengthIndex);
            }
        } else if(param instanceof String) {
            validateByteArray(((String) param).getBytes(UTF_8.INSTANCE), expectedLengthIndex);
        } else if(param instanceof Number) {
            NumberType._validateNumber(param, ((NumberType) baseType).bitLimit);
        } else if(param instanceof Boolean) {
            super.validate(param, CLASS_NAME_BOOLEAN, expectedLengthIndex);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + param.getClass().getName());
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
    public Integer getDataByteLen(Object param) {
        Stack<Integer> dynamicByteLenStack = new Stack<>();
        buildByteLenStack(param, dynamicByteLenStack);

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

    @Override
    public Integer getNumElements(Object value) {
        return _getNumElements(value);
    }

    protected static int _getNumElements(Object value) {
        if (value instanceof String) {
            return ((String) value).length();
        }
        if (value instanceof Number[]) {
            return ((Number[]) value).length;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).length;
        }
        if (value instanceof int[]) {
            return ((int[]) value).length;
        }
        if (value instanceof long[]) {
            return ((long[]) value).length;
        }
        if (value instanceof short[]) {
            return ((short[]) value).length;
        }
        if (value instanceof boolean[]) {
            return ((boolean[]) value).length;
        }
        if (value instanceof Tuple[]) {
            return ((Tuple[]) value).length;
        }
        if (value instanceof Object[]) {
            return ((Object[]) value).length;
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

//    @Override
//    public int calcDynamicByteLen(Object param) {
//
//    }
//                int numElements = 0;
//                Object[] arr = (Object[]) value;
//                for (Object obj : arr) {
//                    numElements += calcNumElements(obj);
//                }
//                return numElements;
//        }
//         if(value instanceof Tuple) {
//            return calcNumElements(((Tuple) value).elements);
////            throw new AssertionError("override expected");
////            dynamicLengthStack.push(((Tuple) value).byteLen);
//        }
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
//            throw new AssertionError("override expected");
//            dynamicLengthStack.push(((Tuple) value).byteLen);
        } else {
            // shouldn't happen if type checks/validation already occurred
            throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
        }
    }

//    protected static int getBaseBitLen(String abiBaseType) {
//        private static String getJavaBaseTypeName(String abiBaseType) {
//
//            int
//            if(abiBaseType.startsWith("int")) {
//                return Integer.parseInt(abiBaseType.substring(abiBaseType.lastIndexOf("int") + "int".length()));
//            }
//        }
//
//            switch (abiBaseType) {
//            case "uint8": return
//            case "uint16": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
//            case "uint24":
//            case "uint32": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
//            case "uint40":
//            case "uint48":
//            case "uint56":
//            case "uint64": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
//            case "uint72":
//            case "uint80":
//            case "uint88":
//            case "uint96":
//            case "uint104":
//            case "uint112":
//            case "uint120":
//            case "uint128":
//            case "uint136":
//            case "uint144":
//            case "uint152":
//            case "uint160":
//            case "uint168":
//            case "uint176":
//            case "uint184":
//            case "uint192":
//            case "uint200":
//            case "uint208":
//            case "uint216":
//            case "uint224":
//            case "uint232":
//            case "uint240":
//            case "uint248":
//            case "uint256": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
//            case "int8": fixedLengthStack.push(1); return element ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE; // signed // TODO
//            case "int16": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
//            case "int24":
//            case "int32": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
//            case "int40":
//            case "int48":
//            case "int56":
//            case "int64": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
//            case "int72":
//            case "int80":
//            case "int88":
//            case "int96":
//            case "int104":
//            case "int112":
//            case "int120":
//            case "int128":
//            case "int136":
//            case "int144":
//            case "int152":
//            case "int160":
//            case "int168":
//            case "int176":
//            case "int184":
//            case "int192":
//            case "int200":
//            case "int208":
//            case "int216":
//            case "int224":
//            case "int232":
//            case "int240":
//            case "int248":
//            case "int256":
//            case "address": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
//            case "bytes1": fixedLengthStack.push(1); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes2": fixedLengthStack.push(2); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes3": fixedLengthStack.push(3); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes4": fixedLengthStack.push(4); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes5": fixedLengthStack.push(5); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes6": fixedLengthStack.push(6); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes7": fixedLengthStack.push(7); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes8": fixedLengthStack.push(8); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes9": fixedLengthStack.push(9); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes10": fixedLengthStack.push(10); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes11": fixedLengthStack.push(11); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes12": fixedLengthStack.push(12); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes13": fixedLengthStack.push(13); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes14": fixedLengthStack.push(14); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes15": fixedLengthStack.push(15); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes16": fixedLengthStack.push(16); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes17": fixedLengthStack.push(17); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes18": fixedLengthStack.push(18); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes19": fixedLengthStack.push(19); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes20": fixedLengthStack.push(20); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes21": fixedLengthStack.push(21); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes22": fixedLengthStack.push(22); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes23": fixedLengthStack.push(23); return CLASS_NAME_ARRAY_BYTE;
//            case "function":
//            case "bytes24": fixedLengthStack.push(24); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes25": fixedLengthStack.push(25); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes26": fixedLengthStack.push(26); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes27": fixedLengthStack.push(27); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes28": fixedLengthStack.push(28); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes29": fixedLengthStack.push(29); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes30": fixedLengthStack.push(30); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes31": fixedLengthStack.push(31); return CLASS_NAME_ARRAY_BYTE;
//            case "bytes32": fixedLengthStack.push(32); return CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ELEMENT_ARRAY_BYTE;
//            case "bool": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
//            case "bytes": /* dynamic*/
//                fixedLengthStack.push(null);
//                return CLASS_NAME_ARRAY_BYTE;
//            case "string": /* dynamic*/
//                fixedLengthStack.push(null);
//                return element ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
//            case "fixed": throw new IllegalArgumentException("fixed not supported. use fixed128x18");
//            case "ufixed": throw new IllegalArgumentException("ufixed not supported. use ufixed128x18");
//            case "int": throw new IllegalArgumentException("int not supported. use int256");
//            case "uint": throw new IllegalArgumentException("uint not supported. use uint256");
//            default: {
//                if(abiBaseType.contains("fixed")) {
//                    fixedLengthStack.push(32);
//                    return element ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
//                }
//                return null;
//            }
//            }
//    }
}
