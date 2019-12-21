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

public final class ByteType extends UnitType<Byte> {

    private static final Class<Byte> CLASS = Byte.class;
    private static final String ARRAY_CLASS_NAME = byte[].class.getName();

    private static final int MAX_BIT_LEN = 8;

//    static final ByteType SIGNED = new ByteType("int8", false);
    static final ByteType UNSIGNED = new ByteType("uint8", true);

    private ByteType(String canonicalType, boolean unsigned) {
        super(canonicalType, CLASS, MAX_BIT_LEN, unsigned);
    }

    @Override
    String arrayClassName() {
        return ARRAY_CLASS_NAME;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BYTE;
    }

    @Override
    int byteLengthPacked(Object value) {
        return 1;
    }

    @Override
    public int validate(Object value) throws ValidationException {
        validateClass(value);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Byte decode(ByteBuffer bb, byte[] unitBuffer) throws ValidationException {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi.byteValue();
    }

    @Override
    public Byte parseArgument(String s) throws ValidationException {
        Byte b = Byte.parseByte(s);
        validate(b);
        return b;
    }
}
