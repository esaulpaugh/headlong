package com.esaulpaugh.headlong.abi.beta.type;

import java.util.Stack;

class DynamicArray extends Array {

    private int dynamicLen;

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType) {
        this(canonicalAbiType, className, elementType, -1);
    }

    protected DynamicArray(String canonicalAbiType, String className, StackableType elementType, int length) {
        super(canonicalAbiType, className, elementType, length, true);
    }

    public void setDynamicLen(int dynamicLen) {
        this.dynamicLen = dynamicLen;
    }

    @Override
    int byteLength(Object value) {

        return getDataLen(value);

//        return getLength(value);
//        return -2;
    }

//    private int getLength(Object[] value, final int i, final int n, Stack<Integer> staticLengthStack) {
//        final boolean dynamic = staticLengthStack.get(i) == null;
//
//        if (i >= n) {
//            final int baseLen = dynamic ? 64 * value.length : 0;
//            String str = i + " >= " + n + ", baseLen = " + baseLen;
//            System.out.print(str);
//            int len = baseLen;
//            for(Object obj : value) {
//                len += getDataLen(obj); // , dynamic
//            }
//            System.out.println(", len = " + len);
//            return len;
//        }
//        final int baseLen = dynamic ? 64 * value.length : 0;
//        String str = i + "(baseLen = " + baseLen;
//        System.out.println(str);
//        int len = baseLen;
//        for (Object[] arr : (Object[][]) value) {
//            len += getLength(arr, i + 1, n, staticLengthStack);
//        }
//        System.out.println("len = " + len + ")" + i);
//        return len;
//
//
//////        int dynamicCount = 0;
//////        int staticCount = 0;
////        int overhead;
////        if(staticLen == null) {
////            overhead = 64 * value.length;
////            System.out.println("before " + i + ": " + overhead);
////            for (Object[] arr : (Object[][]) value) {
////                overhead += getLength(arr, i + 1, n, staticLengthStack);
////            }
////            System.out.println("after " + i + ": " + overhead);
////        } else {
////            overhead = 0;
////            System.out.println(i +  " static");
////        }
////        return overhead;
//    }
}