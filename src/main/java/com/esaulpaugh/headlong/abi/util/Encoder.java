package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.abi.Tuple;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Encoder {

    private static final byte[] PADDING_192_BITS = new byte[24];

    public static void insertBytesArray(byte[][] src, ByteBuffer dest) {
        for(byte[] e : src) {
            insertBytes(e, dest);
        }
    }

    public static void insertBool(boolean bool, ByteBuffer dest) {
        insertInt(bool ? 1L : 0L, dest);
    }

    public static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            insertBool(e, dest);
        }
    }

    public static void insertBytes(byte[] src,/* int offset, int len,*/ ByteBuffer dest) {
        dest.put(src);
        // pad todo
        final int n = Integer.SIZE - src.length;
        for (int i = 0; i < n; i++) {
            dest.put((byte) 0);
        }
    }

    public static void insertShorts(short[] shorts, ByteBuffer dest) {
        for (short e : shorts) {
            insertInt(e, dest);
        }
    }

    public static void insertInts(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

    public static void insertLongs(long[] ints, ByteBuffer dest) {
        for (long e : ints) {
            insertInt(e, dest);
        }
    }

    public static void insertInt(long val, ByteBuffer dest) {
//        final int pos = dest.position();
        dest.put(PADDING_192_BITS);
        dest.putLong(val);
//        putLongBigEndian(val, dest, pos + NUM_PADDING_BYTES);
//        return pos + INT_PARAM_LENGTH_BYTES;
    }

    public static void insertInt(BigInteger bigGuy, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        final int lim = 32 - arr.length;
        for (int i = 0; i < lim; i++) {
            dest.put((byte) 0);
        }
        dest.put(arr);
    }

    public static void insertTuple(Tuple tuple) {
        throw new Error("not yet implemented");
    }

}
