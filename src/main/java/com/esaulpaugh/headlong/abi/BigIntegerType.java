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

import com.esaulpaugh.headlong.exception.DecodeException;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public final class BigIntegerType extends UnitType<BigInteger> {

    private static final Class<BigInteger> CLASS = BigInteger.class;
    private static final String ARRAY_CLASS_NAME = BigInteger[].class.getName();

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, CLASS, bitLength, unsigned);
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
    public BigInteger parseArgument(String s) throws ValidationException {
        BigInteger bigInt = new BigInteger(s);
        validate(bigInt);
        return bigInt;
    }

    @Override
    public int validate(Object value) throws ValidationException {
        validateClass(value);
        try {
            validateBigIntBitLen((BigInteger) value);
        } catch (DecodeException de) {
            throw new ValidationException(de);
        }
        return UNIT_LENGTH_BYTES;
    }

    @Override
    void encodeHead(Object value, ByteBuffer dest, int[] offset) {
        Encoding.insertInt((BigInteger) value, dest);
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) throws DecodeException {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi;
    }
}
