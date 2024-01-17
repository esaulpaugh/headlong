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

import com.esaulpaugh.headlong.util.Strings;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/** @see ABIType */
public final class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    static final String EMPTY_TUPLE_STRING = "()";
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final TupleType EMPTY = new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_ARRAY, null, null, ABIType.FLAGS_NONE);

    final ABIType<?>[] elementTypes;
    final String[] elementNames;
    final String[] elementInternalTypes;
    private final int[] elementHeadOffsets;
    private final int headLength;
    private final int firstOffset;
    final int flags;

    TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes, String[] elementNames, String[] elementInternalTypes, int flags) {
        super(canonicalType, Tuple.class, dynamic);
        this.elementTypes = elementTypes;
        this.elementNames = elementNames;
        this.elementInternalTypes = elementInternalTypes;
        this.elementHeadOffsets = new int[elementTypes.length];
        if(dynamic) {
            this.headLength = OFFSET_LENGTH_BYTES;
            int sum = 0;
            for (int i = 0; i < elementTypes.length; i++) {
                this.elementHeadOffsets[i] = sum;
                sum += elementTypes[i].headLength();
            }
            this.firstOffset = sum;
        } else {
            this.headLength = staticTupleHeadLength();
            this.firstOffset = -1;
        }
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

    @SuppressWarnings("unchecked")
    public <T extends ABIType<?>> T get(int index) {
        return (T) elementTypes[index];
    }

    public ABIType<Object> getNonCapturing(int index) {
        return get(index);
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
        return headLength;
    }

    @Override
    int dynamicByteLength(Tuple value) {
        return countBytes(i -> measureObject(get(i), value.elements[i]));
    }

    @Override
    int byteLength(Tuple value) {
        if(!dynamic) return headLength;
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
        return countBytes(i -> getNonCapturing(i).byteLengthPacked(elements[i]));
    }

    private int countBytes(IntUnaryOperator counter) {
        return countBytes(false, elementTypes.length, counter);
    }

    static int countBytes(boolean array, int len, IntUnaryOperator counter) {
        int i = 0;
        try {
            int count = 0;
            for ( ; i < len; i++) {
                count += counter.applyAsInt(i);
            }
            return count;
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException((array ? "array" : "tuple") + " index " + i + ": " + iae.getMessage(), iae);
        }
    }

    @Override
    public int validate(final Tuple value) {
        if (value.size() == this.size()) {
            return countBytes(i -> validateObject(get(i), value.elements[i]));
        }
        throw new IllegalArgumentException("tuple length mismatch: expected length " + this.size() + " but found " + value.size());
    }

    private static int validateObject(ABIType<Object> type, Object value) {
        try {
            type.validateClass(value);
            return totalLen(type.validate(value), type.dynamic);
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
            encodeStatic(value.elements, dest);
        }
    }

    @Override
    void encodePackedUnchecked(Tuple value, ByteBuffer dest) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            getNonCapturing(i).encodePackedUnchecked(value.elements[i], dest);
        }
    }

    private void encodeStatic(Object[] values, ByteBuffer dest) {
        for (int i = 0; i < values.length; i++) {
            getNonCapturing(i).encodeTail(values[i], dest);
        }
    }

    private void encodeDynamic(Object[] values, ByteBuffer dest) {
        if (values.length == 0) {
            return;
        }
        int i = 0;
        final int last = values.length - 1;
        int offset = firstOffset;
        for (;; i++) {
            final ABIType<Object> t = get(i);
            if (!t.dynamic) {
                t.encodeTail(values[i], dest);
                if (i >= last) {
                    break;
                }
            } else {
                insertIntUnsigned(offset, dest); // insert offset
                if (i >= last) {
                    break;
                }
                offset += t.dynamicByteLength(values[i]); // calculate next offset
            }
        }
        for (i = 0; i < values.length; i++) {
            final ABIType<Object> t = get(i);
            if (t.dynamic) {
                t.encodeTail(values[i], dest);
            }
        }
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {
        final Object[] elements = new Object[size()];
        int i = 0;
        try {
            final int start = bb.position(); // save this value before offsets are decoded
            final int[] offsets = new int[elements.length];
            for ( ; i < elements.length; i++) {
                ABIType<?> t = elementTypes[i];
                if (!t.dynamic) {
                    elements[i] = t.decode(bb, unitBuffer);
                } else {
                    final int offset = IntType.UINT31.decode(bb, unitBuffer);
                    offsets[i] = offset == 0 ? -1 : offset;
                }
            }
            for (i = 0; i < elements.length; i++) {
                final int offset = offsets[i];
                if (offset != 0) {
                    final int jump = start + offset;
                    if (jump != bb.position()) { // && (this.flags & ABIType.FLAG_LEGACY_ARRAY) == 0
                        /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                        bb.position(offset == -1 ? start : jump); // leniently jump to specified offset
                    }
                    elements[i] = elementTypes[i].decode(bb, unitBuffer);
                }
            }
        } catch (IllegalArgumentException cause) {
            throw decodeException(true, i, cause);
        }
        return new Tuple(elements);
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
            if(indices.length == 1) {
                return (T) decodeIndex(bb, indices[0]); // specified element
            }
            return (T) decodeIndices(bb, indices); // Tuple with specified elements populated
        } finally {
            bb.reset();
        }
    }

    private Object decodeIndex(ByteBuffer bb, int index) {
        final ABIType<?> type = elementTypes[index]; // implicit bounds check up front
        final int start = bb.position();
        bb.position(start + elementHeadOffsets[index]);
        return decodeObject(type, bb, start, newUnitBuffer(), index);
    }

    private Tuple decodeIndices(ByteBuffer bb, int... indices) {
        final Object[] results = new Object[elementTypes.length];
        final int start = bb.position();
        final byte[] unitBuffer = newUnitBuffer();
        int prev = -1;
        for (final int index : indices) {
            final ABIType<?> resultType = elementTypes[index]; // implicit bounds check up front
            if (index <= prev) {
                throw new IllegalArgumentException("index out of order: " + index);
            }
            bb.position(start + elementHeadOffsets[index]);
            results[index] = decodeObject(resultType, bb, start, unitBuffer, index);
            prev = index;
        }
        return new Tuple(results);
    }

    private static Object decodeObject(ABIType<?> type, ByteBuffer bb, int start, byte[] unitBuffer, int index) {
        try {
            if (type.dynamic) {
                bb.position(start + IntType.UINT31.decode(bb, unitBuffer));
            }
            return type.decode(bb, unitBuffer);
        } catch (IllegalArgumentException cause) {
            throw decodeException(true, index, cause);
        }
    }

    int staticTupleHeadLength() {
        int len = 0;
        int sum = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            elementHeadOffsets[i] = sum;
            final ABIType<?> e = elementTypes[i];
            sum += e.headLength();
            switch (e.typeCode()) {
            case TYPE_CODE_ARRAY: len += ((ArrayType<?, ?>) e).staticArrayHeadLength(); continue;
            case TYPE_CODE_TUPLE: len += ((TupleType) e).staticTupleHeadLength(); continue;
            default: len += UNIT_LENGTH_BYTES;
            }
        }
        return len;
    }

    static IllegalArgumentException decodeException(boolean tuple, int i, IllegalArgumentException cause) {
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
    public TupleType select(boolean... manifest) {
        return selectElements(manifest, false);
    }

    /**
     * Returns the complement of {@link TupleType#select(boolean...)} -- a new {@link TupleType} containing only the elements which are
     * *not* marked for exclusion with a {@code true} value in the manifest. The order of the remaining elements is preserved.
     *
     * @param manifest  booleans specifying whether to exclude the respective elements
     * @return  the new {@link TupleType}
     */
    public TupleType exclude(boolean... manifest) {
        return selectElements(manifest, true);
    }

    private TupleType selectElements(final boolean[] manifest, final boolean negate) {
        final int size = size();
        if (manifest.length != size) {
            throw new IllegalArgumentException("expected manifest length " + size + " but found length " + manifest.length);
        }
        final StringBuilder canonicalType = new StringBuilder("(");
        boolean dynamic = false;
        final List<ABIType<?>> selected = new ArrayList<>(size);
        final List<String> selectedNames = elementNames == null ? null : new ArrayList<>(size);
        final List<String> selectedInternalTypes = elementInternalTypes == null ? null : new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (negate ^ manifest[i]) {
                ABIType<?> e = get(i);
                canonicalType.append(e.canonicalType).append(',');
                dynamic |= e.dynamic;
                selected.add(e);
                if (selectedNames != null) {
                    selectedNames.add(elementNames[i]);
                }
                if (selectedInternalTypes != null) {
                    selectedInternalTypes.add(elementInternalTypes[i]);
                }
            }
        }
        return new TupleType(
                completeTupleTypeString(canonicalType),
                dynamic,
                selected.toArray(EMPTY_ARRAY),
                selectedNames == null ? null : selectedNames.toArray(EMPTY_STRING_ARRAY),
                selectedInternalTypes == null ? null : selectedInternalTypes.toArray(EMPTY_STRING_ARRAY),
                this.flags
        );
    }

    private static String completeTupleTypeString(StringBuilder sb) {
        final int len = sb.length();
        if (len != 1) {
            sb.setCharAt(len - 1, ')'); // overwrite trailing comma
            return sb.toString();
        }
        return EMPTY_TUPLE_STRING;
    }

    public static TupleType parse(String rawTupleTypeString) {
        return TypeFactory.create(rawTupleTypeString);
    }

    public static TupleType parse(int flags, String rawTupleTypeString) {
        return TypeFactory.create(flags, rawTupleTypeString);
    }

    public static TupleType of(String... typeStrings) {
        StringBuilder sb = new StringBuilder("(");
        for (String str : typeStrings) {
            sb.append(str).append(',');
        }
        return parse(completeTupleTypeString(sb));
    }

    static TupleType empty(int flags) {
        return flags == ABIType.FLAGS_NONE ? EMPTY : new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_ARRAY, null, null, flags);
    }

    public String annotate(byte[] abi) {
        return annotate(decode(ByteBuffer.wrap(abi), newUnitBuffer()));
    }

    public String annotate(Tuple tuple) {
        if (tuple.elements.length == 0) {
            return "";
        }
        int row = 0;
        final StringBuilder sb = new StringBuilder();
        int i = 0;
        final int last = tuple.elements.length - 1;
        int offset = firstOffset;
        for (;; i++) {
            final ABIType<Object> t = get(i);
            if (!t.dynamic) {
                row = encodeTailAnnotated(row, i, t, tuple.elements[i], sb);
                if (i >= last) {
                    break;
                }
            } else {
                final ByteBuffer dest = ByteBuffer.allocate(OFFSET_LENGTH_BYTES);
                insertIntUnsigned(offset, dest); // insert offset
                sb.append(label(row++)).append(Strings.encode(dest));
                annotate(sb, i, t, " offset");
                if (i >= last) {
                    break;
                }
                offset += t.dynamicByteLength(tuple.elements[i]); // calculate next offset
            }
        }
        for (i = 0; i < tuple.elements.length; i++) {
            final ABIType<Object> t = get(i);
            if (t.dynamic) {
                row = encodeTailAnnotated(row, i, t, tuple.elements[i], sb);
            }
        }
        return sb.toString();
    }

    private static int encodeTailAnnotated(int row, int idx, ABIType<Object> t, Object v, StringBuilder sb) {
        final ByteBuffer dest = ByteBuffer.allocate(t.validate(v));
        t.encodeTail(v, dest);
        final int len = dest.position();
        dest.flip();
        int i = 0;
        final boolean dynamicArray = t.dynamic && t instanceof ArrayType && ((ArrayType<?, ?>) t).getLength() == ArrayType.DYNAMIC_LENGTH;
//        final TupleType tt = t.typeCode() == ABIType.TYPE_CODE_TUPLE ? (TupleType)(ABIType)t : null;
        final byte[] rowData = newUnitBuffer();
        if (i < len) {
            appendAnnotatedRow(row++, sb, dest, rowData, idx, t, dynamicArray ? " length" : ""); // tt != null ? " " + tt.get(0).canonicalType : ""
            i += UNIT_LENGTH_BYTES;
            if (i < len) {
                appendAnnotatedRow(row++, sb, dest, rowData, idx, t, dynamicArray ? "" : null);
                i += UNIT_LENGTH_BYTES;
                while (i < len) {
                    appendAnnotatedRow(row++, sb, dest, rowData, idx, t, null);
                    i += UNIT_LENGTH_BYTES;
                }
            }
        }
        return row;
    }

    private static void appendAnnotatedRow(int row, StringBuilder sb, ByteBuffer bb, byte[] rowData, int idx, ABIType<Object> t, String note) {
        bb.get(rowData);
        sb.append(label(row)).append(Strings.encode(rowData));
        annotate(sb, idx, t, note);
    }

    private static String label(int row) {
        String unpadded = Integer.toHexString(row * UNIT_LENGTH_BYTES);
        return ABIType.pad(ABIType.LABEL_LEN - unpadded.length(), unpadded);
    }

    private static void annotate(StringBuilder sb, int idx, ABIType<?> t, String note) {
        sb.append("\t[").append(idx).append("]");
        if (note == null) {
            sb.append(" ...").append('\n');
            return;
        }
        sb.append(' ').append(t.canonicalType).append(note).append('\n');
    }
}
