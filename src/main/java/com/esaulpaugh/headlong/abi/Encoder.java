package com.esaulpaugh.headlong.abi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.esaulpaugh.headlong.abi.Function.FUNCTION_ID_LEN;

public class Encoder {

    private static final byte[] PADDING_192_BITS = new byte[24];

    private static int getLengthInfo(Type[] types, Object[] arguments, int[] headLengths) {
        int dynamicOverheadBytes = 0;
        int paramsByteLen = 0;
        final int n = headLengths.length;
        for (int i = 0; i < n; i++) {
            Type t = types[i];
            int byteLen = t.getDataByteLen(arguments[i]);
            System.out.print(arguments[i] + " --> " + byteLen + ", ");
            paramsByteLen += byteLen;

            if(t.dynamic) {
                headLengths[i] = 32;
                dynamicOverheadBytes += 32 + 32; // 32
                System.out.println(arguments[i] + " dynamic len: " + 32 + " + " + 32 + " + " + byteLen + " = " + (64 + byteLen));
            } else {
                headLengths[i] = byteLen;
                System.out.println(arguments[i] + " static len: " + byteLen);
            }
        }

        System.out.println("**************** " + dynamicOverheadBytes);

        System.out.println("**************** " + paramsByteLen);

        return dynamicOverheadBytes + paramsByteLen;
    }

    public static ByteBuffer encodeFunctionCall(Function function, Object[] arguments) {

        System.out.println("requiredCanonicalization = " + function.requiredCanonicalization);

        final TupleType paramTypes = function.paramTypes;
        final Type[] types = paramTypes.types;
        final int expectedNumParams = types.length;

        if(arguments.length != expectedNumParams) {
            throw new IllegalArgumentException("arguments.length <> types.size(): " + arguments.length + " != " + types.length);
        }

        ABI.checkTypes(types, arguments);

        // head(X(i)) = enc(len(head(X(1)) ... head(X(k)) tail(X(1)) ... tail(X(i-1)) ))

        int[] headLengths = new int[types.length];
        int encodingByteLen = getLengthInfo(types, arguments, headLengths);

        final int allocation = FUNCTION_ID_LEN + encodingByteLen;

        System.out.println("allocating " + allocation);
        ByteBuffer outBuffer = ByteBuffer.wrap(new byte[allocation]); // ByteOrder.BIG_ENDIAN by default
//        Keccak keccak = new Keccak(256);
//        keccak.update(signature.getBytes(ASCII));
//        keccak.digest(outBuffer, 4);

        outBuffer.put(function.selector);

//        List<Type> typeList = new LinkedList<>(Arrays.asList(types));
//        List<Object> paramList = new LinkedList<>(Arrays.asList(values));

        Encoder.encodeTuple(paramTypes, arguments, headLengths, outBuffer);

//        encodeParamTails(outBuffer, typeList, paramList);

        return outBuffer;
    }

    public static void encodeTuple(TupleType paramTypes, Object[] values, int[] headLengths, ByteBuffer outBuffer) {
        List<Type> typeList = new LinkedList<>(Arrays.asList(paramTypes.types));
        List<Object> valuesList = new LinkedList<>(Arrays.asList(values));

        encodeParamHeads(typeList, valuesList, headLengths, outBuffer);
        encodeParamTails(typeList, valuesList, outBuffer);
    }

    public static void encodeParamHeads(List<Type> types, List<Object> values, int[] headLengths, ByteBuffer outBuffer) {
//        final int size = types.size();
//        for (int i = 0; i < size; i++) {
//            types[i].encodeHeads(values[i], outBuffer);
//        }

        int sum = 0;
        for (int i : headLengths) {
            sum += i;
        }

        int mark;
//        int i = 0;
        Iterator<Type> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            Type type = ti.next();
//            sum -= headLengths[i++];
            mark = outBuffer.position();
            encodeHead(type, vi.next(), outBuffer, sum, type.dynamic);
            sum += outBuffer.position() - mark;
            if(!type.dynamic) {
                ti.remove();
                vi.remove();
            }
        }
    }

    public static void encodeParamTails(List<Type> types, List<Object> values, ByteBuffer outBuffer) {
//        final int size = types.size();
//        for (int i = 0; i < size; i++) {
//            types[i].encodeHeads(values[i], outBuffer);
//        }
        Iterator<Type> ti;
        Iterator<Object> vi;
        for(ti = types.iterator(), vi = values.iterator(); ti.hasNext(); ) {
            Type type = ti.next();
            encodeTail(type, vi.next(), outBuffer);
//            if(!type.dynamic) {
//                ti.remove();
//                vi.remove();
//            }
        }
    }

    // TODO switch(typeInt) for performance?
    public static void encodeHead(Type paramType, Object value, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(value instanceof String) { // dynamic
            dest.position(dest.position() + 32); // leave empty for now
//            insertBytes(((String) value).getBytes(StandardCharsets.UTF_8), dest);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigIntsHead((BigInteger[]) value, dest, tailOffset, dynamic);
                } else {
                    insertObjectsHead(paramType, (Object[]) value, dest, tailOffset, dynamic);
                }
            } else if (value instanceof byte[]) {
//                System.out.println("byte[] " + dest.position());
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
        } else if(value instanceof Tuple) {
            insertTupleHead((TupleType) paramType, (Tuple) value, dest, tailOffset, dynamic);
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

    public static void encodeTail(Type paramType, Object value, ByteBuffer dest) {
        if(value instanceof String) { // dynamic
            dest.position(dest.position() + 32); // leave empty for now
//            insertBytes(((String) value).getBytes(StandardCharsets.UTF_8), dest);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                if(value instanceof BigInteger[]) {
                    insertBigInts((BigInteger[]) value, dest);
                } else {
                    for (Object object : (Object[]) value) {
                        encodeTail(paramType, object, dest);
                    }
                }
            } else if (value instanceof byte[]) {
                insertBytes((byte[]) value, dest);
            } else if (value instanceof int[]) {
                insertInts((int[]) value, dest);
            } else if (value instanceof long[]) {
                insertLongs((long[]) value, dest);
            } else if (value instanceof short[]) {
                insertShorts((short[]) value, dest);
            } else if(value instanceof boolean[]) {
                insertBooleans((boolean[]) value, dest);
            }
//        } else if (value instanceof Number) {
//            if(value instanceof BigInteger) {
//                insertInt(((BigInteger) value), dest);
//            } else if(value instanceof BigDecimal) {
//                insertInt(((BigDecimal) value).unscaledValue(), dest);
//            } else {
//                insertInt(((Number) value).longValue(), dest);
//            }
//        } else if(value instanceof Boolean) {
//            insertBool((boolean) value, dest);
        } else if(value instanceof Tuple) {
            TupleType tupleType = (TupleType) paramType;
            int[] headLengths = new int[tupleType.types.length];
            int encodingByteLen = getLengthInfo(tupleType.types, ((Tuple) value).elements, headLengths);
            encodeTuple(tupleType, ((Tuple) value).elements, headLengths, dest);
//            insertTuple((Tuple) value, dest);
        }
    }

    // ----------------------------------------------

    public static void insertBooleansHead(boolean[] bools, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBooleans(bools, dest);
        }
    }

    public static void insertBytesHead(byte[] bytes, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBytes(bytes, dest);
        }
    }

    public static void insertShortsHead(short[] shorts, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertShorts(shorts, dest);
        }
    }

    public static void insertIntsHead(int[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertInts(ints, dest);
        }
    }

    public static void insertLongsHead(long[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertLongs(ints, dest);
        }
    }

    public static void insertBigIntsHead(BigInteger[] ints, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            insertBigInts(ints, dest);
        }
    }

    public static void insertTupleHead(TupleType paramTypes, Tuple tuple, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            encodeTuple(paramTypes, tuple.elements, null, dest);
//            insertTuple(paramType, tuple, dest);
        }
    }

    public static void insertObjectsHead(Type paramType, Object[] objects, ByteBuffer dest, int tailOffset, boolean dynamic) {
        if(dynamic) {
            insertInt(tailOffset, dest);
        } else {
            encodeTail(paramType, objects, dest);
        }
    }

    // -------------------------------------------------------------------------------------------------

    public static void insertBooleans(boolean[] bools, ByteBuffer dest) {
        insertInt(bools.length, dest);
        for (boolean e : bools) {
            insertBool(e, dest);
        }
    }

    public static void insertBytes(byte[] bytes, ByteBuffer dest) {
        insertInt(bytes.length, dest);
        dest.put(bytes);
        // pad todo
        final int n = Integer.SIZE - bytes.length;
        for (int i = 0; i < n; i++) {
            dest.put((byte) 0);
        }
    }

    public static void insertShorts(short[] shorts, ByteBuffer dest) {
        insertInt(shorts.length, dest);
        for (short e : shorts) {
            insertInt(e, dest);
        }
    }

    public static void insertInts(int[] ints, ByteBuffer dest) {
        insertInt(ints.length, dest);
        for (int e : ints) {
            insertInt(e, dest);
        }
    }

    public static void insertLongs(long[] ints, ByteBuffer dest) {
        insertInt(ints.length, dest);
        for (long e : ints) {
            insertInt(e, dest);
        }
    }

    public static void insertBigInts(BigInteger[] bigInts, ByteBuffer dest) {
        insertInt(bigInts.length, dest);
        for (BigInteger e : bigInts) {
            insertInt(e, dest);
        }
    }

//    public static void insertTuple(Tuple tuple, ByteBuffer dest) {
//        insertObjects(tuple.elements, dest);
//    }

//    public static void insertObjects(Object[] objects, ByteBuffer dest) {
//        for (Object obj) {
//            encodeTail();
//        }
////        if(objects instanceof BigInteger[]) {
////            insertBigInts((BigInteger[]) objects, dest);
////        } else {
////            throw new Error(objects.getClass().getName());
////        }
//    }

    // --------------------------

    public static void insertInt(long val, ByteBuffer dest) {
//        final int pos = dest.position();
        dest.put(PADDING_192_BITS);
        dest.putLong(val);
//        putLongBigEndian(val, dest, pos + NUM_PADDING_BYTES);
//        return pos + INT_PARAM_LENGTH_BYTES;
    }

    public static void insertInt(BigInteger bigGuy, ByteBuffer dest) {
        byte[] arr = bigGuy.toByteArray();
        final int lim = 32 - arr.length;
        for (int i = 0; i < lim; i++) {
            dest.put((byte) 0);
        }
        dest.put(arr);
    }

    public static void insertBool(boolean bool, ByteBuffer dest) {
        insertInt(bool ? 1L : 0L, dest);
    }

//    public static void insertObject() {
//
//    }
}
