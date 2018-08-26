package com.esaulpaugh.headlong.abi.beta;

class TupleType extends Type {

    final Type[] types;

    private TupleType(boolean dynamic, Type... types) {
        super(typeStringForTypes(types), Tuple.class.getName(), dynamic);
        this.types = types;
    }

    static TupleType create(Type... types) {

//        boolean dynamic = false;
        for (Type t : types) {
            if(t.dynamic) {
                return new TupleType(true, types);
            }
//            dynamic |= t.dynamic;
        }

        return new TupleType(false, types);
    }

    private static String typeStringForTypes(Type... types) {
        StringBuilder sb = new StringBuilder("(");
        final int len = types.length;
        for (Type type : types) {
            sb.append(type).append(",");
        }
        final int strLen = sb.length();

        if(len > 0) {
            sb.replace(strLen - 1, strLen, "");
        }

        return sb.append(")").toString();
    }

    @Override
    public Integer getDataByteLen(Object value) {

        Tuple tuple = (Tuple) value;
        final Type[] types = this.types;
        final Object[] elements = tuple.elements;

        int dataByteLen = 0;

        final int len = types.length;
        for (int i = 0; i < len; i++) {
            dataByteLen += types[i].getDataByteLen(elements[i]);
        }

        return dataByteLen; // dynamic tuples don't have extra prefix

//        return dynamic ?
//                32 + dataByteLen
//                : dataByteLen;
    }

    static void getLengthInfo(Type[] types, Object[] arguments, int[] headLengths) {
        int argsByteLen = 0;
        final int n = headLengths.length;
        for (int i = 0; i < n; i++) {
            Type t = types[i];
            int byteLen = t.getDataByteLen(arguments[i]);
            System.out.print(arguments[i] + " --> " + byteLen + ", ");
            argsByteLen += byteLen;

            if(t.dynamic) {
                headLengths[i] = 32;
                System.out.println("dynamic");
            } else {
                headLengths[i] = byteLen;
                System.out.println("static");
            }
        }

        System.out.println("**************** " + argsByteLen);

//        return argsByteLen;
    }

    static int[] getHeadLengths(Type[] types, Object[] values) {
        final int len = types.length;
        int[] headLengths = new int[len];
        Type type;
        for (int i = 0; i < len; i++) {
            type = types[i];
            headLengths[i] = type.dynamic
                    ? 32
                    : type.getDataByteLen(values[i]);
        }
        return headLengths;
    }

    @Override
    protected void validate(final Object value, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(value, expectedClassName, expectedLengthIndex);

        final Tuple tuple = (Tuple) value;
        final Object[] elements = tuple.elements;

        final int expected = this.types.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("length valid;");

        checkTypes(this.types, elements);
    }

    public static void checkTypes(Type[] paramTypes, Object[] values) {
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
