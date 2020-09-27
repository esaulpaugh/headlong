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

import com.esaulpaugh.headlong.util.Integers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

final class Encoding {

    static final int OFFSET_LENGTH_BYTES = UNIT_LENGTH_BYTES;

    static final IntType UINT17 = new IntType("uint17", 17, true); // small bit length for Denial-of-Service protection

    static final IntType UINT31 = new IntType("uint31", 31, true);

    private static final byte NEGATIVE_ONE_BYTE = (byte) 0xFF;
    static final byte ZERO_BYTE = (byte) 0x00;
    static final byte ONE_BYTE = (byte) 0x01;

    private static final byte[] CACHED_ZERO_PADDING = new byte[UNIT_LENGTH_BYTES];
    private static final byte[] CACHED_NEG1_PADDING = new byte[UNIT_LENGTH_BYTES];

    static {
        Arrays.fill(CACHED_NEG1_PADDING, NEGATIVE_ONE_BYTE);
    }

    private static final byte[] NON_NEGATIVE_INT_PADDING = new byte[UNIT_LENGTH_BYTES - Long.BYTES];
    private static final byte[] NEGATIVE_INT_PADDING = Arrays.copyOfRange(CACHED_NEG1_PADDING, 0, UNIT_LENGTH_BYTES - Long.BYTES);

    static int insertOffset(final int offset, ByteBuffer dest, int tailByteLen) {
        insertInt(offset, dest);
        return offset + tailByteLen; // return next offset
    }

    static void insertInt(long val, ByteBuffer dest) {
        dest.put(val >= 0 ? NON_NEGATIVE_INT_PADDING : NEGATIVE_INT_PADDING);
        dest.putLong(val);
    }

    static void insertInt(BigInteger signed, int paddedLen, ByteBuffer dest) {
        byte[] arr = signed.toByteArray();
        int arrLen = arr.length;
        if(arrLen <= paddedLen) {
            insertPadding(paddedLen - arrLen, signed.signum() < 0, dest);
            dest.put(arr);
        } else {
            dest.put(arr, 1, paddedLen);
        }
    }

    static void insertBytesPadded(byte[] bytes, ByteBuffer dest) {
        dest.put(bytes);
        int rem = Integers.mod(bytes.length, UNIT_LENGTH_BYTES);
        insertPadding(rem != 0 ? UNIT_LENGTH_BYTES - rem : 0, false, dest);
    }

    static void insertPadding(int n, boolean negativeOnes, ByteBuffer dest) {
        dest.put(!negativeOnes ? CACHED_ZERO_PADDING : CACHED_NEG1_PADDING, 0, n);
    }
}
