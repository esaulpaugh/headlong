/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.DECIMAL_BIT_LEN;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.DECIMAL_SCALE;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.FIXED_BIT_LEN;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.FIXED_SCALE;

/**
 * Creates the appropriate {@link ABIType} object for a given canonical type string.
 */
final class TypeFactory {

    static final String UNRECOGNIZED_TYPE = "unrecognized type";

    private static final ABIType<BigInteger> NAMELESS_INT_TYPE = new BigIntegerType("int256", 256, false);
    private static final ABIType<BigInteger> NAMELESS_UINT_TYPE = new BigIntegerType("uint256", 256, true);

    private static final ABIType<byte[]> NAMELESS_BYTES_TYPE = new ArrayType<>("bytes", ArrayType.BYTE_ARRAY_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME);

    private static final ABIType<String> NAMELESS_STRING_TYPE = new ArrayType<>("string", ArrayType.STRING_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.STRING_ARRAY_CLASS_NAME);

    private static final ABIType<BigDecimal> NAMELESS_FIXED_TYPE = new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false);
    private static final ABIType<BigDecimal> NAMELESS_UFIXED_TYPE = new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true);
    private static final ABIType<BigDecimal> NAMELESS_DECIMAL_TYPE = new BigDecimalType("decimal", DECIMAL_BIT_LEN, DECIMAL_SCALE, false);

    private static final ABIType<Boolean> NAMELESS_BOOLEAN_TYPE = new BooleanType();

    private static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    static ABIType<?> createForTuple(TupleType baseTupleType, String suffix, String name) throws ParseException {
        return create(baseTupleType.canonicalType + suffix, baseTupleType, name);
    }

    static ABIType<?> create(String type) throws ParseException {
        return create(type, null, null);
    }

    static ABIType<?> create(String type, TupleType baseTupleType, String name) throws ParseException {
        try {
            if(name == null) {
                return buildType(type, false, baseTupleType, true);
            }
            return buildType(type, false, baseTupleType, false)
                    .setName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ABIType<?> buildType(String type, boolean isArrayElement, TupleType baseTupleType, boolean nameless) throws ParseException, ClassNotFoundException {
        try {
            final int idxOfLast = type.length() - 1;
            if (type.charAt(idxOfLast) == ']') { // array

                final int fromIndex = idxOfLast - 1;
                final int arrayOpenIndex = type.lastIndexOf('[', fromIndex);

                final int length;
                if (arrayOpenIndex == fromIndex) { // i.e. []
                    length = DYNAMIC_LENGTH;
                } else { // e.g. [4]
                    try {
                        length = Integer.parseInt(type.substring(arrayOpenIndex + 1, idxOfLast));
                        if (length < 0) {
                            throw new ParseException("negative array size", arrayOpenIndex + 1);
                        }
                    } catch (NumberFormatException nfe) {
                        throw (ParseException) new ParseException("illegal argument", arrayOpenIndex + 1).initCause(nfe);
                    }
                }

                final ABIType<?> elementType = buildType(type.substring(0, arrayOpenIndex), true, baseTupleType, nameless);
                final String arrayClassName = elementType.arrayClassName();
                @SuppressWarnings("unchecked")
                final Class<Object> arrayClass = (Class<Object>) Class.forName(arrayClassName, false, CLASS_LOADER);
                final boolean dynamic = length == DYNAMIC_LENGTH || elementType.dynamic;
                return new ArrayType<ABIType<?>, Object>(type, arrayClass, dynamic, elementType, length, '[' + arrayClassName);
            } else {
                if (baseTupleType != null) {
                    return baseTupleType;
                }
                ABIType<?> baseType = resolveBaseType(type, isArrayElement, nameless);
                if (baseType != null) {
                    return baseType;
                }
            }
        } catch (StringIndexOutOfBoundsException sioobe) { // e.g. type equals "" or "82]" or "[]" or "[1]"
            /* fall through */
        }
        throw new ParseException(UNRECOGNIZED_TYPE + ": " + type, 0);
    }

    private static ABIType<?> resolveBaseType(String baseTypeStr, boolean isElement, boolean nameless) throws ParseException, ClassNotFoundException {
        if(baseTypeStr.charAt(0) == '(') {
            return parseTupleType(baseTypeStr);
        }

        BaseTypeInfo info = BaseTypeInfo.get(baseTypeStr);
        if(info != null) {
            switch (baseTypeStr) { // baseType's hash code already cached due to BaseTypeInfo.get(baseTypeStr)
            case "int8":
            case "int16":
            case "int24":
            case "int32":   return new IntType(baseTypeStr, info.bitLen, false);
            case "int40":
            case "int48":
            case "int56":
            case "int64":   return new LongType(baseTypeStr, info.bitLen, false);
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
            case "int248":  return new BigIntegerType(baseTypeStr, info.bitLen, false);
            case "int256":
            case "int":     return nameless ? NAMELESS_INT_TYPE : new BigIntegerType("int256", info.bitLen, false);
            case "uint8":
            case "uint16":
            case "uint24":  return new IntType(baseTypeStr, info.bitLen, true);
            case "uint32":  return isElement ? new IntType(baseTypeStr, info.bitLen, true) : new LongType(baseTypeStr, info.bitLen, true);
            case "uint40":
            case "uint48":
            case "uint56":  return new LongType(baseTypeStr, info.bitLen, true);
            case "uint64":  return isElement ? new LongType(baseTypeStr, info.bitLen, true) : new BigIntegerType(baseTypeStr, info.bitLen, true);
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
            case "uint248": return new BigIntegerType(baseTypeStr, info.bitLen, true);
            case "uint256":
            case "uint":    return nameless ? NAMELESS_UINT_TYPE : new BigIntegerType("uint256", info.bitLen, true);
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
            case "bytes32": return new ArrayType<>(baseTypeStr, ArrayType.BYTE_ARRAY_CLASS, false, ByteType.UNSIGNED, info.arrayLen, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME);
            case "bool":    return nameless ? NAMELESS_BOOLEAN_TYPE : new BooleanType();
            case "bytes":   return nameless ? NAMELESS_BYTES_TYPE : new ArrayType<>(baseTypeStr, ArrayType.BYTE_ARRAY_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME);
            case "string":  return nameless ? NAMELESS_STRING_TYPE : new ArrayType<>(baseTypeStr, ArrayType.STRING_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.STRING_ARRAY_CLASS_NAME);
            case "decimal": return nameless ? NAMELESS_DECIMAL_TYPE : new BigDecimalType(baseTypeStr, DECIMAL_BIT_LEN, DECIMAL_SCALE, false);
            case "fixed":
            case "fixed128x18":
                            return nameless ? NAMELESS_FIXED_TYPE : new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false);
            case "ufixed":
            case "ufixed128x18":
                            return nameless ? NAMELESS_UFIXED_TYPE : new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true);
            default:        return null;
            }
        }
        return tryParseFixed(baseTypeStr);
    }

    private static BigDecimalType tryParseFixed(final String type) {
        final int idx = type.indexOf("fixed");
        boolean unsigned = false;
        if (idx == 0 || (unsigned = idx == 1 && type.charAt(0) == 'u')) {
            final int indexOfX = type.lastIndexOf('x');
            try {
                // no parseUnsignedInt on Android?
                int M = Integer.parseInt(type.substring(idx + "fixed".length(), indexOfX));
                int N = Integer.parseInt(type.substring(indexOfX + 1)); // everything after x
                if ((M & 0x7) /* mod 8 */ == 0 && M >= 8 && M <= 256 && N > 0 && N <= 80) {
                    return new BigDecimalType(type, M, N, unsigned);
                }
            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
            }
        }
        return null;
    }

    static final String EMPTY_PARAMETER = "empty parameter";
    static final String ILLEGAL_TUPLE_TERMINATION = "illegal tuple termination";

    private static TupleType parseTupleType(final String rawTypeStr) throws ParseException, ClassNotFoundException {
        final int end = rawTypeStr.length();
        final ArrayList<ABIType<?>> elements = new ArrayList<>();

        int argEnd = 1; // this inital value is important for empty params case: "()"
        try {
            int argStart = 1; // after opening '('
            WHILE:
            while (argStart < end) {
                int fromIndex;
                char c = rawTypeStr.charAt(argStart);
                switch (c) {
                case '(': // element is tuple or tuple array
                    fromIndex = findSubtupleEnd(rawTypeStr, end, argStart + 1);
                    break;
                case ')':
                    if(rawTypeStr.charAt(argEnd) == ',') {
                        throw new ParseException(EMPTY_PARAMETER, argStart);
                    }
                    break WHILE;
                case ',':
                    if (rawTypeStr.charAt(argStart - 1) == ')') {
                        break WHILE;
                    }
                    throw new ParseException(EMPTY_PARAMETER, argStart);
                default: // non-tuple element
                    fromIndex = argStart + 1;
                }
                argEnd = nextTerminator(rawTypeStr, fromIndex);
                if(argEnd == -1) {
                    break;
                }
                elements.add(buildType(rawTypeStr.substring(argStart, argEnd), false, null, true));
                argStart = argEnd + 1; // jump over terminator
            }
        } catch (ParseException pe) {
            throw (ParseException) new ParseException("@ index " + elements.size() + ", " + pe.getMessage(), pe.getErrorOffset())
                    .initCause(pe);
        }
        if(argEnd < 0 || argEnd != end - 1 || rawTypeStr.charAt(argEnd) != ')') {
            throw new ParseException(ILLEGAL_TUPLE_TERMINATION, Math.max(0, argEnd));
        }
        return TupleType.wrap(elements.toArray(ABIType.EMPTY_TYPE_ARRAY));
    }

    private static int findSubtupleEnd(String parentTypeString, final int end, int i) throws ParseException {
        int depth = 1;
        do {
            if(i < end) {
                char x = parentTypeString.charAt(i++);
                if(x > ')') {
                    continue;
                }
                if(x == ')') {
                    depth--;
                } else if(x == '(') {
                    depth++;
                }
            } else {
                throw new ParseException(ILLEGAL_TUPLE_TERMINATION, end);
            }
        } while(depth > 0);
        return i;
    }

    private static int nextTerminator(String signature, int i) {
        int comma = signature.indexOf(',', i);
        int close = signature.indexOf(')', i);
        if(comma == -1) {
            return close;
        }
        if(close == -1) {
            return comma;
        }
        return Math.min(comma, close);
    }
}
