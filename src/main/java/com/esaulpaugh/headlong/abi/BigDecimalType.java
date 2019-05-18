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

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

final class BigDecimalType extends UnitType<BigDecimal> {

    static final Class<?> CLASS = BigDecimal.class;
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(BigDecimal[].class);

    final int scale;

    BigDecimalType(String canonicalTypeString, int bitLength, int scale, boolean unsigned) {
        super(canonicalTypeString, CLASS, bitLength, unsigned);
        this.scale = scale;
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BIG_DECIMAL;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
    }

    @Override
    public BigDecimal parseArgument(String s) {
        BigDecimal bigDec = new BigDecimal(new BigInteger(s), scale);
        validate(bigDec);
        return bigDec;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        BigDecimal dec = (BigDecimal) value;
        validateBigIntBitLen(dec.unscaledValue());
        if(dec.scale() != scale) {
            throw new IllegalArgumentException("big decimal scale mismatch: actual != expected: " + dec.scale() + " != " + scale);
        }
        return UNIT_LENGTH_BYTES;
    }

    @Override
    void encodeHead(Object value, ByteBuffer dest, int[] offset) {
        Encoding.insertInt(((BigDecimal) value).unscaledValue(), dest);
    }

    @Override
    BigDecimal decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        BigDecimal dec = new BigDecimal(bi, scale);
        validateBigIntBitLen(bi);
        return dec;
    }
}
