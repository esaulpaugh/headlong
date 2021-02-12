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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;

/** @see ABIType */
public final class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    private static final String EMPTY_TUPLE_STRING = "()";

    public static final TupleType EMPTY = new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_TYPE_ARRAY);

    final ABIType<?>[] elementTypes;

    private TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes) {
        super(canonicalType, Tuple.class, dynamic);
        this.elementTypes = elementTypes;
    }

    static <E extends ABIType<?>> TupleType wrap(E[] elements) {
        StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (E e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elements); // TODO .intern() string?
    }

    public int size() {
        return elementTypes.length;
    }

    public ABIType<?> get(int index) {
        return elementTypes[index];
    }

    public ABIType<?>[] elements() {
        return Arrays.copyOf(elementTypes, elementTypes.length);
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
    int byteLength(Object value) {
        final Object[] elements = ((Tuple) value).elements;
        return len((i) -> {
            ABIType<?> type = elementTypes[i];
            int byteLen = type.byteLength(elements[i]);
            return type.dynamic ? OFFSET_LENGTH_BYTES + byteLen : byteLen;
        });
    }

    /**
     * @param value the Tuple being measured. {@code null} if not available
     * @return the length in bytes of the non-standard packed encoding
     */
    @Override
    public int byteLengthPacked(Object value) {
        if (value == null) {
            return len((i) -> elementTypes[i].byteLengthPacked(null));
        }
        final Object[] elements = ((Tuple) value).elements;
        return len((i) -> elementTypes[i].byteLengthPacked(elements[i]));
    }

    private int len(IntFunction<Integer> elementCount) {
        int len = 0;
        for(int i = 0; i < elementTypes.length; i++) {
            len += elementCount.apply(i);
        }
        return len;
    }

    @Override
    public int validate(final Object value) {
        validateClass(value);

        final Object[] elements = ((Tuple) value).elements;

        if(elements.length == elementTypes.length) {
            int i = 0;
            try {
                return len((j) -> {
                    ABIType<?> type = elementTypes[j];
                    int byteLen = type.validate(elements[j]);
                    return type.dynamic ? OFFSET_LENGTH_BYTES + byteLen : byteLen;
                });
            } catch (NullPointerException | IllegalArgumentException e) {
                throw new IllegalArgumentException("tuple index " + i + ": " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + elements.length + " != " + elementTypes.length);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        encodeObjects(dynamic, ((Tuple) value).elements, dest, (i) -> elementTypes[i]);
    }

    static void encodeObjects(boolean dynamic, Object[] values, ByteBuffer dest, IntFunction<ABIType<?>> getType) {
        int nextOffset = !dynamic ? -1 : headLengthSum(values, getType);
        for (int i = 0; i < values.length; i++) {
            nextOffset = getType.apply(i).encodeHead(values[i], dest, nextOffset);
        }
        for (int i = 0; i < values.length; i++) {
            ABIType<?> t = getType.apply(i);
            if(t.dynamic) {
                t.encodeTail(values[i], dest);
            }
        }
    }

    private static int headLengthSum(Object[] elements, IntFunction<ABIType<?>> getType) {
        int sum = 0;
        for (int i = 0; i < elements.length; i++) {
            ABIType<?> type = getType.apply(i);
            sum += !type.dynamic ? type.byteLength(elements[i]) : OFFSET_LENGTH_BYTES;
        }
        return sum;
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {
        final int len = elementTypes.length;
        Object[] elements = new Object[len];
        decodeObjects(len, bb, unitBuffer, elements, (i) -> elementTypes[i]);
        return new Tuple(elements);
    }

    private static int[] decodeHeads(int len, ByteBuffer bb, byte[] unitBuffer, Object[] elements, IntFunction<ABIType<?>> getType) {
        final int[] offsets = new int[len];
        for(int i = 0; i < len; i++) {
            ABIType<?> t = getType.apply(i);
            if(!t.dynamic) {
                elements[i] = t.decode(bb, unitBuffer);
            } else {
                offsets[i] = Encoding.UINT31.decode(bb, unitBuffer);
            }
        }
        return offsets;
    }

    static void decodeObjects(int len, ByteBuffer bb, byte[] unitBuffer, Object[] elements, IntFunction<ABIType<?>> getType) {
        final int start = bb.position(); // save this value before offsets are decoded
        final int[] offsets = decodeHeads(len, bb, unitBuffer, elements, getType);
        for (int i = 0; i < len; i++) {
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
                elements[i] = getType.apply(i).decode(bb, unitBuffer);
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
     * @see #encode(Tuple)
     */
    public ByteBuffer encodeElements(Object... elements) {
        return encode(new Tuple(elements));
    }

    public ByteBuffer encode(Tuple values) {
        ByteBuffer dest = ByteBuffer.allocate(validate(values));
        encodeTail(values, dest);
        return dest;
    }

    public TupleType encode(Tuple values, ByteBuffer dest) {
        validate(values);
        encodeTail(values, dest);
        return this;
    }

    /**
     * Returns the non-standard-packed encoding of {@code values}.
     *
     * @param values the argument to be encoded
     * @return the encoding
     */
    public ByteBuffer encodePacked(Tuple values) {
        validate(values);
        ByteBuffer dest = ByteBuffer.allocate(byteLengthPacked(values));
        PackedEncoder.encodeTuple(this, values, dest);
        return dest;
    }

    /**
     * Puts into the given {@link ByteBuffer} at its current position the non-standard packed encoding of {@code values}.
     *
     * @param values the argument to be encoded
     * @param dest   the destination buffer
     */
    public void encodePacked(Tuple values, ByteBuffer dest) {
        validate(values);
        PackedEncoder.encodeTuple(this, values, dest);
    }

    @Override
    public Iterator<ABIType<?>> iterator() {
        return new Iterator<ABIType<?>>() {

            private int index; // = 0

            @Override
            public boolean hasNext() {
                return index < elementTypes.length;
            }

            @Override
            public ABIType<?> next() {
                try {
                    return elementTypes[index++];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    throw new NoSuchElementException(aioobe.getMessage());
                }
            }
        };
    }

    public TupleType subTupleType(boolean... manifest) {
        return subTupleType(manifest, false);
    }

    public TupleType subTupleTypeNegative(boolean... manifest) {
        return subTupleType(manifest, true);
    }

    private TupleType subTupleType(boolean[] manifest, boolean negate) {
        final int len = manifest.length;
        if(len == elementTypes.length) {
            final StringBuilder canonicalBuilder = new StringBuilder("(");
            boolean dynamic = false;
            final ArrayList<ABIType<?>> selected = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                if (negate ^ manifest[i]) {
                    ABIType<?> e = elementTypes[i];
                    canonicalBuilder.append(e.canonicalType).append(',');
                    dynamic |= e.dynamic;
                    selected.add(e);
                }
            }
            return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, selected.toArray(EMPTY_TYPE_ARRAY));
        }
        throw new IllegalArgumentException("manifest.length != elementTypes.length: " + len + " != " + elementTypes.length);
    }

    static String completeTupleTypeString(StringBuilder sb) {
        final int len = sb.length();
        return len != 1
                ? sb.deleteCharAt(len - 1).append(')').toString() // replace trailing comma
                : EMPTY_TUPLE_STRING;
    }

    public static TupleType parse(String rawTupleTypeString) {
        return (TupleType) TypeFactory.createType(rawTupleTypeString, null);
    }

    public static TupleType of(String... typeStrings) {
        StringBuilder sb = new StringBuilder("(");
        for (String str : typeStrings) {
            sb.append(str).append(',');
        }
        return parse(completeTupleTypeString(sb));
    }

    public static TupleType parseElements(String rawElementsString) {
        return parse('(' + rawElementsString + ')');
    }
}
