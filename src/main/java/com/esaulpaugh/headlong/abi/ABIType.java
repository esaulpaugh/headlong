package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.io.Serializable;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

public abstract class ABIType<J> implements Serializable {

    static final int TYPE_CODE_BOOLEAN = 0;
    static final int TYPE_CODE_BYTE = 1;
    static final int TYPE_CODE_INT = 2;
    static final int TYPE_CODE_LONG = 3;
    static final int TYPE_CODE_BIG_INTEGER = 4;
    static final int TYPE_CODE_BIG_DECIMAL = 5;

    static final int TYPE_CODE_ARRAY = 6;
    static final int TYPE_CODE_TUPLE = 7;

    static final ABIType<?>[] EMPTY_TYPE_ARRAY = new ABIType<?>[0];

    final String canonicalType;
    final boolean dynamic;

    ABIType(String canonicalType, boolean dynamic) {
        this.canonicalType = canonicalType;
        this.dynamic = dynamic;
    }

    public String getCanonicalType() {
        return canonicalType;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public abstract String className();

    abstract String arrayClassNameStub();

    abstract int typeCode();

    abstract int byteLength(Object value);

    abstract int byteLengthPacked(Object value);

    public abstract J parseArgument(String s);

    public int validate(Object value) {
        final String expectedClassName = className();

        // will throw NPE if argument null
        if(!expectedClassName.equals(value.getClass().getName())) {
            // this pretty much only happens in the error case
            boolean assignable;
            try {
                assignable = Class.forName(expectedClassName).isAssignableFrom(value.getClass());
            } catch (ClassNotFoundException cnfe) {
                assignable = false;
            }
            if(!assignable) {
                throw new IllegalArgumentException("class mismatch: "
                        + value.getClass().getName()
                        + " not assignable to "
                        + expectedClassName
                        + " (" + ClassNames.toFriendly(value.getClass().getName()) + " not instanceof " + ClassNames.toFriendly(expectedClassName) + "/" + canonicalType + ")");
            }
        }

        return UNIT_LENGTH_BYTES;
    }

    /**
     *
     * @param buffer    the buffer containing the encoded data
     * @param unitBuffer a buffer of length {@link UnitType#UNIT_LENGTH_BYTES} in which to store intermediate values
     * @return  the decoded value
     */
    abstract J decode(ByteBuffer buffer, byte[] unitBuffer);

    static byte[] newUnitBuffer() {
        return new byte[UNIT_LENGTH_BYTES];
    }

    @Override
    public int hashCode() {
        return canonicalType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return canonicalType.equals(((ABIType<?>) o).canonicalType);
    }

    @Override
    public String toString() {
        return canonicalType;
    }
}
