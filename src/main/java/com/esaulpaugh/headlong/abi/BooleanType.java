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

import java.nio.ByteBuffer;

/** Unsigned 0 or 1. */
public final class BooleanType extends UnitType<Boolean> {

    static final BooleanType INSTANCE = new BooleanType();

    private static final byte[] BOOLEAN_FALSE = new byte[UNIT_LENGTH_BYTES];
    private static final byte[] BOOLEAN_TRUE = new byte[UNIT_LENGTH_BYTES];

    static {
        BOOLEAN_TRUE[BOOLEAN_TRUE.length-1] = 1;
    }

    private BooleanType() {
        super("bool", Boolean.class, 1, true);
    }

    @Override
    Class<?> arrayClass() {
        return boolean[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BOOLEAN;
    }

    @Override
    int byteLengthPacked(Boolean value) {
        return Byte.BYTES;
    }

    @Override
    void validateInternal(Boolean value) {
        // all Booleans are a valid bool
    }

    @Override
    void encodeTail(Boolean value, ByteBuffer dest) {
        encodeBoolean(value, dest);
    }

    @Override
    void encodePackedUnchecked(Boolean value, ByteBuffer dest) {
        encodeBooleanPacked(value, dest);
    }

    @Override
    Boolean decode(ByteBuffer bb, byte[] unitBuffer) {
        return decodeBoolean(decodeValid(bb, unitBuffer).byteValue());
    }

    static void encodeBooleanPacked(boolean value, ByteBuffer dest) {
        dest.put(value ? ONE_BYTE : ZERO_BYTE);
    }

    static void encodeBoolean(boolean val, ByteBuffer dest) {
        dest.put(val ? BOOLEAN_TRUE : BOOLEAN_FALSE);
    }

    static Boolean decodeBoolean(byte b) {
        switch (b) {
            case ZERO_BYTE: return Boolean.FALSE;
            case ONE_BYTE: return Boolean.TRUE;
            default: throw new IllegalArgumentException("illegal boolean value: " + b);
        }
    }
}
