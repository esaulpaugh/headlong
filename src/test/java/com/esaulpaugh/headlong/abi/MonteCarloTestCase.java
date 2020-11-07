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
import com.esaulpaugh.headlong.abi.util.BizarroIntegers;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.SuperSerial;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonteCarloTestCase implements Serializable {

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
        final String[] orderedKeys = {
                "address", "bool", "bytes", "bytes1", "bytes10", "bytes11", "bytes12", "bytes13", "bytes14", "bytes15",
                "bytes16", "bytes17", "bytes18", "bytes19", "bytes2", "bytes20", "bytes21", "bytes22", "bytes23",
                "bytes24", "bytes25", "bytes26", "bytes27", "bytes28", "bytes29", "bytes3", "bytes30", "bytes31",
                "bytes32", "bytes4", "bytes5", "bytes6", "bytes7", "bytes8", "bytes9", "decimal", "fixed",
                "fixed128x18", "function", "int", "int104", "int112", "int120", "int128", "int136", "int144", "int152",
                "int16", "int160", "int168", "int176", "int184", "int192", "int200", "int208", "int216", "int224",
                "int232", "int24", "int240", "int248", "int256", "int32", "int40", "int48", "int56", "int64", "int72",
                "int8", "int80", "int88", "int96", "string", "ufixed", "ufixed128x18", "uint", "uint104", "uint112",
                "uint120", "uint128", "uint136", "uint144", "uint152", "uint16", "uint160", "uint168", "uint176",
                "uint184", "uint192", "uint200", "uint208", "uint216", "uint224", "uint232", "uint24", "uint240",
                "uint248", "uint256", "uint32", "uint40", "uint48", "uint56", "uint64", "uint72", "uint8", "uint80",
                "uint88", "uint96",
        };

        final String[] fromFactory = TypeFactory.SUPPLIER_MAP.keySet().toArray(new String[0]);
        Arrays.sort(fromFactory);
        assertArrayEquals(orderedKeys, fromFactory);

        final String[] arr = new String[orderedKeys.length + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String key : orderedKeys) {
            arr[i++] = key;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        BASE_TYPES = ThreadLocal.withInitial(() -> Arrays.copyOf(arr, arr.length));

        FIXED_START_INDEX = arr.length - NUM_FIXED_ADDED;

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

        final String[] baseTypes = BASE_TYPES.get();

        // insert random elements from FIXED_LIST
        final int size = FIXED_LIST.size();
        for (int i = 0; i < NUM_FIXED_ADDED; i++) {
            baseTypes[FIXED_START_INDEX + i] = FIXED_LIST.get(rng.nextInt(size));
        }

        this.rawSignature = generateFunctionName(rng) + generateTupleTypeString(baseTypes, rng, 0);
        this.function = new Function(rawSignature, null, md);
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
        instance.setSeed(seed + 512);
        byte[] encoded = runStandard().array();
        runFuzzDecode(encoded, instance);
        runSuperSerial();
        runPacked();
        runFuzzPackedDecode(instance);
    }

    ByteBuffer runStandard() {
        final Tuple args = this.argsTuple;
        ByteBuffer bb = function.encodeCall(args);
        if (!args.equals(function.decodeCall((ByteBuffer) bb.flip()))) {
            throw new IllegalArgumentException(seed + " " + function.getCanonicalSignature() + " " + args);
        }
        return bb;
    }

    private void runFuzzDecode(byte[] babar, Random r) {
        final int idx = r.nextInt(babar.length);
//        final byte target = babar[idx];
        final byte addend = (byte) (1 + r.nextInt(255));
        babar[idx] += addend;
        boolean equal = false;
        Tuple decoded = null;
        try {
            decoded = function.decodeCall(babar);
            equal = this.argsTuple.equals(decoded);
        } catch (IllegalArgumentException ignored) {
            /* do nothing */
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
//        if (equal) {
//            if(false) { // strings cause false positives
//                String change = target + " --> " + babar[idx] + " (0x" + Strings.encode(target) + " --> 0x" + Strings.encode(babar[idx]) + ")";
//                try {
//                    Thread.sleep(new Random(seed + 2).nextInt(50)); // deconflict timing of writes to System.err below
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                final Tuple args = this.argsTuple;
//                System.err.println(change);
//                System.err.println(function.getParamTypes() + "\n" + Function.formatCall(babar) + "\nidx=" + idx);
//                System.err.println(Strings.encode(babar, 0, idx, Strings.HEX));
//                System.err.println(SuperSerial.serialize(function.getParamTypes(), args, true));
//                System.err.println(SuperSerial.serialize(function.getParamTypes(), decoded, true));
//                throw new IllegalArgumentException("idx=" + idx + " " + seed + " " + function.getCanonicalSignature() + " " + args);
//            }
//        }
    }

    private void runFuzzPackedDecode(Random r) {
        final TupleType tt = this.function.getParamTypes();
        final Tuple args = this.argsTuple;
        final int packedLen = tt.byteLengthPacked(args);
        if(packedLen == 0) {
            return;
        }
        ByteBuffer packed = ByteBuffer.allocate(packedLen);
        tt.encodePacked(args, packed);
        final byte[] parr = packed.array();
        final int idx = r.nextInt(parr.length);
//        final byte target = parr[idx];
        final byte addend = (byte) (1 + r.nextInt(255));
        parr[idx] += addend;
//        boolean equal = false;
        Tuple decoded = null;
        try {
            decoded = PackedDecoder.decode(tt, parr);
//            equal = args.equals(decoded);
        } catch (IllegalArgumentException ignored) {
            /* do nothing */
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
//        if (equal) {
//            if(false) { // strings cause false positives
//                throw new Error();
//            }
//        }
    }

    void runPacked() {
        final Tuple args = this.argsTuple;
        final TupleType tt = this.function.getParamTypes();
        if(tt.canonicalType.contains("int[")) {
            throw new AssertionError("failed canonicalization!");
        }
        try {
            ByteBuffer bb = tt.encodePacked(args);
            Tuple decoded = PackedDecoder.decode(tt, bb.array());
            if (!decoded.equals(args)) {
                throw new RuntimeException("not equal: " + tt.canonicalType);
            }
        } catch (IllegalArgumentException iae) {
            String msg = iae.getMessage();
            if(!"multiple dynamic elements".equals(msg)
                    && !"array of dynamic elements".equals(msg)
                    && !"can't decode dynamic number of zero-length elements".equals(msg)
                    && !msg.startsWith("illegal boolean value: ")) {
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

    private String generateTupleTypeString(String[] baseTypes, Random r, int tupleDepth) {
        final int len = r.nextInt(1 + maxTupleLen);
        StringBuilder signature = new StringBuilder("(");
        for (int i = 0; i < len; i++) {
            String typeStr = generateType(baseTypes, r, tupleDepth).canonicalType;
            if(r.nextBoolean()) {
                switch (typeStr) {
                case "int256": typeStr = "int"; break;
                case "uint256": typeStr = "uint"; break;
                case "fixed128x18": typeStr = "fixed"; break;
                case "ufixed128x18": typeStr = "ufixed"; break;
                }
            }
            signature.append(typeStr).append(',');
        }
        return TupleType.completeTupleTypeString(signature);
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
        return TypeFactory.createType(sb.toString(), null);
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
        case TYPE_CODE_BYTE: return (byte) r.nextInt();
        case TYPE_CODE_INT: return (int) generateLong(r, (IntType) type);
        case TYPE_CODE_LONG: return generateLong(r, (LongType) type);
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, (BigIntegerType) type);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, (BigDecimalType) type);
        case TYPE_CODE_ARRAY: return generateArray((ArrayType<? extends ABIType<?>, ?>) type, r);
        case TYPE_CODE_TUPLE: return generateTuple((TupleType) type, r);
        default: throw new Error();
        }
    }

    private static long generateLong(Random r, UnitType<? extends Number> unitType) {
        return generateLong(r, unitType.bitLength, 1 + r.nextInt(unitType.bitLength / Byte.SIZE), unitType.unsigned);
    }

    private static long generateLong(Random r, int bitLen, int len, boolean unsigned) {
        long val = r.nextLong();
        switch (len) {
        case 1: val &= 0xFFL; break;
        case 2: val &= 0xFFFFL; break;
        case 3: val &= 0xFFFFFFL; break;
        case 4: val &= 0xFFFFFFFFL; break;
        case 5: val &= 0xFFFFFFFFFFL; break;
        case 6: val &= 0xFFFFFFFFFFFFL; break;
        case 7: val &= 0xFFFFFFFFFFFFFFL; break;
        case 8: break;
        default: throw new Error();
        }
        if(!unsigned) {
            val = r.nextBoolean() ? val : val < 0 ? -(val + 1) : (-val - 1);
            int valBitLen = val < 0 ? BizarroIntegers.bitLen(val) : Integers.bitLen(val);
            if (valBitLen >= bitLen) {
                val >>= 1;
            }
        }
        return val;
    }

    private static BigInteger generateBigInteger(Random r, UnitType<? extends Number> type) {
        byte[] magnitude = new byte[type.bitLength / Byte.SIZE];
        r.nextBytes(magnitude);
        boolean zero = true;
        for (byte b : magnitude) {
            zero &= b == 0;
        }
        BigInteger random = new BigInteger(zero ? 0 : type.unsigned || r.nextBoolean() ? 1 : -1, magnitude);
        if(!type.unsigned && random.bitLength() >= type.bitLength) {
            random = random.shiftRight(1);
        }
        return random;
    }

    private static BigDecimal generateBigDecimal(Random r, BigDecimalType type) {
        return new BigDecimal(generateBigInteger(r, type), type.scale);
    }

    private Object generateArray(ArrayType<? extends ABIType<?>, ?> arrayType, Random r) {
        ABIType<?> elementType = arrayType.elementType;
        final int typeLen = arrayType.getLength();
        final int len = DYNAMIC_LENGTH == typeLen
                ? r.nextInt(maxArrayLen + 1) // [0,max]
                : typeLen;

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

//    private static String generateASCIIString(final int len, Random r) {
//        byte[] bytes = new byte[len];
//        for(int i = 0; i < len; i++) {
//            int v;
//            do {
//                v = (char) (r.nextInt(160)); // 95) + 32
//            } while (Character.isISOControl(v));
//            if(v == '(') v = '_';
//            bytes[i] = (byte) v;
//        }
//        return new String(bytes, 0, 0, len);
//    }

    private static int[] generateIntArray(final int len, IntType intType, Random r) {
        int[] ints = new int[len];
        final int bitLen = intType.bitLength;
        final int bound = bitLen / Byte.SIZE;
        final boolean unsigned = intType.unsigned;
        for (int i = 0; i < len; i++) {
            ints[i] = (int) generateLong(r, bitLen, 1 + r.nextInt(bound), unsigned);
        }
        return ints;
    }

    private static long[] generateLongArray(final int len, LongType longType, Random r) {
        long[] longs = new long[len];
        final int bitLen = longType.bitLength;
        final int bound = bitLen / Byte.SIZE;
        final boolean unsigned = longType.unsigned;
        for (int i = 0; i < len; i++) {
            longs[i] = generateLong(r, bitLen, 1 + r.nextInt(bound), unsigned);
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
        if(!getClass().isInstance(o)) return false;
        MonteCarloTestCase other = (MonteCarloTestCase) o;
        return other.seed == this.seed
                && other.maxTupleDepth == this.maxTupleDepth
                && other.maxTupleLen == this.maxTupleLen
                && other.maxArrayDepth == this.maxArrayDepth
                && other.maxArrayLen == this.maxArrayLen
                && Objects.equals(other.function, this.function)
                && Objects.equals(other.argsTuple, this.argsTuple);
    }

    @Override
    public String toString() {
        return "(" + seed + "L," + maxTupleDepth + ',' + maxTupleLen + ',' + maxArrayDepth + ',' + maxArrayLen + ") --> " + function.getCanonicalSignature();
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
