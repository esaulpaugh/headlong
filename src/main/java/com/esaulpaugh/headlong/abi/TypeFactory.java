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
    private static final ABIType<?> STRING = UnitType.get("string");
    private static final ABIType<?> BYTES = UnitType.get("bytes");
    private static final ABIType<?> BYTES_4 = UnitType.get("bytes4");
    private static final ABIType<?> BYTES_32 = UnitType.get("bytes32");
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
            final int lastCharIdx = len - 1;
            if (rawType.charAt(lastCharIdx) == ']') { // array
                final int secondToLastCharIdx = lastCharIdx - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIdx);

                final ABIType<?> elementType = buildUnchecked(rawType.subSequence(0, arrayOpenIndex), null, baseType, flags);
                final String type = new StringBuilder(elementType.canonicalType).append(rawType, arrayOpenIndex, len).toString();
                final int length = arrayOpenIndex == secondToLastCharIdx ? DYNAMIC_LENGTH : parseArrayLen(rawType, arrayOpenIndex + 1, lastCharIdx);
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
        return (char)(c - '1') <= 8; // cast to wrap negative vals, 1-9 allowed
    }

    private static int parseArrayLen(CharSequenceView rawType, int i, final int end) {
        final char lead = rawType.charAt(i);
        int temp = lead - '0';
        if (end - i == 1 && (char)temp <= 9 /* cast to wrap negative vals */) {
            return temp;
        }
        if (leadDigitValid(lead)) {
            i++;
            do {
                final int d = rawType.charAt(i++) - '0';
                if ((char)d > 9 /* cast to wrap negative vals */) {
                    break; // not a digit
                }
                temp = temp * 10 + d;
                if (i >= end) {
                    return temp;
                }
            } while (true);
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

    private static final int OOL_COMMA_INT = CharSequenceView.toAsciiInt("ool,");
    private static final int OOL_PAREN_INT = CharSequenceView.toAsciiInt("ool)");
    private static final int YTES_INT = CharSequenceView.toAsciiInt("ytes");
    private static final int UINT_INT = CharSequenceView.toAsciiInt("uint");
    private static final long _UINT256_LONG = CharSequenceView.toAsciiLong("\0uint256");
    private static final long _STRING_COMMA_LONG = CharSequenceView.toAsciiLong("\0string,");
    private static final long _STRING_PAREN_LONG = CharSequenceView.toAsciiLong("\0string)");
    private static final long ADDRESS_COMMA_LONG = CharSequenceView.toAsciiLong("address,");
    private static final long ADDRESS_PAREN_LONG = CharSequenceView.toAsciiLong("address)");

    private static TupleType<?> parseTupleType(final CharSequenceView rawType, final String[] elementNames, final int flags) { /* assumes that rawTypeStr.charAt(0) == '(' */
        final int len = rawType.length();
        if (len == 2 && rawType.charAt(0) == '(' && rawType.charAt(1) == ')') {
            return TupleType.empty(flags);
        }
        ABIType<?>[] elements = new ABIType[8];
        int argEnd = 1;
        final StringBuilder canonicalType = TupleType.newTypeBuilder();
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
                    final long val = rawType.getAsciiLong(argStart);
                    if (val == ADDRESS_COMMA_LONG || val == ADDRESS_PAREN_LONG) {
                        e = AddressType.INSTANCE;
                        argEnd = argStart + 7;
                    } else {
                        argEnd = nextTerminator(rawType, argStart + 1);
                    }
                    break;
                }
                case 'b': {
                    final long val = rawType.getAsciiInt(argStart + 1);
                    if (val == YTES_INT) {
                        final int nIdx = argStart + 5;
                        argEnd = nextTerminator(rawType, nIdx);
                        switch (argEnd - argStart) {
                        case 5: e = BYTES; break;
                        case 6: if (rawType.charAt(nIdx) == '4') e = BYTES_4; break;
                        case 7: if (rawType.charAt(nIdx) == '3' && rawType.charAt(argStart + 6) == '2') e = BYTES_32; break;
                        }
                    } else if (val == OOL_COMMA_INT || val == OOL_PAREN_INT) {
                        e = BOOL;
                        argEnd = argStart + 4;
                    } else {
                        argEnd = nextTerminator(rawType, argStart + 1);
                    }
                    break;
                }
                case 's': {
                    final long val = rawType.getAsciiLong(argStart - 1) & 0x00FFFFFF_FFFFFFFFL;
                    if (val == _STRING_COMMA_LONG || val == _STRING_PAREN_LONG) {
                        e = STRING;
                        argEnd = argStart + 6;
                    } else {
                        argEnd = nextTerminator(rawType, argStart + 1);
                    }
                    break;
                }
                case 'u': {
                    argEnd = nextTerminator(rawType, argStart + 1);
                    final int argLen = argEnd - argStart;
                    if (argLen == 7 && (rawType.getAsciiLong(argStart - 1) & 0x00FFFFFF_FFFFFFFFL) == _UINT256_LONG) {
                        e = UINT_256;
                    } else if (rawType.getAsciiInt(argStart) == UINT_INT) {
                        switch (argLen) {
                        case 4:
                            e = UINT_256; // "uint"
                            break;
                        case 5:
                            if (rawType.charAt(argStart + 4) == '8') {
                                e = UINT_8;
                            }
                            break;
                        case 6:
                            if (rawType.charAt(argStart + 4) == '3' && rawType.charAt(argStart + 5) == '2') {
                                e = UINT_32;
                            }
                            break;
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
