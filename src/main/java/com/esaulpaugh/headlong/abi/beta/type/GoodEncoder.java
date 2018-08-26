package com.esaulpaugh.headlong.abi.beta.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.Function.FUNCTION_ID_LEN;

public class GoodEncoder {

    private static final byte[] PADDING_192_BITS = new byte[24];

    public static ByteBuffer encodeFunctionCall(Function function, Object[] arguments) {

        System.out.println("requiredCanonicalization = " + function.requiredCanonicalization());

        final Tuple tupleType = function.paramTypes;
        final com.esaulpaugh.headlong.abi.beta.Tuple tuple = new com.esaulpaugh.headlong.abi.beta.Tuple(arguments);
        final StackableType[] types = tupleType.memberTypes;
        final int expectedNumParams = types.length;

        if(arguments.length != expectedNumParams) {
            throw new IllegalArgumentException("arguments.length <> types.size(): " + arguments.length + " != " + types.length);
        }

//        TupleType.checkTypes(types, arguments);

        tupleType.validate(tuple);

        int[] headLengths = new int[types.length];
        getHeadLengths(types, arguments, headLengths);
//
        int encodingByteLen = tupleType.byteLength(arguments); //.getDataByteLen(tuple);

        System.out.println(tupleType.dynamic + " " + encodingByteLen);
//        encodingByteLen += 64; // TODO REMOVE

        final int allocation = FUNCTION_ID_LEN + encodingByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
        outBuffer.put(function.selector);

        insertTuple(tupleType, tuple, headLengths, outBuffer);

        return outBuffer;
    }

    private static void insertTuple(Tuple tupleType, com.esaulpaugh.headlong.abi.beta.Tuple tuple, int[] headLengths, ByteBuffer outBuffer) {
        Object[] values = tuple.elements;
        List<StackableType> typeList = new LinkedList<>(Arrays.asList(tupleType.memberTypes));
        List<Object> valuesList = new LinkedList<>(Arrays.asList(values));
//        int[] offset = new int[] { tupleType.overhead(tuple) };

        encodeHeadsForTuple(typeList, valuesList, headLengths, outBuffer);
        encodeTailsForTuple(typeList, valuesList, outBuffer);
    }

    private static void encodeHeadsForTuple(List<StackableType> types, List<Object> values, int[] headLengths, ByteBuffer outBuffer) {

        int sum = 0;
        for (int i : headLengths) {
            sum += i;
        }

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

        int i = 0;

        int mark;
        Iterator<StackableType> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            StackableType type = ti.next();
            Object val = vi.next();

            mark = outBuffer.position();

            encodeHead(type, val, outBuffer, offset); // , type.dynamic
            sum += outBuffer.position() - mark;

//            offset[0] = type.overhead(val);

            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    private static void encodeHead(StackableType paramType, Object value, ByteBuffer dest, int[] offset) { // , boolean dynamic
        boolean dynamic = paramType.dynamic;
        if(value instanceof String) { // dynamic
            insertStringHead(dest, offset[0]);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsHead(paramType, (BigInteger[]) value, dest, offset, dynamic);
                } else {
                    Object[] elements = (Object[]) value;
                    insertOffset(offset, paramType, elements, dest);
//                    int[] headLengths = getHeadLengths(((Array) paramType).elementType, elements);
//                    insertArrayOffsets(paramType, elements, dest, tailOffsets[0]); // , dynamic
                }
            } else if (value instanceof byte[]) {
                insertBytesHead(paramType, (byte[]) value, dest, offset, dynamic);
            } else if (value instanceof int[]) {
                insertIntsHead(paramType, (int[]) value, dest, offset, dynamic);
            } else if (value instanceof long[]) {
                insertLongsHead(paramType, (long[]) value, dest, offset, dynamic);
            } else if (value instanceof short[]) {
                insertShortsHead(paramType, (short[]) value, dest, offset, dynamic);
            } else if(value instanceof boolean[]) {
                insertBooleansHead(paramType, (boolean[]) value, dest, offset, dynamic);
            }
        } else if(value instanceof com.esaulpaugh.headlong.abi.beta.Tuple) {
//            insertTupleHead(paramType, (com.esaulpaugh.headlong.abi.beta.Tuple) value, dest, tailOffsets[0], dynamic);
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

    private static void insertLength(int length, ByteBuffer dest) {
        insertInt(0x3333333333000000L + length, dest);
    }

    private static void insertOffset(final int[] offset, StackableType paramType, Object object, ByteBuffer dest) {
        if(paramType.dynamic) {
            System.out.println("\noffset[0] is " + offset[0]);
            insertInt(0xFFFFFFFFFF000000L + offset[0], dest);
//            offset[0] = offset[0] - 32 + paramType.byteLength(object);
//            offset[0] = paramType.overhead(object);
//            System.out.println("overhead " + offset[0]);


            System.out.println("offset[0] = " + offset[0] + " + " + paramType.byteLength(object) + " - " + 32);
            offset[0] += paramType.byteLength(object) - 32;
            System.out.println("aka " + offset[0] + ", " + (offset[0] >>> 5));
        }
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
        if(value instanceof String) { // dynamic
            insertBytesDynamic(((String) value).getBytes(StandardCharsets.UTF_8), dest);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsDynamic((BigInteger[]) value, dest);
                } else {
                    Object[] objects = (Object[]) value;
                    insertLength(objects.length, dest);
                    int[] offset = new int[] { 0 };
                    for(Object object : objects) {
                        insertOffset(offset, paramType, object, dest);
                    }
                    for (Object object : objects) {
                        encodeTail(paramType, object, dest);
                    }
                }
            } else if (value instanceof byte[]) {
                insertBytesDynamic((byte[]) value, dest);
            } else if (value instanceof int[]) {
                insertIntsDynamic((int[]) value, dest);
            } else if (value instanceof long[]) {
                insertLongsDynamic((long[]) value, dest);
            } else if (value instanceof short[]) {
                insertShortsDynamic((short[]) value, dest);
            } else if(value instanceof boolean[]) {
                insertBooleansDynamic((boolean[]) value, dest);
            }
        } else if(value instanceof com.esaulpaugh.headlong.abi.beta.Tuple) {
            Tuple tupleType;
            try {
                tupleType = (Tuple) paramType;
            } catch (ClassCastException cce) {
                throw new RuntimeException(cce);
            }
            throw new Error();
//            com.esaulpaugh.headlong.abi.beta.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.Tuple) value;
//            int[] headLengths = getHeadLengths(tupleType.memberTypes, tuple.elements);
//            insertTuple(tupleType, tuple.elements, null, dest);
        }
    }

    // ----------------------------------------------

//    private static void insertArrayOffsets(StackableType paramType, Object[] objects, ByteBuffer dest, final int tailOffset) {
//        System.out.println("tailOffset = " + tailOffset);
//
//        int sum = tailOffset;
//        int dataLen = 0;
//
//        int off;
////        int[] tailOffsets = new int[objects.length];
//
//        Array type = (Array) paramType;
//        if(type.dynamic) {
//            final int len = objects.length;
//            for(int i = 0; i < len; i++) {
//                off = sum + dataLen - (i * 32);
//                System.out.println(i + " : off = " + off);
//                insertInt(off, dest);
//
//                dataLen = type.byteLength(objects);
//                System.out.println(i + " : dataLen = " +dataLen);
//            }
////            insertInt(0xFFFFFFFFFFFFFL, dest);
//        } else {
//            throw new Error();
////            encodeTail(paramType, objects, dest);
//        }
//    }

    private static void insertStringHead(ByteBuffer dest, int tailOffset) {
        insertInt(tailOffset, dest);
    }

    private static void insertBooleansHead(StackableType paramType, boolean[] bools, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, bools, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertBooleansStatic(bools, dest);
        }
    }

    private static void insertBytesHead(StackableType paramType, byte[] bytes, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, bytes, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertBytesStatic(bytes, dest);
        }
    }

    private static void insertShortsHead(StackableType paramType, short[] shorts, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, shorts, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertShortsStatic(shorts, dest);
        }
    }

    private static void insertIntsHead(StackableType paramType, int[] ints, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, ints, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertIntsStatic(ints, dest);
        }
    }

    private static void insertLongsHead(StackableType paramType, long[] longs, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, longs, dest);
//            insertInt(tailOffset, dest);
        } else {
            insertLongsStatic(longs, dest);
        }
    }

    private static void insertBigIntsHead(StackableType paramType, BigInteger[] ints, ByteBuffer dest, int[] offset, boolean dynamic) {
        if(dynamic) {
            insertOffset(offset, paramType, ints, dest);
        } else {
            insertBigIntsStatic(ints, dest);
        }
    }

    // ========================================
    // ----- TODO
    static void getHeadLengths(StackableType[] types, Object[] arguments, int[] headLengths) {
        int argsByteLen = 0;
        final int n = headLengths.length;
        for (int i = 0; i < n; i++) {
            StackableType t = types[i];
            int byteLen = t.byteLength(arguments[i]); // .getDataByteLen(arguments[i]);
            System.out.print(arguments[i] + " --> " + byteLen + ", ");
            argsByteLen += byteLen;

            if(t.dynamic) {
                headLengths[i] = 32;
                System.out.println("dynamic");
            } else {
                headLengths[i] = byteLen;
                System.out.println("static");
            }
        }

        System.out.println("**************** " + argsByteLen);

//        return argsByteLen;
    }

    static void dataLengths(StackableType[] types, Object[] arguments, int[] dataLengths) {
//        int argsByteLen = 0;
        final int n = dataLengths.length;
        for (int i = 0; i < n; i++) {
            StackableType t = types[i];
            int byteLen = t.byteLength(arguments[i]); // .getDataByteLen(arguments[i]);
            System.out.print(arguments[i] + " data--> " + byteLen + ", ");
//            argsByteLen += byteLen;

            dataLengths[i] = types[i].byteLength(arguments[i]);
        }

//        System.out.println("**************** " + argsByteLen);
    }
    // -------------------------------------------------------------------------------------------------

    private static void insertBooleansDynamic(boolean[] bools, ByteBuffer dest) {
        insertLength(bools.length, dest);
        insertBooleansStatic(bools, dest);
    }

    private static void insertBytesDynamic(byte[] bytes, ByteBuffer dest) {
        insertLength(bytes.length, dest);
        insertBytesStatic(bytes, dest);
    }

    private static void insertShortsDynamic(short[] shorts, ByteBuffer dest) {
        insertLength(shorts.length, dest);
        insertShortsStatic(shorts, dest);
    }

    private static void insertIntsDynamic(int[] ints, ByteBuffer dest) {
        insertLength(ints.length, dest);
        insertIntsStatic(ints, dest);
    }

    private static void insertLongsDynamic(long[] longs, ByteBuffer dest) {
        insertLength(longs.length, dest);
        insertLongsStatic(longs, dest);
    }

    private static void insertBigIntsDynamic(BigInteger[] bigInts, ByteBuffer dest) {
        insertLength(bigInts.length, dest);
        insertBigIntsStatic(bigInts, dest);
    }

    private static void insertBooleansStatic(boolean[] bools, ByteBuffer dest) {
        for (boolean e : bools) {
            dest.put(e ? (byte) 1 : (byte) 0);
//            insertBool(e, dest);
        }
        final int n = 32 - bools.length;
        for (int i = 0; i < n; i++) {
            dest.put((byte) 0);
        }
    }

    private static void insertBytesStatic(byte[] bytes, ByteBuffer dest) {
        dest.put(bytes);
        final int n = 32 - bytes.length;
        for (int i = 0; i < n; i++) {
            dest.put((byte) 0);
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
