package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

// TODO redesign
class ArrayType extends Type {

//    private final Type[] elementTypes; // TODO init each as dynamic or not

    private transient final Type baseType; // TODO needed?
    private transient final Stack<Integer> fixedLengthStack; // TODO needed?
    private transient final int arrayDepth; // TODO needed?

    private ArrayType(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int arrayDepth, boolean dynamic) {
        super(canonicalAbiType, javaClassName, dynamic);
        this.baseType = Type.create(abiBaseType);
        this.fixedLengthStack = fixedLengthStack;
        this.arrayDepth = arrayDepth;
    }

    // TODO redesign
    static ArrayType create(String canonicalAbiType, String abiBaseType, String javaClassName, Stack<Integer> fixedLengthStack, int depth) {
        Integer baseTypeByteLen;
        int byteLen;

        boolean dynamic = fixedLengthStack.contains(null);
        if(!dynamic) {
            baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1); // e.g. uint8[2] --> 1, uint16[2] --> 32?

            if(baseTypeByteLen == 1
                    && !canonicalAbiType.equals("bytes1")
                    && !canonicalAbiType.equals("bool")) { // int8, uint8
                depth--;
            }

            int product = roundUp(baseTypeByteLen);
            StringBuilder sb = new StringBuilder("(" + baseTypeByteLen + " --> " + product + ")");
            for (int i = depth - 1; i >= 0; i--) {
                product *= fixedLengthStack.get(i);
                sb.append(" * ").append(fixedLengthStack.get(i));
            }
            byteLen = product;
            System.out.println(canonicalAbiType + " : static len: " + sb.toString() + " = " + byteLen);
        }

        if(abiBaseType.equals("bool")) {
            canonicalAbiType = canonicalAbiType.replace("bool", "uint8");
            abiBaseType = "uint8";
        } else if (abiBaseType.equals("string")
                || abiBaseType.equals("function")
                || abiBaseType.startsWith("bytes")) {
            abiBaseType = "uint8";
        }

        return new ArrayType(canonicalAbiType, abiBaseType, javaClassName, fixedLengthStack, depth, dynamic);
    }

    private static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 - mod);
    }

    @Override
    protected void validate(final Object value, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(value, expectedClassName, expectedLengthIndex);

        if(value.getClass().isArray()) {
            if(value instanceof Object[]) { // includes BigInteger[]
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
//        Stack<Integer> dynamicByteLenStack = new Stack<>();
//        buildByteLenStack(value, dynamicByteLenStack);

        int len = getLength(value);
        System.out.println("len :: " + len);
        return len;

//        return dynamic
//                ? 32 + 32 + len
//                : len;


//        int dataLen = buildDataLen(value);
//        System.out.println("dataLen = " + dataLen);
//
////        if (value instanceof String) {
////            int len = ((String) value).length();
////            System.out.println("len = " + len);
////            dataLen += 32 + 32 + roundUp(((String) value).length());
////        } else if (value.getClass().isArray()) {
////            if (value instanceof Object[]) {
////                if (value instanceof BigInteger[]) {
////                    dataLen += ((Object[]) value).length;
////                } else {
////                    Object[] arr = (Object[]) value;
////                    int numDynamicElements = arr.length;
////                    int n = 64 * numDynamicElements;
////                    System.out.println("overhead = " + n);
////
////                    final int size = fixedLengthStack.size();
////                    for (int i = 0; i < size; i++) {
////                        Integer fixedLen = fixedLengthStack.get(i);
////                        System.out.println(i + ": " + fixedLen);
////                        if(fixedLen != null) {
////                            System.out.println(i + " static");
////                        }
////                    }
////                }
////            } else if (value instanceof byte[]) {
////                dataLen += ((byte[]) value).length;
////            } else if (value instanceof int[]) {
////                dataLen += ((int[]) value).length << 5;
////            } else if (value instanceof long[]) {
////                dataLen += ((long[]) value).length << 5;
////            } else if (value instanceof short[]) {
////                dataLen += ((short[]) value).length << 5;
////            } else if (value instanceof boolean[]) {
////                dataLen += ((boolean[]) value).length;
////            }
////        } else {
////            throw new AssertionError("bad type");
////        }
//
////        new BigInteger[][]{
////                new BigInteger[]{
////                        BigInteger.ONE,
////                        BigInteger.valueOf(2L),
////                },
////                new BigInteger[]{
////                        BigInteger.valueOf(3L)
////                }
////        },
////        new String[] { "one", "two", "three" }
//
//        if(value instanceof Object[]) {
//            for (int i = 0; i < fixedLengthStack.size(); i++) {
//                System.out.println("stack " + i + " : " + fixedLengthStack.get(i));
//            }
//
//            int product = 1;
//            int i = arrayDepth;
////        boolean fixed = true;
//            while (i >= 0) {
//                Integer fixedLen = fixedLengthStack.get(i);
//                if (fixedLen != null) {
//                    System.out.println(i + " : " + fixedLen);
//                    product *= fixedLen;
//                    i--;
//                } else {
//                    break;
//                }
//            }
//
//            System.out.println("product = " + product);
//
//            System.out.println("first non fixed = " + i + " " + fixedLengthStack.get(i));
//
////            Stack<String> overheadStack = new Stack<>();
////            int overhead = getOverhead((Object[][]) value, 1, fixedLengthStack.size() - 2);
//
//
//
//
////            int dynamicLen = getLen((Object[]) value, i);
////            System.out.println(product + " * " + dynamicLen);
////
////            int innerTotal = product * dynamicLen;
////            System.out.println("innerTotal = " + innerTotal);
////
////            int total = 32 + ((Object[]) value).length * 32 + innerTotal;
////            System.out.println("total = " + total);
//        }
//
////        for (int i = arrayDepth; i >= 0; i--) { // arrayDepth - 1
////            int len;
////            Integer fixedLen = fixedLengthStack.get(i);
////            if(fixedLen != null) {
////                len = fixedLen;
////                n *= len;
////            } else {
////                len = getLen(value);
////                n += len;
////            }
////        }
//
//        int n = dataLen;
//
//        return dynamic
//                ? 32 + 32 + roundUp(n)
//                : roundUp(n);
    }

    private int getLength(Object value) {
        if(value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            return 64 * arr.length + getLength(arr, 0, arrayDepth - 1, fixedLengthStack);
        }
        return getDataLen(value, dynamic);
    }

    private static int getLength(Object[] value, final int i, final int n, Stack<Integer> staticLengthStack) {
        final boolean dynamic = staticLengthStack.get(i) == null;

        if (i >= n) {
            final int baseLen = dynamic ? 64 * value.length : 0;
            String str = i + " >= " + n + ", baseLen = " + baseLen;
            System.out.print(str);
            int len = baseLen;
            for(Object obj : value) {
                len += getDataLen(obj, dynamic);
            }
            System.out.println(", len = " + len);
            return len;
        }
        final int baseLen = dynamic ? 64 * value.length : 0;
        String str = i + "(baseLen = " + baseLen;
        System.out.println(str);
        int len = baseLen;
        for (Object[] arr : (Object[][]) value) {
            len += getLength(arr, i + 1, n, staticLengthStack);
        }
        System.out.println("len = " + len + ")" + i);
        return len;


////        int dynamicCount = 0;
////        int staticCount = 0;
//        int overhead;
//        if(staticLen == null) {
//            overhead = 64 * value.length;
//            System.out.println("before " + i + ": " + overhead);
//            for (Object[] arr : (Object[][]) value) {
//                overhead += getLength(arr, i + 1, n, staticLengthStack);
//            }
//            System.out.println("after " + i + ": " + overhead);
//        } else {
//            overhead = 0;
//            System.out.println(i +  " static");
//        }
//        return overhead;
    }

    private int getOverhead(Object[] value) {
        return 64 * value.length + getOverhead(value, 0, arrayDepth - 1, fixedLengthStack);
    }

    private static int getOverhead(Object[] value, final int i, final int n, Stack<Integer> staticLengthStack) {
        if (i >= n) {
            int overhead = 64 * value.length;
            String str = i + " >= " + n + " | " + 64 + " * " + value.length + " = " + overhead;
            System.out.println(str);
            return overhead;
        }
//        int dynamicCount = 0;
//        int staticCount = 0;
        int overhead;
        if(staticLengthStack.get(i) == null) {
            overhead = 64 * value.length;
            System.out.println("before " + i + ": " + overhead);
            for (Object[] arr : (Object[][]) value) {
                overhead += getOverhead(arr, i + 1, n, staticLengthStack);
            }
            System.out.println("after " + i + ": " + overhead);
        } else {
            overhead = 0;
            System.out.println(i +  " static");
        }

        return overhead;
    }

    // TODO
    private static int getLen(Object[] value, int level) {
        System.out.println("getLen(" + level + ")");
        if(level == 0) {
            return value.length;
        }
        if(level == 1) {
            int dataLen = 0;
            if(value instanceof String[]) {
                int dynamicOverhead = 0;
                for(String s : (String[]) value) {
                    dynamicOverhead += 64;
                    dataLen += roundUp(s.length());
                }
                return dynamicOverhead + dataLen;
//                levelLen =  ((String[]) value).length;
//                System.out.println("levelLen raw = " + levelLen);
//                return levelLen;
//                return roundUp(levelLen);
            }
            for (Object[] arr : ((Object[][]) value)) {
                dataLen += arr.length;
            }
            return dataLen;
        }
        if(level == 2) {
            int levelLen = 0;
            for (Object[][] arr : ((Object[][][]) value)) {
                for (Object[] arr2 : arr) {
                    levelLen += arr2.length;
                }
            }
            return levelLen;
        }
        return -1;
    }

//    private static int getLen(Object value, int i) {
//        if (value instanceof String) {
//            int len = ((String) value).length();
//            System.out.println("len = " + len);
//            return roundUp(len);
//        } else if (value.getClass().isArray()) {
//            if (value instanceof Object[]) {
//                if (value instanceof BigInteger[]) {
//                    return ((Object[]) value).length;
//                } else {
//                    Object[] arr = (Object[]) value;
//
////                    int numDynamicElements = arr.length;
////                    int n = 64 * numDynamicElements;
////                    System.out.println("overhead = " + n);
//
//                    return arr.length;
//
////                    final int size = fixedLengthStack.size();
////                    for (int i = 0; i < size; i++) {
////                        Integer fixedLen = fixedLengthStack.get(i);
////                        System.out.println(i + ": " + fixedLen);
////                        if(fixedLen != null) {
////                            System.out.println(i + " static");
////                        }
////                    }
//                }
//            } else if (value instanceof byte[]) {
//                return ((byte[]) value).length;
//            } else if (value instanceof int[]) {
//                return ((int[]) value).length << 5;
//            } else if (value instanceof long[]) {
//                return ((long[]) value).length << 5;
//            } else if (value instanceof short[]) {
//                return ((short[]) value).length << 5;
//            } else if (value instanceof boolean[]) {
//                return ((boolean[]) value).length;
//            }
//        }
//        throw new AssertionError("bad type");
//    }

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

    private static int getDataLen(Object value, boolean dynamic) {
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
                return ((long[]) value).length << 5; // mul 32
            }
            if (value instanceof short[]) {
                return ((short[]) value).length << 5; // mul 32
            }
            if (value instanceof boolean[]) {
                return ((boolean[]) value).length << 5; // mul 32
            }
            if (value instanceof Number[]) {
                return ((Number[]) value).length << 5; // mul 32
            }
        }
        if (value instanceof String) { // always needs dynamic head?
            return 64 + roundUp(((String) value).length());
        }
        if (value instanceof Number) {
            return 32;
        }
        if (value instanceof Tuple) {
            throw new RuntimeException("arrays of tuples not yet supported"); // TODO **************************************
        }
//        if (value instanceof Object[]) {
//            throw new AssertionError("Object array not expected here");
//        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    private static int buildDataLen(Object value) {
        int dataLen = 0;
        if(value instanceof String) {
            int len = ((String) value).length();
            System.out.println("strlen = " + len);
            dataLen += 32 + 32 + roundUp(((String) value).length());
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    dataLen += ((Object[]) value).length;
                } else {
                    Object[] arr = (Object[]) value;
//                    dynamicLengthStack.push(arr.length);
                    for (Object obj : arr) {
                        dataLen += buildDataLen(obj);
                    }
                }
            } else if (value instanceof byte[]) {
                dataLen += ((byte[]) value).length;
//                dynamicLengthStack.push(roundUp();
            } else if (value instanceof int[]) {
                dataLen += ((int[]) value).length << 5;
//                dynamicLengthStack.push(((int[]) value).length << 5); // mul 32
            } else if (value instanceof long[]) {
                dataLen += ((long[]) value).length << 5;
//                dynamicLengthStack.push(((long[]) value).length << 5); // mul 32
            } else if (value instanceof short[]) {
                dataLen += ((short[]) value).length << 5;
//                dynamicLengthStack.push(((short[]) value).length << 5); // mul 32
            } else if(value instanceof boolean[]) {
                dataLen += ((boolean[]) value).length;
//                dynamicLengthStack.push(((boolean[]) value).length);
            }
        } else if (value instanceof Number) {
            dataLen += 32;
//            dynamicLengthStack.push(32);
        } else if(value instanceof Boolean) {
            dataLen += 32;
//            dynamicLengthStack.push(32);
        } else if(value instanceof Tuple) {
            for(Object e : ((Tuple) value).elements) {
                dataLen += buildDataLen(e);
            }
//            buildByteLenStack(((Tuple) value).elements, dynamicLengthStack);
        } else {
            // shouldn't happen if type checks/validation already occurred
            throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
        }
        return dataLen;
    }
}
