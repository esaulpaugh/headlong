package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.EmptyStackException;

import static com.esaulpaugh.headlong.abi.beta.type.Byte.BYTE_PRIMITIVE;
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

    // basically an unsynchronized Stack
    private static class TypeStack extends ArrayDeque<StackableType> {

        @Override
        public StackableType peek() {
            StackableType e = peekFirst();
            if(e == null) {
                throw new EmptyStackException();
            }
            return e;
        }

    }

    static StackableType createForTuple(String canonicalAbiType, Tuple baseTuple) {

        TypeStack typeStack = new TypeStack();
        Pair<String, String> results = buildTypeStack(canonicalAbiType, typeStack, baseTuple);

        return typeStack.peek();

//        StackableType type = create(abi);
//        return type;
//        if(elementType.dynamic) {
//            return new DynamicArray(null, null, elementType);
//        }
//        return new StaticArray(null, null, elementType, len);
    }

    static StackableType create(String canonicalAbiType) {
        TypeStack typeStack = new TypeStack();
        Pair<String, String> results = buildTypeStack(canonicalAbiType, typeStack, null);

        String abiBaseType = results.first;
        String javaBaseType = results.second;

        String className = buildClassName(typeStack, javaBaseType);

//        int i = 0;
//        for(StackableType stackable : typeStack) {
//            System.out.println(i++ + " " + stackable);
//        }

        return typeStack.peek();
    }

    private static String buildClassName(ArrayDeque<StackableType> typeStack, String javaBaseType) {
        StringBuilder classNameBuilder = new StringBuilder();
        final int depth = typeStack.size() - 1;
        for (int i = 0; i < depth; i++) {
            classNameBuilder.append('[');
        }
        return classNameBuilder.append(javaBaseType).toString();
    }

    private static Pair<String, String> buildTypeStack(String canonicalAbiType, TypeStack typeStack, StackableType baseTuple) {
        StringBuilder brackets = new StringBuilder();
        return buildTypeStack(canonicalAbiType, canonicalAbiType.length() - 1, typeStack, brackets, baseTuple);
    }

    @SuppressWarnings("null")
    private static Pair<String, String> buildTypeStack(String canonicalAbiType, final int i, TypeStack typeStack, StringBuilder brackets, StackableType baseTuple) {

//        if(i < 0) {
//            return null;
//        }

        if(canonicalAbiType.charAt(i) == ']') {

            final int arrayOpenIndex = canonicalAbiType.lastIndexOf('[', i - 1);

            Pair<String, String> results = buildTypeStack(canonicalAbiType, arrayOpenIndex - 1, typeStack, brackets, baseTuple);

//            if(typeStack.empty()) {
//
//            }

            brackets.append('[');
            final String className = brackets.toString() + results.second;

            if(arrayOpenIndex == i - 1) { // []
                typeStack.push(new DynamicArray(canonicalAbiType, className, typeStack.peek(), -1));
            } else { // [...]
                int length = Integer.parseUnsignedInt(canonicalAbiType.substring(arrayOpenIndex + 1, i));
                StackableType top = typeStack.peek();
//                int length = top instanceof Array ? ((Array) top).length : -1;
                if(typeStack.peek().dynamic) {
                    // TODO DynamicArray (e.g. [4] w/ dynamic element) can't enforce specified (top-level) len without length param?
                    typeStack.push(new DynamicArray(canonicalAbiType, className, top, length));
                } else {
                    typeStack.push(new StaticArray(canonicalAbiType, className, typeStack.peek(), length));
                }
            }

            return results;
        } else {
            String abiBaseType = canonicalAbiType.substring(0, i + 1);
            String javaBaseType;
            try {
                boolean isElement = i != canonicalAbiType.length() - 1;
                javaBaseType = getJavaBaseTypeName(abiBaseType, isElement, typeStack, baseTuple);
            } catch (NumberFormatException nfe) {
                javaBaseType = null;
            }
            if(javaBaseType == null) {
                throw new IllegalArgumentException("unrecognized type: " + abiBaseType + " (" + String.format("%040x", new BigInteger(abiBaseType.getBytes(UTF_8))) + ")");
            }
            return new Pair<>(abiBaseType, javaBaseType);
        }
    }

    private static String getJavaBaseTypeName(final String abi, boolean isElement, TypeStack typeStack, StackableType baseTuple) {

        if(abi.isEmpty()) {
            return null;
        }

//        int bits = Integer.parseUnsignedInt(abiBaseType, "uint".length(), abiBaseType.length(), 10); // Java 9

//        String bracketsString = brackets.toString();

        final String className;

        // ~5,220 possible base types (mostly (u)fixedMxN)
        if (abi.charAt(0) == '(') {

            typeStack.push(baseTuple);
            return isElement ? CLASS_NAME_ELEMENT_TUPLE : CLASS_NAME_TUPLE;

//            SignatureParser.parseTuple()
//            throw new IllegalArgumentException("can't create tuple this way");
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
                typeStack.push(new DynamicArray(abi, className, BYTE_PRIMITIVE, -1));
            }
        } else if ("string".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
            typeStack.push(new DynamicArray(abi, CLASS_NAME_STRING, BYTE_PRIMITIVE, -1));
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
