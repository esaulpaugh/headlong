package com.esaulpaugh.headlong.abi.beta.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

import static com.esaulpaugh.headlong.abi.beta.type.Byte.BYTE_PRIMITIVE;
import static com.esaulpaugh.headlong.abi.beta.util.ClassNames.toFriendly;

abstract class StackableType {

    public static final StackableType[] EMPTY_TYPE_ARRAY = new StackableType[0];

    protected static final String CLASS_NAME_BOOLEAN = Boolean.class.getName();
    protected static final String CLASS_NAME_BYTE = java.lang.Byte.class.getName();
    protected static final String CLASS_NAME_SHORT = Short.class.getName();
    protected static final String CLASS_NAME_INT = Integer.class.getName();
    protected static final String CLASS_NAME_LONG = Long.class.getName();

    protected static final String CLASS_NAME_BIG_INTEGER = BigInteger.class.getName();
    protected static final String CLASS_NAME_BIG_DECIMAL = BigDecimal.class.getName();
    protected static final String CLASS_NAME_STRING = String.class.getName();

    protected static final String CLASS_NAME_ELEMENT_BOOLEAN = boolean[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_BYTE = byte[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_SHORT = short[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_INT = int[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_LONG = long[].class.getName().replaceFirst("\\[", "");

    protected static final String CLASS_NAME_ELEMENT_BIG_INTEGER = BigInteger[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_BIG_DECIMAL = BigDecimal[].class.getName().replaceFirst("\\[", "");
    protected static final String CLASS_NAME_ELEMENT_STRING = String[].class.getName().replaceFirst("\\[", "");

    protected static final String CLASS_NAME_ARRAY_BYTE = byte[].class.getName();

    protected final String canonicalAbiType;
    protected final String className;
    protected final int length;

    protected final boolean dynamic;

//    protected transient int tailOffset; // TODO

    protected StackableType(String canonicalAbiType, String className, int length) {
        this(canonicalAbiType, className, length, false);
    }

    protected StackableType(String canonicalAbiType, String className, int length, boolean dynamic) {
        this.canonicalAbiType = canonicalAbiType;
        this.className = className;
        this.length = length;
        this.dynamic = dynamic;
    }

    abstract int byteLength(Object value);

    static String getJavaBaseTypeName(final String abi, boolean isElement, Stack<StackableType> typeStack) {

//        int bits = Integer.parseUnsignedInt(abiBaseType, "uint".length(), abiBaseType.length(), 10); // Java 9

//        String bracketsString = brackets.toString();

        final String className;

        // ~5,220 possible base types (mostly (u)fixedMxN)
        if (abi.charAt(0) == '(') {
//            SignatureParser.parseTuple()
            throw new IllegalArgumentException("can't create tuple this way");
        } else if ("bool".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
            typeStack.push(new Byte(abi, className));
        } else if ("address".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
            typeStack.push(new StaticArray(abi, className, BYTE_PRIMITIVE, 20));
        } else if (abi.startsWith("uint")) {
            if (abi.length() == "uint".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("uint".length()), 10);
            className = classNameForInt(bits, isElement);
            StackableType integer;
            if(bits == 8) {
                integer = BYTE_PRIMITIVE;
            } else if(className.equals(CLASS_NAME_ELEMENT_BIG_INTEGER)) {
                integer = new Int256(abi, CLASS_NAME_BIG_INTEGER, bits);
            } else {
                integer = new Int256(abi, className, bits);
            }
            typeStack.push(integer);
        } else if (abi.startsWith("int")) {
            if (abi.length() == "int".length()) {
                throw new IllegalArgumentException("non-canonical: " + abi);
            }
            int bits = Integer.parseUnsignedInt(abi.substring("int".length()), 10);
            className = classNameForInt(bits, isElement);
//            Int256 int256 = new Int256(abi, className, bits); // .className;
            typeStack.push(bits == 8 ? BYTE_PRIMITIVE : new Int256(abi, CLASS_NAME_BIG_INTEGER, bits));
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
                typeStack.push(new DynamicArray(abi, className, BYTE_PRIMITIVE));
            }
        } else if ("string".equals(abi)) {
            className = isElement ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
            typeStack.push(new DynamicArray(abi, CLASS_NAME_STRING, BYTE_PRIMITIVE));
        } else {
            throw new IllegalArgumentException("?");
        }

        return className;
    }

    private static String classNameForInt(int bits, boolean isElement) {
        if (bits > 64) {
//            Int256 i = new Int256(null, className, bits).className;
//            typeStack.push(bits == 8 ? BYTE_PRIMITIVE : new Int256(abi, className, bits));

            return isElement ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
        }
        if (bits > 32) {
            return isElement ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
        }
        if (bits > 16) {
            return isElement ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
        }
        if (bits > 8) {
            return isElement ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
        }
        return isElement ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE;
    }

    protected void validate(Object value) {
        validate(this, value);
    }

    private static void validate(final StackableType type, final Object value) { //  { // , final String expectedClassName

        String expectedClassName = type.className; // TODO

        // will throw NPE if argument null
        if(!expectedClassName.equals(value.getClass().getName())) {
            boolean isAssignable;
            try {
                isAssignable = Class.forName(expectedClassName).isAssignableFrom(value.getClass());
            } catch (ClassNotFoundException cnfe) {
                isAssignable = false;
            }
            if(!isAssignable) {
                throw new IllegalArgumentException("class mismatch: "
                        + value.getClass().getName()
                        + " not assignable to "
                        + expectedClassName
                        + " (" + toFriendly(value.getClass().getName()) + " not instanceof " + toFriendly(expectedClassName) + "/" + type.canonicalAbiType + ")");
            }
        }
        System.out.print("matches class " + expectedClassName + " ");
    }
}
