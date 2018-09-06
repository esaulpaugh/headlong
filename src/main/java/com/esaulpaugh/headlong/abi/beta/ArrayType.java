package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.esaulpaugh.headlong.rlp.util.Strings.CHARSET_UTF_8;

class ArrayType<T extends StackableType, A> extends DynamicType<A> {

    private static final String STRING_CLASS_NAME = String.class.getName();

    private static final int ARRAY_LENGTH_BYTE_LEN = IntType.MAX_BIT_LEN;
    private static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", ARRAY_LENGTH_BYTE_LEN, false);

    static final int DYNAMIC_LENGTH = -1;

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

        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
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
                Object[] elements = (Object[]) value;
                int len = elementType.dynamic ? elements.length << 5 : 0; // 32 bytes per offset
                for (Object element : elements) {
                    len += elementType.byteLength(element);
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
    @SuppressWarnings("unchecked")
    A decodeDynamic(final byte[] buffer, final int index, final int[] returnIndex) {
        final int arrayLen;
        final int idx;
        if(dynamic) {
            arrayLen = ARRAY_LENGTH_TYPE.decode(buffer, index);
            checkLength(arrayLen);
            idx = index + ARRAY_LENGTH_BYTE_LEN;
        } else {
            arrayLen = length;
            idx = index;
        }
        if(elementType instanceof ByteType) {
            byte[] out = new byte[arrayLen];
            System.arraycopy(buffer, idx, out, 0, arrayLen);
            returnIndex[0] = idx + roundUp(arrayLen);
            if(STRING_CLASS_NAME.equals(className)) {
                return (A) new String(out, CHARSET_UTF_8);
            }
            return (A) out;
        }
        if(elementType instanceof AbstractInt256Type) {
            final ByteBuffer bb = ByteBuffer.wrap(buffer, idx, arrayLen << 5); // mul 32
            if (elementType instanceof BooleanType) {
                return (A) decodeBooleanArray(bb, arrayLen, returnIndex);
            }
            final byte[] elementBuffer = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
            if (elementType instanceof ShortType) {
                return (A) decodeShortArray(bb, arrayLen, elementBuffer, returnIndex);
            }
            if (elementType instanceof IntType) {
                return (A) decodeIntArray((IntType) elementType, bb, arrayLen, elementBuffer, returnIndex);
            }
            if (elementType instanceof LongType) {
                return (A) decodeLongArray((LongType) elementType, bb, arrayLen, elementBuffer, returnIndex);
            }
            if (elementType instanceof BigIntegerType) {
                return (A) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, elementBuffer, returnIndex);
            }
            if (elementType instanceof BigDecimalType) {
                return (A) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, elementBuffer, returnIndex);
            }
            throw new Error();
        } else if(elementType instanceof TupleType) {
            return (A) decodeTupleArray((TupleType) elementType, buffer, idx, arrayLen, returnIndex);
        } else {
            return (A) decodeObjectArray(arrayLen, buffer, idx, returnIndex);
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, int[] returnIndex) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = AbstractInt256Type.INT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < arrayLen; i++) {
            for (int j = 0; j < booleanOffset; j++) {
                if(bb.get() != 0) {
                    throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - j));
                }
            }
            byte last = bb.get();
            if(last == 1) {
                booleans[i] = true;
            } else if(last != 0) {
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - AbstractInt256Type.INT_LENGTH_BYTES));
            }
        }
        returnIndex[0] = bb.position();
        return booleans;
    }

    private static short[] decodeShortArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer, int[] returnIndex) {
        short[] shorts = new short[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer);
            shorts[i] = new BigInteger(elementBuffer).shortValueExact(); // validates that value is in short range
        }
        returnIndex[0] = bb.position();
        return shorts;
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] elementBuffer, int[] returnIndex) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = (int) getLong(intType, bb, elementBuffer);
        }
        returnIndex[0] = bb.position();
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] elementBuffer, int[] returnIndex) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = getLong(longType, bb, elementBuffer);
        }
        returnIndex[0] = bb.position();
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] elementBuffer, int[] returnIndex) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = getBigInteger(bigIntegerType, bb, elementBuffer);
        }
        returnIndex[0] = bb.position();
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] elementBuffer, int[] returnIndex) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(getBigInteger(bigDecimalType, bb, elementBuffer), scale);
        }
        returnIndex[0] = bb.position();
        return bigDecs;
    }

    private static Tuple[] decodeTupleArray(TupleType tupleType, byte[] buffer, int idx, int arrayLen, int[] returnIndex) {
        Tuple[] tuples = new Tuple[arrayLen];
        returnIndex[0] = idx;
        for(int i = 0; i < arrayLen; i++) {
            tuples[i] = tupleType.decodeDynamic(buffer, returnIndex[0], returnIndex);
        }
        return tuples;
    }

    private static long getLong(AbstractInt256Type type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        long longVal = new BigInteger(elementBuffer).longValueExact(); // make sure high bytes are zero
        type.validateLongBitLen(longVal); // validate lower 8 bytes
        return longVal;
    }

    private static BigInteger getBigInteger(AbstractInt256Type type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bigInt = new BigInteger(elementBuffer);
        type.validateBigIntBitLen(bigInt);
        return bigInt;
    }

    private Object[] decodeObjectArray(int arrayLen, byte[] buffer, final int index, int[] returnIndex) {

        final ArrayType elementArrayType = (ArrayType) elementType;

        Object[] dest;
        try {
            dest = (Object[]) Array.newInstance(Class.forName(elementArrayType.className), arrayLen);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        int[] offsets = new int[arrayLen];

        decodeArrayHeads(elementArrayType, buffer, index, offsets, dest, returnIndex);

        if(dynamic) {
            decodeArrayTails(elementArrayType, buffer, index, offsets, dest, returnIndex);
        }
        return dest;
    }

    private static void decodeArrayHeads(ArrayType elementArrayType, final byte[] buffer, final int index, final int[] offsets, final Object[] dest, int[] returnIndex) {
        int idx = index;
        final int len = offsets.length;
        if(elementArrayType.dynamic) {
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoder.OFFSET_TYPE.decode(buffer, idx);
                idx += AbstractInt256Type.INT_LENGTH_BYTES;
            }
            returnIndex[0] = idx;
        } else {
            returnIndex[0] = idx;
            for (int i = 0; i < len; i++) {
                dest[i] = elementArrayType.decodeDynamic(buffer, returnIndex[0], returnIndex);
            }
        }
    }

    private static void decodeArrayTails(ArrayType elementArrayType, final byte[] buffer, final int index, final int[] offsets, final Object[] dest, int[] returnIndex) {
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                dest[i] = elementArrayType.decodeDynamic(buffer, index + offset, returnIndex);
            }
        }
    }

    @Override
    public String toString() {
        return dynamic ? "DYNAMIC[]" : "STATIC[]"
                + "<" + elementType + ">(" + length + ")";
    }

    @Override
    void validate(final Object value) {
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
            return;
        }
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: actual != expected: " + actual + " != " + expected);
        }
    }

    private static int roundUp(int len) {
//        int mod = len % AbstractInt256Type.INT_LENGTH_BYTES;
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }
}
