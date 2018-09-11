package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;
import com.esaulpaugh.headlong.abi.beta.util.Utils;

import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.beta.Encoder.OFFSET_LENGTH_BYTES;

/**
 * TODO subtupletype method like subtuple()
 */
class TupleType extends StackableType<Tuple> {

    private static final String CLASS_NAME = Tuple.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = Utils.getNameStub(Tuple[].class);

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

//        if(elements.length != numTypes) {
//            throw new IllegalArgumentException("tuple length mismatch");
//        }

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
    int validate(final Object value) {
        super.validate(value);

        final Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;
        final int actualLength = elements.length;

        final StackableType<?>[] elementTypes = this.elementTypes;
        final int expectedLength = elementTypes.length;

        if(expectedLength != actualLength) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actualLength + " != " + expectedLength);
        }

        final int numTypes = elementTypes.length;

//        if(elements.length != numTypes) {
//            throw new IllegalArgumentException("tuple length mismatch");
//        }

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
//        System.out.println("T decode " + toString() + " " + convertPos(bb) + " " + dynamic);

//        final int index = bb.position(); // TODO remove eventually

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
//        System.out.println("T heads " + convertPos(bb) + ", " + bb.position());
        final int tupleLen = offsets.length;
        StackableType<?> elementType;
        for (int i = 0; i < tupleLen; i++) {
            elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = Encoder.OFFSET_TYPE.decode(bb, elementBuffer);
//                System.out.println("T offset " + convertOffset(offsets[i]) + " @ " + convert(bb.position() - OFFSET_LENGTH_BYTES));
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    static void decodeTails(ByteBuffer bb, final StackableType<?>[] elementTypes, int[] offsets, byte[] elementBuffer, final Object[] dest) {
//        System.out.println("T tails " + convertPos(bb) + ", " + bb.position());
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            int offset = offsets[i];
//            System.out.println("T jumping to " + convert(index + offset));
            if (offset > 0) {
//                if(bb.position() != index + offset) {
//                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                    throw new RuntimeException();
//                }
                dest[i] = elementTypes[i].decode(bb, elementBuffer);
            }
        }
    }
}
