package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.beta.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.beta.util.ClassNames.toFriendly;
import static java.nio.charset.StandardCharsets.UTF_8;

class ArrayType<T extends StackableType<?>, A> extends StackableType<A> {

    static final int LOG_2_UNIT_LENGTH_BYTES = 31 - Integer.numberOfLeadingZeros(UNIT_LENGTH_BYTES);

    static final String BYTE_ARRAY_CLASS_NAME = byte[].class.getName();
    static final String BYTE_ARRAY_ARRAY_CLASS_NAME_STUB = getNameStub(byte[][].class);

    static final String STRING_CLASS_NAME = String.class.getName();
    static final String STRING_ARRAY_CLASS_NAME_STUB = getNameStub(String[].class);

    private static final IntType ARRAY_LENGTH_TYPE = new IntType("uint32", IntType.MAX_BIT_LEN, false);
    private static final int ARRAY_LENGTH_BYTE_LEN = UNIT_LENGTH_BYTES;

    static final int DYNAMIC_LENGTH = -1;

    final T elementType;
    final String className;
    final Class<?> elementClass;
    final String arrayClassNameStub;

    final int length;
    transient final boolean isString;

//    ArrayType(String canonicalType, T elementType, String elementClassName, String className, String arrayClassNameStub, int length, boolean dynamic) throws ClassNotFoundException {
//        this(canonicalType, elementType, Class.forName(elementClassName), className, arrayClassNameStub, length, dynamic);
//    }

    ArrayType(String canonicalType, T elementType, Class<?> elementClass, String className, String arrayClassNameStub, int length, boolean dynamic) {
        super(canonicalType, dynamic);
//        final String className = clazz.getName();
        this.className = className;
        isString = STRING_CLASS_NAME.equals(className);
        this.elementClass = elementClass;
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
    @SuppressWarnings("unchecked")
    A decode(ByteBuffer bb, byte[] elementBuffer) {
//        System.out.println("A decode " + toString() + " " + ((bb.position() - 4) >>> LOG_2_UNIT_LENGTH_BYTES) + " " + dynamic);
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
        default: throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
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

    private static long getLong(AbstractUnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        long longVal = new BigInteger(elementBuffer).longValueExact(); // make sure high bytes are zero
        type.validateLongElementBitLen(longVal); // validate lower 8 bytes
        return longVal;
    }

    private static BigInteger getBigInteger(AbstractUnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bigInt = new BigInteger(elementBuffer);
        type.validateBigIntBitLen(bigInt);
        return bigInt;
    }

    private Object[] decodeObjectArray(int arrayLen, ByteBuffer bb, byte[] elementBuffer, boolean tupleArray) { // 8.3%

//        final int index = bb.position();

        final StackableType<?> elementType = this.elementType;
        Object[] dest;
        if(tupleArray) {
            dest = new Tuple[arrayLen];
        } else {
            dest = (Object[]) Array.newInstance(elementClass, arrayLen); // reflection ftw
        }

        int[] offsets = new int[arrayLen];

        decodeObjectArrayHeads(elementType, bb, offsets, elementBuffer, dest);

        if(this.dynamic) {
            decodeObjectArrayTails(elementType, bb, offsets, elementBuffer, dest);
        }
        return dest;
    }

    private static void decodeObjectArrayHeads(StackableType<?>  elementType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
//        System.out.println("A(O) heads " + ((bb.position() - 4) >>> LOG_2_UNIT_LENGTH_BYTES) + ", " + bb.position());
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

    private static void decodeObjectArrayTails(StackableType<?> elementType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
//        System.out.println("A(O) tails " + ((bb.position() - 4) >>> LOG_2_UNIT_LENGTH_BYTES) + ", " + bb.position());
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
//            System.out.println("A(O) jumping to " + convert(index + offset));
            if (offset > 0) {
//                if(bb.position() != index + offset) {
//                    System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                    throw new RuntimeException();
//                }
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

//    @Override
//    void validate(final Object value) {
//        super.validate(value);
//        switch (elementType.typeCode()) {
//        case TYPE_CODE_BOOLEAN: checkLength(value, ((boolean[]) value).length); return;
//        case TYPE_CODE_BYTE:
//            byte[] bytes = isString ? ((String) value).getBytes(UTF_8) : (byte[]) value;
//            checkLength(value, bytes.length);
//            return;
//        case TYPE_CODE_SHORT: checkLength(value, ((short[]) value).length); return;
//        case TYPE_CODE_INT: validateIntArray((int[]) value); return;
//        case TYPE_CODE_LONG: validateLongArray((long[]) value); return;
//        case TYPE_CODE_BIG_INTEGER:
//        case TYPE_CODE_BIG_DECIMAL:
//        case TYPE_CODE_ARRAY:
//        case TYPE_CODE_TUPLE: validateObjectArray((Object[]) value); return;
//        default: throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
//        }
//    }

    @Override
    int byteLength(Object value) {
        int staticLen;
        final StackableType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            staticLen = ((boolean[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_BYTE:
            staticLen = roundUp((isString ? ((String) value).getBytes(UTF_8) : (byte[]) value).length);
            break;
        case TYPE_CODE_SHORT:
            staticLen = ((short[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_INT:
            staticLen = ((int[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_LONG:
            staticLen = ((long[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
            staticLen = ((Number[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final Object[] elements = (Object[]) value;
            final int len = elements.length;
            staticLen = 0;
            for (int i = 0; i < len; i++) {
                staticLen += elementType.byteLength(elements[i]);
            }
            if(elementType.dynamic) { // implies this.dynamic
                // 32 bytes per offset, 32 for array length
                return (len << LOG_2_UNIT_LENGTH_BYTES) + ARRAY_LENGTH_BYTE_LEN + staticLen;
            }
            return staticLen;
        default: throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }

        // dynamics get +32 for the array length
        return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
    }

    @Override
    int validate(final Object value) {
        super.validate(value);

        final int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: staticLen = checkLength(((boolean[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE:
            byte[] bytes = isString ? ((String) value).getBytes(UTF_8) : (byte[]) value;
            staticLen = roundUp(checkLength(bytes.length, value));
            break;
        case TYPE_CODE_SHORT: staticLen = checkLength(((short[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_INT: staticLen = validateIntArray((int[]) value); break;
        case TYPE_CODE_LONG: staticLen = validateLongArray((long[]) value); break;
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL: staticLen = validateBigNumberArray((Number[]) value); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: staticLen = validateObjectArray((Object[]) value); break;
        default: throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }

        return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
    }

    private int validateIntArray(int[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateLongArray(long[] arr) {
        LongType longType = (LongType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                longType.validateLongElementBitLen(arr[i]);
//                elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateBigNumberArray(Number[] numbers) {
        final int len = numbers.length;
        checkLength(len, numbers);
        int i = 0;
        try {
            for (; i < len; i++) {
                elementType.validate(numbers[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    /**
     * For arrays of array or arrays of tuples only.
     *
     * @param arr
     * @return
     */
    private int validateObjectArray(Object[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int byteLength = elementType.dynamic ? len << LOG_2_UNIT_LENGTH_BYTES : 0; // 32 bytes per offset
        int i = 0;
        try {
            for (; i < len; i++) {
                byteLength += elementType.validate(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return byteLength;
    }

    private int checkLength(final int valueLength, Object value) {
        final int expected = this.length;
        if(expected != DYNAMIC_LENGTH) { // -1
            if (valueLength != expected) {
                String msg =
                        toFriendly(value.getClass().getName(), valueLength) + " not instanceof " +
                                toFriendly(className, expected) + ", " +
                                valueLength + " != " + expected;
                throw new IllegalArgumentException(msg);
            }
        }
        return valueLength;
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

    static String getNameStub(Class<?> arrayClass) {
        if(arrayClass.isArray()) {
            String className = arrayClass.getName();
            if(className.charAt(0) == '[') {
                return className.substring(1);
            }
        }
        throw new IllegalArgumentException("unexpected class: " + arrayClass.getName());
    }
}
