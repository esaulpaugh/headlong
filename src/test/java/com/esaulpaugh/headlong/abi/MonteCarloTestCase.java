package com.esaulpaugh.headlong.abi;

import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;

import static com.esaulpaugh.headlong.abi.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS_NAME;
import static com.esaulpaugh.headlong.abi.StackableType.*;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.junit.Assert;

public class MonteCarloTestCase implements Serializable {

    private static final long serialVersionUID = -7544539781150389976L;

    private static final ThreadLocal<MessageDigest> KECCAK_THREAD_LOCAL = new ThreadLocal<MessageDigest>() {
        @Override
        public MessageDigest initialValue() {
            return new Keccak(256);
        }
    };

    static class Params implements Serializable {

        private static final long serialVersionUID = 4986365275807940869L;

        static final int DEFAULT_MAX_TUPLE_DEPTH = 1;
        static final int DEFAULT_MAX_TUPLE_LENGTH = 5;

        static final int DEFAULT_MAX_ARRAY_DEPTH = 1;
        static final int DEFAULT_MAX_ARRAY_LENGTH = 33; // does not apply to static base types e.g. bytes1-32

        final int maxTupleDepth;
        final int maxTupleLen;

        final int maxArrayDepth;
        final int maxArrayLen;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Params params = (Params) o;
            return maxTupleDepth == params.maxTupleDepth &&
                    maxTupleLen == params.maxTupleLen &&
                    maxArrayLen == params.maxArrayLen &&
                    maxArrayDepth == params.maxArrayDepth &&
                    seed == params.seed;
        }

        @Override
        public int hashCode() {
            return Objects.hash(maxTupleDepth, maxTupleLen, maxArrayLen, maxArrayDepth, seed);
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
        ArrayList<String> ordered = new ArrayList<>(keySet);
        Collections.sort(ordered);
        final int numKeys = ordered.size();
        FIXED_START_INDEX = numKeys + NUM_TUPLES_ADDED;
        String[] arr = new String[numKeys + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String canonical : ordered) {
            arr[i++] = canonical;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        CANONICAL_BASE_TYPE_STRINGS = arr;
    }

    private static List<String> generateFixedList() {
        Map<String, BaseTypeInfo> fixedMap = new HashMap<>();
        BaseTypeInfo.putFixed(fixedMap, true);
        BaseTypeInfo.putFixed(fixedMap, false);
        ArrayList<String> ordered = new ArrayList<>(fixedMap.keySet());
        Collections.sort(ordered);
        return Collections.unmodifiableList(ordered);
    }

    final Params params;
    final Function function;
    final Tuple argsTuple;

    MonteCarloTestCase(Params params) throws ParseException {
        this.params = params;

        final Random rng = new Random(params.seed);

        // insert random elements from FIXED_LIST
        final int size = FIXED_LIST.size();
        for (int i = 0; i < NUM_FIXED_ADDED; i++) {
            CANONICAL_BASE_TYPE_STRINGS[FIXED_START_INDEX + i] = FIXED_LIST.get(rng.nextInt(size));
        }

        String rawSig = generateFunctionSignature(rng, 0);

        this.function = new Function(rawSig, KECCAK_THREAD_LOCAL.get());
        this.argsTuple = generateTuple(function.paramTypes, rng);
    }

    MonteCarloTestCase(long seed) throws ParseException {
        this(new Params(seed));
    }

    boolean run() {
        return run(this.argsTuple);
    }

    boolean runNewRandomArgs() {
        return run(generateTuple(function.paramTypes, new Random(System.nanoTime())));
    }

    boolean run(Tuple args) {
        Function function = this.function;

//        System.out.println(function.getCanonicalSignature());

        ByteBuffer abi = function.encodeCall(args);

//        byte[] array = abi.array();

//        EncodeTest.printABI(abi.array());

        final Tuple out = function.decodeCall((ByteBuffer) abi.flip());

        boolean equal = args.equals(out);

        if(!equal) {
            try {
                findInequality(function.paramTypes, args, out);
                throw new RuntimeException(function.getCanonicalSignature());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private String generateFunctionSignature(Random r, int tupleDepth) throws ParseException {

        StackableType<?>[] types = new StackableType<?>[r.nextInt(1 + params.maxTupleLen)]; // 0 to max
        for (int i = 0; i < types.length; i++) {
            types[i] = generateType(r, tupleDepth);
        }

        StringBuilder signature = new StringBuilder(generateFunctionName(r) + "(");
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
            if(tupleDepth < params.maxTupleDepth) {
                tupleType = generateTupleType(r, tupleDepth + 1);
                sb = new StringBuilder(tupleType.canonicalType);
            } else {
                sb = new StringBuilder("uint256");
            }
        } else {
            sb = new StringBuilder(baseTypeString);
        }

        boolean isElement = r.nextBoolean() && r.nextBoolean();
        if(isElement) {
            int arrayDepth = 1 + r.nextInt(params.maxArrayDepth);
            for (int i = 0; i < arrayDepth; i++) {
                sb.append('[');
                if(r.nextBoolean()) {
                    sb.append(r.nextInt(params.maxArrayLen + 1));
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
//        if(tupleDepth >= params.maxTupleDepth) {
//            return TupleType.create("()");
//        }
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
//        case TYPE_CODE_SHORT: return generateShort(r, ((ShortType) type).unsigned);
        case TYPE_CODE_INT: return generateInt(r, (IntType) type);
        case TYPE_CODE_LONG: return generateLong(r, (LongType) type, false);
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, (AbstractUnitType<?>) type);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, (BigDecimalType) type);
        case TYPE_CODE_ARRAY: return generateArray((ArrayType<?, ?>) type, r);
        case TYPE_CODE_TUPLE: return generateTuple((TupleType) type, r);
        default: throw new IllegalArgumentException("unexpected type: " + type.toString());
        }
    }

    private static byte generateByte(Random r) {
        return (byte) r.nextInt();
    }

    private static short generateShort(Random r, boolean unsigned) {
        byte[] random = new byte[1 + r.nextInt(2)]; // 1-2 // Short.BYTES
        r.nextBytes(random);
        short x = new BigInteger(random).shortValue();
        if(unsigned && x < 0) {
            return (short) ((-(x + 1) << 1) + (r.nextBoolean() ? 1 : 0));
        }
        return x;
    }

    private static int generateInt(Random r, IntType intType) {
        byte[] buffer = new byte[1 + r.nextInt(intType.bitLength >>> 3)]; // 1-4
        int x = new BigInteger(buffer).intValue();
        if(intType.unsigned && x < 0) {
            return (-(x + 1) << 1) + (r.nextBoolean() ? 1 : 0);
//            if(y < 0) {
//                throw new Error("x,y : " + x + "," + y);
//            }
//            return y;
        }
        return x;
    }

    private static long generateLong(Random r, LongType longType, boolean isElement) {
        byte[] random = new byte[1 + r.nextInt(longType.bitLength >>> 3)]; // 1-8
        r.nextBytes(random);
        long x = new BigInteger(random).longValue();
        boolean unsigned = longType.unsigned && !isElement;
        if(unsigned && x < 0) {
            return ((-(x + 1)) << 1) + (r.nextBoolean() ? 1 : 0);
        }
        return x;
    }

    private static BigInteger generateBigInteger(Random r, AbstractUnitType<?> type) {
        byte[] thirtyTwo = new byte[UNIT_LENGTH_BYTES];
        final int len = 1 + r.nextInt(type.bitLength >>> 3); // 1-32
        boolean zero = true;
        for (int i = UNIT_LENGTH_BYTES - len; i < UNIT_LENGTH_BYTES; i++) {
            byte b = (byte) r.nextInt();
            zero &= b == 0;
            thirtyTwo[i] = b;
        }
        BigInteger nonneg = new BigInteger(zero ? 0 : 1, thirtyTwo);

        if(type.unsigned) {
            return nonneg.shiftRight(1);
        }

        BigInteger temp = nonneg.shiftRight(1);
        return r.nextBoolean() ? temp : temp.add(BigInteger.ONE).negate();
    }

    private static BigDecimal generateBigDecimal(Random r, BigDecimalType type) {
        return new BigDecimal(generateBigInteger(r, type), type.scale);
    }

    private Object generateArray(ArrayType<?, ?> arrayType, Random r) {
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
//        case TYPE_CODE_SHORT: return generateShortArray(len, ((ShortType) elementType).unsigned, r);
        case TYPE_CODE_INT: return generateIntArray(len, (IntType) elementType, r);
        case TYPE_CODE_LONG: return generateLongArray(len, (LongType) elementType, r);
        case TYPE_CODE_BIG_INTEGER: return generateBigIntegerArray(len, (BigIntegerType) elementType, r);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimalArray(len, (BigDecimalType) elementType, r);
        case TYPE_CODE_ARRAY: return generateObjectArray(arrayType, len, r);
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
        byte[] bytes = generateByteArray(len, r);
        return new String(bytes, UTF_8);
    }

    private static String generateFunctionName(Random r) {
        return generateASCIIString(r.nextInt(34), r).replace('(', '_');
    }

    private static String generateASCIIString(final int len, Random r) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; i++) {
            sb.append((char) (r.nextInt(95) + 32));
        }
        return sb.toString();
    }

    private static short[] generateShortArray(final int len, boolean unsigned, Random r) {
        short[] shorts = new short[len];
        for (int i = 0; i < len; i++) {
            shorts[i] = generateShort(r, unsigned);
        }
        return shorts;
    }

    private static int[] generateIntArray(final int len, IntType intType, Random r) {
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            ints[i] = generateInt(r, intType);
        }
        return ints;
    }

    private static long[] generateLongArray(final int len, LongType longType, Random r) {
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            longs[i] = generateLong(r, longType, true);
        }
        return longs;
    }

    private static BigInteger[] generateBigIntegerArray(final int len, BigIntegerType type, Random r) {
        BigInteger[] bigInts = new BigInteger[len];
        for (int i = 0; i < len; i++) {
            bigInts[i] = generateBigInteger(r, type);
        }
        return bigInts;
    }

    private static BigDecimal[] generateBigDecimalArray(final int len, BigDecimalType type, Random r) {
        BigDecimal[] bigDecs = new BigDecimal[len];
        for (int i = 0; i < len; i++) {
            bigDecs[i] = generateBigDecimal(r, type);
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

    private Object[] generateObjectArray(ArrayType<?, ?> arrayType, final int len, Random r) {

        if(arrayType.elementClass == null) {
            System.out.println(arrayType.toString());
        }
        Object[] dest = (Object[]) Array.newInstance(arrayType.elementClass, len);

        final ArrayType<?, ?> elementType = (ArrayType<?, ?>) arrayType.elementType;
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
        } else if(elementType instanceof ArrayType<?, ?>) {
            ArrayType<?, ?> arrayType = (ArrayType<?, ?>) elementType;
            if(arrayType.isString) {
                Assert.assertArrayEquals(Strings.decode((String) in, Strings.UTF_8), Strings.decode((String) out, Strings.UTF_8));
                Assert.assertEquals(in, out);
            } else {
                if(Object[].class.isAssignableFrom(in.getClass())) {
                    findInequalityInArray(arrayType, (Object[]) in, (Object[]) out);
                } else if(byte[].class.isAssignableFrom(in.getClass())) {
                    Assert.assertArrayEquals((byte[]) in, (byte[]) out);
                } else if(int[].class.isAssignableFrom(in.getClass())) {
                    Assert.assertArrayEquals((int[]) in, (int[]) out);
                } else if(long[].class.isAssignableFrom(in.getClass())) {
                    Assert.assertArrayEquals((long[]) in, (long[]) out);
                } else {
                    throw new RuntimeException("??");
                }
            }
        } else {
            throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }
    }

    private static void findInequality(AbstractUnitType<?> abstractUnitType, Object in, Object out) throws Exception {
        if(!in.equals(out)) {
            if(in instanceof BigInteger && out instanceof BigInteger) {
                System.err.println("bitLen: " + ((BigInteger) in).bitLength() + " =? " + ((BigInteger) out).bitLength());
            }
            System.err.println(in + " != " + out + " " + abstractUnitType.bitLength);
            throw new Exception();
        }
    }

    private static void findInequalityInArray(ArrayType<?, ?> arrayType, Object[] in, Object[] out) throws Exception {
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

    @Override
    public int hashCode() {
        return Objects.hash(params, function, argsTuple);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonteCarloTestCase that = (MonteCarloTestCase) o;
        return Objects.equals(params, that.params) &&
                Objects.equals(function, that.function) &&
                Objects.equals(argsTuple, that.argsTuple);
    }
}