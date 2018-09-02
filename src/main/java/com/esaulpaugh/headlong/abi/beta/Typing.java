package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.type.TupleType;
import com.esaulpaugh.headlong.abi.beta.type.array.DynamicArrayType;
import com.esaulpaugh.headlong.abi.beta.type.array.StaticArrayType;
import com.esaulpaugh.headlong.abi.beta.type.integer.AbstractInt256Type;
import com.esaulpaugh.headlong.abi.beta.type.integer.BigDecimalType;
import com.esaulpaugh.headlong.abi.beta.type.integer.BigIntegerType;
import com.esaulpaugh.headlong.abi.beta.type.integer.BooleanType;
import com.esaulpaugh.headlong.abi.beta.type.integer.ByteType;
import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

import static com.esaulpaugh.headlong.abi.beta.type.array.DynamicArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.beta.type.integer.ByteType.BYTE_PRIMITIVE;
import static java.nio.charset.StandardCharsets.UTF_8;

abstract class Typing {

//    private static final String CLASS_NAME_BOOLEAN = Boolean.class.getName();
//    static final String CLASS_NAME_BYTE = java.lang.ByteType.class.getName();
//    private static final String CLASS_NAME_SHORT = Short.class.getName();
//    private static final String CLASS_NAME_INT = Integer.class.getName();
//    private static final String CLASS_NAME_LONG = Long.class.getName();

//    private static final String CLASS_NAME_BIG_INTEGER = BigInteger.class.getName();
//    private static final String CLASS_NAME_BIG_DECIMAL = BigDecimal.class.getName();
    private static final String CLASS_NAME_STRING = String.class.getName();
    private static final String CLASS_NAME_TUPLE = com.esaulpaugh.headlong.abi.beta.util.Tuple.class.getName();

//    private static final String CLASS_NAME_ELEMENT_BOOLEAN = boolean[].class.getName().replaceFirst("\\[", "");
//    private static final String CLASS_NAME_ELEMENT_BYTE = byte[].class.getName().replaceFirst("\\[", "");
//    private static final String CLASS_NAME_ELEMENT_SHORT = short[].class.getName().replaceFirst("\\[", "");
//    private static final String CLASS_NAME_ELEMENT_INT = int[].class.getName().replaceFirst("\\[", "");
//    private static final String CLASS_NAME_ELEMENT_LONG = long[].class.getName().replaceFirst("\\[", "");

//    private static final String CLASS_NAME_ELEMENT_BIG_INTEGER = BigInteger[].class.getName().replaceFirst("\\[", "");
//    private static final String CLASS_NAME_ELEMENT_BIG_DECIMAL = BigDecimal[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_STRING = String[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_TUPLE = com.esaulpaugh.headlong.abi.beta.util.Tuple[].class.getName().replaceFirst("\\[", "");

    private static final String CLASS_NAME_ARRAY_BYTE = byte[].class.getName();

    static StackableType createForTuple(String canonicalAbiType, TupleType baseTupleType) {
        if(baseTupleType == null) {
            throw new NullPointerException();
        }
        return create(canonicalAbiType, baseTupleType);
    }

    static StackableType create(String canonicalAbiType) {
        return create(canonicalAbiType, null);
    }

    private static StackableType create(String canonicalAbiType, TupleType baseTupleType) {
        Deque<StackableType> typeStack = new ArrayDeque<>();
        buildTypeStack(canonicalAbiType, canonicalAbiType.length() - 1, typeStack, new StringBuilder(), baseTupleType);
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
                typeStack.push(new DynamicArrayType(canonicalAbiType, className, typeStack.peek(), DYNAMIC_LENGTH));
            } else { // [...]
                final int length = Integer.parseUnsignedInt(canonicalAbiType.substring(arrayOpenIndex + 1, index));
                final StackableType top = typeStack.peek();
                if(top == null) { // should never happen
                    throw new EmptyStackException();
                }
                if(top.dynamic) {
                    typeStack.push(new DynamicArrayType(canonicalAbiType, className, top, length));
                } else {
                    typeStack.push(new StaticArrayType(canonicalAbiType, className, top, length));
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
            className = isElement ? BooleanType.CLASS_NAME_ELEMENT : BooleanType.CLASS_NAME;
            typeStack.push(new BooleanType(abi, className)); // com.esaulpaugh.headlong.abi.beta.type.integer.ByteType.booleanType(abi, className)); // new ByteType(abi, className));
        } else if ("address".equals(abi)) { // same as uint160
            className = isElement ? BigIntegerType.CLASS_NAME_ELEMENT : BigIntegerType.CLASS_NAME;
            typeStack.push(new BigIntegerType("uint160", BigIntegerType.CLASS_NAME, 160));
//            typeStack.push(new StaticArrayType(abi, className, BYTE_PRIMITIVE, 20));
        } else if (abi.startsWith("uint")) {
            if (abi.length() == "uint".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("uint".length()), 10);
            Pair<String, AbstractInt256Type> pair = AbstractInt256Type.makeInt(abi, bits, isElement);
            className = pair.first;
            typeStack.push(pair.second);
        } else if (abi.startsWith("int")) {
            if (abi.length() == "int".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("int".length()), 10);
            Pair<String, AbstractInt256Type> pair = AbstractInt256Type.makeInt(abi, bits, isElement);
            className = pair.first;
            typeStack.push(pair.second);
        } else if (abi.startsWith("ufixed")) {
            if (abi.length() == "ufixed".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            final int indexOfX = abi.indexOf('x', "ufixed".length());
            int bits = Integer.parseUnsignedInt(abi.substring("ufixed".length(), indexOfX), 10);
            int scale = Integer.parseUnsignedInt(abi.substring(indexOfX + 1), 10);
            className = isElement ? BigDecimalType.CLASS_NAME_ELEMENT : BigDecimalType.CLASS_NAME;
            typeStack.push(new BigDecimalType(abi, BigDecimalType.CLASS_NAME, bits, scale));
        } else if (abi.startsWith("fixed")) {
            if (abi.length() == "fixed".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            final int indexOfX = abi.indexOf('x', "fixed".length());
            int bits = Integer.parseUnsignedInt(abi.substring("fixed".length(), indexOfX), 10);
            int scale = Integer.parseUnsignedInt(abi.substring(indexOfX + 1), 10);
            className = isElement ? BigDecimalType.CLASS_NAME_ELEMENT : BigDecimalType.CLASS_NAME;
            typeStack.push(new BigDecimalType(abi, BigDecimalType.CLASS_NAME, bits, scale));
        } else if ("function".equals(abi)) {
            className = CLASS_NAME_ARRAY_BYTE;
            typeStack.push(new StaticArrayType<ByteType, Byte>(abi, className, BYTE_PRIMITIVE, 24));
        } else if (abi.startsWith("bytes")) {
            className = CLASS_NAME_ARRAY_BYTE;
            if (abi.length() > "bytes".length()) {
                int bytes = Integer.parseUnsignedInt(abi.substring("bytes".length()), 10);
                typeStack.push(new StaticArrayType<ByteType, Byte>(abi, className, BYTE_PRIMITIVE, bytes));
            } else {
                typeStack.push(new DynamicArrayType<ByteType, Byte>(abi, className, BYTE_PRIMITIVE, DYNAMIC_LENGTH));
            }
        } else if ("string".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
            typeStack.push(new DynamicArrayType<ByteType, Byte>(abi, CLASS_NAME_STRING, BYTE_PRIMITIVE, DYNAMIC_LENGTH));
        } else {
            throw new IllegalArgumentException("unrecognized type: " + abi);
        }

        return className;
    }
}
