package com.esaulpaugh.headlong.abi;

class TupleType extends Type {

    final Type[] types;

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

//    Type[] getTypes() {
//        return types;
//    }

//    @Override
//    public int calcDynamicByteLen(Object param) {
//        final Object[] elements = ((Tuple) param).elements;
//        int byteLen = 0;
//        final int len = this.types.length;
//        for (int i = 0; i < len; i++) {
//            byteLen += this.types[i].calcDynamicByteLen(elements[i]);
//        }
//        return byteLen;
//    }

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

        return dataByteLen;
    }

//    @Override
//    public Integer getDataByteLen(Object value) {
//        Stack<Integer> dynamicByteLenStack = new Stack<>();
//        ArrayType.buildByteLenStack(((Tuple) value).elements, dynamicByteLenStack);
//        int tupleDepth = dynamicByteLenStack.size() - 1;
//        if(baseTypeByteLen == 1 && !canonicalAbiType.startsWith("bytes1")) { // typeString.startsWith("int8") || typeString.startsWith("uint8")
//            tupleDepth--;
//        }
//        int n = 1;
//        for (int i = tupleDepth - 1; i >= 0; i--) {
//            int len;
//            Integer fixedLen = fixedLengthStack.get(i);
//            if(fixedLen != null) {
//                len = fixedLen;
//            } else {
//                len = dynamicByteLenStack.get(i);
//            }
//            n *= len;
//        }
//        return roundUp(n);
//    }

    @Override
    public Integer getNumElements(Object value) {
        return ((Tuple) value).elements.length;
    }

//    protected Integer getDataLen(Object param) {
//        final Object[] elements = ((Tuple) param).elements;
//        int byteLen = 0;
//        final int len = this.types.length;
//        for (int i = 0; i < len; i++) {
//            byteLen += this.types[i].getHeadLen(elements[i]);
//        }
//        return byteLen;
//    }

    @Override
    protected void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
        super.validate(param, expectedClassName, expectedLengthIndex);

        Tuple tuple = (Tuple) param;

        Type[] types = this.types;
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
