package com.esaulpaugh.headlong.abi;

class TupleType extends Type {

    private final Type[] types;

    public TupleType(Type... types) {
        super(typeStringForTypes(types));
        this.types = types;
    }

    protected TupleType(String typeString, Type... types) {
        super(typeString);
        this.types = types;
    }

//    private Tuple(String signature, int start) {
//
//    }

    private TupleType(String typeString) {
        super(typeString);
        this.types = null;
    }

    static TupleType forTypeString(String typeString) {
        return new TupleType(typeString);
    }

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

//        return len == 0
//                ? sb.toString()
//                : sb.replace(strLen - 1, strLen, "").toString();
    }

    Type[] getTypes() {
        return types;
    }

    @Override
    public int calcDynamicByteLen(Object param) {

        // TODO conform with spec

//        final Tuple tuple = (Tuple) param;
        final Object[] elements = ((Tuple) param).elements;
        int byteLen = 0;
        final int len = this.types.length;
        for (int i = 0; i < len; i++) {
            byteLen += this.types[i].calcDynamicByteLen(elements[i]);
        }
        return byteLen;
    }

//    private static String parseTuple(Matcher matcher, String signature, int tupleStart) throws ParseException {
//        int idx = tupleStart;
//        int tupleDepth = 0;
//        int openTuple, closeTuple;
//        do {
//            openTuple = signature.indexOf('(', idx);
//            closeTuple = signature.indexOf(')', idx);
//
//            if(closeTuple < 0) {
//                throw new ParseException("non-terminating tuple", tupleStart);
//            }
//
//            while(idx < closeTuple) {
//
//            }
//
//            if(openTuple == -1 || closeTuple < openTuple) {
//                tupleDepth--;
//                idx = closeTuple + 1;
//            } else {
//                tupleDepth++;
//                idx = openTuple + 1;
//            }
//        } while(tupleDepth > 0);
//
////        checkParamChars(matcher, signature, tupleStart, idx);
//        String tuple = signature.substring(tupleStart, idx);
//        System.out.println("tuple: " + tuple); // uncanonicalized
//
//        return tuple;
//    }

}
