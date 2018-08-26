package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.math.BigInteger;
import java.util.Stack;

import static java.nio.charset.StandardCharsets.UTF_8;

// TODO make package-private
public abstract class Typing {





//    protected enum EncodingBaseType {
//        UINT8("uint8"),
//        UINT256("uint256"),
//        STATIC_ARRAY("[0]"),
//        DYNAMIC_ARRAY("uint256"),
//        TUPLE("()");
//
//        final String abiBaseType;
//
//        EncodingBaseType(String abiBaseType) {
//            this.abiBaseType = abiBaseType;
//        }
//    }

//    protected static final String CLASS_NAME_BOOLEAN = Boolean.class.getName();
//    protected static final String CLASS_NAME_BYTE = Byte.class.getName();
//    protected static final String CLASS_NAME_SHORT = Short.class.getName();
//    protected static final String CLASS_NAME_INT = Integer.class.getName();
//    protected static final String CLASS_NAME_LONG = Long.class.getName();
//
//    protected static final String CLASS_NAME_BIG_INTEGER = BigInteger.class.getName();
//    protected static final String CLASS_NAME_BIG_DECIMAL = BigDecimal.class.getName();
//    protected static final String CLASS_NAME_STRING = String.class.getName();
//
//    protected static final String CLASS_NAME_ELEMENT_BOOLEAN = boolean[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_BYTE = byte[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_SHORT = short[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_INT = int[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_LONG = long[].class.getName().replaceFirst("\\[", "");
//
//    protected static final String CLASS_NAME_ELEMENT_BIG_INTEGER = BigInteger[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_BIG_DECIMAL = BigDecimal[].class.getName().replaceFirst("\\[", "");
//    protected static final String CLASS_NAME_ELEMENT_STRING = String[].class.getName().replaceFirst("\\[", "");
//
//    protected static final String CLASS_NAME_ARRAY_BYTE = byte[].class.getName();

//    private final String canonicalAbiType;
//    private final String javaClassName;
//    protected final boolean dynamic;

//    protected final String className;
//    protected final boolean dynamic;
//
//    private final String canonicalAbiType;
//
//    public Typing(String canonicalAbiType, String className, boolean dynamic) {
//        this.canonicalAbiType = canonicalAbiType;
//        this.className = className;
//        this.dynamic = dynamic;
//    }

    // TODO make package-private/protected
    public static StackableType create(String canonicalAbiType) {
        Stack<StackableType> typeStack = new Stack<>();
        Pair<String, String> results = buildTypeStack(canonicalAbiType, typeStack);

        String abiBaseType = results.first;
        String javaBaseType = results.second;

//        StringBuilder classNameBuilder = new StringBuilder();
//        int depth = typeStack.size() - 1;
//        for (int i = 0; i < depth; i++) {
//            classNameBuilder.append('[');
//        }
        String className = buildClassName(typeStack, javaBaseType);

//        final int size = typeStack.size();
        int i = 0;
        for(StackableType stackable : typeStack) {
            System.out.println(i++ + " " + stackable);
        }

//        final StackableType type = typeStack.peek();
//        final StackableType elementType = typeStack.get(typeStack.size() - 2);


//        for(StackableType stackable : typeStack) {
//            if(stackable instanceof DynamicArray) {
//                return new DynamicArray(canonicalAbiType, typeStack.peek(), className);
//            }
//        }
//
//        if(canonicalAbiType.charAt(canonicalAbiType.length() - 1) == ']') {
//            final StackableType elementType = typeStack.peek(); // ??
//            return new StaticArray(canonicalAbiType, elementType, className, elementType.length);
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
