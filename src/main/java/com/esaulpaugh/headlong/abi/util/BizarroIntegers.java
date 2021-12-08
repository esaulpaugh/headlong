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

import java.nio.ByteBuffer;

/**
 * <p>The mirror image of {@link Integers}. Not compatible with the RLP specification.
 *
 * <p>Negative integers are stored in a minimal big-endian two's complement representation. Non-negative integers are
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

    private BizarroIntegers() {}

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
     * NOTE: will always return {@link Long#SIZE} for non-negative integers.
     *
     * @param val the long value
     * @return the bit length of the input
     * @see Integers#bitLen(long)
     */
    public static int bitLen(long val) {
        return Long.SIZE - Long.numberOfLeadingZeros(~val);
    }
}
