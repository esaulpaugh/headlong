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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Creates the appropriate {@link ABIType} object for a given type string. */
final class TypeFactory {

    private static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    private static final int ADDRESS_BIT_LEN = 160;

    private static final int DECIMAL_BIT_LEN = 128;
    private static final int DECIMAL_SCALE = 10;

    private static final int FIXED_BIT_LEN = 128;
    private static final int FIXED_SCALE = 18;

    private static final int FUNCTION_BYTE_LEN = 24;

    private static final Map<String, Supplier<ABIType<?>>> SUPPLIER_MAP;

    static {
        final Map<String, Supplier<ABIType<?>>> lambdaMap = new HashMap<>(256);

        for(int n = 8; n <= 32; n += 8) {
            mapInt(lambdaMap, "int" + n, n, false);
        }
        for(int n = 40; n <= 64; n += 8) {
            mapLong(lambdaMap, "int" + n, n, false);
        }
        for(int n = 72; n <= 256; n += 8) {
            mapBigInteger(lambdaMap, "int" + n, n, false);
        }
        lambdaMap.put("int", lambdaMap.get("int256"));

        for(int n = 8; n <= 24; n += 8) {
            mapInt(lambdaMap, "uint" + n, n, true);
        }
        for(int n = 32; n <= 56; n += 8) {
            mapLong(lambdaMap, "uint" + n, n, true);
        }
        for(int n = 64; n <= 256; n += 8) {
            mapBigInteger(lambdaMap, "uint" + n, n, true);
        }
        lambdaMap.put("uint", lambdaMap.get("uint256"));
        mapBigInteger(lambdaMap, "address", ADDRESS_BIT_LEN, true);

        for (int n = 1; n <= 32; n++) {
            mapStaticByteArray(lambdaMap, "bytes" + n, n);
        }
        mapStaticByteArray(lambdaMap, "function", FUNCTION_BYTE_LEN);
        lambdaMap.put("bytes", () -> new ArrayType<>("bytes", ArrayType.BYTE_ARRAY_CLASS, true, ByteType.UNSIGNED, ArrayType.DYNAMIC_LENGTH, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME));
        lambdaMap.put("string", () -> new ArrayType<>("string", ArrayType.STRING_CLASS, true, ByteType.UNSIGNED, ArrayType.DYNAMIC_LENGTH, ArrayType.STRING_ARRAY_CLASS_NAME));

        lambdaMap.put("fixed128x18", () -> new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false));
        lambdaMap.put("ufixed128x18", () -> new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true));
        lambdaMap.put("fixed", lambdaMap.get("fixed128x18"));
        lambdaMap.put("ufixed", lambdaMap.get("ufixed128x18"));
        lambdaMap.put("decimal", () -> new BigDecimalType("decimal", DECIMAL_BIT_LEN, DECIMAL_SCALE, false));

        lambdaMap.put("bool", BooleanType::new);

        SUPPLIER_MAP = Collections.unmodifiableMap(lambdaMap);
    }

    private static void mapInt(Map<String, Supplier<ABIType<?>>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, () -> new IntType(type, bitLen, unsigned));
    }

    private static void mapLong(Map<String, Supplier<ABIType<?>>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, () -> new LongType(type, bitLen, unsigned));
    }

    private static void mapBigInteger(Map<String, Supplier<ABIType<?>>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, () -> new BigIntegerType(type, bitLen, unsigned));
    }

    private static void mapStaticByteArray(Map<String, Supplier<ABIType<?>>> map, String type, int arrayLen) {
        map.put(type, () -> new ArrayType<>(type, ArrayType.BYTE_ARRAY_CLASS, false, ByteType.UNSIGNED, arrayLen, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME));
    }

    static ABIType<?> create(String rawType, String name) {
        return buildType(rawType, null)
                .setName(name);
    }

    static ABIType<?> createFromBase(TupleType baseType, String typeSuffix, String name) {
        return buildType(baseType.canonicalType + typeSuffix, baseType)
                .setName(name);
    }

    private static ABIType<?> buildType(final String rawType, ABIType<?> baseType) {
        try {
            final int lastCharIndex = rawType.length() - 1;
            if (rawType.charAt(lastCharIndex) == ']') { // array

                final int secondToLastCharIndex = lastCharIndex - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIndex);

                final ABIType<?> elementType = buildType(rawType.substring(0, arrayOpenIndex), baseType);
                final String type = elementType.canonicalType + rawType.substring(arrayOpenIndex);
                final int length = arrayOpenIndex == secondToLastCharIndex ? ArrayType.DYNAMIC_LENGTH : parseLen(rawType, arrayOpenIndex + 1, lastCharIndex);
                final boolean dynamic = length == ArrayType.DYNAMIC_LENGTH || elementType.dynamic;
                final String arrayClassName = elementType.arrayClassName();
                @SuppressWarnings("unchecked")
                final Class<Object> arrayClass = (Class<Object>) Class.forName(arrayClassName, false, CLASS_LOADER);
                return new ArrayType<ABIType<?>, Object>(type, arrayClass, dynamic, elementType, length, '[' + arrayClassName);
            }
            if(baseType != null || (baseType = resolveBaseType(rawType)) != null) {
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
            final String lengthStr = rawType.substring(startLen, lastCharIndex);
            final int length = Integer.parseInt(lengthStr);
            if (length >= 0) {
                if(lengthStr.length() > 1 && rawType.charAt(startLen) == '0') {
                    throw new IllegalArgumentException("leading zero in array length");
                }
                return length;
            }
            throw new IllegalArgumentException("negative array length");
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("illegal number format", nfe);
        }
    }

    private static ABIType<?> resolveBaseType(String baseTypeStr) {
        if(baseTypeStr.charAt(0) == '(') {
            return parseTupleType(baseTypeStr);
        }
        Supplier<ABIType<?>> supplier = SUPPLIER_MAP.get(baseTypeStr);
        return supplier != null ? supplier.get() : tryParseFixed(baseTypeStr);
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
        try {
            int argStart = 1; // after opening '('
            int argEnd = 1; // inital value important for empty params case: "()"
            char prevEndChar = ')'; // inital value important for empty params case
            final int end = rawTypeStr.length(); // must be >= 1
            LOOP:
            while (argStart < end) {
                switch (rawTypeStr.charAt(argStart)) {
                case '(': // element is tuple or tuple array
                    argEnd = nextTerminator(rawTypeStr, findSubtupleEnd(rawTypeStr, argStart + 1));
                    break;
                case ')':
                    if(prevEndChar != ',') {
                        break LOOP;
                    }
                    throw new IllegalArgumentException(EMPTY_PARAMETER);
                case ',':
                    if (rawTypeStr.charAt(argEnd) == ')') {
                        break LOOP;
                    }
                    throw new IllegalArgumentException(EMPTY_PARAMETER);
                default: // non-tuple element
                    argEnd = nextTerminator(rawTypeStr, argStart + 1);
                }
                elements.add(buildType(rawTypeStr.substring(argStart, argEnd), null));
                if((prevEndChar = rawTypeStr.charAt(argEnd)) != ',') {
                    break/*LOOP*/;
                }
                argStart = argEnd + 1; // jump over terminator
            }
            if(argEnd == end - 1 && prevEndChar == ')') {
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
