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

import com.esaulpaugh.headlong.rlp.Notation;
import com.esaulpaugh.headlong.util.SuperSerial;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.Encoding.UINT31;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/** @see ABIType */
public final class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    private static final String EMPTY_TUPLE_STRING = "()";

    public static final TupleType EMPTY = new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_ARRAY, null);

    final ABIType<?>[] elementTypes;
    private final int headLength;

    private TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes, String name) {
        super(canonicalType, Tuple.class, dynamic, name);
        this.elementTypes = elementTypes;
        this.headLength = dynamic ? OFFSET_LENGTH_BYTES : staticTupleHeadLength(this);
    }

    static TupleType wrap(String name, ABIType<?>... elements) {
        StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (ABIType<?> e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elements, name); // TODO .intern() string?
    }

    public int size() {
        return elementTypes.length;
    }

    public boolean isEmpty() {
        return size() == 0;
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
    int dynamicByteLength(Object value) {
        final Object[] elements = ((Tuple) value).elements;
        return countBytes(i -> measureObject(get(i), elements[i]));
    }

    @Override
    int byteLength(Object value) {
        if(!dynamic) return headLength;
        final Object[] elements = ((Tuple) value).elements;
        return countBytes(i -> measureObject(get(i), elements[i]));
    }

    private static int measureObject(ABIType<?> type, Object value) {
        return totalLen(type.byteLength(value), type.dynamic);
    }

    /**
     * @param value the Tuple being measured. {@code null} if not available
     * @return the length in bytes of the non-standard packed encoding
     */
    @Override
    public int byteLengthPacked(Object value) {
        final Object[] elements = value != null ? ((Tuple) value).elements : new Object[size()];
        return countBytes(i -> get(i).byteLengthPacked(elements[i]));
    }

    private int countBytes(IntUnaryOperator counter) {
        return countBytes(false, elementTypes.length, 0, counter);
    }

    static int countBytes(boolean array, int len, int count, IntUnaryOperator counter) {
        int i = 0;
        try {
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
        throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + value.size() + " != " + this.size());
    }

    private static int validateObject(ABIType<?> type, Object value) {
        try {
            return totalLen(type._validate(value), type.dynamic);
        } catch (NullPointerException npe) {
            throw new IllegalArgumentException("null", npe);
        }
    }

    static int totalLen(int byteLen, boolean addUnit) {
        return addUnit ? UNIT_LENGTH_BYTES + byteLen : byteLen;
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        Object[] vals = ((Tuple) value).elements;
        encodeObjects(dynamic, vals, TupleType.this::get, dest, dynamic ? headLength(vals) : -1);
    }

    private int headLength(Object[] elements) {
        int sum = 0;
        for (int i = 0; i < elements.length; i++) {
            sum += get(i).headLength();
        }
        return sum;
    }

    @Override
    void encodePackedUnchecked(Tuple value, ByteBuffer dest) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            get(i).encodeObjectPackedUnchecked(value.elements[i], dest);
        }
    }

    static void encodeObjects(boolean dynamic, Object[] values, IntFunction<ABIType<?>> getType, ByteBuffer dest, int offset) {
        for (int i = 0; i < values.length; i++) {
            offset = getType.apply(i).encodeHead(values[i], dest, offset);
        }
        if(dynamic) {
            for (int i = 0; i < values.length; i++) {
                ABIType<?> t = getType.apply(i);
                if (t.dynamic) {
                    t.encodeTail(values[i], dest);
                }
            }
        }
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {
        Object[] elements = new Object[size()];
        decodeObjects(dynamic, bb, unitBuffer, TupleType.this::get, elements);
        return new Tuple(elements);
    }

    @SuppressWarnings("unchecked")
    public <T> T decode(ByteBuffer bb, int... indices) {
        bb.mark();
        try {
            if(indices.length == 1) {
                return decodeIndex(bb, indices[0]); // decodes and returns specified element
            }
            if(indices.length == 0) {
                throw new IllegalArgumentException("must specify at least one index");
            }
            return (T) decodeIndices(bb, indices); // decodes and returns specified elements
        } finally {
            bb.reset();
        }
    }

    private void ensureIndexInBounds(int index) {
        if(index < 0) {
            throw new IllegalArgumentException("negative index: " + index);
        }
        if (index >= elementTypes.length) {
            throw new IllegalArgumentException("index " + index + " out of bounds for tuple type of length " + elementTypes.length);
        }
    }

    private <T> T decodeIndex(ByteBuffer bb, int index) {
        ensureIndexInBounds(index);
        int skipBytes = 0;
        for (int j = 0; j < index; j++) {
            skipBytes += elementTypes[j].headLength();
        }
        final int pos = bb.position();
        bb.position(pos + skipBytes);
        @SuppressWarnings("unchecked")
        final ABIType<T> resultType = (ABIType<T>) elementTypes[index];
        final byte[] unitBuffer = newUnitBuffer();
        if (resultType.dynamic) {
            bb.position(pos + UINT31.decode(bb, unitBuffer));
        }
        return resultType.decode(bb, unitBuffer);
    }

    private Tuple decodeIndices(ByteBuffer bb, int... indices) {
        final Object[] results = new Object[elementTypes.length];
        final int start = bb.position();
        final byte[] unitBuffer = newUnitBuffer();
        int n = 0, r = 0, skipBytes = 0, prevIndex = -1;
        do {
            final int index = indices[n++];
            ensureIndexInBounds(index);
            if(index <= prevIndex) {
                throw new IllegalArgumentException("index out of order: " + index);
            }
            for (; r < index; r++) {
                results[r] = Tuple.ABSENT;
                skipBytes += elementTypes[r].headLength();
            }
            bb.position(start + skipBytes);
            final ABIType<?> resultType = elementTypes[r++];
            if (resultType.dynamic) {
                bb.position(start + UINT31.decode(bb, unitBuffer));
            }
            results[index] = resultType.decode(bb, unitBuffer);
            if (n >= indices.length) {
                while (r < results.length) {
                    results[r++] = Tuple.ABSENT;
                }
                return new Tuple(results);
            }
            skipBytes += resultType.headLength();
            prevIndex = index;
        } while (true);
    }

    static int staticTupleHeadLength(TupleType tt) {
        int len = 0;
        for (ABIType<?> e : tt) {
            switch (e.typeCode()) {
            case TYPE_CODE_ARRAY: len += ArrayType.staticArrayHeadLength(e); continue;
            case TYPE_CODE_TUPLE: len += staticTupleHeadLength((TupleType) e); continue;
            default: len += UNIT_LENGTH_BYTES;
            }
        }
        return len;
    }

    static void decodeObjects(boolean dynamic, ByteBuffer bb, byte[] unitBuffer, IntFunction<ABIType<?>> getType, Object[] objects) {
        if(!dynamic) {
            for(int i = 0; i < objects.length; i++) {
                objects[i] = getType.apply(i).decode(bb, unitBuffer);
            }
            return;
        }
        final int start = bb.position(); // save this value before offsets are decoded
        final int[] offsets = new int[objects.length];
        for(int i = 0; i < objects.length; i++) {
            ABIType<?> t = getType.apply(i);
            if(!t.dynamic) {
                objects[i] = t.decode(bb, unitBuffer);
            } else {
                offsets[i] = UINT31.decode(bb, unitBuffer);
            }
        }
        for (int i = 0; i < objects.length; i++) {
            final int offset = offsets[i];
            if (offset > 0) {
                final int jump = start + offset;
                final int pos = bb.position();
                if (jump != pos) {
                    /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                    if (jump < pos && offsetNotShared(offset, offsets, i)) {
                        throw new IllegalArgumentException("illegal backwards jump: (" + start + "+" + offset + "=" + jump + ")<" + pos);
                    }
                    bb.position(jump); // leniently jump to specified offset
                }
                objects[i] = getType.apply(i).decode(bb, unitBuffer);
            }
        }
    }

    private static boolean offsetNotShared(int offset, int[] offsets, int i) {
        for (int j = 0; j < i; j++) {
            if (offsets[j] == offset) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses RLP Object {@link Notation} as a {@link Tuple}.
     *
     * @param s the tuple's RLP object notation
     * @return  the parsed tuple
     * @see Notation
     */
    @Override
    public Tuple parseArgument(String s) { // expects RLP object notation
        return SuperSerial.deserialize(this, s, false);
    }

    /**
     * Gives the ABI encoding of the input values according to this {@link TupleType}'s element types.
     *
     * @param elements  values corresponding to this {@link TupleType}'s element types
     * @return  the encoding
     */
    public ByteBuffer encodeElements(Object... elements) {
        return encode(new Tuple(elements));
    }

    @Override
    public Iterator<ABIType<?>> iterator() {
        return Arrays.asList(elementTypes).iterator();
    }

    /**
     * Returns a new {@link TupleType} containing only the elements in this {@link TupleType} whose position is
     * specified with a {@code true} value in the {@code manifest}. Aside from eliminating items not selected, order is
     * preserved.
     *
     * @param manifest  the booleans specifying which elements to select
     * @return  the new {@link TupleType}
     */
    public TupleType select(boolean... manifest) {
        return selectElements(manifest, false);
    }

    /**
     * Returns the complement of {@link TupleType#select(boolean...)} -- a new {@link TupleType} containing only the
     * elements which are *not* specified with {@code true} values. Aside from eliminating excluded items, order is
     * preserved.
     *
     * @param manifest  the booleans specifying which elements to exclude
     * @return  the new {@link TupleType}
     */
    public TupleType exclude(boolean... manifest) {
        return selectElements(manifest, true);
    }

    private TupleType selectElements(final boolean[] manifest, final boolean negate) {
        final int size = size();
        if(manifest.length == size) {
            final StringBuilder canonicalBuilder = new StringBuilder("(");
            boolean dynamic = false;
            final List<ABIType<?>> selected = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (negate ^ manifest[i]) {
                    ABIType<?> e = get(i);
                    canonicalBuilder.append(e.canonicalType).append(',');
                    dynamic |= e.dynamic;
                    selected.add(e);
                }
            }
            return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, selected.toArray(EMPTY_ARRAY), null);
        }
        throw new IllegalArgumentException("manifest.length != size(): " + manifest.length + " != " + size);
    }

    private static String completeTupleTypeString(StringBuilder sb) {
        final int len = sb.length();
        return len != 1
                ? sb.deleteCharAt(len - 1).append(')').toString() // replace trailing comma
                : EMPTY_TUPLE_STRING;
    }

    public static TupleType parse(String rawTupleTypeString) {
        return TypeFactory.create(rawTupleTypeString);
    }

    public static TupleType of(String... typeStrings) {
        StringBuilder sb = new StringBuilder("(");
        for (String str : typeStrings) {
            sb.append(str).append(',');
        }
        return parse(completeTupleTypeString(sb));
    }
}
