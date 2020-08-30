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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Represents a Contract ABI type such as uint256 or decimal. Used to validate, encode, and decode data.
 *
 * @param <J> this {@link ABIType}'s corresponding Java type
 */
public abstract class ABIType<J> {

    public static final int TYPE_CODE_BOOLEAN = 0;
    public static final int TYPE_CODE_BYTE = 1;
    public static final int TYPE_CODE_INT = 2;
    public static final int TYPE_CODE_LONG = 3;
    public static final int TYPE_CODE_BIG_INTEGER = 4;
    public static final int TYPE_CODE_BIG_DECIMAL = 5;

    public static final int TYPE_CODE_ARRAY = 6;
    public static final int TYPE_CODE_TUPLE = 7;

    public static final ABIType<?>[] EMPTY_TYPE_ARRAY = new ABIType<?>[0];

    final String canonicalType;
    final Class<J> clazz;
    final boolean dynamic;

    private String name = null;

    ABIType(String canonicalType, Class<J> clazz, boolean dynamic) {
        this.canonicalType = canonicalType; // .intern() to save memory and allow == comparison?
        this.clazz = clazz;
        this.dynamic = dynamic;
    }

    public final String getCanonicalType() {
        return canonicalType;
    }

    public final Class<J> clazz() {
        return clazz;
    }

    public final boolean isDynamic() {
        return dynamic;
    }

    public final String getName() {
        return name;
    }

    /* don't expose this; cached (nameless) instances are shared and must be immutable */
    final ABIType<J> setName(String name) {
        this.name = name;
        return this;
    }

    abstract Class<?> arrayClass();

    /**
     * Returns an integer code specific to this instance's class, which is a subclass of {@link ABIType}.
     *
     * @return the code
     */
    public abstract int typeCode();

    abstract int byteLength(Object value);

    abstract int byteLengthPacked(Object value);

    /**
     * Checks whether the given object is a valid argument for this {@link ABIType}. Requires an instance of type J.
     *
     * @param value an object of type J
     * @return the byte length of the ABI encoding of {@code value}
     */
    public abstract int validate(Object value);

    int encodeHead(Object value, ByteBuffer dest, int nextOffset) {
        if (!dynamic) {
            encodeTail(value, dest);
            return nextOffset;
        }
        return Encoding.insertOffset(nextOffset, dest, byteLength(value));
    }

    abstract void encodeTail(Object value, ByteBuffer dest);

    /**
     * Decodes the data at the buffer's current position according to this {@link ABIType}.
     *
     * @param buffer     the buffer containing the encoded data
     * @param unitBuffer a buffer of length {@link UnitType#UNIT_LENGTH_BYTES} in which to store intermediate values
     * @return the decoded value
     * @throws IllegalArgumentException if the data is malformed
     */
    abstract J decode(ByteBuffer buffer, byte[] unitBuffer);

    static void decodeTails(ByteBuffer bb, int[] offsets, int tailStart, Consumer<Integer> tailDecoder) {
        for (int i = 0; i < offsets.length; i++) {
            final int offset = offsets[i];
            if(offset > 0) {
                if (offset >= 0x20) {
                    /* LENIENT MODE; see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
                    if (tailStart + offset > bb.position()) {
                        bb.position(tailStart + offset); // leniently jump to specified offset
                    }
                    try {
                        tailDecoder.accept(i);
                    } catch (BufferUnderflowException bue) {
                        throw new IllegalArgumentException(bue);
                    }
                } else {
                    throw new IllegalArgumentException("offset less than 0x20");
                }
            }
        }
    }

    /**
     * Parses and validates a string representation of J. Not supported by {@link ArrayType}, {@link TupleType}.
     *
     * @param s the object's string representation
     * @return  the object
     */
    public abstract J parseArgument(String s);

    void validateClass(Object value) {
        if(!clazz.isInstance(value)) {
            if(value == null) {
                throw new NullPointerException();
            }
            throw new IllegalArgumentException("class mismatch: "
                    + value.getClass().getName()
                    + " not assignable to "
                    + clazz.getName()
                    + " (" + Utils.friendlyClassName(value.getClass()) + " not instanceof " + Utils.friendlyClassName(clazz) + "/" + canonicalType + ")");
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
}
