package com.esaulpaugh.headlong.abi.beta.type.array;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.type.integer.*;
import com.esaulpaugh.headlong.abi.beta.util.Pair;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.esaulpaugh.headlong.abi.beta.type.array.DynamicArrayType.DYNAMIC_LENGTH;

public abstract class ArrayType<T extends StackableType, E> extends StackableType<E[]> {

    public static final int ARRAY_LENGTH_BYTE_LEN = IntType.MAX_BIT_LEN;
    protected static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", IntType.CLASS_NAME, ARRAY_LENGTH_BYTE_LEN);

    public final T elementType;
    protected final int length;

    protected ArrayType(String canonicalAbiType, String className, T elementType, int length) {
        this(canonicalAbiType, className, elementType, length, false);
    }

    protected ArrayType(String canonicalAbiType, String className, T elementType, int length, boolean dynamic) {
        super(canonicalAbiType, className, dynamic);
        this.elementType = elementType;
        this.length = length;
        try {
            Class c = Class.forName(className);
            if(!c.isArray() && !String.class.isAssignableFrom(c)) {
                throw new AssertionError(className);
            }
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int byteLength(Object value) {
        return getDataLen(value);
    }

    @Override
//    @SuppressWarnings("unchecked")
    public E[] decode(byte[] buffer, int index) {
        throw new UnsupportedOperationException("use decodeArray");
//        return (E[]) decodeArray(buffer, index).first;
    }

    public Pair<Object, Integer> decodeArray(final byte[] buffer, final int index) {
        final int arrayLen;
        int idx;
        if(dynamic) {
            arrayLen = ARRAY_LENGTH_TYPE.decode(buffer, index);
            System.out.println("arrayLen " + arrayLen + " @ " + index);
            checkLength(arrayLen);
            idx = index + ARRAY_LENGTH_BYTE_LEN;
        } else {
            arrayLen = length;
            idx = index;
        }
        if(elementType instanceof ByteType) {
            byte[] out = new byte[arrayLen];
            System.arraycopy(buffer, idx, out, 0, arrayLen);
            return new Pair<>(out, idx + arrayLen);
        } else if(elementType instanceof AbstractInt256Type) {
            ByteBuffer bb = ByteBuffer.wrap(buffer, idx, arrayLen << 5); // mul 32
            final byte[] thirtyTwo = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
            if(elementType instanceof BooleanType) {
                return decodeBooleanArray(arrayLen, bb, thirtyTwo);
            } if(elementType instanceof ShortType) {
                short[] shorts = new short[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    shorts[i] = new BigInteger(thirtyTwo).shortValueExact(); // validates that value is in short range
                }
                return new Pair<>(shorts, bb.position());
            } else if(elementType instanceof IntType) {
                int[] ints = new int[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    ints[i] = new BigInteger(thirtyTwo).intValueExact(); // validates that value is in int range
                }
                return new Pair<>(ints, bb.position());
            } else if(elementType instanceof LongType) {
                long[] longs = new long[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    longs[i] = new BigInteger(thirtyTwo).longValueExact(); // validates that value is in long range
                }
                return new Pair<>(longs, bb.position());
            } else if(elementType instanceof BigIntegerType) {
                BigIntegerType et = (BigIntegerType) elementType;
                BigInteger[] bigInts = new BigInteger[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    BigInteger temp = new BigInteger(thirtyTwo);
                    et.validateBitLen(temp.bitLength()); // validate that value is in range
                    bigInts[i] = temp;
                }
                return new Pair<>(bigInts, bb.position());
            } else if(elementType instanceof BigDecimalType) {
                BigDecimalType et = (BigDecimalType) elementType;
                BigDecimal[] bigInts = new BigDecimal[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    BigInteger temp = new BigInteger(thirtyTwo);
                    et.validateBitLen(temp.bitLength()); // validate that value is in range
                    bigInts[i] = new BigDecimal(temp, et.scale);
                }
                return new Pair<>(bigInts, bb.position());
            }
            throw new Error();
        } else {
            return decodeObjectArray(arrayLen, buffer, idx);
        }
    }

    @SuppressWarnings("unchecked")
    private Pair<Object, Integer> decodeObjectArray(int arrayLen, byte[] buffer, final int index) {

        final ArrayType elementArrayType = (ArrayType) elementType;

        Object[] objects; // = new Object[0]; // new Object[arrayLen];
        try {
            objects = (Object[]) Array.newInstance(Class.forName(elementArrayType.className), arrayLen);  // .getDeclaredConstructor(int.class).newInstance(arrayLen);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        int[] offsets = new int[arrayLen];

        int idx = index;
        for (int i = 0; i < arrayLen; i++) {
            if (elementArrayType.dynamic) {
                // TODO offset 5 @ 196, points to 73, increment to 228
                offsets[i] = IntType.OFFSET_TYPE.decode(buffer, idx); // TODO offset 2 @ 228, points to 166, increment to 260
                System.out.println("offset " + offsets[i] + " @ " + idx + ", points to " + (index + offsets[i]) + ", increment to " + (idx + AbstractInt256Type.INT_LENGTH_BYTES));
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            } else {
                Pair<Object, Integer> results = elementArrayType.decodeArray(buffer, idx);
                objects[i] = results.first;
                idx = results.second;
            }
//            idx += AbstractInt256Type.INT_LENGTH_BYTES;
        }

        if (dynamic) {
            for (int i = 0; i < arrayLen; i++) {
                int offset = offsets[i];
                if(offset > 0) {
                   idx = index + offset;
                    Pair<Object, Integer> results = elementArrayType.decodeArray(buffer, idx);
                    objects[i] = results.first;
                }
            }
        }

        return new Pair<>(objects, idx);
    }

    private static Pair<Object, Integer> decodeBooleanArray(int arrayLen, ByteBuffer bb, byte[] temp32) {
        boolean[] booleans = new boolean[arrayLen];
        final int booleanOffset = AbstractInt256Type.INT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < arrayLen; i++) {
            bb.get(temp32);
            int j = 0;
            for ( ; j < booleanOffset; j++) {
                if(temp32[i] != 0) {
                    throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - AbstractInt256Type.INT_LENGTH_BYTES));
                }
            }
            byte last = temp32[j];
            if(last == 0) {
                booleans[i] = false;
            } else if(last == 1) {
                booleans[i] = true;
            } else {
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - AbstractInt256Type.INT_LENGTH_BYTES));
            }
        }
        return new Pair<>(booleans, bb.position());
    }

//            final int shortOffset = AbstractInt256Type.INT_LENGTH_BYTES - Short.BYTES;
//                out[i] = bb.getShort(idx + shortOffset);
//                idx += AbstractInt256Type.INT_LENGTH_BYTES;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + elementType + ">(" + length + ")";
    }

    @Override
    public void validate(final Object value) { // , final String expectedClassName // int stackIndex
        super.validate(value);

        if(value.getClass().isArray()) {
//            StackableType elementType = ((ArrayType) this.typeStack.peek()).elementType;
            if(value instanceof Object[]) { // includes BigInteger[]
                Object[] arr = (Object[]) value;
                final int len = arr.length;
                checkLength(len);
                int i = 0;
                try {
                    for ( ; i < len; i++) {
                        elementType.validate(arr[i]);
                    }
                } catch (IllegalArgumentException | NullPointerException re) {
                    throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
                }
            } else if (value instanceof byte[]) {
                validateByteArray((byte[]) value);
            } else if (value instanceof int[]) {
                validateIntArray((int[]) value);
            } else if (value instanceof long[]) {
                validateLongArray((long[]) value);
            } else if (value instanceof short[]) {
                validateShortArray((short[]) value);
            } else if (value instanceof boolean[]) {
                validateBooleanArray((boolean[]) value);
            } else {
                throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
            }
        } else if(value instanceof String) {
            validateByteArray(((String) value).getBytes(StandardCharsets.UTF_8));
//        } else if(value instanceof Number) {
//            elementType.validate(value);
////            _validateNumber(value, elementType);
//        } else if(value instanceof Boolean) {
//            elementType.validate(value);
////            Type.validate(value, CLASS_NAME_BOOLEAN, elementType.canonicalAbiType); // TODO
        } else {
            throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }
    }

    private void validateBooleanArray(boolean[] arr) {
        checkLength(arr.length);
    }

    private void validateByteArray(byte[] arr) {
        checkLength(arr.length);
    }

    private void validateShortArray(short[] arr) {
        checkLength(arr.length);
    }

    private void validateIntArray(int[] arr) {
        final int len = arr.length;
        checkLength(len);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateLongArray(long[] arr) {
        final int len = arr.length;
        checkLength(len);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    protected void checkLength(int actual) {
        int expected = this.length;
        if(expected == DYNAMIC_LENGTH) { // -1
            System.out.println("dynamic length");
            return;
        }
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: actual != expected: " + actual + " != " + expected);
        }
        System.out.println("array length valid;");
    }

    // -----------------------------------------------------------------------------------------------------------------

//    protected int _overhead(Object value) { // , boolean dynamic
//        if(value.getClass().isArray()) {
//            if (value instanceof byte[]) { // always needs dynamic head?
//                return dynamic ? 64 : 0;
//            }
//            if (value instanceof int[]) {
//                return dynamic ? 64 : 0;
//            }
//            if (value instanceof long[]) {
//                return dynamic ? 64 : 0;
//            }
//            if (value instanceof short[]) {
//                return dynamic ? 64 : 0;
//            }
//            if (value instanceof boolean[]) {
//                return dynamic ? 64 : 0;
//            }
//            if (value instanceof Number[]) {
//                return dynamic ? 64 : 0;
//            }
//        }
//        if (value instanceof String) { // always needs dynamic head
//            return 64;
//        }
//        if (value instanceof Number) {
//            return 0;
//        }
//        if (value instanceof Tuple) {
//            throw new RuntimeException("arrays of tuples not yet supported"); // TODO **************************************
//        }
//        if (value instanceof Object[]) {
////            int len = 0;
////            for (Object element : (Object[]) value) {
////                len += this.elementType.byteLength(element);
////            }
//            return dynamic ? 64 : 0;
////            throw new AssertionError("Object array not expected here");
//        }
//        // shouldn't happen if type checks/validation already occurred
//        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
//    }

    protected int getDataLen(Object value) {
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) { // always needs dynamic head?
                int staticLen = roundUp(((byte[]) value).length);
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof int[]) {
                int staticLen = ((int[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof long[]) {
                int staticLen = ((long[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof short[]) {
                int staticLen = ((short[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof boolean[]) {
                int staticLen = ((boolean[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
            if (value instanceof Number[]) {
                int staticLen = ((Number[]) value).length << 5; // mul 32
                return dynamic ? 64 + staticLen : staticLen;
            }
//            if(value instanceof com.esaulpaugh.headlong.abi.beta.util.Tuple[]) {
//                throw new Error();
////                return elementType.byteLength(value);
//            }
        }
        if (value instanceof String) { // always needs dynamic head
            return 64 + roundUp(((String) value).length());
        }
        if (value instanceof Number) {
            return 32;
        }
        if (value instanceof com.esaulpaugh.headlong.abi.beta.util.Tuple) {
            return elementType.byteLength(value);
        }
        if (value instanceof Object[]) {
            int len = 0;
            for (Object element : (Object[]) value) {
                len += this.elementType.byteLength(element);
            }
            return dynamic ? 64 + len : len;
//            throw new AssertionError("Object array not expected here");
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    // TODO move?
    public static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 - mod);
    }
}
