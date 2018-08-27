package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigInteger;
import java.util.Stack;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class Typing {

    static StackableType createForTuple(String canonicalAbiType, Tuple baseTuple) {

        Stack<StackableType> typeStack = new Stack<>();
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
        Stack<StackableType> typeStack = new Stack<>();
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

    private static String buildClassName(Stack<StackableType> typeStack, String javaBaseType) {
        StringBuilder classNameBuilder = new StringBuilder();
        final int depth = typeStack.size() - 1;
        for (int i = 0; i < depth; i++) {
            classNameBuilder.append('[');
        }
        return classNameBuilder.append(javaBaseType).toString();
    }

    private static Pair<String, String> buildTypeStack(String canonicalAbiType, Stack<StackableType> typeStack, StackableType baseTuple) {
        StringBuilder brackets = new StringBuilder();
        return buildTypeStack(canonicalAbiType, canonicalAbiType.length() - 1, typeStack, brackets, baseTuple);
    }

    private static Pair<String, String> buildTypeStack(String canonicalAbiType, final int i, Stack<StackableType> typeStack, StringBuilder brackets, StackableType baseTuple) {

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
                javaBaseType = StackableType.getJavaBaseTypeName(abiBaseType, isElement, typeStack, baseTuple);
            } catch (NumberFormatException nfe) {
                javaBaseType = null;
            }
            if(javaBaseType == null) {
                throw new IllegalArgumentException("unrecognized type: " + abiBaseType + " (" + String.format("%040x", new BigInteger(abiBaseType.getBytes(UTF_8))) + ")");
            }
            return new Pair<>(abiBaseType, javaBaseType);
        }
    }
}
