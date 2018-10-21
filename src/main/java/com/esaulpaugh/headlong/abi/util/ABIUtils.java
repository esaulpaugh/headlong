//package com.esaulpaugh.headlong.abi.util;
//
//import java.nio.ByteBuffer;
//
///**
// * Utils for debugging
// */
//public class ABIUtils {
//
//    public static int convertPos(ByteBuffer bb) {
//        return (bb.position() - 4) >>> 5;
//    }
//
//    public static int convert(int pos) {
//        return convertOffset(pos - 4);
//    }
//
//    public static int convertOffset(int pos) {
//        return pos >>> 5;
//    }
//}
