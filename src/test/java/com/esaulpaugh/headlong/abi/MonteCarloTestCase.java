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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ADDRESS;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_DECIMAL;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BIG_INTEGER;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BOOLEAN;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_BYTE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_INT;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_LONG;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.ArrayType.DYNAMIC_LENGTH;
import static com.esaulpaugh.headlong.abi.Address.ADDRESS_BIT_LEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonteCarloTestCase {

    private static final int DEFAULT_MAX_TUPLE_DEPTH = 3;
    private static final int DEFAULT_MAX_TUPLE_LENGTH = 3;

    private static final int DEFAULT_MAX_ARRAY_DEPTH = 3;
    private static final int DEFAULT_MAX_ARRAY_LENGTH = 3; // does not apply to static base types e.g. bytes1-32

    private static final int NUM_TUPLES_ADDED = 15; // 17
    private static final int NUM_FIXED_ADDED = 40;

    private static final String[] FIXED_TYPES = genFixedKeys();

    private static final String[] BASE_TYPES;

    private static final String TUPLE_KEY = new String();
    private static final String FIXED_KEY = new String();

    static {
        final String[] orderedKeys = {
                "address", "bool", "bytes", "bytes1", "bytes10", "bytes11", "bytes12", "bytes13", "bytes14", "bytes15",
                "bytes16", "bytes17", "bytes18", "bytes19", "bytes2", "bytes20", "bytes21", "bytes22", "bytes23",
                "bytes24", "bytes25", "bytes26", "bytes27", "bytes28", "bytes29", "bytes3", "bytes30", "bytes31",
                "bytes32", "bytes4", "bytes5", "bytes6", "bytes7", "bytes8", "bytes9", "fixed168x10", "fixed128x18",
                "fixed128x18", "fixed168x10", "function", "int256", "int104", "int112", "int120", "int128", "int136",
                "int144", "int152", "int16", "int160", "int168", "int176", "int184", "int192", "int200", "int208",
                "int216", "int224", "int232", "int24", "int240", "int248", "int256", "int32", "int40", "int48", "int56",
                "int64", "int72", "int8", "int80", "int88", "int96", "string", "ufixed128x18", "ufixed128x18", "uint256",
                "uint104", "uint112", "uint120", "uint128", "uint136", "uint144", "uint152", "uint16", "uint160",
                "uint168", "uint176", "uint184", "uint192", "uint200", "uint208", "uint216", "uint224", "uint232",
                "uint24", "uint240", "uint248", "uint256", "uint32", "uint40", "uint48", "uint56", "uint64", "uint72",
                "uint8", "uint80", "uint88", "uint96",
        };

        assertEquals(109 /*TypeFactory.mapSize()*/, orderedKeys.length);

        for (String key : orderedKeys) {
            ABIType<?> type = TypeFactory.create(key);
            ABIType<?> baseType = ArrayType.baseType(type);
            if (baseType != ByteType.INSTANCE) {
                assertEquals(type, baseType);
            }
            assertEquals(key, type.canonicalType);
        }

        final String[] arr = new String[orderedKeys.length + NUM_TUPLES_ADDED + NUM_FIXED_ADDED];
        int i = 0;
        for (String key : orderedKeys) {
            arr[i++] = key;
        }
        for (int j = 0; j < NUM_TUPLES_ADDED; j++) {
            arr[i++] = TUPLE_KEY;
        }
        for (int j = 0; j < NUM_FIXED_ADDED; j++) {
            arr[i++] = FIXED_KEY;
        }
        BASE_TYPES = arr;
    }

    private final long seed;

    private final Limits limits;

    final String rawInputsStr;
    final Function function;
    final Tuple argsTuple;

    MonteCarloTestCase(long seed) {
        this(
                seed,
                new Limits(DEFAULT_MAX_TUPLE_DEPTH, DEFAULT_MAX_TUPLE_LENGTH, DEFAULT_MAX_ARRAY_DEPTH, DEFAULT_MAX_ARRAY_LENGTH),
                new Random(),
                Function.newDefaultDigest()
        );
    }

    MonteCarloTestCase(String init, Random instance, MessageDigest md) {
        this(parseSeed(init), parseLimits(init), instance, md);
    }

    MonteCarloTestCase(long seed, Limits limits, Random instance, MessageDigest md) {
        this.seed = seed;
        this.limits = limits;

        instance.setSeed(seed);

        StringBuilder sb = new StringBuilder(128);
        generateTupleTypeString(BASE_TYPES, instance, 0, sb);
        this.rawInputsStr = sb.toString();
        this.function = new Function(TypeEnum.FUNCTION, generateFunctionName(instance), TupleType.parse(rawInputsStr), TupleType.EMPTY, null, md);
        this.argsTuple = generateTuple(function.getInputs().elementTypes, instance);
        testDeepCopy(argsTuple);
    }

    private static long parseSeed(String init) {
        final String sub = init.substring(init.indexOf('(') + 1, init.indexOf("L,"));
        return Long.parseLong(sub);
    }

    private static MonteCarloTestCase.Limits parseLimits(String init) {
        final String sub = init.substring(init.indexOf(',') + 1, init.lastIndexOf(')'));
        final String[] tokens = sub.split(",");
        return new MonteCarloTestCase.Limits(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
    }

    String rawSignature() {
        return function.getName() + rawInputsStr;
    }

    long getSeed() {
        return seed;
    }

    private static void testDeepCopy(Tuple values) {
        final Tuple copy = values.deepCopy();
        assertNotSame(values, copy);
        assertEquals(values, copy);
        for (int i = 0; i < values.elements.length; i++) {
            final Object tElement = values.elements[i];
            final Object t2Element = copy.elements[i];
            final Class<?> c = tElement.getClass();
            assertSame(c, t2Element.getClass());
            if (c.isArray()) {
                assertNotSame(tElement, t2Element);
                if (tElement instanceof Object[]) {
                    assertTrue(Arrays.deepEquals((Object[]) tElement, (Object[]) t2Element));
                } else if (c == byte[].class) {
                    assertArrayEquals((byte[]) tElement, (byte[]) t2Element);
                } else if (c == boolean[].class) {
                    assertArrayEquals((boolean[]) tElement, (boolean[]) t2Element);
                } else if (c == int[].class) {
                    assertArrayEquals((int[]) tElement, (int[]) t2Element);
                } else if (c == long[].class) {
                    assertArrayEquals((long[]) tElement, (long[]) t2Element);
                } else {
                    throw new AssertionError();
                }
            } else {
                assertEquals(tElement, t2Element);
                if (tElement instanceof Tuple) {
                    assertNotSame(tElement, t2Element);
                } else {
                    assertSame(tElement, t2Element);
                }
            }
        }
    }

    JsonElement toJsonElement(Gson gson, String name, JsonPrimitive version) {

        Function f = Function.parse(name + this.function.getInputs().canonicalType); // this.function;

        ByteBuffer abi = f.encodeCall(this.argsTuple);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("name", new JsonPrimitive(name));
        jsonObject.add("types", Serializer.serializeTypes(f.getInputs(), gson));
        jsonObject.add("values", Serializer.serializeValues(this.argsTuple, gson));
        jsonObject.add("result", new JsonPrimitive("0x" + Strings.encode(abi)));
        jsonObject.add("version", version);

        return jsonObject;
    }

    void runAll(Random instance) {
        instance.setSeed(seed + 512);
        ByteBuffer encoded = runStandard();
        encoded.position(Function.SELECTOR_LEN);
        runDecodeIndex(encoded, instance);
//        runFuzzDecode(encoded.array(), instance);
        runSuperSerial();
        runPacked();
        runFuzzPackedDecode(instance);
        runJson(instance);
    }

    void runDecodeIndex(ByteBuffer bb, Random r) {
        final TupleType<?> tt = function.getInputs();
        final int size = tt.size();
        if (size > 0) {
            int idx = r.nextInt(size);
            assertTrue(Objects.deepEquals(argsTuple.get(idx), tt.decode(bb, idx)));
        }
    }

    void runJson(Random r) {
        if(r.nextInt(100) == 0) {
            assertEquals(function, Function.fromJson(function.toJson(r.nextBoolean())));
        }
    }

    ByteBuffer runStandard() {
        final Tuple args = this.argsTuple;
        ByteBuffer bb = function.encodeCall(args);
        final byte[] bbArr = bb.array();
        final byte[] copy = Arrays.copyOf(bbArr, bbArr.length);
        if (!args.equals(function.decodeCall(bbArr))) {
            throw new IllegalArgumentException(seed + " " + function.getCanonicalSignature() + " " + args);
        }
        assertArrayEquals(copy, bbArr);
        return bb;
    }

    private void runFuzzDecode(byte[] babar, Random r) {
        final int idx = r.nextInt(babar.length);
        final byte target = babar[idx];
        final byte addend = (byte) (1 + r.nextInt(255));
        babar[idx] += addend;
        boolean equal = false;
        Tuple decoded = null;
        try {
            decoded = function.decodeCall(babar);
            equal = this.argsTuple.equals(decoded);
        } catch (IllegalArgumentException | BufferUnderflowException ignored) {
            /* do nothing */
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
        if(equal && !function.getCanonicalSignature().contains("string")) { // strings cause false positives
            String change = target + " --> " + babar[idx] + " (0x" + Strings.encode(target) + " --> 0x" + Strings.encode(babar[idx]) + ")";
            try {
                Thread.sleep(new Random(seed + 2).nextInt(50)); // deconflict timing of writes to System.err below
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final Tuple args = this.argsTuple;
            System.err.println(change);
            System.err.println(function.getInputs() + "\n" + Function.formatCall(babar) + "\nidx=" + idx);
            System.err.println(Strings.encode(babar, 0, idx, Strings.HEX));
            System.err.println(SuperSerial.serialize(function.getInputs(), args, true));
            System.err.println(SuperSerial.serialize(function.getInputs(), decoded, true));
            throw new AssertionError("idx=" + idx + " " + seed + " " + function.getCanonicalSignature() + " " + args);
        }
    }

    private void runFuzzPackedDecode(Random r) {
        final TupleType<Tuple> tt = this.function.getInputs();
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
        boolean equal = false;
        Tuple decoded = null;
        try {
            decoded = tt.decodePacked(parr);
            equal = args.equals(decoded);
        } catch (IllegalArgumentException | ArithmeticException ignored) {
            /* do nothing */
        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t);
        }
        if (equal && !function.getCanonicalSignature().contains("string")) { // strings cause false positives
            throw new AssertionError("equal: " + function.getCanonicalSignature() + " with " + decoded);
        }
    }

    void runPacked() {
        final Tuple args = this.argsTuple;
        final TupleType<Tuple> tt = this.function.getInputs();
        if (tt.canonicalType.contains("int[")) {
            throw new AssertionError("failed canonicalization!");
        }
        try {
            PackedDecoder.checkDynamics(tt);
            Tuple decoded = tt.decodePacked(tt.encodePacked(args).array());
            if (!decoded.equals(args)) {
                throw new RuntimeException("not equal: " + tt.canonicalType);
            }
        } catch (IllegalArgumentException iae) {
            final String msg = iae.getMessage();
            if(!msg.contains("multiple dynamic elements: ")
                    && !msg.endsWith("array of dynamic elements")
                    && !"can't decode dynamic number of zero-length elements".equals(msg)) {
                throw new RuntimeException(tt.canonicalType + " " + msg, iae);
            }
        }
    }

    void runSuperSerial() {
        final Tuple args = this.argsTuple;
        final TupleType<Tuple> tt = this.function.getInputs();

        String str = SuperSerial.serialize(tt, args, false);
        Tuple deserial = SuperSerial.deserialize(tt, str, false);
        assertEquals(args, deserial);

        str = SuperSerial.serialize(tt, args, true);
        deserial = SuperSerial.deserialize(tt, str, true);
        assertEquals(args, deserial);
    }

    private void generateTupleTypeString(String[] baseTypes, Random r, int tupleDepth, StringBuilder sb) {
        int len = r.nextInt(1 + limits.maxTupleLength);
        if (len == 0) {
            sb.append("()");
            return;
        }
        sb.append('(');
        do {
            generateType(baseTypes, r, tupleDepth, sb);
            if (--len == 0) {
                break;
            }
            sb.append(',');
        } while (true);
        sb.append(')');
    }

    private void generateType(String[] canonicalBaseTypes, Random r, final int tupleDepth, StringBuilder sb) {
        String baseTypeString = canonicalBaseTypes[r.nextInt(canonicalBaseTypes.length)];

        if (baseTypeString == TUPLE_KEY) {
            if (tupleDepth < limits.maxTupleDepth) {
                generateTupleTypeString(canonicalBaseTypes, r, tupleDepth + 1, sb);
            } else {
                sb.append(r.nextBoolean() ? "uint256" : "uint");
            }
        } else if (baseTypeString == FIXED_KEY) {
            sb.append(FIXED_TYPES[r.nextInt(FIXED_TYPES.length)]);
        } else {
            if (r.nextBoolean()) {
                switch (baseTypeString) {
                case "int256": sb.append("int"); return;
                case "uint256": sb.append("uint"); return;
                case "fixed128x18": sb.append("fixed"); return;
                case "ufixed128x18": sb.append("ufixed"); return;
                }
            }
            sb.append(baseTypeString);
        }

        boolean isElement = r.nextBoolean() && r.nextBoolean();
        if(isElement) {
            int arrayDepth = r.nextInt(limits.maxArrayDepth + 1);
            for (int i = 0; i < arrayDepth; i++) {
                sb.append('[');
                if(r.nextBoolean()) {
                    sb.append(r.nextInt(limits.maxArrayLength + 1));
                }
                sb.append(']');
            }
        }
    }

    private Tuple generateTuple(ABIType<?>[] elementTypes, Random r) {
        final int size = elementTypes.length;
        Object[] args = new Object[size];
        for (int i = 0; i < size; i++) {
            args[i] = generateValue(elementTypes[i], r);
        }
        return Tuple.from(args);
    }

    private Object generateValue(ABIType<?> type, Random r) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return r.nextBoolean();
        case TYPE_CODE_BYTE: return (byte) r.nextInt();
        case TYPE_CODE_INT: return (int) generateLong(r, type.asUnitType());
        case TYPE_CODE_LONG: return generateLong(r, type.asUnitType());
        case TYPE_CODE_BIG_INTEGER: return generateBigInteger(r, type.asUnitType());
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimal(r, (BigDecimalType) type);
        case TYPE_CODE_ARRAY: return generateArray(type.asArrayType(), r);
        case TYPE_CODE_TUPLE: return generateTuple(type.asTupleType().elementTypes, r);
        case TYPE_CODE_ADDRESS: return generateAddress(r);
        default: throw new Error();
        }
    }

    private static long generateLong(Random r, UnitType<?> unitType) {
        return TestUtils.wildLong(r, unitType.unsigned, unitType.bitLength);
    }

    private static BigInteger generateBigInteger(Random r, UnitType<?> type) {
        return TestUtils.wildBigInteger(r, type.unsigned, type.bitLength);
    }

    private static BigDecimal generateBigDecimal(Random r, BigDecimalType type) {
        return new BigDecimal(generateBigInteger(r, type), type.getScale());
    }

    static Address generateAddress(Random r) {
        return new Address(new BigInteger(r.nextBoolean() ? ADDRESS_BIT_LEN : ADDRESS_BIT_LEN - r.nextInt(ADDRESS_BIT_LEN), r));
    }

    private Object generateArray(ArrayType<? extends ABIType<?>, ?, ?> arrayType, Random r) {
        final ABIType<?> elementType = arrayType.getElementType();
        final int typeLen = arrayType.getLength();
        final int len = DYNAMIC_LENGTH == typeLen
                ? r.nextInt(limits.maxArrayLength + 1) // [0,max]
                : typeLen;

        switch (elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return generateBooleanArray(len, r);
        case TYPE_CODE_BYTE:
            final byte[] random = TestUtils.randomBytes(len, r);
            return arrayType.isString() ? Strings.encode(random, Strings.UTF_8) : random;
        case TYPE_CODE_INT: return generateIntArray(len, elementType.asUnitType(), r);
        case TYPE_CODE_LONG: return generateLongArray(len, elementType.asUnitType(), r);
        case TYPE_CODE_BIG_INTEGER: return generateBigIntegerArray(len, (BigIntegerType) elementType.asUnitType(), r);
        case TYPE_CODE_BIG_DECIMAL: return generateBigDecimalArray(len, (BigDecimalType) elementType, r);
        case TYPE_CODE_ARRAY: return generateArrayArray(elementType.asArrayType(), len, r);
        case TYPE_CODE_TUPLE: return generateTupleArray(elementType.asTupleType(), len, r);
        case TYPE_CODE_ADDRESS: return generateAddressArray(len, r);
        default: throw new Error();
        }
    }

    private static String generateFunctionName(Random r) {
        return TestUtils.generateASCIIString(r.nextInt(34), r).replace('(', '_');
    }

    private static boolean[] generateBooleanArray(final int len, Random r) {
        boolean[] booleans = new boolean[len];
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = r.nextBoolean();
        }
        return booleans;
    }

    private static int[] generateIntArray(final int len, UnitType<?> intType, Random r) {
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            ints[i] = (int) generateLong(r, intType);
        }
        return ints;
    }

    private static long[] generateLongArray(final int len, UnitType<?> longType, Random r) {
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            longs[i] = generateLong(r, longType);
        }
        return longs;
    }

    private static BigInteger[] generateBigIntegerArray(final int len, UnitType<? extends Number> type, Random r) {
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

    private Object[] generateArrayArray(ArrayType<? extends ABIType<?>, ?, ?> elementType, final int len, Random r) {
        Object[] dest = ArrayType.createArray(elementType.clazz(), len);
        for (int i = 0; i < len; i++) {
            dest[i] = generateArray(elementType, r);
        }
        return dest;
    }

    private Tuple[] generateTupleArray(TupleType<?> tupleType, final int len, Random r) {
        Tuple[] tuples = new Tuple[len];
        for (int i = 0; i < len; i++) {
            tuples[i] = generateTuple(tupleType.elementTypes, r);
        }
        return tuples;
    }

    private Address[] generateAddressArray(final int len, Random r) {
        Address[] addresses = new Address[len];
        for (int i = 0; i < len; i++) {
            addresses[i] = generateAddress(r);
        }
        return addresses;
    }
    // ------------------------------------------------------------------------
    @Override
    public int hashCode() {
        return Objects.hash(seed, limits.maxTupleDepth, limits.maxTupleLength, limits.maxArrayDepth, limits.maxArrayLength, function, argsTuple);
    }

    @Override
    public boolean equals(Object o) {
        if(!getClass().isInstance(o)) return false;
        MonteCarloTestCase other = (MonteCarloTestCase) o;
        return other.seed == this.seed
                && other.limits.maxTupleDepth == this.limits.maxTupleDepth
                && other.limits.maxTupleLength == this.limits.maxTupleLength
                && other.limits.maxArrayDepth == this.limits.maxArrayDepth
                && other.limits.maxArrayLength == this.limits.maxArrayLength
                && Objects.equals(other.function, this.function)
                && Objects.equals(other.argsTuple, this.argsTuple);
    }

    @Override
    public String toString() {
        return "(" + seed + "L," + limits.maxTupleDepth + ',' + limits.maxTupleLength + ',' + limits.maxArrayDepth + ',' + limits.maxArrayLength + ") --> " + function.getCanonicalSignature();
    }

    private static String[] genFixedKeys() {
        final String[] fixedKeys = new String[2 * 32 * 80];
        int count = 0;
        for (int M = 8; M <= 256; M += 8) {
            final String Mx = Integer.toString(M) + 'x';
            for (int N = 1; N <= 80; N++) {
                final String suffix = Mx + N;
                fixedKeys[count++] = "fixed" + suffix;
                fixedKeys[count++] = "ufixed" + suffix;
            }
        }
        for (String key : fixedKeys) {
            ABIType<?> type = TypeFactory.create(key);
            assertEquals(key, type.canonicalType);
            assertEquals(type, ArrayType.baseType(type));
        }
        Arrays.sort(fixedKeys);
        return fixedKeys;
    }

    static class Limits {

        final int maxTupleDepth;
        final int maxTupleLength;
        final int maxArrayDepth;
        final int maxArrayLength;

        Limits(int maxTupleDepth, int maxTupleLength, int maxArrayDepth, int maxArrayLength) {
            this.maxTupleDepth = maxTupleDepth;
            this.maxTupleLength = maxTupleLength;
            this.maxArrayDepth = maxArrayDepth;
            this.maxArrayLength = maxArrayLength;
        }
    }
}
