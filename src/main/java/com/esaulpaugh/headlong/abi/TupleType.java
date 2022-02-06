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

    public static final TupleType EMPTY = new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_ARRAY);

    final ABIType<?>[] elementTypes;
//    private final int staticByteLen;

    private TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes) {
        super(canonicalType, Tuple.class, dynamic);
        this.elementTypes = elementTypes;
//        this.staticByteLen = dynamic ? OFFSET_LENGTH_BYTES : staticTupleLen(this);
    }

    static TupleType wrap(ABIType<?>... elements) {
        StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (ABIType<?> e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elements); // TODO .intern() string?
    }

    public int size() {
        return elementTypes.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public ABIType<?> get(int index) {
        return elementTypes[index];
    }

    @Override
    Class<?> arrayClass() {
        return Tuple[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_TUPLE;
    }

//    @Override
//    int staticByteLength() {
//        return byteLength(null);
//    }
//
//    @Override
//    int dynamicByteLength(Object value) {
//        Tuple tuple = (Tuple) value;
//        return countBytes(false, size(), 0, i -> measureObject(get(i), tuple.get(i)));
//    }

    @Override
    int byteLength(Object value) {
//        if(!dynamic) return staticByteLen;
        Tuple tuple = (Tuple) value;
        return countBytes(false, size(), 0, i -> measureObject(get(i), tuple.get(i)));
//        return dynamic ? dynamicByteLength(value) : staticByteLength();
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
        return countBytes(false, size(), 0, i -> get(i).byteLengthPacked(elements[i]));
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
            return countBytes(false, this.size(), 0, i -> validateObject(get(i), value.get(i)));
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
        encodeObjects(dynamic, ((Tuple) value).elements, TupleType.this::get, dest);
    }

    @Override
    void encodePackedUnchecked(Tuple value, ByteBuffer dest) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            get(i).encodeObjectPackedUnchecked(value.get(i), dest);
        }
    }

    static void encodeObjects(boolean dynamic, Object[] values, IntFunction<ABIType<?>> getType, ByteBuffer dest) {
        int offset = !dynamic ? 0 : headLength(values, getType);
        for (int i = 0; i < values.length; i++) {
            offset = getType.apply(i).encodeHead(values[i], dest, offset);
        }
        for (int i = 0; i < values.length; i++) {
            ABIType<?> t = getType.apply(i);
            if(t.dynamic) {
                t.encodeTail(values[i], dest);
            }
        }
    }

    private static int headLength(Object[] elements, IntFunction<ABIType<?>> getType) {
        int sum = 0;
        for (int i = 0; i < elements.length; i++) {
            ABIType<?> type = getType.apply(i);
            sum += type.dynamic ? OFFSET_LENGTH_BYTES : type.byteLength(elements[i]);
//            sum += getType.apply(i).staticByteLength();
        }
        return sum;
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {
        Object[] elements = new Object[size()];
        decodeObjects(bb, unitBuffer, TupleType.this::get, elements);
        return new Tuple(elements);
    }

    public <T> T decodeIndex(byte[] encoded, int index) {
        return decodeIndex(ByteBuffer.wrap(encoded), index);
    }

    <T> T decodeIndex(ByteBuffer bb, int index) {
        final byte[] unitBuffer = newUnitBuffer();
        final int pos = bb.position();
        int skipBytes = 0;
        for (int i = 0; i < index; i++) {
            final ABIType<?> et = elementTypes[i];
            switch (et.typeCode()) {
            case TYPE_CODE_ARRAY:
                skipBytes += et.dynamic ? OFFSET_LENGTH_BYTES : ArrayType.staticArrLen(et);
                continue;
            case TYPE_CODE_TUPLE:
                skipBytes += et.dynamic ? OFFSET_LENGTH_BYTES : staticTupleLen(et);
                continue;
            default:
                skipBytes += UNIT_LENGTH_BYTES;
            }
        }
        bb.position(pos + skipBytes);
        @SuppressWarnings("unchecked")
        final ABIType<T> t = (ABIType<T>) elementTypes[index];
        if(t.dynamic) {
            bb.position(pos + UINT31.decode(bb, unitBuffer));
        }
        return t.decode(bb, unitBuffer);
    }

    static int staticTupleLen(ABIType<?> tt) {
        int len = 0;
        for (ABIType<?> e : (TupleType) tt) {
            switch (e.typeCode()) {
            case TYPE_CODE_ARRAY: len += ArrayType.staticArrLen(e); continue;
            case TYPE_CODE_TUPLE: len += staticTupleLen(e); continue;
            default: len += UNIT_LENGTH_BYTES;
            }
        }
        return len;
    }

    static void decodeObjects(ByteBuffer bb, byte[] unitBuffer, IntFunction<ABIType<?>> getType, Object[] objects) {
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
            if(offset > 0) {
                final int jump = start + offset;
                final int pos = bb.position();
                if(jump != pos) {
                    /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                    if(jump < pos) {
                        throw new IllegalArgumentException("illegal backwards jump: (" + start + "+" + offset + "=" + jump + ")<" + pos);
                    }
                    bb.position(jump); // leniently jump to specified offset
                }
                objects[i] = getType.apply(i).decode(bb, unitBuffer);
            }
        }
    }

    /**
     * Parses RLP Object {@link com.esaulpaugh.headlong.rlp.util.Notation} as a {@link Tuple}.
     *
     * @param s the tuple's RLP object notation
     * @return  the parsed tuple
     * @see com.esaulpaugh.headlong.rlp.util.Notation
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

    public TupleType subTupleType(boolean... manifest) {
        return subTupleType(manifest, false);
    }

    public TupleType subTupleTypeNegative(boolean... manifest) {
        return subTupleType(manifest, true);
    }

    private TupleType subTupleType(final boolean[] manifest, final boolean negate) {
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
            return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, selected.toArray(EMPTY_ARRAY));
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

    public static TupleType parseElements(String rawTypesList) {
        if(rawTypesList.endsWith(",")) {
            rawTypesList = rawTypesList.substring(0, rawTypesList.length() - 1);
        }
        return parse('(' + rawTypesList + ')');
    }
}
