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

public final class BigIntegerType extends UnitType<BigInteger> {

    private static final String ARRAY_CLASS_NAME = BigInteger[].class.getName();

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, BigInteger.class, bitLength, unsigned);
    }

    @Override
    String arrayClassName() {
        return ARRAY_CLASS_NAME;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BIG_INTEGER;
    }

    @Override
    public BigInteger parseArgument(String s) {
        BigInteger bigInt = new BigInteger(s);
        validate(bigInt);
        return bigInt;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        validateBigInt((BigInteger) value);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int encodeHead(Object value, ByteBuffer dest, int offset) {
        Encoding.insertInt((BigInteger) value, dest);
        return offset;
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigInt(bi);
        return bi;
    }
}
