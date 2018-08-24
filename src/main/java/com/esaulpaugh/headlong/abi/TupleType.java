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

        return dynamic ?
                32 + dataByteLen
                : dataByteLen;
    }

    static int getLengthInfo(Type[] types, Object[] arguments, int[] headLengths) {
//        int dynamicOverheadBytes = 0;
        int paramsByteLen = 0;
        final int n = headLengths.length;
        for (int i = 0; i < n; i++) {
            Type t = types[i];
            int byteLen = t.getDataByteLen(arguments[i]);
            System.out.print(arguments[i] + " --> " + byteLen + ", ");
            paramsByteLen += byteLen;

            if(t.dynamic) {
                headLengths[i] = 32;
//                dynamicOverheadBytes += 32 + 32; // 32
                System.out.println("dynamic");
            } else {
                headLengths[i] = byteLen;
                System.out.println("static");
            }
        }

//        System.out.println("**************** " + dynamicOverheadBytes);

        System.out.println("**************** " + paramsByteLen);

        // dynamicOverheadBytes +
        return paramsByteLen;
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

        final Tuple tuple = (Tuple) param;
        final Object[] elements = tuple.elements;

        final int expected = this.types.length;
        final int actual = elements.length;
        if(expected != actual) {
            throw new IllegalArgumentException("tuple length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("length valid;");

        checkTypes(this.types, elements);
//        for (int i = 0; i < expected; i++) {
//            validate(elements[i], types[i].javaClassName, 0);
//        }
    }

    public static void checkTypes(Type[] paramTypes, Object[] arguments) {
        final int n = paramTypes.length;
        int i = 0;
        try {
            for ( ; i < n; i++) {
                paramTypes[i].validate(arguments[i]);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("invalid param @ " + i + ": " + e.getMessage(), e);
        }
    }
}
