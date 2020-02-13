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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

final class Encoding {

    static final int OFFSET_LENGTH_BYTES = UNIT_LENGTH_BYTES;
    static final IntType OFFSET_TYPE = new IntType("int32", Integer.SIZE, false); // uint256

    static final byte NEGATIVE_ONE_BYTE = (byte) 0xFF;
    static final byte ZERO_BYTE = (byte) 0x00;
    static final byte ONE_BYTE = (byte) 0x01;

    private static final byte[] NON_NEGATIVE_INT_PADDING = new byte[UNIT_LENGTH_BYTES - Long.BYTES];
    private static final byte[] NEGATIVE_INT_PADDING = new byte[UNIT_LENGTH_BYTES - Long.BYTES];

    static {
        Arrays.fill(NEGATIVE_INT_PADDING, NEGATIVE_ONE_BYTE);
    }

    static int insertOffset(final int offset, ByteBuffer dest, int tailByteLen) {
        insertInt(offset, dest);
        return offset + tailByteLen; // return next offset
    }

    static void insertInt(long val, ByteBuffer dest) {
        dest.put(val >= 0 ? NON_NEGATIVE_INT_PADDING : NEGATIVE_INT_PADDING);
        dest.putLong(val);
    }

    static void insertInt(BigInteger signed, int byteLen, ByteBuffer dest) {
        byte[] arr = signed.toByteArray();
        putN(signed.signum() >= 0 ? ZERO_BYTE : NEGATIVE_ONE_BYTE, byteLen - arr.length, dest);
        dest.put(arr);
    }

    static void insertBigIntegers(BigInteger[] arr, int byteLen, ByteBuffer dest) {
        for (BigInteger e : arr) {
            insertInt(e, byteLen, dest);
        }
    }

    static void insertBigDecimals(BigDecimal[] arr, int byteLen, ByteBuffer dest) {
        for (BigDecimal e : arr) {
            insertInt(e.unscaledValue(), byteLen, dest);
        }
    }

    static void putN(byte val, int n, ByteBuffer dest) {
        for (int i = 0; i < n; i++) {
            dest.put(val);
        }
    }
}
