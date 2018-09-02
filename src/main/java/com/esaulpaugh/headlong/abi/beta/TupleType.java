package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Pair;
import com.esaulpaugh.headlong.abi.beta.util.Tuple;

/**
 *
 */
class TupleType extends StackableType<Tuple> {

    final StackableType[] elementTypes;

    transient int tag = -1; // to hold tuple end index temporarily

    TupleType(String canonicalAbiType, boolean dynamic, StackableType... elementTypes) {
        super(canonicalAbiType, com.esaulpaugh.headlong.abi.beta.util.Tuple.class.getName(), dynamic); // Tuple.class.getName()
        this.elementTypes = elementTypes;
    }

    static TupleType create(String canonicalAbiType, StackableType... members) {

        for (StackableType type : members) {
            if(type.dynamic) {
                return new TupleType(canonicalAbiType, true, members);
            }
        }

        return new TupleType(canonicalAbiType, false, members);
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
    @SuppressWarnings("unchecked")
    Tuple decode(byte[] buffer, final int index) {

        final int tupleLen = elementTypes.length;
        Object[] members = new Object[tupleLen];

        int[] offsets = new int[tupleLen];

        int idx = index;
        for (int i = 0; i < tupleLen; i++) {
            StackableType type = elementTypes[i];
            if (type.dynamic) {
                offsets[i] = IntType.OFFSET_TYPE.decode(buffer, idx);
                System.out.println("offset " + offsets[i] + " @ " + idx);
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            } else {
                if (type instanceof ArrayType) {
                    Pair<Object, Integer> results = ((ArrayType) type).decodeArray(buffer, idx);
                    members[i] = results.first;
                    idx = results.second;
                } else if (type instanceof TupleType) {
                    TupleType tt = (TupleType) type;
                    members[i] = tt.decode(buffer, idx);
                    idx = tt.tag;
                } else {
                    members[i] = type.decode(buffer, idx);
                    idx += AbstractInt256Type.INT_LENGTH_BYTES;
                }
            }
        }

        if(dynamic) {
            for (int i = 0; i < tupleLen; i++) {
                idx = index + offsets[i];
                if (idx > index) {
                    StackableType type = elementTypes[i];
                    if (type instanceof ArrayType) {
                        Pair<Object, Integer> results = ((ArrayType) type).decodeArray(buffer, idx);
                        members[i] = results.first;
//                    idx = results.second;
                    } else {
                        members[i] = type.decode(buffer, idx);
                    }
                }
            }
        }

        this.tag = idx;

        return new Tuple(members);
    }

    @Override
    void validate(final Object value) {
        super.validate(value);

        final com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;
        final Object[] elements = tuple.elements;

        final int expected = this.elementTypes.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("tuple length valid;");

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
