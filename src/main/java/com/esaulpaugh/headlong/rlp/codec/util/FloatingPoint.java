package com.esaulpaugh.headlong.rlp.codec.util;

import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;

import java.math.BigDecimal;

public class FloatingPoint {

    /* float */

    public static float getFloat(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Float.intBitsToFloat(Integers.getInt(bytes, i, numBytes));
    }

    public static int putFloat(float val, byte[] bytes, int i) {
        return Integers.put(Float.floatToIntBits(val), bytes, i);
    }

    public static byte[] toBytes(float val) {
        return Integers.toBytes(Float.floatToIntBits(val));
    }

    /* double */

    public static double getDouble(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Double.longBitsToDouble(Integers.getLong(bytes, i, numBytes));
    }

    public static byte[] toBytes(double val) {
        return Integers.toBytes(Double.doubleToLongBits(val));
    }

    public static int putDouble(double val, byte[] bytes, int i) {
        return Integers.put(Double.doubleToLongBits(val), bytes, i);
    }

    /* BigDecimal */

    public BigDecimal getBigDecimal(byte[] bytes, int i, int unscaledNumBytes, int scale) {
        return new BigDecimal(Integers.getBigInteger(bytes, i, unscaledNumBytes), scale);
    }
}
