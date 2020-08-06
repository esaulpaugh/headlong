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

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.SuperSerial;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonteCarloTestCase {

    private static final int DEFAULT_MAX_TUPLE_DEPTH = 3;
    private static final int DEFAULT_MAX_TUPLE_LENGTH = 3;

    private static final int DEFAULT_MAX_ARRAY_DEPTH = 3;
    private static final int DEFAULT_MAX_ARRAY_LENGTH = 3; // does not apply to static base types e.g. bytes1-32

    private static final int NUM_TUPLES_ADDED = 15; // 17
    private static final int NUM_FIXED_ADDED = 40;

    private static final List<String> FIXED_LIST;

    private static final ThreadLocal<String[]> BASE_TYPES;

    private static final String TUPLE_KEY = "(...)";

    private static final int FIXED_START_INDEX;

    static {

        final Map<String, BaseTypeInfo> baseInfoTypeMap = new HashMap<>(BaseTypeInfo.getBaseTypeInfoMap());

        final Set<String> keys = baseInfoTypeMap.keySet();
        final ArrayList<String> ordered = new ArrayList<>(keys);
        Collections.sort(ordered);
        final int numKeys = ordered.size();

        final String[] arr = new String[numKeys + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String canonical : ordered) {
            arr[i++] = canonical;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        BASE_TYPES = ThreadLocal.withInitial(() -> Arrays.copyOf(arr, arr.length));

        FIXED_START_INDEX = numKeys + NUM_TUPLES_ADDED;

        FIXED_LIST = Collections.unmodifiableList(genOrderedFixedKeys());
    }

    private final long seed;

    private final int maxTupleDepth;
    private final int maxTupleLen;

    private final int maxArrayDepth;
    private final int maxArrayLen;

    final String rawSignature;
    final Function function;
    final Tuple argsTuple;

    MonteCarloTestCase(long seed) {
        this(seed, DEFAULT_MAX_TUPLE_DEPTH, DEFAULT_MAX_TUPLE_LENGTH, DEFAULT_MAX_ARRAY_DEPTH, DEFAULT_MAX_ARRAY_LENGTH, new Random(), Function.newDefaultDigest());
    }

    MonteCarloTestCase(long seed, int maxTupleDepth, int maxTupleLen, int maxArrayDepth, int maxArrayLen, Random rng, MessageDigest md) {
        this.seed = seed;
        this.maxTupleDepth = maxTupleDepth;
        this.maxTupleLen = maxTupleLen;
        this.maxArrayDepth = maxArrayDepth;
        this.maxArrayLen = maxArrayLen;

        rng.setSeed(seed);

        final String[] canonicalBaseTypes = BASE_TYPES.get();

        // insert random elements from FIXED_LIST
        final int size = FIXED_LIST.size();
        for (int i = 0; i < NUM_FIXED_ADDED; i++) {
            canonicalBaseTypes[FIXED_START_INDEX + i] = FIXED_LIST.get(rng.nextInt(size));
        }

        // decanonicalize
        final String sig = (generateFunctionName(rng) + generateTupleTypeString(canonicalBaseTypes, rng, 0))
                .replace("int256,", "int,")
                .replace("int256[", "int[")
                .replace("int256)", "int)")
                .replace("fixed128x18,", "fixed,")
                .replace("fixed128x18[", "fixed[")
                .replace("fixed128x18)", "fixed)");

//        if(sig.contains("int256") || sig.contains("fixed128x18")) throw new Error(sig);

        this.rawSignature = sig;
        this.function = new Function(sig, null, md);
        this.argsTuple = generateTuple(function.getParamTypes(), rng);
    }

    JsonElement toJsonElement(Gson gson, String name, JsonPrimitive version) {

        Function f = Function.parse(name + this.function.getParamTypes().canonicalType); // this.function;

        ByteBuffer abi = f.encodeCall(this.argsTuple);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("name", new JsonPrimitive(name));
        jsonObject.add("types", Serializer.serializeTypes(f.getParamTypes(), gson));
        jsonObject.add("values", Serializer.serializeValues(this.argsTuple, gson));
        jsonObject.add("result", new JsonPrimitive("0x" + Strings.encode(abi)));
        jsonObject.add("version", version);

        return jsonObject;
    }

    void runAll(Random instance) {
        ByteBuffer bb = runStandard();
        runFuzzDecode(bb, instance);
        runPacked();
        runSuperSerial();
    }

    ByteBuffer runStandard() {
        final Tuple args = this.argsTuple;
        ByteBuffer bb = function.encodeCall(args);
        if (!args.equals(function.decodeCall((ByteBuffer) bb.flip()))) {
            throw new IllegalArgumentException(seed + " " + function.getCanonicalSignature() + " " + args);
        }
        return bb;
    }

    private void runFuzzDecode(ByteBuffer bb, Random r) {
        final Tuple args = this.argsTuple;
        r.setSeed(seed + 1);
        final byte[] babar = bb.array();
        final int idx = r.nextInt(babar.length);
        final byte target = babar[idx];
        final byte addend = (byte) (1 + r.nextInt(255));
        babar[idx] += addend;
        boolean equal = false;
        Tuple decoded = null;
        try {
            decoded = function.decodeCall(babar);
            equal = args.equals(decoded);
        } catch (IllegalArgumentException e) {
            /* do nothing */
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
        if (equal) {
            if(false) { // disabled
                String change = target + " --> " + babar[idx] + " (0x" + Strings.encode(target) + " --> 0x" + Strings.encode(babar[idx]) + ")";
                try {
                    Thread.sleep(new Random(seed + 2).nextInt(50)); // deconflict timing of writes to System.err below
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println(change);
                System.err.println(function.getParamTypes() + "\n" + Function.formatCall(babar) + "\nidx=" + idx);
                System.err.println(Strings.encode(babar, 0, idx, Strings.HEX));
                System.err.println(SuperSerial.serialize(function.getParamTypes(), args, true));
                System.err.println(SuperSerial.serialize(function.getParamTypes(), decoded, true));
                throw new IllegalArgumentException("idx=" + idx + " " + seed + " " + function.getCanonicalSignature() + " " + args);
            }
        }
    }

    void runPacked() {
        final Tuple args = this.argsTuple;
        final TupleType tt = this.function.getParamTypes();
        if(tt.canonicalType.contains("int[")) {
            throw new AssertionError("failed canonicalization!");
        }
        try {
            if (!PackedDecoder.decode(tt, tt.encodePacked(args).array()).equals(args)) {
                throw new RuntimeException("not equal: " + tt.canonicalType);
            }
        } catch (IllegalArgumentException iae) {
            String msg = iae.getMessage();
            if(!"multiple dynamic elements".equals(msg)
                    && !"array of dynamic elements".equals(msg)
                    && !"can't decode dynamic number of zero-length elements".equals(msg)) {
                throw new RuntimeException(tt.canonicalType + " " + msg, iae);
            }
        }
    }

    void runSuperSerial() {
        final Tuple args = this.argsTuple;
        final TupleType tt = this.function.getParamTypes();

        String str = SuperSerial.serialize(tt, args, false);
        Tuple deserial = SuperSerial.deserialize(tt, str, false);
        assertEquals(args, deserial);

        str = SuperSerial.serialize(tt, args, true);
        deserial = SuperSerial.deserialize(tt, str, true);
        assertEquals(args, deserial);
    }

    private String generateTupleTypeString(String[] canonicalBaseTypes, Random r, int tupleDepth) {

        ABIType<?>[] types = new ABIType<?>[r.nextInt(1 + maxTupleLen)]; // 0 to max
        for (int i = 0; i < types.length; i++) {
            types[i] = generateType(canonicalBaseTypes, r, tupleDepth);
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

    private ABIType<?> generateType(String[] canonicalBaseTypes, Random r, final int tupleDepth) {
        String baseTypeString = canonicalBaseTypes[r.nextInt(canonicalBaseTypes.length)];

        if(baseTypeString.equals(TUPLE_KEY)) {
            baseTypeString = tupleDepth < maxTupleDepth
                    ? generateTupleTypeString(canonicalBaseTypes, r, tupleDepth + 1)
                    : "uint256";
        }

        StringBuilder sb = new StringBuilder(baseTypeString);
        boolean isElement = r.nextBoolean() && r.nextBoolean();
        if(isElement) {
            int arrayDepth = 1 + r.nextInt(maxArrayDepth);
            for (int i = 0; i < arrayDepth; i++) {
                sb.append('[');
                if(r.nextBoolean()) {
                    sb.append(r.nextInt(maxArrayLen + 1));
                }
                sb.append(']');
            }
        }
        return TypeFactory.create(sb.toString(), null);
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
        case TYPE_CODE_LONG: return generateLong(r, (LongType) type);
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, (UnitType<?>) type);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, (BigDecimalType) type);
        case TYPE_CODE_ARRAY: return generateArray((ArrayType<? extends ABIType<?>, ?>) type, r);
        case TYPE_CODE_TUPLE: return generateTuple((TupleType) type, r);
        default: throw new Error();
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

    private static long generateLong(Random r, LongType longType) {
        byte[] random = TestUtils.randomBytes(1 + r.nextInt(longType.bitLength / Byte.SIZE) /* 1-8 */, r);
        long x = new BigInteger(random).longValue();
        if(longType.unsigned && x < 0) {
            return ((-(x + 1)) << 1) + (r.nextBoolean() ? 1 : 0);
        }
        return x;
    }

    private static BigInteger generateBigInteger(Random r, UnitType<?> type) {
        byte[] thirtyTwo = new byte[UNIT_LENGTH_BYTES];
        final int len = 1 + r.nextInt(type.bitLength / Byte.SIZE); // 1-32
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

    private Object generateArray(ArrayType<? extends ABIType<?>, ?> arrayType, Random r) {
        ABIType<?> elementType = arrayType.elementType;
        final int len = arrayType.length == DYNAMIC_LENGTH
                ? r.nextInt(maxArrayLen + 1) // 0 to max
                : arrayType.length;

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return generateBooleanArray(len, r);
        case TYPE_CODE_BYTE:
            if (arrayType.isString()) {
                return generateUtf8String(len, r);
            }
            return TestUtils.randomBytes(len, r);
        case TYPE_CODE_INT: return generateIntArray(len, (IntType) elementType, r);
        case TYPE_CODE_LONG: return generateLongArray(len, (LongType) elementType, r);
        case TYPE_CODE_BIG_INTEGER: return generateBigIntegerArray(len, (BigIntegerType) elementType, r);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimalArray(len, (BigDecimalType) elementType, r);
        case TYPE_CODE_ARRAY: return generateObjectArray(arrayType, len, r);
        case TYPE_CODE_TUPLE: return generateTupleArray((TupleType) elementType, len, r);
        default: throw new Error();
        }
    }

    private static String generateFunctionName(Random r) {
        return generateASCIIString(r.nextInt(34), r).replace('(', '_');
    }

    private static String generateUtf8String(int len, Random r) {
        return Strings.encode(TestUtils.randomBytes(len, r), Strings.UTF_8);
    }

    private static String generateASCIIString(final int len, Random r) {
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
            longs[i] = generateLong(r, longType);
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

    private Object[] generateObjectArray(ArrayType<? extends ABIType<?>, ?> arrayType, final int len, Random r) {

        Object[] dest = (Object[]) Array.newInstance(arrayType.elementType.clazz, len);

        final ArrayType<? extends ABIType<?>, ?> elementType = (ArrayType<? extends ABIType<?>, ?>) arrayType.elementType;
        for (int i = 0; i < len; i++) {
            dest[i] = generateArray(elementType, r);
        }
        return dest;
    }
    // ------------------------------------------------------------------------
    @Override
    public int hashCode() {
        return Objects.hash(seed, maxTupleDepth, maxTupleLen, maxArrayDepth, maxArrayLen, function, argsTuple);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonteCarloTestCase that = (MonteCarloTestCase) o;
        return seed == that.seed
                && maxTupleDepth == that.maxTupleDepth
                && maxTupleLen == that.maxTupleLen
                && maxArrayDepth == that.maxArrayDepth
                && maxArrayLen == that.maxArrayLen
                && Objects.equals(function, that.function)
                && Objects.equals(argsTuple, that.argsTuple);
    }

    @Override
    public String toString() {
        return "(" + seed + ',' + maxTupleDepth + ',' + maxTupleLen + ',' + maxArrayDepth + ',' + maxArrayLen + ") --> " + function.getCanonicalSignature();
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
