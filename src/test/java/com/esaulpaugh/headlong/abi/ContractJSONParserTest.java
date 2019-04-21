package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.List;

public class ContractJSONParserTest {

    private static final String FUNCTION_A_JSON = "{\"name\": \"foo\", \"type\": \"function\", \"inputs\": [ {\"name\": \"complex_nums\", \"type\": \"tuple[]\", \"components\": [ {\"type\": \"decimal\", \"name\": \"real\"}, {\"type\": \"decimal\", \"name\": \"imaginary\"} ]} ], \"outputs\": [ {\"name\": \"count\", \"type\": \"uint64\" } ] }";

    private static final String FUNCTION_B_JSON = "{\n" +
            "    \"name\": \"func\",\n" +
            "    \"type\": \"function\",\n" +
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
            "        \"type\": \"fixed128x18[]\",\n" +
            "        \"components\": [\n" +
            "        ]\n" +
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
            "                \"type\": \"int8\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"type\": \"uint40\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
//            ",    \"outputs\": []\n" +
            "  }\n";

    private static final String CONTRACT_JSON = "[\n" +
            "  {\n" +
            "    \"type\":\"event\",\n" +
            "    \"inputs\": [\n" +
            "     {\"name\":\"a\",\"type\":\"bytes\",\"indexed\":true},\n" +
            "     {\"name\":\"b\",\"type\":\"uint256\",\"indexed\":false}\n" +
            "    ],\n" +
            "    \"name\":\"an_event\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"name\": \"func\",\n" +
            "    \"type\": \"function\",\n" +
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
            "        \"type\": \"fixed128x18[]\",\n" +
            "        \"components\": [\n" +
            "        ]\n" +
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

    private static final String FALLBACK_AND_CONSTRUCTOR =
            "[\n" +
                    "  {\n" +
                    "    \"type\": \"fallback\",\n" +
                    "    \"stateMutability\": \"pure\"" +
                    "  },\n" +
                    "  {\n" +
                    "    \"type\": \"constructor\",\n" +
                    "    \"inputs\": [\n" +
                    "      {\n" +
                    "        \"name\": \"aha\",\n" +
                    "        \"type\": \"bool\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]";

    private static void printTupleType(TupleType tupleType) {
        StringBuilder sb = new StringBuilder();
        tupleType.toString(sb);
        System.out.println("RECURSIVE = " + sb.toString());
    }

    @Test
    public void testParseFunction() throws ParseException {

        Function f;

        f = ContractJSONParser.parseFunction(FUNCTION_A_JSON);
        System.out.println(f.getName() + " : " + f.getCanonicalSignature() + " : " + f.getOutputTypes().get(0));
        Assert.assertEquals(1, f.getParamTypes().elementTypes.length);
        Assert.assertEquals("foo((decimal,decimal)[])", f.getCanonicalSignature());
        Assert.assertEquals(1, f.getOutputTypes().elementTypes.length);
        Assert.assertEquals("uint64", f.getOutputTypes().get(0).canonicalType);
        f.encodeCallWithArgs((Object) new Tuple[] { new Tuple(new BigDecimal(BigInteger.ONE, 10), new BigDecimal(BigInteger.TEN, 10)) });

        printTupleType(f.getParamTypes());

        printTupleType(f.getOutputTypes());

        f = ContractJSONParser.parseFunction(FUNCTION_B_JSON);
        System.out.println(f.getName() + " : " + f.getCanonicalSignature());
        Assert.assertEquals(TupleType.EMPTY, f.getOutputTypes());
        Assert.assertEquals("func((decimal,fixed128x18),fixed128x18[],(uint256,int256[],(int8,uint40)[]))", f.getCanonicalSignature());

        printTupleType(f.getParamTypes());
    }

    @Test
    public void testGetFunctions() throws ParseException {

        List<Function> functions;

        functions = ContractJSONParser.parseFunctions(CONTRACT_JSON);

        Assert.assertEquals(1, functions.size());

        Function func = functions.get(0);

        printTupleType(func.getParamTypes());

        Assert.assertEquals(Function.Type.FUNCTION, func.getType());
        Assert.assertEquals("func", func.getName());
        Assert.assertNull(func.getStateMutability());

        functions = ContractJSONParser.parseFunctions(FALLBACK_AND_CONSTRUCTOR);

        Assert.assertEquals(2, functions.size());

        for(Function x : functions) {
            printTupleType(x.getParamTypes());
            Assert.assertEquals("", x.getName());
            Assert.assertEquals(TupleType.EMPTY, x.getOutputTypes());
        }

        Function fallback = functions.get(0);
        Function constructor = functions.get(1);

        Assert.assertEquals(Function.Type.FALLBACK, fallback.getType());
        Assert.assertEquals(TupleType.EMPTY, fallback.getParamTypes());
        Assert.assertEquals(TupleType.EMPTY, fallback.getOutputTypes());
        Assert.assertEquals("pure", fallback.getStateMutability());

        Assert.assertEquals(Function.Type.CONSTRUCTOR, constructor.getType());
        Assert.assertEquals(TupleType.parse("(bool)"), constructor.getParamTypes());
        Assert.assertEquals(TupleType.EMPTY, fallback.getOutputTypes());
        Assert.assertNull(constructor.getStateMutability());
    }

    @Test
    public void testGetEvents() throws ParseException {
        List<Event> events = ContractJSONParser.parseEvents(CONTRACT_JSON);

        Assert.assertEquals(1, events.size());

        Event event = events.get(0);

        Assert.assertEquals("an_event", event.getName());
        Assert.assertEquals(TupleType.parse("(bytes,uint256)"), event.getParams());
        Assert.assertEquals(TupleType.parse("(bytes)"), event.getIndexedParams());
        Assert.assertEquals(TupleType.parse("(uint256)"), event.getNonIndexedParams());
        Assert.assertArrayEquals(new boolean[] { true, false }, event.getIndexManifest());

        Assert.assertEquals("a", event.getParams().get(0).getName());
        Assert.assertEquals("b", event.getParams().get(1).getName());
    }

    @Test
    public void parseFunction() throws Throwable {
        final JsonObject function = new JsonObject();

        TestUtils.CustomRunnable parse = () -> Function.fromJsonObject(function);

        TestUtils.assertThrown(IllegalArgumentException.class, "unexpected type: null", parse);

        function.add("type", new JsonPrimitive("event"));

        TestUtils.assertThrown(IllegalArgumentException.class, "unexpected type: \"event\"", parse);

        TestUtils.CustomRunnable[] updates = new TestUtils.CustomRunnable[] {
                () -> function.add("type", new JsonPrimitive("function")),
                () -> function.add("type", new JsonPrimitive("fallback")),
                () -> function.add("type", new JsonPrimitive("constructor")),
                () -> function.add("inputs", new JsonArray())
        };

        for(TestUtils.CustomRunnable update : updates) {
            update.run();
            parse.run();
        }
    }

    @Test
    public void parseEvent() throws Throwable {
        JsonObject jsonObject = new JsonObject();

        TestUtils.CustomRunnable runnable = () -> Event.fromJsonObject(jsonObject);

        TestUtils.assertThrown(IllegalArgumentException.class, "unexpected type: null", runnable);

        jsonObject.add("type", new JsonPrimitive("event"));

        TestUtils.assertThrown(IllegalArgumentException.class, "array \"inputs\" null or not found", runnable);

        jsonObject.add("inputs", new JsonArray());

        TestUtils.assertThrown(NullPointerException.class, runnable);

        jsonObject.add("name", new JsonPrimitive("a_name"));

        runnable.run();

        Event expectedA = new Event("a_name", "()", new boolean[0]);
        Event expectedB = new Event("a_name", TupleType.EMPTY, new boolean[0], false);

        Assert.assertEquals(expectedA, Event.fromJsonObject(jsonObject));
        Assert.assertEquals(expectedB, expectedA);
    }
}
