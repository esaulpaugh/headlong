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
import java.util.List;
import java.util.Map;

import static com.esaulpaugh.headlong.abi.ABIType.EMPTY_ARRAY;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_ARRAY_CLASS;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS;
import static com.esaulpaugh.headlong.abi.TupleType.EMPTY_TUPLE_STRING;

/** Creates the appropriate {@link ABIType} object for a given type string. */
public final class TypeFactory {

    private TypeFactory() {}

    static final int ADDRESS_BIT_LEN = 160;

    private static final int DECIMAL_BIT_LEN = 168;
    private static final int DECIMAL_SCALE = 10;

    private static final int FIXED_BIT_LEN = 128;
    private static final int FIXED_SCALE = 18;

    private static final int FUNCTION_BYTE_LEN = 24;

    private static final int MAX_LENGTH_CHARS = 2_000;

    private static final Map<String, ABIType<?>> BASE_TYPE_MAP;

    static {
        BASE_TYPE_MAP = new HashMap<>(256);

        for(int n = 8; n <= 32; n += 8) mapInt("int" + n, n, false);
        for(int n = 40; n <= 64; n += 8) mapLong("int" + n, n, false);
        for(int n = 72; n <= 256; n += 8) mapBigInteger("int" + n, n, false);

        for(int n = 8; n <= 24; n += 8) mapInt("uint" + n, n, true);
        for(int n = 32; n <= 56; n += 8) mapLong("uint" + n, n, true);
        for(int n = 64; n <= 256; n += 8) mapBigInteger("uint" + n, n, true);

        for (int n = 1; n <= 32; n++) {
            mapByteArray("bytes" + n, n);
        }

        BASE_TYPE_MAP.put("address", new AddressType());
        mapByteArray("function", FUNCTION_BYTE_LEN);
        mapByteArray("bytes", DYNAMIC_LENGTH);
        BASE_TYPE_MAP.put("string", new ArrayType<ByteType, String>("string", STRING_CLASS, ByteType.INSTANCE, DYNAMIC_LENGTH, STRING_ARRAY_CLASS));

        BASE_TYPE_MAP.put("fixed128x18", new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false));
        BASE_TYPE_MAP.put("ufixed128x18", new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true));
        BASE_TYPE_MAP.put("fixed168x10", new BigDecimalType("fixed168x10", DECIMAL_BIT_LEN, DECIMAL_SCALE, false));

        BASE_TYPE_MAP.put("decimal", BASE_TYPE_MAP.get("fixed168x10"));

        BASE_TYPE_MAP.put("int", BASE_TYPE_MAP.get("int256"));
        BASE_TYPE_MAP.put("uint", BASE_TYPE_MAP.get("uint256"));
        BASE_TYPE_MAP.put("fixed", BASE_TYPE_MAP.get("fixed128x18"));
        BASE_TYPE_MAP.put("ufixed", BASE_TYPE_MAP.get("ufixed128x18"));

        BASE_TYPE_MAP.put("bool", BooleanType.INSTANCE);
    }

    static Map<String, ABIType<?>> getBaseTypeMap() {
        return Collections.unmodifiableMap(BASE_TYPE_MAP);
    }

    private static void mapInt(String type, int bitLen, boolean unsigned) {
        BASE_TYPE_MAP.put(type, new IntType(type, bitLen, unsigned));
    }

    private static void mapLong(String type, int bitLen, boolean unsigned) {
        BASE_TYPE_MAP.put(type, new LongType(type, bitLen, unsigned));
    }

    private static void mapBigInteger(String type, int bitLen, boolean unsigned) {
        BASE_TYPE_MAP.put(type, new BigIntegerType(type, bitLen, unsigned));
    }

    private static void mapByteArray(String type, int arrayLen) {
        BASE_TYPE_MAP.put(type, new ArrayType<ByteType, byte[]>(type, byte[].class, ByteType.INSTANCE, arrayLen, byte[][].class));
    }

    @SuppressWarnings("unchecked")
    public static <T extends ABIType<?>> T create(String rawType) {
        return (T) build(rawType, null, null);
    }

    @SuppressWarnings("unchecked")
    public static ABIType<Object> createNonCapturing(String rawType) {
        return (ABIType<Object>) build(rawType, null, null);
    }

    /** If you don't need any {@code elementNames}, use {@link TypeFactory#create(String)}. */
    public static TupleType createTupleTypeWithNames(String rawType, String... elementNames) {
        return (TupleType) build(rawType, elementNames, null);
    }

    static ABIType<?> build(String rawType, String[] elementNames, ABIType<?> baseType) {
        if(rawType.length() > MAX_LENGTH_CHARS) {
            throw new IllegalArgumentException("type length exceeds maximum: " + rawType.length() + " > " + MAX_LENGTH_CHARS);
        }
        return buildUnchecked(rawType, elementNames, baseType);
    }

    private static ABIType<?> buildUnchecked(final String rawType, final String[] elementNames, ABIType<?> baseType) {
        try {
            final int lastCharIdx = rawType.length() - 1;
            if (rawType.charAt(lastCharIdx) == ']') { // array

                final int secondToLastCharIdx = lastCharIdx - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIdx);

                final ABIType<?> elementType = buildUnchecked(rawType.substring(0, arrayOpenIndex), null, baseType);
                final String type = elementType.canonicalType + rawType.substring(arrayOpenIndex);
                final int length = arrayOpenIndex == secondToLastCharIdx ? DYNAMIC_LENGTH : parseLen(rawType.substring(arrayOpenIndex + 1, lastCharIdx));
                return new ArrayType<>(type, elementType.arrayClass(), elementType, length, null);
            }
            if(baseType != null || (baseType = resolveBaseType(rawType, elementNames)) != null) {
                return baseType;
            }
        } catch (StringIndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
            /* fall through */
        }
        throw new IllegalArgumentException("unrecognized type: \"" + rawType + '"');
    }

    private static int parseLen(String lenStr) {
        try {
//            final char first = rawType.charAt(start);
//            if(leadDigitValid(first) || (first == '0' && end - start == 1)) {
//                return Integer.parseInt(rawType, start, end, 10); // Java 9+
//            }
            if(leadDigitValid(lenStr.charAt(0)) || "0".equals(lenStr)) {
                return Integer.parseInt(lenStr);
            }
        } catch (NumberFormatException ignored) {
            /* fall through */
        }
        throw new IllegalArgumentException("bad array length");
    }

    private static ABIType<?> resolveBaseType(final String baseTypeStr, final String[] elementNames) {
        if (baseTypeStr.charAt(0) == '(') {
            return parseTupleType(baseTypeStr, elementNames);
        }
        final ABIType<?> ret = BASE_TYPE_MAP.get(baseTypeStr);
        return ret != null ? ret : tryParseFixed(baseTypeStr);
    }

    private static BigDecimalType tryParseFixed(final String type) {
        final int idx = type.indexOf("fixed");
        boolean unsigned = false;
        if (idx == 0 || (unsigned = (idx == 1 && type.charAt(0) == 'u'))) {
            final int indexOfX = type.lastIndexOf('x');
            try {
                final String mStr = type.substring(idx + "fixed".length(), indexOfX);
                final String nStr = type.substring(indexOfX + 1); // everything after x
                if (leadDigitValid(mStr.charAt(0)) && leadDigitValid(nStr.charAt(0))) { // starts with a digit 1-9
                    final int M = Integer.parseInt(mStr); // no parseUnsignedInt on older Android versions?
                    final int N = Integer.parseInt(nStr);
                    if (Integers.isMultiple(M, 8) && M <= 256 && N <= 80) { // no multiples of 8 less than 8 except 0
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

    private static TupleType parseTupleType(final String rawTypeStr, final String[] elementNames) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final int len = rawTypeStr.length();
        if (len == 2 && rawTypeStr.equals(EMPTY_TUPLE_STRING)) return TupleType.EMPTY;
        final List<ABIType<?>> elements = new ArrayList<>();
        int argEnd = 1;
        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        try {
            do {
                final int argStart = argEnd;
                switch (rawTypeStr.charAt(argStart)) {
                case ')':
                case ',': return null;
                case '(': argEnd = nextTerminator(rawTypeStr, findSubtupleEnd(rawTypeStr, argStart)); break;
                default: argEnd = nextTerminator(rawTypeStr, argStart);
                }
                final ABIType<?> e = buildUnchecked(rawTypeStr.substring(argStart, argEnd), null, null);
                canonicalBuilder.append(e.canonicalType).append(',');
                dynamic |= e.dynamic;
                elements.add(e);
            } while (rawTypeStr.charAt(argEnd++) != ')');
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("@ index " + elements.size() + ", " + iae.getMessage(), iae);
        }
        if(elementNames != null && elementNames.length != elements.size()) {
            throw new IllegalArgumentException("expected " + elements.size() + " element names but found " + elementNames.length);
        }
        return argEnd == len
                ? new TupleType(
                    canonicalBuilder.deleteCharAt(canonicalBuilder.length() - 1).append(')').toString(),
                    dynamic,
                    elements.toArray(EMPTY_ARRAY),
                    elementNames
                )
                : null;
    }

    private static int nextTerminator(String signature, int i) {
        char c;
        do {
            c = signature.charAt(++i);
        } while (c != ',' && c != ')');
        return i;
    }

    private static int findSubtupleEnd(String parentTypeString, int i) {
        int depth = 1;
        do {
            char x = parentTypeString.charAt(++i);
            if(x <= ')') {
                if(x == ')') {
                    if(depth <= 1) {
                        return i;
                    }
                    depth--;
                } else if(x == '(') {
                    depth++;
                }
            }
        } while(true);
    }
}
