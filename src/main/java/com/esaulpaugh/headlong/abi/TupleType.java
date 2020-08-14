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
import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.SuperSerial;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;

/** @see ABIType */
public final class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    private static final String ARRAY_CLASS_NAME = Tuple[].class.getName();

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
    String arrayClassName() {
        return ARRAY_CLASS_NAME;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_TUPLE;
    }

    @Override
    int byteLength(Object value) {
        final Object[] elements = ((Tuple) value).elements;
        int len = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            ABIType<?> type = elementTypes[i];
            int byteLen = type.byteLength(elements[i]);
            len += !type.dynamic ? byteLen : OFFSET_LENGTH_BYTES + byteLen;
        }
        return len;
    }

    private int staticByteLengthPacked() {
        int len = 0;
        for (ABIType<?> elementType : elementTypes) {
            len += elementType.byteLengthPacked(null);
        }
        return len;
    }

    /**
     * @param value the Tuple being measured. {@code null} if not available
     * @return the length in bytes of the non-standard packed encoding
     */
    @Override
    public int byteLengthPacked(Object value) {
        if (value == null) {
            return staticByteLengthPacked();
        }
        final Object[] elements = ((Tuple) value).elements;
        int len = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            len += elementTypes[i].byteLengthPacked(elements[i]);
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
                int len = 0;
                for (; i < elementTypes.length; i++) {
                    ABIType<?> type = elementTypes[i];
                    int byteLen = type.validate(elements[i]);
                    len += !type.dynamic ? byteLen : OFFSET_LENGTH_BYTES + byteLen;
                }
                return len;
            } catch (NullPointerException | IllegalArgumentException e) {
                throw new IllegalArgumentException("tuple index " + i + ": " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + elements.length + " != " + elementTypes.length);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        final Object[] values = ((Tuple) value).elements;
        if(!dynamic) {
            encodeHeads(elementTypes, values, dest, -1);
            return;
        }
        final ABIType<?>[] types = elementTypes;
        encodeHeads(types, values, dest, headLengthSum(types, values));
        for (int i = 0; i < types.length; i++) {
            ABIType<?> t = types[i];
            if(t.dynamic) {
                t.encodeTail(values[i], dest);
            }
        }
    }

    private static void encodeHeads(ABIType<?>[] types, Object[] values, ByteBuffer dest, int nextOffset) {
        for (int i = 0; i < types.length; i++) {
            nextOffset = types[i].encodeHead(values[i], dest, nextOffset);
        }
    }

    private static int headLengthSum(ABIType<?>[] types, Object[] elements) {
        int sum = 0;
        for (int i = 0; i < types.length; i++) {
            ABIType<?> type = types[i];
            sum += !type.dynamic ? type.byteLength(elements[i]) : OFFSET_LENGTH_BYTES;
        }
        return sum;
    }

    public Tuple decode(byte[] array) {
        ByteBuffer bb = ByteBuffer.wrap(array);
        Tuple decoded = decode(bb);
        final int remaining = bb.remaining();
        if(remaining == 0) {
            return decoded;
        }
        throw new IllegalArgumentException("unconsumed bytes: " + remaining + " remaining");
    }

    public Tuple decode(ByteBuffer bb) {
        return decode(bb, newUnitBuffer());
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {
        final int len = elementTypes.length;
        final Object[] elements = new Object[len];
        if (!dynamic) {
            for (int i = 0; i < len; i++) {
                elements[i] = elementTypes[i].decode(bb, unitBuffer);
            }
        } else {
            final int tailStart = bb.position(); // save this value for later
            final int[] offsets = new int[len];
            for (int i = 0; i < len; i++) {
                ABIType<?> elementType = elementTypes[i];
                if (!elementType.dynamic) {
                    elements[i] = elementType.decode(bb, unitBuffer);
                } else {
                    offsets[i] = Encoding.UINT31.decode(bb, unitBuffer);
                }
            }
            decodeTails(bb, offsets, tailStart, (i) -> elements[i] = elementTypes[i].decode(bb, unitBuffer));
        }
        return new Tuple(elements);
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

    public int measureEncodedLength(Tuple values) {
        return validate(values);
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
     * Puts into the {@link ByteBuffer} at its current position the non-standard packed encoding of {@code values}.
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

    public TupleType subTupleType(boolean[] manifest) {
        return subTupleType(manifest, false);
    }

    public TupleType subTupleType(boolean[] manifest, boolean negate) {
        final int len = checkLength(elementTypes, manifest);
        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        final ABIType<?>[] selected = new ABIType<?>[getSelectionSize(manifest, negate)];
        for (int i = 0, s = 0; i < len; i++) {
            if(negate ^ manifest[i]) {
                ABIType<?> e = elementTypes[i];
                canonicalBuilder.append(e.canonicalType).append(',');
                dynamic |= e.dynamic;
                selected[s++] = e;
            }
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, selected);
    }

    private static int checkLength(ABIType<?>[] elements, boolean[] manifest) {
        final int len = manifest.length;
        if(len == elements.length) {
            return len;
        }
        throw new IllegalArgumentException("manifest.length != elements.length: " + manifest.length + " != " + elements.length);
    }

    private static int getSelectionSize(boolean[] manifest, boolean negate) {
        int count = 0;
        for (boolean b : manifest) {
            if(b) {
                count++;
            }
        }
        return negate ? manifest.length - count : count;
    }

    private static String completeTupleTypeString(StringBuilder sb) {
        final int len = sb.length();
        return len != 1
                ? sb.replace(len - 1, len, ")").toString() // replace trailing comma
                : EMPTY_TUPLE_STRING;
    }

    public static TupleType parse(String rawTupleTypeString) {
        return (TupleType) TypeFactory.create(rawTupleTypeString, null);
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

    public static String format(byte[] abi) {
        return format(abi, new LabelMaker() {
            @Override
            public String make(int offset) {
                StringBuilder sb = new StringBuilder();
                String labelStr = Long.toHexString(offset);
                int zeroes = 6 - labelStr.length();
                for (int i = 0; i < zeroes; i++) {
                    sb.append(' ');
                }
                sb.append(labelStr);
                pad(sb, ' ');
                return sb.toString();
            }
        });
    }

    public static String format(byte[] abi, LabelMaker labelMaker) {
        Integers.checkIsMultiple(abi.length, UNIT_LENGTH_BYTES);
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while(idx < abi.length) {
            if(idx > 0) {
                sb.append('\n');
            }
            sb.append(labelMaker.make(idx));
            sb.append(Strings.encode(abi, idx, UNIT_LENGTH_BYTES, HEX));
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }

    public static class LabelMaker {

        static final int INDENT_WIDTH = 9;

        public String make(int offset) {
            StringBuilder sb = new StringBuilder();
            sb.append(offset / UNIT_LENGTH_BYTES);
            pad(sb, ' ');
            return sb.toString();
        }

        public void pad( StringBuilder sb, char ch) {
            int n = INDENT_WIDTH - sb.length();
            for (int i = 0; i < n; i++) {
                sb.append(ch);
            }
        }
    }
}
