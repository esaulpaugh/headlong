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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_ARRAY_CLASS;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS;

/** Creates the appropriate {@link ABIType} object for a given type string. */
public final class TypeFactory {

    private TypeFactory() {}

    static final int ADDRESS_BIT_LEN = 160;

    private static final int FIXED_BIT_LEN = 128;
    private static final int FIXED_SCALE = 18;

    private static final int FUNCTION_BYTE_LEN = 24;

    private static final int MAX_LENGTH_CHARS = 2_000;

    private static final Map<String, ABIType<?>> BASE_TYPE_MAP;
    private static final Map<String, ABIType<?>> LEGACY_BASE_TYPE_MAP;

    static {
        final Map<String, ABIType<?>> local = new HashMap<>(256);

        // optimized insertion order
        local.put("string", new ArrayType<ByteType, Byte, String>("string", STRING_CLASS, ByteType.INSTANCE, DYNAMIC_LENGTH, STRING_ARRAY_CLASS, ABIType.FLAGS_NONE));
        local.put("bool", BooleanType.INSTANCE);

        for (int n = 1; n <= 32; n++) {
            mapByteArray(local, "bytes" + n, n);
        }
        mapByteArray(local, "function", FUNCTION_BYTE_LEN);
        mapByteArray(local, "bytes", DYNAMIC_LENGTH);

        for (int n = 8; n <= 24; n += 8) mapInt(local, "uint" + n, n, true);
        for (int n = 32; n <= 56; n += 8) mapLong(local, "uint" + n, n, true);
        for (int n = 64; n <= 256; n += 8) mapBigInteger(local, "uint" + n, n, true);

        local.put("uint", local.get("uint256"));

        mapBigInteger(local, "int256", 256, false);
        local.put("int", local.get("int256"));

        for (int n = 8; n <= 32; n += 8) mapInt(local, "int" + n, n, false);
        for (int n = 40; n <= 64; n += 8) mapLong(local, "int" + n, n, false);
        for (int n = 72; n < 256; n += 8) mapBigInteger(local, "int" + n, n, false);

        local.put("address", AddressType.INSTANCE);

        local.put("fixed128x18", new BigDecimalType("fixed128x18", FIXED_BIT_LEN, FIXED_SCALE, false));
        local.put("ufixed128x18", new BigDecimalType("ufixed128x18", FIXED_BIT_LEN, FIXED_SCALE, true));

        local.put("decimal", local.get("int168"));
        local.put("fixed", local.get("fixed128x18"));
        local.put("ufixed", local.get("ufixed128x18"));

        final Map<String, ABIType<?>> localLegacy = new HashMap<>(256);
        for (Map.Entry<String, ABIType<?>> e : local.entrySet()) {
            ABIType<?> value = e.getValue();
            if (value instanceof ArrayType) {
                final ArrayType<?, ?, ?> at = value.asArrayType();
                value = new ArrayType<>(at.canonicalType, at.clazz, ByteType.INSTANCE, at.getLength(), at.arrayClass(), ABIType.FLAG_LEGACY_DECODE);
            }
            localLegacy.put(e.getKey(), value);
        }

        BASE_TYPE_MAP = Collections.unmodifiableMap(local);
        LEGACY_BASE_TYPE_MAP = Collections.unmodifiableMap(localLegacy);
    }

    private static void mapInt(Map<String, ABIType<?>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, new IntType(type, bitLen, unsigned));
    }

    private static void mapLong(Map<String, ABIType<?>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, new LongType(type, bitLen, unsigned));
    }

    private static void mapBigInteger(Map<String, ABIType<?>> map, String type, int bitLen, boolean unsigned) {
        map.put(type, new BigIntegerType(type, bitLen, unsigned));
    }

    private static void mapByteArray(Map<String, ABIType<?>> map, String type, int arrayLen) {
        map.put(type, new ArrayType<ByteType, Byte, byte[]>(type, byte[].class, ByteType.INSTANCE, arrayLen, byte[][].class, ABIType.FLAGS_NONE));
    }

    /**
     * If the compiler can't infer the return type, use a type witness.
     * <p>
     * From Java:
     * <blockquote><pre>
     *     {@code TypeFactory.<TupleType<?>>create("(int8)").<IntType>get(0).encode(12)}
     * </pre></blockquote><p>
     * <p>
     * From Kotlin:
     * <blockquote><pre>
     *     {@code TypeFactory.create<TupleType<*>>("(int8)").get<IntType>(0).encode(12)}
     * </pre></blockquote><p>
     * @param rawType
     * @return
     * @param <T>
     */
    public static <T extends ABIType<?>> T create(String rawType) {
        return create(ABIType.FLAGS_NONE, rawType);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ABIType<?>> T create(int flags, String rawType) {
        return (T) build(rawType, null, null, flags);
    }

    /** If you don't need any {@code elementNames}, use {@link TypeFactory#create(String)}. */
    @SuppressWarnings("unchecked")
    public static <X extends Tuple> TupleType<X> createTupleTypeWithNames(String rawType, String... elementNames) {
        return (TupleType<X>) build(rawType, elementNames, null, ABIType.FLAGS_NONE)
                .asTupleType();
    }

    static ABIType<?> build(String rawType, String[] elementNames, TupleType<?> baseType, int flags) {
        if (rawType.length() > MAX_LENGTH_CHARS) {
            throw new IllegalArgumentException("type length exceeds maximum: " + rawType.length() + " > " + MAX_LENGTH_CHARS);
        }
        return buildUnchecked(rawType, elementNames, baseType, flags);
    }

    private static ABIType<?> buildUnchecked(final String rawType, final String[] elementNames, TupleType<?> baseType, int flags) {
        try {
            final int lastCharIdx = rawType.length() - 1;
            if (rawType.charAt(lastCharIdx) == ']') { // array
                final int secondToLastCharIdx = lastCharIdx - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIdx);

                final ABIType<?> elementType = buildUnchecked(rawType.substring(0, arrayOpenIndex), null, baseType, flags);
                final String type = elementType.canonicalType + rawType.substring(arrayOpenIndex);
                final int length = arrayOpenIndex == secondToLastCharIdx ? DYNAMIC_LENGTH : parseLen(rawType.substring(arrayOpenIndex + 1, lastCharIdx));
                return new ArrayType<>(type, elementType.arrayClass(), elementType, length, null, flags);
            }
            if (rawType.charAt(0) == '(') {
                return baseType != null ? baseType : parseTupleType(rawType, elementNames, flags);
            } else {
                ABIType<?> t = ((flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? LEGACY_BASE_TYPE_MAP : BASE_TYPE_MAP).get(rawType);
                return t != null ? t : tryParseFixed(rawType);
            }
        } catch (StringIndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
            throw unrecognizedType(rawType);
        }
    }

    private static IllegalArgumentException unrecognizedType(String rawType) {
        return new IllegalArgumentException("unrecognized type: \"" + rawType + '"');
    }

    private static int parseLen(String lenStr) {
        try {
//            final char first = rawType.charAt(start);
//            if (leadDigitValid(first) || (first == '0' && end - start == 1)) {
//                return Integer.parseInt(rawType, start, end, 10); // Java 9+
//            }
            if (leadDigitValid(lenStr.charAt(0)) || "0".equals(lenStr)) {
                return Integer.parseInt(lenStr);
            }
        } catch (NumberFormatException ignored) {
            /* fall through */
        }
        throw new IllegalArgumentException("bad array length");
    }

    private static BigDecimalType tryParseFixed(final String rawType) {
        final int idx = rawType.indexOf("fixed");
        boolean unsigned = false;
        if (idx == 0 || (unsigned = (idx == 1 && rawType.charAt(0) == 'u'))) {
            final int indexOfX = rawType.lastIndexOf('x');
            try {
                final String mStr = rawType.substring(idx + "fixed".length(), indexOfX);
                final String nStr = rawType.substring(indexOfX + 1); // everything after x
                if (leadDigitValid(mStr.charAt(0)) && leadDigitValid(nStr.charAt(0))) { // starts with a digit 1-9
                    final int M = Integer.parseInt(mStr); // no parseUnsignedInt on older Android versions?
                    final int N = Integer.parseInt(nStr);
                    if (Integers.isMultiple(M, 8) && M <= 256 && N <= 80) { // no multiples of 8 less than 8 except 0
                        return new BigDecimalType(rawType, M, N, unsigned);
                    }
                }
            } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                /* fall through */
            }
        }
        throw unrecognizedType(rawType);
    }

    private static boolean leadDigitValid(char c) {
        return c > '0' && c <= '9';
    }

    static StringBuilder newTypeBuilder() {
        return new StringBuilder(40).append('(');
    }

    private static TupleType<?> parseTupleType(final String rawType, final String[] elementNames, final int flags) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final int len = rawType.length();
        if (len == 2 && "()".equals(rawType)) return TupleType.empty(flags);
        ABIType<?>[] elements = new ABIType[8];
        int argEnd = 1;
        final StringBuilder canonicalType = newTypeBuilder();
        boolean dynamic = false;
        int i = 0;
        try {
            for (;;) {
                final int argStart = argEnd;
                switch (rawType.charAt(argStart)) {
                case ')':
                case ',': throw unrecognizedType(rawType);
                case '(': argEnd = nextTerminator(rawType, findSubtupleEnd(rawType, argStart + 1)); break;
                default: argEnd = nextTerminator(rawType, argStart + 1);
                }
                final ABIType<?> e = buildUnchecked(rawType.substring(argStart, argEnd), null, null, flags);
                canonicalType.append(e.canonicalType);
                dynamic |= e.dynamic;
                elements[i++] = e;
                if (rawType.charAt(argEnd++) == ')') {
                    if (argEnd != len) {
                        throw unrecognizedType(rawType);
                    }
                    return new TupleType<>(
                            canonicalType.append(')').toString(),
                            dynamic,
                            Arrays.copyOf(elements, i),
                            elementNames,
                            null,
                            flags
                    );
                }
                if (i == elements.length) {
                    elements = Arrays.copyOf(elements, i << 1);
                }
                canonicalType.append(',');
            }
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("@ index " + i + ", " + iae.getMessage(), iae);
        }
    }

    private static int nextTerminator(String signature, int i) {
        for ( ; ; i++) {
            switch (signature.charAt(i)) {
            case ',':
            case ')': return i;
            }
        }
    }

    private static int findSubtupleEnd(String parentTypeString, int i) {
        int depth = 0;
        do {
            switch (parentTypeString.charAt(i++)) {
            case '(':
                depth++;
                continue;
            case ')':
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        } while (true);
    }
}
