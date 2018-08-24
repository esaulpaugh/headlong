package com.esaulpaugh.headlong.abi;

class TupleType extends Type {

    private final Type[] types;

    private TupleType(boolean dynamic, Type... types) {
        super(typeStringForTypes(types), Tuple.class.getName(), dynamic);
        this.types = types;
    }

    static TupleType create(Type... types) {

        boolean dynamic = false;
        for (Type t : types) {
            dynamic |= t.dynamic;
        }

        return new TupleType(dynamic, types);
    }

//    static TupleType forTypeString(String typeString) {
////        ABI.parseTuple();
//        return null;
//    }

    protected static String typeStringForTypes(Type... types) {
        StringBuilder sb = new StringBuilder("(");
        final int len = types.length;
        for (int i = 0; i < len; i++) {
            sb.append(types[i]).append(",");
        }
        final int strLen = sb.length();

        if(len > 0) {
            sb.replace(strLen - 1, strLen, "");
        }

        return sb.append(")").toString();
    }

    Type[] getTypes() {
        return types;
    }

    @Override
    public int calcDynamicByteLen(Object param) {
        final Object[] elements = ((Tuple) param).elements;
        int byteLen = 0;
        final int len = this.types.length;
        for (int i = 0; i < len; i++) {
            byteLen += this.types[i].calcDynamicByteLen(elements[i]);
        }
        return byteLen;
    }

    @Override
    public Integer getByteLen() {
        return null;
    }

    @Override
    protected void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(param, expectedClassName, expectedLengthIndex);

        Tuple tuple = (Tuple) param;

        Type[] types = getTypes();
        final int typesLen = types.length;
        if(typesLen != tuple.elements.length) {
            throw new IllegalArgumentException("tuple length mismatch: expected: " + typesLen + ", actual: " + tuple.elements.length);
        }
        System.out.println("length valid;");
        for (int i = 0; i < typesLen; i++) {
            validate(tuple.elements[i], types[i].javaClassName, 0);
        }
    }
}
