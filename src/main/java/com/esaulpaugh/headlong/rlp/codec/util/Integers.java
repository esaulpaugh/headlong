package com.esaulpaugh.headlong.rlp.codec.util;

import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;

public class Integers {

    public static int getInt(byte[] buffer, int i, int numBytes) throws DecodeException {
        int shiftAmount = 0;
        int val = 0;
        switch (numBytes) { /* cases fall through */
        case 4: val |= (buffer[i+3] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[i];
            val |= (lead & 0xFF) << shiftAmount;
            // validate
            if(lead == 0 && val > 0) {
                throw new DecodeException("Deserialised positive integers with leading zeroes are invalid.");
            }
            return val;
        default: throw new DecodeException(new IllegalArgumentException("numBytes out of range: " + numBytes));
        }
    }

    public static long get(final byte[] buffer, final int i, final int numBytes) throws DecodeException {

        int shiftAmount = 0;

        long val = 0;
        switch (numBytes) { /* cases fall through */
        case 8: val |= (buffer[i+7] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[i];
            val |= (lead & 0xFFL) << shiftAmount;
            // validate
            if(lead == 0 && val > 0) {
                throw new DecodeException("Deserialised positive integers with leading zeroes are invalid.");
            }
        default: return val;
        }
    }

//    public static byte[] intToBytes(int val) {
//        return longToBytes(val);
//    }
//
//    public static int putInt(int val, byte[] o, int i) {
//        return putLong(val, o, i);
//    }

    public static byte[] toBytes(long val) {
        int n = numBytes(val);
        byte[] bytes = new byte[n];
        put(val, bytes, 0);
        return bytes;
    }



    /**
     *
     * @param val
     * @param o
     * @param i
     * @return  the number of bytes inserted
     */
    public static int put(long val, byte[] o, int i) {

        byte a = 0, b = 0, c = 0, d = 0, e = 0, f = 0, g = 0, h;

        int n = 1;
        h = (byte) (val & 0xFF);
        val = val >>> Byte.SIZE;
        if(val != 0) {
            n = 2;
            g = (byte) (val & 0xFF);
            val = val >>> Byte.SIZE;
            if(val != 0) {
                n = 3;
                f = (byte) (val & 0xFF);
                val = val >>> Byte.SIZE;
                if(val != 0) {
                    n = 4;
                    e = (byte) (val & 0xFF);
                    val = val >>> Byte.SIZE;
                    if(val != 0) {
                        n = 5;
                        d = (byte) (val & 0xFF);
                        val = val >>> Byte.SIZE;
                        if(val != 0) {
                            n = 6;
                            c = (byte) (val & 0xFF);
                            val = val >>> Byte.SIZE;
                            if(val != 0) {
                                n = 7;
                                b = (byte) (val & 0xFF);
                                val = val >>> Byte.SIZE;
                                if(val != 0) {
                                    n = 8;
                                    a = (byte) (val & 0xFF);
                                }
                            }
                        }
                    }
                }
            }
        }

        switch (n) {
        case 1: o[i]=h; return 1;
        case 2: o[i]=g; o[i+1]=h; return 2;
        case 3: o[i]=f; o[i+1]=g; o[i+2]=h; return 3;
        case 4: o[i]=e; o[i+1]=f; o[i+2]=g; o[i+3]=h; return 4;
        case 5: o[i]=d; o[i+1]=e; o[i+2]=f; o[i+3]=g; o[i+4]=h; return 5;
        case 6: o[i]=c; o[i+1]=d; o[i+2]=e; o[i+3]=f; o[i+4]=g; o[i+5]=h; return 6;
        case 7: o[i]=b; o[i+1]=c; o[i+2]=d; o[i+3]=e; o[i+4]=f; o[i+5]=g; o[i+6]=h; return 7;
        default:o[i]=a; o[i+1]=b; o[i+2]=c; o[i+3]=d; o[i+4]=e; o[i+5]=f; o[i+6]=g; o[i+7]=h; return 8;
        }
    }

    public static int numBytes(long val) {
        int n = 1;
        val = val >>> Byte.SIZE;
        if(val != 0) {
            n = 2;
            val = val >>> Byte.SIZE;
            if(val != 0) {
                n = 3;
                val = val >>> Byte.SIZE;
                if(val != 0) {
                    n = 4;
                    val = val >>> Byte.SIZE;
                    if(val != 0) {
                        n = 5;
                        val = val >>> Byte.SIZE;
                        if(val != 0) {
                            n = 6;
                            val = val >>> Byte.SIZE;
                            if(val != 0) {
                                n = 7;
                                val = val >>> Byte.SIZE;
                                if(val != 0) {
                                    n = 8;
                                }
                            }
                        }
                    }
                }
            }
        }
        return n;
    }

    public static void insertBytes(int n, byte[] b, int i, byte w, byte x, byte y, byte z) {
        insertBytes(n, b, i, (byte) 0, (byte) 0, (byte) 0, (byte) 0, w, x, y, z);
    }

    /**
     * Inserts bytes into an array in the order they are given.
     * @param n     the number of bytes to insert
     * @param b     the buffer into which the bytes will be inserted
     * @param i     the index at which to insert
     * @param s     the lead byte if eight bytes are to be inserted
     * @param t     the lead byte if seven bytes are to be inserted
     * @param u     the lead byte if six bytes are to be inserted
     * @param v     the lead byte if five bytes are to be inserted
     * @param w     the lead byte if four bytes are to be inserted
     * @param x     the lead byte if three bytes are to be inserted
     * @param y     the lead byte if two bytes are to be inserted
     * @param z     the last byte
     */
    public static void insertBytes(int n, byte[] b, int i, byte s, byte t, byte u, byte v, byte w, byte x, byte y, byte z) {
        switch (n) { /* cases fall through */
        case 8: b[i++] = s;
        case 7: b[i++] = t;
        case 6: b[i++] = u;
        case 5: b[i++] = v;
        case 4: b[i++] = w;
        case 3: b[i++] = x;
        case 2: b[i++] = y;
        case 1: b[i] = z;
        }
    }
}
