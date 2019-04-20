package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.exception.UnrecoverableDecodeException;
import com.esaulpaugh.headlong.util.BizarroIntegers;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.util.Utils.EMPTY_BYTE_ARRAY;

/**
 * Utility for reading and writing integers from and to RLP format.
 */
public class Integers {

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val   the integer
     * @return  the minimal representation
     */
    public static byte[] toBytes(byte val) {
        if(val == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return new byte[] { val };
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val   the integer
     * @return  the minimal representation
     */
    public static byte[] toBytes(short val) {
        if(val == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        int n = len(val);
        byte[] bytes = new byte[n];
        putShort(val, bytes, 0);
        return bytes;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val   the integer
     * @return  the minimal representation
     */
    public static byte[] toBytes(int val) {
        if(val == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        int n = len(val);
        byte[] bytes = new byte[n];
        putInt(val, bytes, 0);
        return bytes;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val   the integer
     * @return  the minimal representation
     */
    public static byte[] toBytes(long val) {
        if(val == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        int n = len(val);
        byte[] bytes = new byte[n];
        putLong(val, bytes, 0);
        return bytes;
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to one byte in length. The integer zero always has length zero.
     *
     * @see #toBytes(byte)
     * @see #getByte(byte[], int, int)
     * @param val   the integer to be inserted
     * @param o the output array
     * @param i the index into the output
     * @return  the number of bytes inserted
     */
    public static int putByte(byte val, byte[] o, int i) {
        if(val != 0) {
            o[i] = val;
            return 1;
        }
        return 0;
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to two bytes in length. The integer zero always has length zero.
     *
     * @see #toBytes(short)
     * @see #getShort(byte[], int, int)
     * @param val   the integer to be inserted
     * @param o the output array
     * @param i the index into the output
     * @return  the number of bytes inserted
     */
    public static int putShort(short val, byte[] o, int i) {
        byte b = 0;
        int n = 0;
        if(val != 0) {
            n = 1;
            b = (byte) val;
//            val = (short) (val >>> Byte.SIZE); // ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT
            val = (short) (val >> Byte.SIZE); // high bytes chopped off either way, see above
            if (val != 0) {
                n = 2;
            }
        }
        return insertBytes(n, o, i, (byte) 0, (byte) 0, (byte) val, b);
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to four bytes in length. The integer zero always has length zero.
     *
     * @see #toBytes(int)
     * @see #getInt(byte[], int, int)
     * @param val   the integer to be inserted
     * @param o the output array
     * @param i the index into the output
     * @return  the number of bytes inserted
     */
    public static int putInt(int val, byte[] o, int i) {
        byte b = 0, c = 0, d = 0;
        int n = 0;
        if(val != 0) {
            n = 1;
            d = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                n = 2;
                c = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    n = 3;
                    b = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        n = 4;
                    }
                }
            }
        }
        return insertBytes(n, o, i, (byte) val, b, c, d);
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to eight bytes in length. The integer zero always has length zero.
     *
     * @see #toBytes(long)
     * @see #getLong(byte[], int, int)
     * @param val   the integer to be inserted
     * @param o the output array
     * @param i the index into the output
     * @return  the number of bytes inserted
     */
    public static int putLong(long val, byte[] o, int i) {
        byte b = 0, c = 0, d = 0, e = 0, f = 0, g = 0, h = 0;
        int n = 0;
        if(val != 0) {
            n = 1;
            h = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                n = 2;
                g = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    n = 3;
                    f = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        n = 4;
                        e = (byte) val;
                        if ((val >>>= Byte.SIZE) != 0) {
                            n = 5;
                            d = (byte) val;
                            if ((val >>>= Byte.SIZE) != 0) {
                                n = 6;
                                c = (byte) val;
                                if ((val >>>= Byte.SIZE) != 0) {
                                    n = 7;
                                    b = (byte) val;
                                    if ((val >>>= Byte.SIZE) != 0) {
                                        n = 8;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return insertBytes(n, o, i, (byte) val, b, c, d, e, f, g, h);
    }

    public static int putLong(long val, ByteBuffer o) {
        byte b = 0, c = 0, d = 0, e = 0, f = 0, g = 0, h = 0;
        int n = 0;
        if(val != 0) {
            n = 1;
            h = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                n = 2;
                g = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    n = 3;
                    f = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        n = 4;
                        e = (byte) val;
                        if ((val >>>= Byte.SIZE) != 0) {
                            n = 5;
                            d = (byte) val;
                            if ((val >>>= Byte.SIZE) != 0) {
                                n = 6;
                                c = (byte) val;
                                if ((val >>>= Byte.SIZE) != 0) {
                                    n = 7;
                                    b = (byte) val;
                                    if ((val >>>= Byte.SIZE) != 0) {
                                        n = 8;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return insertBytes(n, o, (byte) val, b, c, d, e, f, g, h);
    }

    /**
     * Retrieves an integer up to one byte in length. No leading zeroes allowed. The integer zero always has zero length.
     * Big-endian two's complement format.
     *
     * @see #toBytes(byte)
     * @see #putByte(byte, byte[], int)
     * @param buffer    the array containing the integer
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation
     * @return  the integer
     * @throws DecodeException  if the integer's representation is found to have leading zeroes
     */
    public static byte getByte(byte[] buffer, int i, int len) throws DecodeException {
        switch (len) {
        case 1:
            byte lead = buffer[i];
            if(lead == 0) {
                throw new UnrecoverableDecodeException("deserialised integers with leading zeroes are invalid; index: " + i + ", len: " + len);
            }
            return lead;
        case 0: return 0;
        default: throw new UnrecoverableDecodeException(new IllegalArgumentException("len is out of range: " + len));
        }
    }

    /**
     * Retrieves an integer up to two bytes in length. No leading zeroes allowed. The integer zero always has zero length.
     * Big-endian two's complement format.
     *
     * @see #toBytes(short)
     * @see #putShort(short, byte[], int)
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation, without leading zeroes
     * @return  the integer
     * @throws DecodeException  if the integer's representation is found to have leading zeroes
     */
    public static short getShort(byte[] buffer, int i, int len) throws DecodeException {
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 2 through 1 fall through */
        case 2: val = buffer[i+1] & 0xFF; shiftAmount = Byte.SIZE; // & 0xFF to promote to int before left shift
        case 1:
            byte lead = buffer[i];
            val |= (lead & 0xFF) << shiftAmount;
            if(lead == 0) {
                throw new UnrecoverableDecodeException("deserialised integers with leading zeroes are invalid; index: " + i + ", len: " + len);
            }
        case 0: return (short) val;
        default: throw new UnrecoverableDecodeException(new IllegalArgumentException("len is out of range: " + len));
        }
    }

    /**
     * Retrieves an integer up to four bytes in length. No leading zeroes allowed. The integer zero always has zero length.
     * Big-endian two's complement format.
     *
     * @see #toBytes(int)
     * @see #putInt(int, byte[], int)
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation, without leading zeroes
     * @return  the integer
     * @throws DecodeException  if the integer's representation is found to have leading zeroes
     */
    public static int getInt(byte[] buffer, int i, int len) throws DecodeException {
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[i];
            val |= (lead & 0xFF) << shiftAmount;
            if(lead == 0) {
                throw new UnrecoverableDecodeException("deserialised integers with leading zeroes are invalid; index: " + i + ", len: " + len);
            }
        case 0: return val;
        default: throw new UnrecoverableDecodeException(new IllegalArgumentException("len is out of range: " + len));
        }
    }

    /**
     * Retrieves an integer up to eight bytes in length. No leading zeroes allowed. The integer zero always has zero length.
     * Big-endian two's complement format.
     *
     * @see #toBytes(long)
     * @see #putLong(long, byte[], int)
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation, without leading zeroes
     * @return  the integer
     * @throws DecodeException  if the integer's representation is found to have leading zeroes
     */
    public static long getLong(final byte[] buffer, final int i, final int len) throws DecodeException {
        int shiftAmount = 0;
        long val = 0L;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val = buffer[i+7] & 0xFFL; shiftAmount = Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[i];
            val |= (lead & 0xFFL) << shiftAmount;
            if(lead == 0) {
                throw new UnrecoverableDecodeException("deserialised integers with leading zeroes are invalid; index: " + i + ", len: " + len);
            }
        case 0: return val;
        default: throw new UnrecoverableDecodeException(new IllegalArgumentException("len is out of range: " + len));
        }
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val   the integer
     * @return  the byte length
     */
    public static int len(byte val) {
        if(val == 0)
            return 0;
        return 1;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val   the integer
     * @return  the byte length
     */
    public static int len(short val) {
        int n = 0;
        if(val != 0) {
            n = 1;
//            val = (short) (val >>> Byte.SIZE); // ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT
            val = (short) (val >> Byte.SIZE); // high bytes chopped off either way, see above
            if (val != 0) {
                return 2;
            }
        }
        return n;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val   the integer
     * @return  the byte length
     */
    public static int len(int val) {
        int n = 0;
        if(val != 0) {
            n = 1;
            if ((val >>>= Byte.SIZE) != 0) {
                n = 2;
                if ((val >>>= Byte.SIZE) != 0) {
                    n = 3;
                    if (val >>> Byte.SIZE != 0) {
                        return 4;
                    }
                }
            }
        }
        return n;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val   the integer
     * @return  the byte length
     */
    public static int len(long val) {
        int n = 0;
        if(val != 0) {
            n = 1;
            if ((val >>>= Byte.SIZE) != 0) {
                n = 2;
                if ((val >>>= Byte.SIZE) != 0) {
                    n = 3;
                    if ((val >>>= Byte.SIZE) != 0) {
                        n = 4;
                        if ((val >>>= Byte.SIZE) != 0) {
                            n = 5;
                            if ((val >>>= Byte.SIZE) != 0) {
                                n = 6;
                                if ((val >>>= Byte.SIZE) != 0) {
                                    n = 7;
                                    if (val >>> Byte.SIZE != 0) {
                                        return 8;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return n;
    }

    /**
     * NOTE: will always return {@link Long#SIZE} for negative integers. See also {@link BizarroIntegers#bitLen(long)}.
     *
     * @param val   the long value
     * @return  the bit length of the input
     */
    public static int bitLen(long val) {
        return Long.SIZE - Long.numberOfLeadingZeros(val);
    }

    public static int insertBytes(int n, byte[] b, int i, byte w, byte x, byte y, byte z) {
        if(n > 4) {
            throw new IllegalArgumentException("n must be <= 4");
        }
        return insertBytes(n, b, i, (byte)0, (byte)0, (byte)0, (byte)0, w, x, y, z);
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

    public static int insertBytes(int n, ByteBuffer b, byte s, byte t, byte u, byte v, byte w, byte x, byte y, byte z) {
        switch (n) { /* cases fall through */
        case 8: b.put(s);
        case 7: b.put(t);
        case 6: b.put(u);
        case 5: b.put(v);
        case 4: b.put(w);
        case 3: b.put(x);
        case 2: b.put(y);
        case 1: b.put(z);
        case 0: return n;
        default: throw new IllegalArgumentException("n is out of range: " + n);
        }
    }

    public static BigInteger getBigInt(byte[] bytes, int i, final int len) {
        byte[] dest = new byte[len];
        System.arraycopy(bytes, i, dest, 0, len);
        return new BigInteger(dest);
    }

    public static int putBigInt(BigInteger val, byte[] o, int i) {
        byte[] bytes = val.toByteArray();
        final int len = bytes.length;
        System.arraycopy(bytes, 0, o, i, len);
        return len;
    }
}
