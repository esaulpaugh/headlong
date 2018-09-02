package com.esaulpaugh.headlong.abi.beta.type;

import com.esaulpaugh.headlong.abi.beta.type.array.ArrayType;
import com.esaulpaugh.headlong.abi.beta.type.integer.AbstractInt256Type;
import com.esaulpaugh.headlong.abi.beta.type.integer.IntType;
import com.esaulpaugh.headlong.abi.beta.util.Pair;

/**
 *
 */
public class TupleType extends StackableType<Object[]> {

    public final StackableType[] memberTypes;

    TupleType(String canonicalAbiType, boolean dynamic, StackableType... memberTypes) {
        super(canonicalAbiType, com.esaulpaugh.headlong.abi.beta.util.Tuple.class.getName(), dynamic); // Tuple.class.getName()
        this.memberTypes = memberTypes;
    }

    public static TupleType create(String canonicalAbiType, StackableType... members) {

        for (StackableType type : members) {
            if(type.dynamic) {
                return new TupleType(canonicalAbiType, true, members);
            }
        }

        return new TupleType(canonicalAbiType, false, members);
    }

    @Override
    public String toString() {
        if(memberTypes.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (StackableType memberType : memberTypes) {
            sb.append(memberType).append(',');
        }
        sb.replace(sb.length() - 1, sb.length(), "").append(')');
        return getClass().getSimpleName() + sb.toString();
    }

    @Override
    public int byteLength(Object value) {
        com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;

        if(tuple.elements.length != memberTypes.length) {
            throw new IllegalArgumentException("tuple length mismatch");
        }

        int len = 0;
        for (int i = 0; i < memberTypes.length; i++) {
            len += memberTypes[i].byteLength(tuple.elements[i]);
        }

        return len; // + 32;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] decode(byte[] buffer, final int index) {

        final int tupleLen = memberTypes.length;
        Object[] members = new Object[tupleLen];

        int[] offsets = new int[tupleLen];

        int idx = index;
        for (int i = 0; i < tupleLen; i++) {
            StackableType type = memberTypes[i];
            if (type.dynamic) {
                offsets[i] = IntType.OFFSET_TYPE.decode(buffer, idx);
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            } else {
                if (type instanceof ArrayType) {
                    Pair<Object, Integer> results = ((ArrayType) type).decodeArray(buffer, idx);
                    members[i] = results.first;
                    idx = results.second;
                } else if (type instanceof TupleType) {
                    members[i] = type.decode(buffer, idx); // TODO update idx *********************
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
                    StackableType type = memberTypes[i];
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

//        for(int i = 0; i < len; i++) {
//            // TODO dynamic offsets
//            StackableType type = memberTypes[i];
//            System.out.println(i + (type.dynamic ? " dynamic" : " static"));
//        }
        return members;
    }

    @Override
    public void validate(final Object value) {
        super.validate(value);

        final com.esaulpaugh.headlong.abi.beta.util.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.util.Tuple) value;
        final Object[] elements = tuple.elements;

        final int expected = this.memberTypes.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("tuple length valid;");

        checkTypes(this.memberTypes, elements);
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
