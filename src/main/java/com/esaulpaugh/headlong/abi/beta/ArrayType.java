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
    final Class clazz;

    private transient final boolean isString;

    ArrayType(String canonicalType, String className, T elementType, int length, boolean dynamic) {
        super(canonicalType, dynamic);
        try {
            this.clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        isString = STRING_CLASS_NAME.equals(clazz.getName());
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
    int byteLength(Object value) {
        int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            staticLen = ((boolean[]) value).length << 5; // mul 32
            break;
        case TYPE_CODE_BYTE:
            staticLen = roundUp((isString ? ((String) value).getBytes(UTF_8) : (byte[]) value).length);
            break;
        case TYPE_CODE_SHORT:
            staticLen = ((short[]) value).length << 5; // mul 32
            break;
        case TYPE_CODE_INT:
            staticLen = ((int[]) value).length << 5; // mul 32
            break;
        case TYPE_CODE_LONG:
            staticLen = ((long[]) value).length << 5; // mul 32
            break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
            staticLen = ((Number[]) value).length << 5; // mul 32
            break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            Object[] elements = (Object[]) value;
            staticLen = elementType.dynamic ? elements.length << 5 : 0; // 32 bytes per offset
            for (Object element : elements) {
                staticLen += elementType.byteLength(element);
            }
            break;
        default: throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }

        // dynamics get +32 for the array length
        return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
    }

    @Override
    @SuppressWarnings("unchecked")
    A decode(ByteBuffer bb, byte[] elementBuffer) {
//        System.out.println("A decode " + toString() + " " + ((bb.position() - 4) >>> 5) + " " + dynamic);
        final int arrayLen;
        if(dynamic) {
            arrayLen = ARRAY_LENGTH_TYPE.decode(bb, elementBuffer);
//            System.out.println("A LENGTH = " + arrayLen);
            checkDecodeLength(arrayLen, bb);
        } else {
            arrayLen = length;
        }

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (A) decodeBooleanArray(bb, arrayLen, elementBuffer);
        case TYPE_CODE_BYTE: return (A) decodeByteArray(bb, arrayLen);
        case TYPE_CODE_SHORT: return (A) decodeShortArray(bb, arrayLen, elementBuffer);
        case TYPE_CODE_INT: return (A) decodeIntArray((IntType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_LONG: return (A) decodeLongArray((LongType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_INTEGER: return (A) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_DECIMAL: return (A) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_ARRAY:  return (A) decodeObjectArray(arrayLen, bb, elementBuffer, false);
        case TYPE_CODE_TUPLE: return (A) decodeObjectArray(arrayLen, bb, elementBuffer, true);
        default: throw new Error();
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - Byte.BYTES;
        for(int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer);
            for (int j = 0; j < booleanOffset; j++) {
                if(elementBuffer[j] != 0) {
                    throw new IllegalArgumentException("illegal boolean value @ " + (bb.position() - j));
                }
            }
            byte last = elementBuffer[booleanOffset];
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

    private Object[] decodeObjectArray(int arrayLen, ByteBuffer bb, byte[] elementBuffer, boolean tupleArray) {

        final int index = bb.position(); // TODO remove eventually

        Object[] dest = tupleArray
                ? new Tuple[arrayLen]
                : (Object[]) Array.newInstance(((ArrayType) elementType).clazz, arrayLen);

        int[] offsets = new int[arrayLen];

        decodeObjectArrayHeads(bb, offsets, elementBuffer, dest);

        if(dynamic) {
            decodeObjectArrayTails(bb, index, offsets, elementBuffer, dest);
        }
        return dest;
    }

    private void decodeObjectArrayHeads(ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
//        System.out.println("A(O) heads " + ((bb.position() - 4) >>> 5) + ", " + bb.position());
        final int len = offsets.length;
        if(elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                offsets[i] = Encoder.OFFSET_TYPE.decode(bb, elementBuffer);
//                System.out.println("A(O) offset " + convertOffset(offsets[i]) + " @ " + convert(bb.position() - OFFSET_LENGTH_BYTES));
            }
        } else {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private void decodeObjectArrayTails(ByteBuffer bb, final int index, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
//        System.out.println("A(O) tails " + ((bb.position() - 4) >>> 5) + ", " + bb.position());
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
//            System.out.println("A(O) jumping to " + convert(index + offset));
            if (offset > 0) {
                if(bb.position() != index + offset) { // TODO remove this check eventually
                    System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
                    bb.position(index + offset);
                }
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    @Override
    public String toString() {
        return (dynamic ? "DYNAMIC[]" : "STATIC[]") + "<" + elementType + ">(" + length + ")";
    }

    @Override
    int typeCode() {
        return TYPE_CODE_ARRAY;
    }

    @Override
    void validate(final Object value) {
        super.validate(value);
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: checkLength(value, ((boolean[]) value).length); return;
        case TYPE_CODE_BYTE:
            byte[] bytes = isString ? ((String) value).getBytes(UTF_8) : (byte[]) value;
            checkLength(value, bytes.length);
            return;
        case TYPE_CODE_SHORT: checkLength(value, ((short[]) value).length); return;
        case TYPE_CODE_INT: validateIntArray((int[]) value); return;
        case TYPE_CODE_LONG: validateLongArray((long[]) value); return;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: validateObjectArray((Object[]) value); return;
        default: throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
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

    private void validateObjectArray(Object[] arr) {
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
