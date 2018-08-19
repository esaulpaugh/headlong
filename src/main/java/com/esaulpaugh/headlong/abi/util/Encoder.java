package com.esaulpaugh.headlong.abi.util;

import java.nio.ByteBuffer;

public class Encoder {

    private static final byte[] PADDING_192_BITS = new byte[24];

    public static void insertBytes(byte[] src,/* int offset, int len,*/ ByteBuffer dest) {
        dest.put(src);
        final int n = Integer.SIZE - src.length;
        for (int i = 0; i < n; i++) {
            dest.put((byte) 0);
        }
    }

    public static void insertBytesArray(byte[][] src, ByteBuffer dest) {
        for(byte[] e : src) {
            insertBytes(e, dest);
        }
    }

    public static void insertBool(boolean bool, ByteBuffer dest) {
        insertInt(bool ? 1L : 0L, dest);
    }

    public static void insertInt(long val, ByteBuffer dest) {
//        final int pos = dest.position();
        dest.put(PADDING_192_BITS);
        dest.putLong(val);
//        putLongBigEndian(val, dest, pos + NUM_PADDING_BYTES);
//        return pos + INT_PARAM_LENGTH_BYTES;
    }

    public static void insertInts(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

}
