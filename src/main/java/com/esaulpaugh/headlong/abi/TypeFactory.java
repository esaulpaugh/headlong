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
import java.text.ParseException;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.BaseTypeInfo.*;
import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;

/**
 * Creates the appropriate {@link ABIType} object for a given canonical type string.
 */
final class TypeFactory {

    private static final ClassLoader CLASS_LOADER = TypeFactory.class.getClassLoader();

    static ABIType<?> createForTuple(TupleType baseTupleType, String suffix, String name) throws ParseException {
        return create(baseTupleType.canonicalType + suffix, baseTupleType, name);
    }

    static ABIType<?> create(String type, String name) throws ParseException {
        return create(type, null, name);
    }

    static ABIType<?> create(String type, TupleType baseTupleType, String name) throws ParseException {
        try {
            return buildType(type, false, baseTupleType).setName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ABIType<?> buildType(final String type, boolean isArrayElement, final TupleType baseTupleType) throws ParseException, ClassNotFoundException {

        final int idxOfLast = type.length() - 1;

        if(type.charAt(idxOfLast) == ']') { // array

            final int fromIndex = idxOfLast - 1;
            final int arrayOpenIndex = type.lastIndexOf('[', fromIndex);

            final int length;
            if(arrayOpenIndex == fromIndex) { // i.e. []
                length = DYNAMIC_LENGTH;
            } else { // e.g. [4]
                try {
                    length = Integer.parseInt(type.substring(arrayOpenIndex + 1, idxOfLast));
                    if(length < 0) {
                        throw new ParseException("negative array size", arrayOpenIndex + 1);
                    }
                } catch (NumberFormatException nfe) {
                    throw (ParseException) new ParseException("illegal argument", arrayOpenIndex + 1).initCause(nfe);
                }
            }

            final ABIType<?> elementType = buildType(type.substring(0, arrayOpenIndex), true, baseTupleType);
            final String arrayClassName = elementType.arrayClassName();
            @SuppressWarnings("unchecked")
            final Class<Object> arrayClass = (Class<Object>) Class.forName(arrayClassName, false, CLASS_LOADER);
            final boolean dynamic = length == DYNAMIC_LENGTH || elementType.dynamic;
            return new ArrayType<ABIType<?>, Object>(type, arrayClass, dynamic, elementType, length, '[' + arrayClassName);
        } else {
            ABIType<?> baseType = resolveBaseType(type, isArrayElement, baseTupleType);
            if(baseType == null) {
                throw new ParseException("unrecognized type: "
                        + type + " (" + String.format("%040x", new BigInteger(type.getBytes(CHARSET_UTF_8))) + ")", -1);
            }
            return baseType;
        }
    }

    private static ABIType<?> resolveBaseType(final String baseTypeStr, boolean isElement, TupleType baseTupleType) {

        final ABIType<?> type;

        BaseTypeInfo info = BaseTypeInfo.get(baseTypeStr);

        if(info != null) {
            switch (baseTypeStr) { // baseType's hash code already cached due to BaseTypeInfo.get(baseTypeStr)
            case "int8":
            case "int16":
            case "int24":
            case "int32": type = new IntType(baseTypeStr, info.bitLen, false); break;
            case "int40":
            case "int48":
            case "int56":
            case "int64": type = new LongType(baseTypeStr, info.bitLen, false); break;
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
            case "int256": type = new BigIntegerType(baseTypeStr, info.bitLen, false); break;
            case "int": type = new BigIntegerType("int256", info.bitLen, false); break;
            case "uint8":
            case "uint16":
            case "uint24": type = new IntType(baseTypeStr, info.bitLen, true); break;
            case "uint32": type = isElement ? new IntType(baseTypeStr, info.bitLen, true) : new LongType(baseTypeStr, info.bitLen, true); break;
            case "uint40":
            case "uint48":
            case "uint56": type = new LongType(baseTypeStr, info.bitLen, true); break;
            case "uint64": type = isElement ? new LongType(baseTypeStr, info.bitLen, true) : new BigIntegerType(baseTypeStr, info.bitLen, true); break;
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
            case "uint256": type = new BigIntegerType(baseTypeStr, info.bitLen, true); break;
            case "uint": type = new BigIntegerType("uint256", info.bitLen, true); break;
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
            case "bytes32": type = new ArrayType<>(baseTypeStr, ArrayType.BYTE_ARRAY_CLASS, false, ByteType.UNSIGNED, info.arrayLen, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME); break;
            case "bool": type = BooleanType.INSTANCE; break;
            case "bytes": type = new ArrayType<>(baseTypeStr, ArrayType.BYTE_ARRAY_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME); break;
            case "string": type = new ArrayType<>(baseTypeStr, ArrayType.STRING_CLASS, true, ByteType.UNSIGNED, DYNAMIC_LENGTH, ArrayType.STRING_ARRAY_CLASS_NAME); break;
            case "decimal": type = new BigDecimalType(baseTypeStr, info.bitLen, DECIMAL_SCALE, false); break;
            case "fixed":
            case "fixed128x18": type = new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false); break;
            case "ufixed":
            case "ufixed128x18": type = new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true); break;
            default: type = null;
            }
        } else {
            final int len = baseTypeStr.length();
            if(len >= 2 && baseTypeStr.charAt(0) == '(') {
                type = baseTypeStr.charAt(len - 1) == ')'
                        ? baseTupleType
                        : null;
            } else {
                type = tryParseFixed(baseTypeStr);
            }
        }

        return type;
    }

    private static BigDecimalType tryParseFixed(String type) {
        final int idx = type.indexOf("fixed");
        boolean unsigned = false;
        if (idx == 0 || (unsigned = idx == 1 && type.charAt(0) == 'u')) {
            final int indexOfX = type.lastIndexOf('x');
            try {
                // no parseUnsignedInt on Android?
                int M = Integer.parseInt(type.substring(idx + "fixed".length(), indexOfX));
                int N = Integer.parseInt(type.substring(indexOfX + 1)); // everything after x
                if ((M & 0x7) /* mod 8 */ == 0 && M >= 8 && M <= 256
                        && N > 0 && N <= 80) {
                    return new BigDecimalType(type, M, N, unsigned);
                }
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
