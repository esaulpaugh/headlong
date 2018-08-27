package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigInteger;
import java.util.Stack;

import static java.nio.charset.StandardCharsets.UTF_8;

abstract class Typing {

    static StackableType create(String canonicalAbiType) {
        Stack<StackableType> typeStack = new Stack<>();
        Pair<String, String> results = buildTypeStack(canonicalAbiType, typeStack);

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

    private static Pair<String, String> buildTypeStack(String canonicalAbiType, Stack<StackableType> typeStack) {
        StringBuilder brackets = new StringBuilder();
        return buildTypeStack(canonicalAbiType, canonicalAbiType.length() - 1, typeStack, brackets);
    }

    private static Pair<String, String> buildTypeStack(String canonicalAbiType, final int i, Stack<StackableType> typeStack, StringBuilder brackets) {

        if(canonicalAbiType.charAt(i) == ']') {

            final int arrayOpenIndex = canonicalAbiType.lastIndexOf('[', i - 1);

            Pair<String, String> results = buildTypeStack(canonicalAbiType, arrayOpenIndex - 1, typeStack, brackets);

            brackets.append('[');
            final String className = brackets.toString() + results.second;

            if(arrayOpenIndex == i - 1) { // []
                typeStack.push(new DynamicArray(canonicalAbiType, className, typeStack.peek()));
            } else { // [...]
                int length = Integer.parseInt(canonicalAbiType.substring(arrayOpenIndex + 1, i));
                if(typeStack.peek().dynamic) {
                    typeStack.push(new DynamicArray(canonicalAbiType, className, typeStack.peek()));
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
                javaBaseType = StackableType.getJavaBaseTypeName(abiBaseType, isElement, typeStack);
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
