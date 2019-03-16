package com.esaulpaugh.headlong.abi;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.List;

public class ContractJSONParserTest {

    private static final String FUNCTION_A_JSON = "{\"name\": \"foo\", \"type\": \"function\", \"inputs\": [ {\"name\": \"complex_nums\", \"type\": \"tuple[]\", \"components\": [ {\"type\": \"decimal\"}, {\"type\": \"decimal\"} ]} ], \"outputs\": [ {\"name\": \"count\", \"type\": \"uint64\" } ] }";

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

    private static void printTupleType(TupleType tupleType) {
        StringBuilder sb = new StringBuilder();
        tupleType.recursiveToString(sb);
        System.out.println("RECURSIVE = " + sb.toString());
    }

    @Test
    public void testParseFunction() throws ParseException {

        Function f;

        f = ContractJSONParser.parseFunction(FUNCTION_A_JSON);
        System.out.println(f.getName() + " : " + f.canonicalSignature + " : " + f.outputTypes.get(0));
        Assert.assertEquals(1, f.outputTypes.elementTypes.length);
        Assert.assertEquals("uint64", f.outputTypes.get(0).canonicalType);
        f.encodeCallWithArgs((Object) new Tuple[] { new Tuple(new BigDecimal(BigInteger.ONE, 10), new BigDecimal(BigInteger.TEN, 10)) });

        printTupleType(f.inputTypes);

        printTupleType(f.outputTypes);

        f = ContractJSONParser.parseFunction(FUNCTION_B_JSON);
        System.out.println(f.getName() + " : " + f.canonicalSignature);
        Assert.assertNull(f.outputTypes);
        Assert.assertEquals("func((decimal,fixed128x18),fixed128x18[],(uint256,int256[],(int8,uint40)[]))", f.canonicalSignature);

        printTupleType(f.inputTypes);
    }

    @Test
    public void testGetFunctions() throws ParseException {

        List<Function> functions = ContractJSONParser.parseFunctions(CONTRACT_JSON);

        for(Function f : functions) {
            System.out.println(f.getName() + " : " + f.canonicalSignature);
        }

        Assert.assertEquals(1, functions.size());
    }

    @Test
    public void testGetEvents() throws ParseException {
        List<Event> events = ContractJSONParser.parseEvents(CONTRACT_JSON);

        Assert.assertEquals(1, events.size());

        for(Event event : events) {
            System.out.println(event);
        }
    }
}
