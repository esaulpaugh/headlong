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
package com.esaulpaugh.headlong.abi.util;

import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.nio.ByteBuffer;

/**
 * The mirror image of {@link Integers}. Not compatible with the RLP specification.
 *
 * Negative integers are stored in a minimal big-endian two's complement representation. Non-negative integers are
 * stored full-length. Negative one is represented by the empty byte array. Numbers are sign-extended on decode.
 *
 * -256L ≡ 0x00
 *  -16L ≡ 0xf0
 *   -1L ≡ 0x
 *    0L ≡ 0x0000000000000000
 *    1L ≡ 0x0000000000000001
 *   16L ≡ 0x0000000000000010
 */
public final class BizarroIntegers {

    public static byte[] toBytes(byte val) {
        return val != -1 ? new byte[] { val } : Strings.EMPTY_BYTE_ARRAY;
    }

    public static byte[] toBytes(short val) {
        byte[] bytes = new byte[len(val)];
        putShort(val, bytes, 0);
        return bytes;
    }

    public static byte[] toBytes(int val) {
        byte[] bytes = new byte[len(val)];
        putInt(val, bytes, 0);
        return bytes;
    }

    public static byte[] toBytes(long val) {
        byte[] bytes = new byte[len(val)];
        putLong(val, bytes, 0);
        return bytes;
    }

    public static int putByte(byte val, byte[] o, int i) {
        if(val != -1) {
            o[i] = val;
            return 1;
        }
        return 0;
    }

    public static int putShort(short val, byte[] o, int i) {
        int v = val;
        if (val != -1) {
            byte b = (byte) v;
            if ((v >>= Byte.SIZE) != -1) {
                o[i]=(byte)v; o[i+1]=b; return 2;
            } else o[i]=b; return 1;
        } else return 0;
    }

    public static int putInt(int val, byte[] o, int i) {
        if (val != -1) {
            byte d = (byte) val;
            if ((val >>= Byte.SIZE) != -1) {
                byte c = (byte) val;
                if ((val >>= Byte.SIZE) != -1) {
                    byte b = (byte) val;
                    if ((val >>= Byte.SIZE) != -1) {
                        o[i]=(byte) val; o[i+1]=b; o[i+2]=c; o[i+3]=d; return 4;
                    } else o[i]=b; o[i+1]=c; o[i+2]=d; return 3;
                } else o[i]=c; o[i+1]=d; return 2;
            } else o[i]=d; return 1;
        } else return 0;
    }

    public static int putLong(long val, byte[] o, int i) {
        if (val != -1) {
            byte h = (byte) val;
            if ((val >>= Byte.SIZE) != -1) {
                byte g = (byte) val;
                if ((val >>= Byte.SIZE) != -1) {
                    byte f = (byte) val;
                    if ((val >>= Byte.SIZE) != -1) {
                        byte e = (byte) val;
                        if ((val >>= Byte.SIZE) != -1) {
                            byte d = (byte) val;
                            if ((val >>= Byte.SIZE) != -1) {
                                byte c = (byte) val;
                                if ((val >>= Byte.SIZE) != -1) {
                                    byte b = (byte) val;
                                    if ((val >>= Byte.SIZE) != -1) {
                                        o[i]=(byte)val; o[i+1]=b; o[i+2]=c; o[i+3]=d; o[i+4]=e; o[i+5]=f; o[i+6]=g; o[i+7]=h; return 8;
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
        if (val != -1) {
            byte h = (byte) val;
            if ((val >>= Byte.SIZE) != -1) {
                byte g = (byte) val;
                if ((val >>= Byte.SIZE) != -1) {
                    byte f = (byte) val;
                    if ((val >>= Byte.SIZE) != -1) {
                        byte e = (byte) val;
                        if ((val >>= Byte.SIZE) != -1) {
                            byte d = (byte) val;
                            if ((val >>= Byte.SIZE) != -1) {
                                byte c = (byte) val;
                                if ((val >>= Byte.SIZE) != -1) {
                                    byte b = (byte) val;
                                    if ((val >>= Byte.SIZE) != -1) {
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
// ********* PRIVATE INTERNAL, NO RANGE CHECK **********
    private static int _getShortInt(byte[] buffer, int i) {
        return (buffer[i+1] & 0xFF) | ((buffer[i] & 0xFF) << Byte.SIZE);
    }

    private static int _getInt(byte[] buffer, int i, int len) {
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= (buffer[i] & 0xFF) << shiftAmount;
        default: return val;
        }
    }

    private static long _getLong(final byte[] buffer, final int i, final int len) {
        int shiftAmount = 0;
        long val = 0L;
        switch (len) { /* cases 8 through 1 fall through */
        case 8: val = buffer[i+7] & 0xFF; shiftAmount = Byte.SIZE;
        case 7: val |= (buffer[i+6] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 6: val |= (buffer[i+5] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 5: val |= (buffer[i+4] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 4: val |= (buffer[i+3] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFFL) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= (buffer[i] & 0xFFL) << shiftAmount;
        default: return val;
        }
    }
// *******************
    public static byte getByte(byte[] buffer, int index, int len) {
        switch (len) {
        case 0: return (byte) 0xFF;
        case 1: return buffer[index];
        default: throw outOfRangeException(len);
        }
    }

    public static short getShort(byte[] buffer, int index, int len) {
        // do sign extension for negative shorts, i.e. len < 2
        switch (len) {
        case 0: return (short) 0xFFFF;
        case 1: return (short) (0xFFFFFF00 | buffer[index]);
        case 2: return (short) _getShortInt(buffer, index);
        default: throw outOfRangeException(len);
        }
    }

    public static int getInt(byte[] buffer, int index, int len) {
        // do sign extension for negative ints, i.e. len < 4
        switch (len) {
        case 0: return 0xFFFFFFFF;
        case 1: return 0xFFFFFF00 | buffer[index];
        case 2: return 0xFFFF0000 | _getShortInt(buffer, index);
        case 3: return 0xFF000000 | _getInt(buffer, index, 3);
        case 4: return _getInt(buffer, index, 4);
        default: throw outOfRangeException(len);
        }
    }

    public static long getLong(final byte[] buffer, final int index, final int len) {
        // do sign extension for negative longs, i.e. len < 8
        switch (len) {
        case 0: return 0xFFFFFFFF_FFFFFFFFL;
        case 1: return 0xFFFFFFFF_FFFFFF00L | buffer[index];
        case 2: return 0xFFFFFFFF_FFFF0000L | _getShortInt(buffer, index);
        case 3: return 0xFFFFFFFF_FF000000L | _getInt(buffer, index, 3);
        case 4: return 0xFFFFFFFF_00000000L | _getInt(buffer, index, 4);
        case 5: return 0xFFFFFF00_00000000L | _getLong(buffer, index, 5);
        case 6: return 0xFFFF0000_00000000L | _getLong(buffer, index, 6);
        case 7: return 0xFF000000_00000000L | _getLong(buffer, index, 7);
        case 8: return _getLong(buffer, index, 8);
        default: throw outOfRangeException(len);
        }
    }

    private static IllegalArgumentException outOfRangeException(int len) {
        return new IllegalArgumentException("len is out of range: " + len);
    }

    public static int len(byte val) {
        return val != -1 ? 1 : 0;
    }

    public static int len(short val) {
        if(val != -1)
            if (val >> Byte.SIZE != -1)
                return 2;
            else return 1;
        return 0;
    }

    public static int len(int val) {
        if (val != -1)
            if ((val >>= Byte.SIZE) != -1)
                if ((val >>= Byte.SIZE) != -1)
                    if (val >> Byte.SIZE != -1)
                        return 4;
                    else return 3;
                else return 2;
            else return 1;
        return 0;
    }

    public static int len(long val) {
        if (val != -1)
            if ((val >>= Byte.SIZE) != -1)
                if ((val >>= Byte.SIZE) != -1)
                    if ((val >>= Byte.SIZE) != -1)
                        if ((val >>= Byte.SIZE) != -1)
                            if ((val >>= Byte.SIZE) != -1)
                                if ((val >>= Byte.SIZE) != -1)
                                    if (val >> Byte.SIZE != -1)
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

    /**
     * NOTE: will always return {@link Long#SIZE} for non-negative integers. See also {@link Integers#bitLen(long)}.
     *
     * @param val   the long value
     * @return  the bit length of the input
     */
    public static int bitLen(long val) {
        return Long.SIZE - Long.numberOfLeadingZeros(~val);
    }
}
