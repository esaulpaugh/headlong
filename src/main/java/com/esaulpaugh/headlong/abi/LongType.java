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

import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
import com.esaulpaugh.headlong.util.Integers;

import java.nio.ByteBuffer;

public final class LongType extends UnitType<Long> {

    LongType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, Long.class, bitLength, unsigned);
    }

    @Override
    Class<?> arrayClass() {
        return long[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_LONG;
    }

    @Override
    Long decode(ByteBuffer bb, byte[] unitBuffer) {
        return decodeValid(bb, unitBuffer).longValue();
    }

    @Override
    public Long parseArgument(String s) {
        Long lo = Long.parseLong(s);
        validate(lo);
        return lo;
    }

    static void encodeLong(long value, int byteLen, ByteBuffer dest) {
        if(value >= 0) {
            Encoding.insertPadding(byteLen - Integers.len(value), false, dest);
            Integers.putLong(value, dest);
        } else {
            Encoding.insertPadding(byteLen - BizarroIntegers.len(value), true, dest);
            BizarroIntegers.putLong(value, dest);
        }
    }
}
