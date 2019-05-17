package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.util.Integers;
import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;

public class ABIJsonTest2 {

    private static final String ABI_V2_CASES_PATH = "tests/ethers-io/tests/tests/contract-interface-abi2.json";
    private static final String HEADLONG_CASES_PATH = "tests/headlong/tests/abi_tests.json";

    private static class TestCase {

        private static final Integers.UintType ADDRESS = new Integers.UintType(160);

        private final String name;

        private final Function function;
        private final TupleType types;
        private final Tuple values;
        private final byte[] result;

        public TestCase(JsonObject object, boolean function) throws ParseException {
            this.name = object.get("name").getAsString();
            String typesStr = object.get("types").getAsString();
            String valuesStr = object.get("values").getAsString();
            String resultStr = object.get("result").getAsString();

            JsonArray typesArray = JsonUtils.parseArray(typesStr);
            JsonArray valuesArray = JsonUtils.parseArray(valuesStr);

            StringBuilder tupleTypeString = new StringBuilder("(");
            for (JsonElement e : typesArray) {
                tupleTypeString.append(e.getAsString().replace("tuple", "")).append(',');
            }

            String tts = completeTupleTypeString(tupleTypeString);
            this.types = TupleType.parse(tts);
            this.values = parseTuple(this.types, valuesArray);
            this.result = FastHex.decode(resultStr, 2, resultStr.length() - 2);

            if(function) {
                this.function = Function.parse(name + tts);
            } else {
                this.function = null;
            }
        }

        private boolean test(boolean function) {
//            System.out.println(this.function.getCanonicalSignature());
            byte[] encoding = function ? this.function.encodeCall(values).array() : types.encode(values).array();
            try {
                Assert.assertArrayEquals(result, encoding);
                return true;
            } catch (AssertionError ae) {
                if(function) {
                    System.out.println(this.function.getCanonicalSignature() + ", " + this.values);
                    System.out.println(buildCallComparison(result, encoding));
                } else {
                    String[] resultTokens = format(result).split("[\n]");
                    String[] encodingTokens = format(encoding).split("[\n]");
                    System.out.println(types.canonicalType);
                    int i = 0;
                    for (; i < resultTokens.length; i++) {
                        String r = resultTokens[i];
                        String e = encodingTokens[i];
                        System.out.println(r + " " + e + " " + (r.equals(e) ? "" : "**************"));
                    }
                    for (; i < encodingTokens.length; i++) {
                        System.out.println("----------------------------------------------------------------" + " " + encodingTokens[i]);
                    }
                }
                throw ae;
            }
        }

        public static String buildCallComparison(byte[] expected, byte[] actual) {

            StringBuilder sb = new StringBuilder("ID\t")
                    .append(encode(Arrays.copyOfRange(expected, 0, Function.SELECTOR_LEN), HEX)).append(' ')
                    .append(encode(Arrays.copyOfRange(actual, 0, Function.SELECTOR_LEN), HEX))
                    .append('\n');
            int idx = Function.SELECTOR_LEN;
            final int min = Math.min(expected.length, actual.length);
            while(idx < min) {
                byte[] expectedRow = Arrays.copyOfRange(expected, idx, idx + UNIT_LENGTH_BYTES);
                byte[] actualRow = Arrays.copyOfRange(actual, idx, idx + UNIT_LENGTH_BYTES);
                sb.append(idx >>> UnitType.LOG_2_UNIT_LENGTH_BYTES)
                        .append('\t')
                        .append(encode(expectedRow, HEX)).append(' ')
                        .append(encode(actualRow, HEX)).append(Arrays.equals(expectedRow, actualRow) ? "" : " *************")
                        .append('\n');
                idx += UNIT_LENGTH_BYTES;
            }
            return sb.toString();
        }

        private static String format(byte[] abi) {
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            while(idx < abi.length) {
                sb.append(encode(Arrays.copyOfRange(abi, idx, idx + UNIT_LENGTH_BYTES), HEX)).append('\n');
                idx += UNIT_LENGTH_BYTES;
            }
            return sb.toString();
        }

        private static Object parseValue(ABIType<?> type, JsonElement value) {
            switch (type.typeCode()) {
            case ABIType.TYPE_CODE_BOOLEAN: return value.getAsJsonObject().get("value").getAsBoolean();
            case ABIType.TYPE_CODE_BYTE: return (byte) value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_INT: return value.getAsJsonObject().get("value").getAsInt();
            case ABIType.TYPE_CODE_LONG: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueValue = valueObj.get("value").getAsString();
                return Long.parseLong(valueValue);
            }
            case ABIType.TYPE_CODE_BIG_INTEGER: {
                JsonObject valueObj = value.getAsJsonObject();
                String valueType = valueObj.get("type").getAsString();
                String valueValue = valueObj.get("value").getAsString();
                if("string".equals(valueType)) {
                    BigInteger val = new BigInteger(FastHex.decode(valueValue, 2, valueValue.length() - 2));
                    if("address".equals(type.canonicalType)) {
                        return Integers.toUnsigned(val, ADDRESS);
                    }
                    BigIntegerType bigIntType = (BigIntegerType) type;
                    if(bigIntType.unsigned) {
                        return Integers.toUnsigned(val, new Integers.UintType(bigIntType.bitLength));
                    }
                    return val;
                } else {
                    return new BigInteger(valueValue);
                }
            }
            case ABIType.TYPE_CODE_BIG_DECIMAL: {
                String valueValue = value.getAsJsonObject().get("value").getAsString();
                return new BigDecimal(new BigInteger(valueValue), ((BigDecimalType) type).scale);
            }
            case ABIType.TYPE_CODE_ARRAY: return parseArray((ArrayType<?, ?>) type, value);
            case ABIType.TYPE_CODE_TUPLE: return parseTuple((TupleType) type, value.getAsJsonObject().get("value").getAsJsonArray());
            default: throw new Error();
            }
        }

        private static Object parseArray(ArrayType<?, ?> arrayType, JsonElement value) {
            if (arrayType.isString) {
                return value.getAsJsonObject().get("value").getAsString();
            } else if (value.isJsonArray()) {
                JsonArray valueValue = value.getAsJsonArray();
                final int len = valueValue.size();
                final ABIType<?> elementType = arrayType.elementType;
                int i = 0;
                if(Boolean.class == elementType.clazz) {
                    boolean[] array = new boolean[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Boolean) parseValue(elementType, iter.next());
                    }
                    return array;
                } else if(Byte.class == elementType.clazz) {
                    byte[] array = new byte[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Byte) parseValue(elementType, iter.next());
                    }
                    return array;
                } if (Integer.class == elementType.clazz) {
                    int[] array = new int[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Integer) parseValue(elementType, iter.next());
                    }
                    return array;
                } else if (Long.class == elementType.clazz) {
                    long[] array = new long[len];
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = (Long) parseValue(elementType, iter.next());
                    }
                    return array;
                } else {
                    Object[] array = (Object[]) Array.newInstance(elementType.clazz, len);
                    for (Iterator<JsonElement> iter = valueValue.iterator(); i < len; i++) {
                        array[i] = parseValue(elementType, iter.next());
                    }
                    return array;
                }
            } else {
                JsonObject valueObj = value.getAsJsonObject();
                String valueType = valueObj.get("type").getAsString();
                String valueValue = valueObj.get("value").getAsString();
                if ("buffer".equals(valueType)) {
                    return FastHex.decode(valueValue, 2, valueValue.length() - 2);
                } else {
                    throw new RuntimeException("????");
                }
            }
        }

        private static Tuple parseTuple(TupleType tupleType, JsonArray values) {
            ABIType[] abiTypes = tupleType.elementTypes;
            final int len = abiTypes.length;
            Object[] elements = new Object[len];
            int i = 0;
            for (Iterator<JsonElement> iter = values.iterator(); i < len; i++) {
                elements[i] = parseValue(abiTypes[i], iter.next());
            }
            return new Tuple(elements);
        }

        private static String completeTupleTypeString(StringBuilder canonicalTupleType) {
            final int len = canonicalTupleType.length();
            if(len == 1) {
                return "()";
            }
            return canonicalTupleType.replace(len - 1, len, ")").toString(); // replace trailing comma
        }
    }

    @Test
    public void testAbiV2Cases() throws ParseException, IOException {
        final JsonArray testCases = JsonUtils.parseArray(TestUtils.readResourceAsString(ABIJsonTest.class, ABI_V2_CASES_PATH));
        for (JsonElement e : testCases) {
            new TestCase(e.getAsJsonObject(), false).test(false);
        }
        System.out.println(testCases.size() + " cases passed");
    }

    @Test
    public void testHeadlongCases() throws ParseException, IOException {
        final JsonArray testCases = JsonUtils.parseArray(TestUtils.readResourceAsString(ABIJsonTest.class, HEADLONG_CASES_PATH));
        for (JsonElement e : testCases) {
            new TestCase(e.getAsJsonObject(), true).test(true);
        }
        System.out.println(testCases.size() + " cases passed");
    }
}
