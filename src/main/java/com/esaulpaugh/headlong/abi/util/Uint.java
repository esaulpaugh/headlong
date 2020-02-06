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
package com.esaulpaugh.headlong.abi.util;

import java.math.BigInteger;

/**
 * For converting integers to and from signed and unsigned representation. Use {@code new Uint(8)} for uint8 et cetera.
 */
public final class Uint {

    private static final BigInteger TWO = BigInteger.valueOf(2L);

    private static final long ZERO = 0L;

    public final int numBits;
    public final BigInteger range;
    public final long rangeLong;
    public final BigInteger halfRange;
    public final long halfRangeLong;
    public final long maskLong;

    public Uint(int numBits) {
        if(numBits < 0) {
            throw new IllegalArgumentException("numBits must be non-negative");
        }
        this.numBits = numBits;
        this.range = TWO.shiftLeft(numBits - 1); // TWO.pow(numBits)
        long rangeLong, halfRangeLong, maskLong;
        try {
            rangeLong = range.longValueExact();
            halfRangeLong = rangeLong >> 1;
            maskLong = range.subtract(BigInteger.ONE).longValueExact();
        } catch (ArithmeticException ae) {
            rangeLong = halfRangeLong = maskLong = ZERO;
        }
        this.rangeLong = rangeLong;
        this.halfRange = range.shiftRight(1);
        this.halfRangeLong = halfRangeLong;
        this.maskLong = maskLong;
    }

    public long toSigned(long unsigned) {
        if(rangeLong != ZERO) {
            if(unsigned < 0) {
                throw new IllegalArgumentException("unsigned value is negative: " + unsigned);
            }
            final int bitLen = com.esaulpaugh.headlong.util.Integers.bitLen(unsigned);
            if(bitLen <= numBits) {
                // if in upper half of range, subtract range
                return unsigned >= halfRangeLong
                        ? unsigned - rangeLong
                        : unsigned;
            }
            throw tooManyBitsException(bitLen, numBits, false);
        }
        return toSigned(BigInteger.valueOf(unsigned)).longValueExact();
    }

    public long toUnsigned(long signed) {
        if(maskLong != ZERO) {
            final int bitLen = signed < 0 ? BizarroIntegers.bitLen(signed) : com.esaulpaugh.headlong.util.Integers.bitLen(signed);
            if(bitLen < numBits) {
                return signed & maskLong;
            }
            throw tooManyBitsException(bitLen, numBits, true);
        }
        return toUnsigned(BigInteger.valueOf(signed))
                .longValueExact(); // beware of ArithmeticException
    }

    public BigInteger toSigned(BigInteger unsigned) {
        if(unsigned.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("unsigned value is negative: " + unsigned);
        }
        final int bitLen = unsigned.bitLength();
        if(bitLen <= numBits) {
            // if in upper half of range, subtract range
            return unsigned.compareTo(halfRange) >= 0
                    ? unsigned.subtract(range)
                    : unsigned;
        }
        throw tooManyBitsException(bitLen, numBits, false);
    }

    public BigInteger toUnsigned(BigInteger signed) {
        final int bitLen = signed.bitLength();
        if(bitLen < numBits) {
            return signed.compareTo(BigInteger.ZERO) >= 0
                    ? signed
                    : signed.add(range);
        }
        throw tooManyBitsException(bitLen, numBits, true);
    }

    private static IllegalArgumentException tooManyBitsException(int bitLen, int rangeNumBits, boolean signed) {
        return signed
                ? new IllegalArgumentException("signed has too many bits: " + bitLen + " is not less than " + rangeNumBits)
                : new IllegalArgumentException("unsigned has too many bits: " + bitLen + " > " + rangeNumBits);
    }
}
