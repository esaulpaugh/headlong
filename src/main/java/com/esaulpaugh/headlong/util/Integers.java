/*
   Copyright 2019-2026 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/** Utility for reading and writing integers from and to RLP-compatible format. */
public final class Integers {

    private Integers() {}

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val the integer
     * @return the minimal representation
     */
    public static byte[] toBytes(byte val) {
        return val != 0 ? new byte[] { val } : Strings.EMPTY_BYTE_ARRAY;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val the integer
     * @return the minimal representation
     */
    public static byte[] toBytes(short val) {
        final int len = len(val);
        byte[] bytes = new byte[len];
        putLong(val, len, bytes, 0);
        return bytes;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val the integer
     * @return the minimal representation
     */
    public static byte[] toBytes(int val) {
        final int len = len(val);
        byte[] bytes = new byte[len];
        putLong(val, len, bytes, 0);
        return bytes;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val the integer
     * @return the minimal representation
     */
    public static byte[] toBytes(long val) {
        final int len = len(val);
        byte[] bytes = new byte[len];
        putLong(val, len, bytes, 0);
        return bytes;
    }

    /**
     * Returns an integer's minimal big-endian two's complement representation. The integer zero is represented by the
     * empty byte array.
     *
     * @param val the integer
     * @return the minimal representation
     */
    public static byte[] toBytesUnsigned(BigInteger val) {
//        if (val.signum() < 0) throw new IllegalArgumentException("signed value given for unsigned encoding");
        byte[] bytes = new byte[len(val)];
        putBigInt(val, bytes, 0);
        return bytes;
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to one byte in length. The integer zero always has length zero.
     *
     * @param val the integer to be inserted
     * @param o   the output array
     * @param i   the index into the output
     * @return the number of bytes inserted
     * @see #toBytes(byte)
     * @see #getByte(byte[], int, int, boolean)
     */
    public static int putByte(byte val, byte[] o, int i) {
        if (val != 0) {
            o[i] = val;
            return 1;
        }
        return 0;
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to two bytes in length. The integer zero always has length zero.
     *
     * @param val the integer to be inserted
     * @param o   the output array
     * @param i   the index into the output
     * @return the number of bytes inserted
     * @see #toBytes(short)
     * @see #getShort(byte[], int, int, boolean)
     */
    public static int putShort(short val, byte[] o, int i) {
        return putLong(val, len(val), o, i);
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to four bytes in length. The integer zero always has length zero.
     *
     * @param val the integer to be inserted
     * @param o   the output array
     * @param i   the index into the output
     * @return the number of bytes inserted
     * @see #toBytes(int)
     * @see #getInt(byte[], int, int, boolean)
     */
    public static int putInt(int val, byte[] o, int i) {
        return putLong(val, len(val), o, i);
    }

    /**
     * Inserts into a byte array an integer's minimal (without leading zeroes), big-endian two's complement representation,
     * up to eight bytes in length. The integer zero always has length zero.
     *
     * @param val the integer to be inserted
     * @param o   the output array
     * @param i   the index into the output
     * @return the number of bytes inserted
     * @see #toBytes(long)
     * @see #getLong(byte[], int, int, boolean)
     */
    public static int putLong(long val, byte[] o, int i) {
        return putLong(val, len(val), o, i);
    }

    public static int putLong(long val, ByteBuffer o) {
        return putLong(val, len(val), o);
    }

    public static int putLong(long val, int len, byte[] o, int i) {
        switch (len) { /* cases 8 through 1 fall through */
        case 8: o[i++] = (byte) (val >>> 56);
        case 7: o[i++] = (byte) (val >>> 48);
        case 6: o[i++] = (byte) (val >>> 40);
        case 5: o[i++] = (byte) (val >>> 32);
        case 4: o[i++] = (byte) (val >>> 24);
        case 3: o[i++] = (byte) (val >>> 16);
        case 2: o[i++] = (byte) (val >>> 8);
        case 1: o[i  ] = (byte) val;
        case 0: return len;
        default: throw outOfRangeException(len);
        }
    }

    public static int putLong(long val, int len, ByteBuffer o) {
        switch (len) { /* cases 8 through 1 fall through */
        case 8: o.put((byte) (val >>> 56));
        case 7: o.put((byte) (val >>> 48));
        case 6: o.put((byte) (val >>> 40));
        case 5: o.put((byte) (val >>> 32));
        case 4: o.put((byte) (val >>> 24));
        case 3: o.put((byte) (val >>> 16));
        case 2: o.put((byte) (val >>> 8));
        case 1: o.put((byte) val);
        case 0: return len;
        default: throw outOfRangeException(len);
        }
    }

    /**
     * Retrieves an integer up to one byte in length. Big-endian two's complement format.
     *
     * @param buffer  the array containing the integer
     * @param offset  the array index locating the integer
     * @param len     the length in bytes of the integer's representation
     * @param lenient whether to allow leading zeroes
     * @return the integer
     * @throws IllegalArgumentException if {@code lenient} is false and the integer's representation is found to have leading zeroes
     * @see #toBytes(byte)
     * @see #putByte(byte, byte[], int)
     */
    public static byte getByte(byte[] buffer, int offset, int len, boolean lenient) {
        switch (len) {
        case 1:
            byte lead = buffer[offset];
            if (!lenient && lead == 0) {
                throw leadingZeroException(offset, len);
            }
            return lead;
        case 0: return 0;
        default: throw outOfRangeException(len);
        }
    }

    /**
     * Retrieves an integer up to two bytes in length. Big-endian two's complement format.
     *
     * @param buffer the array containing the integer's representation
     * @param offset      the array index locating the integer
     * @param len    the length in bytes of the integer's representation
     * @param lenient whether to allow leading zeroes
     * @return the integer
     * @throws IllegalArgumentException if {@code lenient} is false and the integer's representation is found to have leading zeroes
     * @see #toBytes(short)
     * @see #putShort(short, byte[], int)
     */
    public static short getShort(byte[] buffer, int offset, int len, boolean lenient) {
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 2 and 1 fall through */
        case 2: val = buffer[offset+1] & 0xFF; shiftAmount = Byte.SIZE;
        case 1:
            byte lead = buffer[offset];
            if (!lenient && lead == 0) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFF) << shiftAmount;
        case 0: return (short) val;
        default: throw outOfRangeException(len);
        }
    }

    /**
     * Retrieves an integer up to four bytes in length. Big-endian two's complement format.
     *
     * @param buffer  the array containing the integer's representation
     * @param offset  the array index locating the integer
     * @param len     the length in bytes of the integer's representation
     * @param lenient whether to allow leading zeroes
     * @return the integer
     * @throws IllegalArgumentException if {@code lenient} is false and the integer's representation is found to have leading zeroes
     * @see #toBytes(int)
     * @see #putInt(int, byte[], int)
     */
    public static int getInt(byte[] buffer, int offset, int len, boolean lenient) {
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val  =  buffer[offset+3] & 0xFF;                 shiftAmount  = Byte.SIZE;
        case 3: val |= (buffer[offset+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[offset+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[offset];
            if (!lenient && lead == 0) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFF) << shiftAmount;
        case 0: return val;
        default: throw outOfRangeException(len);
        }
    }

    /**
     * Retrieves an integer up to eight bytes in length. Big-endian two's complement format.
     *
     * @param buffer  the array containing the integer's representation
     * @param offset  the array index locating the integer
     * @param len     the length in bytes of the integer's representation
     * @param lenient whether to allow leading zeroes
     * @return the integer
     * @throws IllegalArgumentException if the integer's representation is found to have leading zeroes
     * @see #toBytes(long)
     * @see #putLong(long, byte[], int)
     */
    public static long getLong(final byte[] buffer, final int offset, final int len, final boolean lenient) {
        int shiftAmount = 0;
        long val = 0L;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val  =  buffer[offset+7] & 0xFFL;                 shiftAmount  = Byte.SIZE;
        case 7: val |= (buffer[offset+6] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[offset+5] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[offset+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[offset+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[offset+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[offset+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1:
            byte lead = buffer[offset];
            if (!lenient && lead == 0) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFFL) << shiftAmount;
        case 0: return val;
        default: throw outOfRangeException(len);
        }
    }

    private static IllegalArgumentException leadingZeroException(int idx, int len) {
        return new IllegalArgumentException("deserialized integers with leading zeroes are invalid; index: " + idx + ", len: " + len);
    }

    private static IllegalArgumentException outOfRangeException(int len) {
        return new IllegalArgumentException("len is out of range: " + len);
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the length in bytes of the argument's encoding
     */
    public static int len(byte val) {
        return val != 0 ? 1 : 0;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the length in bytes of the argument's encoding
     */
    public static int len(short val) {
        if (val != 0)
            if (val >> Byte.SIZE != 0)
                return 2;
            else return 1;
        return 0;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the length in bytes of the argument's encoding
     */
    public static int len(int val) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(val) + 7 >> 3;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the length in bytes of the argument's encoding
     */
    public static int len(long val) {
        return bitLen(val) + 7 >> 3;
    }

    public static int len(BigInteger val) {
        return val.bitLength() + 7 >> 3; // roundLengthUp(val.bitLength(), Byte.SIZE) / Byte.SIZE;
    }

    /**
     * NOTE: will always return {@link Long#SIZE} for negative integers.
     *
     * @param val the long value
     * @return the bit length of the input
     */
    public static int bitLen(long val) {
        return Long.SIZE - Long.numberOfLeadingZeros(val);
    }

    public static BigInteger getBigInt(byte[] buffer, int offset, int len, boolean lenient) {
        if (len != 0) {
            if (!lenient && buffer[offset] == 0x00) {
                throw leadingZeroException(offset, len);
            }
            byte[] arr = new byte[len];
            System.arraycopy(buffer, offset, arr, 0, len);
//            return new BigInteger(1, buffer, offset, len); // Java 9+
            return new BigInteger(1, arr);
        }
        return BigInteger.ZERO;
    }

    public static int putBigInt(BigInteger val, byte[] dest, int destIdx) {
        final byte[] bytes = val.toByteArray();
        int len = bytes.length;
        if (bytes[0] == 0x00) {
            len--;
            System.arraycopy(bytes, 1, dest, destIdx, len);
        } else {
            System.arraycopy(bytes, 0, dest, destIdx, len);
        }
        return len;
    }

    public static int mod(int val, int powerOfTwo) {
        return val & (powerOfTwo - 1);
    }

    /**
     * Rounds a length up to the nearest multiple of {@code powerOfTwo}. If {@code len} is already a multiple, method has
     * no effect.
     *
     * @param len the length, a non-negative integer
     * @param powerOfTwo a power of two of which the result will be a multiple
     * @return the rounded-up value
     */
    public static int roundLengthUp(int len, int powerOfTwo) {
        return -powerOfTwo & (len + (powerOfTwo - 1));
    }

    public static boolean isMultiple(int val, int powerOfTwo) {
        return mod(val, powerOfTwo) == 0;
    }

    public static void checkIsMultiple(int val, int powerOfTwo) {
        if (!isMultiple(val, powerOfTwo)) {
            throw new IllegalArgumentException("expected length mod " + powerOfTwo + " == 0, found: " + (val % powerOfTwo));
        }
    }
}
