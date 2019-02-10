package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.CallEncoder.OFFSET_LENGTH_BYTES;

public class TupleType extends StackableType<Tuple> {

    private static final String CLASS_NAME = Tuple.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(Tuple[].class);

    final StackableType<?>[] elementTypes;

    private TupleType(String canonicalType, boolean dynamic, StackableType<?>... elementTypes) {
        super(canonicalType, dynamic);
        this.elementTypes = elementTypes;
    }

    static TupleType create(String canonicalType, StackableType<?>... members) {
        for (StackableType<?> type : members) {
            if(type.dynamic) {
                return new TupleType(canonicalType, true, members);
            }
        }
        return new TupleType(canonicalType, false, members);
    }

    @Override
    String className() {
        return CLASS_NAME;
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

        final StackableType<?>[] types = this.elementTypes;
        final int numTypes = types.length;

        int len = 0;
        StackableType<?> type;
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

        final StackableType<?>[] types = this.elementTypes;
        final int numTypes = types.length;

        int len = 0;
        for (int i = 0; i < numTypes; i++) {
            len += types[i].byteLengthPacked(elements[i]);
        }

        return len;
    }

    @Override
    int validate(final Object value) {
        super.validate(value);

        final Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;
        final int actualLength = elements.length;

        final StackableType<?>[] elementTypes = this.elementTypes;
        final int expectedLength = elementTypes.length;

        if(actualLength != expectedLength) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actualLength + " != " + expectedLength);
        }

        final int numTypes = elementTypes.length;

        int byteLength = 0;
        StackableType<?> type;
        int i = 0;
        try {
            for ( ; i < numTypes; i++) {
                type = elementTypes[i];
                byteLength += type.dynamic
                        ? OFFSET_LENGTH_BYTES + type.validate(elements[i])
                        : type.validate(elements[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("illegal arg @ " + i + ": " + e.getMessage());
        }

        return byteLength;
    }

    public Tuple decode(ByteBuffer bb) {
        return decode(bb, newUnitBuffer());
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] unitBuffer) {

//        final int index = bb.position(); // TODO must pass index to decodeTails if you want to support lenient mode

        final StackableType<?>[] elementTypes = this.elementTypes;
        final int tupleLen = elementTypes.length;
        Object[] elements = new Object[tupleLen];

        int[] offsets = new int[tupleLen];
        decodeHeads(bb, elementTypes, offsets, unitBuffer, elements);

        if(dynamic) {
            decodeTails(bb, elementTypes, offsets, unitBuffer, elements);
        }

        return new Tuple(elements);
    }

    static void decodeHeads(ByteBuffer bb, StackableType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, Object[] dest) {
        final int tupleLen = offsets.length;
        StackableType<?> elementType;
        for (int i = 0; i < tupleLen; i++) {
            elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = CallEncoder.OFFSET_TYPE.decode(bb, elementBuffer);
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    static void decodeTails(ByteBuffer bb, final StackableType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            final StackableType<?> type = elementTypes[i];
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

    public static TupleType parse(String tupleTypeString) throws ParseException {
        return TupleTypeParser.parseTupleType(tupleTypeString);
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
}
