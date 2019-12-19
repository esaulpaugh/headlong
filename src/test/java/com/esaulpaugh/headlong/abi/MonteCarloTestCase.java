/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.exception.ValidationException;
import com.esaulpaugh.headlong.abi.util.Utils;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.ArrayType.STRING_CLASS;
import static com.esaulpaugh.headlong.abi.ABIType.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonteCarloTestCase implements Serializable {

    private static final ThreadLocal<MessageDigest> KECCAK_THREAD_LOCAL = ThreadLocal.withInitial(() -> new Keccak(256));

    static class Params implements Serializable {

        private static final int DEFAULT_MAX_TUPLE_DEPTH = 3;
        private static final int DEFAULT_MAX_TUPLE_LENGTH = 3;

        private static final int DEFAULT_MAX_ARRAY_DEPTH = 3;
        private static final int DEFAULT_MAX_ARRAY_LENGTH = 3; // does not apply to static base types e.g. bytes1-32

        private final int maxTupleDepth;
        private final int maxTupleLen;

        private final int maxArrayDepth;
        private final int maxArrayLen;

        private final long seed;

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
        public int hashCode() {
            return Objects.hash(maxTupleDepth, maxTupleLen, maxArrayLen, maxArrayDepth, seed);
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
        public String toString() {
            // (seed,mtd,mtl,mad,mal)
            return "(" + seed + ',' + maxTupleDepth + ',' + maxTupleLen + ',' + maxArrayDepth + ',' + maxArrayLen + ')';
        }
    }

    private static final int NUM_TUPLES_ADDED = 17; // 17
    private static final int NUM_FIXED_ADDED = 50;

    private static final List<String> FIXED_LIST;

    private static final String[] CANONICAL_BASE_TYPE_STRINGS;

    private static final String TUPLE_KEY = "(...)";

    private static final int FIXED_START_INDEX;

    static {

        Map<String, BaseTypeInfo> baseInfoTypeMap = new HashMap<>(BaseTypeInfo.getBaseTypeInfoMap());

        final Set<String> keySet = baseInfoTypeMap.keySet();
        ArrayList<String> ordered = new ArrayList<>(keySet);
        Collections.sort(ordered);
        final int numKeys = ordered.size();

        String[] arr = new String[numKeys + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String canonical : ordered) {
            arr[i++] = canonical;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        CANONICAL_BASE_TYPE_STRINGS = arr;

        FIXED_START_INDEX = numKeys + NUM_TUPLES_ADDED;
        FIXED_LIST = Collections.unmodifiableList(genOrderedFixedKeys());
    }

    final Params params;
    final String rawSignature;
    final Function function;
    final Tuple argsTuple;

    MonteCarloTestCase(Params params) {
        this.params = params;

        final Random rng = new Random(params.seed);

        // insert random elements from FIXED_LIST
        final int size = FIXED_LIST.size();
        for (int i = 0; i < NUM_FIXED_ADDED; i++) {
            CANONICAL_BASE_TYPE_STRINGS[FIXED_START_INDEX + i] = FIXED_LIST.get(rng.nextInt(size));
        }

        String sig = generateFunctionName(rng) + generateTupleTypeString(rng, 0);

        // decanonicalize
        sig = sig
                .replace("int256,", "int,")
                .replace("int256[", "int[")
                .replace("int256)", "int)")
                .replace("uint256,", "uint,")
                .replace("uint256[", "uint[")
                .replace("uint256)", "uint)")
                .replace("fixed128x18,", "fixed,")
                .replace("fixed128x18[", "fixed[")
                .replace("fixed128x18)", "fixed)")
                .replace("ufixed128x18,", "ufixed,")
                .replace("ufixed128x18[", "ufixed[")
                .replace("ufixed128x18)", "ufixed)");

        this.rawSignature = sig;
        this.function = new Function(sig, null, KECCAK_THREAD_LOCAL.get());
        this.argsTuple = generateTuple(function.getParamTypes(), rng);
    }

    MonteCarloTestCase(long seed) {
        this(new Params(seed));
    }

    JsonElement toJsonElement(Gson gson, String name, JsonPrimitive version) throws ValidationException {

        Function f = Function.parse(name + this.function.getParamTypes().canonicalType); // this.function;

//        System.out.println(f.getCanonicalSignature());

        ByteBuffer abi = f.encodeCall(this.argsTuple);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("name", new JsonPrimitive(name));
        jsonObject.add("types", serializeTypes(f.getParamTypes(), gson));
        jsonObject.add("values", serializeValues(this.argsTuple, gson));
        jsonObject.add("result", new JsonPrimitive("0x" + FastHex.encodeToString(abi.array())));
        jsonObject.add("version", version);

        return jsonObject;
    }

    private static JsonPrimitive serializeTypes(TupleType tupleType, Gson gson) {
        JsonArray array = new JsonArray();

        for(ABIType<?> type : tupleType) {
            array.add(new JsonPrimitive(type.canonicalType.replace("(", "tuple(")));
        }
        return new JsonPrimitive(gson.toJson(array));
    }

    private static JsonPrimitive serializeValues(Tuple tuple, Gson gson) {
        JsonArray valuesArray = new JsonArray();
        for(Object val : tuple) {
            valuesArray.add(toJsonElement(val));
        }
        return new JsonPrimitive(gson.toJson(valuesArray));
    }

    private static JsonElement toJsonElement(Object val) {
        if(val instanceof Boolean) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("bool"));
            object.add("value", new JsonPrimitive(val.toString()));
            return object;
        } else if(val instanceof Integer || val instanceof Long) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("number"));
            object.add("value", new JsonPrimitive(val.toString()));
            return object;
        } else if(val instanceof BigInteger) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("string"));
            object.add("value", new JsonPrimitive("0x" + FastHex.encodeToString(((BigInteger) val).toByteArray())));
            return object;
        } else if(val instanceof BigDecimal) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("number"));
            object.add("value", new JsonPrimitive(((BigDecimal) val).unscaledValue().toString()));
            return object;
        } else if(val instanceof byte[]) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("buffer"));
            object.add("value", new JsonPrimitive("0x" + FastHex.encodeToString((byte[]) val)));
            return object;
        } else if(val instanceof String) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("buffer"));
            object.add("value", new JsonPrimitive((String) val));
            return object;
        } else if(val instanceof boolean[]) {
            JsonArray array = new JsonArray();
            for(boolean e : (boolean[]) val) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof int[]) {
            JsonArray array = new JsonArray();
            for(int e : (int[]) val) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof long[]) {
            JsonArray array = new JsonArray();
            for(long e : (long[]) val) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof Object[]) {
            JsonArray array = new JsonArray();
            for(Object e : (Object[]) val) {
                array.add(toJsonElement(e));
            }
            return array;
        } else if(val instanceof Tuple) {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("tuple"));
            JsonArray array = new JsonArray();
            for(Object e : (Tuple) val) {
                array.add(toJsonElement(e));
            }
            object.add("value", array);
            return object;
        }
        throw new RuntimeException("???");
    }

    void run() throws ValidationException, DecodeException {
        run(this.argsTuple);
    }

    void runNewRandomArgs() throws ValidationException, DecodeException {
        run(generateTuple(function.getParamTypes(), new Random(System.nanoTime())));
    }

    void run(Tuple args) throws DecodeException, ValidationException {
        Function function = this.function;

        ByteBuffer abi = function.encodeCall(args);

        final Tuple out = function.decodeCall((ByteBuffer) abi.flip());

        boolean equal = args.equals(out);

        if(!equal) {
            try {
                findInequality(function.getParamTypes(), args, out);
                throw new RuntimeException(function.getCanonicalSignature());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String generateTupleTypeString(Random r, int tupleDepth) {

        ABIType<?>[] types = new ABIType<?>[r.nextInt(1 + params.maxTupleLen)]; // 0 to max
        for (int i = 0; i < types.length; i++) {
            types[i] = generateType(r, tupleDepth);
        }

        StringBuilder signature = new StringBuilder("(");
        for (ABIType<?> t : types) {
            signature.append(t.canonicalType).append(',');
        }
        if (types.length > 0) {
            signature.replace(signature.length() - 1, signature.length(), "");
        }
        signature.append(')');

        return signature.toString();
    }

    private ABIType<?> generateType(Random r, final int tupleDepth) {
        int index = r.nextInt(CANONICAL_BASE_TYPE_STRINGS.length);
        String baseTypeString = CANONICAL_BASE_TYPE_STRINGS[index];

        if(baseTypeString.equals(TUPLE_KEY)) {
            baseTypeString = tupleDepth < params.maxTupleDepth
                    ? generateTupleTypeString(r, tupleDepth + 1)
                    : "uint256";
        }

        StringBuilder sb = new StringBuilder(baseTypeString);
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
        try {
            return TypeFactory.create(sb.toString());
        } catch (ParseException pe) {
            throw Utils.illegalArgumentException(pe);
        }
    }

    private Tuple generateTuple(TupleType tupleType, Random r) {
        final ABIType<?>[] types = tupleType.elementTypes;
        Object[] args = new Object[types.length];

        for (int i = 0; i < types.length; i++) {
            args[i] = generateValue(types[i], r);
        }

        return new Tuple(args);
    }

    private Object generateValue(ABIType<?> type, Random r) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return r.nextBoolean();
        case TYPE_CODE_BYTE: return generateByte(r);
        case TYPE_CODE_INT: return generateInt(r, (IntType) type);
        case TYPE_CODE_LONG: return generateLong(r, (LongType) type, false);
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, (UnitType<?>) type);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, (BigDecimalType) type);
        case TYPE_CODE_ARRAY: return generateArray((ArrayType<?, ?>) type, r);
        case TYPE_CODE_TUPLE: return generateTuple((TupleType) type, r);
        default: throw new IllegalArgumentException("unexpected type: " + type.toString());
        }
    }

    private static byte generateByte(Random r) {
        return (byte) r.nextInt();
    }

    private static int generateInt(Random r, IntType intType) {
        byte[] buffer = new byte[1 + r.nextInt(intType.bitLength >>> 3)]; // 1-4
        int x = new BigInteger(buffer).intValue();
        if(intType.unsigned && x < 0) {
            return (-(x + 1) << 1) + (r.nextBoolean() ? 1 : 0);
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

    private static BigInteger generateBigInteger(Random r, UnitType<?> type) {
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
        ABIType<?> elementType = arrayType.elementType;
        final int len = arrayType.length == DYNAMIC_LENGTH
                ? r.nextInt(params.maxArrayLen + 1) // 0 to max
                : arrayType.length;

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return generateBooleanArray(len, r);
        case TYPE_CODE_BYTE:
            if (arrayType.clazz() == STRING_CLASS) {
                return generateString(len, r);
            }
            return generateByteArray(len, r);
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

    static String generateASCIIString(final int len, Random r) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; i++) {
            sb.append((char) (r.nextInt(95) + 32));
        }
        return sb.toString();
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

        Object[] dest = (Object[]) Array.newInstance(arrayType.elementType.clazz, len);

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
            ABIType<?> type = tupleType.elementTypes[i];
            findInequality(type, in.elements[i], out.elements[i]);
        }
    }

    private static void findInequality(ABIType<?> elementType, Object in, Object out) throws Exception {
        System.out.println("findInequality(" + elementType.getClass().getName() + ')');
        if(elementType instanceof UnitType<?>) {
            findInequality((UnitType<?>) elementType, in, out);
        } else if(elementType instanceof TupleType) {
            findInequality((TupleType) elementType, (Tuple) in, (Tuple) out);
        } else if(elementType instanceof ArrayType<?, ?>) {
            ArrayType<?, ?> arrayType = (ArrayType<?, ?>) elementType;
            if(arrayType.isString) {
                assertArrayEquals(Strings.decode((String) in, Strings.UTF_8), Strings.decode((String) out, Strings.UTF_8));
                assertEquals(in, out);
            } else {
                final Class<?> inClass = in.getClass();
                if(Object[].class.isAssignableFrom(inClass)) {
                    findInequalityInArray(arrayType, (Object[]) in, (Object[]) out);
                } else if(byte[].class.isAssignableFrom(inClass)) {
                    assertArrayEquals((byte[]) in, (byte[]) out);
                } else if(int[].class.isAssignableFrom(inClass)) {
                    assertArrayEquals((int[]) in, (int[]) out);
                } else if(long[].class.isAssignableFrom(inClass)) {
                    assertArrayEquals((long[]) in, (long[]) out);
                } else {
                    throw new RuntimeException("??");
                }
            }
        } else {
            throw new IllegalArgumentException("unrecognized type: " + elementType.toString());
        }
    }

    private static void findInequality(UnitType<?> unitType, Object in, Object out) throws Exception {
        if(!in.equals(out)) {
            if(in instanceof BigInteger && out instanceof BigInteger) {
                System.err.println("bitLen: " + ((BigInteger) in).bitLength() + " =? " + ((BigInteger) out).bitLength());
            }
            System.err.println(in + " != " + out + " " + unitType.bitLength);
            throw new Exception();
        }
    }

    private static void findInequalityInArray(ArrayType<?, ?> arrayType, Object[] in, Object[] out) throws Exception {
        final ABIType<?> elementType = arrayType.elementType;
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

    private static List<String> genOrderedFixedKeys() {
        final ArrayList<String> ordered = new ArrayList<>();
        final String signedStub = "fixed";
        final String unsignedStub = "ufixed";
        for(int M = 8; M <= 256; M += 8) {
            for (int N = 1; N <= 80; N++) {
                final String suffix = Integer.toString(M) + 'x' + N;
                ordered.add(signedStub + suffix);
                ordered.add(unsignedStub + suffix);
            }
        }
        Collections.sort(ordered);
        return ordered;
    }
}