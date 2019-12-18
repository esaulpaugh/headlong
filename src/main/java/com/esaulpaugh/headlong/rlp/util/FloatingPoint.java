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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.util.Integers;

import java.math.BigDecimal;

/**
 * Utility for reading and writing floating point numbers from and to RLP format.
 */
public final class FloatingPoint {

    /* float */

    public static float getFloat(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Float.intBitsToFloat(Integers.getInt(bytes, i, numBytes));
    }

    public static int putFloat(float val, byte[] bytes, int i) {
        return Integers.putLong(Float.floatToIntBits(val), bytes, i);
    }

    public static byte[] toBytes(float val) {
        return Integers.toBytes(Float.floatToIntBits(val));
    }

    /* double */

    public static double getDouble(byte[] bytes, int i, int numBytes) throws DecodeException {
        return Double.longBitsToDouble(Integers.getLong(bytes, i, numBytes));
    }

    public static int putDouble(double val, byte[] bytes, int i) {
        return Integers.putLong(Double.doubleToLongBits(val), bytes, i);
    }

    public static byte[] toBytes(double val) {
        return Integers.toBytes(Double.doubleToLongBits(val));
    }

    /* BigDecimal */

    public static BigDecimal getBigDecimal(byte[] bytes, int i, int unscaledNumBytes, int scale) {
        return new BigDecimal(Integers.getBigInt(bytes, i, unscaledNumBytes), scale);
    }
}
