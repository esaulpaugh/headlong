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

import java.math.BigInteger;
import java.nio.ByteBuffer;

/** Superclass for any 256-bit ("unit") Contract ABI type. Usually numbers or boolean. Not for arrays. */
public abstract class UnitType<J> extends ABIType<J> { // J generally extends Number or is Boolean

    public static final int UNIT_LENGTH_BYTES = 32;

    final int bitLength;
    final boolean unsigned;

    UnitType(String canonicalType, Class<J> clazz, int bitLength, boolean unsigned) {
        super(canonicalType, clazz, false);
        this.bitLength = bitLength;
        this.unsigned = unsigned;
    }

    public final int getBitLength() {
        return bitLength;
    }

    public final boolean isUnsigned() {
        return unsigned;
    }

    public final BigInteger minValue() {
        return unsigned ? BigInteger.ZERO : BigInteger.valueOf(2L).pow(bitLength - 1).negate();
    }

    public final BigInteger maxValue() {
        return BigInteger.valueOf(2L)
                .pow(unsigned ? bitLength : bitLength - 1)
                .subtract(BigInteger.ONE);
    }

    @Override
    final int byteLength(Object value) {
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength / Byte.SIZE;
    }

    @Override
    public int validate(J value) {
        return validatePrimitive(((Number) value).longValue());
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        Encoding.insertInt(((Number) value).longValue(), dest);
    }

    final int validatePrimitive(long longVal) {
        if(longVal < 0) {
            if(unsigned) {
                throw new IllegalArgumentException("signed value given for unsigned type");
            }
            checkBitLen(BizarroIntegers.bitLen(longVal));
        } else {
            checkBitLen(Integers.bitLen(longVal));
        }
        return UNIT_LENGTH_BYTES;
    }

    final void validateBigInt(BigInteger bigIntVal) {
        if(unsigned && bigIntVal.signum() < 0) {
            throw new IllegalArgumentException("signed value given for unsigned type");
        }
        checkBitLen(bigIntVal.bitLength());
    }

    final void checkBitLen(int actual) {
        if(unsigned) {
            if(actual > bitLength) {
                throw new IllegalArgumentException("unsigned val exceeds bit limit: " + actual + " > " + bitLength);
            }
        } else {
            if (actual >= bitLength) {
                throw new IllegalArgumentException("signed val exceeds bit limit: " + actual + " >= " + bitLength);
            }
        }
    }

    protected final BigInteger decodeValid(ByteBuffer bb, byte[] unitBuffer) {
        int idx = 0;
        if(unsigned) {
            unitBuffer = new byte[1 + UNIT_LENGTH_BYTES]; // a leading zero byte
            idx = 1;
        }
        bb.get(unitBuffer, idx, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigInt(bi);
        return bi;
    }
}
