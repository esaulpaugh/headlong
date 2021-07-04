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

import com.esaulpaugh.headlong.util.Integers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.abi.ABIType.EMPTY_ARRAY;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_ARRAY_CLASS;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS;

/** Creates the appropriate {@link ABIType} object for a given type string. */
public final class TypeFactory {

    private TypeFactory() {}

    private static final int ADDRESS_BIT_LEN = 160;

    private static final int DECIMAL_BIT_LEN = 128;
    private static final int DECIMAL_SCALE = 10;

    private static final int FIXED_BIT_LEN = 128;
    private static final int FIXED_SCALE = 18;

    private static final int FUNCTION_BYTE_LEN = 24;

    static final Map<String, Supplier<ABIType<?>>> SUPPLIER_MAP;

    static {
        final Map<String, Supplier<ABIType<?>>> lambdaMap = new HashMap<>(256);

        for(int n = 8; n <= 32; n += 8) mapInt(lambdaMap, "int" + n, n, false);
        for(int n = 40; n <= 64; n += 8) mapLong(lambdaMap, "int" + n, n, false);
        for(int n = 72; n <= 256; n += 8) mapBigInteger(lambdaMap, "int" + n, n, false);

        for(int n = 8; n <= 24; n += 8) mapInt(lambdaMap, "uint" + n, n, true);
        for(int n = 32; n <= 56; n += 8) mapLong(lambdaMap, "uint" + n, n, true);
        for(int n = 64; n <= 256; n += 8) mapBigInteger(lambdaMap, "uint" + n, n, true);

        for (int n = 1; n <= 32; n++) {
            mapByteArray(lambdaMap, "bytes" + n, n);
        }

        mapBigInteger(lambdaMap, "address", ADDRESS_BIT_LEN, true);
        mapByteArray(lambdaMap, "function", FUNCTION_BYTE_LEN);
        mapByteArray(lambdaMap, "bytes", DYNAMIC_LENGTH);
        lambdaMap.put("string", () -> new ArrayType<ByteType, String>("string", STRING_CLASS, ByteType.SIGNED, DYNAMIC_LENGTH, STRING_ARRAY_CLASS));

        lambdaMap.put("fixed128x18", () -> new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false));
        lambdaMap.put("ufixed128x18", () -> new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true));
        lambdaMap.put("decimal", () -> new BigDecimalType("decimal", DECIMAL_BIT_LEN, DECIMAL_SCALE, false));

        lambdaMap.put("int", lambdaMap.get("int256"));
        lambdaMap.put("uint", lambdaMap.get("uint256"));
        lambdaMap.put("fixed", lambdaMap.get("fixed128x18"));
        lambdaMap.put("ufixed", lambdaMap.get("ufixed128x18"));

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

    private static void mapByteArray(Map<String, Supplier<ABIType<?>>> map, String type, int arrayLen) {
        map.put(type, () -> new ArrayType<ByteType, byte[]>(type, byte[].class, ByteType.SIGNED, arrayLen, byte[][].class));
    }

    public static <T extends ABIType<?>> T create(String rawType) {
        return create(rawType, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ABIType<?>> T create(String rawType, String name) {
        return (T) build(rawType, null, name);
    }

    static ABIType<?> build(String rawType, TupleType baseType, String name) {
        return _build(rawType, baseType)
                .setName(name);
    }

    private static ABIType<?> _build(final String rawType, ABIType<?> baseType) {
        try {
            final int lastCharIdx = rawType.length() - 1;
            if (rawType.charAt(lastCharIdx) == ']') { // array

                final int secondToLastCharIdx = lastCharIdx - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIdx);

                final ABIType<?> elementType = _build(rawType.substring(0, arrayOpenIndex), baseType);
                final String type = elementType.canonicalType + rawType.substring(arrayOpenIndex);
                final int length = arrayOpenIndex == secondToLastCharIdx ? DYNAMIC_LENGTH : parseLen(rawType.substring(arrayOpenIndex + 1, lastCharIdx));
                return new ArrayType<>(type, elementType.arrayClass(), elementType, length);
            }
            if(baseType != null || (baseType = resolveBaseType(rawType)) != null) {
                return baseType;
            }
        } catch (ClassNotFoundException cnfe) {
            throw new Error(cnfe);
        } catch (StringIndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
            /* fall through */
        }
        throw new IllegalArgumentException("unrecognized type: \"" + rawType + '"');
    }

    private static int parseLen(final String lenStr) {
        try {
            if(leadDigitValid(lenStr.charAt(0)) || lenStr.equals("0")) {
                final int length = Integer.parseInt(lenStr);
                if (length >= 0) {
                    return length;
                }
            }
        } catch (NumberFormatException ignored) {
            /* fall through */
        }
        throw new IllegalArgumentException("bad array length");
    }

    private static ABIType<?> resolveBaseType(final String baseTypeStr) {
        if(baseTypeStr.charAt(0) == '(') {
            return parseTupleType(baseTypeStr);
        }
        Supplier<ABIType<?>> supplier = SUPPLIER_MAP.get(baseTypeStr);
        return supplier != null ? supplier.get() : tryParseFixed(baseTypeStr);
    }

    private static BigDecimalType tryParseFixed(final String type) {
        final int idx = type.indexOf("fixed");
        boolean unsigned = false;
        if (idx == 0 || (unsigned = (idx == 1 && type.charAt(0) == 'u'))) {
            final int indexOfX = type.lastIndexOf('x');
            try {
                final String mStr = type.substring(idx + "fixed".length(), indexOfX);
                final String nStr = type.substring(indexOfX + 1); // everything after x
                if (leadDigitValid(mStr.charAt(0)) && leadDigitValid(nStr.charAt(0))) {
                    final int M = Integer.parseInt(mStr); // no parseUnsignedInt on Android?
                    final int N = Integer.parseInt(nStr);
                    if (Integers.mod(M, 8) == 0 && M >= 8 && M <= 256 && N > 0 && N <= 80) {
                        return new BigDecimalType((unsigned ? "ufixed" : "fixed") + M + 'x' + N, M, N, unsigned);
                    }
                }
            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                /* fall through */
            }
        }
        return null;
    }

    private static boolean leadDigitValid(char c) {
        return c > '0' && c <= '9';
    }

    static final String EMPTY_PARAMETER = "empty parameter";

    private static TupleType parseTupleType(final String rawTypeStr) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final ArrayList<ABIType<?>> elements = new ArrayList<>();
        try {
            int argStart = 1; // after opening '('
            int argEnd = 1; // inital value important for empty params case: "()"
            char prevTerminator = ')'; // inital value important for empty params case
            final int last = rawTypeStr.length() - 1; // must be >= 0
            while (argStart <= last) {
                char c = rawTypeStr.charAt(argStart);
                if(c == ')' || c == ',') {
                    if(c == ')' && prevTerminator != ',') {
                        break;
                    }
                    throw new IllegalArgumentException(EMPTY_PARAMETER);
                }
                argEnd = nextTerminator(rawTypeStr, c == '(' ? findSubtupleEnd(rawTypeStr, argStart) : argStart);
                elements.add(_build(rawTypeStr.substring(argStart, argEnd), null));
                if((prevTerminator = rawTypeStr.charAt(argEnd)) != ',') {
                    break;
                }
                argStart = argEnd + 1; // jump over terminator
            }
            if(argEnd == last && prevTerminator == ')') {
                return TupleType.wrap(elements.toArray(EMPTY_ARRAY));
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("@ index " + elements.size() + ", " + iae.getMessage(), iae);
        }
        return null;
    }

    private static int findSubtupleEnd(String parentTypeString, int i) {
        int depth = 1;
        do {
            char x = parentTypeString.charAt(++i);
            if(x <= ')') {
                if(x == ')') {
                    depth--;
                } else if(x == '(') {
                    depth++;
                }
            }
        } while(depth > 0);
        return i + 1;
    }

    private static int nextTerminator(String signature, int i) {
        final int len = signature.length();
        for( ; i < len; i++) {
            char c = signature.charAt(i);
            if(c == ',' || c == ')') return i;
        }
        return -1;
    }
}
