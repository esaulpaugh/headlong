package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;

import static com.esaulpaugh.headlong.abi.beta.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.beta.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.beta.ArrayType.STRING_CLASS_NAME;
import static com.esaulpaugh.headlong.abi.beta.StackableType.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MonteCarloTestCase {

    static class Params {

        static final int DEFAULT_MAX_TUPLE_DEPTH = 4; // 2
        static final int DEFAULT_MAX_TUPLE_LENGTH = 9; // 4

        static final int DEFAULT_MAX_ARRAY_DEPTH = 4; // 2
        static final int DEFAULT_MAX_ARRAY_LENGTH = 4; // 35

        final int maxTupleDepth;
        final int maxTupleLen;

        final int maxArrayLen;
        final int maxArrayDepth;

        final long seed;

        Params(long seed) {
            this.seed = seed;
            this.maxTupleDepth = DEFAULT_MAX_TUPLE_DEPTH;
            this.maxTupleLen = DEFAULT_MAX_TUPLE_LENGTH;
            this.maxArrayDepth = DEFAULT_MAX_ARRAY_DEPTH;
            this.maxArrayLen = DEFAULT_MAX_ARRAY_LENGTH;
        }

        Params(long seed, int maxTupleDepth, int maxTupleLen, int maxArrayDepth, int maxArrayLen) {
            this.seed = seed;
            this.maxTupleDepth = maxTupleDepth;
            this.maxTupleLen = maxTupleLen;
            this.maxArrayDepth = maxArrayDepth;
            this.maxArrayLen = maxArrayLen;
        }

        Params(String paramsString) {
            String[] tokens = paramsString.substring(1, paramsString.length() - 1).split("[,]");
            this.seed = Long.parseLong(tokens[0]);
            this.maxTupleDepth = Integer.parseInt(tokens[1]);
            this.maxTupleLen = Integer.parseInt(tokens[2]);
            this.maxArrayDepth = Integer.parseInt(tokens[3]);
            this.maxArrayLen = Integer.parseInt(tokens[4]);
        }

        @Override
        public String toString() {
            // (seed,mtd,mtl,mad,mal)
            return "(" + seed + ',' + maxTupleDepth + ',' + maxTupleLen + ',' + maxArrayDepth + ',' + maxArrayLen + ')';
        }
    }

    private static final int NUM_TUPLES_ADDED = 17; // 17
    private static final int NUM_FIXED_ADDED = 50;

    private static final List<String> FIXED_LIST;

    private static String[] CANONICAL_BASE_TYPE_STRINGS;

    private static final String TUPLE_KEY = "(...)";

    private static final int FIXED_START_INDEX;

    static {

        Map<String, BaseTypeInfo> baseInfoTypeMap = new HashMap<>(BaseTypeInfo.getBaseTypeInfoMap());

        FIXED_LIST = generateFixedList();

        final Set<String> keySet = baseInfoTypeMap.keySet();
        final int numKeys = keySet.size();
        FIXED_START_INDEX = numKeys + NUM_TUPLES_ADDED;
        String[] arr = new String[numKeys + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String canonical : keySet) {
            arr[i++] = canonical;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        CANONICAL_BASE_TYPE_STRINGS = arr;
    }

    private static List<String> generateFixedList() {
        Map<String, BaseTypeInfo> fixedMap = new HashMap<>();
        BaseTypeInfo.putFixed(0, fixedMap, true);
        BaseTypeInfo.putFixed(10000, fixedMap, false);
        return Collections.unmodifiableList(new ArrayList<>(fixedMap.keySet()));
    }

    private final Params params;

    final String canonicalSignature;
    private final Tuple argsTuple;

    MonteCarloTestCase(Params params) throws ParseException {
        this.params = params;

        final Random rng = new Random(params.seed);

        // insert random elements from FIXED_LIST
        final int size = FIXED_LIST.size();
        for (int i = 0; i < NUM_FIXED_ADDED; i++) {
            CANONICAL_BASE_TYPE_STRINGS[FIXED_START_INDEX + i] = FIXED_LIST.get(rng.nextInt(size));
        }

        String rawSig = generateFunctionSignature(rng, 0);

        Function function = new Function(rawSig);

        this.canonicalSignature = function.getCanonicalSignature();

        this.argsTuple = generateTuple(function.paramTypes, rng);
    }

    MonteCarloTestCase(long seed) throws ParseException {
        this(new Params(seed));
    }

    boolean run() {
        Function function = function();

        ByteBuffer abi = function.encodeCall(argsTuple);

        byte[] array = abi.array();

//        EncodeTest.printABI(abi.array());

        final Tuple out = function.decodeCall(array);

        boolean equal = argsTuple.equals(out);
//        System.out.println(equal);

        if(!equal) {
            try {
                findInequality(function.paramTypes, argsTuple, out);
                throw new RuntimeException(function.getCanonicalSignature());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    Function function() {
        try {
            return new Function(canonicalSignature);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateFunctionSignature(Random r, int tupleDepth) throws ParseException {

        StackableType<?>[] types = new StackableType<?>[r.nextInt(1 + params.maxTupleLen)]; // 0 to max
        for (int i = 0; i < types.length; i++) {
            types[i] = generateType(r, tupleDepth);
        }
//        TupleType tupleType = TupleType.create(TUPLE_BASE_TYPE_STRING, types);
//        TupleType tupleType = generateTupleType(r, tupleDepth);

        StringBuilder signature = new StringBuilder("(");
        for (StackableType<?> t : types) {
            signature.append(t.canonicalType).append(',');
        }
        if (types.length > 0) {
            signature.replace(signature.length() - 1, signature.length(), "");
        }
        signature.append(')');

        return signature.toString();
    }

    private StackableType<?> generateType(Random r, final int tupleDepth) throws ParseException {
        int index = r.nextInt(CANONICAL_BASE_TYPE_STRINGS.length);
        String baseTypeString = CANONICAL_BASE_TYPE_STRINGS[index];

        StringBuilder sb;

        TupleType tupleType = null;
        if(baseTypeString.equals(TUPLE_KEY)) {
            tupleType = generateTupleType(r, tupleDepth + 1);
            sb = new StringBuilder(tupleType.canonicalType);
        } else {
            sb = new StringBuilder(baseTypeString);
        }

        boolean isElement = r.nextBoolean() && r.nextBoolean();
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
        return tupleType != null
                ? TypeFactory.createForTuple(canonicalTypeString, tupleType)
                : TypeFactory.create(canonicalTypeString);
    }

    private TupleType generateTupleType(Random r, int tupleDepth) throws ParseException {
        if(tupleDepth >= params.maxTupleDepth) {
            return TupleType.create("()");
        }
        return new Function(generateFunctionSignature(r, tupleDepth)).paramTypes;
    }

    private Tuple generateTuple(TupleType tupleType, Random r) {
        final StackableType<?>[] types = tupleType.elementTypes;
        Object[] args = new Object[types.length];

        for (int i = 0; i < types.length; i++) {
            args[i] = generateValue(types[i], r);
        }

        return new Tuple(args);
    }

    private Object generateValue(StackableType<?> type, Random r) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return r.nextBoolean();
        case TYPE_CODE_BYTE: return generateByte(r);
        case TYPE_CODE_SHORT: return generateShort(r);
        case TYPE_CODE_INT: return generateInt(r, ((AbstractUnitType<?>) type).bitLength);
        case TYPE_CODE_LONG: return generateLong(r, ((AbstractUnitType<?>) type).bitLength);
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, ((AbstractUnitType<?>) type).bitLength);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, ((AbstractUnitType<?>) type).bitLength, ((BigDecimalType) type).scale);
        case TYPE_CODE_ARRAY: return generateArray((ArrayType) type, r);
        case TYPE_CODE_TUPLE: return generateTuple((TupleType) type, r);
        default: throw new IllegalArgumentException("unexpected type: " + type.toString());
        }
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
        byte[] buffer = new byte[1 + r.nextInt(bitLimit >>> 3)]; // 1-4
        return new BigInteger(buffer).intValueExact();
    }

    private static long generateLong(Random r, int bitLimit) {
        byte[] random = new byte[1 + r.nextInt(bitLimit >>> 3)]; // 1-8
        r.nextBytes(random);
        return new BigInteger(random).longValueExact();
    }

    private static BigInteger generateBigInteger(Random r, int bitLimit) {
        if((bitLimit & 0b111) == 7) {
            return generateSignedBigInteger(r, bitLimit);
        }
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

    private static BigInteger generateSignedBigInteger(Random r, int bitLimit) {
        return generateBigInteger(r, bitLimit + 1).divide(BigInteger.valueOf(2L));
    }

    private static BigDecimal generateBigDecimal(Random r, int bitLimit, int scale) {
        return new BigDecimal(generateBigInteger(r, bitLimit), scale);
    }

    private Object generateArray(ArrayType arrayType, Random r) {
        StackableType<?> elementType = arrayType.elementType;
        final int len = arrayType.length == DYNAMIC_LENGTH
                ? r.nextInt(params.maxArrayLen + 1) // 0 to max
                : arrayType.length;

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return generateBooleanArray(len, r);
        case TYPE_CODE_BYTE:
            if (arrayType.className().equals(STRING_CLASS_NAME)) {
                return generateString(len, r);
            }
            return generateByteArray(len, r);
        case TYPE_CODE_SHORT: return generateShortArray(len, r);
        case TYPE_CODE_INT: return generateIntArray(len, ((AbstractUnitType<?>) elementType).bitLength, r);
        case TYPE_CODE_LONG: return generateLongArray(len, ((AbstractUnitType<?>) elementType).bitLength, r);
        case TYPE_CODE_BIG_INTEGER: return generateBigIntegerArray(len, ((AbstractUnitType<?>) elementType).bitLength, r);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimalArray(len, ((AbstractUnitType<?>) elementType).bitLength, ((BigDecimalType) elementType).scale, r);
        case TYPE_CODE_ARRAY: return generateObjectArray((ArrayType) elementType, len, r);
        case TYPE_CODE_TUPLE: return generateTupleArray((TupleType) elementType, len, r);
        default: throw new IllegalArgumentException("unexpected element type: " + elementType.toString());
        }
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
            bigDecs[i] = generateBigDecimal(r, elementBitLimit, elementScale);
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

        Object[] dest = (Object[]) Array.newInstance(elementType.clazz, len);

        for (int i = 0; i < len; i++) {
            dest[i] = generateArray(elementType, r);
        }
        return dest;
    }

    // ------------------------------------------------------------------------

    private static void findInequality(TupleType tupleType, Tuple in, Tuple out) throws Exception {
        final int len = tupleType.elementTypes.length;
        for (int i = 0; i < len; i++) {
            StackableType<?> type = tupleType.elementTypes[i];
            findInequality(type, in.elements[i], out.elements[i]);
        }
    }

    private static void findInequality(StackableType<?> elementType, Object in, Object out) throws Exception {
        System.out.println("findInequality(" + elementType.getClass().getName() + ')');
        if(elementType instanceof AbstractUnitType<?>) {
            findInequality((AbstractUnitType<?>) elementType, in, out);
        } else if(elementType instanceof TupleType) {
            findInequality((TupleType) elementType, (Tuple) in, (Tuple) out);
        } else if(elementType instanceof ArrayType) {
            findInequalityInArray((ArrayType) elementType, (Object[]) in, (Object[]) out);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }
    }

    private static void findInequality(AbstractUnitType<?> abstractUnitType, Object in, Object out) throws Exception {
        if(!in.equals(out)) {
            if(in instanceof BigInteger && out instanceof BigInteger) {
                System.err.println("bitLength: " + ((BigInteger) in).bitLength() + " =? " + ((BigInteger) out).bitLength());
            }
            System.err.println(in + " != " + out + " " + abstractUnitType.bitLength);
            throw new Exception();
        }
    }

    private static void findInequalityInArray(ArrayType arrayType, Object[] in, Object[] out) throws Exception {
        final StackableType<?> elementType = arrayType.elementType;
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