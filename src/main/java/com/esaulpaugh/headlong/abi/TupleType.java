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

import com.esaulpaugh.headlong.util.FastHex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/** {@link ABIType} for a struct, a tuple, a set of function parameters, a function return type, or to represent the types in an event or custom error, or a subset thereof. */
public final class TupleType<J extends Tuple> extends ABIType<J> implements Iterable<ABIType<?>> {

    private static final boolean[] EMPTY_INDEX = new boolean[0];

    public static final TupleType<Tuple> EMPTY = new TupleType<>("()", false, EMPTY_ARRAY, null, null, EMPTY_INDEX, ABIType.FLAGS_NONE);

    final ABIType<?>[] elementTypes;
    final String[] elementNames;
    final String[] elementInternalTypes;
    final boolean[] indexed;
    private final int[] elementHeadOffsets;
    final int headLengthSum;
    private final int flags;

    TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes, String[] elementNames, String[] elementInternalTypes, boolean[] indexed, int flags) {
        super(canonicalType, Tuple.classFor(elementTypes.length), dynamic);
        if (elementNames != null && elementNames.length != elementTypes.length) {
            throw new IllegalArgumentException("expected " + elementTypes.length + " element names but found " + elementNames.length);
        }
        this.elementTypes = elementTypes;
        this.elementNames = elementNames;
        this.elementInternalTypes = elementInternalTypes;
        final int[] elementHeadOffsets = new int[elementTypes.length];
        int headLengthSum = 0;
        for (int i = 0; i < elementTypes.length; headLengthSum += elementTypes[i++].headLength()) {
            elementHeadOffsets[i] = headLengthSum;
        }
        this.elementHeadOffsets = elementHeadOffsets;
        this.headLengthSum = headLengthSum;
        this.indexed = indexed;
        this.flags = flags;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    public int size() {
        return elementTypes.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public String getElementName(int index) {
        return elementNames == null ? null : elementNames[index];
    }

    public String getElementInternalType(int index) {
        return elementInternalTypes == null ? null : elementInternalTypes[index];
    }

    /**
     * Returns the {@link ABIType} at the given index. If the compiler can't infer the return type, use a type witness.
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
     * @param index the type's index
     * @return  the type
     * @param <T>   the expected return type, e.g. {@link BooleanType} or {@link ABIType}&#60;Boolean&#62;
     */
    @SuppressWarnings("unchecked")
    public <T extends ABIType<?>> T get(int index) {
        return (T) elementTypes[index];
    }

    @Override
    Class<?> arrayClass() {
        return Tuple[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_TUPLE;
    }

    @Override
    int headLength() {
        return dynamic ? OFFSET_LENGTH_BYTES : headLengthSum;
    }

    @Override
    int dynamicByteLength(Tuple value) {
        return countBytes(i -> measureObject(get(i), value.elements[i]));
    }

    @Override
    int byteLength(Tuple value) {
        if (!dynamic) return headLengthSum;
        return dynamicByteLength(value);
    }

    private static int measureObject(ABIType<Object> type, Object value) {
        return totalLen(type.byteLength(value), type.dynamic);
    }

    /**
     * @param value the Tuple being measured. {@code null} if not available
     * @return the length in bytes of the non-standard packed encoding
     */
    @Override
    public int byteLengthPacked(Tuple value) {
        final Object[] elements = value != null ? value.elements : new Object[size()];
        return countBytes(i -> this.<ABIType<? super Object>>get(i).byteLengthPacked(elements[i]));
    }

    private int countBytes(IntUnaryOperator counter) {
        return countBytes(true, size(), counter);
    }

    static int countBytes(boolean tuple, int len, IntUnaryOperator counter) {
        int i = 0;
        try {
            int count = 0;
            for ( ; i < len; i++) {
                count += counter.applyAsInt(i);
            }
            return count;
        } catch (IllegalArgumentException cause) {
            throw exceptionWithIndex(tuple, i, cause);
        }
    }

    @Override
    public int validate(final J value) {
        if (value.size() == this.size()) {
            return countBytes(i -> validateObject(get(i), value.elements[i]));
        }
        throw lengthMismatch(value);
    }

    private IllegalArgumentException lengthMismatch(Tuple args) {
        throw new IllegalArgumentException("tuple length mismatch: expected length " + this.size() + " but found " + args.size());
    }

    private static <X> int validateObject(ABIType<X> type, X value) {
        try {
            return totalLen(type.validate(value), type.dynamic);
        } catch (ClassCastException cce) {
            type.validateClass(value); // generates better error message
            throw new AssertionError();
        } catch (NullPointerException npe) {
            throw new IllegalArgumentException("null", npe);
        }
    }

    static int totalLen(int byteLen, boolean addUnit) {
        return addUnit ? UNIT_LENGTH_BYTES + byteLen : byteLen;
    }

    @Override
    void encodeTail(Tuple value, ByteBuffer dest) {
        if (dynamic) {
            encodeDynamic(value.elements, dest);
        } else {
            for (int i = 0; i < value.elements.length; i++) {
                this.<ABIType<? super Object>>get(i).encodeTail(value.elements[i], dest);
            }
        }
    }

    @Override
    void encodePackedUnchecked(Tuple value, ByteBuffer dest) {
        for (int i = 0; i < value.elements.length; i++) {
            this.<ABIType<? super Object>>get(i).encodePackedUnchecked(value.elements[i], dest);
        }
    }

    private void encodeDynamic(Object[] values, ByteBuffer dest) {
        int i = 0;
        final int last = values.length - 1; // dynamic tuples are guaranteed not to be empty
        int offset = headLengthSum;
        for (;; i++) {
            final ABIType<Object> t = get(i);
            if (!t.dynamic) {
                t.encodeTail(values[i], dest);
                if (i == last) {
                    break;
                }
            } else {
                insertIntUnsigned(offset, dest); // insert offset
                if (i == last) {
                    break;
                }
                offset += t.dynamicByteLength(values[i]); // calculate next offset
            }
        }
        i = 0;
        do {
            final ABIType<Object> t = get(i);
            if (t.dynamic) {
                t.encodeTail(values[i], dest);
            }
        } while (++i < values.length);
    }

    @Override
    J decode(ByteBuffer bb, byte[] unitBuffer) {
        final Object[] elements = new Object[size()];
        int i = 0;
        try {
            if (!dynamic) {
                for ( ; i < elements.length; i++) {
                    elements[i] = get(i).decode(bb, unitBuffer);
                }
            } else {
                final int start = bb.position(); // save this value before offsets are decoded
                final int[] offsets = new int[elements.length];
                do {
                    ABIType<?> t = get(i);
                    if (!t.dynamic) {
                        elements[i] = t.decode(bb, unitBuffer);
                    } else {
                        final int offset = IntType.UINT31.decode(bb, unitBuffer);
                        offsets[i] = offset == 0 ? -1 : offset;
                    }
                } while (++i < elements.length);
                i = 0;
                do {
                    final int offset = offsets[i];
                    if (offset != 0) {
                        final int jump = start + offset;
                        if (jump != bb.position()) { // && (this.flags & ABIType.FLAG_LEGACY_ARRAY) == 0
                            /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                            bb.position(offset == -1 ? start : jump); // leniently jump to specified offset
                        }
                        elements[i] = get(i).decode(bb, unitBuffer);
                    }
                } while (++i < elements.length);
            }
        } catch (IllegalArgumentException cause) {
            throw exceptionWithIndex(true, i, cause);
        }
        return Tuple.create(elements);
    }

    /**
     * Decodes only the elements at the specified indices.
     *
     * @param bb    the buffer containing the encoding
     * @param indices   the positions of the elements to decode
     * @return  the decoded data
     * @param <T>   the type of the decoded element or {@link Tuple} if decoding multiple elements
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(ByteBuffer bb, int... indices) {
        bb.mark();
        try {
            if (indices.length == 1) {
                return (T) decodeIndex(bb, bb.position(), newUnitBuffer(), indices[0]); // specified element
            }
            return (T) decodeIndices(bb, indices); // Tuple with specified elements populated
        } finally {
            bb.reset();
        }
    }

    private Object decodeIndex(ByteBuffer bb, int start, byte[] unitBuffer, int i) {
        try {
            final ABIType<?> t = get(i);
            bb.position(start + elementHeadOffsets[i]);
            if (t.dynamic) {
                bb.position(start + IntType.UINT31.decode(bb, unitBuffer));
            }
            return t.decode(bb, unitBuffer);
        } catch (IllegalArgumentException cause) {
            throw exceptionWithIndex(true, i, cause);
        }
    }

    private J decodeIndices(ByteBuffer bb, int... indices) {
        final Object[] results = new Object[size()];
        final int start = bb.position();
        final byte[] unitBuffer = newUnitBuffer();
        int prev = -1;
        for (final int index : indices) {
            results[index] = decodeIndex(bb, start, unitBuffer, index);
            if (index <= prev) {
                throw new IllegalArgumentException("index out of order: " + index);
            }
            prev = index;
        }
        return Tuple.create(results);
    }

    static IllegalArgumentException exceptionWithIndex(boolean tuple, int i, IllegalArgumentException cause) {
        return new IllegalArgumentException((tuple ? "tuple index " : "array index ") + i + ": " + cause.getMessage(), cause);
    }

    @Override
    public Iterator<ABIType<?>> iterator() {
        return Arrays.asList(elementTypes).iterator();
    }

    /**
     * Returns a new {@link TupleType} from select elements in this {@link TupleType}. Only elements marked with {@code true} in {@code manifest} are included.
     * Order is preserved among the selected elements.
     *
     * @param manifest  booleans specifying whether to include the respective elements
     * @return  the new {@link TupleType}
     */
    public TupleType<J> select(boolean... manifest) {
        return selectElements(manifest, false);
    }

    /**
     * Returns the complement of {@link TupleType#select(boolean...)} -- a new {@link TupleType} containing only the elements which are
     * *not* marked for exclusion with a {@code true} value in the manifest. The order of the remaining elements is preserved.
     *
     * @param manifest  booleans specifying whether to exclude the respective elements
     * @return  the new {@link TupleType}
     */
    public TupleType<J> exclude(boolean... manifest) {
        return selectElements(manifest, true);
    }

    private TupleType<J> selectElements(final boolean[] manifest, final boolean negate) {
        if (manifest.length != size()) {
            throw new IllegalArgumentException("expected manifest length " + size() + " but found length " + manifest.length);
        }
        final StringBuilder canonicalType = new StringBuilder("(");
        boolean dynamic = false;
        final ABIType<?>[] selected = new ABIType<?>[size()];
        final String[] selectedNames = elementNames == null ? null : new String[size()];
        final String[] selectedInternalTypes = elementInternalTypes == null ? null : new String[size()];
        final boolean[] selectedIsIndexed = indexed == null ? null : new boolean[size()];
        int c = 0;
        for (int i = 0; i < size(); i++) {
            if (negate ^ manifest[i]) {
                if (selectedNames != null) {
                    selectedNames[c] = elementNames[i];
                }
                if (selectedInternalTypes != null) {
                    selectedInternalTypes[c] = elementInternalTypes[i];
                }
                if (selectedIsIndexed != null) {
                    selectedIsIndexed[c] = indexed[i];
                }
                ABIType<?> e = get(i);
                canonicalType.append(e.canonicalType).append(',');
                dynamic |= e.dynamic;
                selected[c] = e;
                c++;
            }
        }
        return new TupleType<>(
                completeTupleTypeString(canonicalType),
                dynamic,
                Arrays.copyOf(selected, c),
                selectedNames == null ? null : Arrays.copyOf(selectedNames, c),
                selectedInternalTypes == null ? null : Arrays.copyOf(selectedInternalTypes, c),
                selectedIsIndexed == null ? null : Arrays.copyOf(selectedIsIndexed, c),
                this.flags
        );
    }

    private static String completeTupleTypeString(StringBuilder sb) {
        if (sb.length() == 1) {
            return "()";
        }
        sb.setCharAt(sb.length() - 1, ')'); // overwrite trailing comma
        return sb.toString();
    }

    public static <X extends Tuple> TupleType<X> parse(String rawTupleTypeString) {
        return TypeFactory.create(rawTupleTypeString);
    }

    public static <X extends Tuple> TupleType<X> parse(int flags, String rawTupleTypeString) {
        return TypeFactory.create(flags, rawTupleTypeString);
    }

    public static <X extends Tuple> TupleType<X> of(String... typeStrings) {
        StringBuilder rawType = new StringBuilder("(");
        for (String t : typeStrings) {
            rawType.append(t).append(',');
        }
        return parse(completeTupleTypeString(rawType));
    }

    /**
     * Experimental. Annotates the given ABI encoding and returns an informational formatted String. This
     * method is subject to change or removal in a future release.
     */
    public String annotate(byte[] abi) {
        return annotate(decode(ByteBuffer.wrap(abi), newUnitBuffer()));
    }

    /**
     * Experimental. Annotates the ABI encoding of the given {@link Tuple} and returns an informational formatted String.
     * This method is subject to change or removal in a future release.
     */
    public String annotate(Tuple tuple) {
        return annotate(tuple, new StringBuilder(512));
    }

    String annotate(Tuple tuple, StringBuilder sb) {
        if (tuple.elements.length != size()) {
            throw lengthMismatch(tuple);
        }
        if (size() > 0) {
            int row = 0;
            int i = 0;
            final int last = size() - 1;
            int offset = headLengthSum;
            final ByteBuffer rowBuf = ByteBuffer.allocate(UNIT_LENGTH_BYTES);
            do {
                final ABIType<Object> t = get(i);
                if (!t.dynamic) {
                    row = encodeTailAnnotated(sb, t, row, i, tuple.elements[i]);
                    if (i == last) {
                        break;
                    }
                } else {
                    // encode offset
                    insertIntUnsigned(offset, rowBuf); // insert offset
                    rowBuf.flip();
                    appendAnnotatedRow(sb, rowBuf, row++, i, " offset");
                    rowBuf.flip();
                    if (i == last) {
                        break;
                    }
                    offset += t.dynamicByteLength(tuple.elements[i]); // calculate next offset
                }
                i++;
            } while (true);
            i = 0;
            do {
                final ABIType<Object> t = get(i);
                if (t.dynamic) {
                    row = encodeTailAnnotated(sb, t, row, i, tuple.elements[i]);
                }
            } while (++i < size());
        }
        return sb.toString();
    }

    private int encodeTailAnnotated(StringBuilder sb, ABIType<Object> t, int row, int i, Object v) {
        final ByteBuffer encoding = ByteBuffer.allocate(t.validate(v));
        t.encodeTail(v, encoding);
        final int len = encoding.position();
        encoding.flip();
        int n = 0;
        if (n < len) {
            final boolean dynamicArray = t instanceof ArrayType && ArrayType.DYNAMIC_LENGTH == t.asArrayType().getLength();
            appendAnnotatedRow(sb, encoding, row++, i, dynamicArray ? " length" : "");
            n += UNIT_LENGTH_BYTES;
            if (n < len) {
                appendAnnotatedRow(sb, encoding, row++, i, dynamicArray ? "" : null);
                n += UNIT_LENGTH_BYTES;
                while (n < len) {
                    appendAnnotatedRow(sb, encoding, row++, i, null);
                    n += UNIT_LENGTH_BYTES;
                }
            }
        }
        return row;
    }

    private void appendAnnotatedRow(StringBuilder sb, ByteBuffer encoding, int row, int i, String note) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(hexLabel(row));
        byte[] rowBuffer = newUnitBuffer();
        encoding.get(rowBuffer);
        sb.append(FastHex.encodeToString(rowBuffer));
        sb.append("\t[").append(i).append(']');
        if (note == null) {
            sb.append(" ...");
            return;
        }
        ABIType<?> t = get(i);
        sb.append(' ').append(t.canonicalType);
        if (" offset".equals(note) || !t.dynamic) {
            String name = getElementName(i);
            if (name != null) {
                sb.append(" \"").append(name).append('"');
            }
            sb.append(note);
            String internalType = getElementInternalType(i);
            if (internalType != null && !internalType.equals(t.canonicalType)) {
                sb.append(" \tinternal=").append(internalType);
            }
        } else {
            sb.append(note);
        }
    }

    static TupleType<Tuple> empty(int flags) {
        return flags == ABIType.FLAGS_NONE ? EMPTY : new TupleType<>("()", false, EMPTY_ARRAY, null, null, EMPTY_INDEX, flags);
    }
}
