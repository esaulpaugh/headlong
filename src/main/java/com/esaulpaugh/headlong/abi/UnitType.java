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
import com.esaulpaugh.headlong.abi.util.Uint;
import com.esaulpaugh.headlong.util.Integers;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/** Superclass for any 256-bit ("unit") Contract ABI type. Usually numbers or boolean. Not for arrays. */
public abstract class UnitType<V> extends ABIType<V> { // V generally extends Number or is Boolean

    public static final int UNIT_LENGTH_BYTES = 32;

    final int bitLength;
    final boolean unsigned;

    UnitType(String canonicalType, Class<V> clazz, int bitLength, boolean unsigned) {
        super(canonicalType, clazz, false);
        this.bitLength = bitLength;
        this.unsigned = unsigned;
    }

    public int getBitLength() {
        return bitLength;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    @Override
    final int byteLength(Object value) {
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        validatePrimitive(((Number) value).longValue());
        return UNIT_LENGTH_BYTES;
    }

    @Override
    int encodeHead(Object value, ByteBuffer dest, int nextOffset) {
        Encoding.insertInt(((Number) value).longValue(), dest);
        return nextOffset;
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        throw new UnsupportedOperationException();
    }

    final void validatePrimitive(long longVal) {
        if(longVal < 0) {
            if(unsigned) {
                throw new IllegalArgumentException("signed value given for unsigned type");
            }
            new Uint(bitLength).toUnsigned(longVal);
        } else {
            checkBitLen(Integers.bitLen(longVal));
        }
    }

    final void validateBigInt(BigInteger bigIntVal) {
        if(bigIntVal.signum() < 0) {
            if(unsigned) {
                throw new IllegalArgumentException("signed value given for unsigned type");
            }
            new Uint(bitLength).toUnsigned(bigIntVal);
        } else {
            checkBitLen(bigIntVal.bitLength());
        }
    }

    final void checkBitLen(int actual) {
        if (actual > bitLength) {
            throw new IllegalArgumentException("exceeds bit limit: " + actual + " > " + bitLength);
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
