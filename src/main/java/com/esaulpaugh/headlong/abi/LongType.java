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

public final class LongType extends UnitType<Long> {

    private static final Class<Long> CLASS = Long.class;
    private static final String ARRAY_CLASS_NAME = long[].class.getName();

    LongType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, CLASS, bitLength, unsigned);
    }

    @Override
    String arrayClassName() {
        return ARRAY_CLASS_NAME;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_LONG;
    }

    @Override
    Long decode(ByteBuffer bb, byte[] unitBuffer) throws ABIException {
        bb.get(unitBuffer);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigInt(bi);
        return bi.longValue();
    }

    @Override
    public Long parseArgument(String s) throws ABIException {
        Long lo = Long.parseLong(s);
        validate(lo);
        return lo;
    }
}
