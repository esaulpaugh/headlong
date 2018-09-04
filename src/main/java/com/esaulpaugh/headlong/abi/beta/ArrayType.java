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
    private static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", ARRAY_LENGTH_BYTE_LEN, false);

    final T elementType;
    private final int length;
    private final String className;
    private final String arrayClassNameStub;

    ArrayType(String canonicalType, String className, String arrayClassNameStub, T elementType, int length, boolean dynamic) {
        super(canonicalType, dynamic);
        this.className = className;
        this.arrayClassNameStub = arrayClassNameStub;
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
    String className() {
        return className;
    }

    @Override
    String arrayClassNameStub() {
        return arrayClassNameStub;
    }

    @Override
    int byteLength(Object value) {
        // dynamics get +32 for the array length
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) {
                int staticLen = roundUp(((byte[]) value).length);
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof int[]) {
                int staticLen = ((int[]) value).length << 5; // mul 32
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof long[]) {
                int staticLen = ((long[]) value).length << 5; // mul 32
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof short[]) {
                int staticLen = ((short[]) value).length << 5; // mul 32
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof boolean[]) {
                int staticLen = ((boolean[]) value).length << 5; // mul 32
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof Number[]) {
                int staticLen = ((Number[]) value).length << 5; // mul 32
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
            }
            if (value instanceof Object[]) {
                int len = 0;
                for (Object element : (Object[]) value) {
                    len += this.elementType.byteLength(element);
                    if(this.elementType.dynamic) {
                        len += ARRAY_LENGTH_BYTE_LEN;
                    }
                }
                return dynamic ? ARRAY_LENGTH_BYTE_LEN + len : len;
            }
        }
        if (value instanceof String) { // always needs dynamic head
            return ARRAY_LENGTH_BYTE_LEN + roundUp(((String) value).length());
        }
        if (value instanceof Number) {
            return ARRAY_LENGTH_BYTE_LEN;
        }
        if (value instanceof Tuple) {
            return dynamic ? ARRAY_LENGTH_BYTE_LEN + elementType.byteLength(value) : elementType.byteLength(value);
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
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
            if(elementType instanceof ShortType) {
                short[] shorts = new short[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    shorts[i] = new BigInteger(thirtyTwo).shortValueExact(); // validates that value is in short range
                }
                return new Pair<>(shorts, bb.position());
            } else if(elementType instanceof IntType) {
                final IntType intType = (IntType) elementType;
                int[] ints = new int[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    long longVal = new BigInteger(thirtyTwo).longValueExact(); // throw on overflow
                    intType.validateLongBitLen(longVal);
                    ints[i] = (int) longVal;
                }
                return new Pair<>(ints, bb.position());
            } else if(elementType instanceof LongType) {
                long[] longs = new long[arrayLen];
                for(int i = 0; i < arrayLen; i++) {
                    bb.get(thirtyTwo);
                    long longVal = new BigInteger(thirtyTwo).longValueExact(); // throw on overflow
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
            } else if(elementType instanceof BooleanType) {
                return decodeBooleanArray(arrayLen, bb, thirtyTwo);
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
                offsets[i] = Encoder.OFFSET_TYPE.decode(buffer, idx);
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
            if (value instanceof byte[]) {
                checkLength(((byte[]) value).length);
            } else if (value instanceof int[]) {
                validateIntArray((int[]) value);
            } else if (value instanceof long[]) {
                validateLongArray((long[]) value);
            } else if (value instanceof short[]) {
                checkLength(((short[]) value).length);
            } else if (value instanceof boolean[]) {
                checkLength(((boolean[]) value).length);
            } else if (value instanceof Object[]) { // includes BigInteger[]
                Object[] arr = (Object[]) value;
                final int len = arr.length;
                checkLength(len);
                int i = 0;
                try {
                    for (; i < len; i++) {
                        elementType.validate(arr[i]);
                    }
                } catch (IllegalArgumentException | NullPointerException re) {
                    throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
                }
            } else {
                throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
            }
        } else if(value instanceof String) {
            checkLength(((String) value).getBytes(StandardCharsets.UTF_8).length);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }
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

    private void checkLength(int actual) {
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

    private static int roundUp(int len) {
//        int mod = len % AbstractInt256Type.INT_LENGTH_BYTES;
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }
}
