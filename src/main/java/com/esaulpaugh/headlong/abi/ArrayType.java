package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.abi.AbstractUnitType.LOG_2_UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;

public class ArrayType<T extends StackableType<?>, J> extends StackableType<J> {

    static final String BYTE_ARRAY_CLASS_NAME = byte[].class.getName();
    static final String BYTE_ARRAY_ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(byte[][].class);

    static final String STRING_CLASS_NAME = String.class.getName();
    static final String STRING_ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(String[].class);

    private static final IntType ARRAY_LENGTH_TYPE = new IntType("int32", Integer.SIZE, false);
    private static final int ARRAY_LENGTH_BYTE_LEN = UNIT_LENGTH_BYTES;

    static final int DYNAMIC_LENGTH = -1;

    final T elementType;
    final Class<?> elementClass;
    final String className;
    final String arrayClassNameStub;

    final int length;
    /* transient */ final boolean isString;

    ArrayType(String canonicalType, T elementType, Class<?> elementClass, String className, String arrayClassNameStub, int length, boolean dynamic) {
        super(canonicalType, dynamic);

        this.elementType = elementType;
        this.elementClass = elementClass;
        this.className = className;
        this.arrayClassNameStub = arrayClassNameStub;

        this.length = length;

        if(length < DYNAMIC_LENGTH) {
            throw new IllegalArgumentException("length must be non-negative or " + DYNAMIC_LENGTH + ". found: " + length);
        }

        this.isString = STRING_CLASS_NAME.equals(className);
    }

    public T getElementType() {
        return elementType;
    }

    public String getElementClassName() {
        return ClassNames.getArrayElementClassName(className());
    }

    @Override
    public String className() {
        return className;
    }

    @Override
    String arrayClassNameStub() {
        return arrayClassNameStub;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_ARRAY;
    }

    @Override
    int byteLength(Object value) {
        int staticLen;
        final StackableType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            staticLen = ((boolean[]) value).length << LOG_2_UNIT_LENGTH_BYTES; // mul 32
            break;
        case TYPE_CODE_BYTE:
            staticLen = roundLengthUp((isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value).length);
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
    int byteLengthPacked(Object value) {
        final StackableType<?> elementType = this.elementType;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN:
            return ((boolean[]) value).length; // * 1
        case TYPE_CODE_BYTE:
            return (isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value).length; // * 1
        case TYPE_CODE_SHORT:
            return ((short[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_INT:
            return ((int[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_LONG:
            return ((long[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_BIG_INTEGER:
        case TYPE_CODE_BIG_DECIMAL:
            return ((Number[]) value).length * elementType.byteLengthPacked(null);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final Object[] elements = (Object[]) value;
            int staticLen = 0;
            final int len = elements.length;
            for (int i = 0; i < len; i++) {
                staticLen += elementType.byteLengthPacked(elements[i]);
            }
            return staticLen;
        default: throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }
    }

    @Override
    public J parseArgument(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int validate(final Object value) {
        super.validate(value);

        final int staticLen;
        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: staticLen = checkLength(((boolean[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_BYTE:
            byte[] bytes = isString ? ((String) value).getBytes(CHARSET_UTF_8) : (byte[]) value;
            staticLen = roundLengthUp(checkLength(bytes.length, value));
            break;
        case TYPE_CODE_SHORT: staticLen = checkLength(((short[]) value).length, value) << LOG_2_UNIT_LENGTH_BYTES; break;
        case TYPE_CODE_INT: staticLen = validateIntArray((int[]) value); break;
        case TYPE_CODE_LONG: staticLen = validateLongArray((long[]) value); break;
        case TYPE_CODE_BIG_INTEGER: staticLen = validateBigIntegerArray((BigInteger[]) value); break;
        case TYPE_CODE_BIG_DECIMAL: staticLen = validateBigDecimalArray((BigDecimal[]) value); break;
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE: staticLen = validateObjectArray((Object[]) value); break;
        default: throw new IllegalArgumentException("unrecognized type: " + value.getClass().getName());
        }

        return dynamic ? ARRAY_LENGTH_BYTE_LEN + staticLen : staticLen;
    }

    private int validateIntArray(int[] arr) {
        IntType intType = (IntType) elementType;
        final int len = arr.length;
        checkLength(len, arr);
        int i = 0;
        try {
            for ( ; i < len; i++) {
                // validate without boxing primitive
                intType.validatePrimitiveElement(arr[i]);
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
                // validate without boxing primitive
                longType.validatePrimitiveElement(arr[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateBigIntegerArray(BigInteger[] bigIntegers) {
        final int len = bigIntegers.length;
        checkLength(len, bigIntegers);
        BigIntegerType bigIntegerType = (BigIntegerType) elementType;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                bigIntegerType.validateBigIntBitLen(bigIntegers[i]);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    private int validateBigDecimalArray(BigDecimal[] bigDecimals) {
        final int len = bigDecimals.length;
        checkLength(len, bigDecimals);
        BigDecimalType bigDecimalType = (BigDecimalType) elementType;
        final int scale = bigDecimalType.scale;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                BigDecimal element = bigDecimals[i];
                if(element.scale() != scale) {
                    throw new IllegalArgumentException("unexpected scale: " + element.scale());
                }
                bigDecimalType.validateBigIntBitLen(element.unscaledValue());
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
        return len << LOG_2_UNIT_LENGTH_BYTES; // mul 32
    }

    /**
     * For arrays of arrays or arrays of tuples only.
     */
    private int validateObjectArray(Object[] arr) {
        final int len = arr.length;
        checkLength(len, arr);
        int byteLength = elementType.dynamic ? len << LOG_2_UNIT_LENGTH_BYTES : 0; // 32 bytes per offset
        int i = 0;
        try {
            for ( ; i < len; i++) {
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
                        ClassNames.toFriendly(value.getClass().getName(), valueLength) + " not instanceof " +
                                ClassNames.toFriendly(className, expected) + ", " +
                                valueLength + " != " + expected;
                throw new IllegalArgumentException(msg);
            }
        }
        return valueLength;
    }

    @Override
    @SuppressWarnings("unchecked")
    J decode(ByteBuffer bb, byte[] elementBuffer) {
        final int arrayLen;
        if(dynamic) {
            arrayLen = ARRAY_LENGTH_TYPE.decode(bb, elementBuffer);
            final int expectedLen = this.length;
            if(expectedLen != DYNAMIC_LENGTH && arrayLen != expectedLen) {
                throw new IllegalArgumentException("array length mismatch @ "
                        + (bb.position() - ARRAY_LENGTH_BYTE_LEN)
                        + ": actual != expected: " + arrayLen + " != " + expectedLen);
            }
        } else {
            arrayLen = length;
        }

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return (J) decodeBooleanArray(bb, arrayLen, elementBuffer);
        case TYPE_CODE_BYTE: return (J) decodeByteArray(bb, arrayLen);
        case TYPE_CODE_SHORT: return (J) decodeShortArray(bb, arrayLen, elementBuffer);
        case TYPE_CODE_INT: return (J) decodeIntArray((IntType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_LONG: return (J) decodeLongArray((LongType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_INTEGER: return (J) decodeBigIntegerArray((BigIntegerType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_BIG_DECIMAL: return (J) decodeBigDecimalArray((BigDecimalType) elementType, bb, arrayLen, elementBuffer);
        case TYPE_CODE_ARRAY:  return (J) decodeObjectArray(arrayLen, bb, elementBuffer, false);
        case TYPE_CODE_TUPLE: return (J) decodeObjectArray(arrayLen, bb, elementBuffer, true);
        default: throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }
    }

    private static boolean[] decodeBooleanArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        boolean[] booleans = new boolean[arrayLen]; // elements are false by default
        final int booleanOffset = UNIT_LENGTH_BYTES - 1; // Byte.BYTES
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
        bb.position(mark + roundLengthUp(arrayLen));
        if(isString) {
            return new String(out, CHARSET_UTF_8);
        }
        return out;
    }

    private static short[] decodeShortArray(ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        short[] shorts = new short[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
            BigInteger bi = new BigInteger(elementBuffer);
            if(bi.bitLength() > Short.SIZE) { // don't treat array elements as signed
                throw new IllegalArgumentException("value not in short range");
            }
            shorts[i] = bi.shortValue();
        }
        return shorts;
    }

    private static int[] decodeIntArray(IntType intType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        int[] ints = new int[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ints[i] = getIntElement(intType, bb, elementBuffer);
        }
        return ints;
    }

    private static long[] decodeLongArray(LongType longType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        long[] longs = new long[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            longs[i] = getLongElement(longType, bb, elementBuffer);
        }
        return longs;
    }

    private static BigInteger[] decodeBigIntegerArray(BigIntegerType bigIntegerType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigInteger[] bigInts = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            bigInts[i] = getBigIntElement(bigIntegerType, bb, elementBuffer);
        }
        return bigInts;
    }

    private static BigDecimal[] decodeBigDecimalArray(BigDecimalType bigDecimalType, ByteBuffer bb, int arrayLen, byte[] elementBuffer) {
        BigDecimal[] bigDecs = new BigDecimal[arrayLen];
        final int scale = bigDecimalType.scale;
        for (int i = 0; i < arrayLen; i++) {
            bigDecs[i] = new BigDecimal(getBigIntElement(bigDecimalType, bb, elementBuffer), scale);
        }
        return bigDecs;
    }

    private static int getIntElement(AbstractUnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(elementBuffer);
        type.validateBigIntElement(bi);
        return bi.intValue();
    }

    private static long getLongElement(AbstractUnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(elementBuffer);
        type.validateBigIntElement(bi);
        return bi.longValue();
    }

    private static BigInteger getBigIntElement(AbstractUnitType<?> type, ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bigInt = new BigInteger(elementBuffer);
        type.validateBigIntElement(bigInt);
        return bigInt;
    }

    private Object[] decodeObjectArray(int arrayLen, ByteBuffer bb, byte[] elementBuffer, boolean tupleArray) {

//        final int index = bb.position(); // TODO must pass index to decodeObjectArrayTails if you want to support lenient mode

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
        final int len = offsets.length;
        if(elementType.dynamic) {
            for (int i = 0; i < len; i++) {
                offsets[i] = CallEncoder.OFFSET_TYPE.decode(bb, elementBuffer);
            }
        } else {
            for (int i = 0; i < len; i++) {
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    private static void decodeObjectArrayTails(StackableType<?> elementType, ByteBuffer bb, final int[] offsets, byte[] elementBuffer, final Object[] dest) {
        final int len = offsets.length;
        for (int i = 0; i < len; i++) {
            int offset = offsets[i];
            if (offset > 0) {
                /* OPERATES IN STRICT MODE see https://github.com/ethereum/solidity/commit/3d1ca07e9b4b42355aa9be5db5c00048607986d1 */
//                if(bb.position() != index + offset) {
//                    System.err.println(ArrayType.class.getName() + " setting " + bb.position() + " to " + (index + offset) + ", offset=" + offset);
//                    bb.position(index + offset);
//                    throw new RuntimeException();
//                }
                dest[i] = elementType.decode(bb, elementBuffer);
            }
        }
    }

    /**
     * Rounds a length up to the nearest multiple of 32. If {@code len} is already a multiple, method has no effect.
     * @param len   the length, a non-negative integer
     * @return  the rounded-up value
     */
    public static int roundLengthUp(int len) {
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }
}
