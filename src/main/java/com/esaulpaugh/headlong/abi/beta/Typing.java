package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;

import static com.esaulpaugh.headlong.abi.beta.DynamicArrayType.DYNAMIC_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;

abstract class Typing {

    static StackableType createForTuple(String canonicalType, TupleType baseTupleType) {
        if(baseTupleType == null) {
            throw new NullPointerException();
        }
        return create(canonicalType, baseTupleType);
    }

    static StackableType create(String canonicalType) {
        return create(canonicalType, null);
    }

    private static StackableType create(String canonicalType, TupleType baseTupleType) {
        Deque<StackableType> typeStack = new ArrayDeque<>();
        buildTypeStack(canonicalType, canonicalType.length() - 1, typeStack, new StringBuilder(), baseTupleType);
        return typeStack.peek();
    }

    private static String buildTypeStack(final String canonicalType,
                                         final int index,
                                         final Deque<StackableType> typeStack,
                                         final StringBuilder brackets,
                                         final StackableType baseTuple) {

        if(canonicalType.charAt(index) == ']') {

            final int fromIndex = index - 1;
            final int arrayOpenIndex = canonicalType.lastIndexOf('[', fromIndex);

            final String baseClassName = buildTypeStack(canonicalType, arrayOpenIndex - 1, typeStack, brackets, baseTuple);

            brackets.append('[');
            final String className = brackets.toString() + baseClassName;

            if(arrayOpenIndex == fromIndex) { // i.e. []
                typeStack.push(new DynamicArrayType<StackableType, Object>(canonicalType, className, null, typeStack.peek(), DYNAMIC_LENGTH));
            } else { // e.g. [10]
                final int length = Integer.parseUnsignedInt(canonicalType.substring(arrayOpenIndex + 1, index));
                final StackableType top = typeStack.peek();
                if(top == null) { // should never happen
                    throw new EmptyStackException();
                }
                if(top.dynamic) {
                    typeStack.push(new DynamicArrayType<StackableType, Object>(canonicalType, className, null, top, length));
                } else {
                    typeStack.push(new StaticArrayType<StackableType, Object>(canonicalType, className, null, top, length));
                }
            }

            return baseClassName;
        } else {
            final String baseType = canonicalType.substring(0, index + 1);
            final boolean isArrayElement = index != canonicalType.length() - 1;

            String javaBaseType;
            try {

                javaBaseType = resolveBaseType(baseType, isArrayElement, typeStack, baseTuple);
            } catch (NumberFormatException nfe) {
                javaBaseType = null;
            }
            if(javaBaseType == null) {
                throw new IllegalArgumentException("unrecognized type: " + baseType + " (" + String.format("%040x", new BigInteger(baseType.getBytes(UTF_8))) + ")");
            }

            return javaBaseType;
        }
    }

    private static String resolveBaseType(final String canonicalType, boolean isElement, Deque<StackableType> typeStack, StackableType baseTuple) {

        final StackableType type;

        BaseTypeInfo info = BaseTypeInfo.get(canonicalType);

        if(info != null) {
            // hash code should already be cached due to BaseTypeInfo.get()
            switch (info.canonical) {
            case "uint8": type = new ByteType(canonicalType, false); break;
            case "uint16": type = new ShortType(canonicalType, false); break;
            case "uint24":
            case "uint32": type = new IntType(canonicalType, info.bitLength, false); break;
            case "uint40":
            case "uint48":
            case "uint56":
            case "uint64": type = new LongType(canonicalType, info.bitLength, false); break;
            case "uint72":
            case "uint80":
            case "uint88":
            case "uint96":
            case "uint104":
            case "uint112":
            case "uint120":
            case "uint128":
            case "uint136":
            case "uint144":
            case "uint152":
            case "uint160":
            case "address":
            case "uint168":
            case "uint176":
            case "uint184":
            case "uint192":
            case "uint200":
            case "uint208":
            case "uint216":
            case "uint224":
            case "uint232":
            case "uint240":
            case "uint248":
            case "uint256": type = new BigIntegerType(canonicalType, info.bitLength, false); break;
            case "int8": type = new ByteType(canonicalType, true); break;
            case "int16": type = new ShortType(canonicalType, true); break;
            case "int24":
            case "int32": type = new IntType(canonicalType, info.bitLength, true); break;
            case "int40":
            case "int48":
            case "int56":
            case "int64": type = new LongType(canonicalType, info.bitLength, true); break;
            case "int72":
            case "int80":
            case "int88":
            case "int96":
            case "int104":
            case "int112":
            case "int120":
            case "int128":
            case "int136":
            case "int144":
            case "int152":
            case "int160":
            case "int168":
            case "int176":
            case "int184":
            case "int192":
            case "int200":
            case "int208":
            case "int216":
            case "int224":
            case "int232":
            case "int240":
            case "int248":
            case "int256": type = new BigIntegerType(canonicalType, info.bitLength, true); break;
            case "bytes1":
            case "bytes2":
            case "bytes3":
            case "bytes4":
            case "bytes5":
            case "bytes6":
            case "bytes7":
            case "bytes8":
            case "bytes9":
            case "bytes10":
            case "bytes11":
            case "bytes12":
            case "bytes13":
            case "bytes14":
            case "bytes15":
            case "bytes16":
            case "bytes17":
            case "bytes18":
            case "bytes19":
            case "bytes20":
            case "bytes21":
            case "bytes22":
            case "bytes23":
            case "bytes24":
            case "function":
            case "bytes25":
            case "bytes26":
            case "bytes27":
            case "bytes28":
            case "bytes29":
            case "bytes30":
            case "bytes31":
            case "bytes32": type = new StaticArrayType<ByteType, Byte>(canonicalType, info.className, info.arrayClassNameStub, (ByteType) info.elementType, info.arrayLength); break;
            case "bool": type = new BooleanType(); break;
            case "bytes":
            case "string": type = new DynamicArrayType<ByteType, Byte>(canonicalType, info.className, info.arrayClassNameStub, (ByteType) info.elementType, DYNAMIC_LENGTH); break;
            default: type = null;
            }
        } else {
            if(canonicalType.startsWith("(")) {
                type = baseTuple;
            } else {
                int idx;
                boolean isSignedDecimal;
                if ((isSignedDecimal = (idx = canonicalType.indexOf("fixed")) == 0) || idx == 1) {
                    final int indexOfX = canonicalType.indexOf('x');
                    int M = Integer.parseUnsignedInt(canonicalType.substring(idx + "fixed".length(), indexOfX), 10);
                    int N = Integer.parseUnsignedInt(canonicalType.substring(indexOfX + 1), 10); // everything after x
                    if ((M & 0b111) /* mod 8 */ == 0 && M >= 8 && M <= 256
                            && N > 0 && N <= 80) {
                        type = new BigDecimalType(canonicalType, M, N, isSignedDecimal);
                    } else {
                        type = null;
                    }
                } else {
                    type = null;
                }
            }
        }

        if(type == null) {
            return null;
        }

        typeStack.push(type);
        return isElement ? type.arrayClassNameStub() : type.className();
    }
}

//        int bits = Integer.parseUnsignedInt(abiBaseType, "uint".length(), abiBaseType.length(), 10); // Java 9

//        int idx;
//        // ~5,220 possible base types (mostly (u)fixedMxN)
//        if (abi.charAt(0) == '(') {
////            if(baseTuple == null) {
////                throw new NullPointerException("baseTuple is null");
////            }
//            typeStack.push(baseTuple);
//            return isElement ? CLASS_NAME_ELEMENT_TUPLE : CLASS_NAME_TUPLE;
//        } else if ("bool".equals(abi)) {
//            className = isElement ? BooleanType.ARRAY_CLASS_NAME_STUB : BooleanType.CLASS_NAME;
//            typeStack.push(new BooleanType()); // com.esaulpaugh.headlong.abi.beta.ByteType.booleanType(abi, className)); // new ByteType(abi, className));
//        } else if ("address".equals(abi)) { // same as uint160
//            className = isElement ? BigIntegerType.ARRAY_CLASS_NAME_STUB : BigIntegerType.CLASS_NAME;
//            typeStack.push(new BigIntegerType("address", 160, false)); // "uint160"
////            typeStack.push(new StaticArrayType(abi, className, BYTE_PRIMITIVE, 20));
//        } else if ((idx = abi.indexOf("int")) != -1) {
//            if (abi.length() == "int".length()) {
//                throw new IllegalArgumentException("non-canonical: " + abi);
//            }
//            int bitLength = Integer.parseUnsignedInt(abi.substring(idx + "int".length()), 10);
//
//            AbstractInt256Type integer = AbstractInt256Type.makeInt(abi, bitLength, abi.charAt(0) == 'u');
//            className = isElement ? integer.arrayClassNameStub() : integer.className();
//            typeStack.push(integer);
//        } else if (abi.startsWith("fixed")) {
//            if (abi.length() == "fixed".length()) {
//                throw new IllegalArgumentException("non-canonical: " + abi);
//            }
//            final int indexOfX = abi.indexOf('x', "fixed".length());
//            int bits = Integer.parseUnsignedInt(abi.substring("fixed".length(), indexOfX), 10);
//            int scale = Integer.parseUnsignedInt(abi.substring(indexOfX + 1), 10);
//            if(bits % 8 != 0 || bits < 8 || bits > 256 || scale < 1 || scale > 80) {
//                return null;
//            }
//            className = isElement ? BigDecimalType.ARRAY_CLASS_NAME_STUB : BigDecimalType.CLASS_NAME;
//            typeStack.push(new BigDecimalType(abi, bits, scale, true));
//        } else if (abi.startsWith("ufixed")) {
//            if (abi.length() == "ufixed".length()) {
//                throw new IllegalArgumentException("non-canonical: " + abi);
//            }
//            final int indexOfX = abi.indexOf('x', "ufixed".length());
//            int bits = Integer.parseUnsignedInt(abi.substring("ufixed".length(), indexOfX), 10);
//            int scale = Integer.parseUnsignedInt(abi.substring(indexOfX + 1), 10);
//            if(bits % 8 != 0 || bits < 8 || bits > 256 || scale < 1 || scale > 80) {
//                return null;
//            }
//            className = isElement ? BigDecimalType.ARRAY_CLASS_NAME_STUB : BigDecimalType.CLASS_NAME;
//            typeStack.push(new BigDecimalType(abi, bits, scale, false));
//        } else if ("function".equals(abi)) {
//            className = CLASS_NAME_ARRAY_BYTE;
//            typeStack.push(new StaticArrayType<ByteType, Byte>(abi, className, UNSIGNED_BYTE_OBJECT, 24));
//        } else if (abi.startsWith("bytes")) {
//            className = CLASS_NAME_ARRAY_BYTE;
//            if (abi.length() > "bytes".length()) {
//                int bytes = Integer.parseUnsignedInt(abi.substring("bytes".length()), 10);
//                if(bytes < 1 || bytes > 32) {
//                    return null;
//                }
//                typeStack.push(new StaticArrayType<ByteType, Byte>(abi, className, UNSIGNED_BYTE_OBJECT, bytes));
//            } else {
//                typeStack.push(new DynamicArrayType<ByteType, Byte>(abi, className, UNSIGNED_BYTE_OBJECT, DYNAMIC_LENGTH));
//            }
//        } else if ("string".equals(abi)) {
//            className = isElement ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
//            typeStack.push(new DynamicArrayType<ByteType, Byte>(abi, CLASS_NAME_STRING, UNSIGNED_BYTE_OBJECT, DYNAMIC_LENGTH));
//        } else {
//            return null;
//        }