package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Pair;
import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.esaulpaugh.headlong.abi.beta.DynamicArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.rlp.util.Strings.CHARSET_UTF_8;

abstract class ArrayType<T extends StackableType, E> extends StackableType<E[]> {

    private static final int ARRAY_LENGTH_BYTE_LEN = IntType.MAX_BIT_LEN;
    private static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", IntType.CLASS_NAME, ARRAY_LENGTH_BYTE_LEN, false);

    final T elementType;
    private final int length;

    ArrayType(String canonicalAbiType, String className, T elementType, int length) {
        this(canonicalAbiType, className, elementType, length, false);
    }

    ArrayType(String canonicalAbiType, String className, T elementType, int length, boolean dynamic) {
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
    int byteLength(Object value) {
        return getDataLen(value);
    }

    @Override
    E[] decode(byte[] buffer, int index) {
        throw new UnsupportedOperationException("use decodeArray");
    }

    Pair<Object, Integer> decodeArray(final byte[] buffer, final int index) {
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
            if(String.class.getName().equals(className)) {
                return new Pair<>(new String(out, CHARSET_UTF_8), roundUp(idx + arrayLen));
            }
            return new Pair<>(out, roundUp(idx + arrayLen));
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
                    long longVal = new BigInteger(thirtyTwo).longValueExact();
                    ((LongType) elementType).validateLongBitLen(longVal);
                    longs[i] = longVal;
                }
                return new Pair<>(longs, bb.position());
            } else if(elementType instanceof BigIntegerType) {
                BigIntegerType et = (BigIntegerType) elementType;
                BigInteger[] bigInts = new BigInteger[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    BigInteger temp = new BigInteger(thirtyTwo);
                    et.validateBigIntBitLen(temp);
                    bigInts[i] = temp;
                }
                return new Pair<>(bigInts, bb.position());
            } else if(elementType instanceof BigDecimalType) {
                BigDecimalType et = (BigDecimalType) elementType;
                BigDecimal[] bigInts = new BigDecimal[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    BigInteger temp = new BigInteger(thirtyTwo);
                    et.validateBigIntBitLen(temp);
                    bigInts[i] = new BigDecimal(temp, et.scale);
                }
                return new Pair<>(bigInts, bb.position());
            }
        } else if(elementType instanceof TupleType) {
            TupleType tt = (TupleType) elementType;
            Tuple[] tuples = new Tuple[arrayLen];
            for(int i = 0; i < arrayLen; i++) {
                tuples[i] = tt.decode(buffer, idx);
                idx = tt.tag;
            }
            return new Pair<>(tuples, idx);
        } else {
            return decodeObjectArray(arrayLen, buffer, idx);
        }
        throw new Error();
    }

    private Pair<Object, Integer> decodeObjectArray(int arrayLen, byte[] buffer, final int index) {

        final ArrayType elementArrayType = (ArrayType) elementType;

        Object[] dest;
        try {
            dest = (Object[]) Array.newInstance(Class.forName(elementArrayType.className), arrayLen);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        int[] offsets = new int[arrayLen];

        int idx = decodeHeads(buffer, index, offsets, dest);

        if (dynamic) {
            decodeTails(buffer, index, offsets, dest);
        }

        return new Pair<>(dest, idx);
    }

    @SuppressWarnings("unchecked")
    private int decodeHeads(final byte[] buffer, final int index, final int[] offsets, final Object[] dest) {
        final ArrayType elementArrayType = (ArrayType) elementType;
        int idx = index;
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            if (elementArrayType.dynamic) {
                offsets[i] = IntType.OFFSET_TYPE.decode(buffer, idx);
                System.out.println("offset " + offsets[i] + " @ " + idx + ", points to " + (index + offsets[i]) + ", increment to " + (idx + AbstractInt256Type.INT_LENGTH_BYTES));
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            } else {
                Pair<Object, Integer> results = elementArrayType.decodeArray(buffer, idx);
                dest[i] = results.first;
                idx = results.second;
            }
        }
        return idx;
    }

    private void decodeTails(final byte[] buffer, final int index, final int[] offsets, final Object[] dest) {
        final ArrayType et = (ArrayType) elementType;
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                dest[i] = et.decodeArray(buffer, index + offset).first;
            }
        }
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + elementType + ">(" + length + ")";
    }

    @Override
    void validate(final Object value) { // , final String expectedClassName // int stackIndex
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

    void checkLength(int actual) {
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

    // dynamics get +32 for the array length
    int getDataLen(Object value) {
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) { // always needs dynamic head?
                int staticLen = roundUp(((byte[]) value).length);
                return dynamic ? 32 + staticLen : staticLen;
            }
            if (value instanceof int[]) {
                int staticLen = ((int[]) value).length << 5; // mul 32
                return dynamic ? 32 + staticLen : staticLen;
            }
            if (value instanceof long[]) {
                int staticLen = ((long[]) value).length << 5; // mul 32
                return dynamic ? 32 + staticLen : staticLen;
            }
            if (value instanceof short[]) {
                int staticLen = ((short[]) value).length << 5; // mul 32
                return dynamic ? 32 + staticLen : staticLen;
            }
            if (value instanceof boolean[]) {
                int staticLen = ((boolean[]) value).length << 5; // mul 32
                return dynamic ? 32 + staticLen : staticLen;
            }
            if (value instanceof Number[]) {
                int staticLen = ((Number[]) value).length << 5; // mul 32
                return dynamic ? 32 + staticLen : staticLen;
            }
//            if(value instanceof com.esaulpaugh.headlong.abi.beta.util.Tuple[]) {
//                throw new Error();
////                return elementType.byteLength(value);
//            }
        }
        if (value instanceof String) { // always needs dynamic head
            return 32 + roundUp(((String) value).length());
        }
        if (value instanceof Number) {
            return 32;
        }
        if (value instanceof com.esaulpaugh.headlong.abi.beta.util.Tuple) {
            return dynamic ? 32 + elementType.byteLength(value) : elementType.byteLength(value); // TODO
        }
        if (value instanceof Object[]) {
            int len = 0;
            for (Object element : (Object[]) value) {
                len += this.elementType.byteLength(element);
                if(this.elementType.dynamic) {
                    len += 32;
                }
            }
            return dynamic ? 32 + len : len;
//            throw new AssertionError("Object array not expected here");
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    // TODO move?
    static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 - mod);
    }
}
