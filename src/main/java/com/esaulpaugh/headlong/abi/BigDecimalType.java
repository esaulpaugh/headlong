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
import java.nio.ByteBuffer;

/** Represents a decimal type such as fixed or ufixed. */
public final class BigDecimalType extends UnitType<BigDecimal> {

    static final Object CODE = new String("Pas de tel code.");

    final int scale;

    BigDecimalType(String canonicalTypeString, int bitLength, int scale, boolean unsigned, Object code) {
        super(canonicalTypeString, BigDecimal.class, bitLength, unsigned);
        if (scale <= 0 || scale > 80) {
            throw new IllegalStateException("bad scale");
        }
        if (code == CODE) {
            this.scale = scale;
            return;
        }
        throw new IllegalStateException("BigDecimalType should not be instantiated directly.");
    }

    public int getScale() {
        return scale;
    }

    public BigDecimal minDecimal() {
        return new BigDecimal(minValue(), scale);
    }

    public BigDecimal maxDecimal() {
        return new BigDecimal(maxValue(), scale);
    }

    @Override
    Class<?> arrayClass() {
        return BigDecimal[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BIG_DECIMAL;
    }

    @Override
    int validateInternal(BigDecimal value) {
        if (value.scale() != scale) {
            throw new IllegalArgumentException("BigDecimal scale mismatch: expected scale " + scale + " but found " + value.scale());
        }
        return validateBigInt(value.unscaledValue());
    }

    @Override
    void encodeTail(BigDecimal value, ByteBuffer dest) {
        insertInt(value.unscaledValue(), UNIT_LENGTH_BYTES, dest);
    }

    @Override
    void encodePackedUnchecked(BigDecimal value, ByteBuffer dest) {
        insertInt(value.unscaledValue(), byteLengthPacked(null), dest);
    }

    @Override
    BigDecimal decode(ByteBuffer bb, byte[] unitBuffer) {
        return new BigDecimal(decodeValid(bb, unitBuffer), scale);
    }
}
