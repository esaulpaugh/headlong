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

import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;

/** Creates the appropriate {@link ABIType} object for a given type string. */
public final class TypeFactory {

    static {
        UnitType.initInstances(); // initialize type maps
    }

    private static final int MAX_LENGTH_CHARS = 1_600;

    private TypeFactory() {}

    /**
     * Creates an {@link ABIType}. If the compiler can't infer the return type, use a type witness.
     * <p>
     * From Java:
     * <blockquote><pre>
     *     {@code TypeFactory.<TupleType<?>>create("(int8)").<IntType>get(0).encode(12)}
     * </pre></blockquote><p>
     * From Kotlin:
     * <blockquote><pre>
     *     {@code TypeFactory.create<TupleType<*>>("(int8)").get<IntType>(0).encode(12)}
     * </pre></blockquote>
     *
     * @param rawType   the type's string representation, e.g. "int" or "(address,bytes)[]"
     * @return  the type
     * @param <T>   the expected return type, e.g. {@link IntType} or {@link ABIType}&#60;Integer&#62;
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
        return (TupleType<X>) build(rawType, elementNames, null, ABIType.FLAGS_NONE);
    }

    static ABIType<?> build(String rawType, String[] elementNames, TupleType<?> baseType, int flags) {
        if (rawType.length() > MAX_LENGTH_CHARS) {
            throw new IllegalArgumentException("type length exceeds maximum: " + rawType.length() + " > " + MAX_LENGTH_CHARS);
        }
        return buildUnchecked(rawType, elementNames, baseType, flags);
    }

    private static ABIType<?> buildUnchecked(final String rawType, final String[] elementNames, final TupleType<?> baseType, final int flags) {
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
                if (baseType != null) {
                    if (rawType.length() == baseType.canonicalType.length()) {
                        return baseType;
                    }
                } else {
                    return parseTupleType(rawType, elementNames, flags);
                }
            } else {
                ABIType<?> t = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? UnitType.getLegacy(rawType) : UnitType.get(rawType);
                return t != null ? t : tryParseFixed(rawType);
            }
        } catch (StringIndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
        }
        throw unrecognizedType(rawType);
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

    private static TupleType<?> parseTupleType(final String rawType, final String[] elementNames, final int flags) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final int len = rawType.length();
        if (len == 2 && "()".equals(rawType)) return TupleType.empty(flags);
        ABIType<?>[] elements = new ABIType[8];
        int argEnd = 1;
        final StringBuilder canonicalType = TupleType.newTypeBuilder();
        boolean dynamic = false;
        int i = 0;
        try {
            for ( ; true; canonicalType.append(',')) {
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
                            null,
                            flags
                    );
                }
                if (i == elements.length) {
                    elements = Arrays.copyOf(elements, i << 1);
                }
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
