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

    static int insertOffset(final int offset, ABIType<?> paramType, Object object, ByteBuffer dest) {
        insertInt(offset, dest);
        return offset + paramType.byteLength(object);
    }

    static void insertInt(long val, ByteBuffer dest) {
        dest.put(val >= 0 ? NON_NEGATIVE_INT_PADDING : NEGATIVE_INT_PADDING);
        dest.putLong(val);
    }

    static void insertInt(BigInteger bigGuy, ByteBuffer dest) {
        final byte[] arr = bigGuy.toByteArray();
        final byte paddingByte = bigGuy.signum() == -1 ? NEGATIVE_ONE_BYTE : ZERO_BYTE;
        final int lim = UNIT_LENGTH_BYTES - arr.length;
        for (int i = 0; i < lim; i++) {
            dest.put(paddingByte);
        }
        dest.put(arr);
    }
}
