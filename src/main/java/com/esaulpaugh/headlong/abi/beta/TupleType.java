package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.beta.Encoder.OFFSET_LENGTH_BYTES;

/**
 *
 */
class TupleType extends StackableType<Tuple> {

    private static final String CLASS_NAME = Tuple.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = Tuple[].class.getName().replaceFirst("\\[", "");

    final StackableType[] elementTypes;

    private TupleType(String canonicalType, boolean dynamic, StackableType... elementTypes) {
        super(canonicalType, dynamic);
        this.elementTypes = elementTypes;
    }

    static TupleType create(String canonicalType, StackableType... members) {
        for (StackableType type : members) {
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
        for (StackableType memberType : elementTypes) {
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
    int byteLength(Object value) {
        com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;

        if(tuple.elements.length != elementTypes.length) {
            throw new IllegalArgumentException("tuple length mismatch");
        }

        int len = 0;
        for (int i = 0; i < elementTypes.length; i++) {
            StackableType type = elementTypes[i];
            if(type.dynamic) {
                len += OFFSET_LENGTH_BYTES;
            }
            len += type.byteLength(tuple.elements[i]);
        }

        return len;
    }

    @Override
    Tuple decode(ByteBuffer bb, byte[] elementBuffer) {

        final int index = bb.position(); // TODO remove eventually

        final int tupleLen = elementTypes.length;
        Object[] elements = new Object[tupleLen];

        int[] offsets = new int[tupleLen];
        decodeHeads(bb, offsets, elementBuffer, elements);

        if(dynamic) {
            decodeTails(bb, index, offsets, elementBuffer, elements);
        }

        return new Tuple(elements);
    }

    private void decodeHeads(ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            StackableType elementType = elementTypes[i];
            if (elementType.dynamic) {
                offsets[i] = Encoder.OFFSET_TYPE.decode(bb, elementBuffer);
            } else {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private void decodeTails(ByteBuffer bb, final int index, int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int tupleLen = offsets.length;
        for (int i = 0; i < tupleLen; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                if(bb.position() != index + offset) { // TODO remove this check eventually
                    System.err.println(TupleType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
                    bb.position(index + offset);
                }
                dest[i] = elementTypes[i].decode(bb, elementBuffer);
            }
        }
    }

    @Override
    void validate(final Object value) {
        super.validate(value);

        final Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        final int expected = this.elementTypes.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }

        checkTypes(this.elementTypes, elements);
    }

    private static void checkTypes(StackableType[] paramTypes, Object[] values) {
        final int n = paramTypes.length;
        int i = 0;
        try {
            for ( ; i < n; i++) {
                paramTypes[i].validate(values[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid arg @ " + i + ": " + e.getMessage(), e);
        }
    }
}
