package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.beta.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.beta.util.ClassNames.toFriendly;
import static java.nio.charset.StandardCharsets.UTF_8;

class ArrayType<T extends StackableType, A> extends StackableType<A> {

    static final String STRING_CLASS_NAME = String.class.getName();

    private static final int ARRAY_LENGTH_BYTE_LEN = IntType.MAX_BIT_LEN;
    private static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", ARRAY_LENGTH_BYTE_LEN, false);

    static final int DYNAMIC_LENGTH = -1;

    final T elementType;
    final int length;
//    final String className;
    final Class clazz;
    private final String arrayClassNameStub;

    private transient final boolean isString;

    ArrayType(String canonicalType, String className, String arrayClassNameStub, T elementType, int length, boolean dynamic) {
        super(canonicalType, dynamic);
//        this.className = className;
        try {
            this.clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        isString = STRING_CLASS_NAME.equals(clazz.getName());
        this.arrayClassNameStub = arrayClassNameStub;
        this.elementType = elementType;
        this.length = length;

        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }
    }

    @Override
    String className() {
        return clazz.getName();
    }

    @Override
    String arrayClassNameStub() {
        return arrayClassNameStub;
    }

    @Override
    int byteLength(Object value) {
        // dynamics get +32 for the array length
        if(value.getClass().isArray()) {
            if (value instanceof byte[]) { // TODO use switch(int) and unchecked cast instead of repeated instanceof
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
            String string = (String) value;
            byte[] bytes = string.getBytes(UTF_8);
            return ARRAY_LENGTH_BYTE_LEN + roundUp(bytes.length);
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
    A decode(ByteBuffer bb, byte[] elementBuffer) {
        final int arrayLen;
        if(dynamic) {
            arrayLen = ARRAY_LENGTH_TYPE.decode(bb, elementBuffer);
            checkDecodeLength(arrayLen, bb);
        } else {
            arrayLen = length;
        }
        if(elementType instanceof AbstractUnitType) { // TODO use switch(int) and unchecked cast instead of repeated instanceof
            if(elementType instanceof ByteType) {
                return (A) decodeByteArray(bb, arrayLen);
            }
            if (elementType instanceof ShortType) {
                return (A) decodeShortArray(bb, arrayLen, elementBuffer);
            }
            if (elementType instanceof IntType) {
                return (A) decodeIntArray((IntType) elementType, bb, arrayLen, elementBuffer);
            }
            if (elementType instanceof LongType) {
                return (A) decodeLongArray((LongType) elementType, bb, arrayLen, elementBuffer);
            }
            if (elementType instanceof BigIntegerType) {
                return (A) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, elementBuffer);
            }
            if (elementType instanceof BigDecimalType) {
                return (A) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, elementBuffer);
            }
            if (elementType instanceof BooleanType) {
                return (A) decodeBooleanArray(bb, arrayLen);
            }
            throw new Error();
        } else if(elementType instanceof TupleType) {
            return (A) decodeTupleArray((TupleType) elementType, bb, arrayLen, elementBuffer);
        } else {
            return (A) decodeObjectArray(arrayLen, bb, elementBuffer);
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - Byte.BYTES;
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
                throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - UNIT_LENGTH_BYTES));
            }
        }
        return booleans;
    }

    private Object decodeByteArray(ByteBuffer bb, int arrayLen) {
        final int mark = bb.position();
        byte[] out = new byte[arrayLen];
        bb.get(out);
        bb.position(mark + roundUp(arrayLen));
        if(isString) {
            return new String(out, UTF_8);
        }
        return out;
    }

    private static short[] decodeShortArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        short[] shorts = new short[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
            shorts[i] = new BigInteger(elementBuffer).shortValueExact(); // validates that value is in short range
        }
        return shorts;
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = (int) getLong(intType, bb, elementBuffer);
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = getLong(longType, bb, elementBuffer);
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = getBigInteger(bigIntegerType, bb, elementBuffer);
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(getBigInteger(bigDecimalType, bb, elementBuffer), scale);
        }
        return bigDecs;
    }

    private static Tuple[] decodeTupleArray(TupleType tupleType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        Tuple[] tuples = new Tuple[arrayLen];
        for(int i = 0; i < arrayLen; i++) {
            tuples[i] = tupleType.decode(bb, elementBuffer);
        }
        return tuples;
    }

    private static long getLong(AbstractUnitType type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        long longVal = new BigInteger(elementBuffer).longValueExact(); // make sure high bytes are zero
        type.validateLongBitLen(longVal); // validate lower 8 bytes
        return longVal;
    }

    private static BigInteger getBigInteger(AbstractUnitType type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bigInt = new BigInteger(elementBuffer);
        type.validateBigIntBitLen(bigInt);
        return bigInt;
    }

    private Object[] decodeObjectArray(int arrayLen, ByteBuffer bb, byte[] elementBuffer) {

        final int index = bb.position(); // TODO remove eventually

        final ArrayType elementArrayType = (ArrayType) elementType;

        Object[] dest = (Object[]) Array.newInstance(elementArrayType.clazz, arrayLen);

//        Object[] dest;
//        try {
//            dest = (Object[]) Array.newInstance(Class.forName(elementArrayType.className), arrayLen);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        int[] offsets = new int[arrayLen];

        decodeArrayHeads(elementArrayType, bb, offsets, elementBuffer, dest);

        if(dynamic) {
            decodeArrayTails(elementArrayType, bb, index, offsets, elementBuffer, dest);
        }
        return dest;
    }

    private static void decodeArrayHeads(ArrayType elementArrayType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int len = offsets.length;
        if(elementArrayType.dynamic) {
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoder.OFFSET_TYPE.decode(bb, elementBuffer);
            }
        } else {
            for (int i = 0; i < len; i++) {
                dest[i] = elementArrayType.decode(bb, elementBuffer);
            }
        }
    }

    private static void decodeArrayTails(ArrayType elementArrayType, ByteBuffer bb, final int index, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                if(bb.position() != index + offset) { // TODO remove this check eventually
                    System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
                    bb.position(index + offset);
                }
                dest[i] = elementArrayType.decode(bb, elementBuffer);
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

        if(value.getClass().isArray()) { // TODO use switch(int) and unchecked cast instead of repeated instanceof
            if (value instanceof byte[]) {
                byte[] arr = (byte[]) value;
                checkLength(arr, arr.length);
            } else if (value instanceof int[]) {
                validateIntArray((int[]) value);
            } else if (value instanceof long[]) {
                validateLongArray((long[]) value);
            } else if (value instanceof short[]) {
                short[] arr = (short[]) value;
                checkLength(arr, arr.length);
            } else if (value instanceof boolean[]) {
                boolean[] arr = (boolean[]) value;
                checkLength(arr, arr.length);
            } else if (value instanceof Object[]) { // includes BigInteger[]
                Object[] arr = (Object[]) value;
                final int len = arr.length;
                checkLength(arr, len);
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
            byte[] arr = ((String) value).getBytes(UTF_8);
            checkLength(arr, arr.length);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }
    }

    private void validateIntArray(int[] arr) {
        final int len = arr.length;
        checkLength(arr, len);
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
        checkLength(arr, len);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void checkLength(Object value, int valueLength) {
        final int expected = this.length;
        if(expected == DYNAMIC_LENGTH) { // -1
            return;
        }
        if(valueLength != expected) {
            String msg =
                    toFriendly(value.getClass().getName(), valueLength)+ " not instanceof " +
                    toFriendly(clazz.getName(), expected) + ", " +
                    valueLength + " != " + expected;
            throw new IllegalArgumentException(msg);
        }
    }

    private void checkDecodeLength(int valueLength, ByteBuffer bb) {
        final int expected = this.length;
        if(expected == DYNAMIC_LENGTH) { // -1
            return;
        }
        if(valueLength != expected) {
            throw new IllegalArgumentException("array length mismatch @ " + (bb.position() - ARRAY_LENGTH_BYTE_LEN) + ": actual != expected: " + valueLength + " != " + expected);
        }
    }

    private static int roundUp(int len) {
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }
}
