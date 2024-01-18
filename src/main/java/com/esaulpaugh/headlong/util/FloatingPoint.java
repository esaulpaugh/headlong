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
package com.esaulpaugh.headlong.util;

import com.esaulpaugh.headlong.util.Integers;

/** Utility for reading and writing floating point numbers from and to RLP format. */
public final class FloatingPoint {

    private FloatingPoint() {}

    /* float */

    public static float getFloat(byte[] bytes, int i, int len, boolean lenient) {
        return Float.intBitsToFloat(Integers.getInt(bytes, i, len, lenient));
    }

    public static byte[] toBytes(float val) {
        return Integers.toBytes(Float.floatToIntBits(val));
    }

    /* double */

    public static double getDouble(byte[] bytes, int i, int len, boolean lenient) {
        return Double.longBitsToDouble(Integers.getLong(bytes, i, len, lenient));
    }

    public static byte[] toBytes(double val) {
        return Integers.toBytes(Double.doubleToLongBits(val));
    }
}
