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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.Integers;

import java.nio.ByteBuffer;

/** Represents a long integer type such as int40, int64, uint32, or uint56. */
public final class LongType extends UnitType<Long> {

    static int init() {return 0;}

    static {
        UnitType.ensureInitialized();
    }

    LongType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, Long.class, bitLength, unsigned);
    }

    @Override
    Class<?> arrayClass() {
        return long[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_LONG;
    }

    @Override
    Long decode(ByteBuffer bb, byte[] unitBuffer) {
        return unsigned
                    ? decodeUnsignedLong(bb)
                    : decodeSignedLong(bb);
    }

    static void encodeLong(long value, int byteLen, ByteBuffer dest) {
        if (value >= 0) {
            insert00Padding(byteLen - Integers.len(value), dest);
            Integers.putLong(value, dest);
        } else {
            insertFFPadding(byteLen - lenNegative(value), dest);
            putLongNegative(value, dest);
        }
    }

    private static int lenNegative(long val) {
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

    private static void putLongNegative(long val, ByteBuffer o) {
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
                                        o.put((byte) val).put(b).put(c).put(d).put(e).put(f).put(g).put(h);
                                    } else o.put(b).put(c).put(d).put(e).put(f).put(g).put(h);
                                } else o.put(c).put(d).put(e).put(f).put(g).put(h);
                            } else o.put(d).put(e).put(f).put(g).put(h);
                        } else o.put(e).put(f).put(g).put(h);
                    } else o.put(f).put(g).put(h);
                } else o.put(g).put(h);
            } else o.put(h);
        }
    }
}
