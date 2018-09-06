package com.esaulpaugh.headlong.abi.beta;

import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.beta.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.beta.util.ClassNames.toFriendly;

// TODO support model classes à la Student.java
abstract class StackableType<V> {

    static final StackableType[] EMPTY_TYPE_ARRAY = new StackableType[0];

    private final String canonicalType;

    final boolean dynamic;

    StackableType(String canonicalType, boolean dynamic) {
        this.canonicalType = canonicalType;
        this.dynamic = dynamic;
    }

    abstract String className();

    abstract String arrayClassNameStub();

    abstract int byteLength(Object value);

    V decode(byte[] buffer, int index) {
        return decode(ByteBuffer.wrap(buffer, index, buffer.length - index), new byte[UNIT_LENGTH_BYTES]);
    }

    /**
     *
     * @param buffer    the buffer containing the encoded data
     * @param unitBuffer a buffer of length {@link AbstractUnitType#UNIT_LENGTH_BYTES} in which to store intermediate values
     * @return
     */
    abstract V decode(ByteBuffer buffer, byte[] unitBuffer);

    void validate(Object value) {
        validate(this, value);
    }

    private static void validate(final StackableType type, final Object value) {

        final String expectedClassName = type.className();

        // will throw NPE if argument null
        if(!expectedClassName.equals(value.getClass().getName())) {
            boolean isAssignable;
            try {
                isAssignable = Class.forName(expectedClassName).isAssignableFrom(value.getClass());
            } catch (ClassNotFoundException cnfe) {
                isAssignable = false;
            }
            if(!isAssignable) {
                throw new IllegalArgumentException("class mismatch: "
                        + value.getClass().getName()
                        + " not assignable to "
                        + expectedClassName
                        + " (" + toFriendly(value.getClass().getName()) + " not instanceof " + toFriendly(expectedClassName) + "/" + type.canonicalType + ")");
            }
        }
    }
}
