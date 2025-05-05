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
        return buildUnchecked(new Slice(rawType), elementNames, baseType, flags);
    }

    private static ABIType<?> buildUnchecked(final Slice rawType, final String[] elementNames, final TupleType<?> baseType, final int flags) {
        try {
            final int lastCharIdx = rawType.length() - 1;
            if (rawType.charAt(lastCharIdx) == ']') { // array
                final int secondToLastCharIdx = lastCharIdx - 1;
                final int arrayOpenIndex = rawType.lastIndexOf('[', secondToLastCharIdx);

                final ABIType<?> elementType = buildUnchecked(rawType.slice(0, arrayOpenIndex), null, baseType, flags);
                final StringBuilder sb = new StringBuilder(elementType.canonicalType);
                rawType.append(sb, arrayOpenIndex);
                final String type = sb.toString();
                final int length = arrayOpenIndex == secondToLastCharIdx ? DYNAMIC_LENGTH : parseArrayLen(rawType, arrayOpenIndex + 1, lastCharIdx);
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
                return t != null ? t : tryParseFixed(rawType.toString());
            }
        } catch (IndexOutOfBoundsException ignored) { // e.g. type equals "" or "82]" or "[]" or "[1]"
        }
        throw unrecognizedType(rawType);
    }

    private static IllegalArgumentException unrecognizedType(Slice rawType) {
        return unrecognizedType(rawType.toString());
    }

    private static IllegalArgumentException unrecognizedType(String rawType) {
        return new IllegalArgumentException("unrecognized type: \"" + rawType + '"');
    }

    private static boolean leadDigitValid(char c) {
        return (char)(c - '1') <= 8; // cast to wrap negative vals, 1-9 allowed
    }

    private static int parseArrayLen(Slice rawType, int start, int end) {
        final char lead = rawType.charAt(start);
        int temp = lead - '0';
        if (end - start == 1 && (char)temp <= 9 /* cast to wrap negative vals */) {
            return temp;
        }
        if (leadDigitValid(lead)) {
            int i = start + 1;
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

    private static TupleType<?> parseTupleType(final Slice rawType, final String[] elementNames, final int flags) { /* assumes that rawTypeStr.charAt(0) == '(' */
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
                case 'a':
                    argEnd = nextTerminator(rawType, argStart + 1);
                    if (argEnd - argStart == 7
                            && rawType.charAt(argStart + 1) == 'd'
                            && rawType.charAt(argStart + 2) == 'd'
                            && rawType.charAt(argStart + 3) == 'r'
                            && rawType.charAt(argStart + 4) == 'e'
                            && rawType.charAt(argStart + 5) == 's'
                            && rawType.charAt(argStart + 6) == 's') {
                        e = AddressType.INSTANCE;
                    }
                    break;
                case 'b': {
                    argEnd = nextTerminator(rawType, argStart + 1);
                    final int argLen = argEnd - argStart;
                    if (rawType.charAt(argStart + 1) == 'y'
                            && rawType.charAt(argStart + 2) == 't'
                            && rawType.charAt(argStart + 3) == 'e'
                            && rawType.charAt(argStart + 4) == 's') {
                        switch (argLen) {
                        case 5: e = BYTES; break;
                        case 6: if (rawType.charAt(argStart + 5) == '4') e = BYTES_4; break;
                        case 7: if (rawType.charAt(argStart + 5) == '3' && rawType.charAt(argStart + 6) == '2') e = BYTES_32; break;
                        }
                    } else if (argLen == 4
                            && rawType.charAt(argStart + 1) == 'o'
                            && rawType.charAt(argStart + 2) == 'o'
                            && rawType.charAt(argStart + 3) == 'l') {
                        e = BOOL;
                    }
                    break;
                }
                case 's':
                    argEnd = nextTerminator(rawType, argStart + 1);
                    if (argEnd - argStart == 6
                            && rawType.charAt(argStart + 1) == 't'
                            && rawType.charAt(argStart + 2) == 'r'
                            && rawType.charAt(argStart + 3) == 'i'
                            && rawType.charAt(argStart + 4) == 'n'
                            && rawType.charAt(argStart + 5) == 'g') {
                        e = STRING;
                    }
                    break;
                case 'u':
                    argEnd = nextTerminator(rawType, argStart + 1);
                    if (rawType.charAt(argStart + 1) == 'i' && rawType.charAt(argStart + 2) == 'n' && rawType.charAt(argStart + 3) == 't') {
                        final int argLen = argEnd - argStart;
                        switch (argLen) {
                        case 4:
                            e = UINT_256;
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
                        case 7:
                            if (rawType.charAt(argStart + 4) == '2'
                                    && rawType.charAt(argStart + 5) == '5'
                                    && rawType.charAt(argStart + 6) == '6') {
                                e = UINT_256;
                            }
                            break;
                        }
                    }
                    break;
                default: argEnd = nextTerminator(rawType, argStart + 1);
                }
                if (e == null) {
                    e = buildUnchecked(rawType.slice(argStart, argEnd), null, null, flags);
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

    private static int nextTerminator(Slice signature, int i) {
        for ( ; ; i++) {
            switch (signature.charAt(i)) {
            case ',':
            case ')': return i;
            }
        }
    }

    private static int findSubtupleEnd(Slice parentTypeString, int i) {
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
