package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.esaulpaugh.headlong.abi.beta.ArrayType.DYNAMIC_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;

final class TypeFactory {

    static StackableType createForTuple(String canonicalType, TupleType baseTupleType) throws ParseException {
        if(baseTupleType == null) {
            throw new NullPointerException();
        }
        return create(canonicalType, baseTupleType);
    }

    static StackableType create(String canonicalType) throws ParseException {
        return create(canonicalType, null);
    }

    private static StackableType create(String canonicalType, TupleType baseTupleType) throws ParseException {
        Deque<StackableType> typeStack = new ArrayDeque<>();
        buildTypeStack(canonicalType, canonicalType.length() - 1, typeStack, new StringBuilder(), baseTupleType);
        return typeStack.peek();
    }

    private static String buildTypeStack(final String canonicalType,
                                         final int index,
                                         final Deque<StackableType> typeStack,
                                         final StringBuilder brackets,
                                         final StackableType baseTuple) throws ParseException {
        if(canonicalType.charAt(index) == ']') {

            final int fromIndex = index - 1;
            final int arrayOpenIndex = canonicalType.lastIndexOf('[', fromIndex);

            final String baseClassName = buildTypeStack(canonicalType, arrayOpenIndex - 1, typeStack, brackets, baseTuple);

            final int arrayLength;
            if(arrayOpenIndex == fromIndex) { // i.e. []
                arrayLength = DYNAMIC_LENGTH;
            } else {
                try {
                    arrayLength = Integer.parseUnsignedInt(canonicalType.substring(arrayOpenIndex + 1, index));
                } catch (NumberFormatException nfe) {
                    throw (ParseException) new ParseException("illegal argument", arrayOpenIndex).initCause(nfe);
                }
            }

            brackets.append('[');
            final String className = brackets.toString() + baseClassName;

            final StackableType top = typeStack.peekFirst();
            final boolean dynamic = arrayLength == DYNAMIC_LENGTH || top.dynamic;
            // push onto stack
            typeStack.addFirst(new ArrayType<StackableType, Object>(canonicalType, className, top, arrayLength, dynamic));

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
                throw new ParseException("unrecognized type: " + baseType + " (" + String.format("%040x", new BigInteger(baseType.getBytes(UTF_8))) + ")", -1);
            }

            return javaBaseType;
        }
    }

    private static String resolveBaseType(final String canonicalType, boolean isElement, Deque<StackableType> typeStack, StackableType baseTuple) {

        final StackableType type;

        BaseTypeInfo info = BaseTypeInfo.get(canonicalType);

        final String arrayClassNameStub;
        if(info != null) {
            switch (canonicalType) { // canonicalType's hash code already cached from BaseTypeInfo.get()
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
            case "bytes32": type = new ArrayType<ByteType, byte[]>(canonicalType, info.className, (ByteType) info.elementType, info.arrayLength, false); break;
            case "bool": type = new BooleanType(); break;
            case "bytes":
            case "string": type = new ArrayType<ByteType, byte[]>(canonicalType, info.className, (ByteType) info.elementType, DYNAMIC_LENGTH, true); break;
            case "decimal": type = new BigDecimalType(canonicalType, info.bitLength, info.scale, true); break;
            default: type = null;
            }
            arrayClassNameStub = info.arrayClassNameStub;
        } else {
            if(canonicalType.startsWith("(")) {
                int last = canonicalType.charAt(canonicalType.length() - 1);
                type = last == ')' || last == ']' ? baseTuple : null;
                arrayClassNameStub = type != null ? TupleType.ARRAY_CLASS_NAME_STUB : null;
            } else {
                type = tryParseFixed(canonicalType);
                arrayClassNameStub = type != null ? BigDecimalType.ARRAY_CLASS_NAME_STUB : null;
            }
        }

        if(type == null) {
            return null;
        }

        typeStack.addFirst(type);
        return isElement ? arrayClassNameStub : type.className();
    }

    static BigDecimalType tryParseFixed(String canonicalType) {
        int idx;
        boolean isSignedDecimal;
        if ((isSignedDecimal = (idx = canonicalType.indexOf("fixed")) == 0) || idx == 1) {
            if(idx == 1 && canonicalType.charAt(0) != 'u') {
                return null;
            }
            final int indexOfX = canonicalType.lastIndexOf('x');
            int M = Integer.parseUnsignedInt(canonicalType.substring(idx + "fixed".length(), indexOfX), 10);
            int N = Integer.parseUnsignedInt(canonicalType.substring(indexOfX + 1), 10); // everything after x
            if ((M & 0b111) /* mod 8 */ == 0 && M >= 8 && M <= 256
                    && N > 0 && N <= 80) {
                return new BigDecimalType(canonicalType, M, N, isSignedDecimal);
            }
        }
        return null;
    }
}
