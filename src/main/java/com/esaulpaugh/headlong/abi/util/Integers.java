package com.esaulpaugh.headlong.abi.util;

public class Integers {

    /**
     * Retrieves an integer up to four bytes in length. Big-endian two's complement format.
     *
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation
     * @return  the integer
     */
    public static int getInt(byte[] buffer, int i, int len) {
        boolean negative = (buffer[i] & 0b1000000) != 0;
        int shiftAmount = 0;
        int val = 0;
        switch (len) { /* cases 4 through 1 fall through */
        case 4: val = buffer[i+3] & 0xFF; shiftAmount = Byte.SIZE;
        case 3: val |= (buffer[i+2] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 2: val |= (buffer[i+1] & 0xFF) << shiftAmount; shiftAmount += Byte.SIZE;
        case 1: val |= (buffer[i] & 0xFF) << shiftAmount;
        case 0: break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(negative) {
            // sign extend
            switch (len) {
            case 0: val |= 0xFF;
            case 1: val |= 0xFF << 8;
            case 2: val |= 0xFF << 16;
            case 3: val |= 0xFF << 24;
            }
        }
        return val;
    }

    /**
     * Retrieves an integer up to eight bytes in length. Big-endian two's complement format.
     *
     * @param buffer    the array containing the integer's representation
     * @param i the array index locating the integer
     * @param len  the length in bytes of the integer's representation
     * @return  the integer
     */
    public static long getLong(final byte[] buffer, final int i, final int len) {
        boolean negative = (buffer[i] & 0b1000000) != 0;
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
        case 1: val |= (buffer[i] & 0xFFL) << shiftAmount;
        case 0: break;
        default: throw new IllegalArgumentException("len out of range: " + len);
        }
        if(negative) {
            // sign extend
            switch (len) { /* cases fall through */
            case 0: val |= 0xFF;
            case 1: val |= 0xFF << 8;
            case 2: val |= 0xFF << 16;
            case 3: val |= 0xFF << 24;
            case 4: val |= 0xFFL << 32;
            case 5: val |= 0xFFL << 40;
            case 6: val |= 0xFFL << 48;
            case 7: val |= 0xFFL << 56;
            }
        }
        return val;
    }

}
