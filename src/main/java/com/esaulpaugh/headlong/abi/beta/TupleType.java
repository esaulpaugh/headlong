package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

/**
 *
 */
class TupleType extends DynamicType<Tuple> {

    private static final String CLASS_NAME = Tuple.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = Tuple[].class.getName().replaceFirst("\\[", "");

    final StackableType[] elementTypes;

//    transient int tag = -1; // to hold tuple end index temporarily

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
            len += type.byteLength(tuple.elements[i]);
            if(type.dynamic) {
                len += 32; // for offset
            }
        }

        return len;
    }

    @Override
    Tuple decode(byte[] buffer, int index) {
        return decodeDynamic(buffer, index, new int[1]);
    }

    @Override
    Tuple decodeDynamic(final byte[] buffer, final int index, final int[] returnIndex) {

        if(returnIndex.length != 1) {
            throw new IllegalArgumentException("returnIndex must be length 1");
        }

        final int tupleLen = elementTypes.length;
        Object[] members = new Object[tupleLen];

        int[] offsets = new int[tupleLen];

        int idx = index;
        for (int i = 0; i < tupleLen; i++) {
            StackableType type = elementTypes[i];
            if (type.dynamic) {
                offsets[i] = Encoder.OFFSET_TYPE.decodeStatic(buffer, idx);
                System.out.println("offset " + offsets[i] + " @ " + idx);
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            } else {
                if (type instanceof DynamicType) {
                    members[i] = ((DynamicType) type).decodeDynamic(buffer, idx, returnIndex);
                    idx = returnIndex[0];
                } else {
                    members[i] = ((StaticType) type).decodeStatic(buffer, idx);
                    idx += AbstractInt256Type.INT_LENGTH_BYTES;
                }
            }
        }

        if(dynamic) {
            for (int i = 0; i < tupleLen; i++) {
                idx = index + offsets[i];
                if (idx > index) {
                    StackableType type = elementTypes[i];
                    if (type instanceof DynamicType) {
                        members[i] = ((DynamicType) type).decodeDynamic(buffer, idx, returnIndex);
                    } else {
                        members[i] = ((StaticType) type).decodeStatic(buffer, idx);
                    }
                }
            }
        }

        returnIndex[0] = idx;
        return new Tuple(members);
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
