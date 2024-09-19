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

import java.nio.ByteBuffer;

/** Represents a small integer type such as int8, int32, uint8 or uint24. */
public final class IntType extends UnitType<Integer> {

    static final IntType UINT21 = new IntType("uint21", 21, true); // small bit length for Denial-of-Service protection
    static final IntType UINT31 = new IntType("uint31", 31, true);

    IntType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, Integer.class, bitLength, unsigned);
    }

    @Override
    Class<?> arrayClass() {
        return int[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_INT;
    }

    @Override
    Integer decode(ByteBuffer bb, byte[] unitBuffer) {
        return (int) (
                    unsigned
                        ? decodeUnsignedLong(bb)
                        : decodeSignedLong(bb)
                );
    }
}
