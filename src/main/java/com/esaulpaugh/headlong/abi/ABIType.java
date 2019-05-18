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

import java.io.Serializable;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 *
 * @param <J>   this {@link ABIType}'s corresponding Java type
 */
public abstract class ABIType<J> implements Serializable {

    static final int TYPE_CODE_BOOLEAN = 0;
    static final int TYPE_CODE_BYTE = 1;
    static final int TYPE_CODE_INT = 2;
    static final int TYPE_CODE_LONG = 3;
    static final int TYPE_CODE_BIG_INTEGER = 4;
    static final int TYPE_CODE_BIG_DECIMAL = 5;

    static final int TYPE_CODE_ARRAY = 6;
    static final int TYPE_CODE_TUPLE = 7;

    public static final ABIType<?>[] EMPTY_TYPE_ARRAY = new ABIType<?>[0];

    final String canonicalType;
    final Class<J> clazz;
    final boolean dynamic;

    private String name;

    ABIType(String canonicalType, Class<J> clazz, boolean dynamic) {
        this.canonicalType = canonicalType; // .intern() to save memory and allow == comparison?
        this.clazz = clazz;
        this.dynamic = dynamic;
        this.name = null;
    }

    public String getCanonicalType() {
        return canonicalType;
    }

    public Class<J> clazz() {
        return clazz;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public String getName() {
        return name;
    }

    ABIType<J> setName(String name) {
        this.name = name;
        return this;
    }

    abstract String arrayClassNameStub();

    abstract int typeCode();

    abstract int byteLength(Object value);

    abstract int byteLengthPacked(Object value);

    public abstract int validate(Object value);

    abstract void encodeHead(Object value, ByteBuffer dest, int[] offset);

    abstract void encodeTail(Object value, ByteBuffer dest);

    /**
     * Decodes the data at the buffer's current position according to this {@link ABIType}.
     *
     * @param buffer    the buffer containing the encoded data
     * @param unitBuffer a buffer of length {@link UnitType#UNIT_LENGTH_BYTES} in which to store intermediate values
     * @return  the decoded value
     */
    abstract J decode(ByteBuffer buffer, byte[] unitBuffer);

    public abstract J parseArgument(String s);

    void validateClass(Object value) {
        // may throw NPE
        if(clazz != value.getClass()) {
            // this pretty much only happens in the error case
            if(!clazz.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("class mismatch: "
                        + value.getClass().getName()
                        + " not assignable to "
                        + clazz.getName()
                        + " (" + ClassNames.toFriendly(value.getClass().getName()) + " not instanceof " + ClassNames.toFriendly(clazz.getName()) + "/" + canonicalType + ")");
            }
        }
    }

    static byte[] newUnitBuffer() {
        return new byte[UNIT_LENGTH_BYTES];
    }

    @Override
    public final int hashCode() {
        return canonicalType.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return canonicalType.equals(((ABIType<?>) o).canonicalType);
    }

    @Override
    public final String toString() {
        return canonicalType;
    }

    final void toString(StringBuilder sb) {
        switch (typeCode()) {
        case TYPE_CODE_ARRAY:
            sb.append("[");
            ((ArrayType<?, ?>) this).elementType.toString(sb);
            sb.append("]");
            break;
        case TYPE_CODE_TUPLE:
            sb.append("(");
            for(ABIType<?> e : ((TupleType) this)) {
                e.toString(sb);
            }
            sb.append(")");
            break;
        default:
            sb.append(this);
        }
        sb.append(' ').append(getName()).append(',');
    }
}
