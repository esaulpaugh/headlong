package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.Function.SELECTOR_LEN;

class Encoder {

    static final IntType OFFSET_TYPE = new IntType("uint32", IntType.MAX_BIT_LEN, false);

    private static final byte ZERO_BYTE = (byte) 0;

    private static final byte[] BOOLEAN_FALSE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
    private static final byte[] BOOLEAN_TRUE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];

    private static final byte[] NON_NEGATIVE_INT_PADDING = new byte[24];
    private static final byte[] NEGATIVE_INT_PADDING = new byte[24];

    static {
        BOOLEAN_TRUE[BOOLEAN_TRUE.length-1] = 1;
        Arrays.fill(NEGATIVE_INT_PADDING, (byte) 0xFF);
    }

    static ByteBuffer encodeFunctionCall(Function function, Tuple argsTuple) {

        System.out.println("requiredCanonicalization = " + function.requiredCanonicalization());

        final TupleType tupleType = function.paramTypes;
        final StackableType[] types = tupleType.elementTypes;
        final int expectedNumParams = types.length;

        if(argsTuple.elements.length != expectedNumParams) {
            throw new IllegalArgumentException("arguments.length <> types.size(): " + argsTuple.elements.length + " != " + types.length);
        }

        tupleType.validate(argsTuple);

        int encodingByteLen = tupleType.byteLength(argsTuple);

        System.out.println(tupleType.dynamic + " " + encodingByteLen);

        final int allocation = SELECTOR_LEN + encodingByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
        outBuffer.put(function.selector);

        insertTuple(tupleType, argsTuple, outBuffer);

        return outBuffer;
    }

    private static void insertTuple(TupleType tupleType, Tuple tuple, ByteBuffer outBuffer) {
        System.out.println("insertTuple(" + tupleType + ")");

        LinkedList<StackableType> typeList = new LinkedList<>(Arrays.asList(tupleType.elementTypes));
        LinkedList<Object> valuesList = new LinkedList<>(Arrays.asList(tuple.elements));

        encodeHeadsForTuple(typeList, valuesList, headLengthSum(tupleType, tuple), outBuffer);
        encodeTailsForTuple(typeList, valuesList, outBuffer);
    }

    private static void encodeHeadsForTuple(LinkedList<StackableType> types, LinkedList<Object> values, int headLengthSum, ByteBuffer outBuffer) {

        final int[] offset = new int[] { headLengthSum };

        Iterator<StackableType> ti = types.iterator();
        Iterator<Object> vi = values.iterator();

        while(ti.hasNext()) {

            StackableType type = ti.next();
            Object val = vi.next();

            encodeHead(type, val, outBuffer, offset);

            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    private static void encodeHead(StackableType paramType, Object value, ByteBuffer dest, int[] offset) {
        if(value instanceof String) {
            insertStringHead(paramType, (String) value, dest, offset);
        } else if(value.getClass().isArray()) {
            if(paramType.dynamic) {
                insertOffset(offset, paramType, value, dest);
            } else {
                encodeArrayStatic((ArrayType) paramType, value, dest);
            }
        } else if(value instanceof Tuple) {
            insertTupleHead((TupleType) paramType, (Tuple) value, dest, offset);
        } else if (value instanceof Number) {
            if(value instanceof BigInteger) {
                insertInt(((BigInteger) value), dest);
            } else if(value instanceof BigDecimal) {
                insertInt(((BigDecimal) value).unscaledValue(), dest);
            } else {
                insertInt(((Number) value).longValue(), dest);
            }
        } else if(value instanceof Boolean) {
            insertBool((boolean) value, dest);
        }
    }

    private static void insertTupleHead(TupleType tupleType, Tuple tuple, ByteBuffer dest, int[] offset) {
        if(tupleType.dynamic) {
            insertOffset(offset, tupleType, tuple, dest);
        } else {
            insertTuple(tupleType, tuple, dest);
        }
    }

    private static void insertLength(int length, ByteBuffer dest) {
        System.out.println("insertLength(" + length + ")");
        insertInt(length, dest);
    }

    private static void insertOffset(final int[] offset, StackableType paramType, Object object, ByteBuffer dest) {
        System.out.println("\noffset[0] is " + offset[0]);
        insertInt(offset[0], dest);
        System.out.println("offset[0] = " + offset[0] + " + " + paramType.byteLength(object) + " - " + 32);
        offset[0] += paramType.byteLength(object); //  - 32
        System.out.println("aka " + offset[0] + ", " + (offset[0] >>> 5));
    }

    private static void encodeTailsForTuple(List<StackableType> types, List<Object> values, ByteBuffer outBuffer) {
        Iterator<StackableType> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            StackableType type = ti.next();
            encodeTail(type, vi.next(), outBuffer);
        }
    }

    /**
     * No Booleans or Numbers expected, except as arrays
     *
     * @param type
     * @param value
     * @param dest
     */
    private static void encodeTail(StackableType type, Object value, ByteBuffer dest) {
        if(value instanceof String) {
            insertBytesTail(((String) value).getBytes(StandardCharsets.UTF_8), dest, true);
        } else if(value.getClass().isArray()) {
            encodeArrayDynamic((ArrayType) type, value, dest);
        } else if(value instanceof Tuple) {
            insertTuple((TupleType) type, (Tuple) value, dest);
        } else {
            throw new Error("unexpected type: " + value.getClass().getName());
        }
    }

    // ----------------------------------------------

    private static void insertStringHead(StackableType paramType, String string, ByteBuffer dest, int[] offset) {
        // strings always dynamic
        insertOffset(offset, paramType, string, dest);
    }

    private static void encodeArrayStatic(ArrayType arrayType, Object value, ByteBuffer dest) {
        if (value instanceof Object[]) {
            if(value instanceof BigInteger[]) {
                insertBigIntegers((BigInteger[]) value, dest);
            } else if(value instanceof BigDecimal[]) {
                insertBigDecimals((BigDecimal[]) value, dest);
            } else {
                final StackableType elementType = arrayType.elementType;
                for(Object e : (Object[]) value) {
                    encodeHead(elementType, e, dest, null);
                }
            }
        } else if (value instanceof byte[]) {
            insertBytes((byte[]) value, dest);
        } else if (value instanceof short[]) {
            insertShorts((short[]) value, dest);
        } else if (value instanceof int[]) {
            insertInts((int[]) value, dest);
        } else if (value instanceof long[]) {
            insertLongs((long[]) value, dest);
        } else if(value instanceof boolean[]) {
            insertBooleans((boolean[]) value, dest);
        } else {
            throw new IllegalArgumentException("unexpected array type: " + value.getClass().getName());
        }
    }

    private static void encodeArrayDynamic(ArrayType arrayType, Object value, ByteBuffer dest) {
        if (value instanceof Object[]) {
            if(value instanceof BigInteger[]) {
                insertBigIntegersTail((BigInteger[]) value, dest, arrayType.dynamic);
            } else if(value instanceof BigDecimal[]) {
                insertBigDecimalsTail((BigDecimal[]) value, dest, arrayType.dynamic);
            } else {
                Object[] objects = (Object[]) value;
                final StackableType elementType = arrayType.elementType;
                if(arrayType.dynamic) { // if parent is dynamic
                    insertLength(objects.length, dest);
                }
                if(elementType.dynamic) { // if elements are dynamic
                    final int[] offset = new int[] { objects.length << 5 }; // mul 32 (0x20)
                    for (Object element : objects) {
                        insertOffset(offset, elementType, element, dest);
                    }
                }
                for (Object element : objects) {
                    encodeTail(elementType, element, dest);
                }
            }
        } else if (value instanceof byte[]) {
            insertBytesTail((byte[]) value, dest, arrayType.dynamic);
        } else if (value instanceof int[]) {
            insertIntsTail((int[]) value, dest, arrayType.dynamic);
        } else if (value instanceof long[]) {
            insertLongsTail((long[]) value, dest, arrayType.dynamic);
        } else if (value instanceof short[]) {
            insertShortsTail((short[]) value, dest, arrayType.dynamic);
        } else if(value instanceof boolean[]) {
            insertBooleansTail((boolean[]) value, dest, arrayType.dynamic);
        } else {
            throw new Error("unexpected array type: " + value.getClass().getName());
        }
    }

    // ========================================

    private static int headLengthSum(TupleType tupleType, Tuple tuple) {
        StackableType[] elementTypes = tupleType.elementTypes;
        Object[] elements = tuple.elements;
        int headLengths = 0;
        final int n = elementTypes.length;
        for (int i = 0; i < n; i++) {
            StackableType t = elementTypes[i];
//            int byteLen = t.byteLength(arguments[i]);
//            System.out.print(arguments[i] + " --> " + byteLen + ", ");
            if(t.dynamic) {
                headLengths += 32;
                System.out.println("dynamic");
            } else {
                headLengths += t.byteLength(elements[i]);
                System.out.println("static");
            }
        }

        System.out.println("**************** " + headLengths);

        return headLengths;
    }

    // -------------------------------------------------------------------------------------------------

    private static void insertBooleansTail(boolean[] bools, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bools.length, dest);
        insertBooleans(bools, dest);
    }

    private static void insertBytesTail(byte[] bytes, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bytes.length, dest);
        insertBytes(bytes, dest);
    }

    private static void insertShortsTail(short[] shorts, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(shorts.length, dest);
        insertShorts(shorts, dest);
    }

    private static void insertIntsTail(int[] ints, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(ints.length, dest);
        insertInts(ints, dest);
    }

    private static void insertLongsTail(long[] longs, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(longs.length, dest);
        insertLongs(longs, dest);
    }

    private static void insertBigIntegersTail(BigInteger[] bigInts, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bigInts.length, dest);
        insertBigIntegers(bigInts, dest);
    }

    private static void insertBigDecimalsTail(BigDecimal[] bigDecs, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bigDecs.length, dest);
        insertBigDecimals(bigDecs, dest);
    }

    private static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        }
    }

    private static void insertBytes(byte[] bytes, ByteBuffer dest) {
        final int len = bytes.length;
        if(len > 0) { // don't pad empty array to 32 bytes
            dest.put(bytes);
            final int remainder = 32 - (len & 31); // 32 - (len % 32)
            for (int i = 0; i < remainder; i++) {
                dest.put(ZERO_BYTE);
            }
        }
    }

    private static void insertShorts(short[] shorts, ByteBuffer dest) {
        for (short e : shorts) {
            insertInt(e, dest);
        }
    }

    private static void insertInts(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

    private static void insertLongs(long[] longs, ByteBuffer dest) {
        for (long e : longs) {
            insertInt(e, dest);
        }
    }

    private static void insertBigIntegers(BigInteger[] bigInts, ByteBuffer dest) {
        for (BigInteger e : bigInts) {
            insertInt(e, dest);
        }
    }

    private static void insertBigDecimals(BigDecimal[] bigDecs, ByteBuffer dest) {
        for (BigDecimal e : bigDecs) {
            insertInt(e.unscaledValue(), dest);
        }
    }

    // ------------------------------------------------------------------------------

    private static void insertInt(long val, ByteBuffer dest) {
        dest.put(val < 0 ? NEGATIVE_INT_PADDING : NON_NEGATIVE_INT_PADDING);
        dest.putLong(val);
    }

    private static void insertInt(BigInteger bigGuy, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        final int lim = 32 - arr.length;
        for (int i = 0; i < lim; i++) {
            dest.put((byte) 0);
        }
        dest.put(arr);
    }

    private static void insertBool(boolean bool, ByteBuffer dest) {
        insertInt(bool ? 1L : 0L, dest);
    }
}
