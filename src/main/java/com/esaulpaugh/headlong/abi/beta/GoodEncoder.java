package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.type.TupleType;
import com.esaulpaugh.headlong.abi.beta.type.array.ArrayType;
import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.type.integer.AbstractInt256Type;
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

public class GoodEncoder {

    private static final byte[] BOOLEAN_FALSE;
    private static final byte[] BOOLEAN_TRUE;

    static {
        BOOLEAN_FALSE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
        BOOLEAN_TRUE = new byte[AbstractInt256Type.INT_LENGTH_BYTES];
        BOOLEAN_TRUE[BOOLEAN_TRUE.length - 1] = 1;
    }

    private static final byte[] PADDING_192_BITS = new byte[24];

    public static ByteBuffer encodeFunctionCall(Function function, Object[] arguments) {

        System.out.println("requiredCanonicalization = " + function.requiredCanonicalization());

        final TupleType tupleType = function.paramTypes;
        final Tuple tuple = new Tuple(arguments);
        final StackableType[] types = tupleType.memberTypes;
        final int expectedNumParams = types.length;

        if(arguments.length != expectedNumParams) {
            throw new IllegalArgumentException("arguments.length <> types.size(): " + arguments.length + " != " + types.length);
        }

//        TupleType.checkTypes(types, arguments);

        tupleType.validate(tuple);

        int sum = getHeadLengthSum(types, arguments);

        int encodingByteLen = tupleType.byteLength(tuple);

//        encodingByteLen -= 32; // top level dynamic tuple needs no offset?

        System.out.println(tupleType.dynamic + " " + encodingByteLen);

        final int allocation = SELECTOR_LEN + encodingByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
        outBuffer.put(function.selector);

        insertTuple(tupleType, tuple, sum, outBuffer);

        return outBuffer;
    }

    private static void insertTuple(TupleType tupleType, Tuple tuple, int offset, ByteBuffer outBuffer) {
        System.out.println("insertTuple(" + tupleType + ")");

        Object[] values = tuple.elements;
        List<StackableType> typeList = new LinkedList<>(Arrays.asList(tupleType.memberTypes));
        List<Object> valuesList = new LinkedList<>(Arrays.asList(values));
//        int[] offset = new int[] { tupleType.overhead(tuple) };

        encodeHeadsForTuple(typeList, valuesList, offset, outBuffer);
        encodeTailsForTuple(typeList, valuesList, outBuffer);
    }

    private static void encodeHeadsForTuple(List<StackableType> types, List<Object> values, int sum, ByteBuffer outBuffer) {

//        int sum = 0;

        int[] offset = new int[] { sum };


//        int[] dataLen = new int[] { -1 };

//        int[] offset = new int[] { -1 };
//        offset[0] = sum;

//        int[] dataLengths = new int[values.size()];
//        dataLengths(types.toArray(StackableType.EMPTY_TYPE_ARRAY), values.toArray(), dataLengths);

//        int dataSum = 0;
//        for (int i : dataLengths) {
//            dataSum += i;
//        }

//        int dataSum = dataLengths[0];

//        int i = 0;

//        int mark;
        Iterator<StackableType> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            StackableType type = ti.next();
            Object val = vi.next();

//            mark = outBuffer.position();
            encodeHead(type, val, outBuffer, offset); // , type.dynamic
//            sum += outBuffer.position() - mark;

//            offset[0] = type.overhead(val);

            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    private static void encodeHead(StackableType paramType, Object value, ByteBuffer dest, int[] offset) { // , boolean dynamic
//        boolean dynamic = paramType.dynamic;
        if(value instanceof String) { // dynamic
            insertStringHead(paramType, (String) value, dest, offset);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsHead(paramType, (BigInteger[]) value, dest, offset);
                } else if(value instanceof BigDecimal[]) {
                    insertBigDecimalsHead(paramType, (BigDecimal[]) value, dest, offset);
                } else {
                    Object[] elements = (Object[]) value;
//                    ArrayType arrayType = (ArrayType) paramType;
                    final StackableType elementType = ((ArrayType) paramType).elementType;
                    if(paramType.dynamic) {
                        insertOffset(offset, paramType, value, dest);
                    } else {
                        for(Object e : elements) {
                            encodeHead(elementType, e, dest, offset);
                        }
                    }
//                    int[] headLengths = getHeadLengths(((ArrayType) paramType).elementType, elements);
//                    insertArrayOffsets(paramType, elements, dest, tailOffsets[0]); //
                }
            } else if (value instanceof byte[]) {
                insertBytesHead(paramType, (byte[]) value, dest, offset);
            } else if (value instanceof int[]) {
                insertIntsHead(paramType, (int[]) value, dest, offset);
            } else if (value instanceof long[]) {
                insertLongsHead(paramType, (long[]) value, dest, offset);
            } else if (value instanceof short[]) {
                insertShortsHead(paramType, (short[]) value, dest, offset);
            } else if(value instanceof boolean[]) {
                insertBooleansHead(paramType, (boolean[]) value, dest, offset);
            }
        } else if(value instanceof Tuple) {
//            throw new Error(); // TODO TEST
//            insertTuple((Tuple) paramType, (Tuple) value, offset[0], dest);
            insertTupleHead(paramType, (Tuple) value, dest, offset);
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

    private static void insertTupleHead(StackableType t, Tuple tuple, ByteBuffer dest, int[] offset) {
        TupleType tupleType;
        try {
            tupleType = (TupleType) t;
        } catch (ClassCastException cce) {
            throw new RuntimeException(cce);
        }
        if(tupleType.dynamic) {
            insertOffset(offset, tupleType, tuple, dest);
//            insertInt(tailOffset, dest);
        } else {
            int headLengths = getHeadLengthSum(tupleType.memberTypes, tuple.elements);
            insertTuple(tupleType, tuple, headLengths, dest);
        }
    }

    private static void insertLength(int length, ByteBuffer dest) {
        System.out.println("insertLength(" + length + ")");
        insertInt(length, dest); // 0x3333333333000000L +
    }

    private static void insertOffset(final int[] offset, StackableType paramType, Object object, ByteBuffer dest) {
//        if(paramType.dynamic) {
            System.out.println("\noffset[0] is " + offset[0]);
            insertInt(offset[0], dest); // 0xFFFFFFFFFF000000L +
//            offset[0] = offset[0] - 32 + paramType.byteLength(object);
//            offset[0] = paramType.overhead(object);
//            System.out.println("overhead " + offset[0]);


            System.out.println("offset[0] = " + offset[0] + " + " + paramType.byteLength(object) + " - " + 32);
            offset[0] += paramType.byteLength(object); //  - 32
            System.out.println("aka " + offset[0] + ", " + (offset[0] >>> 5));
//        }
//        else {
////            insertArrayStatic(bools, dest);
//            for (Object object : objects) {
////                encodeTail(paramType, object, dest);
//            }
//        }
//        return Integer.MIN_VALUE;
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
     * Only for dynamic types -- no Booleans, no Numbers
     *
     * @param paramType
     * @param value
     * @param dest
     */
    private static void encodeTail(StackableType paramType, Object value, ByteBuffer dest) {
//        if(!paramType.dynamic) {
//            throw new AssertionError("statics not expected");
//        }
        final boolean dynamic = paramType.dynamic;
        if(value instanceof String) { // dynamic
            insertBytesTail(((String) value).getBytes(StandardCharsets.UTF_8), dest, dynamic);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsTail((BigInteger[]) value, dest, dynamic);
                } else if(value instanceof BigDecimal[]) {
                    insertBigDecimalsTail((BigDecimal[]) value, dest, dynamic);
                } else {
                    Object[] objects = (Object[]) value;
                    int[] offset = new int[] { objects.length << 5 }; // mul 32 (0x20)
                    final StackableType elementType = ((ArrayType) paramType).elementType;
                    if(dynamic) {
                        insertLength(objects.length, dest);
                    }
                    if(elementType.dynamic) {
                        for (Object element : objects) {
                            insertOffset(offset, elementType, element, dest);
                        }
                    }
                    for (Object element : objects) {
                        encodeTail(elementType, element, dest);
                    }
                }
            } else if (value instanceof byte[]) {
                insertBytesTail((byte[]) value, dest, dynamic);
            } else if (value instanceof int[]) {
                insertIntsTail((int[]) value, dest, dynamic);
            } else if (value instanceof long[]) {
                insertLongsTail((long[]) value, dest, dynamic);
            } else if (value instanceof short[]) {
                insertShortsTail((short[]) value, dest, dynamic);
            } else if(value instanceof boolean[]) {
                insertBooleansTail((boolean[]) value, dest, dynamic);
            } else {
                throw new Error("unexpected array type: " + paramType);
            }
        } else if(value instanceof Tuple) {
            TupleType tupleType;
            try {
                tupleType = (TupleType) paramType;
            } catch (ClassCastException cce) {
                throw new RuntimeException(cce);
            }
//            throw new Error();
            // TODO TEST ***********************************************************************
            Tuple tuple = (Tuple) value;
            int sum = getHeadLengthSum(tupleType.memberTypes, tuple.elements);
            insertTuple(tupleType, tuple, sum, dest);
        } else {
            throw new Error("unexpected type: " + paramType);
        }
    }

    // ----------------------------------------------

    private static void insertStringHead(StackableType paramType, String string, ByteBuffer dest, int[] offset) {
        // strings always dynamic
        insertOffset(offset, paramType, string, dest);
    }

    private static void insertBooleansHead(StackableType paramType, boolean[] bools, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, bools, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertBooleansStatic(bools, dest);
        }
    }

    private static void insertBytesHead(StackableType paramType, byte[] bytes, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, bytes, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertBytesStatic(bytes, dest);
        }
    }

    private static void insertShortsHead(StackableType paramType, short[] shorts, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, shorts, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertShortsStatic(shorts, dest);
        }
    }

    private static void insertIntsHead(StackableType paramType, int[] ints, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, ints, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertIntsStatic(ints, dest);
        }
    }

    private static void insertLongsHead(StackableType paramType, long[] longs, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, longs, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertLongsStatic(longs, dest);
        }
    }

    private static void insertBigIntsHead(StackableType paramType, BigInteger[] bigInts, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, bigInts, dest);
        } else {
            insertBigIntsStatic(bigInts, dest);
        }
    }

    private static void insertBigDecimalsHead(StackableType paramType, BigDecimal[] bigDecs, ByteBuffer dest, int[] offset) {
        if(paramType.dynamic) {
            insertOffset(offset, paramType, bigDecs, dest);
        } else {
            insertBigDecimalsStatic(bigDecs, dest);
        }
    }

    // ========================================

    private static int getHeadLengthSum(StackableType[] types, Object[] arguments) {
        int headLengths = 0;
        final int n = types.length;
        for (int i = 0; i < n; i++) {
            StackableType t = types[i];
//            int byteLen = t.byteLength(arguments[i]);
//            System.out.print(arguments[i] + " --> " + byteLen + ", ");

            if(t.dynamic) {
                headLengths += 32;
                System.out.println("dynamic");
            } else {
                headLengths += t.byteLength(arguments[i]);
                System.out.println("static");
            }
        }

        System.out.println("**************** " + headLengths);

        return headLengths;
    }

//    static void dataLengths(StackableType[] types, Object[] arguments, int[] dataLengths) {
////        int argsByteLen = 0;
//        final int n = dataLengths.length;
//        for (int i = 0; i < n; i++) {
//            StackableType t = types[i];
//            int byteLen = t.byteLength(arguments[i]); // .getDataByteLen(arguments[i]);
//            System.out.print(arguments[i] + " data--> " + byteLen + ", ");
////            argsByteLen += byteLen;
//
//            dataLengths[i] = types[i].byteLength(arguments[i]);
//        }
//
////        System.out.println("**************** " + argsByteLen);
//    }
    // -------------------------------------------------------------------------------------------------

    private static void insertBooleansTail(boolean[] bools, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bools.length, dest);
        insertBooleansStatic(bools, dest);
    }

    private static void insertBytesTail(byte[] bytes, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bytes.length, dest);
        insertBytesStatic(bytes, dest);
    }

    private static void insertShortsTail(short[] shorts, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(shorts.length, dest);
        insertShortsStatic(shorts, dest);
    }

    private static void insertIntsTail(int[] ints, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(ints.length, dest);
        insertIntsStatic(ints, dest);
    }

    private static void insertLongsTail(long[] longs, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(longs.length, dest);
        insertLongsStatic(longs, dest);
    }

    private static void insertBigIntsTail(BigInteger[] bigInts, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bigInts.length, dest);
        insertBigIntsStatic(bigInts, dest);
    }

    private static void insertBigDecimalsTail(BigDecimal[] bigDecs, ByteBuffer dest, boolean dynamic) {
        if(dynamic) insertLength(bigDecs.length, dest);
        insertBigDecimalsStatic(bigDecs, dest);
    }

    private static void insertBooleansStatic(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? BOOLEAN_TRUE : BOOLEAN_FALSE);
//            insertBool(e, dest);
        }
//        final int n = 32 - bools.length;
//        for (int i = 0; i < n; i++) {
//            dest.put((byte) 0);
//        }
    }

    private static void insertBytesStatic(byte[] bytes, ByteBuffer dest) {
        if(bytes.length > 0) { // don't pad empty array to 32 bytes
            dest.put(bytes);
            final int n = 32 - bytes.length;
            for (int i = 0; i < n; i++) {
                dest.put((byte) 0);
            }
        }
    }

    private static void insertShortsStatic(short[] shorts, ByteBuffer dest) {
        for (short e : shorts) {
            insertInt(e, dest);
        }
    }

    private static void insertIntsStatic(int[] ints, ByteBuffer dest) {
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

    private static void insertLongsStatic(long[] ints, ByteBuffer dest) {
        for (long e : ints) {
            insertInt(e, dest);
        }
    }

    private static void insertBigIntsStatic(BigInteger[] bigInts, ByteBuffer dest) {
        for (BigInteger e : bigInts) {
            insertInt(e, dest);
        }
    }

    private static void insertBigDecimalsStatic(BigDecimal[] bigDecs, ByteBuffer dest) {
        for (BigDecimal e : bigDecs) {
            insertInt(e.unscaledValue(), dest);
        }
    }

    // --------------------------

    private static void insertInt(long val, ByteBuffer dest) {
        dest.put(PADDING_192_BITS);
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
