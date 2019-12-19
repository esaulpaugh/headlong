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
import com.esaulpaugh.headlong.abi.util.Utils;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.exception.UnrecoverableDecodeException;
import com.esaulpaugh.headlong.util.Integers;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Superclass for any 32-byte ("unit") Contract ABI type. Usually numbers or boolean. Not for arrays.
 */
abstract class UnitType<V> extends ABIType<V> { // V generally extends Number or is Boolean

    static final int UNIT_LENGTH_BYTES = 32;
    static final int LOG_2_UNIT_LENGTH_BYTES = 5;

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
        try {
            validateLongBitLen(((Number) value).longValue());
        } catch (DecodeException de) {
            throw Utils.illegalArgumentException(de);
        }
        return UNIT_LENGTH_BYTES;
    }

    @Override
    void encodeHead(Object value, ByteBuffer dest, int[] offset) {
        Encoding.insertInt(((Number) value).longValue(), dest);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        throw new UnsupportedOperationException();
    }

    // don't do unsigned check for array element
    final void validatePrimitiveElement(long longVal) throws DecodeException {
        checkBitLen(longVal >= 0 ? Integers.bitLen(longVal) : BizarroIntegers.bitLen(longVal));
    }

    // don't do unsigned check for array element
    final void validateBigIntElement(final BigInteger bigIntVal) throws DecodeException {
        checkBitLen(bigIntVal.bitLength());
    }

    // --------------------------------

    final void validateLongBitLen(long longVal) throws DecodeException {
        checkBitLen(longVal >= 0 ? Integers.bitLen(longVal) : BizarroIntegers.bitLen(longVal));
        if (unsigned && longVal < 0) {
            throw new UnrecoverableDecodeException("signed value given for unsigned type");
        }
    }

    final void validateBigIntBitLen(final BigInteger bigIntVal) throws DecodeException {
        checkBitLen(bigIntVal.bitLength());
        if (unsigned && bigIntVal.signum() == -1) {
            throw new UnrecoverableDecodeException("signed value given for unsigned type");
        }
    }

    private void checkBitLen(int actual) throws DecodeException {
        if (actual > bitLength) {
            throw new UnrecoverableDecodeException("exceeds bit limit: " + actual + " > " + bitLength);
        }
    }
}
