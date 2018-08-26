package com.esaulpaugh.headlong.abi.beta.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.Function.FUNCTION_ID_LEN;

public class Encoder {

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
        getLengthInfo(types, arguments, headLengths);
//
        int encodingByteLen = tupleType.byteLength(arguments); //.getDataByteLen(tuple);

//        encodingByteLen += 64; // TODO REMOVE

        final int allocation = FUNCTION_ID_LEN + encodingByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
        outBuffer.put(function.selector);

        Encoder.encodeTuple(tupleType, arguments, headLengths, outBuffer);

        return outBuffer;
    }

    private static void encodeTuple(Tuple tupleType, Object[] values, int[] headLengths, ByteBuffer outBuffer) {

        int sum = 0;
        for (int i : headLengths) {
            sum += i;
        }

        int mark;
        StackableType[] types = tupleType.memberTypes;
        final int len = values.length;
        for(int i = 0; i < len; i++) {
            StackableType type = types[i];
            Object val = values[i];
            mark = outBuffer.position();
            if(type.dynamic) {
                encodeHead(type, val, outBuffer, sum, true);
                encodeTail(type, val, outBuffer);
            } else {
                encodeHead(type, val, outBuffer, sum, false);
            }
            sum += outBuffer.position() - mark;
            System.out.println("sum = " + sum + " (" + String.format("%040x", BigInteger.valueOf(sum)) +")");
        }

//        List<Type> typeList = new LinkedList<>(Arrays.asList(tupleType.types));
//        List<Object> valuesList = new LinkedList<>(Arrays.asList(values));
//        encodeHeadsForTuple(typeList, valuesList, headLengths, outBuffer);
//        encodeTailsForTuple(typeList, valuesList, outBuffer);
    }

    private static void encodeHeadsForTuple(List<StackableType> types, List<Object> values, int[] headLengths, ByteBuffer outBuffer) {

        int sum = 0;
        for (int i : headLengths) {
            sum += i;
        }

        int mark;
        Iterator<StackableType> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            StackableType type = ti.next();
            mark = outBuffer.position();
            encodeHead(type, vi.next(), outBuffer, sum, type.dynamic);
            sum += outBuffer.position() - mark;
            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    private static void encodeTailsForTuple(List<StackableType> types, List<Object> values, ByteBuffer outBuffer) {
        Iterator<StackableType> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            StackableType type = ti.next();
            encodeTail(type, vi.next(), outBuffer);
        }
    }

    // TODO switch(typeInt) for performance?
    private static void encodeHead(StackableType paramType, Object value, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(value instanceof String) { // dynamic
            insertStringHead(dest, tailOffset);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsHead((BigInteger[]) value, dest, tailOffset, dynamic);
                } else {
                    insertArrayHead(paramType, (Object[]) value, dest, tailOffset, dynamic);
                }
            } else if (value instanceof byte[]) {
                insertBytesHead((byte[]) value, dest, tailOffset, dynamic);
            } else if (value instanceof int[]) {
                insertIntsHead((int[]) value, dest, tailOffset, dynamic);
            } else if (value instanceof long[]) {
                insertLongsHead((long[]) value, dest, tailOffset, dynamic);
            } else if (value instanceof short[]) {
                insertShortsHead((short[]) value, dest, tailOffset, dynamic);
            } else if(value instanceof boolean[]) {
                insertBooleansHead((boolean[]) value, dest, tailOffset, dynamic);
            }
        } else if(value instanceof com.esaulpaugh.headlong.abi.beta.Tuple) {
            insertTupleHead(paramType, (com.esaulpaugh.headlong.abi.beta.Tuple) value, dest, tailOffset, dynamic);
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
                    for (Object object : (Object[]) value) {
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
//            throw new Error();
            com.esaulpaugh.headlong.abi.beta.Tuple tuple = (com.esaulpaugh.headlong.abi.beta.Tuple) value;
            int[] headLengths = getHeadLengths(tupleType.memberTypes, tuple.elements);
            encodeTuple(tupleType, tuple.elements, headLengths, dest);
        }
    }

    // ----------------------------------------------

    private static void insertStringHead(ByteBuffer dest, int tailOffset) {
        insertInt(tailOffset, dest);
    }

    private static void insertBooleansHead(boolean[] bools, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBooleansStatic(bools, dest);
        }
    }

    private static void insertBytesHead(byte[] bytes, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBytesStatic(bytes, dest);
        }
    }

    private static void insertShortsHead(short[] shorts, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertShortsStatic(shorts, dest);
        }
    }

    private static void insertIntsHead(int[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertIntsStatic(ints, dest);
        }
    }

    private static void insertLongsHead(long[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertLongsStatic(ints, dest);
        }
    }

    private static void insertBigIntsHead(BigInteger[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBigIntsStatic(ints, dest);
        }
    }
// ----- TODO
    static void getLengthInfo(StackableType[] types, Object[] arguments, int[] headLengths) {
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

    static int[] getHeadLengths(StackableType[] types, Object[] values) {
        final int len = types.length;
        int[] headLengths = new int[len];
        StackableType type;
        for (int i = 0; i < len; i++) {
            type = types[i];
            headLengths[i] = type.dynamic
                    ? 32
                    : type.byteLength(values[i]);// getDataByteLen(values[i]);
        }
        return headLengths;
    }

    private static void insertTupleHead(StackableType tupleType, com.esaulpaugh.headlong.abi.beta.Tuple tuple, ByteBuffer dest, int tailOffset, boolean dynamic) {
        Tuple paramTypes;
        try {
            paramTypes = (Tuple) tupleType;
        } catch (ClassCastException cce) {
            throw new RuntimeException(cce);
        }
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
//            throw new Error();
            int[] headLengths = getHeadLengths(paramTypes.memberTypes, tuple.elements);
            encodeTuple(paramTypes, tuple.elements, headLengths, dest);
        }
    }

    private static void insertArrayHead(StackableType paramType, Object[] objects, ByteBuffer dest, int tailOffset, boolean dynamic) {
//        for(Object obj : objects) {
//            encodeHead();
//        }
//        if(dynamic) {
//            insertInt(tailOffset, dest);
//        } else {
//            encodeTail(paramType, objects, dest);
//        }
    }

    // -------------------------------------------------------------------------------------------------

    private static void insertBooleansDynamic(boolean[] bools, ByteBuffer dest) {
        insertInt(bools.length, dest);
        insertBooleansStatic(bools, dest);
    }

    private static void insertBytesDynamic(byte[] bytes, ByteBuffer dest) {
        insertInt(bytes.length, dest);
        insertBytesStatic(bytes, dest);
    }

    private static void insertShortsDynamic(short[] shorts, ByteBuffer dest) {
        insertInt(shorts.length, dest);
        insertShortsStatic(shorts, dest);
    }

    private static void insertIntsDynamic(int[] ints, ByteBuffer dest) {
        insertInt(ints.length, dest);
        insertIntsStatic(ints, dest);
    }

    private static void insertLongsDynamic(long[] longs, ByteBuffer dest) {
        insertInt(longs.length, dest);
        insertLongsStatic(longs, dest);
    }

    private static void insertBigIntsDynamic(BigInteger[] bigInts, ByteBuffer dest) {
        insertInt(bigInts.length, dest);
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
