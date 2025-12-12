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
package com.esaulpaugh.headlong.rlp;

import java.util.Arrays;

/** Enumeration of the five RLP data types. */
public enum DataType {

    SINGLE_BYTE(0, true, false),
    STRING_SHORT(0x80, true, false),
    STRING_LONG(0xb7, true, true),
    LIST_SHORT(0xc0, false, false),
    LIST_LONG(0xf7, false, true);

    static final byte STRING_SHORT_OFFSET = (byte) 0x80;
    static final byte STRING_LONG_OFFSET = (byte) 0xb7;
    static final byte LIST_SHORT_OFFSET = (byte) 0xc0;
    static final byte LIST_LONG_OFFSET = (byte) 0xf7;

    static final int ORDINAL_SINGLE_BYTE = 0;
    static final int ORDINAL_STRING_SHORT = 1;
    static final int ORDINAL_STRING_LONG = 2;
    static final int ORDINAL_LIST_SHORT = 3;
    static final int ORDINAL_LIST_LONG = 4;

    public static final int MIN_LONG_DATA_LEN = 56;

    private static final DataType[] LOOKUP = new DataType[1 << Byte.SIZE];
    private static final int[] ORDINALS = new int[LOOKUP.length];

    static {
        Arrays.fill(LOOKUP, 0x00, 0x80, SINGLE_BYTE);
        Arrays.fill(LOOKUP, 0x80, 0xb8, STRING_SHORT);
        Arrays.fill(LOOKUP, 0xb8, 0xc0, STRING_LONG);
        Arrays.fill(LOOKUP, 0xc0, 0xf8, LIST_SHORT);
        Arrays.fill(LOOKUP, 0xf8, 0x100, LIST_LONG);
        Arrays.fill(ORDINALS, 0x00, 0x80, ORDINAL_SINGLE_BYTE);
        Arrays.fill(ORDINALS, 0x80, 0xb8, ORDINAL_STRING_SHORT);
        Arrays.fill(ORDINALS, 0xb8, 0xc0, ORDINAL_STRING_LONG);
        Arrays.fill(ORDINALS, 0xc0, 0xf8, ORDINAL_LIST_SHORT);
        Arrays.fill(ORDINALS, 0xf8, 0x100, ORDINAL_LIST_LONG);
    }

    public final byte offset;
    public final boolean isString;
    public final boolean isLong;

    DataType(int offset, boolean isString, boolean isLong) {
        this.offset = (byte) offset;
        this.isString = isString;
        this.isLong = isLong;
    }

    /**
     * @param leadByte the first (zeroth) byte of an RLP encoding
     * @return one of the five enumerated RLP data types
     */
    public static DataType type(final byte leadByte) {
        return LOOKUP[leadByte & 0xff];
    }

    public static int ordinal(byte leadByte) {
        return ORDINALS[leadByte & 0xff];
    }

    public static boolean isSingleByte(byte b) {
        return b >= 0x00; // same as (buffer[idx] & 0xFF) < 0x80
    }
}
