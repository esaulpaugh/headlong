package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.CallEncoder.OFFSET_LENGTH_BYTES;

class TupleType extends StackableType<Tuple> {

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
    public String toString() {
        if(elementTypes.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (StackableType<?> memberType : elementTypes) {
            sb.append(memberType).append(',');
        }
        sb.replace(sb.length() - 1, sb.length(), "").append(')');
        return getClass().getSimpleName() + sb.toString();
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
    int byteLengthPacked(Object value) {
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

    @Override
    Tuple decode(ByteBuffer bb, byte[] elementBuffer) {

//        final int index = bb.position(); // TODO must pass index to decodeTails if you want to support lenient mode

        final StackableType<?>[] elementTypes = this.elementTypes;
        final int tupleLen = elementTypes.length;
        Object[] elements = new Object[tupleLen];

        int[] offsets = new int[tupleLen];
        decodeHeads(bb, elementTypes, offsets, elementBuffer, elements);

        if(dynamic) {
            decodeTails(bb, elementTypes, offsets, elementBuffer, elements);
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
            int offset = offsets[i];
            if (offset > 0) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                    throw new RuntimeException();
//                }
                dest[i] = elementTypes[i].decode(bb, elementBuffer);
            }
        }
    }

    public boolean recursiveEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TupleType tupleType = (TupleType) o;
        return Arrays.equals(elementTypes, tupleType.elementTypes);
    }

    public void encodePacked(Tuple tuple, byte[] dest, int idx) {
        PackedEncoder.insertTuple(this, tuple, dest, idx);
    }
}
