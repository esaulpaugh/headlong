package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.DecodeException;

import java.math.BigDecimal;

public class FloatingPoint {

    /* float */

    public static float getFloat(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Float.intBitsToFloat(RLPIntegers.getInt(bytes, i, numBytes));
    }

    public static int putFloat(float val, byte[] bytes, int i) {
        return RLPIntegers.putLong(Float.floatToIntBits(val), bytes, i);
    }

    public static byte[] toBytes(float val) {
        return RLPIntegers.toBytes(Float.floatToIntBits(val));
    }

    /* double */

    public static double getDouble(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Double.longBitsToDouble(RLPIntegers.getLong(bytes, i, numBytes));
    }

    public static byte[] toBytes(double val) {
        return RLPIntegers.toBytes(Double.doubleToLongBits(val));
    }

    public static int putDouble(double val, byte[] bytes, int i) {
        return RLPIntegers.putLong(Double.doubleToLongBits(val), bytes, i);
    }

    /* BigDecimal */

    public BigDecimal getBigDecimal(byte[] bytes, int i, int unscaledNumBytes, int scale) {
        return new BigDecimal(RLPIntegers.getBigInt(bytes, i, unscaledNumBytes), scale);
    }
}
