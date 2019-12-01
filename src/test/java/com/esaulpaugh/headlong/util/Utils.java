package com.esaulpaugh.headlong.util;

public class Utils {

    public static int insertBytes(int n, byte[] b, int i, byte w, byte x, byte y, byte z) {
        if(n <= 4) {
            return Utils.insertBytes(n, b, i, (byte) 0, (byte) 0, (byte) 0, (byte) 0, w, x, y, z);
        }
        throw new IllegalArgumentException("n must be <= 4");
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
     * @return n    the number of bytes inserted
     */
    public static int insertBytes(int n, byte[] b, int i, byte s, byte t, byte u, byte v, byte w, byte x, byte y, byte z) {
        switch (n) { /* cases fall through */
        case 8: b[i++] = s;
        case 7: b[i++] = t;
        case 6: b[i++] = u;
        case 5: b[i++] = v;
        case 4: b[i++] = w;
        case 3: b[i++] = x;
        case 2: b[i++] = y;
        case 1: b[i] = z;
        case 0: return n;
        default: throw new IllegalArgumentException("n is out of range: " + n);
        }
    }
}
