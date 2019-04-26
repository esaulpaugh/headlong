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

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Unsigned 0 or 1.
 */
class BooleanType extends UnitType<Boolean> {

    static final Class<?> CLASS = Boolean.class;
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(boolean[].class);

    static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {
        super("bool", CLASS, 1, true);
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BOOLEAN;
    }

    @Override
    int byteLengthPacked(Object value) {
        return 1;
    }

    @Override
    public Boolean parseArgument(String s) {
        Boolean bool = Boolean.parseBoolean(s);
        validate(bool);
        return bool;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Boolean decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        switch (bi.byteValue()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new IllegalArgumentException("negative value given for boolean type");
        }
    }
}
