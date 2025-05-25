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
    private static final int NESTING_LIMIT = 80;
    private static final ABIType<?> STRING = UnitType.get("string");
    private static final ABIType<?> BYTES = UnitType.get("bytes");
    private static final ABIType<?> BYTES4 = UnitType.get("bytes4");
    private static final ABIType<?> BYTES32 = UnitType.get("bytes32");
    private static final ABIType<?> LEGACY_STRING = UnitType.getLegacy("string");
    private static final ABIType<?> LEGACY_BYTES = UnitType.getLegacy("bytes");
    private static final ABIType<?> LEGACY_BYTES4 = UnitType.getLegacy("bytes4");
    private static final ABIType<?> LEGACY_BYTES32 = UnitType.getLegacy("bytes32");
    private static final ABIType<?> BOOL = UnitType.get("bool");
    private static final ABIType<?> UINT_256 = UnitType.get("uint256");
    private static final ABIType<?> UINT_8 = UnitType.get("uint8");
    private static final ABIType<?> UINT_32 = UnitType.get("uint32");

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
        return buildUnchecked(new CharSequenceView(rawType), elementNames, baseType, flags);
    }

    private static ABIType<?> buildUnchecked(final CharSequenceView rawType, final String[] elementNames, final TupleType<?> baseType, final int flags) {
        try {
            final int len = rawType.length();
            if (rawType.charAt(len - 1) == ']') { // array
                final int arrayOpenIndex = rawType.lastArrayOpen(len - 2);
                final ABIType<?> elementType = buildUnchecked(rawType.subSequence(0, arrayOpenIndex), null, baseType, flags);
                final String type = new StringBuilder(elementType.canonicalType).append(rawType, arrayOpenIndex, len).toString();
                final int length = arrayOpenIndex == len - 2 ? DYNAMIC_LENGTH : parseArrayLen(rawType, arrayOpenIndex + 1, len - 1);
                return new ArrayType<>(type, elementType.arrayClass(), elementType, length, null, flags);
            }
            if (rawType.charAt(0) == '(') {
                if (baseType != null) {
                    if (len == baseType.canonicalType.length()) {
                        return baseType;
                    }
                } else {
                    return parseTupleType(rawType, elementNames, flags);
                }
            } else {
                ABIType<?> t = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? UnitType.getLegacy(rawType) : UnitType.get(rawType);
                return t != null ? t : tryParseFixed(rawType.toString());
            }
        } catch (IndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
        }
        throw unrecognizedType(rawType);
    }

    private static IllegalArgumentException unrecognizedType(CharSequenceView rawType) {
        return unrecognizedType(rawType.toString());
    }

    private static IllegalArgumentException unrecognizedType(String rawType) {
        return new IllegalArgumentException("unrecognized type: \"" + rawType + '"');
    }

    private static boolean leadDigitValid(char c) {
        return (char)(c - '1') < 9; // cast to wrap negative vals, 1-9 allowed
    }

    private static int parseArrayLen(CharSequenceView rawType, int i, final int end) {
        if (leadDigitValid(rawType.charAt(i)) || end - i == 1) {
            long len = 0;
            while (true) {
                final int d = rawType.charAt(i) - '0';
                if ((char)d > 9 || (len = len * 10 + d) > Integer.MAX_VALUE) {
                    break;
                }
                if (++i == end) {
                    return (int)len;
                }
            }
        }
        throw new IllegalArgumentException("bad array length");
    }

    private static BigDecimalType tryParseFixed(final String rawType) {
        final int idx = rawType.indexOf("fixed");
        if (idx == 0 || (idx == 1 && rawType.charAt(0) == 'u')) {
            final int indexOfX = rawType.lastIndexOf('x');
            if (leadDigitValid(rawType.charAt(idx + "fixed".length())) && leadDigitValid(rawType.charAt(indexOfX + 1))) { // starts with a digit 1-9
                try {
                    final int M = Integer.parseInt(rawType.substring(idx + "fixed".length(), indexOfX)); // no parseUnsignedInt on older Android versions?
                    final int N = Integer.parseInt(rawType.substring(indexOfX + 1)); // everything after x
                    if (Integers.isMultiple(M, 8) && M <= 256 && N <= 80) { // no multiples of 8 less than 8 except 0
                        return new BigDecimalType(rawType, M, N, idx == 1);
                    }
                } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    /* fall through */
                }
            }
        }
        throw unrecognizedType(rawType);
    }

    private static TupleType<?> parseTupleType(final CharSequenceView rawType, final String[] elementNames, final int flags) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final int len = rawType.length();
        if (len == 2 && rawType.charAt(1) == ')') {
            return TupleType.empty(flags);
        }
        ABIType<?>[] elements = new ABIType[8];
        int argEnd = 1;
        final StringBuilder canonicalType = new StringBuilder(len).append('(');
        boolean dynamic = false;
        int i = 0;
        try {
            for ( ; true; canonicalType.append(',')) {
                final int argStart = argEnd;
                ABIType<?> e = null;
                switch (rawType.charAt(argStart)) {
                case ')':
                case ',': throw unrecognizedType(rawType);
                case '(': argEnd = nextTerminator(rawType, findSubtupleEnd(rawType, argStart + 1)); break;
// ======================= OPTIONAL FAST PATHS FOR COMMON TYPES TO SKIP buildUnchecked =================================
                case 'a': {
                    long val = rawType.getFourCharLong(argStart);
                    if (val == CharSequenceView.fourCharLong("addr")) {
                        val = rawType.getFourCharLong(argStart + 4);
                        if (val == CharSequenceView.fourCharLong("ess,") || val == CharSequenceView.fourCharLong("ess)")) {
                            e = AddressType.INSTANCE;
                            argEnd = argStart + "address".length();
                            break;
                        }
                    }
                    argEnd = nextTerminator(rawType, argStart + 1);
                    break;
                }
                case 'b': {
                    final long val = rawType.getFourCharLong(argStart + 1);
                    if (val == CharSequenceView.fourCharLong("ytes")) {
                        argEnd = nextTerminator(rawType, argStart + 5);
                        switch (argEnd - argStart) {
                        case 5: e = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? LEGACY_BYTES : BYTES; break;
                        case 6: if (rawType.charAt(argStart + 5) == '4') e = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? LEGACY_BYTES4 : BYTES4; break;
                        case 7: if (rawType.charAt(argStart + 5) == '3' && rawType.charAt(argStart + 6) == '2') e = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? LEGACY_BYTES32 : BYTES32; break;
                        }
                    } else if (val == CharSequenceView.fourCharLong("ool,") || val == CharSequenceView.fourCharLong("ool)")) {
                        e = BOOL;
                        argEnd = argStart + "bool".length();
                    } else {
                        argEnd = nextTerminator(rawType, argStart + 1);
                    }
                    break;
                }
                case 's': {
                    argEnd = nextTerminator(rawType, argStart + 1);
                    if (argEnd - argStart == "string".length()
                                    && rawType.charAt(argStart + 5) == 'g'
                                    && rawType.getFourCharLong(argStart + 1) == CharSequenceView.fourCharLong("trin")) {
                        e = (flags & ABIType.FLAG_LEGACY_DECODE) != 0 ? LEGACY_STRING : STRING;
                    }
                    break;
                }
                case 'u': {
                    argEnd = nextTerminator(rawType, argStart + 1);
                    final int argLen = argEnd - argStart;
                    if (argLen == 7 && rawType.getFourCharLong(argStart + 1) == CharSequenceView.fourCharLong("int2")
                                    && rawType.charAt(argStart + 5) == '5'
                                    && rawType.charAt(argStart + 6) == '6') {
                        e = UINT_256;
                    } else if (rawType.getFourCharLong(argStart) == CharSequenceView.fourCharLong("uint")) {
                        switch (argLen) {
                        case 4: e = UINT_256; break; // "uint"
                        case 5: if (rawType.charAt(argStart + 4) == '8') e = UINT_8; break;
                        case 6: if (rawType.charAt(argStart + 4) == '3' && rawType.charAt(argStart + 5) == '2') e = UINT_32; break;
                        }
                    }
                    break;
                }
// ======================= END OPTIONAL FAST PATHS =====================================================================
                default: argEnd = nextTerminator(rawType, argStart + 1);
                }
                if (e == null) {
                    e = buildUnchecked(rawType.subSequence(argStart, argEnd), null, null, flags);
                }
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

    private static int nextTerminator(CharSequenceView signature, int i) {
        for ( ; ; i++) {
            switch (signature.charAt(i)) {
            case ',':
            case ')': return i;
            }
        }
    }

    private static int findSubtupleEnd(CharSequenceView parentTypeString, int i) {
        int depth = 1;
        do {
            switch (parentTypeString.charAt(i++)) {
            case '(':
                if (++depth >= NESTING_LIMIT) {
                    throw new IllegalArgumentException("exceeds nesting limit: " + NESTING_LIMIT);
                }
                continue;
            case ')':
                if (depth == 1) {
                    return i;
                }
                depth--;
            }
        } while (true);
    }
}
