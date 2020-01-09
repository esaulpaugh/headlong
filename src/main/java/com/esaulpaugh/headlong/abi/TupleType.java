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
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.abi.Encoding.OFFSET_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;

public final class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    private static final Class<Tuple> CLASS = Tuple.class;
    private static final String ARRAY_CLASS_NAME = Tuple[].class.getName();

    private static final String EMPTY_TUPLE_STRING = "()";

    public static final TupleType EMPTY = new TupleType(EMPTY_TUPLE_STRING, false, EMPTY_TYPE_ARRAY);

    final ABIType<?>[] elementTypes;

    private TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes) {
        super(canonicalType, CLASS, dynamic);
        this.elementTypes = elementTypes;
    }

    static <A extends ABIType<?>> TupleType wrap(A[] elements) {
        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (A e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elements);
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
        Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        int len = 0;
        ABIType<?> type;
        for (int i = 0; i < elementTypes.length; i++) {
            type = elementTypes[i];
            len += type.dynamic
                    ? OFFSET_LENGTH_BYTES + type.byteLength(elements[i])
                    : type.byteLength(elements[i]);
        }
        return len;
    }

    @Override
    public int byteLengthPacked(Object value) {
        Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        int len = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            len += elementTypes[i].byteLengthPacked(elements[i]);
        }
        return len;
    }

    @Override
    public int validate(final Object value) throws ABIException {
        validateClass(value);

        final Object[] elements = ((Tuple) value).elements;

        final int numTypes = elementTypes.length;

        if(elements.length != numTypes) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " +
                    elements.length + " != " + numTypes);
        }

        int len = 0;
        int i = 0;
        try {
            for ( ; i < numTypes; i++) {
                final ABIType<?> type = elementTypes[i];
                len += type.dynamic
                        ? OFFSET_LENGTH_BYTES + type.validate(elements[i])
                        : type.validate(elements[i]);
            }
        } catch (NullPointerException npe) {
            throw new ABIException("illegal arg @ " + i + ": " + npe.getMessage());
        }
        return len;
    }

    @Override
    void encodeHead(Object value, ByteBuffer dest, int[] offset) {
        if (!dynamic) {
            encodeTail(value, dest);
        } else {
            Encoding.insertOffset(offset, this, value, dest);
        }
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        final Object[] values = ((Tuple) value).elements;
        final int[] offset = new int[] { headLengthSum(values) };

        final int len = elementTypes.length;
        for (int i = 0; i < len; i++) {
            elementTypes[i].encodeHead(values[i], dest, offset);
        }
        if(dynamic) {
            for (int i = 0; i < len; i++) {
                ABIType<?> type = elementTypes[i];
                if (type.dynamic) {
                    type.encodeTail(values[i], dest);
                }
            }
        }
    }

    private int headLengthSum(Object[] elements) {
        int headLengths = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            ABIType<?> et = elementTypes[i];
            headLengths += et.dynamic
                    ? OFFSET_LENGTH_BYTES
                    : et.byteLength(elements[i]);
        }
        return headLengths;
    }

    public Tuple decode(byte[] array) throws ABIException {
        return decode(ByteBuffer.wrap(array));
    }

    public Tuple decode(ByteBuffer bb) throws ABIException {
        return decode(bb, newUnitBuffer());
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) throws ABIException {

//        final int index = bb.position(); // TODO must pass index to decodeTails if you want to support lenient mode

        final int tupleLen = elementTypes.length;
        Object[] elements = new Object[tupleLen];

        int[] offsets = new int[tupleLen];
        decodeHeads(bb, elementTypes, offsets, unitBuffer, elements);

        if(dynamic) {
            decodeTails(bb, elementTypes, offsets, unitBuffer, elements);
        }

        return new Tuple(elements);
    }

    private static void decodeHeads(ByteBuffer bb, ABIType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, Object[] dest) throws ABIException {
        final int tupleLen = offsets.length;
        ABIType<?> elementType;
        for (int i = 0; i < tupleLen; i++) {
            elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = Encoding.OFFSET_TYPE.decode(bb, elementBuffer);
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private static void decodeTails(ByteBuffer bb, final ABIType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, final Object[] dest) throws ABIException {
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            final ABIType<?> type = elementTypes[i];
            final int offset = offsets[i];
            final boolean offsetExists = offset > 0;
            if(type.dynamic ^ offsetExists) { // if not matching
                throw new ABIException(type.dynamic ? "offset not found" : "offset found for static element");
            }
            if (offsetExists) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                }
                dest[i] = type.decode(bb, elementBuffer);
            }
        }
    }

    @Override
    public Tuple parseArgument(String s) {
        throw new UnsupportedOperationException();
    }

    public static TupleType parse(String rawTupleTypeString) {
        try {
            return (TupleType) TypeFactory.create(rawTupleTypeString, null);
        } catch (ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
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

    public ByteBuffer encodeElements(Object... elements) throws ABIException {
        return encode(new Tuple(elements));
    }

    public ByteBuffer encode(Tuple values) throws ABIException {
        ByteBuffer dest = ByteBuffer.allocate(validate(values));
        encodeTail(values, dest);
        return dest;
    }

    public TupleType encode(Tuple values, ByteBuffer dest) throws ABIException {
        validate(values);
        encodeTail(values, dest);
        return this;
    }

    public int measureEncodedLength(Tuple values) throws ABIException {
        return validate(values);
    }

    public ByteBuffer encodePacked(Tuple values) {
        ByteBuffer dest = ByteBuffer.allocate(byteLengthPacked(values));
        encodePacked(values, dest);
        return dest;
    }

    public void encodePacked(Tuple values, ByteBuffer dest) {
        PackedEncoder.insertTuple(this, values, dest);
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
        if(len != elements.length) {
            throw new IllegalArgumentException("manifest.length != elements.length: " + manifest.length + " != " + elements.length);
        }
        return len;
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

    private static String completeTupleTypeString(StringBuilder ttsb) {
        final int len = ttsb.length();
        if(len == 1) {
            return EMPTY_TUPLE_STRING;
        }
        return ttsb.replace(len - 1, len, ")").toString(); // replace trailing comma
    }

    public static String format(byte[] abi) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while(idx < abi.length) {
            sb.append(Strings.encode(Arrays.copyOfRange(abi, idx, idx + UNIT_LENGTH_BYTES), HEX)).append('\n');
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }
}
