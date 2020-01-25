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

    static <E extends ABIType<?>> TupleType wrap(E[] elements) {
        StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        for (E e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
        }
        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elements); // TODO .intern() string?
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
        int len = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            ABIType<?> type = elementTypes[i];
            len += !type.dynamic
                    ? type.byteLength(tuple.elements[i])
                    : OFFSET_LENGTH_BYTES + type.byteLength(tuple.elements[i]);
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
    int encodeHead(Object value, ByteBuffer dest, int offset) {
        if (!dynamic) {
            encodeTail(value, dest);
            return offset;
        }
        return Encoding.insertOffset(offset, this, value, dest);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        final Object[] values = ((Tuple) value).elements;
        final int len = elementTypes.length;
        int offset = headLengthSum(values);
        for (int i = 0; i < len; i++) {
            offset = elementTypes[i].encodeHead(values[i], dest, offset);
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
        Object[] elements = new Object[elementTypes.length];
        if(dynamic) {
            decodeDynamic(bb, elementTypes, unitBuffer, elements);
        } else {
            for (int i = 0; i < elementTypes.length; i++) {
                elements[i] = elementTypes[i].decode(bb, unitBuffer);
            }
        }
        return new Tuple(elements);
    }

    private static void decodeDynamic(ByteBuffer bb, ABIType<?>[] elementTypes, byte[] elementBuffer, Object[] dest) throws ABIException {
//        final int index = bb.position(); // *** save this value here if you want to support lenient mode below
        final int len = elementTypes.length;
        int[] offsets = new int[len];
        for (int i = 0; i < len; i++) {
            ABIType<?> elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = Encoding.OFFSET_TYPE.decode(bb, elementBuffer);
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
        for (int i = 0; i < len; i++) {
            if (offsets[i] > 0) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset); // lenient
//                }
                dest[i] = elementTypes[i].decode(bb, elementBuffer);
            }
        }
    }

    @Override
    public Tuple parseArgument(String s) {
        throw new UnsupportedOperationException();
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

    public ByteBuffer encodePacked(Tuple values) throws ABIException {
        validate(values);
        ByteBuffer dest = ByteBuffer.allocate(byteLengthPacked(values));
        PackedEncoder.encodeTuple(this, values, dest);
        return dest;
    }

    public void encodePacked(Tuple values, ByteBuffer dest) throws ABIException {
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

    private static String completeTupleTypeString(StringBuilder ttsb) {
        final int len = ttsb.length();
        if(len != 1) {
            return ttsb.replace(len - 1, len, ")").toString(); // replace trailing comma
        }
        return EMPTY_TUPLE_STRING;
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

    public static String format(byte[] abi) {
        Utils.checkIsMultiple(abi.length);
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while(idx < abi.length) {
            sb.append(Strings.encode(abi, idx, UNIT_LENGTH_BYTES, HEX)).append('\n');
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }
}
