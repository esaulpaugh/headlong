package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;

import static com.esaulpaugh.headlong.abi.CallEncoder.OFFSET_LENGTH_BYTES;

public class TupleType extends ABIType<Tuple> implements Iterable<ABIType<?>> {

    private static final Class<?> CLASS = Tuple.class;
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(Tuple[].class);

    final ABIType<?>[] elementTypes;

    private TupleType(String canonicalType, boolean dynamic, ABIType<?>[] elementTypes) {
        super(canonicalType, CLASS, dynamic);
        this.elementTypes = elementTypes;
    }

    static <L extends List<ABIType<?>> & RandomAccess> TupleType create(L elementsList) {

        final int len = elementsList.size();

        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        final ABIType<?>[] elementsArray = new ABIType<?>[len];

        for (int i = 0; i < len; i++) {
            ABIType<?> e = elementsList.get(i);
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.dynamic;
            elementsArray[i] = e;
        }

        return new TupleType(completeTupleTypeString(canonicalBuilder), dynamic, elementsArray);
    }

    private static String completeTupleTypeString(StringBuilder canonicalTupleType) {
        final int len = canonicalTupleType.length();
        if(len == 1) {
            return "()";
        }
        return canonicalTupleType.replace(len - 1, len, ")").toString(); // replace trailing comma
    }

    public ABIType<?> get(int index) {
        return elementTypes[index];
    }

    public ABIType<?>[] elements() {
        return Arrays.copyOf(elementTypes, elementTypes.length);
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_TUPLE;
    }

    @Override
    int byteLength(Object value) {
        Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        final ABIType<?>[] types = this.elementTypes;
        final int numTypes = types.length;

        int len = 0;
        ABIType<?> type;
        for (int i = 0; i < numTypes; i++) {
            type = types[i];
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

        final ABIType<?>[] types = this.elementTypes;
        final int numTypes = types.length;

        int len = 0;
        for (int i = 0; i < numTypes; i++) {
            len += types[i].byteLengthPacked(elements[i]);
        }

        return len;
    }

    @Override
    public Tuple parseArgument(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int validate(final Object value) {
        validateClass(value);

        final Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        final ABIType<?>[] elementTypes = this.elementTypes;

        final int numTypes = elementTypes.length;

        if(elements.length != numTypes) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " +
                    elements.length + " != " + numTypes);
        }

        int byteLength = 0;
        int i = 0;
        try {
            for ( ; i < numTypes; i++) {
                final ABIType<?> type = elementTypes[i];
                byteLength += type.dynamic
                        ? OFFSET_LENGTH_BYTES + type.validate(elements[i])
                        : type.validate(elements[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("illegal arg @ " + i + ": " + e.getMessage());
        }

        return byteLength;
    }

    public Tuple decode(byte[] array) {
        return decode(ByteBuffer.wrap(array));
    }

    public Tuple decode(ByteBuffer bb) {
        return decode(bb, newUnitBuffer());
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {

//        final int index = bb.position(); // TODO must pass index to decodeTails if you want to support lenient mode

        final ABIType<?>[] elementTypes = this.elementTypes;
        final int tupleLen = elementTypes.length;
        Object[] elements = new Object[tupleLen];

        int[] offsets = new int[tupleLen];
        decodeHeads(bb, elementTypes, offsets, unitBuffer, elements);

        if(dynamic) {
            decodeTails(bb, elementTypes, offsets, unitBuffer, elements);
        }

        return new Tuple(elements);
    }

    static void decodeHeads(ByteBuffer bb, ABIType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, Object[] dest) {
        final int tupleLen = offsets.length;
        ABIType<?> elementType;
        for (int i = 0; i < tupleLen; i++) {
            elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = CallEncoder.OFFSET_TYPE.decode(bb, elementBuffer);
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    static void decodeTails(ByteBuffer bb, final ABIType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            final ABIType<?> type = elementTypes[i];
            final int offset = offsets[i];
            final boolean offsetExists = offset > 0;
            if(type.dynamic ^ offsetExists) { // if not matching
                throw new IllegalArgumentException(type.dynamic ? "offset not found" : "offset found for static element");
            }
            if (offsetExists) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                    throw new RuntimeException();
//                }
                dest[i] = type.decode(bb, elementBuffer);
            }
        }
    }

    boolean recursiveEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TupleType tupleType = (TupleType) o;
        return Arrays.equals(elementTypes, tupleType.elementTypes);
    }

    public static TupleType parse(String rawTupleTypeString) throws ParseException {
        return TupleTypeParser.parseTupleType(rawTupleTypeString);
    }

    public static TupleType parseElements(String rawElementsString) throws ParseException {
        return parse('(' + rawElementsString + ')');
    }

    public ByteBuffer encodeElements(Object... elements) {
        return encode(new Tuple(elements));
    }

    public ByteBuffer encode(Tuple values) {
        ByteBuffer output = ByteBuffer.allocate(validate(values));
        CallEncoder.insertTuple(this, values, output);
        return output;
    }

    public TupleType encode(Tuple values, ByteBuffer dest, boolean validate) {
        if(validate) {
            validate(values);
        }
        CallEncoder.insertTuple(this, values, dest);
        return this;
    }

    public int encodedLen(Tuple values) {
        return validate(values);
    }

    public int encodedLen(Tuple values, boolean validate) {
        return validate ? validate(values) : byteLength(values);
    }

    public byte[] encodePacked(Tuple values) {
        byte[] dest = new byte[byteLengthPacked(values)];
        PackedEncoder.insertTuple(this, values, dest, 0);
        return dest;
    }

    public void encodePacked(Tuple values, byte[] dest, int idx) {
        PackedEncoder.insertTuple(this, values, dest, idx);
    }

    @Override
    public Iterator<ABIType<?>> iterator() {
        return new Iterator<ABIType<?>>() {

            private int index = 0;

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

    public void recursiveToString(StringBuilder sb) {
        for(ABIType<?> e : this) {
            if(e.typeCode() == TYPE_CODE_TUPLE) {
                ((TupleType) e).recursiveToString(sb);
                sb.append(' ').append(e.getName()).append(',');
            } else {
                sb.append(e).append(' ').append(e.getName()).append(',');
            }
        }
    }
}
