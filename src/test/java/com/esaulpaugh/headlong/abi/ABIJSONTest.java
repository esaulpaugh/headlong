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
import com.esaulpaugh.headlong.util.FastHex;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.abi.ABIType.FLAGS_NONE;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_ARRAY;
import static com.esaulpaugh.headlong.abi.ABIType.TYPE_CODE_TUPLE;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
            "          \"type\": \"fixed168x10\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"name\": \"imaginary\",\n" +
            "          \"type\": \"fixed168x10\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"outputs\": [\n" +
            "    {\n" +
            "      \"name\": \"count\",\n" +
            "      \"type\": \"uint64\"\n" +
            "    }\n" +
            "  ]\n" +
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
            "          \"type\": \"fixed168x10\"\n" +
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
            "  \"stateMutability\": \"view\"\n" +
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
            "    ],\n" +
            "    \"anonymous\": true\n" +
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
            "            \"type\": \"fixed168x10\"\n" +
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
            "    \"outputs\": []\n" +
            "  }\n" +
            "]";

    private static final String FALLBACK_CONSTRUCTOR_RECEIVE = "[\n" +
            "  {\n" +
            "    \"type\": \"fallback\",\n" +
            "    \"stateMutability\": \"pure\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"constructor\",\n" +
            "    \"inputs\": [\n" +
            "      {\n" +
            "        \"name\": \"aha\",\n" +
            "        \"type\": \"bool\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"stateMutability\": \"nonpayable\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"receive\",\n" +
            "    \"stateMutability\": \"payable\"\n" +
            "  }\n" +
            "]";

    private static void toString(String name, ABIType<?> type, StringBuilder sb) {
        switch (type.typeCode()) {
        case TYPE_CODE_ARRAY:
            sb.append('[');
            toString(null, type.asArrayType().getElementType(), sb);
            sb.append(']');
            break;
        case TYPE_CODE_TUPLE:
            sb.append('(');
            int i = 0;
            TupleType<?> tt = (TupleType<?>) type;
            for(ABIType<?> e : tt) {
                toString(tt.getElementName(i++), e, sb);
            }
            sb.append(')');
            break;
        default:
            sb.append(type);
        }
        sb.append(' ').append(name).append(',');
    }

    private static void printTupleType(TupleType<?> tupleType) {
        StringBuilder sb = new StringBuilder("RECURSIVE = ");
        toString(null, tupleType, sb);
        System.out.println(sb);
    }

    @Test
    public void testToJson() {

        String[] jsons = new String[7];

        int i = 0;
        jsons[i++] = FUNCTION_A_JSON;
        jsons[i++] = FUNCTION_B_JSON;
        JsonArray contractArray = TestUtils.parseArray(CONTRACT_JSON);
        final int n = contractArray.size();
        for (int j = 0; j < n; j++) {
            jsons[i++] = TestUtils.toPrettyPrint(contractArray.get(j).getAsJsonObject());
        }
        JsonArray fallbackEtc = TestUtils.parseArray(FALLBACK_CONSTRUCTOR_RECEIVE);
        final int n2 = fallbackEtc.size();
        for (int j = 0; j < n2; j++) {
            jsons[i++] = TestUtils.toPrettyPrint(fallbackEtc.get(j).getAsJsonObject());
        }

        for (String originalJson : jsons) {
            ABIObject orig = ABIObject.fromJson(FLAGS_NONE, TestUtils.parseObject(originalJson).toString());
            String newJson = orig.toJson(false);
            assertNotEquals(originalJson, newJson);

            ABIObject reconstructed = ABIObject.fromJson(FLAGS_NONE, TestUtils.parseObject(newJson).toString());

            assertEquals(orig, reconstructed);
            assertEquals(originalJson, reconstructed.toString());
            assertEquals(orig, ABIObject.fromJson(newJson));
        }
    }

    @Test
    public void testParseFunctionA() throws Throwable {

        assertEquals(
                "foo((fixed168x10,fixed168x10)[11][6])",
                Function.fromJson(FUNCTION_A_JSON.replace("tuple[][]", "tuple[11][6]")).getCanonicalSignature()
        );

        final JsonObject object = TestUtils.parseObject(FUNCTION_A_JSON);
        final Function f = Function.fromJson(FLAGS_NONE, object.toString());
        assertEquals(FUNCTION_A_JSON, f.toJson(true));
        final TupleType<?> in = f.getInputs();
        final TupleType<?> out = f.getOutputs();
        final ABIType<?> out0 = out.get(0);

        final BigInteger val = BigInteger.valueOf(40L);
        final Object obj = val;
        final Object a = out.<ABIType<Object>>get(0).encode(val);
        final Object b = out.<ABIType<? super Object>>get(0).encode(obj);
        assertEquals(a, b);
        final ABIType<? super Object> type = out.get(0);
        assertEquals(a, type.encode(obj));
        assertEquals(a, type.encode(val));

        System.out.println(f.getName() + " : " + f.getCanonicalSignature() + " : " + out0);
        assertEquals(1, in.elementTypes.length);
        assertEquals(1, out.elementTypes.length);

        assertEquals("foo((fixed168x10,fixed168x10)[][])", f.getCanonicalSignature());
        assertEquals("uint64", out0.getCanonicalType());

        assertFalse(out0.isDynamic());
        assertNull(f.getStateMutability());
        f.encodeCallWithArgs((Object) new Tuple[][] { new Tuple[] { Tuple.of(new BigDecimal(BigInteger.ONE, 10), new BigDecimal(BigInteger.TEN, 10)) } });

        printTupleType(in);
        printTupleType(out);

        Function f2 = ABIObject.fromJson(FUNCTION_A_JSON);
        assertEquals(f, f2);
        assertEquals(f, ABIObject.fromJson(FLAGS_NONE, object.toString()));

        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.ContractError", f2::asContractError);
        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Event", f2::asEvent);

        assertTrue(f.isFunction());
        assertFalse(f.isEvent());
        assertFalse(f.isContractError());

        assertTrue(in.isDynamic());
    }

    @Test
    public void testParseFunctionB() {
        final Function f = Function.fromJson(FUNCTION_B_JSON);
        assertEquals(FUNCTION_B_JSON, f.toJson(true));
        System.out.println(f.getName() + " : " + f.getCanonicalSignature());
        assertEquals(TupleType.EMPTY, f.getOutputs());
        assertEquals("func((fixed168x10,fixed128x18),fixed128x18[],(uint256,int256[],(int8,uint40)[]))", f.getCanonicalSignature());
        assertEquals("view", f.getStateMutability());

        printTupleType(f.getInputs());
    }

    @Test
    public void testParseFunction2() throws Throwable {
        final JsonObject function = new JsonObject();

        TestUtils.CustomRunnable parse = () -> Function.fromJson(FLAGS_NONE, function.toString());

        assertThrown(NullPointerException.class, parse);

        function.add("type", new JsonPrimitive("event"));

        assertThrown(IllegalArgumentException.class, "unexpected type", parse);

        function.add("type", new JsonPrimitive("function"));

        assertThrown(IllegalArgumentException.class, "type is \"function\"; functions of this type must define name", parse);

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

        TestUtils.CustomRunnable runnable = () -> Event.fromJson(FLAGS_NONE, jsonObject.toString());

        assertThrown(NullPointerException.class, runnable);

        jsonObject.add("type", new JsonPrimitive("event"));

        assertThrown(NullPointerException.class, runnable);

        jsonObject.add("inputs", new JsonArray());

        assertThrown(NullPointerException.class, runnable);

        jsonObject.add("name", new JsonPrimitive("a_name"));

        runnable.run();

        Event<?> expectedA = Event.create("a_name", TupleType.parse("()"));
        Event<?> expectedB = Event.create("a_name", TupleType.EMPTY);
        assertEquals(expectedA, expectedB);
        assertEquals(expectedA.hashCode(), expectedB.hashCode());

        String json = jsonObject.toString();
        assertEquals(expectedA, Event.fromJson(json));

        Event<?> e = ABIObject.fromJson(json);
        assertEquals(expectedA, e);
        assertEquals(e, ABIObject.fromJson(FLAGS_NONE, jsonObject.toString()));

        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.ContractError", e::asContractError);
        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Function", e::asFunction);

        assertFalse(e.isFunction());
        assertTrue(e.isEvent());
        assertFalse(e.isContractError());
    }

    @Test
    public void testAnonymousEvent() {
        TupleType<?> inputs1 = TupleType.parse("(bool[],int,(uint32,string)[])");
        TupleType<?> inputs2 = TupleType.parse(inputs1.canonicalType);
        TupleType<?> inputs3 = Function.parse("foo(bool[],int,(uint32,string)[])").getInputs();
        Event<?> a = Event.createAnonymous("x17", inputs1, true, false, true);
        Event<?> b = Event.createAnonymous("x17", inputs2, true, false, true);
        Event<?> c = new Event<>("x17", true, inputs3, true, false, true);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.hashCode(), c.hashCode());
        assertEquals(a.toString(), b.toString());

        assertEquals(a.toJson(true), b.toJson(true));
        assertEquals(a.toJson(false), b.toJson(false));
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
        List<Event<Tuple>> events = ABIJSON.parseEvents(CONTRACT_JSON);

        assertEquals(1, events.size());

        Event<?> event = events.get(0);

        assertEquals("an_event", event.getName());
        assertEquals(TupleType.parse("(bytes,uint256)"), event.getInputs());
        assertEquals(TupleType.parse("(bytes)"), event.getIndexedParams());
        assertEquals(TupleType.parse("(uint256)"), event.getNonIndexedParams());
        assertArrayEquals(new boolean[] { true, false }, event.getIndexManifest());
        assertTrue(event.isElementIndexed(0));
        assertFalse(event.isElementIndexed(1));

        assertEquals("a", event.getInputs().getElementName(0));
        assertEquals("b", event.getInputs().getElementName(1));

        assertEquals("a", event.getIndexedParams().getElementName(0));
        assertEquals("b", event.getNonIndexedParams().getElementName(0));

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
                "  ],\n" +
                "  \"anonymous\": true\n" +
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
                "      \"type\": \"uint24\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

    private static final String ERROR_JSON_ARRAY = "[" + ERROR_JSON + "]";

    @Test
    public void testGetErrors() throws Throwable {
        JsonObject object = TestUtils.parseObject(ERROR_JSON);

        ContractError<Tuple> error0 = ABIJSON.parseErrors(ERROR_JSON_ARRAY).get(0);
        ContractError<?> error1 = ABIObject.fromJson(ERROR_JSON);
        ContractError<Pair<BigInteger, Integer>> error2 = ABIObject.fromJson(FLAGS_NONE, object.toString());

        {
            TupleType<Pair<BigInteger, Integer>> in = error2.getInputs();
            Pair<BigInteger, Integer> pair = in.decode(new byte[UNIT_LENGTH_BYTES * 2]);
            BigInteger a = pair.get0();
            Integer b = pair.get1();
            assertEquals(BigInteger.ZERO, a);
            assertEquals(0, b);
        }

        testError(error0, ERROR_JSON, object);
        testError(error1, ERROR_JSON, object);
        testError(error2, ERROR_JSON, object);

        assertEquals(error0, error1);
        assertEquals(error0, error2);

        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Function", error0::asFunction);
        assertThrown(ClassCastException.class, "com.esaulpaugh.headlong.abi.Event", error0::asEvent);
    }

    private static void testError(ContractError<?> error, String json, JsonObject object) {
        assertEquals(TypeEnum.ERROR, error.getType());
        assertEquals("InsufficientBalance", error.getName());
        assertEquals(TupleType.parse("(uint,uint24)"), error.getInputs());
        assertEquals("InsufficientBalance(uint256,uint24)", error.getCanonicalSignature());
        assertEquals(Function.parse("InsufficientBalance(uint,uint24)"), error.function());
        assertEquals(json, error.toJson(true));
        assertEquals(json, error.toString());

        testEqualNotSame(error, ContractError.fromJson(json));
        testEqualNotSame(error, ContractError.fromJson(FLAGS_NONE, object.toString()));
        testEqualNotSame(error, ABIObject.fromJson(json));
        testEqualNotSame(error, ABIObject.fromJson(FLAGS_NONE, object.toString()));

        assertFalse(error.isFunction());
        assertFalse(error.isEvent());
        assertTrue(error.isContractError());
    }

    private static void testEqualNotSame(ContractError<?> a, ContractError<?> b) {
        assertNotSame(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    private static final Set<TypeEnum> FUNCTIONS_AND_EVENTS = EnumSet.of(TypeEnum.FUNCTION, TypeEnum.RECEIVE, TypeEnum.FALLBACK, TypeEnum.CONSTRUCTOR, TypeEnum.EVENT);
    private static final Set<TypeEnum> EVENTS_AND_ERRORS = EnumSet.of(TypeEnum.EVENT, TypeEnum.ERROR);

    @Test
    public void testParseElements() throws Throwable {

        List<ABIObject> list = ABIJSON.parseElements(ABIType.FLAG_LEGACY_DECODE, CONTRACT_JSON, EnumSet.noneOf(TypeEnum.class));
        assertEquals(0, list.size());

        list = ABIJSON.parseElements(CONTRACT_JSON);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));
        assertTrue(list.stream().anyMatch(ABIObject::isFunction));

        list = ABIJSON.parseElements(CONTRACT_JSON, FUNCTIONS_AND_EVENTS);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));
        assertTrue(list.stream().anyMatch(ABIObject::isFunction));
        assertFalse(list.stream().anyMatch(ABIObject::isContractError));

        list = ABIJSON.parseElements(CONTRACT_JSON, EVENTS_AND_ERRORS);
        assertEquals(1, list.size());
        assertTrue(list.stream().anyMatch(ABIObject::isEvent));

        List<Function> fList = ABIJSON.parseElements(CONTRACT_JSON, ABIJSON.FUNCTIONS);
        assertEquals(1, fList.size());
        assertTrue(fList.stream().anyMatch(ABIObject::isFunction));

        List<ContractError<?>> errList = ABIJSON.parseElements(ERROR_JSON_ARRAY, ABIJSON.ERRORS);
        assertEquals(1, errList.size());
        assertTrue(errList.stream().anyMatch(ABIObject::isContractError));

        assertThrown(UnsupportedOperationException.class, () -> ABIJSON.FUNCTIONS.add(TypeEnum.EVENT));
        assertThrown(UnsupportedOperationException.class, () -> ABIJSON.EVENTS.add(TypeEnum.CONSTRUCTOR));
        assertThrown(UnsupportedOperationException.class, () -> ABIJSON.ERRORS.add(TypeEnum.EVENT));
        assertThrown(UnsupportedOperationException.class, () -> ABIJSON.ALL.remove(TypeEnum.EVENT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIteratorAndStream() throws Throwable {
        assertFalse(ABIJSON.iterator(ABIType.FLAG_LEGACY_DECODE, CONTRACT_JSON, EnumSet.noneOf(TypeEnum.class)).hasNext());
        assertFalse(ABIJSON.iterator(ABIType.FLAG_LEGACY_DECODE, "[]", ABIJSON.ALL).hasNext());
        assertEquals(0, (int) ABIJSON.stream("[]").count());

        Iterator<ABIObject> iter;
        for (Set<?> set : new Set[] { ABIJSON.ALL, FUNCTIONS_AND_EVENTS, EnumSet.of(TypeEnum.EVENT, TypeEnum.FUNCTION) }) {
            iter = ABIJSON.iterator(FLAGS_NONE, CONTRACT_JSON, (Set<TypeEnum>) set);
            assertTrue(iter.next().isEvent());
            assertTrue(iter.next().isFunction());
            assertFalse(iter.hasNext());
            assertThrown(NoSuchElementException.class, iter::next);
            assertEquals(2, ABIJSON.stream(CONTRACT_JSON).count());
            assertTrue(ABIJSON.stream(CONTRACT_JSON).anyMatch(ABIObject::isEvent));
            assertTrue(ABIJSON.stream(CONTRACT_JSON).anyMatch(ABIObject::isFunction));
            assertFalse(ABIJSON.stream(CONTRACT_JSON).anyMatch(ABIObject::isContractError));
        }

        iter = ABIJSON.iterator(FLAGS_NONE, CONTRACT_JSON, EVENTS_AND_ERRORS);
        assertTrue(iter.next().isEvent());
        assertFalse(iter.hasNext());
        assertTrue(ABIJSON.stream(CONTRACT_JSON).anyMatch(ABIObject::isEvent));

        Iterator<Function> fIter = ABIJSON.iterator(FLAGS_NONE, CONTRACT_JSON, ABIJSON.FUNCTIONS);
        assertInstanceOf(Function.class, fIter.next());
        assertFalse(fIter.hasNext());
        assertTrue(ABIJSON.stream(CONTRACT_JSON).anyMatch(ABIObject::isFunction));

        Iterator<ContractError<Tuple>> errIter = ABIJSON.iterator(FLAGS_NONE, ERROR_JSON_ARRAY, ABIJSON.ERRORS);
        assertInstanceOf(ContractError.class, errIter.next());
        assertFalse(errIter.hasNext());
        assertEquals(1, ABIJSON.stream(ERROR_JSON_ARRAY).count());
        assertInstanceOf(ContractError.class, ABIJSON.stream(ERROR_JSON_ARRAY).filter(ABIObject::isContractError).findFirst().get());

        assertFalse(ABIJSON.iterator(FLAGS_NONE, ERROR_JSON_ARRAY, ABIJSON.FUNCTIONS).hasNext());
    }

    @Test
    public void testEnumSet() {
        {
            List<ABIObject> list = ABIJSON.parseElements(CONTRACT_JSON, EnumSet.noneOf(TypeEnum.class));
            assertEquals(0, list.size());

            list = ABIJSON.parseElements(CONTRACT_JSON, FUNCTIONS_AND_EVENTS);
            assertEquals(2, list.size());
            assertTrue(list.stream().anyMatch(ABIObject::isEvent));
            assertTrue(list.stream().anyMatch(ABIObject::isFunction));

            list = ABIJSON.parseElements(CONTRACT_JSON, EVENTS_AND_ERRORS);
            assertEquals(1, list.size());
            assertTrue(list.stream().anyMatch(ABIObject::isEvent));

            List<Function> fList = ABIJSON.parseNormalFunctions(CONTRACT_JSON);
            assertEquals(1, fList.size());
            assertTrue(fList.stream().anyMatch(ABIObject::isFunction));

            List<ContractError<?>> errList = ABIJSON.parseElements(ERROR_JSON_ARRAY, EnumSet.of(TypeEnum.ERROR));
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

        List<Event<?>> events = objects.stream()
                .filter(ABIObject::isEvent)
                .map(ABIObject::asEvent)
                .collect(Collectors.toList());
        assertEquals(1, events.size());

        List<ContractError<?>> errors = objects.stream()
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
        return type instanceof TupleType<?>
                ? StreamSupport.stream(((TupleType<?>) type).spliterator(), false)
                    .flatMap(ABIJSONTest::flatten)
                : Stream.of(type);
    }

    @Test
    public void testInternalType() throws Throwable {
        String eventStr = EVENT_STR;

        Event<Single<Single<String>[]>> e = Event.fromJson(eventStr);

        TupleType<Single<Single<String>[]>> in = e.getInputs();
        Single<Single<String>[]> s = in.decode(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000001" +
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000006" +
                        "000a09080c0d0000000000000000000000000000000000000000000000000000"
                )
        );
        final Single<String>[] arr = s.get0();
        assertEquals(Single[].class, arr.getClass());
        final Single<String> single = arr[0];
        final String str = single.get0();
        assertEquals(String.class, str.getClass());
        assertEquals("\0\n\t\b\f\r", str);
        assertEquals("struct Thing[]", in.getElementInternalType(0));

        TupleType<?> indexed = e.getIndexedParams();
        assertEquals(0, indexed.size());

        TupleType<?> nonIndexed = e.getNonIndexedParams();
        assertEquals("struct Thing[]", nonIndexed.getElementInternalType(0));

        assertThrown(ArrayIndexOutOfBoundsException.class, () -> nonIndexed.getElementInternalType(-1));
        assertThrown(ArrayIndexOutOfBoundsException.class, () -> nonIndexed.getElementInternalType(1));

        assertEquals(eventStr, e.toString());
    }

    private static final String EVENT_STR = "{\n" +
            "  \"type\": \"event\",\n" +
            "  \"name\": \"ManyThings\",\n" +
            "  \"inputs\": [\n" +
            "    {\n" +
            "      \"internalType\": \"struct Thing[]\",\n" +
            "      \"name\": \"thing\",\n" +
            "      \"type\": \"tuple[]\",\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"thing_string\",\n" +
            "          \"type\": \"string\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"indexed\": false\n" +
            "    }\n" +
            "  ],\n" +
            "  \"anonymous\": false\n" +
            "}";

    @Test
    public void testStaticTupleArray() throws Throwable {
        String eventStr = EVENT_STR.replace("tuple[]", "tuple[1]");

        Event<Single<Single<String>[]>> e = Event.fromJson(eventStr);

        TupleType<Single<Single<String>[]>> in = e.getInputs();
        Single<Single<String>[]> s = in.decode(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000006" +
                        "000a09080c0d0000000000000000000000000000000000000000000000000000"
                )
        );
        final Single<String>[] arr = s.get0();
        assertEquals(Single[].class, arr.getClass());
        final Single<String> single = arr[0];
        final String str = single.get0();
        assertEquals(String.class, str.getClass());
        assertEquals("\0\n\t\b\f\r", str);
        assertEquals("struct Thing[]", in.getElementInternalType(0));

        TupleType<?> indexed = e.getIndexedParams();
        assertEquals(0, indexed.size());

        TupleType<?> nonIndexed = e.getNonIndexedParams();
        assertEquals("struct Thing[]", nonIndexed.getElementInternalType(0));

        assertThrown(ArrayIndexOutOfBoundsException.class, () -> nonIndexed.getElementInternalType(-1));
        assertThrown(ArrayIndexOutOfBoundsException.class, () -> nonIndexed.getElementInternalType(1));

        assertEquals(eventStr, e.toString());
    }

    @Test
    public void testUserDefinedValueTypes() {
        String json = "{\n" +
                "  \"type\": \"constructor\",\n" +
                "  \"inputs\": [\n" +
                "    {\n" +
                "      \"internalType\": \"MyNamespace.UIntMax\",\n" +
                "      \"name\": \"unsigned256\",\n" +
                "      \"type\": \"uint256\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"internalType\": \"CustomType\",\n" +
                "      \"name\": \"signed24\",\n" +
                "      \"type\": \"int24\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"internalType\": \"SomeOtherDudesNamespace.Bytes\",\n" +
                "      \"name\": \"byteArr\",\n" +
                "      \"type\": \"bytes\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"internalType\": \"contract AContract\",\n" +
                "      \"name\": \"contractAddr\",\n" +
                "      \"type\": \"address\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"stateMutability\": \"pure\"\n" +
                "}";

        Function f = Function.fromJson(json);

        TupleType<?> in = f.getInputs();
        assertEquals("MyNamespace.UIntMax", in.getElementInternalType(0));
        assertEquals("CustomType", in.getElementInternalType(1));
        assertEquals("SomeOtherDudesNamespace.Bytes", in.getElementInternalType(2));
        assertEquals("contract AContract", in.getElementInternalType(3));

        assertEquals(json, f.toString());
    }

    @Test
    public void testParseABIField() {
        final String json = "{\n" +
                "  \"abi\": [\n" +
                "    {\n" +
                "      \"name\": \"aller\",\n" +
                "      \"inputs\": [\n" +
                "        { \"name\": \"amt\", \"type\": \"uint256\" },\n" +
                "        { \"name\": \"balance\", \"type\": \"uint256\" },\n" +
                "        { \"name\": \"rate\", \"type\": \"uint256\" },\n" +
                "        { \"name\": \"dest\", \"type\": \"address\" },\n" +
                "        { \"name\": \"msg\", \"type\": \"string\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        final List<ABIObject> objects = ABIJSON.parseABIField(FLAGS_NONE, json, ABIJSON.ALL);
        assertEquals(1, objects.size());
        assertEquals(TypeEnum.FUNCTION, objects.get(0).getType());
        assertEquals("aller(uint256,uint256,uint256,address,string)", objects.get(0).getCanonicalSignature());
    }
}
