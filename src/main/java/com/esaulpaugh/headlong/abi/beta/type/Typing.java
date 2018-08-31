package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.Queue;

import static com.esaulpaugh.headlong.abi.beta.type.Byte.BYTE_PRIMITIVE;
import static com.esaulpaugh.headlong.abi.beta.type.DynamicArray.DYNAMIC_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;

abstract class Typing {

    private static final String CLASS_NAME_BOOLEAN = Boolean.class.getName();
    static final String CLASS_NAME_BYTE = java.lang.Byte.class.getName();
    private static final String CLASS_NAME_SHORT = Short.class.getName();
    private static final String CLASS_NAME_INT = Integer.class.getName();
    private static final String CLASS_NAME_LONG = Long.class.getName();

    private static final String CLASS_NAME_BIG_INTEGER = BigInteger.class.getName();
    private static final String CLASS_NAME_BIG_DECIMAL = BigDecimal.class.getName();
    private static final String CLASS_NAME_STRING = String.class.getName();
    private static final String CLASS_NAME_TUPLE = com.esaulpaugh.headlong.abi.beta.util.Tuple.class.getName();

    private static final String CLASS_NAME_ELEMENT_BOOLEAN = boolean[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_BYTE = byte[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_SHORT = short[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_INT = int[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_LONG = long[].class.getName().replaceFirst("\\[", "");

    private static final String CLASS_NAME_ELEMENT_BIG_INTEGER = BigInteger[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_BIG_DECIMAL = BigDecimal[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_STRING = String[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_TUPLE = com.esaulpaugh.headlong.abi.beta.util.Tuple[].class.getName().replaceFirst("\\[", "");

    private static final String CLASS_NAME_ARRAY_BYTE = byte[].class.getName();

    static StackableType createForTuple(String canonicalAbiType, Tuple baseTuple) {
        if(baseTuple == null) {
            throw new NullPointerException();
        }
        return create(canonicalAbiType, baseTuple);
    }

    static StackableType create(String canonicalAbiType) {
        return create(canonicalAbiType, null);
    }

    private static StackableType create(String canonicalAbiType, Tuple baseTuple) {
        Deque<StackableType> typeStack = new ArrayDeque<>();
        buildTypeStack(canonicalAbiType, canonicalAbiType.length() - 1, typeStack, new StringBuilder(), baseTuple);
        return typeStack.peek();
    }

//    private static String buildClassName(ArrayDeque<StackableType> typeStack, String javaBaseType) {
//        StringBuilder classNameBuilder = new StringBuilder();
//        final int depth = typeStack.size() - 1;
//        for (int i = 0; i < depth; i++) {
//            classNameBuilder.append('[');
//        }
//        return classNameBuilder.append(javaBaseType).toString();
//    }

    private static String buildTypeStack(final String canonicalAbiType,
                                         final int index,
                                         final Deque<StackableType> typeStack,
                                         final StringBuilder brackets,
                                         final StackableType baseTuple) {

        if(canonicalAbiType.charAt(index) == ']') {

            final int fromIndex = index - 1;
            final int arrayOpenIndex = canonicalAbiType.lastIndexOf('[', fromIndex);

            final String javaBaseType = buildTypeStack(canonicalAbiType, arrayOpenIndex - 1, typeStack, brackets, baseTuple);

            brackets.append('[');
            final String className = brackets.toString() + javaBaseType; // results.second;

            if(arrayOpenIndex == fromIndex) { // []
                typeStack.push(new DynamicArray(canonicalAbiType, className, typeStack.peek(), DYNAMIC_LENGTH));
            } else { // [...]
                final int length = Integer.parseUnsignedInt(canonicalAbiType.substring(arrayOpenIndex + 1, index));
                final StackableType top = typeStack.peek();
                if(top == null) { // should never happen
                    throw new EmptyStackException();
                }
                if(top.dynamic) {
                    typeStack.push(new DynamicArray(canonicalAbiType, className, top, length));
                } else {
                    typeStack.push(new StaticArray(canonicalAbiType, className, top, length));
                }
            }

            return javaBaseType;
        } else {
            final String abiBaseType = canonicalAbiType.substring(0, index + 1);
            String javaBaseType;
            try {
                boolean isElement = index != canonicalAbiType.length() - 1;
                javaBaseType = getJavaBaseTypeName(abiBaseType, isElement, typeStack, baseTuple);
            } catch (NumberFormatException nfe) {
                javaBaseType = null;
            }
            if(javaBaseType == null) {
                throw new IllegalArgumentException("unrecognized type: " + abiBaseType + " (" + String.format("%040x", new BigInteger(abiBaseType.getBytes(UTF_8))) + ")");
            }
            return javaBaseType;
//            return new Pair<>(abiBaseType, javaBaseType);
        }
    }

    private static String getJavaBaseTypeName(final String abi, boolean isElement, Deque<StackableType> typeStack, StackableType baseTuple) {

        if(abi.isEmpty()) {
            return null;
        }

//        int bits = Integer.parseUnsignedInt(abiBaseType, "uint".length(), abiBaseType.length(), 10); // Java 9

        final String className;

        // ~5,220 possible base types (mostly (u)fixedMxN)
        if (abi.charAt(0) == '(') {
//            if(baseTuple == null) {
//                throw new NullPointerException("baseTuple is null");
//            }
            typeStack.push(baseTuple);
            return isElement ? CLASS_NAME_ELEMENT_TUPLE : CLASS_NAME_TUPLE;
        } else if ("bool".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
            typeStack.push(Byte.booleanType(abi, className)); // new Byte(abi, className));
        } else if ("address".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
            typeStack.push(new StaticArray(abi, className, BYTE_PRIMITIVE, 20));
        } else if (abi.startsWith("uint")) {
            if (abi.length() == "uint".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("uint".length()), 10);
            Pair<String, Int256> pair = makeInt(abi, bits, isElement);
            className = pair.first;
            typeStack.push(pair.second);
        } else if (abi.startsWith("int")) {
            if (abi.length() == "int".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("int".length()), 10);
            Pair<String, Int256> pair = makeInt(abi, bits, isElement);
            className = pair.first;
            typeStack.push(pair.second);
        } else if (abi.startsWith("ufixed")) {
            if (abi.length() == "ufixed".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("ufixed".length(), abi.indexOf('x', "ufixed".length())), 10);
            className = isElement ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
            typeStack.push(new Int256(abi, className, bits));
        } else if (abi.startsWith("fixed")) {
            if (abi.length() == "fixed".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("fixed".length(), abi.indexOf('x', "fixed".length())), 10);
            className = isElement ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
            typeStack.push(new Int256(abi, className, bits));
        } else if ("function".equals(abi)) {
            className = CLASS_NAME_ARRAY_BYTE;
            typeStack.push(new StaticArray(abi, className, BYTE_PRIMITIVE, 24));
        } else if (abi.startsWith("bytes")) {
            className = CLASS_NAME_ARRAY_BYTE;
            if (abi.length() > "bytes".length()) {
                int bytes = Integer.parseUnsignedInt(abi.substring("bytes".length()), 10);
                typeStack.push(new StaticArray(abi, className, BYTE_PRIMITIVE, bytes));
            } else {
                typeStack.push(new DynamicArray(abi, className, BYTE_PRIMITIVE, DYNAMIC_LENGTH));
            }
        } else if ("string".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
            typeStack.push(new DynamicArray(abi, CLASS_NAME_STRING, BYTE_PRIMITIVE, DYNAMIC_LENGTH));
        } else {
            throw new IllegalArgumentException("unrecognized type: " + abi);
        }

        return className;
    }

    private static Pair<String, Int256> makeInt(String abi, int bits, boolean isElement) {
        String className;
        Int256 integer;
        if (bits > 64) {
            className = isElement ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
            integer = new Int256(abi, CLASS_NAME_BIG_INTEGER, bits);
        } else if (bits > 32) {
            className = isElement ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
            integer = new Int256(abi, CLASS_NAME_LONG, bits);
        } else if (bits > 16) {
            className = isElement ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
            integer = new Int256(abi, CLASS_NAME_INT, bits);
        } else if (bits > 8) {
            className = isElement ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
            integer = new Int256(abi, CLASS_NAME_SHORT, bits);
        } else {
            className = isElement ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE;
            integer = new Int256(abi, CLASS_NAME_BYTE, bits);
        }
        return new Pair<>(className, integer);

    }
}
