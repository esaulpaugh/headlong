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
import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ABIJSONTest {

    private static final String FUNCTION_A_JSON = "{\n" +
            "  \"type\": \"function\",\n" +
            "  \"name\": \"foo\",\n" +
            "  \"inputs\": [\n" +
            "    {\n" +
            "      \"name\": \"complex_nums\",\n" +
            "      \"type\": \"tuple[][]\",\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"real\",\n" +
            "          \"type\": \"decimal\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"imaginary\",\n" +
            "          \"type\": \"decimal\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"outputs\": [\n" +
            "    {\n" +
            "      \"name\": \"count\",\n" +
            "      \"type\": \"uint64\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"constant\": false\n" +
            "}";

    private static final String FUNCTION_B_JSON = "{\n" +
            "  \"type\": \"function\",\n" +
            "  \"name\": \"func\",\n" +
            "  \"inputs\": [\n" +
            "    {\n" +
            "      \"name\": \"aa\",\n" +
            "      \"type\": \"tuple\",\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"aa_d\",\n" +
            "          \"type\": \"decimal\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"aa_f\",\n" +
            "          \"type\": \"fixed128x18\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"bb\",\n" +
            "      \"type\": \"fixed128x18[]\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"cc\",\n" +
            "      \"type\": \"tuple\",\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"cc_uint\",\n" +
            "          \"type\": \"uint256\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"cc_int_arr\",\n" +
            "          \"type\": \"int256[]\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"cc_tuple_arr\",\n" +
            "          \"type\": \"tuple[]\",\n" +
            "          \"components\": [\n" +
            "            {\n" +
            "              \"type\": \"int8\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"uint40\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"outputs\": [],\n" +
            "  \"stateMutability\": \"view\",\n" +
            "  \"constant\": true\n" +
            "}";

    private static final String CONTRACT_JSON = "[\n" +
            "  {\n" +
            "    \"type\": \"event\",\n" +
            "    \"name\": \"an_event\",\n" +
            "    \"inputs\": [\n" +
            "      {\n" +
            "        \"name\": \"a\",\n" +
            "        \"type\": \"bytes\",\n" +
            "        \"indexed\": true\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"b\",\n" +
            "        \"type\": \"uint256\",\n" +
            "        \"indexed\": false\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"function\",\n" +
            "    \"name\": \"\",\n" +
            "    \"inputs\": [\n" +
            "      {\n" +
            "        \"name\": \"aa\",\n" +
            "        \"type\": \"tuple\",\n" +
            "        \"components\": [\n" +
            "          {\n" +
            "            \"name\": \"aa_d\",\n" +
            "            \"type\": \"decimal\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"aa_f\",\n" +
            "            \"type\": \"fixed128x18\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"bb\",\n" +
            "        \"type\": \"fixed128x18[]\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"cc\",\n" +
            "        \"type\": \"tuple\",\n" +
            "        \"components\": [\n" +
            "          {\n" +
            "            \"name\": \"cc_uint\",\n" +
            "            \"type\": \"uint256\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"cc_int_arr\",\n" +
            "            \"type\": \"int256[]\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"cc_tuple_arr\",\n" +
            "            \"type\": \"tuple[]\",\n" +
            "            \"components\": [\n" +
            "              {\n" +
            "                \"name\": \"cc_tuple_arr_int_eight\",\n" +
            "                \"type\": \"int8\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"name\": \"cc_tuple_arr_uint_forty\",\n" +
            "                \"type\": \"uint40\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ],\n" +
            "    \"outputs\": [],\n" +
            "    \"constant\": false\n" +
            "  }\n" +
            "]";

    private static final String FALLBACK_CONSTRUCTOR_RECEIVE = "[\n" +
            "  {\n" +
            "    \"type\": \"fallback\",\n" +
            "    \"stateMutability\": \"pure\",\n" +
            "    \"constant\": true\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"constructor\",\n" +
            "    \"inputs\": [\n" +
            "      {\n" +
            "        \"name\": \"aha\",\n" +
            "        \"type\": \"bool\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"stateMutability\": \"nonpayable\",\n" +
            "    \"constant\": false\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"receive\",\n" +
            "    \"name\": \"receive\",\n" +
            "    \"stateMutability\": \"payable\",\n" +
            "    \"constant\": false\n" +
            "  }\n" +
            "]";

    private static void toString(ABIType<?> type, StringBuilder sb) {
        switch (type.typeCode()) {
        case TYPE_CODE_ARRAY:
            sb.append('[');
            toString(((ArrayType<? extends ABIType<?>, ?>) type).getElementType(), sb);
            sb.append(']');
            break;
        case TYPE_CODE_TUPLE:
            sb.append('(');
            for(ABIType<?> e : ((TupleType) type).elementTypes()) {
                toString(e, sb);
            }
            sb.append(')');
            break;
        default:
            sb.append(type);
        }
        sb.append(' ').append(type.getName()).append(',');
    }

    private static void printTupleType(TupleType tupleType) {
        StringBuilder sb = new StringBuilder();
        toString(tupleType, sb);
        System.out.println("RECURSIVE = " + sb.toString());
    }

    @Test
    public void testToJson() {

        String[] jsons = new String[7];

        int i = 0;
        jsons[i++] = FUNCTION_A_JSON;
        jsons[i++] = FUNCTION_B_JSON;
        JsonArray contractArray = JsonUtils.parseArray(CONTRACT_JSON);
        final int n = contractArray.size();
        for (int j = 0; j < n; j++) {
            jsons[i++] = JsonUtils.toPrettyPrint(contractArray.get(j).getAsJsonObject());
        }
        JsonArray fallbackEtc = JsonUtils.parseArray(FALLBACK_CONSTRUCTOR_RECEIVE);
        final int n2 = fallbackEtc.size();
        for (int j = 0; j < n2; j++) {
            jsons[i++] = JsonUtils.toPrettyPrint(fallbackEtc.get(j).getAsJsonObject());
        }

        for (String originalJson : jsons) {
            ABIObject orig = ABIJSON.parseABIObject(JsonUtils.parseObject(originalJson));
            String newJson = orig.toJson(false);
            assertNotEquals(originalJson, newJson);

            ABIObject reconstructed = ABIJSON.parseABIObject(JsonUtils.parseObject(newJson));

            assertEquals(orig, reconstructed);
            assertEquals(originalJson, reconstructed.toString());

            if(orig instanceof Function) {
                assertEquals(orig, ABIJSON.parseFunction(newJson));
            } else {
                assertEquals(orig, ABIJSON.parseEvent(newJson));
            }
        }
    }

    @Test
    public void testBigIntAddrs() throws Throwable {
        testBigIntAddr(BigInteger.ZERO);
        testBigIntAddr(BigInteger.ONE);
        testBigIntAddr(BigInteger.TEN);
        testBigIntAddr(BigInteger.valueOf(2L));
        testBigIntAddr(BigIntegerType.decodeAddress("0x82095cafebabecafebabe00083ce15d74e191051"));
        testBigIntAddr(BigIntegerType.decodeAddress("0x4bec173f8d9d3d90188777cafebabecafebabe99"));
        testBigIntAddr(BigIntegerType.decodeAddress("0x5cafebabecafebabe7570ad8ac11f8d812ee0606"));
        testBigIntAddr(BigIntegerType.decodeAddress("0x0000000005cafebabecafebabe7570ad8ac11f8d"));
        testBigIntAddr(BigIntegerType.decodeAddress("0x0000000000000000000082095cafebabecafebab"));

        TestUtils.assertThrown(IllegalArgumentException.class,
                "invalid bit length: 161",
                () -> BigIntegerType.formatAddress(new BigInteger("182095cafebabecafebabe00083ce15d74e191051", 16))
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "invalid bit length: 164",
                () -> BigIntegerType.formatAddress(new BigInteger("82095cafebabecafebabe00083ce15d74e1910510", 16))
        );

        final SecureRandom sr = new SecureRandom();
        sr.setSeed(new SecureRandom().generateSeed(64));
        sr.setSeed(sr.generateSeed(64));
        final BigIntegerType type = TypeFactory.create("address");
        for (int i = 0; i < 500; i++) {
            testBigIntAddr(new BigInteger(type.bitLength, sr));
        }

        final Random r = new Random(sr.nextLong());
        for (int bitlen = 0; bitlen <= 160; bitlen++) {
            for (int i = 0; i < 10; i++) {
                testBigIntAddr(new BigInteger(bitlen, r));
            }
        }
        BigInteger temp;
        do {
            temp = new BigInteger(161, r);
        } while (temp.bitLength() < 161);
        final BigInteger tooBig = temp;
        TestUtils.assertThrown(IllegalArgumentException.class, "invalid bit length: 161", () -> BigIntegerType.formatAddress(tooBig));
    }

    @Test
    public void testStringAddrs() throws Throwable {
        testStringAddr(BigIntegerType.formatAddress(BigInteger.ZERO));
        testStringAddr(BigIntegerType.formatAddress(BigInteger.ONE));
        testStringAddr(BigIntegerType.formatAddress(BigInteger.TEN));
        testStringAddr(BigIntegerType.formatAddress(BigInteger.valueOf(2L)));
        testStringAddr("0x82095cafebabecafebabe00083ce15d74e191051");
        testStringAddr("0x4bec173f8d9d3d90188777cafebabecafebabe99");
        testStringAddr("0x5cafebabecafebabe7570ad8ac11f8d812ee0606");
        testStringAddr("0x0000000005cafebabecafebabe7570ad8ac11f8d");
        testStringAddr("0x0000000000000000000082095cafebabecafebab");
        testStringAddr("0xc0ec0fbb1c07aebe2a6975d50b5f6441b05023f9");
        testStringAddr("0xa62274005cafebabecafebabecaebb178db50ad6");
        testStringAddr("0xc6782c3a8155971a5d16005cafebabecafebabe8");

        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected prefix 0x not found",
                () -> BigIntegerType.decodeAddress("aaaaa")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected prefix 0x not found",
                () -> BigIntegerType.decodeAddress("5cafebabecafebabe7570ad8ac11f8d812ee0606")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected address length: 42; actual: 41",
                () -> BigIntegerType.decodeAddress("0xa83aaef1b5c928162005cafebabecafebabecb0")
        );
        TestUtils.assertThrown(IllegalArgumentException.class,
                "expected address length: 42; actual: 43",
                () -> BigIntegerType.decodeAddress("0xa83aaef1b5c928162005cafebabecafebabecb0a0")
        );

        final byte[] _20 = new byte[20];
        final Random r = TestUtils.seededRandom();
        for (int i = 0; i < 1_000; i++) {
            testStringAddr(generateStringAddress(_20, r));
        }

        BigInteger _ffff = BigIntegerType.decodeAddress("0x000000000000000000000000000000000000ffff");
        assertEquals(BigInteger.valueOf(65535L), _ffff);
    }

    private static void testStringAddr(final String addrString) {
        final BigInteger addr = BigIntegerType.decodeAddress(addrString);
        assertTrue(addr.bitLength() <= 160);
        assertEquals(addr, BigIntegerType.decodeAddress(addrString));
    }

    private static String generateStringAddress(byte[] _20, Random r) {
        r.nextBytes(_20);
        return "0x" + Strings.encode(_20);
    }

    private static void testBigIntAddr(final BigInteger addr) {
        final String addrString = BigIntegerType.formatAddress(addr);
        assertTrue(addrString.startsWith("0x"));
        assertEquals(BigIntegerType.ADDRESS_STRING_LEN, addrString.length());
        assertEquals(addr, BigIntegerType.decodeAddress(addrString));
    }

    @Test
    public void testParseFunctionA() throws Throwable {
        final JsonObject object = JsonUtils.parseObject(FUNCTION_A_JSON);
        final Function f = Function.fromJsonObject(object);
        final TupleType in = f.getInputs();
        final TupleType out = f.getOutputs();
        final ABIType<?> out0 = out.get(0);

        System.out.println(f.getName() + " : " + f.getCanonicalSignature() + " : " + out0);
        assertEquals(1, in.elementTypes.length);
        assertEquals(1, out.elementTypes.length);

        assertEquals("foo((decimal,decimal)[][])", f.getCanonicalSignature());
        assertEquals("uint64", out0.getCanonicalType());

        assertFalse(out0.isDynamic());
        assertNull(f.getStateMutability());
        f.encodeCallWithArgs((Object) new Tuple[][] { new Tuple[] { new Tuple(new BigDecimal(BigInteger.ONE, 10), new BigDecimal(BigInteger.TEN, 10)) } });

        printTupleType(in);
        printTupleType(out);

        Function f2 = ABIObject.fromJson(FUNCTION_A_JSON).asFunction();
        assertEquals(f, f2);
        assertEquals(f, ABIObject.fromJsonObject(object));

        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.ContractError", f2::asContractError);
        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Event", f2::asEvent);

        assertTrue(f.isFunction());
        assertFalse(f.isEvent());
        assertFalse(f.isContractError());

        assertTrue(in.isDynamic());
    }

    @Test
    public void testParseFunctionB() {
        final Function f = Function.fromJson(FUNCTION_B_JSON);
        System.out.println(f.getName() + " : " + f.getCanonicalSignature());
        assertEquals(TupleType.EMPTY, f.getOutputs());
        assertEquals("func((decimal,fixed128x18),fixed128x18[],(uint256,int256[],(int8,uint40)[]))", f.getCanonicalSignature());
        assertEquals("view", f.getStateMutability());

        printTupleType(f.getInputs());
    }

    @Test
    public void testParseFunction2() throws Throwable {
        final JsonObject function = new JsonObject();

        TestUtils.CustomRunnable parse = () -> Function.fromJsonObject(function);

        TestUtils.assertThrown(IllegalArgumentException.class, "type is \"function\"; functions of this type must define name", parse);

        function.add("type", new JsonPrimitive("event"));

        TestUtils.assertThrown(IllegalArgumentException.class, "unexpected type: \"event\"", parse);

        function.add("type", new JsonPrimitive("function"));

        TestUtils.assertThrown(IllegalArgumentException.class, "type is \"function\"; functions of this type must define name", parse);

        TestUtils.CustomRunnable[] updates = new TestUtils.CustomRunnable[] {
                () -> function.add("type", new JsonPrimitive("fallback")),
                () -> function.add("type", new JsonPrimitive("constructor")),
                () -> function.add("inputs", new JsonArray()),
                () -> {
                    function.remove("inputs");
                    function.add("name", new JsonPrimitive(""));
                    function.add("type", new JsonPrimitive("function"));
                }
        };

        for(TestUtils.CustomRunnable update : updates) {
            update.run();
            parse.run();
        }
    }

    @Test
    public void testParseEvent() throws Throwable {
        JsonObject jsonObject = new JsonObject();

        TestUtils.CustomRunnable runnable = () -> Event.fromJsonObject(jsonObject);

        TestUtils.assertThrown(IllegalArgumentException.class, "unexpected type: null", runnable);

        jsonObject.add("type", new JsonPrimitive("event"));

        TestUtils.assertThrown(IllegalArgumentException.class, "array \"inputs\" null or not found", runnable);

        jsonObject.add("inputs", new JsonArray());

        TestUtils.assertThrown(NullPointerException.class, runnable);

        jsonObject.add("name", new JsonPrimitive("a_name"));

        runnable.run();

        Event expectedA = Event.create("a_name", TupleType.parse("()"));
        Event expectedB = Event.create("a_name", TupleType.EMPTY);
        assertEquals(expectedA, expectedB);
        assertEquals(expectedA.hashCode(), expectedB.hashCode());

        String json = jsonObject.toString();
        assertEquals(expectedA, Event.fromJson(json));

        Event e = ABIObject.fromJson(json).asEvent();
        assertEquals(expectedA, e);
        assertEquals(e, ABIObject.fromJsonObject(jsonObject));

        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.ContractError", e::asContractError);
        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Function", e::asFunction);

        assertFalse(e.isFunction());
        assertTrue(e.isEvent());
        assertFalse(e.isContractError());
    }

    @Test
    public void testAnonymousEvent() {
        TupleType inputs1 = TupleType.parse("(bool[],int,(uint32,string)[])");
        TupleType inputs2 = TupleType.parse(inputs1.canonicalType);
        TupleType inputs3 = Function.parse("foo(bool[],int,(uint32,string)[])").getInputs();
        Event a = Event.createAnonymous("x17", inputs1, true, false, true);
        Event b = Event.createAnonymous("x17", inputs2, true, false, true);
        Event c = new Event("x17", true, inputs3, true, false, true);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.hashCode(), c.hashCode());
    }

    @Test
    public void testGetFunctions() {

        List<Function> functions;

        functions = ABIJSON.parseFunctions(CONTRACT_JSON);

        assertEquals(1, functions.size());

        {
            List<Function> f2 = ABIJSON.parseFunctions(CONTRACT_JSON.replace("    \"type\": \"function\",\n", ""));
            assertEquals(1, f2.size());
            assertEquals(functions.get(0), f2.get(0));
        }

        Function func = functions.get(0);

        printTupleType(func.getInputs());

        assertEquals(TypeEnum.FUNCTION, func.getType());
        assertEquals("", func.getName());
        assertNull(func.getStateMutability());

        functions = ABIJSON.parseNormalFunctions(FALLBACK_CONSTRUCTOR_RECEIVE);
        assertEquals(0, functions.size());

        functions = ABIJSON.parseFunctions(FALLBACK_CONSTRUCTOR_RECEIVE);
        assertEquals(3, functions.size());

        assertNull(functions.get(0).getName());
        assertNull(functions.get(1).getName());

        for(Function x : functions) {
            printTupleType(x.getInputs());
            assertEquals(TupleType.EMPTY, x.getOutputs());
        }

        Function fallback = functions.get(0);
        Function constructor = functions.get(1);

        assertEquals(TypeEnum.FALLBACK, fallback.getType());
        assertEquals(TupleType.EMPTY, fallback.getInputs());
        assertEquals(TupleType.EMPTY, fallback.getOutputs());
        assertEquals("pure", fallback.getStateMutability());

        assertEquals(TypeEnum.CONSTRUCTOR, constructor.getType());
        assertEquals(TupleType.parse("(bool)"), constructor.getInputs());
        assertEquals(TupleType.EMPTY, fallback.getOutputs());
        assertEquals("nonpayable", constructor.getStateMutability());
    }

    @Test
    public void testGetEvents() {
        List<Event> events = ABIJSON.parseEvents(CONTRACT_JSON);

        assertEquals(1, events.size());

        Event event = events.get(0);

        assertEquals("an_event", event.getName());
        assertEquals(TupleType.parse("(bytes,uint256)"), event.getInputs());
        assertEquals(TupleType.parse("(bytes)"), event.getIndexedParams());
        assertEquals(TupleType.parse("(uint256)"), event.getNonIndexedParams());
        assertArrayEquals(new boolean[] { true, false }, event.getIndexManifest());

        assertEquals("a", event.getInputs().get(0).getName());
        assertEquals("b", event.getInputs().get(1).getName());

        assertEquals("a", event.getIndexedParams().get(0).getName());
        assertEquals("b", event.getNonIndexedParams().get(0).getName());

        final String eventJson = "{\n" +
                "  \"type\": \"event\",\n" +
                "  \"name\": \"an_event\",\n" +
                "  \"inputs\": [\n" +
                "    {\n" +
                "      \"name\": \"a\",\n" +
                "      \"type\": \"bytes\",\n" +
                "      \"indexed\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"b\",\n" +
                "      \"type\": \"uint256\",\n" +
                "      \"indexed\": false\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertEquals(eventJson, event.toJson(true));
    }
    
    private static final String ERROR_JSON = "{\n" +
                "  \"type\": \"error\",\n" +
                "  \"name\": \"InsufficientBalance\",\n" +
                "  \"inputs\": [\n" +
                "    {\n" +
                "      \"name\": \"available\",\n" +
                "      \"type\": \"uint256\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"required\",\n" +
                "      \"type\": \"uint256\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    
    private static final String ERROR_JSON_ARRAY = "[" + ERROR_JSON + "]";

    @Test
    public void testGetErrors() throws Throwable {
        JsonObject object = JsonUtils.parseObject(ERROR_JSON);

        ContractError error0 = ABIJSON.parseErrors(ERROR_JSON_ARRAY).get(0);
        ContractError error1 = ABIJSON.parseError(object);

        testError(error0, ERROR_JSON, object);
        testError(error1, ERROR_JSON, object);

        assertEquals(error0, error1);

        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Function", error0::asFunction);
        TestUtils.assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Event", error0::asEvent);
    }

    private static void testError(ContractError error, String json, JsonObject object) {
        assertEquals(TypeEnum.ERROR, error.getType());
        assertEquals("InsufficientBalance", error.getName());
        assertEquals(TupleType.parse("(uint,uint)"), error.getInputs());
        assertEquals("InsufficientBalance(uint256,uint256)", error.getCanonicalSignature());
        assertEquals(Function.parse("InsufficientBalance(uint,uint)"), error.function());
        assertEquals(json, error.toJson(true));
        assertEquals(json, error.toString());

        testEqualNotSame(error, ContractError.fromJson(json));
        testEqualNotSame(error, ContractError.fromJsonObject(object));
        testEqualNotSame(error, ABIObject.fromJson(json).asContractError());
        testEqualNotSame(error, ABIObject.fromJsonObject(object).asContractError());

        assertFalse(error.isFunction());
        assertFalse(error.isEvent());
        assertTrue(error.isContractError());
    }

    private static void testEqualNotSame(ContractError a, ContractError b) {
        assertNotSame(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, b);
    }

    @Test
    public void testJsonUtils() {
        JsonObject empty = new JsonObject();
        Boolean b = JsonUtils.getBoolean(empty, "constant");
        assertNull(b);
        Boolean b2 = JsonUtils.getBoolean(empty, "constant", null);
        assertNull(b2);
    }

    @Test
    public void testParseElements() throws Throwable {
        
        List<ABIObject> list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.noneOf(TypeEnum.class));
        assertEquals(0, list.size());
        
        list = ABIJSON.parseElements(CONTRACT_JSON);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));
        assertTrue(list.stream().anyMatch(ABIObject::isFunction));

        list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.of(TypeEnum.FUNCTION, TypeEnum.RECEIVE, TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR, TypeEnum.EVENT));
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));
        assertTrue(list.stream().anyMatch(ABIObject::isFunction));
        
        list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.of(TypeEnum.EVENT, TypeEnum.ERROR));
        assertEquals(1, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));
        
        List<Function> fList = ABIJSON.parseElements(CONTRACT_JSON, ABIJSON.FUNCTIONS);
        assertEquals(1, fList.size());
        assertTrue(fList.stream().anyMatch(ABIObject::isFunction));
        
        List<ContractError> errList = ABIJSON.parseElements(ERROR_JSON_ARRAY, ABIJSON.ERRORS);
        assertEquals(1, errList.size());
        assertTrue(errList.stream().anyMatch(ABIObject::isContractError));

        TestUtils.assertThrown(UnsupportedOperationException.class, () -> ABIJSON.FUNCTIONS.add(TypeEnum.EVENT));
        TestUtils.assertThrown(UnsupportedOperationException.class, () -> ABIJSON.EVENTS.add(TypeEnum.CONSTRUCTOR));
        TestUtils.assertThrown(UnsupportedOperationException.class, () -> ABIJSON.ERRORS.add(TypeEnum.EVENT));
        TestUtils.assertThrown(UnsupportedOperationException.class, () -> ABIJSON.ALL.remove(TypeEnum.EVENT));
    }

    @Test
    public void testEnumSet() {
        {
            List<ABIObject> list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.noneOf(TypeEnum.class));
            assertEquals(0, list.size());

            list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.of(TypeEnum.FUNCTION, TypeEnum.EVENT));
            assertEquals(2, list.size());
            assertTrue(list.stream().anyMatch(ABIObject::isEvent));
            assertTrue(list.stream().anyMatch(ABIObject::isFunction));

            list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.of(TypeEnum.EVENT, TypeEnum.ERROR));
            assertEquals(1, list.size());
            assertTrue(list.stream().anyMatch(ABIObject::isEvent));

            List<Function> fList = ABIJSON.parseNormalFunctions(CONTRACT_JSON);
            assertEquals(1, fList.size());
            assertTrue(fList.stream().anyMatch(ABIObject::isFunction));

            List<ContractError> errList = ABIJSON.parseElements(ERROR_JSON_ARRAY, EnumSet.of(TypeEnum.ERROR));
            assertEquals(1, errList.size());
            assertTrue(errList.stream().anyMatch(ABIObject::isContractError));
        }

        testFallbackConstructorReceive(EnumSet.noneOf(TypeEnum.class), 0);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.FUNCTION), 0);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.EVENT), 0);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.ERROR), 0);

        testFallbackConstructorReceive(EnumSet.of(TypeEnum.RECEIVE), 1);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.FALLBACK), 1);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.CONSTRUCTOR), 1);

        testFallbackConstructorReceive(EnumSet.of(TypeEnum.RECEIVE, TypeEnum.FALLBACK), 2);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.RECEIVE, TypeEnum.CONSTRUCTOR), 2);
        testFallbackConstructorReceive(EnumSet.of(TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR), 2);

        testFallbackConstructorReceive(EnumSet.of(TypeEnum.RECEIVE, TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR), 3);
    }

    private static void testFallbackConstructorReceive(EnumSet<TypeEnum> expectedTypes, int expectedSize) {
        List<Function> fns = ABIJSON.parseElements(FALLBACK_CONSTRUCTOR_RECEIVE, expectedTypes);
        assertEquals(expectedSize, fns.size());
        for(Function f : fns) {
            assertTrue(expectedTypes.contains(f.getType()));
        }
    }

    @Test
    public void testStreamObjects() {
        List<ABIObject> objects = ABIJSON.parseElements(CONTRACT_JSON);

        List<Function> functions = objects.stream()
                .filter(ABIObject::isFunction)
                .map(ABIObject::asFunction)
                .collect(Collectors.toList());
        assertEquals(1, functions.size());

        List<Event> events = objects.stream()
                .filter(ABIObject::isEvent)
                .map(ABIObject::asEvent)
                .collect(Collectors.toList());
        assertEquals(1, events.size());

        List<ContractError> errors = objects.stream()
                .filter(ABIObject::isContractError)
                .map(ABIObject::asContractError)
                .collect(Collectors.toList());
        assertEquals(0, errors.size());

        List<ABIType<?>> flat = objects.stream()
                .map(ABIObject::getInputs)
                .flatMap(ABIJSONTest::flatten)
                .collect(Collectors.toList());

        assertEquals(8, flat.size());
    }

    public static Stream<ABIType<?>> flatten(ABIType<?> type) {
        return type instanceof TupleType
                ? StreamSupport.stream(((TupleType) type).spliterator(), false)
                    .flatMap(ABIJSONTest::flatten)
                : Stream.of(type);
    }
}
