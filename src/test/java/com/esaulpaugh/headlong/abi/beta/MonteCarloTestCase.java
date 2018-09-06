package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;
import org.junit.Assert;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Random;
import java.util.Set;

import static com.esaulpaugh.headlong.abi.beta.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.beta.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.beta.ArrayType.STRING_CLASS_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MonteCarloTestCase {

    private static String[] CANONICAL_BASE_TYPE_STRINGS;

    private static final String TUPLE_BASE_TYPE_STRING = "(...)";

    static {
        final Set<String> keySet = BaseTypeInfo.keySet();
        CANONICAL_BASE_TYPE_STRINGS = new String[keySet.size() + 2];
        int i = 0;
        for (String canonical : keySet) {
            CANONICAL_BASE_TYPE_STRINGS[i++] = canonical;
        }
        CANONICAL_BASE_TYPE_STRINGS[i++] = TUPLE_BASE_TYPE_STRING;
        CANONICAL_BASE_TYPE_STRINGS[i++] = TUPLE_BASE_TYPE_STRING;

    }

    final Params params;

    final String canonicalSignature;
    final Tuple argsTuple;

    MonteCarloTestCase(Params params) throws ParseException {
        this.params = params;

        Random rng = new Random(params.seed);

        String rawSig = generateFunctionSignature(rng, 0);

        Function function = new Function(rawSig);

        this.canonicalSignature = function.getCanonicalSignature();

        this.argsTuple = generateTuple(function.paramTypes, rng);
    }

    MonteCarloTestCase(long seed) throws ParseException {
        this(new Params(seed));
    }

    void run() {
        Function function = function();

        ByteBuffer abi = function.encodeCall(argsTuple);

        byte[] array = abi.array();

//                EncodeTest.printABI(abi.array());

        final Tuple out = function.decodeCall(array);

        boolean equal = argsTuple.equals(out);
        System.out.println(equal);

        if(!equal) {
            findInequality(function.paramTypes, argsTuple, out);
        }

        Assert.assertEquals(argsTuple, out);
    }

    static class Params {

        static final int DEFAULT_MAX_TUPLE_DEPTH = 10;
        static final int DEFAULT_MAX_TUPLE_LENGTH = 12;

        static final int DEFAULT_MAX_ARRAY_DEPTH = 5;
        static final int DEFAULT_MAX_ARRAY_LENGTH = 12;

        final int maxTupleLen;
        final int maxArrayLen;

        final int maxTupleDepth;
        final int maxArrayDepth;

        final long seed;

        Params(long seed) {
            this.maxTupleDepth = DEFAULT_MAX_TUPLE_DEPTH;
            this.maxTupleLen = DEFAULT_MAX_TUPLE_LENGTH;
            this.maxArrayDepth = DEFAULT_MAX_ARRAY_DEPTH;
            this.maxArrayLen = DEFAULT_MAX_ARRAY_LENGTH;
            this.seed = seed;
        }

        Params(int maxTupleDepth, int maxTupleLen, int maxArrayDepth, int maxArrayLen, long seed) {
            this.maxTupleLen = maxTupleLen;
            this.maxArrayLen = maxArrayLen;
            this.maxTupleDepth = maxTupleDepth;
            this.maxArrayDepth = maxArrayDepth;
            this.seed = seed;
        }
    }

    Function function() {
        try {
            return new Function(canonicalSignature);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateFunctionSignature(Random r, int tupleDepth) throws ParseException {

        StackableType[] types = new StackableType[r.nextInt(params.maxTupleLen)];
        for (int i = 0; i < types.length; i++) {
            types[i] = generateType(r, tupleDepth);
        }
//        TupleType tupleType = TupleType.create(TUPLE_BASE_TYPE_STRING, types);
//        TupleType tupleType = generateTupleType(r, tupleDepth);

        StringBuilder signature = new StringBuilder("(");
        for (StackableType t : types) {
            signature.append(t.canonicalType).append(',');
        }
        if (types.length > 0) {
            signature.replace(signature.length() - 1, signature.length(), "");
        }
        signature.append(')');

        return signature.toString();
    }

    private StackableType generateType(Random r, final int tupleDepth) throws ParseException {
        int index = r.nextInt(CANONICAL_BASE_TYPE_STRINGS.length);
        String baseTypeString = CANONICAL_BASE_TYPE_STRINGS[index];
        StringBuilder sb = new StringBuilder(baseTypeString);
        boolean isElement = r.nextBoolean();
        if(isElement) {
            int arrayDepth = 1 + r.nextInt(params.maxArrayDepth);
            for (int i = 0; i < arrayDepth; i++) {
                sb.append('[');
                if(r.nextBoolean()) {
                    sb.append(1 + r.nextInt(params.maxArrayLen));
                }
                sb.append(']');
            }
        }

        String canonicalTypeString = sb.toString();

        if(baseTypeString.equals(TUPLE_BASE_TYPE_STRING)) {
            return TypeFactory.createForTuple(canonicalTypeString, generateTupleType(r, tupleDepth + 1));
        }
        return TypeFactory.create(canonicalTypeString);
    }

    private TupleType generateTupleType(Random r, int tupleDepth) throws ParseException {
        if(tupleDepth >= params.maxTupleDepth) {
            return TupleType.create("()");
        }
        return new Function(generateFunctionSignature(r, tupleDepth)).paramTypes;
    }

    private Tuple generateTuple(TupleType tupleType, Random r) {
        final StackableType[] types = tupleType.elementTypes;
        Object[] args = new Object[types.length];

        for (int i = 0; i < types.length; i++) {
            args[i] = generateValue(types[i], r);
        }

        return new Tuple(args);
    }

    private Object generateValue(StackableType type, Random r) {

        if(type instanceof AbstractUnitType) {
            int bitLimit = ((AbstractUnitType) type).bitLength;
            byte[] unitBuffer = new byte[UNIT_LENGTH_BYTES];
            r.nextBytes(unitBuffer);
            if(type instanceof ByteType) {
                return generateByte(r);
            }
            if(type instanceof ShortType) {
                return generateShort(r);
            }
            if(type instanceof IntType) {
                return generateInt(r, bitLimit);
            }
            if(type instanceof LongType) {
                return generateLong(r, bitLimit);
            }
            if(type instanceof BigIntegerType) {
                return generateBigInteger(r, bitLimit);
            }
            if(type instanceof BigDecimalType) {
                return generateBigDecimal(r, bitLimit, ((BigDecimalType) type).scale);
            }
            if(type instanceof BooleanType) {
                return r.nextBoolean();
            }
            throw new Error(type.getClass().getName());
        }
        if(type instanceof ArrayType) {
            return generateArray((ArrayType) type, r);
        }
        if(type instanceof TupleType) {
            return generateTuple((TupleType) type, r);
        }
        throw new Error(type.getClass().getName());
    }

    private static byte generateByte(Random r) {
        return (byte) r.nextInt();
    }

    private static short generateShort(Random r) {
        byte[] random = new byte[1 + r.nextInt(Short.BYTES)]; // 1-2
        r.nextBytes(random);
        return new BigInteger(random).shortValueExact();
    }

    private static int generateInt(Random r, int bitLimit) {
        // bitLimit div 8
        byte[] buffer = new byte[1 + r.nextInt(bitLimit >>> 3)]; // 1-4
        return new BigInteger(buffer).intValueExact();
    }

    private static long generateLong(Random r, int bitLimit) {
        byte[] random = new byte[1 + r.nextInt(bitLimit >>> 3)]; // 1-8
        r.nextBytes(random);
        return new BigInteger(random).longValueExact();
//        if(r.nextBoolean()) {
//            return BizarroIntegers.getLong(buffer, 0, buffer.length);
//        }
//        return RLPIntegers.getLong(buffer, 0, buffer.length);
    }

//    private static int getScale(BigDecimalType bigDecimalType) {
//        BaseTypeInfo info = BaseTypeInfo.get(bigDecimalType.canonicalType);
//        if(info != null) { // decimal
//            return info.scale;
//        } else { // (u)fixedMxN
//            BigDecimalType type = tryParseFixed();
//        }
//
//    }

    private static BigInteger generateBigInteger(Random r, int bitLimit) {
        byte[] thirtyTwo = new byte[UNIT_LENGTH_BYTES];
        final int len = 1 + r.nextInt(bitLimit >>> 3); // 1-32
        for (int i = UNIT_LENGTH_BYTES - len; i < UNIT_LENGTH_BYTES; i++) {
            thirtyTwo[i] = (byte) r.nextInt();
        }
        BigInteger bi = new BigInteger(thirtyTwo);
        if(r.nextBoolean()) {
            if(bi.compareTo(BigInteger.ZERO) < 0) {
                return bi.add(BigInteger.ONE).negate();
            }
            return bi.negate();
        }
        return bi;
    }

    private static BigDecimal generateBigDecimal(Random r, int bitLimit, int scale) {
        return new BigDecimal(generateBigInteger(r, bitLimit), scale);
    }

    private Object generateArray(ArrayType arrayType, Random r) {
        StackableType elementType = arrayType.elementType;
        final int len = arrayType.length == DYNAMIC_LENGTH
                ? r.nextInt(params.maxArrayLen + 1) // 0 to max
                : arrayType.length;
        if(elementType instanceof AbstractUnitType) {
            int elementBitLimit = ((AbstractUnitType) elementType).bitLength;
            if (elementType instanceof ByteType) {
                if (arrayType.className().equals(STRING_CLASS_NAME)) {
                    return generateString(len, r);
                }
                return generateByteArray(len, r);
            }
            if (elementType instanceof ShortType) {
                return generateShortArray(len, r);
            }
            if (elementType instanceof IntType) {
                return generateIntArray(len, elementBitLimit, r);
            }
            if (elementType instanceof LongType) {
                return generateLongArray(len, elementBitLimit, r);
            }
            if (elementType instanceof BigIntegerType) {
                return generateBigIntegerArray(len, elementBitLimit, r);
            }
            if (elementType instanceof BigDecimalType) {
//                int scale = ;
                return generateBigDecimalArray(len, elementBitLimit, ((BigDecimalType) elementType).scale, r);
            }
            if(elementType instanceof BooleanType) {
                return generateBooleanArray(len, r);
            }
            throw new Error();
        }
        if(elementType instanceof TupleType) {
            return generateTupleArray((TupleType) elementType, len, r);
        }
        if(elementType instanceof ArrayType) {
            return generateObjectArray((ArrayType) elementType, len, r);
        }
        throw new Error();
    }

    private static byte[] generateByteArray(int len, Random r) {
        byte[] random = new byte[len];
        r.nextBytes(random);
        return random;
    }

    private static String generateString(int len, Random r) {
        return new String(generateByteArray(len, r), UTF_8);
    }

    private static short[] generateShortArray(final int len, Random r) {
        short[] shorts = new short[len];
        for (int i = 0; i < len; i++) {
            shorts[i] = generateShort(r);
        }
        return shorts;
    }

    private static int[] generateIntArray(final int len, int elementBitLimit, Random r) {
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            ints[i] = generateInt(r, elementBitLimit);
        }
        return ints;
    }

    private static long[] generateLongArray(final int len, int elementBitLimit, Random r) {
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            longs[i] = generateLong(r, elementBitLimit);
        }
        return longs;
    }

    private static BigInteger[] generateBigIntegerArray(final int len, int elementBitLimit, Random r) {
        BigInteger[] bigInts = new BigInteger[len];
        for (int i = 0; i < len; i++) {
            bigInts[i] = generateBigInteger(r, elementBitLimit);
        }
        return bigInts;
    }

    private static BigDecimal[] generateBigDecimalArray(final int len, int elementBitLimit, int elementScale, Random r) {
        BigDecimal[] bigDecs = new BigDecimal[len];
        for (int i = 0; i < len; i++) {
            bigDecs[i] = generateBigDecimal(r, elementBitLimit, elementScale); // TODO
        }
        return bigDecs;
    }

    private static boolean[] generateBooleanArray(final int len, Random r) {
        boolean[] booleans = new boolean[len];
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = r.nextBoolean();
        }
        return booleans;
    }
    private Tuple[] generateTupleArray(TupleType tupleType, final int len, Random r) {
        Tuple[] tuples = new Tuple[len];
        for (int i = 0; i < len; i++) {
            tuples[i] = generateTuple(tupleType, r);
        }
        return tuples;
    }

    private Object[] generateObjectArray(ArrayType elementType, final int len, Random r) {

        Object[] dest;
        try {
            dest = (Object[]) Array.newInstance(Class.forName(elementType.className), len);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < len; i++) {
            dest[i] = generateArray(elementType, r);
        }
        return dest;
    }

    // ------------------------------------------------------------------------

    private static void findInequality(TupleType tupleType, Tuple in, Tuple out) {
        final int len = tupleType.elementTypes.length;
        for (int i = 0; i < len; i++) {
            StackableType type = tupleType.elementTypes[i];
            findInequality(type, in.elements[i], out.elements[i]);
        }
    }

    private static void findInequality(StackableType elementType, Object in, Object out) {
        System.out.println("findInequality(" + elementType.getClass().getName() + ')');
        if(elementType instanceof AbstractUnitType) {
            findInequality((AbstractUnitType) elementType, in, out);
        } else if(elementType instanceof TupleType) {
            findInequality((TupleType) elementType, (Tuple) in, (Tuple) out);
        } else if(elementType instanceof ArrayType) {
            findInequalityInArray((ArrayType) elementType, (Object[]) in, (Object[]) out);
        } else {
            throw new Error();
        }
    }

    private static void findInequality(AbstractUnitType abstractUnitType, Object in, Object out) {
        if(!in.equals(out)) {
            if(in instanceof BigInteger && out instanceof BigInteger) {
                System.err.println("bitLength: " + ((BigInteger) in).bitLength() + " =? " + ((BigInteger) out).bitLength());
            }
            System.err.println(in + " != " + out + " " + abstractUnitType.bitLength);
            throw new Error();
        }
    }

    private static void findInequalityInArray(ArrayType arrayType, Object[] in, Object[] out) {
        final StackableType elementType = arrayType.elementType;
        if (in.length != out.length) {
            throw new AssertionError(elementType.toString() + " len " + in.length + " != " + out.length);
        }
        for (int i = 0; i < in.length; i++) {
            Object ie = in[i];
            Object oe = out[i];
            if (!ie.equals(oe)) {
                findInequality(elementType, ie, oe);
            }
        }
    }
}