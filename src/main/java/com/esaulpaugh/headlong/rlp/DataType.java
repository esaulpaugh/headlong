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
        switch (leadByte) {
        case (byte) 0x80: case (byte) 0x81: case (byte) 0x82: case (byte) 0x83: case (byte) 0x84: case (byte) 0x85: case (byte) 0x86: case (byte) 0x87:
        case (byte) 0x88: case (byte) 0x89: case (byte) 0x8A: case (byte) 0x8B: case (byte) 0x8C: case (byte) 0x8D: case (byte) 0x8E: case (byte) 0x8F:
        case (byte) 0x90: case (byte) 0x91: case (byte) 0x92: case (byte) 0x93: case (byte) 0x94: case (byte) 0x95: case (byte) 0x96: case (byte) 0x97:
        case (byte) 0x98: case (byte) 0x99: case (byte) 0x9A: case (byte) 0x9B: case (byte) 0x9C: case (byte) 0x9D: case (byte) 0x9E: case (byte) 0x9F:
        case (byte) 0xA0: case (byte) 0xA1: case (byte) 0xA2: case (byte) 0xA3: case (byte) 0xA4: case (byte) 0xA5: case (byte) 0xA6: case (byte) 0xA7:
        case (byte) 0xA8: case (byte) 0xA9: case (byte) 0xAA: case (byte) 0xAB: case (byte) 0xAC: case (byte) 0xAD: case (byte) 0xAE: case (byte) 0xAF:
        case (byte) 0xB0: case (byte) 0xB1: case (byte) 0xB2: case (byte) 0xB3: case (byte) 0xB4: case (byte) 0xB5: case (byte) 0xB6: case (byte) 0xB7: return STRING_SHORT;

        case (byte) 0xB8: case (byte) 0xB9: case (byte) 0xBA: case (byte) 0xBB: case (byte) 0xBC: case (byte) 0xBD: case (byte) 0xBE: case (byte) 0xBF: return STRING_LONG;

        case (byte) 0xC0: case (byte) 0xC1: case (byte) 0xC2: case (byte) 0xC3: case (byte) 0xC4: case (byte) 0xC5: case (byte) 0xC6: case (byte) 0xC7:
        case (byte) 0xC8: case (byte) 0xC9: case (byte) 0xCA: case (byte) 0xCB: case (byte) 0xCC: case (byte) 0xCD: case (byte) 0xCE: case (byte) 0xCF:
        case (byte) 0xD0: case (byte) 0xD1: case (byte) 0xD2: case (byte) 0xD3: case (byte) 0xD4: case (byte) 0xD5: case (byte) 0xD6: case (byte) 0xD7:
        case (byte) 0xD8: case (byte) 0xD9: case (byte) 0xDA: case (byte) 0xDB: case (byte) 0xDC: case (byte) 0xDD: case (byte) 0xDE: case (byte) 0xDF:
        case (byte) 0xE0: case (byte) 0xE1: case (byte) 0xE2: case (byte) 0xE3: case (byte) 0xE4: case (byte) 0xE5: case (byte) 0xE6: case (byte) 0xE7:
        case (byte) 0xE8: case (byte) 0xE9: case (byte) 0xEA: case (byte) 0xEB: case (byte) 0xEC: case (byte) 0xED: case (byte) 0xEE: case (byte) 0xEF:
        case (byte) 0xF0: case (byte) 0xF1: case (byte) 0xF2: case (byte) 0xF3: case (byte) 0xF4: case (byte) 0xF5: case (byte) 0xF6: case (byte) 0xF7: return LIST_SHORT;

        case (byte) 0xF8: case (byte) 0xF9: case (byte) 0xFA: case (byte) 0xFB: case (byte) 0xFC: case (byte) 0xFD: case (byte) 0xFE: case (byte) 0xFF: return LIST_LONG;

        default: // 0x00 - 0x7F
            return SINGLE_BYTE;
        }
    }
}
