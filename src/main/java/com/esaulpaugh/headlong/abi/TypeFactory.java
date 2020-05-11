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

import java.math.BigInteger;
import java.util.ArrayList;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.DECIMAL_BIT_LEN;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.DECIMAL_SCALE;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.FIXED_BIT_LEN;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.FIXED_SCALE;

/** Creates the appropriate {@link ABIType} object for a given type string. */
final class TypeFactory {

    private static final ABIType<BigInteger> CACHED_UINT_TYPE = new BigIntegerType("uint256", 256, true);

    private static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    static ABIType<?> create(String rawType, String name) {
        return buildType(rawType, null, name == null)
                .setName(name);
    }

    static ABIType<?> createFromBase(TupleType baseType, String typeSuffix, String name) {
        return buildType(baseType.canonicalType + typeSuffix, baseType, name == null)
                .setName(name);
    }

    private static ABIType<?> buildType(final String rawType, ABIType<?> baseType, final boolean nameless) {
        try {
            final int lastCharIndex = rawType.length() - 1;
            if (rawType.charAt(lastCharIndex) == ']') { // array

                final int secondToLastCharIndex = lastCharIndex - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIndex);

                final ABIType<?> elementType = buildType(rawType.substring(0, arrayOpenIndex), baseType, nameless);
                final String type = elementType.canonicalType + rawType.substring(arrayOpenIndex);
                final int length = arrayOpenIndex == secondToLastCharIndex ? DYNAMIC_LENGTH : parseLen(rawType, arrayOpenIndex + 1, lastCharIndex);
                final boolean dynamic = length == DYNAMIC_LENGTH || elementType.dynamic;
                final String arrayClassName = elementType.arrayClassName();
                @SuppressWarnings("unchecked")
                final Class<Object> arrayClass = (Class<Object>) Class.forName(arrayClassName, false, CLASS_LOADER);
                return new ArrayType<ABIType<?>, Object>(type, arrayClass, dynamic, elementType, length, '[' + arrayClassName);
            }
            if(baseType != null || (baseType = resolveBaseType(rawType, nameless)) != null) {
                return baseType;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (StringIndexOutOfBoundsException sioobe) { // e.g. type equals "" or "82]" or "[]" or "[1]"
            /* fall through */
        }
        throw new IllegalArgumentException("unrecognized type: " + rawType);
    }

    private static int parseLen(String rawType, int startLen, int lastCharIndex) {
        try {
            String lengthStr = rawType.substring(startLen, lastCharIndex);
            if(lengthStr.length() > 1 && rawType.charAt(startLen) == '0') {
                throw new IllegalArgumentException("leading zero in array length");
            }
            int length = Integer.parseInt(lengthStr);
            if (length >= 0) {
                return length;
            }
            throw new IllegalArgumentException("negative array length");
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("illegal number format", nfe);
        }
    }

    private static ABIType<?> resolveBaseType(String baseTypeStr, boolean nameless) {
        if(baseTypeStr.charAt(0) == '(') {
            return parseTupleType(baseTypeStr);
        }

        BaseTypeInfo info = BaseTypeInfo.get(baseTypeStr);
        if(info != null) {
            switch (baseTypeStr) {
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
            case "int":     return new BigIntegerType("int256", info.bitLen, false);
            case "uint8":
            case "uint16":
            case "uint24":  return new IntType(baseTypeStr, info.bitLen, true);
            case "uint32":
            case "uint40":
            case "uint48":
            case "uint56":  return new LongType(baseTypeStr, info.bitLen, true);
            case "uint64":
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
            case "uint":    return nameless ? CACHED_UINT_TYPE : new BigIntegerType("uint256", info.bitLen, true);
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
            case "bool":    return new BooleanType();
            case "bytes":   return new ArrayType<>(baseTypeStr, ArrayType.BYTE_ARRAY_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME);
            case "string":  return new ArrayType<>(baseTypeStr, ArrayType.STRING_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.STRING_ARRAY_CLASS_NAME);
            case "decimal": return new BigDecimalType(baseTypeStr, DECIMAL_BIT_LEN, DECIMAL_SCALE, false);
            case "fixed":
            case "fixed128x18":
                            return new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false);
            case "ufixed":
            case "ufixed128x18":
                            return new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true);
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

    private static TupleType parseTupleType(final String rawTypeStr) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final ArrayList<ABIType<?>> elements = new ArrayList<>();
        int argEnd = 1; // this inital value is important for empty params case: "()"
        try {
            int argStart = 1; // after opening '('
            final int end = rawTypeStr.length(); // must be >= 1
            LOOP:
            while (argStart < end) {
                switch (rawTypeStr.charAt(argStart)) {
                case '(': // element is tuple or tuple array
                    argEnd = nextTerminator(rawTypeStr, findSubtupleEnd(rawTypeStr, argStart + 1));
                    break;
                case ')':
                    if(rawTypeStr.charAt(argEnd) != ',') {
                        break LOOP;
                    }
                    throw new IllegalArgumentException(EMPTY_PARAMETER);
                case ',':
                    if (rawTypeStr.charAt(argStart - 1) == ')') {
                        break LOOP;
                    }
                    throw new IllegalArgumentException(EMPTY_PARAMETER);
                default: // non-tuple element
                    argEnd = nextTerminator(rawTypeStr, argStart + 1);
                }
                if(argEnd >= 0) {
                    elements.add(buildType(rawTypeStr.substring(argStart, argEnd), null, true));
                    if(rawTypeStr.charAt(argEnd) == ',') {
                        argStart = argEnd + 1; // jump over terminator
                        continue;
                    }
                }
                break;
            }
            if(argEnd == end - 1 && rawTypeStr.charAt(argEnd) == ')') {
                return TupleType.wrap(elements.toArray(ABIType.EMPTY_TYPE_ARRAY));
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("@ index " + elements.size() + ", " + iae.getMessage(), iae);
        }
        throw new IllegalArgumentException("unrecognized type: " + rawTypeStr);
    }

    private static int findSubtupleEnd(String parentTypeString, int i) {
        int depth = 1;
        do {
            char x = parentTypeString.charAt(i++);
            if(x <= ')') {
                if(x == ')') {
                    depth--;
                } else if(x == '(') {
                    depth++;
                }
            }
        } while(depth > 0);
        return i;
    }

    private static int nextTerminator(String signature, int i) {
        final int comma = signature.indexOf(',', i);
        final int close = signature.indexOf(')', i);
        return comma == -1
                ? close
                : close == -1
                    ? comma
                    : Math.min(comma, close);
    }
}
