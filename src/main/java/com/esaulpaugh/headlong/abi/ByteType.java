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

/** Currently used only as the element type for some {@link ArrayType}s. */
public final class ByteType extends UnitType<Byte> {

    static final ByteType SIGNED = new ByteType("int8", false);

    private ByteType(String canonicalType, boolean unsigned) {
        super(canonicalType, Byte.class, Byte.SIZE, unsigned);
    }

    @Override
    Class<?> arrayClass() {
        return byte[].class;
    }

    @Override
    public int typeCode() {
        return TYPE_CODE_BYTE;
    }

    @Override
    int byteLengthPacked(Byte value) {
        return Byte.BYTES;
    }

    @Override
    public int validate(Byte value) {
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Byte decode(ByteBuffer bb, byte[] unitBuffer) {
        return decodeValid(bb, unitBuffer).byteValue();
    }

    @Override
    public Byte parseArgument(String s) {
        Byte b = Byte.parseByte(s);
        validate(b);
        return b;
    }
}
