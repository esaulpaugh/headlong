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

public final class BigDecimalType extends UnitType<BigDecimal> {

    private static final Class<BigDecimal> CLASS = BigDecimal.class;
    private static final String ARRAY_CLASS_NAME = BigDecimal[].class.getName();

    final int scale;

    BigDecimalType(String canonicalTypeString, int bitLength, int scale, boolean unsigned) {
        super(canonicalTypeString, CLASS, bitLength, unsigned);
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    @Override
    String arrayClassName() {
        return ARRAY_CLASS_NAME;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BIG_DECIMAL;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
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

    @Override
    public BigDecimal parseArgument(String s) {
        BigDecimal bigDec = new BigDecimal(new BigInteger(s), scale);
        validate(bigDec);
        return bigDec;
    }
}
