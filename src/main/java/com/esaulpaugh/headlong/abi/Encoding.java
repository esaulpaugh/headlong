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

    private Encoding() {}

    static final int OFFSET_LENGTH_BYTES = UNIT_LENGTH_BYTES;

    static final IntType UINT17 = new IntType("uint17", 17, true, null); // small bit length for Denial-of-Service protection

    static final IntType UINT31 = new IntType("uint31", 31, true, null);

    static final byte ZERO_BYTE = (byte) 0x00;
    static final byte ONE_BYTE = (byte) 0x01;

    private static final byte[] CACHED_ZERO_PADDING = new byte[UNIT_LENGTH_BYTES];
    private static final byte[] CACHED_NEG1_PADDING = new byte[UNIT_LENGTH_BYTES];

    static {
        Arrays.fill(CACHED_NEG1_PADDING, (byte) 0xFF);
    }

    static void insertIntUnsigned(long val, ByteBuffer dest) {
        insert00Padding(UNIT_LENGTH_BYTES - Long.BYTES, dest);
        dest.putLong(val);
    }

    static void insertInt(long val, ByteBuffer dest) {
        insertPadding(UNIT_LENGTH_BYTES - Long.BYTES, val < 0, dest);
        dest.putLong(val);
    }

    static void insertInt(BigInteger signed, int paddedLen, ByteBuffer dest) {
        byte[] arr = signed.toByteArray();
        if(arr.length <= paddedLen) {
            insertPadding(paddedLen - arr.length, signed.signum() < 0, dest);
            dest.put(arr, 0 ,arr.length);
        } else {
            dest.put(arr, 1, paddedLen);
        }
    }

    private static void insertPadding(int n, boolean negativeOnes, ByteBuffer dest) {
        if(negativeOnes) {
            insertFFPadding(n, dest);
        } else {
            insert00Padding(n, dest);
        }
    }

    static void insert00Padding(int n, ByteBuffer dest) {
        dest.put(CACHED_ZERO_PADDING, 0, n);
    }

    static void insertFFPadding(int n, ByteBuffer dest) {
        dest.put(CACHED_NEG1_PADDING, 0, n);
    }
}
