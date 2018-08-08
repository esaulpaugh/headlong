package com.esaulpaugh.headlong.rlp;

public enum DataType {

    SINGLE_BYTE(0),
    STRING_SHORT(0x80),
    STRING_LONG(0xb7),
    LIST_SHORT(0xc0),
    LIST_LONG(0xf7);

    static final byte SINGLE_BYTE_OFFSET = 0;
    static final byte STRING_SHORT_OFFSET = (byte) 0x80;
    static final byte STRING_LONG_OFFSET = (byte) 0xb7;
    static final byte LIST_SHORT_OFFSET = (byte) 0xc0;
    static final byte LIST_LONG_OFFSET = (byte) 0xf7;


    //    public static final int MAX_SHORT_DATA_LEN = 55;
    public static final int MIN_LONG_DATA_LEN = 56;

    public final byte offset;

    DataType(int offset) {
        this.offset = (byte) offset;
    }

    /**
     *
     * @param leadByte
     * @return
     */
    public static DataType type(byte leadByte) {

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

//        case (byte) 0x00: case (byte) 0x01: case (byte) 0x02: case (byte) 0x03: case (byte) 0x04: case (byte) 0x05: case (byte) 0x06: case (byte) 0x07:
//        case (byte) 0x08: case (byte) 0x09: case (byte) 0x0A: case (byte) 0x0B: case (byte) 0x0C: case (byte) 0x0D: case (byte) 0x0E: case (byte) 0x0F:
//        case (byte) 0x10: case (byte) 0x11: case (byte) 0x12: case (byte) 0x13: case (byte) 0x14: case (byte) 0x15: case (byte) 0x16: case (byte) 0x17:
//        case (byte) 0x18: case (byte) 0x19: case (byte) 0x1A: case (byte) 0x1B: case (byte) 0x1C: case (byte) 0x1D: case (byte) 0x1E: case (byte) 0x1F:
//        case (byte) 0x20: case (byte) 0x21: case (byte) 0x22: case (byte) 0x23: case (byte) 0x24: case (byte) 0x25: case (byte) 0x26: case (byte) 0x27:
//        case (byte) 0x28: case (byte) 0x29: case (byte) 0x2A: case (byte) 0x2B: case (byte) 0x2C: case (byte) 0x2D: case (byte) 0x2E: case (byte) 0x2F:
//        case (byte) 0x30: case (byte) 0x31: case (byte) 0x32: case (byte) 0x33: case (byte) 0x34: case (byte) 0x35: case (byte) 0x36: case (byte) 0x37:
//        case (byte) 0x38: case (byte) 0x39: case (byte) 0x3A: case (byte) 0x3B: case (byte) 0x3C: case (byte) 0x3D: case (byte) 0x3E: case (byte) 0x3F:
//        case (byte) 0x40: case (byte) 0x41: case (byte) 0x42: case (byte) 0x43: case (byte) 0x44: case (byte) 0x45: case (byte) 0x46: case (byte) 0x47:
//        case (byte) 0x48: case (byte) 0x49: case (byte) 0x4A: case (byte) 0x4B: case (byte) 0x4C: case (byte) 0x4D: case (byte) 0x4E: case (byte) 0x4F:
//        case (byte) 0x50: case (byte) 0x51: case (byte) 0x52: case (byte) 0x53: case (byte) 0x54: case (byte) 0x55: case (byte) 0x56: case (byte) 0x57:
//        case (byte) 0x58: case (byte) 0x59: case (byte) 0x5A: case (byte) 0x5B: case (byte) 0x5C: case (byte) 0x5D: case (byte) 0x5E: case (byte) 0x5F:
//        case (byte) 0x60: case (byte) 0x61: case (byte) 0x62: case (byte) 0x63: case (byte) 0x64: case (byte) 0x65: case (byte) 0x66: case (byte) 0x67:
//        case (byte) 0x68: case (byte) 0x69: case (byte) 0x6A: case (byte) 0x6B: case (byte) 0x6C: case (byte) 0x6D: case (byte) 0x6E: case (byte) 0x6F:
//        case (byte) 0x70: case (byte) 0x71: case (byte) 0x72: case (byte) 0x73: case (byte) 0x74: case (byte) 0x75: case (byte) 0x76: case (byte) 0x77:
//        case (byte) 0x78: case (byte) 0x79: case (byte) 0x7A: case (byte) 0x7B: case (byte) 0x7C: case (byte) 0x7D: case (byte) 0x7E: case (byte) 0x7F:
        default:
            return SINGLE_BYTE;
        }
    }
}
