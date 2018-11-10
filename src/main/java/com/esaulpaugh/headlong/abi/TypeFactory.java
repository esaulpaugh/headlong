package com.esaulpaugh.headlong.abi;

import java.math.BigInteger;
import java.text.ParseException;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;

/**
 * Creates the appropriate {@link StackableType} object for a given canonical type string.
 */
final class TypeFactory {

    private static final ClassLoader CLASS_LOADER = TypeFactory.class.getClassLoader();

    static StackableType<?> createForTuple(String canonicalType, TupleType baseTupleType) throws ParseException {
        if(baseTupleType == null) {
            throw new NullPointerException();
        }
        return create(canonicalType, baseTupleType);
    }

    static StackableType<?> create(String canonicalType) throws ParseException {
        return create(canonicalType, null);
    }

    private static StackableType<?> create(String canonicalType, TupleType baseTupleType) throws ParseException {
        try {
            return buildType(canonicalType, canonicalType.length() - 1, baseTupleType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static StackableType<?> buildType(final String canonicalType, final int index, final StackableType<?> baseTuple) throws ParseException, ClassNotFoundException {

        if(canonicalType.charAt(index) == ']') { // array

            final int fromIndex = index - 1;
            final int arrayOpenIndex = canonicalType.lastIndexOf('[', fromIndex);

            final int length;
            if(arrayOpenIndex == fromIndex) { // i.e. []
                length = DYNAMIC_LENGTH;
            } else { // e.g. [4]
                try {
                    length = Integer.parseInt(canonicalType.substring(arrayOpenIndex + 1, index));
                    if(length < 0) {
                        throw new ParseException("negative array size", arrayOpenIndex + 1);
                    }
                } catch (NumberFormatException nfe) {
                    throw (ParseException) new ParseException("illegal argument", arrayOpenIndex).initCause(nfe);
                }
            }

            final StackableType<?> elementType = buildType(canonicalType, arrayOpenIndex - 1, baseTuple);
            final String elementArrayClassNameStub = elementType.arrayClassNameStub();
            final Class<?> elementClass;
            if(elementType.typeCode() == StackableType.TYPE_CODE_ARRAY) {
                elementClass = Class.forName(elementType.className(), true, CLASS_LOADER);
            } else {
                elementClass = null;
            }
            final String className = '[' + elementArrayClassNameStub;
            final boolean dynamic = length == DYNAMIC_LENGTH || elementType.dynamic;
            return new ArrayType<StackableType<?>, Object>(canonicalType, elementType, elementClass, className, className, length, dynamic);
        } else {
            final String baseTypeString = canonicalType.substring(0, index + 1);
            final boolean isArrayElement = index < canonicalType.length() - 1;
            StackableType<?> baseType = resolveBaseType(baseTypeString, isArrayElement, baseTuple);
            if(baseType == null) {
                throw new ParseException("unrecognized type: "
                        + baseTypeString + " (" + String.format("%040x", new BigInteger(baseTypeString.getBytes(CHARSET_UTF_8))) + ")", -1);
            }
            return baseType;
        }
    }

    private static StackableType<?> resolveBaseType(final String ct, boolean isElement, StackableType<?> baseTuple) {

        final StackableType<?> type;

        BaseTypeInfo info = BaseTypeInfo.get(ct);

        if(info != null) {
            switch (ct) { // canonicalType's hash code already cached from BaseTypeInfo.get()
            case "uint8": type = isElement ? new ByteType(ct, true) : new IntType(ct, info.bitLen, true); break;
            case "uint16":
            case "uint24": type = new IntType(ct, info.bitLen, true); break;
            case "uint32": type = isElement ? new IntType(ct, info.bitLen, true) : new LongType(ct, info.bitLen, true); break;
            case "uint40":
            case "uint48":
            case "uint56": type = new LongType(ct, info.bitLen, true); break;
            case "uint64": type = isElement ? new LongType(ct, info.bitLen, true) : new BigIntegerType(ct, info.bitLen, true); break;
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
            case "uint256": type = new BigIntegerType(ct, info.bitLen, true); break;
            case "int8":
            case "int16":
            case "int24":
            case "int32": type = new IntType(ct, info.bitLen, false); break;
            case "int40":
            case "int48":
            case "int56":
            case "int64": type = new LongType(ct, info.bitLen, false); break;
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
            case "int256": type = new BigIntegerType(ct, info.bitLen, false); break;
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
            case "bytes32": type = new ArrayType<ByteType, byte[]>(ct, (ByteType) info.elementType, Byte.class, info.className, info.arrayClassNameStub, info.arrayLen, false); break;
            case "bool": type = new BooleanType(); break;
            case "bytes":
            case "string": type = new ArrayType<ByteType, byte[]>(ct, (ByteType) info.elementType, Byte.class, info.className, info.arrayClassNameStub, DYNAMIC_LENGTH, true); break;
            case "decimal": type = new BigDecimalType(ct, info.bitLen, info.scale, false); break;
            default: type = null;
            }
        } else {
            if(ct.startsWith("(")) {
                int last = ct.charAt(ct.length() - 1);
                type = last == ')' || last == ']' ? baseTuple : null;
            } else {
                type = tryParseFixed(ct);
            }
        }

        return type;
    }

    private static BigDecimalType tryParseFixed(String canonicalType) {
        final int idx = canonicalType.indexOf("fixed");
        boolean unsigned = idx == 1;
        if (unsigned || idx == 0) {
            if(unsigned && canonicalType.charAt(0) != 'u') {
                return null;
            }
            final int indexOfX = canonicalType.lastIndexOf('x');
            try {
                int M = Integer.parseInt(canonicalType.substring(idx + "fixed".length(), indexOfX), 10);
                int N = Integer.parseInt(canonicalType.substring(indexOfX + 1), 10); // everything after x
                if ((M & 0x7) /* mod 8 */ == 0 && M >= 8 && M <= 256
                        && N > 0 && N <= 80) {
                    return new BigDecimalType(canonicalType, M, N, unsigned);
                }
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
