/*
   Copyright 2019 Evan Saulpaugh

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
        byte[] bytes = new byte[len(val)];
        putShort(val, bytes, 0);
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
        byte[] bytes = new byte[len(val)];
        putInt(val, bytes, 0);
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
        byte[] bytes = new byte[len(val)];
        putLong(val, bytes, 0);
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
     * @param val the integer to be inserted
     * @param o   the output array
     * @param i   the index into the output
     * @return the number of bytes inserted
     * @see #toBytes(short)
     * @see #getShort(byte[], int, int, boolean)
     */
    public static int putShort(short val, byte[] o, int i) {
        if(val != 0) {
            byte b = (byte) val;
//            val = (short) (val >>> Byte.SIZE); // ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT
            val = (short) (val >> Byte.SIZE); // high bytes chopped off either way, see above
            if (val != 0) {
                o[i]=(byte) val; o[i+1]=b; return 2;
            } else o[i]=b; return 1;
        } else return 0;
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
        if(val != 0) {
            byte d = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                byte c = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    byte b = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        o[i]=(byte) val; o[i+1]=b; o[i+2]=c; o[i+3]=d; return 4;
                    } else o[i]=b; o[i+1]=c; o[i+2]=d; return 3;
                } else o[i]=c; o[i+1]=d; return 2;
            } else o[i]=d; return 1;
        } else return 0;
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
        if(val != 0) {
            byte h = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                byte g = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    byte f = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        byte e = (byte) val;
                        if ((val >>>= Byte.SIZE) != 0) {
                            byte d = (byte) val;
                            if ((val >>>= Byte.SIZE) != 0) {
                                byte c = (byte) val;
                                if ((val >>>= Byte.SIZE) != 0) {
                                    byte b = (byte) val;
                                    if ((val >>>= Byte.SIZE) != 0) {
                                        o[i]=(byte) val; o[i+1]=b; o[i+2]=c; o[i+3]=d; o[i+4]=e; o[i+5]=f; o[i+6]=g; o[i+7]=h; return 8;
                                    } else o[i]=b; o[i+1]=c; o[i+2]=d; o[i+3]=e; o[i+4]=f; o[i+5]=g; o[i+6]=h; return 7;
                                } else o[i]=c; o[i+1]=d; o[i+2]=e; o[i+3]=f; o[i+4]=g; o[i+5]=h; return 6;
                            } else o[i]=d; o[i+1]=e; o[i+2]=f; o[i+3]=g; o[i+4]=h; return 5;
                        } else o[i]=e; o[i+1]=f; o[i+2]=g; o[i+3]=h; return 4;
                    } else o[i]=f; o[i+1]=g; o[i+2]=h; return 3;
                } else o[i]=g; o[i+1]=h; return 2;
            } else o[i]=h; return 1;
        } else return 0;
    }

    public static int putLong(long val, ByteBuffer o) {
        if(val != 0) {
            byte h = (byte) val;
            if ((val >>>= Byte.SIZE) != 0) {
                byte g = (byte) val;
                if ((val >>>= Byte.SIZE) != 0) {
                    byte f = (byte) val;
                    if ((val >>>= Byte.SIZE) != 0) {
                        byte e = (byte) val;
                        if ((val >>>= Byte.SIZE) != 0) {
                            byte d = (byte) val;
                            if ((val >>>= Byte.SIZE) != 0) {
                                byte c = (byte) val;
                                if ((val >>>= Byte.SIZE) != 0) {
                                    byte b = (byte) val;
                                    if ((val >>>= Byte.SIZE) != 0) {
                                        o.put((byte) val).put(b).put(c).put(d).put(e).put(f).put(g).put(h); return 8;
                                    } else o.put(b).put(c).put(d).put(e).put(f).put(g).put(h); return 7;
                                } else o.put(c).put(d).put(e).put(f).put(g).put(h); return 6;
                            } else o.put(d).put(e).put(f).put(g).put(h); return 5;
                        } else o.put(e).put(f).put(g).put(h); return 4;
                    } else o.put(f).put(g).put(h); return 3;
                } else o.put(g).put(h); return 2;
            } else o.put(h); return 1;
        } else return 0;
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
            if(!lenient && lead == 0) {
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
        switch (len) { /* cases 2 through 1 fall through */
        case 2: val = buffer[offset+1] & 0xFF; shiftAmount = Byte.SIZE; // & 0xFF to promote to int before left shift
        case 1:
            byte lead = buffer[offset];
            if(!lenient && lead == 0 && len > 1) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFFL) << shiftAmount;
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
            if (!lenient && lead == 0 && len > 1) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFFL) << shiftAmount;
        case 0: return val;
        default: throw outOfRangeException(len);
        }
    }

    /**
     * Retrieves an integer up to eight bytes in length. Big-endian two's complement format.
     *
     * @param buffer  the array containing the integer's representation
     * @param offset  the array index locating the integer
     * @param len     the length in bytes of the integer's representation, without leading zeroes
     * @param lenient whether to allow leading zeroes
     * @return the integer
     * @throws IllegalArgumentException if the integer's representation is found to have leading zeroes
     * @see #toBytes(long)
     * @see #putLong(long, byte[], int)
     */
    public static long getLong(final byte[] buffer, final int offset, final int len, boolean lenient) {
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
            if (!lenient && lead == 0 && len > 1) {
                throw leadingZeroException(offset, len);
            }
            val |= (lead & 0xFFL) << shiftAmount;
        case 0: return val;
        default: throw outOfRangeException(len);
        }
    }

    private static IllegalArgumentException leadingZeroException(int idx, int len) {
        return new IllegalArgumentException("deserialised positive integers with leading zeroes are invalid; index: " + idx + ", len: " + len);
    }

    private static IllegalArgumentException outOfRangeException(int len) {
        return new IllegalArgumentException("len is out of range: " + len);
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the byte length
     */
    public static int len(byte val) {
        return val != 0 ? 1 : 0;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the byte length
     */
    public static int len(short val) {
        if(val != 0)
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
     * @return the byte length
     */
    public static int len(int val) {
        if (val != 0)
            if ((val >>>= Byte.SIZE) != 0)
                if ((val >>>= Byte.SIZE) != 0)
                    if (val >>> Byte.SIZE != 0)
                        return 4;
                    else return 3;
                else return 2;
            else return 1;
        return 0;
    }

    /**
     * Returns the byte length of an integer's minimal (without leading zeroes) two's complement representation. The
     * integer zero always has zero length.
     *
     * @param val the integer
     * @return the byte length
     */
    public static int len(long val) {
        if (val != 0)
            if ((val >>>= Byte.SIZE) != 0)
                if ((val >>>= Byte.SIZE) != 0)
                    if ((val >>>= Byte.SIZE) != 0)
                        if ((val >>>= Byte.SIZE) != 0)
                            if ((val >>>= Byte.SIZE) != 0)
                                if ((val >>>= Byte.SIZE) != 0)
                                    if (val >>> Byte.SIZE != 0)
                                        return 8;
                                    else return 7;
                                else return 6;
                            else return 5;
                        else return 4;
                    else return 3;
                else return 2;
            else return 1;
        return 0;
    }

    public static int len(BigInteger val) {
        return roundLengthUp(val.bitLength(), Byte.SIZE) >> 3; // div 8
    }

    /**
     * NOTE: will always return {@link Long#SIZE} for negative integers. See also abi.util.BizarroIntegers.bitLen(long).
     *
     * @param val the long value
     * @return the bit length of the input
     */
    public static int bitLen(long val) {
        return Long.SIZE - Long.numberOfLeadingZeros(val);
    }

    public static BigInteger getBigInt(byte[] buffer, int offset, int len, boolean lenient) {
        if(!lenient && len > 0 && buffer[offset] == 0x00) {
            throw leadingZeroException(offset, len);
        }
        return new BigInteger("00" + Strings.encode(buffer, offset, len, Strings.HEX), 16);
    }

    public static int putBigInt(BigInteger val, byte[] dest, int destIdx) {
        byte[] bytes = val.toByteArray();
        int srcPos = 0;
        int len = bytes.length;
        if(bytes[0] == 0x00) {
            srcPos++;
            len--;
        }
        System.arraycopy(bytes, srcPos, dest, destIdx, len);
        return len;
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
        int mod = len & (powerOfTwo - 1);
        return mod != 0 ? len + (powerOfTwo - mod) : len;
    }

    public static void checkIsMultiple(int len, int powerOfTwo) {
        if((len & (powerOfTwo - 1)) != 0) {
            throw new IllegalArgumentException("expected length mod " + powerOfTwo + " == 0, found: " + (len % powerOfTwo));
        }
    }

    public static void putN(byte val, int n, ByteBuffer dest) {
        for (int i = 0; i < n; i++) {
            dest.put(val);
        }
    }
}
